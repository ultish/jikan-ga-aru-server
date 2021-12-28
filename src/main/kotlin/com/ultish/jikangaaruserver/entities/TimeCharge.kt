package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.TimeCharge
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document
@QueryEntity
class TimeCharge(
   @Id
   val id: String,
   val date: Date,
   val chargeCode: ChargeCode,
   val value: Double
) : GraphQLEntity<TimeCharge> {
   override fun toGqlType(): TimeCharge =
      TimeCharge(
         id,
         date.time.toInt(),
         chargeCode.toGqlType(),
         value
      )
}