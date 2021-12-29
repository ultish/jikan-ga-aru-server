package com.ultish.jikangaaruserver.repositories

import com.ultish.jikangaaruserver.entities.ETimeBlock
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface TimeBlockRepository : MongoRepository<ETimeBlock, String>,
   QuerydslPredicateExecutor<ETimeBlock> {
}