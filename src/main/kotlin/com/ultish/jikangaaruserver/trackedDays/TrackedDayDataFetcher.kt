package com.ultish.jikangaaruserver.trackedDays

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.DayMode
import com.ultish.generated.types.TrackedDay
import com.ultish.generated.types.User
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.dataFetchers.fetchPaginated
import com.ultish.jikangaaruserver.users.UserDataFetcher
import com.ultish.jikangaaruserver.users.UserRepository
import graphql.relay.Connection
import graphql.schema.DataFetchingEnvironment
import org.bson.types.ObjectId
import org.dataloader.DataLoader
import org.dataloader.MappedBatchLoader
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import java.util.concurrent.CompletableFuture

@DgsComponent
class TrackedDayDataFetcher {

   companion object {
      const val DATA_LOADER_FOR_USERS = "trackedDaysForUsers"
   }

   @Autowired
   lateinit var repository: TrackedDayRepository

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
         id = ObjectId().toString(),
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
      @InputArgument mode: DayMode?,
      @InputArgument date: Double?,
   ): TrackedDay {
      val record = repository.findById(id)
         .map { it }
         .orElseThrow {
            DgsInvalidInputArgumentException("Couldn't find TrackedDay[${id}]")
         }

      val copy = record.copy(
         mode = mode ?: record.mode,
         date = if (date != null) Date(date.toLong()) else record.date
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
   fun usersForTrackedDays(dfe: DataFetchingEnvironment): CompletableFuture<User> {
      val dataLoader: DataLoader<String, User> = dfe.getDataLoader(UserDataFetcher.DATA_LOADER_FOR_TRACKED_DAYS)
      val trackedDay = dfe.getSource<TrackedDay>()
      return dataLoader.load(trackedDay.id)
   }

   //
   // Data Loaders
   // -------------------------------------------------------------------------
   @DgsDataLoader(name = DATA_LOADER_FOR_USERS, caching = true)
   val trackedDaysBatchLoader = MappedBatchLoader<String, List<TrackedDay>> {
      CompletableFuture.supplyAsync {
         repository.findAll(QETrackedDay.eTrackedDay.userId.`in`(it))
            .groupBy({ it.userId }, { it.toGqlType() })
      }
   }
}