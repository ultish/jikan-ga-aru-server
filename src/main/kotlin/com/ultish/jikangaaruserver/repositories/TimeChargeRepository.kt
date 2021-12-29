package com.ultish.jikangaaruserver.repositories

import com.ultish.jikangaaruserver.entities.ETimeCharge
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface TimeChargeRepository : MongoRepository<ETimeCharge, String>,
   QuerydslPredicateExecutor<ETimeCharge> {
}