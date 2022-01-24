package com.ultish.jikangaaruserver.timeChargeTotals

import com.ultish.jikangaaruserver.entities.ETimeChargeTotal
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface TimeChargeTotalRepository : MongoRepository<ETimeChargeTotal, String>,
   QuerydslPredicateExecutor<ETimeChargeTotal> {
}