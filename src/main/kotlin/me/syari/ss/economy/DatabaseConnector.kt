package me.syari.ss.economy

import me.syari.ss.core.player.UUIDPlayer
import me.syari.ss.core.scheduler.CustomScheduler.runLater
import me.syari.ss.core.sql.MySQL
import me.syari.ss.economy.Main.Companion.economyPlugin

object DatabaseConnector {
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
                return when(bool){
                    true -> Success
                    false -> CatchException
                    null -> NullError
                }
            }
        }
    }

    fun createTable(): ConnectState {
        return ConnectState.get(sql?.use {
            executeUpdate("CREATE TABLE IF NOT EXISTS Money(UUID VARCHAR(36) PRIMARY KEY, Value INT UNSIGNED);")
        })
    }

    object MoneyRank {
        data class RankData(
            val rank: Int,
            val uuidPlayer: UUIDPlayer,
            val money: Long
        )

        private val rankDataList = mutableListOf<RankData>()

        private fun load() {
            if(rankDataList.isNotEmpty()) return
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
                while(result.next()){
                    val uuidPlayer = UUIDPlayer.create(result.getString(1)) ?: continue
                    val money = result.getLong(2)
                    val rank = result.getInt(3)
                    rankDataList.add(RankData(rank, uuidPlayer, money))
                }
            }
            runLater(economyPlugin, 60 * 20){
                rankDataList.clear()
            }
        }

        fun get(page: Int): List<RankData> {
            if(page < 1) return get(1)
            load()
            val begin = (page - 1) * 10
            val end = page * 10 - 1
            return rankDataList.slice(begin .. end)
        }
    }
}