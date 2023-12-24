package com.ultish.jikangaaruserver.dataFetchers

import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.exceptions.DgsBadRequestException
import com.ultish.jikangaaruserver.contexts.CustomContext
import com.ultish.jikangaaruserver.entities.GraphQLEntity
import graphql.relay.*
import graphql.schema.DataFetchingEnvironment
import org.dataloader.BatchLoaderEnvironment
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.mongodb.repository.MongoRepository
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

//const val DGS_CONTEXT_DATA = "dgsContextData"

/**
 * An empty Specification so we can build upon it
 */
inline fun <reified T> emptySpecification(): Specification<T> = Specification { _, _, _ -> null }

inline fun <reified T> specById(id: String, idAttr: String = "id"): Specification<T> =
   Specification { root, _, builder ->
      builder.equal(root.get<String>(idAttr), id)
   }

inline fun <reified T> specEquals(attr: String, value: String): Specification<T> = Specification { root, _, builder ->
   builder.equal(root.get<String>(attr), value)
}

inline fun <reified T, R> specEquals(attr: String, value: R): Specification<T> = Specification { root, _, builder ->
   builder.equal(root.get<R>(attr), value)
}

/**
 * A common Specification for a list of IDS, will split the list into chunks
 */
inline fun <reified T> specByIds(ids: Collection<String>): Specification<T> = specInStrings("id", ids)

inline fun <reified T> specInStrings(attr: String, strings: Collection<String>): Specification<T> =
   Specification { root, _, builder ->
      val predicates = strings.chunked(10).map { chunk ->
         root.get<String>(attr).`in`(chunk)
//         val exp = builder.`in`(root.get<String>(attr))
//         chunk.forEach { exp.value(it) }
//         exp
      }
      builder.or(*predicates.toTypedArray())
   }

inline fun <reified T, R> specIn(attr: String, objs: Collection<R>): Specification<T> =
   Specification { root, _, builder ->
      val predicates = objs.chunked(10).map { chunk ->
         root.get<R>(attr).`in`(chunk)
      }
      builder.or(*predicates.toTypedArray())
   }

/**
 * Utility to delete an Entity via a String-based key. (eg id, name etc).
 * Will only match the key exactly.
 *
 * @param repository the repository instance to use to look up and delete the Entity
 * @param keyPath the path of the key within the Entity
 * @param key the value to look for
 * @return successfully deleted the Entity or not
 */
inline fun <G, reified E : GraphQLEntity<G>, R> delete(
   repository: R, key: String, keyPath: String = "id"
): Boolean
   where R : JpaSpecificationExecutor<E>,
         R : MongoRepository<E, String> {

   val toDelete = repository.findOne(specById(key, keyPath))
   return if (toDelete.isPresent) {
      repository.delete(toDelete.get())
      true
   } else false
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
   dfe: DataFetchingEnvironment,
   repository: R,
   sortKey: String,
   sortDirection: Direction = Direction.ASC,
   after: String? = null,
   first: Int? = null,
   specification: Specification<E>? = null,
): Connection<G>
   where R : JpaSpecificationExecutor<E>,
         R : MongoRepository<E, String> {

   val (pagable, pageNumber, itemsPerPage) = createPageable(sortKey, sortDirection, after, first)

   val page = if (specification != null) {
      repository.findAll(specification, pagable)
   } else {
      repository.findAll(pagable)
   }

   // load the entity data into the DGS context for later use if necessary (eg dataLoaders)
//   dfe.graphQlContext.put(DGS_CONTEXT_DATA, page.content)
   val customContext = DgsContext.getCustomContext<CustomContext>(dfe)
   customContext.entities.addAll(page.content)

   val edges = page.mapIndexed { index, it ->
      val gqlType = it.toGqlType()
      val cursor = createCursor(index, pageNumber, itemsPerPage)
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

fun createPageable(
   sortKey: String,
   sortDirection: Sort.Direction = Sort.Direction.ASC,
   after: String? = null,
   first: Int? = null,
): Triple<Pageable, Int, Int> {
   val index = after?.let { decode(it) } ?: -1
   val itemsPerPage = first ?: 10;
   val pageNumber = (index + 1) / itemsPerPage;

   val sorter = Sort.by(sortDirection, sortKey);

   return Triple(
      PageRequest.of(pageNumber, itemsPerPage, sorter),
      pageNumber,
      itemsPerPage
   )
}

fun getUser(dfe: DataFetchingEnvironment): String {
   val request = DgsContext.getRequestData(dfe)
   return request?.headers?.getFirst("user-id") ?: throw DgsBadRequestException("user-id not provided in headers")
}

fun <G, E : GraphQLEntity<G>> dgsQuery(
   dfe: DataFetchingEnvironment,
   entities: () -> Iterable<E>,
): List<G> {

   // LEARN: can fetch request headers like this. Can use it to request the x-token for user auth
   val request = DgsContext.getRequestData(dfe)
   val userId = request?.headers?.getFirst("user-id")
   println("request from ${userId}")

   val entitiesToAdd = entities()
   // push the entities into the graphql context
   val customContext = DgsContext.getCustomContext<CustomContext>(dfe)
   customContext.entities.addAll(entitiesToAdd)

   return entitiesToAdd.map { it.toGqlType() }
}

fun <G, E : GraphQLEntity<G>> dgsMutate(dfe: DataFetchingEnvironment, entity: () -> E): G {
   val request = DgsContext.getRequestData(dfe)
   val userId = request?.headers?.getFirst("user-id")
   println("create request from ${userId}")

   val e = entity()

   val customContext = DgsContext.getCustomContext<CustomContext>(dfe)
   customContext.entities.add(e)

   return e.toGqlType()
}

/**
 * Helper function for @DgsData annotated document reference functions (functions that will use DgsDataLoaders)
 */
fun <R, G/*, E : GraphQLEntity<G>*/> dgsData(
   dfe: DataFetchingEnvironment,
   dataLoaderKey: String,
   keySupplier: (g: G) -> String,
): CompletableFuture<R> {
   val dataLoader = dfe.getDataLoader<String, R>(dataLoaderKey)
   val graphQLType = dfe.getSource<G>()
   val graphQLTypeKey = keySupplier(graphQLType)

//   val contextData = dfe.graphQlContext.get<List<E>>(DGS_CONTEXT_DATA)

   return dataLoader.load(graphQLTypeKey)
}

/**
 * Helper function to pull out Map from DSG context
 */
inline fun <V, reified E : GraphQLEntity<*>> contextToMap(
   context: BatchLoaderEnvironment,
   getValue: (entity: E?) -> V?,
): Map<String, V?> {
   return context.keyContexts.entries.associate { entry ->
      // LEARN: as? is kotlin's safe-cast operator which returns null instead of ClassCastException
      val value = if (entry.value is E) getValue(entry.value as E) else null
      Pair(entry.key.toString(), value)
   }
}

//fun <R, E : GraphQLEntity<*>> dgsDataLoader(
//   environment: BatchLoaderEnvironment,
//   ids: Collection<String>,
//   getEntities: (forIds: Collection<String>) -> Collection<E>,
//   getResult: (entities: Collection<E>) -> R,
//): R {
//   val entities = getEntities(ids);
//
//   val customContext = DgsContext.getCustomContext<CustomContext>(environment)
//
//   // pass down to next level if needed
//   customContext.entities.addAll(entities)
//
//   return getResult(entities)
//}

inline fun <V, reified E : GraphQLEntity<*>> getEntitiesFromEnv(
   environment: BatchLoaderEnvironment,
   ids: Collection<String>,
   getValue: (it: E) -> V,
): Map<String, V> {
   val customContext = DgsContext.getCustomContext<CustomContext>(environment)
   return customContext.entities.mapNotNull {
      if (it is E && ids.contains(it.id())) {
         it
      } else {
         null
      }
   }.associateBy({ it.id() }, { getValue(it) })

}

fun <G, E : GraphQLEntity<G>, R> future(repository: R, specification: Specification<E>)
   : CompletionStage<List<G>>
   where R : JpaSpecificationExecutor<E>,
         R : MongoRepository<E, String> {
   return CompletableFuture.supplyAsync {
      repository.findAll(specification)
         .map {
            it.toGqlType()
         }
   }
}