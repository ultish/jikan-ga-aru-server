package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.ChargeCode
import jakarta.persistence.Entity
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "chargeCode")
@QueryEntity
@Entity
data class EChargeCode(
    @Id
    val id: String = ObjectId().toString(),
    val name: String,
    val code: String,
    val description: String?,
    val expired: Boolean,
    val group: String?,
    val sortOrder: Int?
) : GraphQLEntity<ChargeCode> {
    override fun toGqlType(): ChargeCode =
        ChargeCode(id, name, code, description, expired, group, sortOrder)

    override fun id(): String = id
}