package net.wussmann.rds.serverless

import org.http4k.contract.bindContract
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.format.Jackson
import org.http4k.routing.routes
import org.http4k.serverless.AppLoader

object RestApiHandler : AppLoader {

    private val userRepository = if (System.getenv("DB_CONNECTION").equals("jdbc", true)) {
        JdbcUserRepository().also { println("Using JDBC connection") }
    } else RdsDataUserRepsitory().also { println("Using RDS Data api") }

    override fun invoke(env: Map<String, String>): HttpHandler = { request ->
        val contract = listOf(
            contract {
                renderer = OpenApi3(ApiInfo("RDS Serverless API", "v1.0.0", "API to test Serverless RDS"), Jackson)
                descriptionPath = "/swagger.json"
                this.routes += listOf(
                    "/users" bindContract GET to GetUsersRoute(userRepository),
                    "/users" bindContract POST to PostUsersRoute(userRepository)
                )
            }
        )
        routes(*contract.toTypedArray())(request)
    }
}
