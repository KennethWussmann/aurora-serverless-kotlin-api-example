package net.wussmann.rds.serverless

import com.amazonaws.services.rdsdata.AWSRDSDataClient
import com.amazonaws.services.rdsdata.model.ExecuteStatementRequest
import com.amazonaws.services.rdsdata.model.Field
import com.amazonaws.services.rdsdata.model.SqlParameter
import java.util.UUID

interface UserRepository {

    fun createUser(username: String): UserDto

    fun getAllUsers(): ListDto<UserDto>
}

class JdbcUserRepository : UserRepository, DatabaseContext() {

    override fun createUser(username: String) = transaction {
        User.new {
            this.username = username
        }.toDto()
    }

    override fun getAllUsers() = transaction {
        User.all().toDto()
    }
}

class RdsDataUserRepsitory : UserRepository {

    private val rds = AWSRDSDataClient.builder().build()
    private val baseRequest = ExecuteStatementRequest()
        .withDatabase(System.getenv("DB_NAME"))
        .withSecretArn(System.getenv("DB_SECRET"))
        .withResourceArn(System.getenv("DB_CLUSTER"))

    override fun createUser(username: String): UserDto = UUID.randomUUID().toString().let { id ->
        rds.executeStatement(
                baseRequest
                    .clone()
                    .withSql("INSERT INTO users(id, username) VALUES ('$id', :username)")
                    .withParameters(
                        SqlParameter().withName("username").withValue(Field().withStringValue(username))
                    )
            )
            .takeIf { it.numberOfRecordsUpdated == 1L }
            ?.let {
                UserDto(
                    id = id,
                    username = username
                )
            } ?: error("Failed to create user")
    }


    override fun getAllUsers() =
        rds.executeStatement(
            baseRequest
                .clone()
                .withSql("SELECT * FROM users")
        ).records.map {
            UserDto(
                id = it[0].stringValue,
                username = it[1].stringValue
            )
        }.toDto()
}
