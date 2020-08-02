package net.wussmann.rds.serverless

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.jetbrains.exposed.sql.transactions.transaction

class GetUsersRoute : HttpHandler, DatabaseContext() {

    private val responseLens = Body
        .auto<List<User>>("List of all existing users")
        .toLens()

    override fun invoke(request: Request) =
        transaction {
            Response(OK)
                .with(responseLens of User.all().toList())
        }
}