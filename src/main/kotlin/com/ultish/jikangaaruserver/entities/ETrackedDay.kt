package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.DayMode
import com.ultish.generated.types.TrackedDay
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document(value = "trackedDay")
@QueryEntity
data class ETrackedDay(
   @Id
   val id: String,
   val date: Date,
   val mode: DayMode,
   val EUser: EUser,
   var tasks: List<ETrackedTask> = listOf(),
) : GraphQLEntity<TrackedDay> {
   override fun toGqlType(): TrackedDay =
      TrackedDay(id,
         date.time.toDouble(),
         mode,
         EUser.toGqlType(),
         tasks.map { it.toGqlType() }
      )
}