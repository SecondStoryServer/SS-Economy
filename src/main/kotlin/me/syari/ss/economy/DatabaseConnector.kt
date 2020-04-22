package me.syari.ss.economy

import me.syari.ss.core.auto.OnEnable
import me.syari.ss.core.player.UUIDPlayer
import me.syari.ss.core.scheduler.CustomScheduler.runLater
import me.syari.ss.core.sql.MySQL
import me.syari.ss.economy.Main.Companion.economyPlugin
import org.bukkit.OfflinePlayer

object DatabaseConnector : OnEnable {
    override fun onEnable() {
        createTable()
    }

    private var sql: MySQL? = null

    fun setConfig(host: String?, port: Int?, database: String?, user: String?, password: String?) {
        sql = MySQL.create(host, port, database, user, password)
    }

    fun checkConnect(): ConnectState {
        return ConnectState.get(sql?.canConnect())
    }

    enum class ConnectState(val message: String) {
        Success("成功しました"),
        CatchException("失敗しました"),
        NullError("必要な設定が足りていません");

        val isSuccess get() = this == Success

        companion object {
            fun get(bool: Boolean?): ConnectState {
                return when (bool) {
                    true -> Success
                    false -> CatchException
                    null -> NullError
                }
            }
        }
    }

    fun createTable(): ConnectState {
        return ConnectState.get(sql?.use {
            executeUpdate("""
                CREATE TABLE IF NOT EXISTS Money(UUID VARCHAR(36) PRIMARY KEY, Value INT);
            """.trimIndent())
        })
    }

    object MoneyData {
        private val moneyDataCache = mutableMapOf<UUIDPlayer, Int>()

        var OfflinePlayer.money: Int
            get() = get(UUIDPlayer(uniqueId))
            set(value) {
                set(UUIDPlayer(uniqueId), value)
            }

        fun set(uuidPlayer: UUIDPlayer, money: Int) {
            sql?.use {
                if (money != 0) {
                    executeUpdate("""
                        INSERT INTO Money VALUE ('$uuidPlayer', $money) ON DUPLICATE KEY UPDATE Money = $money;
                    """.trimIndent())
                } else {
                    executeUpdate("""
                        DELETE FROM Money WHERE UUID = '$uuidPlayer' LIMIT 1;
                    """.trimIndent())
                }
            }
            moneyDataCache[uuidPlayer] = money
        }

        fun get(uuidPlayer: UUIDPlayer): Int {
            return moneyDataCache.getOrPut(uuidPlayer) { getFromSQL(uuidPlayer) }
        }

        private fun getFromSQL(uuidPlayer: UUIDPlayer): Int {
            var money = 0
            sql?.use {
                val result = executeQuery("""
                    SELECT Value FROM Money WHERE UUID = '$uuidPlayer' LIMIT 1;
                """.trimIndent())
                if (result.next()) {
                    money = result.getInt(1)
                }
            }
            return money
        }

        fun getCacheList(): List<String> {
            return moneyDataCache.mapNotNull { it.key.name }
        }

        fun deleteCache(uuidPlayer: UUIDPlayer) {
            moneyDataCache.remove(uuidPlayer)
        }

        fun clearCache() {
            moneyDataCache.clear()
        }
    }

    object MoneyRank {
        data class RankData(
                val rank: Int,
                val uuidPlayer: UUIDPlayer,
                val money: Int
        ) {
            override fun toString() = "&6$rank &f${uuidPlayer.name} &a${money}JPY"
        }

        private val rankDataList = mutableListOf<RankData>()
        var lastPage = 0
            private set

        private fun load() {
            if (rankDataList.isNotEmpty()) return
            sql?.use {
                val result = executeQuery("""
                    SELECT UUID, Value, Rank FROM (
                        SELECT
                            CASE
                                WHEN @lastValue = Value THEN
                                    @rank
                                ELSE
                                    @rank := @rank + @count
                            END AS Rank,
                            CASE
                                WHEN @lastValue = Value THEN
                                    @count := @count + 1
                                ELSE
                                    @count := 1
                            END AS SameValueCount,
                            UUID,
                            Value,
                            @lastValue := Value
                        FROM (SELECT @rank := 1, @lastValue := null, @count := 0) AS DummyTable,
                        Money ORDER BY Value DESC
                    ) AS ResultTable;
                """.trimIndent())
                while (result.next()) {
                    val uuidPlayer = UUIDPlayer.create(result.getString(1)) ?: continue
                    val money = result.getInt(2)
                    val rank = result.getInt(3)
                    rankDataList.add(RankData(rank, uuidPlayer, money))
                }
            }
            lastPage = (rankDataList.size / 10) + 1
            runLater(economyPlugin, 60 * 20) {
                rankDataList.clear()
            }
        }

        fun get(page: Int): List<RankData> {
            if (page < 1) return get(1)
            load()
            val begin = (page - 1) * 10
            val end = page * 10 - 1
            return rankDataList.slice(begin..end)
        }
    }
}