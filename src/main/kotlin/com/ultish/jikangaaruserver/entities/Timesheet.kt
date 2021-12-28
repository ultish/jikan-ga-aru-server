package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.Timesheet
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document
@QueryEntity
class Timesheet(
   @Id
   val id: String,
   val weekEndingDate: Date,
   val user: User,
   val timeCharged: List<TimeCharge>,
   val trackedDays: List<TrackedDay>
) : GraphQLEntity<Timesheet> {
   override fun toGqlType(): Timesheet =
      Timesheet(id,
         weekEndingDate.time.toInt(),
         user.toGqlType(),
         timeCharged.map { it.toGqlType() },
         trackedDays.map { it.toGqlType() }
      )
}