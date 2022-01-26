package com.ultish.jikangaaruserver.timeChargeTotals

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.context.DgsContext
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.ChargeCode
import com.ultish.generated.types.TimeChargeTotal
import com.ultish.generated.types.TrackedDay
import com.ultish.jikangaaruserver.chargeCodes.ChargeCodeService
import com.ultish.jikangaaruserver.contexts.CustomContext
import com.ultish.jikangaaruserver.dataFetchers.dgsData
import com.ultish.jikangaaruserver.dataFetchers.dgsQuery
import com.ultish.jikangaaruserver.dataFetchers.getEntitiesFromEnv
import com.ultish.jikangaaruserver.dataFetchers.getUser
import com.ultish.jikangaaruserver.entities.ETimeChargeTotal
import com.ultish.jikangaaruserver.entities.QETimeCharge
import com.ultish.jikangaaruserver.entities.QETimeChargeTotal
import com.ultish.jikangaaruserver.entities.QETrackedDay
import com.ultish.jikangaaruserver.timeCharges.TimeChargeService
import com.ultish.jikangaaruserver.trackedDays.TrackedDayService
import graphql.schema.DataFetchingEnvironment
import org.dataloader.MappedBatchLoaderWithContext
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.ConnectableFlux
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.concurrent.CompletableFuture
import javax.annotation.PostConstruct

@DgsComponent
class TimeChargeTotalService {

   companion object {
      const val BLOCK_SIZE = 6
      const val DATA_LOADER_FOR_CHARGE_CODE = "chargeCodeForTimeChargeTotal"
      const val DATA_LOADER_FOR_TRACKED_DAY = "trackedDayForTimeChargeTotal"
   }

   private lateinit var timeChargeTotalStream: FluxSink<ETimeChargeTotal>
   private lateinit var timeChargeTotalPublisher: ConnectableFlux<ETimeChargeTotal>

   @Autowired
   lateinit var repository: TimeChargeTotalRepository

   @Autowired
   lateinit var timeChargeService: TimeChargeService

   @Autowired
   lateinit var trackedDayService: TrackedDayService

   @Autowired
   lateinit var chargeCodeService: ChargeCodeService

   @PostConstruct
   fun initialise() {
      val publisher = Flux.create<ETimeChargeTotal> { emitter ->
         timeChargeTotalStream = emitter
      }
      timeChargeTotalPublisher = publisher.publish()
      timeChargeTotalPublisher.connect()
   }

   @DgsSubscription
   fun timeChargeTotalChanged(@InputArgument userId: String): Publisher<TimeChargeTotal> {
      return timeChargeTotalPublisher.filter {
         it.userId == userId
      }.map {
         it.toGqlType()
      }
   }

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
      // TODO it's not great storing this data in the ID, eg trackedDayId is a composite as well so we need to ignore
      //  it's userId
      val (userId, _, delTrackedDayId, _, delChargeCodeId) = deletedTimeCharge?.split(":")

      // rebuild the trackedDayId
      val trackedDayId = "$userId:$delTrackedDayId"
      updateTimeChargeTotals(trackedDayId, delChargeCodeId, userId)
   }

   fun updateTimeChargeTotals(
      trackedDayId: String,
      chargeCodeId: String,
      userId: String,
   ) {
      val timeChargesForChargeCodeThatDay = timeChargeService.repository.findAll(BooleanBuilder()
         .and(QETimeCharge.eTimeCharge.trackedDayId.eq(trackedDayId))
         .and(QETimeCharge.eTimeCharge.chargeCodeId.eq(chargeCodeId)))

      val totalTime = timeChargesForChargeCodeThatDay.fold(0.0) { acc, timeCharge ->
         val time =
            (timeCharge.chargeCodeAppearance.toDouble() / timeCharge.totalChargeCodesForSlot.toDouble()) * BLOCK_SIZE
         acc + time
      }

      val timeChargeTotal = repository.findById("$userId:$trackedDayId:$chargeCodeId").map {
         it.copy(
            value = totalTime
         )
      }.orElse(ETimeChargeTotal(
         value = totalTime,
         trackedDayId = trackedDayId,
         chargeCodeId = chargeCodeId,
         userId = userId,
      ))

      // we could delete timeChargeTotals if Value == 0, ignoring for now

      repository.save(timeChargeTotal)
      timeChargeTotalStream.next(timeChargeTotal)

   }

   //
   // Document References (relationships)
   // -------------------------------------------------------------------------
   @DgsData(parentType = DgsConstants.TIMECHARGETOTAL.TYPE_NAME,
      field = DgsConstants.TIMECHARGETOTAL.ChargeCode)
   fun relatedChargeCode(dfe: DataFetchingEnvironment): CompletableFuture<ChargeCode> {
      return dgsData<ChargeCode, TimeChargeTotal>(dfe, DATA_LOADER_FOR_CHARGE_CODE) {
         it.id
      }
   }

   @DgsData(parentType = DgsConstants.TIMECHARGETOTAL.TYPE_NAME,
      field = DgsConstants.TIMECHARGETOTAL.TrackedDay)
   fun relatedTrackedDay(dfe: DataFetchingEnvironment): CompletableFuture<TrackedDay> {
      return dgsData<TrackedDay, TimeChargeTotal>(dfe, DATA_LOADER_FOR_TRACKED_DAY) {
         it.id
      }
   }
 
   //
   // Data Loaders
   // -------------------------------------------------------------------------
   @DgsDataLoader(name = DATA_LOADER_FOR_CHARGE_CODE, caching = true)
   val loadForTrackedTaskBatchLoader =
      MappedBatchLoaderWithContext<String, ChargeCode> { timeChargeTotalIds, environment ->
         CompletableFuture.supplyAsync {
            // Relationship: Many-To-One

            val customContext = DgsContext.getCustomContext<CustomContext>(environment)

            val timeChargeTotalToChargeCodeMap =
               getEntitiesFromEnv<String, ETimeChargeTotal>(environment, timeChargeTotalIds) { it ->
                  it.chargeCodeId
               }

            val chargeCodeMap = chargeCodeService.repository.findAllById(
               timeChargeTotalToChargeCodeMap.values.toList())
               .associateBy { it.id }

            // TODO not sure how these contexts are used in a federated graphQL scenario. I assume it probably wouldn't
            //  and I'd have to re-implement the logic to fetch from DB for the related trackedDayIds here if this was
            //  split into it's own microservice
            // pass down to next level if needed
            customContext.entities.addAll(chargeCodeMap.values)

            timeChargeTotalToChargeCodeMap.keys.associateWith { timeChargeTotalId ->
               val chargeCode =
                  timeChargeTotalToChargeCodeMap[timeChargeTotalId]?.let { chargeCodeMap[it] }
               chargeCode?.toGqlType()
            }
         }
      }

   @DgsDataLoader(name = DATA_LOADER_FOR_TRACKED_DAY, caching = true)
   val loadForTrackedDayBatchLoader =
      MappedBatchLoaderWithContext<String, TrackedDay> { timeChargeTotalIds, environment ->
         CompletableFuture.supplyAsync {
            // Relationship: Many-To-One

            val customContext = DgsContext.getCustomContext<CustomContext>(environment)

            val timeChargeTotalToTrackedDayMap =
               getEntitiesFromEnv<String, ETimeChargeTotal>(environment, timeChargeTotalIds) { it ->
                  it.trackedDayId
               }

            val trackedDayMap = trackedDayService.repository.findAllById(
               timeChargeTotalToTrackedDayMap.values.toList())
               .associateBy { it.id }

            // TODO not sure how these contexts are used in a federated graphQL scenario. I assume it probably wouldn't
            //  and I'd have to re-implement the logic to fetch from DB for the related trackedDayIds here if this was
            //  split into it's own microservice
            // pass down to next level if needed
            customContext.entities.addAll(trackedDayMap.values)

            timeChargeTotalToTrackedDayMap.keys.associateWith { timeChargeTotalId ->
               val trackedDay =
                  timeChargeTotalToTrackedDayMap[timeChargeTotalId]?.let { trackedDayMap[it] }

               trackedDay?.toGqlType()
            }
         }
      }

}