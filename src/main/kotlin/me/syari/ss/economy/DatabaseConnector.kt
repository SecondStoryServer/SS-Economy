package me.syari.ss.economy

import me.syari.ss.core.auto.OnEnable
import me.syari.ss.core.player.UUIDPlayer
import me.syari.ss.core.scheduler.CreateScheduler.runLater
import me.syari.ss.core.sql.ConnectState
import me.syari.ss.core.sql.ConnectState.Companion.checkConnect
import me.syari.ss.core.sql.MySQL
import me.syari.ss.economy.Main.Companion.economyPlugin
import org.bukkit.OfflinePlayer

object DatabaseConnector: OnEnable {
    /**
     * 起動時にテーブルを作成します
     */
    override fun onEnable() {
        createTable()
    }

    internal var sql: MySQL? = null

    /**
     * データベースに接続できるか確認します
     * @return [ConnectState]
     */
    fun checkConnect(): ConnectState {
        return sql.checkConnect()
    }

    /**
     * データベースにテーブルを作成します
     * @return [ConnectState]
     */
    fun createTable(): ConnectState {
        return sql?.run {
            use {
                executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS Money(UUID VARCHAR(36) PRIMARY KEY, Value INT);
                """.trimIndent()
                )
            } ?: return ConnectState.CatchException
            ConnectState.Success
        } ?: ConnectState.NullError
    }

    /**
     * プレイヤーの所持金関連
     */
    object MoneyData {
        private val moneyDataCache = mutableMapOf<UUIDPlayer, Int>()

        /**
         * プレイヤーの所持金
         */
        var OfflinePlayer.money: Int
            get() = get(UUIDPlayer(uniqueId))
            set(value) {
                set(UUIDPlayer(uniqueId), value)
            }

        /**
         * プレイヤーが指定金額所持しているか返します
         * @param money 指定金額
         * @return [Boolean]
         */
        fun OfflinePlayer.hasMoney(money: Int) = this.money <= money

        private fun set(
            uuidPlayer: UUIDPlayer,
            money: Int
        ) {
            sql?.use {
                if (money != 0) {
                    executeUpdate(
                        """
                        INSERT INTO Money VALUE ('$uuidPlayer', $money) ON DUPLICATE KEY UPDATE Money = $money;
                    """.trimIndent()
                    )
                } else {
                    executeUpdate(
                        """
                        DELETE FROM Money WHERE UUID = '$uuidPlayer' LIMIT 1;
                    """.trimIndent()
                    )
                }
            }
            moneyDataCache[uuidPlayer] = money
        }

        private fun get(uuidPlayer: UUIDPlayer): Int {
            return moneyDataCache.getOrPut(uuidPlayer) { getFromSQL(uuidPlayer) }
        }

        private fun getFromSQL(uuidPlayer: UUIDPlayer): Int {
            var money = 0
            sql?.use {
                val result = executeQuery(
                    """
                    SELECT Value FROM Money WHERE UUID = '$uuidPlayer' LIMIT 1;
                """.trimIndent()
                )
                if (result.next()) {
                    money = result.getInt(1)
                }
            }
            return money
        }

        /**
         * キャッシュの名前一覧を取得します
         * @return [List]<[String]>
         */
        fun getCacheList(): List<String> {
            return moneyDataCache.mapNotNull { it.key.name }
        }

        /**
         * 指定プレイヤーのキャッシュを削除します
         * @param uuidPlayer 指定プレイヤー
         */
        fun deleteCache(uuidPlayer: UUIDPlayer) {
            moneyDataCache.remove(uuidPlayer)
        }

        /**
         * 全プレイヤーのキャッシュを削除します
         */
        fun clearCache() {
            moneyDataCache.clear()
        }
    }

    /**
     * 所持金のランキング関連
     */
    object MoneyRank {
        /**
         * ランキングデータ
         * @param rank 順位
         * @param uuidPlayer プレイヤー
         * @param money 所持金
         */
        data class RankData(
            val rank: Int,
            val uuidPlayer: UUIDPlayer,
            val money: Int
        ) {
            /**
             * ランダムデータを文字列に変換します
             * @return [String]
             */
            override fun toString() = "&6$rank &f${uuidPlayer.name} &a${money}JPY"
        }

        private val rankDataList = mutableListOf<RankData>()

        /**
         * 読み込んだランキングの最終ページ
         */
        var lastPage = 0
            private set

        private fun load() {
            if (rankDataList.isNotEmpty()) return
            sql?.use {
                val result = executeQuery(
                    """
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
                """.trimIndent()
                )
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

        /**
         * ランキングの指定したページを取得します
         * @param page 指定ページ
         * @return [List]<[RankData]>
         */
        fun get(page: Int): List<RankData> {
            if (page < 1) return get(1)
            load()
            val begin = (page - 1) * 10
            val end = page * 10 - 1
            return rankDataList.slice(begin..end)
        }
    }
}