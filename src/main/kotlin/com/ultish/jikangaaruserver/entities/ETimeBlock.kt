package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.TimeBlock
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "timeBlock")
@QueryEntity
data class ETimeBlock(
   @Id
   val id: String,
   val timeSlot: Int
) : GraphQLEntity<TimeBlock> {
   override fun toGqlType(): TimeBlock =
      TimeBlock(id, timeSlot)
}