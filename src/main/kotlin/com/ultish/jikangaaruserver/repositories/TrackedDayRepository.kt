package com.ultish.jikangaaruserver.repositories

import com.ultish.jikangaaruserver.entities.ETrackedDay
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface TrackedDayRepository : MongoRepository<ETrackedDay, String>,
   QuerydslPredicateExecutor<ETrackedDay> {
}