package com.ultish.jikangaaruserver.entities

//@Document(value = "timesheet")
//@QueryEntity
//data class ETimesheet(
//   @Id
//   val id: String = ObjectId().toString(),
//   val weekEndingDate: Date,
//   @Indexed
//   val userId: String,
//   @Indexed
//   val timeChargeTotalIds: List<String>,
//   @Indexed
//   val trackedDayIds: List<String>,
//) : GraphQLEntity<Timesheet> {
//   override fun toGqlType(): Timesheet =
//      Timesheet(
//         id,
//         weekEndingDate.time.toDouble()
//      )
//
//   override fun id(): String = id
//}