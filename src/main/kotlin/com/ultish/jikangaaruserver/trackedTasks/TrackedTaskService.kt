package com.ultish.jikangaaruserver.trackedTasks

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.TimeBlock
import com.ultish.generated.types.TrackedDay
import com.ultish.generated.types.TrackedTask
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.entities.ETrackedTask
import com.ultish.jikangaaruserver.entities.QETimeBlock
import com.ultish.jikangaaruserver.entities.QETrackedDay
import com.ultish.jikangaaruserver.entities.QETrackedTask
import com.ultish.jikangaaruserver.timeBlocks.TimeBlockRepository
import com.ultish.jikangaaruserver.trackedDays.TrackedDayRepository
import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoader
import org.dataloader.MappedBatchLoader
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture

@DgsComponent
class TrackedTaskService {
   private companion object {
      const val DATA_LOADER_FOR_TRACKED_DAY = "trackedDayForTrackedTask"
      const val DATA_LOADER_FOR_TIME_BLOCKS = "timeBlocksForTrackedTask"
   }

   @Autowired
   lateinit var repository: TrackedTaskRepository

   @Autowired
   lateinit var trackedDayRepository: TrackedDayRepository

   @Autowired
   lateinit var timeBlockRepository: TimeBlockRepository

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
   fun updateTrackedTask(
      @InputArgument id: String,
      @InputArgument notes: String? = null,
      @InputArgument chargeCodeIds: List<String>? = null, // TrackedTask owns this relationship
   ): TrackedTask {
      val record = repository.findById(id)
         .map { it }
         .orElseThrow {
            DgsInvalidInputArgumentException("Couldn't find TrackedTask[${id}]")
         }

      val copy = record.copy(
         notes = notes ?: record.notes,
         chargeCodeIds = chargeCodeIds ?: record.chargeCodeIds
      )

      return repository.save(copy).toGqlType()
   }

   @DgsMutation
   fun deleteTrackedTask(@InputArgument id: String): Boolean {
      return delete(repository, QETrackedTask.eTrackedTask.id, id)
   }

   //
   // Document References (relationships)
   // -------------------------------------------------------------------------
   @DgsData(parentType = DgsConstants.TRACKEDTASK.TYPE_NAME, field = DgsConstants.TRACKEDTASK.TrackedDay)
   fun relatedUsers(dfe: DataFetchingEnvironment): CompletableFuture<TrackedDay> {
      val dataLoader: DataLoader<String, TrackedDay> = dfe.getDataLoader(DATA_LOADER_FOR_TRACKED_DAY)
      val trackedTask = dfe.getSource<TrackedTask>()
      return dataLoader.load(trackedTask.id)
   }

   @DgsData(parentType = DgsConstants.TRACKEDTASK.TYPE_NAME, field = DgsConstants.TRACKEDTASK.TimeBlocks)
   fun relatedTimeBlocks(dfe: DataFetchingEnvironment): CompletableFuture<List<TimeBlock>> {
      val dataLoader = dfe.getDataLoader<String, List<TimeBlock>>(DATA_LOADER_FOR_TIME_BLOCKS)
      val trackedTask = dfe.getSource<TrackedTask>()
      return dataLoader.load(trackedTask.id)
   }

   //
   // Data Loaders
   // -------------------------------------------------------------------------
   @DgsDataLoader(name = DATA_LOADER_FOR_TRACKED_DAY, caching = true)
   val loadForTrackedTaskBatchLoader = MappedBatchLoader<String, TrackedDay> { trackedTaskIds ->
      CompletableFuture.supplyAsync {
         val trackedDays = trackedDayRepository.findAll(QETrackedDay.eTrackedDay.trackedTaskIds.any().`in`
            (trackedTaskIds))

         trackedTaskIds.associateBy({ it },
            { trackedTaskId ->
               trackedDays.find { td -> td.trackedTaskIds.contains(trackedTaskId) }?.toGqlType()
            })
      }
   }

   @DgsDataLoader(name = DATA_LOADER_FOR_TIME_BLOCKS, caching = true)
   val loadForTimeBlocks = MappedBatchLoader<String, List<TimeBlock>> { trackedTaskIDs ->
      CompletableFuture.supplyAsync {
         timeBlockRepository.findAll(QETimeBlock.eTimeBlock.trackedTaskId.`in`(trackedTaskIDs))
            .groupBy({ it.trackedTaskId }, { it.toGqlType() })
      }
   }
}

