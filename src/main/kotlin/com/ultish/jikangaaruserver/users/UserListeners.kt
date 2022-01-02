package com.ultish.jikangaaruserver.users

import com.ultish.jikangaaruserver.entities.QEUser
import com.ultish.jikangaaruserver.listeners.getIdFrom
import com.ultish.jikangaaruserver.trackedDays.ETrackedDay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent
import org.springframework.stereotype.Component

@Component
class UserTrackedDayListener : AbstractMongoEventListener<ETrackedDay>() {
   @Autowired
   lateinit var userDataFetcher: UserDataFetcher

   override fun onAfterSave(event: AfterSaveEvent<ETrackedDay>) {
      val userId = event.source.userId

      userDataFetcher.repository.findById(userId).map { user ->
         userDataFetcher.updateUser(
            user,
            user.trackedDayIds + listOf(getIdFrom(event)))
      }
   }

   override fun onAfterDelete(event: AfterDeleteEvent<ETrackedDay>) {
      getIdFrom(event)?.let { trackedDayId ->
         userDataFetcher.repository.findAll(QEUser.eUser.trackedDayIds.contains(trackedDayId))
            .forEach { user ->
               userDataFetcher.updateUser(user,
                  user.trackedDayIds - listOf(trackedDayId).toSet())
            }
      }
   }
}