package me.syari.ss.economy

import me.syari.ss.core.auto.OnEnable
import me.syari.ss.core.command.create.CreateCommand.createCommand
import me.syari.ss.core.command.create.CreateCommand.element
import me.syari.ss.core.command.create.CreateCommand.tab
import me.syari.ss.economy.Main.Companion.economyPlugin

object CommandCreator : OnEnable {
    override fun onEnable() {
        createCommand(economyPlugin, "economy", "Economy",
            tab { _, _ -> element("database") },
            tab("database") { _, _ -> element("check") }
        ) { _, args ->
            when (args.whenIndex(0)) {
                "database" -> {
                    when (args.whenIndex(1)) {
                        "check" -> {
                            val state = DatabaseConnector.checkConnect()
                            val builder = StringBuilder()
                            builder.append(if (state.isSuccess) "&f" else "&c")
                            builder.append("接続に")
                            builder.append(state.message)
                            sendWithPrefix(builder.toString())
                        }
                    }
                }
            }
        }
    }
}