TODO this branch is broken, spring-data-jpa does not support mongodb!!

An exercise in implementing the Timesheet tracker (https://github.com/ultish/jikan-ga-nai-server) using Netflix DGS
framework and Kotlin.

Graphiql: http://localhost:8080/graphiql

## Learning

### DataLoaders and Custom Contexts

A custom context is passed from parent to child(s) which contains a mutable list of Entities that are loaded within a
request. These are in-turn used to pull relationship information for Many-to-One DataLoaders, so we can perform a
relatively easy query against the DB. Each dataLoader that pulls new data from the DB for the single request will keep
adding Entities loaded into this Custom Context for the next child. (This may assume the child queries are not loaded in
parallel...)

Not sure if this code will survive a refactor to a federated graphQL service in the future though. 
