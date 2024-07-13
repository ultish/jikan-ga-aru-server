package com.ultish.jikangaaruserver.entities
 
import com.ultish.generated.types.User
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Using a Kotlin data class here with immutable properties.
 * Note: all properties defined in the primary constructor will be used for:
 *    toString(), equals(), hashCode(), and copy()
 */
@Document(value = "user")
data class EUser(
   @Id
   val id: String = ObjectId().toString(),
   val username: String,
   val password: String,
   @Indexed
   val trackedDayIds: List<String> = listOf(), // by using IDs only you need to fetch the relationships yourself via DGS
) : GraphQLEntity<User> {
   override fun toGqlType(): User = User(id, username)
   override fun id(): String = id
}