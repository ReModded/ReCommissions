package dev.remodded.recommission.gui

import dev.remodded.recommission.Commission
import dev.remodded.recommission.translate
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack

class CommissionDonateMenu(
    commission: Commission
): CommissionMenu(commission, Component.translatable("commission.gui.donate.title")) {

    override fun getDisplayItem(item: Commission.Item): ItemStack {
        val itemStack = item.display.clone()

        val collected = item.amount -item.left
        val amount = item.amount
        val percentage =
            if (amount > 0)
                collected.toFloat() / amount.toFloat() * 100.0f
            else
                -1.0f

        itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(listOf(
            (
                if (amount > 0)
                    Component.text("(${collected}/$amount)  ${"%.2f".format(percentage)}%", when (percentage) {
                        in 0.0f..33.0f -> NamedTextColor.RED
                        in 33.0f..66.0f -> NamedTextColor.YELLOW
                        else -> NamedTextColor.GREEN
                    })
                else
                    Component.translatable("commission.gui.donate.fulfilled", NamedTextColor.GOLD)
            ).style {
                it.decoration(TextDecoration.ITALIC, false)
            }.translate(getViewerLocale())
        )))
        itemStack.unsetData(DataComponentTypes.ATTRIBUTE_MODIFIERS)

        return itemStack
    }

    companion object {
        fun open(player: Player, commission: Commission) {
            val menu = CommissionDonateMenu(commission)
            player.openInventory(menu.inventory)
            menu.update()
            commission.openMenus.add(menu)
        }
    }

    object Listener : org.bukkit.event.Listener {
        @EventHandler
        fun onInventoryClick(ev: InventoryClickEvent) {
            val commissionMenu = ev.inventory.holder
            val clickedInventory = ev.clickedInventory
            if (commissionMenu !is CommissionDonateMenu || clickedInventory == null) return

            // We can ignore this event if the player is trying to collect the item due to fact that
            // all items in the Commission inventory have custom data (lore, name, etc)
            if (ev.action == InventoryAction.COLLECT_TO_CURSOR)
                return

            // Clicked commission inventory
            if (ev.clickedInventory == commissionMenu.inventory) {
                if (!setOf(InventoryAction.PLACE_ALL, InventoryAction.PLACE_ONE, InventoryAction.PLACE_FROM_BUNDLE, InventoryAction.SWAP_WITH_CURSOR).contains(ev.action)) {
                    ev.isCancelled = true
                    return
                }
            }

            val item = if (ev.cursor.isEmpty) ev.currentItem else ev.cursor
            if (item == null || item.isEmpty) {
                ev.isCancelled = true
                return
            }

            if (ev.clickedInventory != commissionMenu.inventory && !setOf(ClickType.SHIFT_RIGHT, ClickType.SHIFT_LEFT).contains(ev.click) )
                return

            for (commissionItem in commissionMenu.commission.items) {
                if (commissionItem.tryDonate(item, ev.whoClicked)) {
                    ev.isCancelled = true

                    commissionMenu.commission.save()
                    commissionMenu.commission.update()
                    break
                }
            }

            if (!ev.isCancelled)
                ev.whoClicked.sendMessage(Component.translatable("commission.gui.donate.invalid", NamedTextColor.RED))

            ev.isCancelled = true
        }

        @EventHandler
        fun onItemDrag(ev: InventoryDragEvent) {
            val commissionHolder = ev.inventory.holder
            if (commissionHolder !is CommissionDonateMenu) return

            // todo: handle dragging
            ev.whoClicked.sendMessage(Component.text("Dragging is not supported yet.", NamedTextColor.RED))
            ev.isCancelled = true
            return

//            val view = ev.view
//            // Allow dragging in the bottom inventory
//            if (ev.rawSlots.all { view.getInventory(it) == view.bottomInventory }) return
//
//            val result = handleItemInsert(commissionHolder, ev.oldCursor)
//            if (result == true)
//                return
//
//            if (result == false)
//                ev.whoClicked.sendMessage(Component.text("Invalid commission item", NamedTextColor.RED))
//            ev.isCancelled = true
        }
    }
}
