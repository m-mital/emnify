# Product Offer Aggregation Service

It's a microservice that aggregates offers for a product.
An offer has two attributes a floating point "price" and a string "productCode".
A "product offer aggregation" has the following attributes: min, max and average price of the aggregated offers, count of offers.
An aggregation can be in two states, either "open" or "closed".
Only aggregations with more than N offers can be closed (where N is configurable and defaults to 3)

Acceptance criteria
1. A closed aggregation does not accept any more offers.
2. The service's API should allow:
   a. querying for an aggregation for a given product code
   b. closing the aggregation
   c. supplying offers for the aggregation
3. The offers to aggregate should be batched or streamed.

The application starts as a microservice that allows us pinging about different aggregations that are in DB.
Also, we can close the aggregation so that it can no longer accept updates.

Application is fed by Fs2 Stream from CSV file. It could also be Kafka or something else.

## Requirements
- Scala 2.13
- SBT 1.7.1
- Java 11 or higher

## How to run it:

- Go to the console and run: `sbt run`.
- Run the app and use Swagger `http://localhost:8080/docs`. You will receive an API description.
- `curl -X POST -H 'Content-Type: application/json' -d '{"productCode":"BLF01SVEU"}' localhost:8080/aggregations`
- `curl -X POST -H 'Content-Type: application/json' -d '{"productCode":"WCA301P"}' localhost:8080/aggregations/close`

## TODO:
- Fs2 Testing
- API Testing

## Problems
- A combination of `munit` testing library and conventional `hook`-style Scalatest.