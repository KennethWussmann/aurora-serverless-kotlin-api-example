package net.wussmann.rds.serverless

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.awaitResponse
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Exception
import java.util.UUID
import kotlin.random.Random
import kotlin.time.measureTimedValue

private val fuel = FuelManager().apply {
    basePath = "<URL GOES HERE>"
}

private val connectionType = CloudWatchService.ConnectionType.DATA
private val slowMode = true // connectionType == CloudWatchService.ConnectionType.JDBC
private val maxRequests = 100_000

private var requests = 0

/**
 * Stress test to run multiple read & write operations at the same time
 */
fun main() {
    runBlocking {
        while (true) {
            if (requests >= maxRequests) break
            launch {
                if (Random.nextBoolean()) {
                    readOperation()
                } else {
                    writeOperation()
                }
            }
            if (slowMode) {
                delay(10)
            }
            requests++
            println("Requests $requests")
        }
    }
}

private suspend fun readOperation() {
    measureTimedValue {
        try {
            fuel.get("/users")
                .header("DB-Connection", connectionType.name)
                .awaitStringResponse()
        } catch (e: Exception) {
            println("Error ${e.message}")
            null
        }
    }.let { (res, duration) ->
        println("Read: $duration, ${res?.second?.header("Connection-Type")?.firstOrNull()}, ${res?.second?.body()?.asString("application/json")?.substring(0..7)}...")
    }
}

private suspend fun writeOperation() {
    measureTimedValue {
        try {
            fuel.post("/users")
                .header("DB-Connection", connectionType.name)
                .body(
                    """
                        {"username": "${UUID.randomUUID().toString().substring(0, 6)}"}
                    """.trimIndent()
                )
                .awaitStringResponse()
        } catch (e: Exception) {
            println("Error ${e.message}")
            null
        }
    }.let { (res, duration) ->
        println("Write: $duration, ${res?.second?.header("Connection-Type")?.firstOrNull()}, ${res?.second?.body()?.asString("application/json")?.substring(0..7)}...")
    }
}
