package com.ultish.jikangaaruserver.trackedDays

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.DayMode
import com.ultish.generated.types.TrackedDay
import com.ultish.generated.types.TrackedTask
import com.ultish.generated.types.User
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.dataFetchers.fetchPaginated
import com.ultish.jikangaaruserver.entities.ETrackedDay
import com.ultish.jikangaaruserver.entities.QETrackedDay
import com.ultish.jikangaaruserver.entities.QETrackedTask
import com.ultish.jikangaaruserver.entities.QEUser
import com.ultish.jikangaaruserver.trackedTasks.TrackedTaskRepository
import com.ultish.jikangaaruserver.users.UserRepository
import graphql.relay.Connection
import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoader
import org.dataloader.MappedBatchLoader
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
   fun trackedDays(): List<TrackedDay> {
      val trackedDays = repository.findAll()
      return trackedDays.map { it.toGqlType() }
   }

   @DgsQuery
   fun trackedDaysPaginated(
      @InputArgument after: String?,
      @InputArgument first: Int?,
   ): Connection<TrackedDay> {
      return fetchPaginated(
         repository,
         QETrackedDay.eTrackedDay.date.toString(),
         after,
         first)
   }

   @DgsMutation
   fun createTrackedDay(
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

      return repository.save(ETrackedDay(
         date = Date(date.toLong()),
         mode = mode ?: DayMode.NORMAL,
         userId = userId,
      )).toGqlType()
   }

   @DgsMutation
   fun deleteTrackedDay(@InputArgument id: String): Boolean {
      return delete(repository, QETrackedDay.eTrackedDay.id, id)
   }

   @DgsMutation
   fun updateTrackedDay(
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

      return updateTrackedDay(record, mode, date, trackedTaskIds)
   }

   fun updateTrackedDay(
      trackedDay: ETrackedDay,
      mode: DayMode? = null,
      date: Double? = null,
      trackedTaskIds: List<String>? = null,
   ): TrackedDay {

      val copy = trackedDay.copy(
         mode = mode ?: trackedDay.mode,
         date = if (date != null) Date(date.toLong()) else trackedDay.date,
         trackedTaskIds = trackedTaskIds ?: trackedDay.trackedTaskIds
      )
      return repository.save(copy).toGqlType()
   }

   //
   // Document References (relationships)
   // -------------------------------------------------------------------------
   /**
    * This Data Fetcher is for the TrackedDay type's User field. When graphQl requests the
    * User field on a TrackedDay type we'll pass it onto the "users" DataLoader in order
    * to batch the fetches
    */
   @DgsData(parentType = DgsConstants.TRACKEDDAY.TYPE_NAME, field = DgsConstants.TRACKEDDAY.User)
   fun relatedUsers(dfe: DataFetchingEnvironment): CompletableFuture<User> {
      val dataLoader: DataLoader<String, User> = dfe.getDataLoader(DATA_LOADER_FOR_USERS)
      val trackedDay = dfe.getSource<TrackedDay>()
      return dataLoader.load(trackedDay.id)
   }

   @DgsData(parentType = DgsConstants.TRACKEDDAY.TYPE_NAME, field = DgsConstants.TRACKEDDAY.TrackedTasks)
   fun relatedTrackedTasks(dfe: DataFetchingEnvironment): CompletableFuture<List<TrackedTask>> {
      val dataLoader: DataLoader<String, List<TrackedTask>> =
         dfe.getDataLoader(DATA_LOADER_FOR_TRACKED_TASKS)
      val trackedDay = dfe.getSource<TrackedDay>()
      return dataLoader.load(trackedDay.id)
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
   val userBatchLoader = MappedBatchLoader<String, User> { trackedDayIds ->
      CompletableFuture.supplyAsync {
         val usersForTrackedDays = userRepository.findAll(QEUser.eUser.trackedDayIds.any().`in`(trackedDayIds))
         val associateBy: Map<String, User?> = trackedDayIds.associateBy(
            { it },
            { trackedDayId ->
               usersForTrackedDays.find { user -> user.trackedDayIds.contains(trackedDayId) }?.toGqlType()
            },
         )

         // LEARN: @ is a label marker and @supplyAsync is an implicit label that has the same
         //  name as the function to which the lambda is passed. We can omit the return statement
         //  altogether as well and simply have 'assocateBy', or go further and remove the
         //  associateBy val
         return@supplyAsync associateBy
      }
   }

   @DgsDataLoader(name = DATA_LOADER_FOR_TRACKED_TASKS, caching = true)
   val loadForTrackedDayBatchLoader = MappedBatchLoader<String, List<TrackedTask>> { trackedDayIds ->
      CompletableFuture.supplyAsync {
         val result: Map<String, List<TrackedTask>> = trackedTaskRepository
            .findAll(QETrackedTask.eTrackedTask.trackedDayId
               .`in`(trackedDayIds))
            .groupBy({ it.trackedDayId }, { it.toGqlType() })

         result
      }
   }

}