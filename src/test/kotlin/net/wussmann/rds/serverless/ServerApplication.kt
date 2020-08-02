package net.wussmann.rds.serverless

import org.http4k.server.Undertow
import org.http4k.server.asServer

fun main() {
    RestApiHandler(emptyMap()).asServer(Undertow(8080)).start()
}
