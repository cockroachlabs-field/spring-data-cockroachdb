# Spring Data CockroachDB Examples

Example projects for Spring Data CockroachDB to showcase how to use 
the features provided by the Spring Data modules.

## Modules

* jdbc-basics - Basic example using JDBC repositories
* jpa-basics - Basic example using JPA repositories

## Run Integration Tests

The integration tests will run through a series of contended workloads to exercise the
retry mechanism and other JDBC driver features.

First start a [local](https://www.cockroachlabs.com/docs/stable/start-a-local-cluster.html) CockroachDB node or cluster.

Create the database:

```bash
cockroach sql --insecure --host=localhost -e "CREATE database spring_data"
```

Then activate the integration test Maven profile:

```bash
./mvnw -P it clean install
```
