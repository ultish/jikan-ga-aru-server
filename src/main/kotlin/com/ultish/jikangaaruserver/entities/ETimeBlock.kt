package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.TimeBlock
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "timeBlock")
@QueryEntity
data class ETimeBlock(
   @Id
   val id: String = ObjectId().toString(),
   val timeSlot: Int,
   @Indexed
   val trackedTaskId: String,
) : GraphQLEntity<TimeBlock> {
   override fun toGqlType(): TimeBlock =
      TimeBlock(id, timeSlot)

   override fun id(): String = id
}