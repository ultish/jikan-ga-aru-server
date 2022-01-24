package com.ultish.jikangaaruserver.trackedDays

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.DayMode
import com.ultish.generated.types.TrackedDay
import com.ultish.generated.types.TrackedTask
import com.ultish.generated.types.User
import com.ultish.jikangaaruserver.contexts.CustomContext
import com.ultish.jikangaaruserver.dataFetchers.*
import com.ultish.jikangaaruserver.entities.ETrackedDay
import com.ultish.jikangaaruserver.entities.QETrackedDay
import com.ultish.jikangaaruserver.entities.QETrackedTask
import com.ultish.jikangaaruserver.trackedTasks.TrackedTaskRepository
import com.ultish.jikangaaruserver.users.UserRepository
import graphql.relay.Connection
import graphql.schema.DataFetchingEnvironment
import org.dataloader.MappedBatchLoaderWithContext
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import java.util.concurrent.CompletableFuture

@DgsComponent
class TrackedDayService {

   private companion object {
      const val DATA_LOADER_FOR_TRACKED_TASKS = "trackedTasksForTrackedDay"
      const val DATA_LOADER_FOR_USERS = "usersForTrackedDay"
   }

   @Autowired
   lateinit var repository: TrackedDayRepository

   @Autowired
   lateinit var trackedTaskRepository: TrackedTaskRepository

   @Autowired
   lateinit var userRepository: UserRepository

   @DgsQuery
   fun trackedDays(dfe: DataFetchingEnvironment): List<TrackedDay> {
      return dgsQuery(dfe) {
         repository.findAll()
      }
   }

   @DgsQuery
   fun trackedDaysPaginated(
      dfe: DataFetchingEnvironment,
      @InputArgument after: String?,
      @InputArgument first: Int?,
   ): Connection<TrackedDay> {
      return fetchPaginated(
         dfe,
         repository,
         QETrackedDay.eTrackedDay.date.toString(),
         after,
         first)
   }

   @DgsMutation
   fun createTrackedDay(
      dfe: DataFetchingEnvironment,
      @InputArgument userId: String,
      @InputArgument date: Double, // not confusing at all, graphql's Float is passed in as a Double
      @InputArgument mode: DayMode?,
   ): TrackedDay {
      if (!userRepository.existsById(userId)) {
         throw DgsInvalidInputArgumentException("Couldn't find User[${userId}]")
      }

      // make sure we can't re-create a TrackedDay for a user with an existing date
      if (repository.exists(BooleanBuilder()
            .and(QETrackedDay.eTrackedDay.userId.eq(userId))
            .and(QETrackedDay.eTrackedDay.date.eq(Date(date.toLong()))))
      ) {
         throw DgsInvalidInputArgumentException("Date[${Date(date.toLong())} already exists")
      }

      val d = Date(date.toLong())
      val cal = Calendar.getInstance()
      cal.time = d
      val week = cal.get(Calendar.WEEK_OF_YEAR)

      return dgsMutate(dfe) {
         repository.save(ETrackedDay(
            date = d,
            week = week,
            mode = mode ?: DayMode.NORMAL,
            userId = userId,
         ))
      }
   }

   @DgsMutation
   fun deleteTrackedDay(@InputArgument id: String): Boolean {
      return delete(repository, QETrackedDay.eTrackedDay.id, id)
   }

   @DgsMutation
   fun updateTrackedDay(
      dfe: DataFetchingEnvironment,
      @InputArgument id: String,
      @InputArgument mode: DayMode? = null,
      @InputArgument date: Double? = null,
      @InputArgument trackedTaskIds: List<String>? = null,
   ): TrackedDay {
      val record = repository.findById(id)
         .map { it }
         .orElseThrow {
            DgsInvalidInputArgumentException("Couldn't find TrackedDay[${id}]")
         }

      return dgsMutate(dfe) {
         updateTrackedDay(record, mode, date, trackedTaskIds)
      }
   }

   fun updateTrackedDay(
      trackedDay: ETrackedDay,
      mode: DayMode? = null,
      date: Double? = null,
      trackedTaskIds: List<String>? = null,
   ): ETrackedDay {

      val copy = trackedDay.copy(
         mode = mode ?: trackedDay.mode,
         date = if (date != null) Date(date.toLong()) else trackedDay.date,
         trackedTaskIds = trackedTaskIds ?: trackedDay.trackedTaskIds
      )
      return repository.save(copy)
   }

   //
   // Document References (relationships)
   // -------------------------------------------------------------------------
   /**
    * This Data Fetcher is for the TrackedDay type's User field. When graphQl requests the
    * User field on a TrackedDay type we'll pass it onto the "users" DataLoader in order
    * to batch the fetches
    */
   @DgsData(parentType = DgsConstants.TRACKEDDAY.TYPE_NAME,
      field = DgsConstants.TRACKEDDAY.User)
   fun relatedUsers(dfe: DataFetchingEnvironment): CompletableFuture<User> {
      return dgsData<User, TrackedDay/*, ETrackedDay*/>(dfe,
         DATA_LOADER_FOR_USERS) { trackedDay ->
         trackedDay.id
      }
   }

   @DgsData(parentType = DgsConstants.TRACKEDDAY.TYPE_NAME,
      field = DgsConstants.TRACKEDDAY.TrackedTasks)
   fun relatedTrackedTasks(dfe: DataFetchingEnvironment): CompletableFuture<List<TrackedTask>> {

//      val dataLoader = dfe.getDataLoader<String, List<TrackedTask>>(DATA_LOADER_FOR_TRACKED_TASKS)
//      val source = dfe.getSource<TrackedDay>()
////      dfe.graphQlContext.get<List<ETrackedDay>>("ETRACKEDDAY")
//
//      return dataLoader.load(source.id)

      return dgsData<List<TrackedTask>, TrackedDay>(dfe,
         DATA_LOADER_FOR_TRACKED_TASKS) { trackedDay ->
         trackedDay.id
      }
   }

   //
   // Data Loaders. These functions will batch request other entities, if these
   // entities were deployed on different servers, we'd be fetching them
   // remotely from here
   // -------------------------------------------------------------------------
   /**
    * TODO unsure if use of DgsDataLoaders is useful in this small app vs just using Mongo/Spring
    *  to eagerly load all relationships every time. Probably no noticeable performance for this
    *  application. But a good study exercise.
    */
   /**
    * This data-loader will batch load User objects from a list of trackedDay IDs. We need to use
    * MappedBatchLoader as not every user may have a tracked day
    */
   @DgsDataLoader(name = DATA_LOADER_FOR_USERS, caching = true)
   val userBatchLoader = MappedBatchLoaderWithContext<String, User?> { trackedDayIds, env ->
      CompletableFuture.supplyAsync {

         // Relationship: Many to One

         val customContext = DgsContext.getCustomContext<CustomContext>(env)

         val trackedDayToUserIdMap = getEntitiesFromEnv<String, ETrackedDay>(env, trackedDayIds) {
            it.userId
         }
         val userMap = userRepository.findAllById(
            trackedDayToUserIdMap.values.toList())
            .associateBy { it.id }

         // TODO make this common somehow, probably by moving everything into the common supplier and have it return
         //  Pair<Collection<E>, R>
         // pass down to next level if needed
         customContext.entities.addAll(userMap.values)

         val result =
            trackedDayToUserIdMap.keys.associateWith { trackedDayId ->
               val user =
                  trackedDayToUserIdMap[trackedDayId]?.let { userMap[it] }
               user?.toGqlType()
            }

         // LEARN: @ is a label marker and @supplyAsync is an implicit label that has the same
         //  name as the function to which the lambda is passed. We can omit the return statement
         //  altogether as well and simply have 'assocateBy', or go further and remove the
         //  associateBy val
         return@supplyAsync result
      }
   }

   @DgsDataLoader(name = DATA_LOADER_FOR_TRACKED_TASKS, caching = true)
   val loadForTrackedDayBatchLoader =
      MappedBatchLoaderWithContext<String, List<TrackedTask>> { trackedDayIds, environment ->
         CompletableFuture.supplyAsync {
            // Relationship: One to Many

            val customContext = DgsContext.getCustomContext<CustomContext>(environment)

            // For One-To-Many relationships it's quicker to just look up the One side and groupBy at the end
            // instead
            // of using the context object
            val trackedTasks = trackedTaskRepository
               .findAll(QETrackedTask.eTrackedTask.trackedDayId
                  .`in`(trackedDayIds)

               )

            customContext.entities.addAll(trackedTasks)

            trackedTasks.groupBy({ it.trackedDayId }, { it.toGqlType() })
         }
      }

}