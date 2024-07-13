package com.ultish.jikangaaruserver.entities

import com.ultish.generated.types.TrackedTask
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "trackedTask")

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
   @Indexed
   val userId: String,
) : GraphQLEntity<TrackedTask> {
   constructor(
      notes: String?,
      trackedDayId: String,
      timeSlots: List<Int> = listOf(),
      chargeCodeIds: List<String> = listOf(),
      userId: String,
   ) : this(
      "$userId:$trackedDayId:${ObjectId()}",
      notes,
      trackedDayId,
      timeSlots,
      chargeCodeIds,
      userId,
   )

   override fun toGqlType(): TrackedTask = TrackedTask(id, notes, timeSlots)
   override fun id(): String = id
}