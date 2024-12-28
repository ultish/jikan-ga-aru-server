package com.ultish.jikangaaruserver.users

import com.ultish.jikangaaruserver.entities.ETrackedDay
import com.ultish.jikangaaruserver.entities.QEUser
import com.ultish.jikangaaruserver.listeners.getIdFrom
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent
import org.springframework.stereotype.Component

@Component
class UserTrackedDayListener : AbstractMongoEventListener<ETrackedDay>() {
    @Autowired
    lateinit var userService: UserService

    override fun onAfterSave(event: AfterSaveEvent<ETrackedDay>) {
        val userId = event.source.userId

        userService.repository.findById(userId)
            .map { user ->
                userService.updateUser(
                    user,
                    user.trackedDayIds + listOf(getIdFrom(event))
                )
            }
    }

    override fun onAfterDelete(event: AfterDeleteEvent<ETrackedDay>) {
        getIdFrom(event)?.let { trackedDayId ->
            userService.repository.findAll(QEUser.eUser.trackedDayIds.contains(trackedDayId))
                .forEach { user ->
                    userService.updateUser(
                        user,
                        user.trackedDayIds - listOf(trackedDayId).toSet()
                    )
                }
        }
    }
}