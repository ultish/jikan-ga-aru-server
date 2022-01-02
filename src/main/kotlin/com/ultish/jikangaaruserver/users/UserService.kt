package com.ultish.jikangaaruserver.users

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.TrackedDay
import com.ultish.generated.types.User
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.entities.EUser
import com.ultish.jikangaaruserver.entities.QETrackedDay
import com.ultish.jikangaaruserver.entities.QEUser
import com.ultish.jikangaaruserver.trackedDays.TrackedDayRepository
import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoader
import org.dataloader.MappedBatchLoader
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture

@DgsComponent
class UserService {

   companion object {
      const val DATA_LOADER_FOR_TRACKED_DAYS = "trackedDaysForUser"
   }

   @Autowired
   lateinit var repository: UserRepository

   @Autowired()
//   @Qualifier("TrackedDayRepository")
   lateinit var trackedDayRepository: TrackedDayRepository

   @DgsQuery
   fun users(
      @InputArgument username: String?,
   ): List<User> {
      val builder = BooleanBuilder()

      username?.let {
         builder.and(QEUser.eUser.username.equalsIgnoreCase(it))
      }

      return repository.findAll(builder).map { it.toGqlType() }
   }

   @DgsMutation
   fun createUser(
      @InputArgument username: String,
      @InputArgument password: String,
   ): User {
      return repository.save(
         EUser(
            username = username,
            password = password, // TODO hash this
            trackedDayIds = listOf()
         )
      ).toGqlType()
   }

   @DgsMutation
   fun deleteUser(@InputArgument username: String): Boolean {
      return delete(repository, QEUser.eUser.username, username)
   }

   @DgsMutation
   fun updateUser(
      @InputArgument userId: String,
      @InputArgument trackedDayIds: List<String>? = null,
   ): User {
      val user = repository.findById(userId).map { it }.orElseThrow {
         DgsInvalidInputArgumentException("Couldn't find User[${userId}]")
      }

      return updateUser(user, trackedDayIds)
   }

   fun updateUser(user: EUser, trackedDayIds: List<String>? = null): User {
      println("Updating user[${user.username}] with trackedDayIds[${trackedDayIds}]")
      val copy = user.copy(
         trackedDayIds = trackedDayIds ?: user.trackedDayIds
      )
      return repository.save(copy).toGqlType()
   }

   //
   // Document References (relationships)
   // -------------------------------------------------------------------------
   @DgsData(parentType = DgsConstants.USER.TYPE_NAME, field = DgsConstants.USER.TrackedDays)
   fun trackedDaysForUser(dfe: DataFetchingEnvironment): CompletableFuture<List<TrackedDay>> {
      val dataLoader: DataLoader<String, List<TrackedDay>> =
         dfe.getDataLoader(DATA_LOADER_FOR_TRACKED_DAYS)
      val user = dfe.getSource<User>()
      return dataLoader.load(user.id)
   }

   //
   // Data Loaders
   // -------------------------------------------------------------------------
   @DgsDataLoader(name = DATA_LOADER_FOR_TRACKED_DAYS, caching = true)
   val trackedDaysBatchLoader = MappedBatchLoader<String, List<TrackedDay>> {
      CompletableFuture.supplyAsync {
         trackedDayRepository.findAll(QETrackedDay.eTrackedDay.userId.`in`(it))
            .groupBy({ it.userId }, { it.toGqlType() })
      }
   }
}