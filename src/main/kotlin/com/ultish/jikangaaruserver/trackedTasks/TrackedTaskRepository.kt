package com.ultish.jikangaaruserver.trackedTasks

import com.ultish.jikangaaruserver.entities.ETrackedTask
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface TrackedTaskRepository : MongoRepository<ETrackedTask, String>,
   QuerydslPredicateExecutor<ETrackedTask> {
}