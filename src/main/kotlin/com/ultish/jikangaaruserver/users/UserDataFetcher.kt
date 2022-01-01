package com.ultish.jikangaaruserver.users

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.TrackedDay
import com.ultish.generated.types.User
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.entities.EUser
import com.ultish.jikangaaruserver.entities.QEUser
import com.ultish.jikangaaruserver.trackedDays.TrackedDayDataFetcher
import graphql.schema.DataFetchingEnvironment
import org.bson.types.ObjectId
import org.dataloader.DataLoader
import org.dataloader.MappedBatchLoader
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture

@DgsComponent
class UserDataFetcher {

   companion object {
      const val DATA_LOADER_FOR_TRACKED_DAYS = "usersForTrackedDays"
   }

   @Autowired
   lateinit var repository: UserRepository

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
            id = ObjectId().toString(),
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
         dfe.getDataLoader(TrackedDayDataFetcher.DATA_LOADER_FOR_USERS)
      val user = dfe.getSource<User>()
      return dataLoader.load(user.id)
   }

   //
   // Data Loaders
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
   @DgsDataLoader(name = DATA_LOADER_FOR_TRACKED_DAYS, caching = true)
   val userBatchLoader = MappedBatchLoader<String, User> { trackedDayIds ->
      CompletableFuture.supplyAsync {
         val usersForTrackedDays = repository.findAll(QEUser.eUser.trackedDayIds.any().`in`(trackedDayIds))
         val associateBy: Map<String, User?> = trackedDayIds.associateBy(
            { it },
            { trackedDayId ->
               usersForTrackedDays.find { user -> user.trackedDayIds.contains(trackedDayId) }?.toGqlType()
            },
         )

         // LEARN: @ is a label marker and @supplyAsync is an implicit label that has the same
         //  name as the function to which the lambda is passed. We can omit the return statement
         //  altogether as well and simply have 'assocateBy', or go futher and remove the
         //  associateBy val
         return@supplyAsync associateBy
      }
   }
}