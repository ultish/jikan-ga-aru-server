package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.TrackedTask
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "trackedTask")
@QueryEntity
data class ETrackedTask(
   @Id
   val id: String,
   val notes: String?,
   @Indexed
   val trackedDayId: String,
   @Indexed
   val timeSlots: List<Int> = listOf(),
   @Indexed
   val chargeCodeIds: List<String> = listOf(),
) : GraphQLEntity<TrackedTask> {
   constructor(
      notes: String?,
      trackedDayId: String,
      timeSlots: List<Int> = listOf(),
      chargeCodeIds: List<String> = listOf(),
   ) : this(
      "$trackedDayId:${ObjectId()}",
      notes,
      trackedDayId,
      timeSlots,
      chargeCodeIds
   )

   override fun toGqlType(): TrackedTask = TrackedTask(id, notes, timeSlots)
   override fun id(): String = id
}