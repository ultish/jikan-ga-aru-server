package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.Timesheet
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document(value = "timesheet")
@QueryEntity
data class ETimesheet(
   @Id
   val id: String = ObjectId().toString(),
   val weekEndingDate: Date,
   @Indexed
   val userId: String,
   @Indexed
   val timeChargeTotalIds: List<String>,
   @Indexed
   val trackedDayIds: List<String>,
) : GraphQLEntity<Timesheet> {
   override fun toGqlType(): Timesheet =
      Timesheet(
         id,
         weekEndingDate.time.toDouble()
      )

   override fun id(): String = id
}