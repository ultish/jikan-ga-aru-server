package com.ultish.jikangaaruserver.timeCharges

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.ChargeCode
import com.ultish.generated.types.TimeCharge
import com.ultish.jikangaaruserver.chargeCodes.ChargeCodeService
import com.ultish.jikangaaruserver.contexts.CustomContext
import com.ultish.jikangaaruserver.dataFetchers.dgsData
import com.ultish.jikangaaruserver.dataFetchers.dgsMutate
import com.ultish.jikangaaruserver.dataFetchers.dgsQuery
import com.ultish.jikangaaruserver.entities.ETimeCharge
import com.ultish.jikangaaruserver.entities.ETrackedTask
import com.ultish.jikangaaruserver.entities.QETimeCharge
import com.ultish.jikangaaruserver.entities.QETrackedTask
import com.ultish.jikangaaruserver.trackedTasks.TrackedTaskService
import graphql.schema.DataFetchingEnvironment
import org.dataloader.MappedBatchLoaderWithContext
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture

@DgsComponent
class TimeChargeService {

   private companion object {
      const val DATA_LOADER_FOR_CHARGE_CODE = "chargeCodesForTimeCharge"
   }

   @Autowired
   lateinit var repository: TimeChargeRepository

   @Autowired
   lateinit var trackedTaskService: TrackedTaskService

   @Autowired
   lateinit var chargeCodeService: ChargeCodeService

   @DgsQuery
   fun timeCharges(
      dfe: DataFetchingEnvironment,
      @InputArgument timeSlot: Int? = null,
      @InputArgument chargeCodeId: String? = null,
   ): List<TimeCharge> {
      return dgsQuery(dfe) {
         val builder = BooleanBuilder()
         timeSlot?.let {
            builder.and(QETimeCharge.eTimeCharge.timeSlot.eq(timeSlot))
         }
         chargeCodeId?.let {
            builder.and(QETimeCharge.eTimeCharge.chargeCodeId.eq(chargeCodeId))
         }
         repository.findAll(builder)
      }
   }

   @DgsMutation
   fun createTimeCharge(
      dfe: DataFetchingEnvironment,
      @InputArgument timeSlot: Int,
      @InputArgument chargeCodeId: String,
      @InputArgument trackedDayId: String,
      @InputArgument chargeCodeAppearance: Int = 0,
      @InputArgument totalChargeCodesForSlot: Int = 0,
   ): TimeCharge {
      if (repository.exists(BooleanBuilder()
            .and(QETimeCharge.eTimeCharge.timeSlot.eq(timeSlot))
            .and(QETimeCharge.eTimeCharge.chargeCodeId.eq(chargeCodeId))
         )
      ) {
         throw DgsInvalidInputArgumentException("TimeCharge for [${chargeCodeId}:${timeSlot}] already exists")
      }

      return dgsMutate(dfe) {
         repository.save(
            ETimeCharge(timeSlot,
               chargeCodeAppearance,
               totalChargeCodesForSlot,
               trackedDayId,
               chargeCodeId)
         )
      }
   }

   @DgsMutation
   fun updateTimeCharge(
      dfe: DataFetchingEnvironment,
      @InputArgument id: String,
      @InputArgument chargeCodeAppearance: Int? = null,
      @InputArgument totalChargeCodesForSlot: Int? = null,
   ): TimeCharge {
      val record = repository.findById(id)
         .map { it }
         .orElseThrow {
            DgsInvalidInputArgumentException("Couldn't find TimeCharge[${id}]")
         }

      return dgsMutate(dfe) {
         updateTimeCharge(record, chargeCodeAppearance, totalChargeCodesForSlot)
      }
   }

   fun updateTimeCharge(
      timeCharge: ETimeCharge,
      chargeCodeAppearance: Int? = null,
      totalChargeCodesForSlot: Int? = null,
   ): ETimeCharge {
      return timeCharge.copy(
         chargeCodeAppearance = chargeCodeAppearance ?: timeCharge.chargeCodeAppearance,
         totalChargeCodesForSlot = totalChargeCodesForSlot ?: timeCharge.totalChargeCodesForSlot
      )
   }

   fun updateTimeCharges(
      trackedTaskToSave: ETrackedTask,
      trackedDayId: String,
      timeSlotsAdded: List<Int>,
//      timeSlotsRemoved: List<Int>,
   ) {

      // TODO the following code works for timeslotsAdded, for removed we need to do something different
      //  for example we may need to delete timeCharges when it's the last one using it in the remove case.
      //  and need to see if remove and add have same logic here for caclualting timeCharges

      val affectedTrackedTasks = trackedTaskService.repository.findAll(QETrackedTask.eTrackedTask.timeSlots.any().`in`
         (timeSlotsAdded)).toMutableList()

      // this function is called during beforeSave, so remove the stored version
      affectedTrackedTasks.removeIf { it.id == trackedTaskToSave.id }
      // and add the before saved version
      affectedTrackedTasks.add(trackedTaskToSave)
      // TODO may be odd, since the saved version hasn't made it to the DB and could fail

      val timeSlotToTrackedTasksMap = timeSlotsAdded.associateBy({ it }, { timeSlot ->
         affectedTrackedTasks.filter {
            it.timeSlots.contains(timeSlot)
         }
      })

      val allTimeCharges: List<ETimeCharge> = timeSlotToTrackedTasksMap.entries.flatMap { entry ->
         val timeSlot = entry.key
         val trackedTasksAtTimeSlot = entry.value

         // find out how many charge codes are used, including duplicates
         val allChargeCodes = trackedTasksAtTimeSlot.flatMap { trackedTask -> trackedTask.chargeCodeIds }
         val numberOfChargeCodes = allChargeCodes.size
         val chargeCodeIdsAtTimeSlot = allChargeCodes.distinct()

         // map to TimeCharge per chargecode ID
         val timeCharges = chargeCodeIdsAtTimeSlot.map { chargeCodeId ->
            val chargeCodeAppearance = trackedTasksAtTimeSlot.count { trackedTask ->
               trackedTask.chargeCodeIds.contains(chargeCodeId)
            }

            ETimeCharge(
               timeSlot = timeSlot,
               chargeCodeAppearance = chargeCodeAppearance,
               totalChargeCodesForSlot = numberOfChargeCodes,
               trackedDayId = trackedDayId,
               chargeCodeId = chargeCodeId
            )
         }

         println(timeCharges)
         // TODO find existing timeCharge by trackedDayId and timeSlot

         return@flatMap timeCharges
      }

      val ids = allTimeCharges.map { it.id }

      // find existing timeCharges
      val existingTimeCharges = repository.findAllById(ids)
      val newTimeCharges = allTimeCharges.minus(existingTimeCharges.toSet())


      existingTimeCharges.forEach {
         updateTimeCharge(it, it.chargeCodeAppearance, it.totalChargeCodesForSlot)
      }

      newTimeCharges.forEach {
         repository.save(it)
      }
   }

   //
   // Document References (relationships)
   // -------------------------------------------------------------------------
   @DgsData(parentType = DgsConstants.TIMECHARGE.TYPE_NAME,
      field = DgsConstants.TIMECHARGE.ChargeCode)
   fun relatedChargeCodes(dfe: DataFetchingEnvironment): CompletableFuture<ChargeCode> {
      return dgsData<ChargeCode, TimeCharge>(dfe, DATA_LOADER_FOR_CHARGE_CODE) {
         it.id
      }
   }

   //
   // Data Loaders
   // -------------------------------------------------------------------------
   @DgsDataLoader(name = DATA_LOADER_FOR_CHARGE_CODE, caching = true)
   val loadForTrackedTaskBatchLoader = MappedBatchLoaderWithContext<String, ChargeCode> { timeChargeIds, environment ->
      CompletableFuture.supplyAsync {
         // Relationship: Many-To-One

         val customContext = DgsContext.getCustomContext<CustomContext>(environment)

         val timeChargeToChargeCodeMap = customContext.entities.mapNotNull {
            if (it is ETimeCharge && timeChargeIds.contains(it.id)) {
               it
            } else {
               null
            }
         }.associateBy({ it.id }, { it.chargeCodeId })

         val chargeCodeMap = chargeCodeService.repository.findAllById(
            timeChargeToChargeCodeMap.values.toList())
            .associateBy { it.id }

         // TODO not sure how these contexts are used in a federated graphQL scenario. I assume it probably wouldn't
         //  and I'd have to re-implement the logic to fetch from DB for the related trackedDayIds here if this was
         //  split into it's own microservice
         // pass down to next level if needed
         customContext.entities.addAll(chargeCodeMap.values)

         timeChargeToChargeCodeMap.keys.associateWith { timeChargeId ->
            val chargeCode =
               timeChargeToChargeCodeMap[timeChargeId]?.let { chargeCodeMap[it] }
            chargeCode?.toGqlType()
         }
      }
   }
}
