package com.ultish.jikangaaruserver.dataFetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.types.User
import com.ultish.jikangaaruserver.entities.QEUser
import com.ultish.jikangaaruserver.repositories.UserRepository
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired

@DgsComponent
class UserDataFetcher {

   @Autowired
   lateinit var repository: UserRepository

   @DgsQuery
   fun users(
      @InputArgument username: String?
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
      @InputArgument password: String
   ): User {
      return repository.save(
         com.ultish.jikangaaruserver.entities.EUser(
            id = ObjectId().toString(),
            username = username,
            password = password // TODO hash this
         )
      ).toGqlType()
   }

   @DgsMutation
   fun deleteUser(@InputArgument username: String): Boolean {
      val toDelete = repository.findOne(
         QEUser.eUser.username
            .equalsIgnoreCase(username)
      )

      if (toDelete.isPresent) {
         repository.delete(toDelete.get())
         return true
      }
      return false
   }
}