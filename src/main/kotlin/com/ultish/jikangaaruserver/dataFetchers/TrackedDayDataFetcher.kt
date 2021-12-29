package com.ultish.jikangaaruserver.dataFetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.ultish.generated.types.DayMode
import com.ultish.generated.types.TrackedDay
import com.ultish.jikangaaruserver.entities.ETrackedDay
import com.ultish.jikangaaruserver.entities.QETrackedDay
import com.ultish.jikangaaruserver.entities.QEUser
import com.ultish.jikangaaruserver.repositories.TrackedDayRepository
import com.ultish.jikangaaruserver.repositories.UserRepository
import graphql.relay.Connection
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

@DgsComponent
class TrackedDayDataFetcher {

   @Autowired
   lateinit var repository: TrackedDayRepository

   @Autowired
   lateinit var userRepository: UserRepository

   @DgsQuery
   fun trackedDaysPaginated(
      @InputArgument after: String?,
      @InputArgument first: Int?,
   ): Connection<TrackedDay> {
      return fetchPaginated(
         repository,
         after,
         first,
         QETrackedDay.eTrackedDay.date.toString())
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

      return repository.save(ETrackedDay(
         id = ObjectId().toString(),
         date = Date(date.toLong()),
         mode = mode ?: DayMode.NORMAL,
         EUser = user.get(),
      )).toGqlType()
   }
}