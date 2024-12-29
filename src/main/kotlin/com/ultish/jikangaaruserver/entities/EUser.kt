package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.User
import jakarta.persistence.Entity
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

/**
 * Using a Kotlin data class here with immutable properties.
 * Note: all properties defined in the primary constructor will be used for:
 *    toString(), equals(), hashCode(), and copy()
 */
@Document(value = "user")
@QueryEntity
@Entity
data class EUser(
    @Id
    val id: String,
    val username: String,
    val password: String,
    @Indexed
    val trackedDayIds: List<String> = listOf(), // by using IDs only you need to fetch the relationships yourself via DGS
) : GraphQLEntity<User> {
    constructor(username: String, password: String, trackedDayIds: List<String> = listOf()) :
            this(
                UUID.nameUUIDFromBytes(username.toByteArray())
                    .toString(), username, password, trackedDayIds
            )


    override fun toGqlType(): User = User(id, username)
    override fun id(): String = id
}