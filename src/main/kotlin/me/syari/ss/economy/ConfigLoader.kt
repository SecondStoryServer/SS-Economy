package me.syari.ss.economy

import me.syari.ss.core.Main.Companion.console
import me.syari.ss.core.auto.OnEnable
import me.syari.ss.core.config.CreateConfig.config
import me.syari.ss.core.config.dataType.ConfigDataType
import me.syari.ss.economy.Main.Companion.economyPlugin
import org.bukkit.command.CommandSender

object ConfigLoader : OnEnable {
    /**
     * 起動時にコンフィグを読み込みます
     */
    override fun onEnable() {
        loadConfig(console)
    }

    /**
     * コンフィグを読み込みます
     * @param output メッセージ出力先
     */
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