package net.wussmann.rds.serverless

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.pool.HikariPool
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.postgresql.Driver
import java.lang.Exception
import java.sql.SQLException
import kotlin.time.minutes
import kotlin.time.seconds

abstract class DatabaseContext {

    init {
        Database.connect({
            for (retries in 1..3) {
                try {
                    return@connect connectionPool.connection
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
            throw Exception("Failed to connect to db")
        })
        SchemaUtils.createMissingTablesAndColumns()
    }

    private val jdbcUrl = "jdbc:postgresql://${System.getenv("DB_HOST")}:${System.getenv("DB_PORT")}/${System.getenv("DB_NAME")}?ssl=true&sslmode=verify-full&sslrootcert=${DatabaseContext::class.java.classLoader.getResource("rds-combined-ca-bundle")!!.path}"

    private val connectionPool by lazy {
        try {
            HikariDataSource(
                HikariConfig().apply {
                    driverClassName = Driver::class.qualifiedName
                    jdbcUrl = this@DatabaseContext.jdbcUrl
                    maximumPoolSize = 2
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
}
