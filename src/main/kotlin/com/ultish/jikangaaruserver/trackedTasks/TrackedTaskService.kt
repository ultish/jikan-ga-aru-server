package com.ultish.jikangaaruserver.trackedTasks

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.TimeBlock
import com.ultish.generated.types.TrackedDay
import com.ultish.generated.types.TrackedTask
import com.ultish.jikangaaruserver.contexts.CustomContext
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.dataFetchers.dgsData
import com.ultish.jikangaaruserver.dataFetchers.dgsQuery
import com.ultish.jikangaaruserver.entities.ETrackedTask
import com.ultish.jikangaaruserver.entities.QETimeBlock
import com.ultish.jikangaaruserver.entities.QETrackedTask
import com.ultish.jikangaaruserver.timeBlocks.TimeBlockRepository
import com.ultish.jikangaaruserver.trackedDays.TrackedDayRepository
import graphql.schema.DataFetchingEnvironment
import org.dataloader.MappedBatchLoaderWithContext
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
      dfe: DataFetchingEnvironment,
      @InputArgument trackedDayId: String?,
   ): List<TrackedTask> {

      return dgsQuery(dfe) {
         val builder = BooleanBuilder()
         trackedDayId?.let {
            builder.and(QETrackedTask.eTrackedTask.trackedDayId.eq(
               trackedDayId))
         }
         repository.findAll(builder)
      }

//      val trackedTasks = repository.findAll(QETrackedTask.eTrackedTask.trackedDayId.eq(
//         trackedDayId))
//
//      // push the entities into the graphql context
//      dfe.graphQlContext.put(DGS_CONTEXT_DATA, trackedTasks)
//
//      return trackedTasks.map { it.toGqlType() }
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
   @DgsData(parentType = DgsConstants.TRACKEDTASK.TYPE_NAME,
      field = DgsConstants.TRACKEDTASK.TrackedDay)
   fun relatedUsers(dfe: DataFetchingEnvironment): CompletableFuture<TrackedDay> {
//      return dgsData<TrackedDay, TrackedTask, ETrackedTask>(dfe,
//         DATA_LOADER_FOR_TRACKED_DAY) { trackedTask ->
//         trackedTask.id
//      }

//      val customContext = DgsContext.getCustomContext<CustomContext>(dfe)
//
//      val dataLoader: DataLoader<String, TrackedDay> = dfe.getDataLoader(DATA_LOADER_FOR_TRACKED_DAY)
//      val trackedTask = dfe.getSource<TrackedTask>()
//      return dataLoader.load(trackedTask.id, dfe.graphQlContext)
//
      return dgsData<TrackedDay, TrackedTask>(dfe, DATA_LOADER_FOR_TRACKED_DAY) {
         it.id
      }
   }

   @DgsData(parentType = DgsConstants.TRACKEDTASK.TYPE_NAME,
      field = DgsConstants.TRACKEDTASK.TimeBlocks)
   fun relatedTimeBlocks(dfe: DataFetchingEnvironment): CompletableFuture<List<TimeBlock>> {
      return dgsData<List<TimeBlock>, TrackedTask>(dfe,
         DATA_LOADER_FOR_TIME_BLOCKS) { trackedTask ->
         trackedTask.id
      }
   }

   //
   // Data Loaders
   // -------------------------------------------------------------------------
   @DgsDataLoader(name = DATA_LOADER_FOR_TRACKED_DAY, caching = true)
   val loadForTrackedTaskBatchLoader = MappedBatchLoaderWithContext<String, TrackedDay> { trackedTaskIds, environment ->
      CompletableFuture.supplyAsync {
         // Relationship: Many-To-One

         val customContext = DgsContext.getCustomContext<CustomContext>(environment)

         val trackedTaskToTrackedDayMap = customContext.entities.mapNotNull {
            if (it is ETrackedTask && trackedTaskIds.contains(it.id)) {
               it
            } else {
               null
            }
         }.associateBy({ it.id }, { it.trackedDayId })

         val trackedDayMap = trackedDayRepository.findAllById(
            trackedTaskToTrackedDayMap.values.toList())
            .associateBy { it.id }

         // pass down to next level if needed
         customContext.entities.addAll(trackedDayMap.values)

         trackedTaskToTrackedDayMap.keys.associateWith { trackedTaskId ->
            val trackedDay =
               trackedTaskToTrackedDayMap[trackedTaskId]?.let { trackedDayMap[it] }
            trackedDay?.toGqlType()
         }
      }
   }

   @DgsDataLoader(name = DATA_LOADER_FOR_TIME_BLOCKS, caching = true)
   val loadForTimeBlocks =
      MappedBatchLoaderWithContext<String, List<TimeBlock>> { trackedTaskIDs, env ->
         CompletableFuture.supplyAsync {
            // Relationship: One-To-Many

            val customContext = DgsContext.getCustomContext<CustomContext>(env)

            val timeBlocks = timeBlockRepository.findAll(QETimeBlock.eTimeBlock.trackedTaskId.`in`(
               trackedTaskIDs))

            customContext.entities.addAll(timeBlocks)

            timeBlocks.groupBy({ it.trackedTaskId }, { it.toGqlType() })
         }
      }
}

