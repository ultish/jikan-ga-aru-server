package com.ultish.jikangaaruserver.timeBlocks

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.types.TimeBlock
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.entities.ETimeBlock
import com.ultish.jikangaaruserver.entities.QETimeBlock
import org.springframework.beans.factory.annotation.Autowired

@DgsComponent
class TimeBlockService {

   @Autowired
   lateinit var repository: TimeBlockRepository

   @DgsQuery
   fun timeBlocks(
      @InputArgument ids: List<String>? = null,
      @InputArgument trackedTaskId: String? = null,
   ): List<TimeBlock> {
      val builder = BooleanBuilder()
      ids?.let {
         builder.and(QETimeBlock.eTimeBlock.id.`in`(it))
      }
      trackedTaskId?.let {
         builder.and(QETimeBlock.eTimeBlock.trackedTaskId.eq(it))
      }
      return repository.findAll(builder).map { it.toGqlType() }
   }

   @DgsMutation
   fun createTimeBlock(
      @InputArgument timeSlot: Int,
      @InputArgument trackedTaskId: String,
   ): TimeBlock {
      if (repository.exists(BooleanBuilder()
            .and(QETimeBlock.eTimeBlock.timeSlot.eq(timeSlot))
            .and(QETimeBlock.eTimeBlock.trackedTaskId.eq(trackedTaskId))
         )
      ) {
         throw DgsInvalidInputArgumentException("TimeBlock for [${timeSlot}] already exists")
      }
      return repository.save(
         ETimeBlock(timeSlot = timeSlot, trackedTaskId = trackedTaskId)
      ).toGqlType()
   }

   @DgsMutation
   fun deleteTimeBlock(
      @InputArgument id: String,
   ): Boolean {
      return delete(repository, QETimeBlock.eTimeBlock.id, id)
   }

   fun trackedTaskDeleted(trackedTaskId: String) {
      val toDelete = repository.findAll(QETimeBlock.eTimeBlock.trackedTaskId.eq(trackedTaskId))

      println("${trackedTaskId} deleted, deleting ${toDelete.count()} TimeBlocks")

      // TODO would need a subscription event

      // TODO would need to re-calculate timeCharge and timeSheet

      repository.deleteAll(toDelete)
   }

}