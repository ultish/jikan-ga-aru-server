package com.ultish.jikangaaruserver.dataFetchers

import com.querydsl.core.types.dsl.StringPath
import com.ultish.jikangaaruserver.entities.GraphQLEntity
import graphql.relay.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Utility to delete an Entity via a String-based key. (eg id, name etc).
 * Will only match the key exactly.
 *
 * @param repository the repository instance to use to look up and delete the Entity
 * @param keyPath the path of the key within the Entity
 * @param key the value to look for
 * @return successfully deleted the Entity or not
 */
fun <G, E : GraphQLEntity<G>, R> delete(
   repository: R, keyPath: StringPath, key: String,
): Boolean
   where R : QuerydslPredicateExecutor<E>,
         R : MongoRepository<E, String> {
   val toDelete = repository.findOne(keyPath.eq(key))
   if (toDelete.isPresent) {
      repository.delete(toDelete.get())
      return true
   }
   return false
}

fun decode(cursor: String): Int {
   return Integer.parseInt(String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8))
}

fun createCursor(index: Int, pageNumber: Int, size: Int): String {
   val realIndex = pageNumber * size + index
   return Base64.getEncoder().encodeToString(realIndex.toString().toByteArray(StandardCharsets.UTF_8))
}

/**
 * Fetch a paginated set from the Repository, supports an [after] cursor
 * and the [first] X number of records from it. Will default to 10 if none
 * is specified
 *
 * TODO support before cursor and last X values. See graphql.relay.SimpleListConnection for an example
 *
 * @param repository The Repository to fetch from
 * @param after An optional starting cursor
 * @param first An optional number of values to return, defaults to 10
 * @param sortKey The attribute to sort the list of values in the repository, required for pagination to work
 * consistently
 */
fun <G, E : GraphQLEntity<G>, R> fetchPaginated(
   repository: R,
   after: String?,
   first: Int?,
   sortKey: String,
): Connection<G>
   where R : QuerydslPredicateExecutor<E>,
         R : MongoRepository<E, String> {
   val index = after?.let { decode(it) } ?: -1
   val size = first ?: 10;
   val pageNumber = (index + 1) / size;

   val pagable: Pageable = PageRequest.of(pageNumber, size, Sort.by(sortKey))
   val page = repository.findAll(pagable)

   val edges = page.mapIndexed { index, it ->
      val gqlType = it.toGqlType()
      val cursor = createCursor(index, pageNumber, size)
      DefaultEdge(gqlType, DefaultConnectionCursor(cursor))
   }
   val pageInfo = DefaultPageInfo(
      edges.first().cursor,
      edges.last().cursor,
      page.hasPrevious(),
      page.hasNext()
   )

   return DefaultConnection(edges, pageInfo)
}