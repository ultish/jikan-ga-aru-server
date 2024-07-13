package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.TimeChargeTotal
import jakarta.persistence.Entity
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "timeChargeTotal")
@QueryEntity
@Entity
data class ETimeChargeTotal(
   @Id
   val id: String,
   val value: Double,
   @Indexed
   val trackedDayId: String,
   @Indexed
   val chargeCodeId: String,
   @Indexed
   val userId: String,
) : GraphQLEntity<TimeChargeTotal> {
   constructor(
      value: Double,
      trackedDayId: String,
      chargeCodeId: String,
      userId: String,
   ) : this(
      id = "$userId:$trackedDayId:$chargeCodeId",
      value = value,
      trackedDayId = trackedDayId,
      chargeCodeId = chargeCodeId,
      userId = userId,
   )

   override fun toGqlType(): TimeChargeTotal =
      TimeChargeTotal(
         id,
         value
      )

   override fun id(): String = id
}