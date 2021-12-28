package com.ultish.jikangaaruserver.entities

/**
 * An interface to convert a Mongo entity to it's GraphQL type. Any entity
 * exposed to GraphQL schema should extend this interface
 */
fun interface GraphQLEntity<T> {
   fun toGqlType(): T
}