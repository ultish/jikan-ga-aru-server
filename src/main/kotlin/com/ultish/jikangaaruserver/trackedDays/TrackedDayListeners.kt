package com.ultish.jikangaaruserver.trackedDays

import com.ultish.jikangaaruserver.entities.ETrackedTask
import com.ultish.jikangaaruserver.entities.EUser
import com.ultish.jikangaaruserver.entities.QETrackedDay
import com.ultish.jikangaaruserver.listeners.getIdFrom
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent
import org.springframework.stereotype.Component

@Component
class TrackedDayUserListener : AbstractMongoEventListener<EUser>() {
   @Autowired
   lateinit var trackedDayService: TrackedDayService

   override fun onAfterDelete(event: AfterDeleteEvent<EUser>) {
      getIdFrom(event)?.let { userId ->
         trackedDayService.repository.findAll(QETrackedDay.eTrackedDay.userId.eq(userId)).forEach { toDel ->
            trackedDayService.deleteTrackedDay(toDel.id)
         }
      }
   }
}

@Component
class TrackedDayTrackedTaskListener : AbstractMongoEventListener<ETrackedTask>() {
   @Autowired
   lateinit var trackedDayService: TrackedDayService

   override fun onAfterSave(event: AfterSaveEvent<ETrackedTask>) {
      val trackedDayId = event.source.trackedDayId
      trackedDayService.repository.findById(trackedDayId).map { trackedDay ->
         trackedDayService.updateTrackedDay(
            trackedDay = trackedDay,
            trackedTaskIds = trackedDay.trackedTaskIds + listOf(getIdFrom(event))
         )
      }
   }

   override fun onAfterDelete(event: AfterDeleteEvent<ETrackedTask>) {
      getIdFrom(event)?.let { trackedTaskId ->
         trackedDayService.repository.findAll(QETrackedDay.eTrackedDay.trackedTaskIds.contains(trackedTaskId))
            .forEach { trackedDay ->
               trackedDayService.updateTrackedDay(
                  trackedDay = trackedDay,
                  trackedTaskIds = trackedDay.trackedTaskIds - listOf(trackedTaskId).toSet()
               )
            }
      }
   }
}