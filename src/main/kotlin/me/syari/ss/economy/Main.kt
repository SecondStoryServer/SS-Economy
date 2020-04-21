package me.syari.ss.economy

import me.syari.ss.core.auto.OnEnable
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    companion object {
        lateinit var economyPlugin: JavaPlugin
    }

    override fun onEnable() {
        economyPlugin = this
        OnEnable.register(CommandCreator)
    }
}