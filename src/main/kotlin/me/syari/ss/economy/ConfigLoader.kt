package me.syari.ss.economy

import me.syari.ss.core.Main.Companion.console
import me.syari.ss.core.auto.OnEnable
import me.syari.ss.core.config.CreateConfig.config
import me.syari.ss.core.config.dataType.ConfigDataType
import me.syari.ss.economy.Main.Companion.economyPlugin
import org.bukkit.command.CommandSender

object ConfigLoader: OnEnable {
    /**
     * 起動時にコンフィグを読み込みます
     */
    override fun onEnable() {
        loadConfig(console)
    }

    private val defaultConfig = mapOf(
        "sql.host" to "localhost", "sql.port" to 3306, "sql.database" to "", "sql.user" to "", "sql.password" to ""
    )

    /**
     * コンフィグを読み込みます
     * @param output メッセージ出力先
     */
    fun loadConfig(output: CommandSender) {
        config(economyPlugin, output, "config.yml", default = defaultConfig) {
            DatabaseConnector.sql = get("sql", ConfigDataType.MYSQL)
        }
    }
}