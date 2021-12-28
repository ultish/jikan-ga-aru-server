package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.ChargeCode
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
@QueryEntity
class EChargeCode(
   @Id
   val id: String,
   val name: String,
   val code: String,
   val description: String,
   val expired: Boolean
) : GraphQLEntity<ChargeCode> {
   override fun toGqlType(): ChargeCode =
      ChargeCode(id, name, code, description, expired)
}