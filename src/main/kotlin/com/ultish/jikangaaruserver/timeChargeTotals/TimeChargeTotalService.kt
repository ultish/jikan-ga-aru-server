package com.ultish.jikangaaruserver.timeChargeTotals

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.types.TimeChargeTotal
import com.ultish.jikangaaruserver.dataFetchers.dgsQuery
import com.ultish.jikangaaruserver.dataFetchers.getUser
import com.ultish.jikangaaruserver.entities.ETimeChargeTotal
import com.ultish.jikangaaruserver.entities.QETimeCharge
import com.ultish.jikangaaruserver.entities.QETimeChargeTotal
import com.ultish.jikangaaruserver.entities.QETrackedDay
import com.ultish.jikangaaruserver.timeCharges.TimeChargeService
import com.ultish.jikangaaruserver.trackedDays.TrackedDayService
import graphql.schema.DataFetchingEnvironment
import org.springframework.beans.factory.annotation.Autowired

@DgsComponent
class TimeChargeTotalService {

   companion object {
      const val BLOCK_SIZE = 6
   }

   @Autowired
   lateinit var repository: TimeChargeTotalRepository

   @Autowired
   lateinit var timeChargeService: TimeChargeService

   @Autowired
   lateinit var trackedDayService: TrackedDayService

   @DgsQuery
   fun timeChargeTotals(
      dfe: DataFetchingEnvironment,
      @InputArgument week: Int? = null,
   ): List<TimeChargeTotal> {

      val userId = getUser(dfe)

      val builder = BooleanBuilder()

      week?.let {
         val trackedDayIds = trackedDayService.repository.findAll(
            BooleanBuilder()
               .and(QETrackedDay.eTrackedDay.week.eq(it))
               .and(QETrackedDay.eTrackedDay.userId.eq(userId))
         ).map { trackedDay ->
            trackedDay.id
         }
         builder.and(QETimeChargeTotal.eTimeChargeTotal.trackedDayId.`in`(trackedDayIds))
      }

      return dgsQuery(dfe) {
         repository.findAll(builder)
      }
   }

   fun deletedTimeCharge(deletedTimeCharge: String) {
      val (delTrackedDayId, _, delChargeCodeId) = deletedTimeCharge?.split(":")
      updateTimeChargeTotals(delTrackedDayId, delChargeCodeId)
   }

   fun updateTimeChargeTotals(
      trackedDayId: String,
      chargeCodeId: String,
   ) {
      val timeChargesForChargeCodeThatDay = timeChargeService.repository.findAll(BooleanBuilder()
         .and(QETimeCharge.eTimeCharge.trackedDayId.eq(trackedDayId))
         .and(QETimeCharge.eTimeCharge.chargeCodeId.eq(chargeCodeId)))

      val totalTime = timeChargesForChargeCodeThatDay.fold(0.0) { acc, timeCharge ->
         val time =
            (timeCharge.chargeCodeAppearance.toDouble() / timeCharge.totalChargeCodesForSlot.toDouble()) * BLOCK_SIZE
         acc + time
      }

      val timeChargeTotal = repository.findById("$trackedDayId:$chargeCodeId").map {
         it.copy(
            value = totalTime
         )
      }.orElse(ETimeChargeTotal(
         value = totalTime,
         trackedDayId = trackedDayId,
         chargeCodeId = chargeCodeId,
      ))

      // we could delete timeChargeTotals if Value == 0, ignoring for now

      repository.save(timeChargeTotal)
   }
}