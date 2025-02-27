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

class CommissionClaimMenu(
    commission: Commission
): CommissionMenu(commission, Component.translatable("commission.gui.claim.title")) {

    override fun getDisplayItem(item: Commission.Item): ItemStack {
        val itemStack = item.display.clone()

        val amount = item.amount

        itemStack.setData(DataComponentTypes.LORE, ItemLore.lore(listOf(
            (
                if (item.hasBeenClaimed())
                    Component.translatable("commission.gui.claim.claimed", NamedTextColor.GOLD)
                else
                    Component.translatable("commission.gui.claim.left", NamedTextColor.GOLD)
                        .arguments(Component.text(amount.toString()))
            ).style {
                it.decoration(TextDecoration.ITALIC, false)
            }.translate(getViewerLocale())
        )))
        itemStack.unsetData(DataComponentTypes.ATTRIBUTE_MODIFIERS)

        return itemStack
    }

    companion object {
        fun open(player: Player, commission: Commission) {
            val menu = CommissionClaimMenu(commission)
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
            if (commissionMenu !is CommissionClaimMenu || clickedInventory == null) return

            // We only care about the commission inventory
            if (ev.clickedInventory != commissionMenu.inventory) {
                if (setOf(ClickType.SHIFT_RIGHT, ClickType.SHIFT_LEFT).contains(ev.click))
                    ev.isCancelled = true
                return
            }

            // We can ignore this event if the player is trying to collect the item due to fact that
            // all items in the Commission inventory have custom data (lore, name, etc)
            if (ev.action == InventoryAction.COLLECT_TO_CURSOR)
                return

            ev.isCancelled = true

            val item = commissionMenu.getItemAt(ev.slot) ?: return

            item.tryClaim(ev.cursor)?.let {
                ev.isCancelled = true
                ev.view.setCursor(it)

                commissionMenu.commission.save()
                commissionMenu.commission.update()
            }
        }

        @EventHandler
        fun onItemDrag(ev: InventoryDragEvent) {
            val commissionHolder = ev.inventory.holder
            if (commissionHolder !is CommissionClaimMenu) return

            val view = ev.view
            // Forbid dragging in the top inventory
            if (ev.rawSlots.any { view.getInventory(it) == view.topInventory }) {
                ev.isCancelled = true
                return
            }
        }
    }
}
