package me.syari.ss.economy

import me.syari.ss.core.config.CreateConfig.config
import me.syari.ss.core.config.dataType.ConfigDataType
import me.syari.ss.economy.Main.Companion.economyPlugin
import org.bukkit.command.CommandSender

object ConfigLoader {
    fun loadConfig(output: CommandSender) {
        config(economyPlugin, output, "config.yml") {
            DatabaseConnector.setConfig(
                get("sql.host", ConfigDataType.STRING),
                get("sql.port", ConfigDataType.INT),
                get("sql.database", ConfigDataType.STRING),
                get("sql.user", ConfigDataType.STRING),
                get("sql.password", ConfigDataType.STRING)
            )
        }
    }
}