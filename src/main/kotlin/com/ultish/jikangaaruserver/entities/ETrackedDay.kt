package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.DayMode
import com.ultish.generated.types.TrackedDay
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document
@QueryEntity
class ETrackedDay(
   @Id
   val id: String,
   val date: Date,
   val mode: DayMode,
   val EUser: EUser,
   val tasks: List<ETrackedTask>
) : GraphQLEntity<TrackedDay> {
   override fun toGqlType(): TrackedDay =
      TrackedDay(id,
         date.time.toInt(),
         mode,
         EUser.toGqlType(),
         tasks.map { it.toGqlType() }
      )
}