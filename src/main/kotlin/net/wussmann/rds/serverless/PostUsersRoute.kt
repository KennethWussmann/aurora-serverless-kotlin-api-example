package net.wussmann.rds.serverless

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.with
import org.http4k.format.Jackson.auto

class PostUsersRoute : HttpHandler, DatabaseContext() {

    private val responseLens = Body
        .auto<UserDto>("Newly created user")
        .toLens()

    private val requestBodyLens = Body
        .auto<CreateUserRequest>("Request body to create a new user")
        .toLens()

    private data class CreateUserRequest(
        val username: String
    )

    override fun invoke(request: Request) =
        transaction {
            Response(CREATED)
                .with(responseLens of User.new {
                    username = requestBodyLens(request).username
                }.toDto())
        }
}
