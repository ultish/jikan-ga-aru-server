package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.TrackedTask
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(value = "trackedTask")
@QueryEntity
data class ETrackedTask(
   @Id
   val id: String,
   val notes: String,
   val EChargeCodes: List<EChargeCode>,
   val ETimeBlocks: List<ETimeBlock>
) : GraphQLEntity<TrackedTask> {
   override fun toGqlType(): TrackedTask =
      TrackedTask(
         id,
         notes,
         EChargeCodes.map { it.toGqlType() },
         ETimeBlocks.map { it.toGqlType() }
      )
}