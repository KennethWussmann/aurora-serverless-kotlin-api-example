# aurora-serverless-kotlin-api-example
Example project using AWS Lambda powered by Kotlin to build a basic REST API with a RDS Aurora Serverless as datasource. 

## Motivation
Testing the usage of RDS Aurora Serverless in a JVM Lambda environment and comparing JDBC vs. RDS Data API usage.

## Content
* [Serverless Framework](https://www.serverless.com/) to deploy infrastructure
    * CloudWatch Dashboard with custom transaction metrics to compare JDBC and RDS Data API
* RDS [Aurora Serverless](https://aws.amazon.com/rds/aurora/serverless/) Postgres 10.7
* [http4k](https://github.com/http4k/http4k) REST API written in Kotlin and deployed to AWS Lambda

### JDBC
The API can either connect via JDBC using:
* [Exposed](https://github.com/JetBrains/Exposed/) as ORM
* [HikariCP](https://github.com/brettwooldridge/HikariCP) as Connection Pool with Exposed

### RDS Data API
Or using the [RDS Data API](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/data-api.html#data-api.java-client-library) that handles connection pooling at AWS.

### Switch connection
To switch between JDBC and RDS Data API set the `DB_CONNECTION` environment variable either to `JDBC` or `DATA`.
You can also specify the `DB-Connection` header with either `JDBC` or `DATA` with your request. Notice that warm lambdas might still have connections open which could influence the metrics.
 
## Pitfalls
* DB needs to be in same VPC with application because no public accessible endpoint can be assigned.
* DB needs too long to start, so the first Lambda request will always time out.
* RDS Data API requires SecretsManager.
* RDS Data API doesn't support prepared statements with special column types like `UUID` in Postgres. You need to use unsafe way of inserting raw values into queries
* [rds-data-api-client-library-java](https://github.com/awslabs/rds-data-api-client-library-java) offering mapping but not compatible with Kotlin due to inaccessible fields and only compatible with AWS SDK v1.

## Learnings
* Connection pooling in Lambda can lead to issues with max. Database Connections and useless scaling.
    * In Lambda pooling will still establish once connection per cold lambda. The RDS Data API can handle that smarter.
    * When there are a lot of connections, RDS scales up to the configured max. 16 ACUs.
    * Even though the transactions are really simple, and the database could handle them easily on 2 ACUs.
    * Tests proven, that with RDS Data API this issue does not occur. RDS scales down to 2 ACUs in the same test.
* Shooting the same amount of requests on JDBC and Data API: JDBC reaches the connection limits of 16 ACUs and starts to complain that the max. connections reached. RDS Data API was still at 2 ACUs with pretty good in response times.
    
## Deployment

> Requires Yarn!

```
./gradlew deploy
```
