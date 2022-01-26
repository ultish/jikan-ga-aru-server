package com.ultish.jikangaaruserver.timeCharges

import com.ultish.jikangaaruserver.entities.ETrackedTask
import com.ultish.jikangaaruserver.listeners.getIdFrom
import com.ultish.jikangaaruserver.trackedTasks.TrackedTaskService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent
import org.springframework.data.mongodb.core.mapping.event.BeforeDeleteEvent
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent
import org.springframework.stereotype.Component

@Component
class TimeChargeTrackedTaskListener : AbstractMongoEventListener<ETrackedTask>() {
   @Autowired
   lateinit var timeChargeService: TimeChargeService

   @Autowired
   lateinit var trackedTaskService: TrackedTaskService

   override fun onBeforeSave(event: BeforeSaveEvent<ETrackedTask>) {

      if (event.source?.id != null) {

         val existingTrackedTask = trackedTaskService.repository.findById(event.source.id)
         val existingTimeSlots = if (existingTrackedTask.isPresent) {
            existingTrackedTask.get().timeSlots
         } else {
            listOf()
         }

         val addedTimeSlots = event.source.timeSlots.minus(existingTimeSlots.toSet())
         val removedTimeSlots = existingTimeSlots.minus(event.source.timeSlots.toSet())

         println("added timeslots: $addedTimeSlots")
         println("removed timeslots: $removedTimeSlots")

         val trackedTaskToSave = event.source
         val userId = trackedTaskToSave.userId

         timeChargeService.updateTimeCharges(
            userId,
            trackedTaskToSave,
            trackedTaskToSave.trackedDayId,
            addedTimeSlots + removedTimeSlots)
      }

   }

   override fun onAfterDelete(event: AfterDeleteEvent<ETrackedTask>) {
      super.onAfterDelete(event)

      // TODO as these mongo events are limiting it may be time to move to custom events instead.
      //  That way you can always provide the difference of before and after save or delete
      //  For now, we're jamming data into the IDs of the objects which is dodgy
      val id = getIdFrom(event)
      val (userId, trackedDayId, _) = id?.split(":") ?: listOf()

      if (trackedDayId != null) {
         timeChargeService.resetTimeCharges(userId, trackedDayId)
      }
   }

   override fun onBeforeDelete(event: BeforeDeleteEvent<ETrackedTask>) {
      super.onBeforeDelete(event)

   }

}