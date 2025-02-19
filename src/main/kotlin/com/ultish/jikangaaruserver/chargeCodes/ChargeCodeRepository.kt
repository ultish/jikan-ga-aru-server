package com.ultish.jikangaaruserver.chargeCodes

import com.ultish.jikangaaruserver.entities.EChargeCode
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface ChargeCodeRepository : MongoRepository<EChargeCode, String>,
    QuerydslPredicateExecutor<EChargeCode> {
}