package com.ultish.jikangaaruserver.entities

import com.querydsl.core.annotations.QueryEntity
import com.ultish.generated.types.DayMode
import com.ultish.generated.types.TrackedDay
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
   val mode: DayMode,
   @Indexed
   val userId: String,
//   @DocumentReference(lazy = true)
//   val user: EUser,
//   @DocumentReference
//   var tasks: List<ETrackedTask> = listOf(),
) : GraphQLEntity<TrackedDay> {
   override fun toGqlType(): TrackedDay =
      TrackedDay(id,
         date.time.toDouble(),
         mode
//         EUser.toGqlType(),
//         tasks.map { it.toGqlType() }
      )

   override fun id(): String = id
}