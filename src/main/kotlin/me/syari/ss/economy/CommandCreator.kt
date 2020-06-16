package me.syari.ss.economy

import me.syari.ss.core.auto.OnEnable
import me.syari.ss.core.command.create.CreateCommand.createCommand
import me.syari.ss.core.command.create.CreateCommand.element
import me.syari.ss.core.command.create.CreateCommand.tab
import me.syari.ss.core.command.create.ErrorMessage
import me.syari.ss.core.player.UUIDPlayer
import me.syari.ss.economy.ConfigLoader.loadConfig
import me.syari.ss.economy.DatabaseConnector.MoneyData.clearCache
import me.syari.ss.economy.DatabaseConnector.MoneyData.deleteCache
import me.syari.ss.economy.DatabaseConnector.MoneyData.getCacheList
import me.syari.ss.economy.DatabaseConnector.MoneyData.money
import me.syari.ss.economy.DatabaseConnector.MoneyRank
import me.syari.ss.economy.DatabaseConnector.MoneyRank.lastPage
import me.syari.ss.economy.DatabaseConnector.createTable
import me.syari.ss.economy.Main.Companion.economyPlugin
import org.bukkit.OfflinePlayer

object CommandCreator: OnEnable {
    /**
     * コマンドを作成します
     */
    override fun onEnable() {
        createCommand(
            economyPlugin,
            "economy",
            "Economy",
            tab { _, _ -> element("config", "database") },
            tab("config") { _, _ -> element("reload") },
            tab("database") { _, _ -> element("create", "check", "cache") },
            tab("database cache") { _, _ -> element("delete", "clear") },
            tab("database cache delete") { _, _ -> element(getCacheList()) }
        ) { sender, args ->
            when (args.whenIndex(0)) {
                "config" -> {
                    when (args.whenIndex(1)) {
                        "reload" -> {
                            loadConfig(sender)
                            sendWithPrefix("&fコンフィグをリロードしました")
                        }
                    }
                }
                "database" -> {
                    when (args.whenIndex(1)) {
                        "create" -> {
                            val state = createTable()
                            val builder = StringBuilder()
                            builder.append(if (state.isSuccess) "&f" else "&c")
                            builder.append("テーブル作成に")
                            builder.append(state.message)
                            sendWithPrefix(builder.toString())
                        }
                        "check" -> {
                            val state = DatabaseConnector.checkConnect()
                            val builder = StringBuilder()
                            builder.append(if (state.isSuccess) "&f" else "&c")
                            builder.append("接続に")
                            builder.append(state.message)
                            sendWithPrefix(builder.toString())
                        }
                        "cache" -> {
                            when (args.whenIndex(2)) {
                                "delete" -> {
                                    val player = args.getOfflinePlayer(3, false) ?: return@createCommand
                                    deleteCache(UUIDPlayer(player))
                                    sendWithPrefix("&6${player.name} &fのキャッシュを削除しました")
                                }
                                "clear" -> {
                                    clearCache()
                                    sendWithPrefix("&f全てのキャッシュを削除しました")
                                }
                            }
                        }
                    }
                }
            }
        }

        createCommand(economyPlugin, "money", "Money", tab { sender, _ ->
            element("check", "rank").joinIfOp(sender, "set", "inc", "dec")
        }) { sender, args ->
            fun help() {
                sendHelp(
                    "money check" to "所持金を確認します", "money rank [ページ数]" to "所持金ランキングを表示します"
                ).ifOp(
                    "money set" to "所持金を設定します", "money inc" to "所持金を加算します", "money dec" to "所持金を減算します"
                )
            }

            when (args.whenIndex(0)) {
                null, "check" -> {
                    val player = if (args.size == 1) {
                        sender as? OfflinePlayer ?: return@createCommand sendError(ErrorMessage.OnlyPlayer)
                    } else {
                        args.getOfflinePlayer(1, true) ?: return@createCommand
                    }
                    val builder = StringBuilder()
                    if (player != sender) {
                        builder.append("&6${player.name} &fの")
                    }
                    builder.append("&f所持金は &6${player.money}JPY &fです")
                    sendWithPrefix(builder.toString())
                }
                "rank" -> {
                    val page = args.getOrNull(1)?.toIntOrNull() ?: 1
                    val rank = MoneyRank.get(page)
                    val builder = StringBuilder()
                    builder.appendln("&f所持金ランキング &d$page &7/ &d$lastPage")
                    rank.forEach { data ->
                        builder.appendln(data.toString())
                    }
                    sendWithPrefix(builder.toString())
                }
                "set", "inc", "dec" -> {
                    if (sender.isOp) {
                        val player: OfflinePlayer
                        val value: Int?
                        val builder = StringBuilder()
                        if (args.size == 2) {
                            player = sender as? OfflinePlayer ?: return@createCommand sendError(ErrorMessage.OnlyPlayer)
                            value = args.getOrNull(1)?.toIntOrNull()
                        } else {
                            player = args.getOfflinePlayer(1, true) ?: return@createCommand
                            value = args.getOrNull(2)?.toIntOrNull()
                            builder.append("&6${player.name} &fの")
                        }
                        if (value == null) return@createCommand sendError("変更する金額を入力してください")
                        builder.append("&f所持金を")
                        when (args.whenIndex(0)) {
                            "set" -> {
                                builder.append("&6${player.money}JPY &fから ")
                                player.money = value
                                builder.append("&6${value}JPY &fに変更しました")
                            }
                            "inc" -> {
                                builder.append("&6${player.money}JPY &fに ")
                                player.money += value
                                builder.append("&6${value}JPY &f足して &6${player.money}JPY &fに変更しました")
                            }
                            "dec" -> {
                                builder.append("&6${player.money}JPY &fから ")
                                player.money -= value
                                builder.append("&6${value}JPY &減らして &6${player.money}JPY &fに変更しました")
                            }
                        }
                        sendWithPrefix(builder.toString())
                    } else {
                        help()
                    }
                }
                else -> help()
            }
        }
    }
}