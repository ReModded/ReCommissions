package dev.remodded.recommission

import dev.remodded.recommission.command.argument.CustomItemPredicate
import dev.remodded.recommission.gui.CommissionClaimMenu
import dev.remodded.recommission.gui.CommissionDonateMenu
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.itemStack
import org.bukkit.block.ShulkerBox
import org.bukkit.configuration.Configuration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import java.io.File
import java.util.function.Predicate
import kotlin.math.min

data class Commission(
    val name: String,

    val items: MutableList<Item> = arrayListOf()
) {
    private val donateMenuLazy = lazy { CommissionDonateMenu(this) }
    val donateMenu by donateMenuLazy

    private val claimMenuLazy = lazy { CommissionClaimMenu(this) }
    val claimMenu by claimMenuLazy

    fun update() {
        if (donateMenuLazy.isInitialized())
            donateMenu.update()

        if (claimMenuLazy.isInitialized())
            claimMenu.update()
    }

    fun close() {
        if (donateMenuLazy.isInitialized())
            donateMenu.inventory.close()

        if (claimMenuLazy.isInitialized())
            claimMenu.inventory.close()
    }

    fun isFulfilled() = items.all { it.isFulfilled() }

    data class Item(
        val predicate: CustomItemPredicate.Result,
        val display: ItemStack,

        var amount: Int,
        var left: Int = amount,
    ) : Predicate<ItemStack> {
        private val singleItem = predicate.tryGetSingleItem()
        private val isSingleItem: Boolean get() = singleItem != null

        private val consumedItems: MutableList<ItemStack> = mutableListOf()

        override fun test(item: ItemStack): Boolean {
            return !isFulfilled() && predicate.test(item)
        }

        fun isFulfilled() = left <= 0
        fun hasBeenClaimed() = amount <= 0


        /**
         * Tries to donate the given [inStack] to the commission.
         *
         * @param inStack the stack to donate
         * @return the donated item, or null if no item was donated
         */
        fun tryDonate(inStack: ItemStack): Boolean {
            if (isFulfilled())
                return false

            if (inStack.isEmpty)
                return false

            if (isShulkerBox(inStack))
                if (handleShulkerBox(inStack) { stack -> if (tryDonate(stack)) stack else null })
                    return true

            if (!test(inStack))
                return false

            val toConsume = min(left, inStack.amount)

            if (!isSingleItem) {
                val consumedItem = inStack.clone()
                consumedItem.amount = toConsume

                for (stack in consumedItems) {
                    val toFull = stack.maxStackSize - stack.amount
                    if (toFull > 0 && stack.isSimilar(consumedItem)) {
                        val filing = min(toFull, consumedItem.amount)
                        consumedItem.amount -= filing
                        stack.amount += filing
                    }
                }

                if (!consumedItem.isEmpty)
                    consumedItems.add(consumedItem)
            }

            inStack.amount -= toConsume
            left -= toConsume

            return true
        }

        /**
         * Tries to claim commission item, filling the input stack with the items
         * that are claimed.
         *
         * @param inStack Input stack to witch claimed items will be added
         *
         * @return the claimed item, or null if no item was claimed
         */
        fun tryClaim(inStack: ItemStack): ItemStack? {
            if (hasBeenClaimed())
                return null

            if (!isFulfilled())
                return null

            // iF the stack is a shulker box, we try to fill it with items
            if (isShulkerBox(inStack))
                if (handleShulkerBox(inStack, ::tryClaim))
                    return inStack

            // Cannot claim more than the stack can hold
            if (inStack.amount >= inStack.maxStackSize)
                return null

            if (consumedItems.isNotEmpty()) {
                var haveClaimedAny = false
                val iter = consumedItems.iterator()

                while (iter.hasNext()) {
                    val stack = iter.next()
                    if (inStack.isSimilar(stack)) {
                        val fill = min(inStack.maxStackSize - inStack.amount, stack.amount)
                        inStack.amount += fill
                        stack.amount -= fill
                        haveClaimedAny = true

                        // If the stack is full, we can't claim any more
                        if (inStack.amount >= inStack.maxStackSize) {
                            if (stack.isEmpty)
                                iter.remove()
                            break
                        }
                    } else if (inStack.isEmpty) {
                        iter.remove()

                        // If the stack is not full, we can try to claim it again
                        if (stack.amount < stack.maxStackSize)
                            return tryClaim(stack) ?: stack
                        return stack
                    }

                    // If the stack is empty, we should remove it
                    if (stack.isEmpty)
                        iter.remove()
                }

                if (haveClaimedAny)
                    return inStack
            }

            // It's not single item commission, at this point we didn't claim anything
            if (singleItem == null)
                return null

            val toClaim = min(amount, singleItem.maxStackSize)

            // Filling an empty stack
            if (inStack.isEmpty) {
                val result = singleItem.clone()

                result.amount = toClaim
                amount -= toClaim

                return result
            }

            // We can't file the stack if it's not similar
            if (!inStack.isSimilar(singleItem))
                return null

            val claimed = min(toClaim, inStack.maxStackSize - inStack.amount)
            inStack.amount += claimed
            amount -= claimed

            return inStack
        }

        private fun isShulkerBox(item: ItemStack): Boolean {
            val itemMeta = item.itemMeta
            if (itemMeta !is BlockStateMeta)
                return false

            val blockStateMeta = itemMeta.blockState
            if (blockStateMeta !is ShulkerBox)
                return false

            return true
        }

        private fun handleShulkerBox(item: ItemStack, action: (ItemStack) -> ItemStack?): Boolean {
            val itemMeta = item.itemMeta
            if (itemMeta !is BlockStateMeta)
                return false

            val blockStateMeta = itemMeta.blockState
            if (blockStateMeta !is ShulkerBox)
                return false

            val inventory = blockStateMeta.inventory
//            if (inventory.isEmpty)
//                return false

            var hadValid = false

            for (slot in 0 ..< inventory.size) {
                val itemStack = inventory.getItem(slot) ?: ItemStack.empty()

                val result = action(itemStack)
                if (result == null)
                    continue

                inventory.setItem(slot, result)

                hadValid = true
            }

            if (hadValid) {
                itemMeta.blockState = blockStateMeta
                item.itemMeta = itemMeta
            }

            return hadValid
        }

        fun saveToMap(): Map<String, Any> {
            return mapOf(
                "amount" to amount,
                "predicate" to predicate.predicateString,
                "display" to display,
                "left" to left,
                "consumedItems" to consumedItems,
            )
        }

        companion object {
            fun load(config: Map<String, Any>): Item {
                val amount = config["amount"] as? Int ?: 1
                val left = config["left"] as? Int ?: amount

                val predicateString = config["predicate"] as? String ?: throw IllegalArgumentException("Predicate is required")
                val predicate = CustomItemPredicate.Result.fromString(predicateString)

                val predicateItem = predicate.tryGetSingleItem()


                val display = config["display"] as? ItemStack ?: predicateItem ?: throw IllegalArgumentException("Display is required")
                val item = Item(predicate, display, amount, left)

                item.consumedItems.addAll((config["consumedItems"] as? List<*>)?.filterIsInstance<ItemStack>() ?: emptyList())

                return item
            }
        }
    }

    fun save(commissionsDir: File = ReCommission.INSTANCE.commissionsDir) {
        val data = YamlConfiguration()
        data["items"] = items.map { it.saveToMap() }.toList()
        data.save(commissionsDir.resolve("$name.yml"))
    }

    companion object {
        fun load(dataFile: File): Commission {
            val dataConfig: Configuration = YamlConfiguration.loadConfiguration(dataFile)
            val items = dataConfig.getMapList("items")

            return Commission(dataFile.nameWithoutExtension, items.map {
                @Suppress("UNCHECKED_CAST")
                Item.load(it as Map<String, Any>)
            }.toMutableList())
        }
    }
}
