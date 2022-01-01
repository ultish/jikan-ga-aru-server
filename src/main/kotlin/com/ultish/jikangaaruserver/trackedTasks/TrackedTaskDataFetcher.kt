package com.ultish.jikangaaruserver.trackedTasks

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.ultish.generated.types.TrackedTask
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.entities.ETrackedTask
import com.ultish.jikangaaruserver.entities.QETrackedTask
import com.ultish.jikangaaruserver.trackedDays.TrackedDayRepository
import org.springframework.beans.factory.annotation.Autowired

@DgsComponent
class TrackedTaskDataFetcher {

   @Autowired
   lateinit var repository: TrackedTaskRepository

   @Autowired
   lateinit var trackedDayRepository: TrackedDayRepository

   @DgsQuery
   fun trackedTasks(
      @InputArgument trackedDayId: String,
   ): List<TrackedTask> {

      return repository.findAll(QETrackedTask.eTrackedTask.trackedDayId.eq(trackedDayId))
         .map { it.toGqlType() }
   }

   @DgsMutation
   fun createTrackedTask(
      @InputArgument trackedDayId: String,
      @InputArgument notes: String?,
   ): TrackedTask {
      if (!trackedDayRepository.existsById(trackedDayId)) {
         throw DgsInvalidInputArgumentException("Couldn't find TrackedDay[${trackedDayId}]")
      }

      val trackedTask = repository.save(
         ETrackedTask(
            trackedDayId = trackedDayId,
            notes = notes
         )
      )
      return trackedTask.toGqlType()
   }

   @DgsMutation
   fun deleteTrackedTask(@InputArgument id: String): Boolean {
      return delete(repository, QETrackedTask.eTrackedTask.id, id)

      // delete time blocks
      // remove from trackedDay

   }
}

