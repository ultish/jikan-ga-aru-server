package com.ultish.jikangaaruserver.trackedTasks

import com.ultish.jikangaaruserver.entities.ETrackedDay
import com.ultish.jikangaaruserver.entities.QETrackedTask
import com.ultish.jikangaaruserver.listeners.getIdFrom
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent
import org.springframework.stereotype.Component

@Component
class TrackedTaskTrackedDayListener : AbstractMongoEventListener<ETrackedDay>() {
   @Autowired
   lateinit var trackedTaskService: TrackedTaskService

   override fun onAfterDelete(event: AfterDeleteEvent<ETrackedDay>) {
      getIdFrom(event)?.let { trackedDayId ->
         trackedTaskService.repository.findAll(QETrackedTask.eTrackedTask.trackedDayId.eq(trackedDayId))
            .forEach { trackedTask ->
               trackedTaskService.deleteTrackedTask(trackedTask.id)
            }
      }
   }
}