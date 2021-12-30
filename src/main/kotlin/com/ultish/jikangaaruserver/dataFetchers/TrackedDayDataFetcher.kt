package com.ultish.jikangaaruserver.dataFetchers

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.DayMode
import com.ultish.generated.types.TrackedDay
import com.ultish.generated.types.User
import com.ultish.jikangaaruserver.entities.ETrackedDay
import com.ultish.jikangaaruserver.entities.QETrackedDay
import com.ultish.jikangaaruserver.entities.QEUser
import com.ultish.jikangaaruserver.repositories.TrackedDayRepository
import com.ultish.jikangaaruserver.repositories.UserRepository
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

   @Autowired
   lateinit var repository: TrackedDayRepository

   @Autowired
   lateinit var userRepository: UserRepository

   @DgsDataLoader(name = "trackedDaysForUsers", caching = true)
   val trackedDayBatchLoader = MappedBatchLoader<String, List<TrackedDay>> {
      CompletableFuture.supplyAsync {
         repository.findAll(BooleanBuilder(QETrackedDay.eTrackedDay.userId.`in`(it)))
            .groupBy({ it.userId }, { it.toGqlType() })
      }
   }

   /**
    * This Data Fetcher is for the TrackedDay type's User field. When graphQl requests the
    * User field on a TrackedDay type we'll pass it onto the "users" DataLoader in order
    * to batch the fetches
    */
   @DgsData(parentType = DgsConstants.TRACKEDDAY.TYPE_NAME, field = DgsConstants.TRACKEDDAY.User)
   fun usersForTrackedDays(dfe: DataFetchingEnvironment): CompletableFuture<User> {
      val dataLoader: DataLoader<String, User> = dfe.getDataLoader("usersForTrackedDays")
      val trackedDay = dfe.getSource<TrackedDay>()
      return dataLoader.load(trackedDay.id)
   }

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
      @InputArgument username: String,
      @InputArgument date: Double, // not confusing at all, graphql's Float is passed in as a Double
      @InputArgument mode: DayMode?,
   ): TrackedDay {
      val user = userRepository.findOne(QEUser.eUser.username.eq(username))
      if (user.isEmpty) {
         throw DgsInvalidInputArgumentException(
            message = "Couldn't find user ${username}"
         )
      }
      val userEntity = user.get()
      val trackedDay = repository.save(ETrackedDay(
         id = ObjectId().toString(),
         date = Date(date.toLong()),
         mode = mode ?: DayMode.NORMAL,
         userId = userEntity.id,
      ))

      // make sure to update the back-reference
      userEntity.trackedDayIds.add(trackedDay.id)
      userRepository.save(userEntity)

      return trackedDay.toGqlType()
   }
}