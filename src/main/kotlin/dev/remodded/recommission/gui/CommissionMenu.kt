package dev.remodded.recommission.gui

import dev.remodded.recommission.Commission
import dev.remodded.recommission.translate
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class CommissionMenu(
    val commission: Commission,
    title: Component,
) : InventoryHolder {
    private val inv = Bukkit.createInventory(this, 6 * 9, title)

    var page: Int = 0
        private set

    protected abstract fun getDisplayItem(item: Commission.Item): ItemStack

    fun update() {
        setPage(page)
    }

    fun setPage(page: Int) {
        this.page = page
        val startIndex = page * PAGE_SIZE
        inv.clear()

        for (i in 0 ..< PAGE_SIZE) {
            val itemIndex = startIndex + i

            if (itemIndex >= commission.items.size)
                break

            val item = commission.items[itemIndex]

            inv.setItem(i, getDisplayItem(item))
        }

        if (startIndex > 0)
            inv.setItem(PREV_PAGE_SLOT, ItemStack(Material.PAPER).apply {
                setData(DataComponentTypes.ITEM_NAME, Component.translatable("commission.gui.prev_page").translate(getViewerLocale()))
                setData(DataComponentTypes.LORE, ItemLore.lore(listOf(Component.translatable("commission.gui.prev_page.tooltip",
                    Style.style { s ->
                        s.decoration(TextDecoration.ITALIC, false)
                    }
                ).translate(getViewerLocale()))))
            })

        if (startIndex + PAGE_SIZE < commission.items.size)
            inv.setItem(NEXT_PAGE_SLOT, ItemStack(Material.PAPER).apply {
                setData(DataComponentTypes.ITEM_NAME, Component.translatable("commission.gui.next_page").translate(getViewerLocale()))
                setData(DataComponentTypes.LORE, ItemLore.lore(listOf(Component.translatable("commission.gui.next_page.tooltip",
                    Style.style { s ->
                        s.decoration(TextDecoration.ITALIC, false)
                    }
                ).translate(getViewerLocale()))))
            })

        inv.viewers.forEach {
            if (it is Player)
                it.updateInventory()
        }
    }

    fun getItemAt(slot: Int): Commission.Item? {
        val pageStartIndex = page * PAGE_SIZE

        val itemIndex = pageStartIndex + slot
        if (itemIndex < commission.items.size)
            return commission.items[itemIndex]

        return null
    }

    fun getViewerLocale(): Locale {
        return inv.viewers.firstOrNull()?.let {
            if (it is Player) it.locale() else null
        } ?: Locale.US
    }

    override fun getInventory() = inv

    companion object {
        private const val PAGE_SIZE = 5 * 9

        private const val PREV_PAGE_SLOT = PAGE_SIZE
        private const val NEXT_PAGE_SLOT = PAGE_SIZE + 8
    }

    object Listener : org.bukkit.event.Listener {
        @EventHandler
        fun onInventoryClick(ev: InventoryClickEvent) {
            val commissionHolder = ev.inventory.holder
            val clickedInventory = ev.clickedInventory
            if (commissionHolder !is CommissionMenu || clickedInventory == null) return

            // Handle page navigation
            if (ev.clickedInventory == commissionHolder.inventory) {
                if (ev.currentItem?.isEmpty == false) {
                    if (ev.slot == PREV_PAGE_SLOT) {
                        commissionHolder.setPage(commissionHolder.page - 1)
                        ev.isCancelled = true
                        return
                    }

                    if (ev.slot == NEXT_PAGE_SLOT) {
                        commissionHolder.setPage(commissionHolder.page + 1)
                        ev.isCancelled = true
                        return
                    }
                }
            }
        }

        @EventHandler
        fun onInventoryClose(ev: InventoryCloseEvent) {
            val commissionMenu = ev.inventory.holder
            if (commissionMenu !is CommissionMenu) return

            commissionMenu.commission.openMenus.remove(commissionMenu)
        }
    }
}
