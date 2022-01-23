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
   val id: String = ObjectId().toString(),
   val date: Date,
   val mode: DayMode,
   @Indexed
   val userId: String,
   @Indexed
   val trackedTaskIds: List<String> = listOf(),
) : GraphQLEntity<TrackedDay> {
   override fun toGqlType(): TrackedDay =
      TrackedDay(id,
         date.time.toDouble(),
         mode
      )

   override fun id(): String = id
}