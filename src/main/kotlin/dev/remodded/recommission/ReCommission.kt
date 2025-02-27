package dev.remodded.recommission

import dev.remodded.recommission.command.CommissionCommand
import dev.remodded.recommission.gui.CommissionClaimMenu
import dev.remodded.recommission.gui.CommissionDonateMenu
import dev.remodded.recommission.gui.CommissionMenu
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ReCommission : JavaPlugin() {

    val commissions = mutableListOf<Commission>()

    val commissionsDir = File(this.dataFolder, "commissions")

    override fun onEnable() {
        _instance = this

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { ev ->
            ev.registrar().register(CommissionCommand.register())
        }

        Bukkit.getPluginManager().registerEvents(CommissionMenu.Listener, this)
        Bukkit.getPluginManager().registerEvents(CommissionClaimMenu.Listener, this)
        Bukkit.getPluginManager().registerEvents(CommissionDonateMenu.Listener, this)

        reloadCommissions()
    }

    override fun onDisable() {
        // Plugin shutdown logic
        saveCommissions()
    }

    fun saveCommissions() {
        if (!commissionsDir.exists())
            commissionsDir.mkdirs()

        for (commission in commissions) {
            commission.save(commissionsDir)
        }
    }

    fun reloadCommissions() {
        for (commission in commissions)
            commission.close()
        commissions.clear()

        if (!commissionsDir.exists())
            commissionsDir.mkdirs()

        for (file in commissionsDir.listFiles()) {
            if (!file.isFile)
                continue

            val commission = try {
                Commission.load(file)
            } catch (e: Exception) {
                logger.warning("Failed to load commission ${file.name}. Skipping.")
                e.printStackTrace()
                continue
            }

            commissions.add(commission)
        }
    }

    companion object {
        private lateinit var _instance: ReCommission
        val INSTANCE get() = _instance
    }
}
