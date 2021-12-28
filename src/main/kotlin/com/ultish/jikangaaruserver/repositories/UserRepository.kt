package com.ultish.jikangaaruserver.repositories

import com.ultish.jikangaaruserver.entities.EUser
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface UserRepository : MongoRepository<EUser, String>,
   QuerydslPredicateExecutor<EUser> {
}