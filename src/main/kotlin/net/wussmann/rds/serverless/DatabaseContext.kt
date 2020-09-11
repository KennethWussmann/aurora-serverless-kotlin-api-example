package net.wussmann.rds.serverless

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.pool.HikariPool
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.postgresql.Driver
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.time.measureTimedValue
import kotlin.time.minutes
import kotlin.time.seconds

abstract class DatabaseContext(private val connectionPooling: Boolean = true) {

    fun <T> transaction(statement: Transaction.() -> T): T {
        Database.connect({
            for (retries in 1..3) {
                try {
                    return@connect if (connectionPooling) {
                        getConnectionFromPool()
                    } else {
                        getSingleConnection()
                    }
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
            throw Exception("Failed to connect to db")
        })
        return org.jetbrains.exposed.sql.transactions.transaction {
            SchemaUtils.createMissingTablesAndColumns(Users)
            statement()
        }
    }

    private val jdbcUrl = "jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}"

    private val connectionPool by lazy {
        try {
            HikariDataSource(
                HikariConfig().apply {
                    driverClassName = Driver::class.qualifiedName
                    jdbcUrl = this@DatabaseContext.jdbcUrl
                    maximumPoolSize = 1
                    isAutoCommit = false
                    transactionIsolation = "TRANSACTION_READ_COMMITTED"
                    connectionTimeout = 2.seconds.inMicroseconds.toLong()
                    maxLifetime = 14.minutes.inMilliseconds.toLong()
                    validationTimeout = 500
                    username = System.getenv("DB_USER")
                    password = System.getenv("DB_PASSWORD")
                }
            )
        } catch (e: HikariPool.PoolInitializationException) {
            e.printStackTrace()
            throw e
        }
    }

    private fun getConnectionFromPool() = measureTimedValue {
        connectionPool.connection
    }.let { (connection, duration) ->
        println("Got connection in $duration")
        connection
    }

    private fun getSingleConnection() =
        DriverManager.getConnection(jdbcUrl, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"))
}
