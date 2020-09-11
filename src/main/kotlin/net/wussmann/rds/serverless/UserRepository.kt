package net.wussmann.rds.serverless

import com.amazonaws.services.rdsdata.AWSRDSDataClient
import com.amazonaws.services.rdsdata.model.ExecuteStatementRequest
import com.amazonaws.services.rdsdata.model.Field
import com.amazonaws.services.rdsdata.model.SqlParameter
import net.wussmann.rds.serverless.CloudWatchService.TransactionType.READ
import net.wussmann.rds.serverless.CloudWatchService.TransactionType.WRITE
import java.util.UUID

interface UserRepository {

    fun createUser(username: String): UserDto

    fun getAllUsers(): ListDto<UserDto>
}

class JdbcUserRepository(
    private val cloudWatchService: CloudWatchService
) : UserRepository, DatabaseContext(connectionPooling = false) {

    override fun createUser(username: String) = cloudWatchService.measureTransaction(WRITE) {
        transaction {
            User.new {
                this.username = username
            }.toDto()
        }
    }

    override fun getAllUsers() = cloudWatchService.measureTransaction(READ) {
        transaction {
            User.all().limit(1000).toDto()
        }
    }
}

class RdsDataUserRepository(
    private val cloudWatchService: CloudWatchService
) : UserRepository {

    private val rds = AWSRDSDataClient.builder().build()
    private val baseRequest = ExecuteStatementRequest()
        .withDatabase(System.getenv("DB_NAME"))
        .withSecretArn(System.getenv("DB_SECRET"))
        .withResourceArn(System.getenv("DB_CLUSTER"))

    override fun createUser(username: String): UserDto = cloudWatchService.measureTransaction(WRITE) {
        UUID.randomUUID().toString().let { id ->
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
    }


    override fun getAllUsers() =
        cloudWatchService.measureTransaction(READ) {
            rds.executeStatement(
                baseRequest
                    .clone()
                    .withSql("SELECT * FROM users LIMIT 1000;")
            ).records.map {
                UserDto(
                    id = it[0].stringValue,
                    username = it[1].stringValue
                )
            }.toDto()
        }
}
