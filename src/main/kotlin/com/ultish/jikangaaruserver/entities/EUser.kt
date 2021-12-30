package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.User
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "user")
@QueryEntity
data class EUser(
   @Id
   val id: String,
   val username: String,
   val password: String,
//   @ReadOnlyProperty
//   @DocumentReference(lookup = "{'user':?#{#self._id} }")
//   @DocumentReference(lazy = true)
//   val trackedDays: MutableList<ETrackedDay>,
   @Indexed
   val trackedDayIds: MutableList<String>, // by using IDs only you need to fetch the relationships yourself via DGS dataloaders
) : GraphQLEntity<User> {
   override fun toGqlType(): User = User(id, username)
   override fun id(): String = id
}