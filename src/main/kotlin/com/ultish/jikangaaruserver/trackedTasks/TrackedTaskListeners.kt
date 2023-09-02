package com.ultish.jikangaaruserver.trackedTasks

import com.ultish.jikangaaruserver.dataFetchers.specEquals
import com.ultish.jikangaaruserver.entities.ETrackedDay
import com.ultish.jikangaaruserver.entities.ETrackedTask

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
         trackedTaskService.repository.findAll(
            specEquals<ETrackedTask>("trackedDayId", trackedDayId)

//            QETrackedTask.eTrackedTask.trackedDayId.eq(trackedDayId))
         )
            .forEach { trackedTask ->
               trackedTaskService.deleteTrackedTask(trackedTask.id)
            }
      }
   }
}

//@Component
//class TrackedTaskTimeBlockListener : AbstractMongoEventListener<ETimeBlock>() {
//   @Autowired
//   lateinit var trackedTaskService: TrackedTaskService
//
//   override fun onAfterSave(event: AfterSaveEvent<ETimeBlock>) {
//      // when a timeblock is created, add to the timeBlockIds
//      trackedTaskService.repository.findById(event.source.trackedTaskId).map {
//         trackedTaskService.updateTrackedTask(
//            trackedTask = it,
//            timeBlockIds = it.timeBlockIds + listOf(getIdFrom(event))
//         )
//      }
//   }
//
//   override fun onAfterDelete(event: AfterDeleteEvent<ETimeBlock>) {
//      // when a timeblock is deleted, remove from timeBlockIDs
//      getIdFrom(event)?.let { timeBlockId ->
//         // TODO is there a faster way to do this instead of finding all TrackedTasks that use this TimeBlock?
//         //  (There's only ever 1 Tracked Task)
//         trackedTaskService.repository.findAll(QETrackedTask.eTrackedTask.timeBlockIds.contains(timeBlockId))
//            .forEach { trackedTask ->
//               trackedTaskService.updateTrackedTask(
//                  trackedTask,
//                  timeBlockIds = trackedTask.timeBlockIds - listOf(timeBlockId).toSet()
//               )
//            }
//      }
//   }
//}