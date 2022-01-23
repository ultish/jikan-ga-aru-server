package com.ultish.jikangaaruserver.timeBlocks

//@DgsComponent
class TimeBlockService {
//
//   @Autowired
//   lateinit var repository: TimeBlockRepository
//
//   @DgsQuery
//   fun timeBlocks(
//      dfe: DataFetchingEnvironment,
//      @InputArgument ids: List<String>? = null,
//      @InputArgument trackedTaskId: String? = null,
//   ): List<TimeBlock> {
//      return dgsQuery(dfe) {
//         val builder = BooleanBuilder()
//         ids?.let {
//            builder.and(QETimeBlock.eTimeBlock.id.`in`(it))
//         }
//         trackedTaskId?.let {
//            builder.and(QETimeBlock.eTimeBlock.trackedTaskId.eq(it))
//         }
//         repository.findAll(builder)
//      }
//   }
//
//   @DgsMutation
//   fun createTimeBlock(
//      dfe: DataFetchingEnvironment,
//      @InputArgument timeSlot: Int,
//      @InputArgument trackedTaskId: String,
//   ): TimeBlock {
//      if (repository.exists(BooleanBuilder()
//            .and(QETimeBlock.eTimeBlock.timeSlot.eq(timeSlot))
//            .and(QETimeBlock.eTimeBlock.trackedTaskId.eq(trackedTaskId))
//         )
//      ) {
//         throw DgsInvalidInputArgumentException("TimeBlock for [${timeSlot}] already exists")
//      }
//
//      return dgsMutate(dfe) {
//         repository.save(
//            ETimeBlock(timeSlot = timeSlot, trackedTaskId = trackedTaskId)
//         )
//      }
//   }
//
//   @DgsMutation
//   fun deleteTimeBlock(
//      @InputArgument id: String,
//   ): Boolean {
//      return delete(repository, QETimeBlock.eTimeBlock.id, id)
//   }

}