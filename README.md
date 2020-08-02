# aurora-serverless-kotlin-api-example
Example project using AWS Lambda powered by Kotlin to build a basic REST API with a RDS Aurora Serverless as datasource. 

## Motivation
Testing the usage of RDS Aurora Serverless in a JVM Lambda environment

## Content
* [Serverless Framework](https://www.serverless.com/) to deploy infrastructure
* RDS [Aurora Serverless](https://aws.amazon.com/rds/aurora/serverless/) Postgres 10.7
* [http4k](https://github.com/http4k/http4k) REST API written in Kotlin and deployed to AWS Lambda
* [Exposed](https://github.com/JetBrains/Exposed/) as ORM
* [HikariCP](https://github.com/brettwooldridge/HikariCP) as Connection Pool with Exposed

## Pitfalls
* DB needs to be in same VPC with application because no public accessible endpoint can be assigned
* DB needs too long to start, so the first Lambda request will always time out


## Deployment

> Requires Yarn!

```
./gradlew deploy
```
