package com.ultish.jikangaaruserver.trackedTasks

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.ChargeCode
import com.ultish.generated.types.TrackedDay
import com.ultish.generated.types.TrackedTask
import com.ultish.jikangaaruserver.chargeCodes.ChargeCodeRepository
import com.ultish.jikangaaruserver.contexts.CustomContext
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.dataFetchers.dgsData
import com.ultish.jikangaaruserver.dataFetchers.dgsMutate
import com.ultish.jikangaaruserver.dataFetchers.dgsQuery
import com.ultish.jikangaaruserver.entities.ETrackedTask
import com.ultish.jikangaaruserver.entities.QETrackedTask
import com.ultish.jikangaaruserver.trackedDays.TrackedDayRepository
import graphql.schema.DataFetchingEnvironment
import org.dataloader.MappedBatchLoaderWithContext
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture

@DgsComponent
class TrackedTaskService {
   private companion object {
      const val DATA_LOADER_FOR_TRACKED_DAY = "trackedDayForTrackedTask"

      //      const val DATA_LOADER_FOR_TIME_BLOCKS = "timeBlocksForTrackedTask"
      const val DATA_LOADER_FOR_CHARGE_CODES = "chargeCodesForTrackedTask"
   }

   @Autowired
   lateinit var repository: TrackedTaskRepository

   @Autowired
   lateinit var trackedDayRepository: TrackedDayRepository

//   @Autowired
//   lateinit var timeBlockRepository: TimeBlockRepository

   @Autowired
   lateinit var chargeCodeRepository: ChargeCodeRepository

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
   }

   @DgsMutation
   fun createTrackedTask(
      dfe: DataFetchingEnvironment,
      @InputArgument trackedDayId: String,
      @InputArgument notes: String?,
   ): TrackedTask {
      if (!trackedDayRepository.existsById(trackedDayId)) {
         throw DgsInvalidInputArgumentException("Couldn't find TrackedDay[${trackedDayId}]")
      }

      return dgsMutate(dfe) {
         repository.save(
            ETrackedTask(
               trackedDayId = trackedDayId,
               notes = notes
            )
         )
      }
   }

   @DgsMutation
   fun updateTrackedTask(
      dfe: DataFetchingEnvironment,
      @InputArgument id: String,
      @InputArgument notes: String? = null,
      @InputArgument chargeCodeIds: List<String>? = null, // TrackedTask owns this relationship
      @InputArgument timeSlots: List<Int>? = null,
   ): TrackedTask {
      val record = repository.findById(id)
         .map { it }
         .orElseThrow {
            DgsInvalidInputArgumentException("Couldn't find TrackedTask[${id}]")
         }

      return dgsMutate(dfe) {
         updateTrackedTask(record, notes, chargeCodeIds, timeSlots)
      }
   }

   fun updateTrackedTask(
      trackedTask: ETrackedTask,
      notes: String? = null,
      chargeCodeIds: List<String>? = null,
      timeSlots: List<Int>? = null,
   ): ETrackedTask {
      val copy = trackedTask.copy(
         notes = notes ?: trackedTask.notes,
         chargeCodeIds = chargeCodeIds ?: trackedTask.chargeCodeIds,
         timeSlots = timeSlots ?: trackedTask.timeSlots
      )
      return repository.save(copy)
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
      return dgsData<TrackedDay, TrackedTask>(dfe, DATA_LOADER_FOR_TRACKED_DAY) {
         it.id
      }
   }

//   @DgsData(parentType = DgsConstants.TRACKEDTASK.TYPE_NAME,
//      field = DgsConstants.TRACKEDTASK.TimeBlocks)
//   fun relatedTimeBlocks(dfe: DataFetchingEnvironment): CompletableFuture<List<TimeBlock>> {
//      return dgsData<List<TimeBlock>, TrackedTask>(dfe,
//         DATA_LOADER_FOR_TIME_BLOCKS) { trackedTask ->
//         trackedTask.id
//      }
//   }

   @DgsData(parentType = DgsConstants.TRACKEDTASK.TYPE_NAME,
      field = DgsConstants.TRACKEDTASK.ChargeCodes)
   fun relatedChargeCodes(dfe: DataFetchingEnvironment): CompletableFuture<List<ChargeCode>> {
      return dgsData<List<ChargeCode>, TrackedTask>(dfe,
         DATA_LOADER_FOR_CHARGE_CODES) { trackedTask ->
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

         // TODO not sure how these contexts are used in a federated graphQL scenario. I assume it probably wouldn't
         //  and I'd have to re-implement the logic to fetch from DB for the related trackedDayIds here if this was
         //  split into it's own microservice
         // pass down to next level if needed
         customContext.entities.addAll(trackedDayMap.values)

         trackedTaskToTrackedDayMap.keys.associateWith { trackedTaskId ->
            val trackedDay =
               trackedTaskToTrackedDayMap[trackedTaskId]?.let { trackedDayMap[it] }
            trackedDay?.toGqlType()
         }
      }
   }

//   @DgsDataLoader(name = DATA_LOADER_FOR_TIME_BLOCKS, caching = true)
//   val loadForTimeBlocks =
//      MappedBatchLoaderWithContext<String, List<TimeBlock>> { trackedTaskIDs, env ->
//         CompletableFuture.supplyAsync {
//            // Relationship: One-To-Many
//
//            val customContext = DgsContext.getCustomContext<CustomContext>(env)
//
//            val timeBlocks = timeBlockRepository.findAll(QETimeBlock.eTimeBlock.trackedTaskId.`in`(
//               trackedTaskIDs))
//
//            customContext.entities.addAll(timeBlocks)
//
//            timeBlocks.groupBy({ it.trackedTaskId }, { it.toGqlType() })
//         }
//      }

   @DgsDataLoader(name = DATA_LOADER_FOR_CHARGE_CODES, caching = true)
   val loadForChargeCodes = MappedBatchLoaderWithContext<String, List<ChargeCode>> { trackedTaskIds, env ->
      CompletableFuture.supplyAsync {
         // relationship: Many-To-Many

         val customContext = DgsContext.getCustomContext<CustomContext>(env)

         val trackedTaskToChargeCodesMap = customContext.entities.mapNotNull {
            if (it is ETrackedTask && trackedTaskIds.contains(it.id)) {
               it
            } else {
               null
            }
         }.associateBy({ it.id }, { it.chargeCodeIds })

         val chargeCodeIds = trackedTaskToChargeCodesMap.values.flatten().distinct()
         val chargeCodeMap = chargeCodeRepository.findAllById(chargeCodeIds).associateBy { it.id }

         // add to context for others to use
         customContext.entities.addAll(chargeCodeMap.values)

         trackedTaskToChargeCodesMap.keys.associateWith { trackedTaskId ->
            val chargeCodes = trackedTaskToChargeCodesMap[trackedTaskId]?.let { chargeCodeIds ->
               chargeCodeIds.mapNotNull {
                  chargeCodeMap[it]?.toGqlType()
               }
            }
            chargeCodes ?: listOf()
         }

      }
   }
}

