package me.syari.ss.economy

import me.syari.ss.core.auto.OnEnable
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    companion object {
        /**
         * 経済プラグインのインスタンス
         */
        lateinit var economyPlugin: JavaPlugin
    }

    override fun onEnable() {
        economyPlugin = this
        OnEnable.register(CommandCreator, ConfigLoader, DatabaseConnector)
    }
}