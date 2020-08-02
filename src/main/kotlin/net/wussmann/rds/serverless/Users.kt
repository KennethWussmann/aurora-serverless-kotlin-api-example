package net.wussmann.rds.serverless

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SizedIterable
import java.util.UUID

object Users : UUIDTable(name = "users") {
    val username = text("username")
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var username by Users.username

    fun toDto() = UserDto(
        id = id.value.toString(),
        username = username
    )
}

fun <T> List<T>.toDto() = ListDto(
    total = size.toLong(),
    items = this
)

fun SizedIterable<User>.toDto() = ListDto(
    total = count(),
    items = map { it.toDto() }
)

data class ListDto<T>(
    val total: Long = 0L,
    val items: List<T> = emptyList()
)

data class UserDto(
    val id: String? = null,
    val username: String? = null
)
