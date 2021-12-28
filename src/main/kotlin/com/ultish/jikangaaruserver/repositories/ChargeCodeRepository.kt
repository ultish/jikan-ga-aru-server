package com.ultish.jikangaaruserver.repositories

import com.ultish.jikangaaruserver.entities.ChargeCode
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface ChargeCodeRepository : MongoRepository<ChargeCode, String>,
   QuerydslPredicateExecutor<ChargeCode> {
}