package com.ultish.jikangaaruserver.timeBlocks

import com.netflix.graphql.dgs.DgsComponent
import com.ultish.jikangaaruserver.entities.QETimeBlock
import org.springframework.beans.factory.annotation.Autowired

@DgsComponent
class TimeBlockService {

   @Autowired
   lateinit var repository: TimeBlockRepository

   fun trackedTaskDeleted(trackedTaskId: String) {
      val toDelete = repository.findAll(QETimeBlock.eTimeBlock.trackedTaskId.eq(trackedTaskId))

      println("${trackedTaskId} deleted, deleting ${toDelete.count()} TimeBlocks")

      // TODO would need a subscription event

      // TODO would need to re-calculate timeCharge and timeSheet

      repository.deleteAll(toDelete)
   }

}