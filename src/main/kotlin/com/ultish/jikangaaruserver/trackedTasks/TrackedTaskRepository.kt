package com.ultish.jikangaaruserver.trackedTasks

import com.ultish.jikangaaruserver.entities.ETrackedTask
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.mongodb.repository.MongoRepository

interface TrackedTaskRepository : MongoRepository<ETrackedTask, String>,
   JpaSpecificationExecutor<ETrackedTask> {
}