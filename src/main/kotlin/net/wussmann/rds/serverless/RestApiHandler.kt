package net.wussmann.rds.serverless

import org.http4k.contract.bindContract
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.format.Jackson
import org.http4k.routing.routes
import org.http4k.serverless.AppLoader

object RestApiHandler : AppLoader {

    override fun invoke(env: Map<String, String>): HttpHandler = { request ->
        val connectionType = getConnectionType(request)
        val userRepository = getUserRepository(connectionType)
        val connectionTypeInjector = connectionTypeInjector(connectionType)

        val contract = listOf(
            contract {
                renderer = OpenApi3(ApiInfo("RDS Serverless API", "v1.0.0", "API to test Serverless RDS"), Jackson)
                descriptionPath = "/swagger.json"
                this.routes += listOf(
                    "/users" bindContract GET to connectionTypeInjector.then(GetUsersRoute(userRepository)),
                    "/users" bindContract POST to connectionTypeInjector.then(PostUsersRoute(userRepository))
                )
            }
        )
        routes(*contract.toTypedArray())(request)
    }

    private fun connectionTypeInjector(connectionType: CloudWatchService.ConnectionType) = Filter { next ->
        { request ->
            next(request).header("Connection-Type", connectionType.name)
        }
    }

    private fun getConnectionType(request: Request) =
        (request.header("DB-Connection") ?: System.getenv("DB_CONNECTION"))
        ?.let { CloudWatchService.ConnectionType.valueOf(it.toUpperCase()) }
        ?: CloudWatchService.ConnectionType.values().random()

    private fun getUserRepository(connectionType: CloudWatchService.ConnectionType): UserRepository {
        val cloudWatchService = CloudWatchService(connectionType)
        println("Using ${connectionType.name} connection")
        return when (connectionType) {
            CloudWatchService.ConnectionType.DATA -> RdsDataUserRepsitory(cloudWatchService)
            CloudWatchService.ConnectionType.JDBC -> JdbcUserRepository(cloudWatchService)
        }
    }
}
