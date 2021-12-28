package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.TimeCharge
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document
@QueryEntity
class ETimeCharge(
   @Id
   val id: String,
   val date: Date,
   val EChargeCode: EChargeCode,
   val value: Double
) : GraphQLEntity<TimeCharge> {
   override fun toGqlType(): TimeCharge =
      TimeCharge(
         id,
         date.time.toInt(),
         EChargeCode.toGqlType(),
         value
      )
}