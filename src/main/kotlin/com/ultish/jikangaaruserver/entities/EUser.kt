package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.User
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
@QueryEntity
class EUser(
   @Id
   val id: String,
   val username: String,
   val password: String
) : GraphQLEntity<User> {
   override fun toGqlType(): User = User(id, username)
}