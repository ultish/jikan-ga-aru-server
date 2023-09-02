package com.ultish.jikangaaruserver.timeCharges

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException

import com.ultish.generated.DgsConstants
import com.ultish.generated.types.ChargeCode
import com.ultish.generated.types.TimeCharge
import com.ultish.generated.types.TrackedDay
import com.ultish.jikangaaruserver.chargeCodes.ChargeCodeService
import com.ultish.jikangaaruserver.contexts.CustomContext
import com.ultish.jikangaaruserver.dataFetchers.*
import com.ultish.jikangaaruserver.entities.ETimeCharge
import com.ultish.jikangaaruserver.entities.ETrackedTask
import com.ultish.jikangaaruserver.entities.QETimeCharge
import com.ultish.jikangaaruserver.entities.QETrackedTask
import com.ultish.jikangaaruserver.trackedDays.TrackedDayService
import com.ultish.jikangaaruserver.trackedTasks.TrackedTaskService
import graphql.schema.DataFetchingEnvironment
import org.dataloader.MappedBatchLoaderWithContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.domain.Specification
import java.util.concurrent.CompletableFuture

@DgsComponent
class TimeChargeService {

   private companion object {
      const val DATA_LOADER_FOR_CHARGE_CODE = "chargeCodeForTimeCharge"
      const val DATA_LOADER_FOR_TRACKED_DAY = "trackedDayForTimeCharge"
   }

   @Autowired
   lateinit var repository: TimeChargeRepository

   @Autowired
   lateinit var trackedTaskService: TrackedTaskService

   @Autowired
   lateinit var chargeCodeService: ChargeCodeService

   @Autowired
   lateinit var trackedDayService: TrackedDayService

   @DgsQuery
   fun timeCharges(
      dfe: DataFetchingEnvironment,
      @InputArgument trackedDayId: String? = null,
      @InputArgument timeSlot: Int? = null,
      @InputArgument chargeCodeId: String? = null,
   ): List<TimeCharge> {
      return dgsQuery(dfe) {
         val spec = emptySpecification<ETimeCharge>()

         trackedDayId?.let {
            spec.and(specEquals("trackedDayId", trackedDayId))
         }
         timeSlot?.let {
            spec.and(specEquals("timeSlot", timeSlot))
         }
         chargeCodeId?.let {
            spec.and(specEquals("chargeCodeId", chargeCodeId))
         }
         repository.findAll(spec)
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
      if (repository.exists(
            specEquals<ETimeCharge, Int>("timeSlot", timeSlot)
               .and(specEquals("chargeCodeId", chargeCodeId))
//            BooleanBuilder()
//               .and(QETimeCharge.eTimeCharge.timeSlot.eq(timeSlot))
//               .and(QETimeCharge.eTimeCharge.chargeCodeId.eq(chargeCodeId))
         )
      ) {
         throw DgsInvalidInputArgumentException("TimeCharge for [${chargeCodeId}:${timeSlot}] already exists")
      }

      val userId = getUser(dfe)

      return dgsMutate(dfe) {
         repository.save(
            ETimeCharge(
               timeSlot,
               chargeCodeAppearance,
               totalChargeCodesForSlot,
               trackedDayId,
               chargeCodeId,
               userId,
            )
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
      userId: String,
      trackedTaskToSave: ETrackedTask? = null,
      trackedDayId: String,
      timeSlotsChanged: List<Int>,
   ) {
      val anyTimeSlotMatches = Specification<ETrackedTask> { root, _, builder ->
         val preds = timeSlotsChanged.map { ts ->
            builder.isMember(ts, root.get<List<Int>>("timeSlots"))
         }
         builder.or(*preds.toTypedArray())
      }
      // Find all the Tracked Tasks that use any of the TimeSlots that have changed, these will need new TimeCharge
      // calculations
      val affectedTrackedTasks = trackedTaskService.repository.findAll(
         specEquals<ETrackedTask>("trackedDayId", trackedDayId)
            .and(anyTimeSlotMatches)
//
//         BooleanBuilder()
//            .and(QETrackedTask.eTrackedTask.timeSlots.any().`in`(timeSlotsChanged))
//            .and(QETrackedTask.eTrackedTask.trackedDayId.eq(trackedDayId))
      ).toMutableList()

      if (trackedTaskToSave != null) {
         // this function is called during beforeSave, so remove the stored version
         affectedTrackedTasks.removeIf { it.id == trackedTaskToSave.id }
         // and add the before saved version
         affectedTrackedTasks.add(trackedTaskToSave)
         // TODO may be odd, since the saved version hasn't made it to the DB and could fail
      }

      // For each TimeSlot group any TrackedTasks that use it
      val timeSlotToTrackedTasksMap = timeSlotsChanged.associateBy({ it }, { timeSlot ->
         affectedTrackedTasks.filter {
            it.timeSlots.contains(timeSlot)
         }
      })

      val timeSlotToTimeChargesMap = repository.findAll(
         specIn<ETimeCharge, Int>("timeSlot", timeSlotsChanged).and(
            specEquals("trackedDayId", trackedDayId)
         )
//         BooleanBuilder()
//            .and(QETimeCharge.eTimeCharge.timeSlot.`in`(timeSlotsChanged))
//            .and(QETimeCharge.eTimeCharge.trackedDayId.eq(trackedDayId))
      ).groupBy { it.timeSlot }

      val toDelete = mutableSetOf<ETimeCharge>()

      val allTimeCharges: List<ETimeCharge> = timeSlotToTrackedTasksMap.entries.flatMap { entry ->
         val timeSlot = entry.key
         val trackedTasksAtTimeSlot = entry.value

         // find out how many charge codes are used, including duplicates
         val allChargeCodes = trackedTasksAtTimeSlot.flatMap { trackedTask -> trackedTask.chargeCodeIds }
         val numberOfChargeCodes = allChargeCodes.size
         val chargeCodeIdsAtTimeSlot = allChargeCodes.distinct()

         // find TimeCharges for ChargeCodes at this TimeSlot that aren't used anymore
         val timeChargesForTimeSlot = timeSlotToTimeChargesMap[timeSlot]

         val timeChargesNotUsedByChargeCodesAnymore = timeChargesForTimeSlot?.filter {
            !chargeCodeIdsAtTimeSlot.contains(it.chargeCodeId)
         } ?: listOf()
         toDelete.addAll(timeChargesNotUsedByChargeCodesAnymore)

         println("These TimeCharges aren't used by ChargeCodes at timeslot $timeSlot: $timeChargesNotUsedByChargeCodesAnymore")

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
               chargeCodeId = chargeCodeId,
               userId = userId,
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


      repository.deleteAll(toDelete)

      existingTimeCharges.forEach {
         updateTimeCharge(it, it.chargeCodeAppearance, it.totalChargeCodesForSlot)
      }

      newTimeCharges.forEach {
         repository.save(it)
      }
   }

   fun resetTimeCharges(userId: String, trackedDayId: String) {
      // full time range 0=00:00, 1=00:06, 2=00:12, etc to X=23:54, 10 per hour
      val timeSlots = (0..240).toList()

      updateTimeCharges(
         trackedDayId = trackedDayId,
         timeSlotsChanged = timeSlots,
         userId = userId,
      )
   }

   //
   // Document References (relationships)
   // -------------------------------------------------------------------------
   @DgsData(
      parentType = DgsConstants.TIMECHARGE.TYPE_NAME,
      field = DgsConstants.TIMECHARGE.ChargeCode
   )
   fun relatedChargeCode(dfe: DataFetchingEnvironment): CompletableFuture<ChargeCode> {
      return dgsData<ChargeCode, TimeCharge>(dfe, DATA_LOADER_FOR_CHARGE_CODE) {
         it.id
      }
   }

   @DgsData(
      parentType = DgsConstants.TIMECHARGE.TYPE_NAME,
      field = DgsConstants.TIMECHARGE.TrackedDay
   )
   fun relatedTrackedDay(dfe: DataFetchingEnvironment): CompletableFuture<TrackedDay> {
      return dgsData<TrackedDay, TimeCharge>(dfe, DATA_LOADER_FOR_TRACKED_DAY) {
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

         val timeChargeToChargeCodeMap = getEntitiesFromEnv<String, ETimeCharge>(environment, timeChargeIds) {
            it.chargeCodeId
         }

         val chargeCodeMap = chargeCodeService.repository.findAllById(
            timeChargeToChargeCodeMap.values.toList()
         )
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

   @DgsDataLoader(name = DATA_LOADER_FOR_TRACKED_DAY, caching = true)
   val loadForTrackedDayBatchLoader = MappedBatchLoaderWithContext<String, TrackedDay> { timeChargeIds, environment ->
      CompletableFuture.supplyAsync {
         // Relationship: Many-To-One

         val customContext = DgsContext.getCustomContext<CustomContext>(environment)

         val timeChargeToTrackedDayMap = getEntitiesFromEnv<String, ETimeCharge>(environment, timeChargeIds) { it ->
            it.trackedDayId
         }

         val trackedDayMap = trackedDayService.repository.findAllById(
            timeChargeToTrackedDayMap.values.toList()
         )
            .associateBy { it.id }

         // TODO not sure how these contexts are used in a federated graphQL scenario. I assume it probably wouldn't
         //  and I'd have to re-implement the logic to fetch from DB for the related trackedDayIds here if this was
         //  split into it's own microservice
         // pass down to next level if needed
         customContext.entities.addAll(trackedDayMap.values)

         timeChargeToTrackedDayMap.keys.associateWith { timeChargeId ->
            val trackedDay =
               timeChargeToTrackedDayMap[timeChargeId]?.let { trackedDayMap[it] }

            trackedDay?.toGqlType()
         }
      }
   }
}
