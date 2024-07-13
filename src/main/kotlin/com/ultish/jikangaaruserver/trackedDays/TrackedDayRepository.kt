package com.ultish.jikangaaruserver.trackedDays

import com.ultish.jikangaaruserver.entities.ETrackedDay
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.mongodb.repository.MongoRepository

interface TrackedDayRepository : MongoRepository<ETrackedDay, String>,
   JpaSpecificationExecutor<ETrackedDay> {
}