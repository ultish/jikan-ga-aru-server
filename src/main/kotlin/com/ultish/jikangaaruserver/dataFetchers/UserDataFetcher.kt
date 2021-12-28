package com.ultish.portfolios

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.types.User
import com.ultish.jikangaaruserver.entities.QUser
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
         builder.and(QUser.user.username.equalsIgnoreCase(it))
      }

      return repository.findAll(builder).map { it.toGqlType() }
   }

   @DgsMutation
   fun createPortfolio(
      @InputArgument username: String,
      @InputArgument password: String
   ): User {
      return repository.save(
         com.ultish.jikangaaruserver.entities.User(
            id = ObjectId().toString(),
            username = username,
            password = password // TODO hash this
         )
      ).toGqlType()
   }

   @DgsMutation
   fun deletePortfolio(@InputArgument username: String): Boolean {
      val toDelete = repository.findOne(
         QUser.user.username
            .equalsIgnoreCase(username)
      )

      if (toDelete.isPresent) {
         repository.delete(toDelete.get())
         return true;
      }
      return false;
   }
}