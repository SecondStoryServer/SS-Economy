package me.syari.ss.economy

import org.bukkit.plugin.java.JavaPlugin

class Main: JavaPlugin() {
    companion object {
        lateinit var economyPlugin: JavaPlugin
    }

    override fun onEnable() {
        economyPlugin = this
    }
}