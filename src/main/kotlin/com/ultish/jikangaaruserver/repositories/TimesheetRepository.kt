package com.ultish.jikangaaruserver.repositories

import com.ultish.jikangaaruserver.entities.ETimesheet
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface TimesheetRepository : MongoRepository<ETimesheet, String>,
   QuerydslPredicateExecutor<ETimesheet> {
}