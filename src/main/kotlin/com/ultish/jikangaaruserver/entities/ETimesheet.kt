package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.Timesheet
import com.ultish.jikangaaruserver.trackedDays.ETrackedDay
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document(value = "timesheet")
@QueryEntity
data class ETimesheet(
   @Id
   val id: String = ObjectId().toString(),
   val weekEndingDate: Date,
   val EUser: EUser,
   val ETimeCharged: List<ETimeCharge>,
   val ETrackedDays: List<ETrackedDay>,
) : GraphQLEntity<Timesheet> {
   override fun toGqlType(): Timesheet =
      Timesheet(id,
         weekEndingDate.time.toDouble(),
         EUser.toGqlType(),
         ETimeCharged.map { it.toGqlType() },
         ETrackedDays.map { it.toGqlType() }
      )

   override fun id(): String = id
}