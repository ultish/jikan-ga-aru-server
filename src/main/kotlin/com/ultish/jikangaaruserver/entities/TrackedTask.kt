package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.TrackedTask
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
@QueryEntity
class TrackedTask(
   @Id
   val id: String,
   val notes: String,
   val chargeCodes: List<ChargeCode>,
   val timeBlocks: List<TimeBlock>
) : GraphQLEntity<TrackedTask> {
   override fun toGqlType(): TrackedTask =
      TrackedTask(
         id,
         notes,
         chargeCodes.map { it.toGqlType() },
         timeBlocks.map { it.toGqlType() }
      )
}