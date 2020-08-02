# aurora-serverless-kotlin-api-example
Example project using AWS Lambda powered by Kotlin to build a basic REST API with a RDS Aurora Serverless as datasource. 

## Motivation
Testing the usage of RDS Aurora Serverless in a JVM Lambda environment

## Content
* Serverless Framework to deploy infrastructure
* RDS Aurora Serverless Postgres 10.7
* http4k REST API written in Kotlin
* Exposed as ORM
* HikariCP as Connection Pool with Exposed

## Deployment

> Requires Yarn!

```
./gradlew deploy
```
