package com.ultish.jikangaaruserver.dataLoaders
//
//import com.netflix.graphql.dgs.DgsDataLoader
//import com.querydsl.core.BooleanBuilder
//import com.ultish.generated.types.TrackedDay
//import com.ultish.jikangaaruserver.entities.QETrackedDay
//import com.ultish.jikangaaruserver.repositories.TrackedDayRepository
//import org.dataloader.BatchLoader
//import org.springframework.beans.factory.annotation.Autowired
//import java.util.concurrent.CompletableFuture
//import java.util.concurrent.CompletionStage
//
//@DgsDataLoader(name = "trackedDays")
//class TrackedDayDataLoader : BatchLoader<String, TrackedDay> {
//
//   @Autowired
//   lateinit var repository: TrackedDayRepository
//
//   override fun load(keys: MutableList<String>?): CompletionStage<List<TrackedDay>> {
//
//      return CompletableFuture.supplyAsync {
//         println("loading trackedDays ${keys}")
//         repository.findAll(BooleanBuilder(QETrackedDay.eTrackedDay.id.`in`
//            (keys))).map { it.toGqlType() }
//      }
//
//   }
//
//}