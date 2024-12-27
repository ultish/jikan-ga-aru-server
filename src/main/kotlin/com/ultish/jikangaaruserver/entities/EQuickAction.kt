package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.QuickAction
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "quickAction")
@QueryEntity
@Entity
data class EQuickAction(
    @Id val id: String,
    val name: String,
    val description: String? = null,
    @Indexed
    val timeSlots: List<Int> = listOf(),
    @Indexed
    val chargeCodeIds: List<String> = listOf(),
    @Indexed
    val userId: String,
) : GraphQLEntity<QuickAction> {
    constructor(
        name: String,
        description: String?,
        timeSlots: List<Int> = listOf(),
        chargeCodeIds: List<String> = listOf(),
        userId: String,
    ) : this(
        "$userId:${ObjectId()}",
        name,
        description,
        timeSlots,
        chargeCodeIds,
        userId
    )

    override fun toGqlType(): QuickAction =
        QuickAction(id = id, name = name, description = description, timeSlots = timeSlots)

    override fun id(): String = id

}