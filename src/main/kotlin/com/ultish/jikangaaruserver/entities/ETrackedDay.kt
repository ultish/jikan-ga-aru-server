package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.DayMode
import com.ultish.generated.types.TrackedDay
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document(value = "trackedDay")
@QueryEntity
data class ETrackedDay(
   @Id
   val id: String,
   val date: Date,
   val week: Int,
   val year: Int,
   val mode: DayMode,
   @Indexed
   val userId: String,
   @Indexed
   val trackedTaskIds: List<String>,
) : GraphQLEntity<TrackedDay> {
   constructor(
      date: Date,
      week: Int,
      year: Int,
      mode: DayMode,
      userId: String,
      trackedTaskIds: List<String> = listOf(),
   ) : this(
      id = "$userId:${ObjectId()}",
      date,
      week,
      year,
      mode,
      userId,
      trackedTaskIds
   )

   override fun toGqlType(): TrackedDay =
      TrackedDay(id,
         date.time.toDouble(),
         week,
         year,
         mode
      )

   override fun id(): String = id
}