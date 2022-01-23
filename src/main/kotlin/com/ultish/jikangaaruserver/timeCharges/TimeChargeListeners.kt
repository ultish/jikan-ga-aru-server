package com.ultish.jikangaaruserver.timeCharges

import com.ultish.jikangaaruserver.entities.ETrackedTask
import com.ultish.jikangaaruserver.trackedTasks.TrackedTaskService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.mapping.event.*
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

         println(addedTimeSlots)
         println(removedTimeSlots)

         val trackedTaskToSave = event.source

         timeChargeService.updateTimeCharges(trackedTaskToSave,
            trackedTaskToSave.trackedDayId,
            addedTimeSlots + removedTimeSlots)
      }

      // TODO try to find differences
   }

   override fun onAfterSave(event: AfterSaveEvent<ETrackedTask>) {
      // if tracked task was saved then timecharges may change

      super.onAfterSave(event)

      /*
      whenever TrackedTasks change we need to:
       1. find all tracked tasks for the given tracked day
       2.

       */
   }

   override fun onBeforeDelete(event: BeforeDeleteEvent<ETrackedTask>) {
      super.onBeforeDelete(event)

   }

   override fun onAfterDelete(event: AfterDeleteEvent<ETrackedTask>) {
      // if the tracked task was deleted then timecharges may change
      super.onAfterDelete(event)
   }
}