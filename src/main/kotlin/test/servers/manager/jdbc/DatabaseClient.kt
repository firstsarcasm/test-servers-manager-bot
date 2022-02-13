package test.servers.manager.jdbc

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import test.servers.manager.utils.TimeUtils
import java.net.URI
import java.net.URISyntaxException
import java.sql.SQLException


object DatabaseClient {
    public fun initDatabase() {
        getConnection()
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Server)

            Server.insertIfNotExists("c0")
            Server.insertIfNotExists("c1")
            Server.insertIfNotExists("c4")
            Server.insertIfNotExists("c5")
        }
    }

    object Server : Table() {
        val id = varchar("id", length = 10000).primaryKey()
        val user = varchar("user", length = 100000).nullable()
        val time = varchar("time", length = 100000).nullable()
        val chatId = varchar("chatId", length = 100000).nullable()

        public fun insertIfNotExists(serverId: String) {
            transaction {
                val server = selectBy(serverId)
                if (server == null) {
                    Server.insert {
                        it[id] = serverId
                    }
                }
            }
        }

        public fun selectBy(rowId: String): String? {
            return transaction {
                Server.select { id eq rowId }.firstOrNull()?.get(id)
            }
        }

        public fun selectByChatId(cId: String): String? {
            return transaction {
                Server.select { chatId eq cId }.firstOrNull()?.get(id)
            }
        }

        public fun update(serverId: String, newUser: String, userChatId: String) {
            transaction {
                Server.update({ id eq serverId }) {
                    it[user] = newUser
                    it[chatId] = userChatId
                    it[time] = TimeUtils.getMoscowTimeNow().toString()
                }
            }
        }

        public fun releaseServers(userChatId: String) {
            transaction {
                Server.update({ chatId eq userChatId }) {
                    it[user] = null
                    it[chatId] = null
                    it[time] = null
                }
            }
        }
        public fun releaseAllServers() {
            transaction {
                Server.update {
                    it[user] = null
                    it[chatId] = null
                    it[time] = null
                }
            }
        }
    }

    @Throws(URISyntaxException::class, SQLException::class)
    private fun getConnection(): Database {
        val dbUri = URI(System.getenv("DATABASE_URL").trim())

        val username = dbUri.userInfo.split(":")[0]
        val password = dbUri.userInfo.split(":")[1]
        val dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path

        return Database.connect(dbUrl, "org.postgresql.Driver", username, password)
    }
}