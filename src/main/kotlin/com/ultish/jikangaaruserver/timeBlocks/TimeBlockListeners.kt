package com.ultish.jikangaaruserver.timeBlocks

//@Component
//class TimeBlockTrackedTaskListener : AbstractMongoEventListener<ETrackedTask>() {
//   @Autowired
//   lateinit var timeBlockService: TimeBlockService
//
//   override fun onAfterDelete(event: AfterDeleteEvent<ETrackedTask>) {
//      getIdFrom(event)?.let { trackedTaskId ->
//         timeBlockService.repository.findAll(QETimeBlock.eTimeBlock.trackedTaskId.eq(trackedTaskId))
//            .forEach { timeBlock ->
//               timeBlockService.deleteTimeBlock(timeBlock.id)
//            }
//
//      }
//   }
//}
