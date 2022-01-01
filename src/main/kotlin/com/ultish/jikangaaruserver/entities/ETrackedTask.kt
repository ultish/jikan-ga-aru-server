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
   val id: String = ObjectId().toString(),
   val notes: String?,
   @Indexed
   val trackedDayId: String,
   @Indexed
   val chargeCodeIds: List<String> = listOf(),
   // TODO this may not be wise to add due to frequency of modifications
   @Indexed
   val timeBlockIDs: List<String> = listOf(),
) : GraphQLEntity<TrackedTask> {
   override fun toGqlType(): TrackedTask = TrackedTask(id, notes)
   override fun id(): String = id
}