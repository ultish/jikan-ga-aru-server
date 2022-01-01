package com.ultish.jikangaaruserver.timeCharges

import com.ultish.jikangaaruserver.entities.ETimeCharge
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface TimeChargeRepository : MongoRepository<ETimeCharge, String>,
   QuerydslPredicateExecutor<ETimeCharge> {
}