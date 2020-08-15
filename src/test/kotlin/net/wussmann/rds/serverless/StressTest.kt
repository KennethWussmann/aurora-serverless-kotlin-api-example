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
    basePath = "https://cg64bs3pbe.execute-api.eu-central-1.amazonaws.com/dev"
}

private var requests = 0

/**
 * Stess test to run multiple read & write operations at the same time
 */
fun main() {
    runBlocking {
        while (true) {
            if (requests >= 5000) break
            launch {
                if (Random.nextBoolean()) {
                    readOperation()
                } else {
                    writeOperation()
                }
            }
            requests++
            println("Requests $requests")
        }
    }
}

private suspend fun readOperation() {
    measureTimedValue {
        try {
            fuel.get("/users").awaitStringResponse()
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
            fuel.post("/users").body(
                """
                {"username": "${UUID.randomUUID().toString().substring(0, 6)}"}
            """.trimIndent()
            ).awaitStringResponse()
        } catch (e: Exception) {
            println("Error ${e.message}")
            null
        }
    }.let { (res, duration) ->
        println("Write: $duration, ${res?.second?.header("Connection-Type")?.firstOrNull()}, ${res?.second?.body()?.asString("application/json")?.substring(0..7)}...")
    }
}
