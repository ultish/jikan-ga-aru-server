package com.ultish.jikangaaruserver.timeBlocks

import com.ultish.jikangaaruserver.entities.ETrackedTask
import com.ultish.jikangaaruserver.entities.QETimeBlock
import com.ultish.jikangaaruserver.listeners.getIdFrom
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent
import org.springframework.stereotype.Component

@Component
class TimeBlockTrackedTaskListener : AbstractMongoEventListener<ETrackedTask>() {
   @Autowired
   lateinit var timeBlockService: TimeBlockService

   override fun onAfterDelete(event: AfterDeleteEvent<ETrackedTask>) {
      getIdFrom(event)?.let { trackedTaskId ->
         timeBlockService.repository.findAll(QETimeBlock.eTimeBlock.trackedTaskId.eq(trackedTaskId))
            .forEach { timeBlock ->
               timeBlockService.deleteTimeBlock(timeBlock.id)
            }

      }
   }
}
