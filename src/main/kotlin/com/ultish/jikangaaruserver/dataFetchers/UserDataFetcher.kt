package com.ultish.jikangaaruserver.dataFetchers

import com.netflix.graphql.dgs.*
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.TrackedDay
import com.ultish.generated.types.User
import com.ultish.jikangaaruserver.entities.QEUser
import com.ultish.jikangaaruserver.repositories.UserRepository
import graphql.schema.DataFetchingEnvironment
import org.bson.types.ObjectId
import org.dataloader.BatchLoader
import org.dataloader.DataLoader
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture

@DgsComponent
class UserDataFetcher {

   @Autowired
   lateinit var repository: UserRepository

   /**
    * TODO unsure if use of DgsDataLoaders is useful in this small app vs just using Mongo/Spring
    *  to eagerly load all relationships every time. Probably no noticeable performance for this
    *  application. But a good study exercise.
    */

   /**
    * This data-loader will batch load User objects via the User ID
    */
   @DgsDataLoader(name = "usersForTrackedDays")
   val userBatchLoader = BatchLoader<String, User> {
      future(repository, QEUser.eUser.trackedDayIds.any().`in`(it))
   }

   @DgsData(parentType = DgsConstants.USER.TYPE_NAME, field = DgsConstants.USER.TrackedDays)
   fun trackedDaysForUser(dfe: DataFetchingEnvironment): CompletableFuture<List<TrackedDay>> {
      val dataLoader: DataLoader<String, List<TrackedDay>> = dfe.getDataLoader("trackedDaysForUsers")
      val user = dfe.getSource<User>()
      return dataLoader.load(user.id)
   }

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
         com.ultish.jikangaaruserver.entities.EUser(
            id = ObjectId().toString(),
            username = username,
            password = password, // TODO hash this
            trackedDayIds = mutableListOf()
         )
      ).toGqlType()
   }

   @DgsMutation
   fun deleteUser(@InputArgument username: String): Boolean {
      return delete(repository, QEUser.eUser.username, username)
   }
}