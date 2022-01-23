package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.TimeCharge
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "timeCharge")
@QueryEntity
data class ETimeCharge(
   @Id
   val id: String,
   val timeSlot: Int,
   val chargeCodeAppearance: Int,
   val totalChargeCodesForSlot: Int,
   val trackedDayId: String,
   val chargeCodeId: String,
) : GraphQLEntity<TimeCharge> {
   constructor(
      timeSlot: Int,
      chargeCodeAppearance: Int,
      totalChargeCodesForSlot: Int,
      trackedDayId: String,
      chargeCodeId: String,
   ) : this(
      id = "${chargeCodeId}:${timeSlot}",
      timeSlot = timeSlot,
      chargeCodeAppearance = chargeCodeAppearance,
      totalChargeCodesForSlot = totalChargeCodesForSlot,
      trackedDayId = trackedDayId,
      chargeCodeId = chargeCodeId)

   override fun toGqlType(): TimeCharge =
      TimeCharge(
         id,
         timeSlot,
         chargeCodeAppearance,
         totalChargeCodesForSlot
      )

   override fun id(): String = id
}