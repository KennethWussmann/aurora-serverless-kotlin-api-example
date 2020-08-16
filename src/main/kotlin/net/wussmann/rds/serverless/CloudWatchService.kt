package net.wussmann.rds.serverless

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.Dimension
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * Service to publish own metrics about transactions
 */
class CloudWatchService(
    private val connectionType: ConnectionType,
    private val cloudWatchClient: CloudWatchClient = CloudWatchClient.create()
) {

    enum class ConnectionType(val displayName: String) {
        JDBC("Jdbc"), DATA("Data")
    }

    enum class TransactionType(val displayName: String) {
        READ("Read"), WRITE("Write")
    }

    private fun writeTransactionDuration(transactionType: TransactionType, duration: Duration) {
        cloudWatchClient.putMetricData(
            PutMetricDataRequest.builder()
                .namespace("AuroraServerlessTest")
                .metricData(
                    MetricDatum.builder()
                        .metricName("Transaction${connectionType.displayName}${transactionType.displayName}")
                        .timestamp(Instant.now())
                        .value(duration.inMilliseconds)
                        .dimensions(
                            Dimension.builder()
                                .name("connectionType")
                                .value(connectionType.name)
                                .build(),
                            Dimension.builder()
                                .name("transactionType")
                                .value(transactionType.name)
                                .build()
                        )
                        .build()
                )
                .build()
        )
    }

    fun <T> measureTransaction(transactionType: TransactionType, fn: () -> T): T {
        println("Start ${transactionType.name} transaction")
        return measureTimedValue(fn)
            .let { (any, duration) ->
                println("${transactionType.name} transaction finished in $duration")
                writeTransactionDuration(transactionType, duration)
                any
            }
    }
}
