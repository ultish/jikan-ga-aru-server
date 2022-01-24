package com.ultish.jikangaaruserver.timeChargeTotals

import com.ultish.jikangaaruserver.entities.ETimeCharge
import com.ultish.jikangaaruserver.listeners.getIdFrom
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent
import org.springframework.stereotype.Component

@Component
class TimeChargeTotalTimeChargeListener : AbstractMongoEventListener<ETimeCharge>() {

   @Autowired
   lateinit var timeChargeTotalService: TimeChargeTotalService

   override fun onAfterSave(event: AfterSaveEvent<ETimeCharge>) {
      super.onAfterSave(event)

      val timeCharge = event.source

      val trackedDay = timeCharge.trackedDayId
      val chargeCode = timeCharge.chargeCodeId

      timeChargeTotalService.updateTimeChargeTotals(trackedDay, chargeCode)
   }

   override fun onAfterDelete(event: AfterDeleteEvent<ETimeCharge>) {
      val id = getIdFrom(event)
      id?.let {
         timeChargeTotalService.deletedTimeCharge(it)
      }
   }
}