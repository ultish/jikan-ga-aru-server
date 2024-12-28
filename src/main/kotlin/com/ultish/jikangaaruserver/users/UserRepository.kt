package com.ultish.jikangaaruserver.users

import com.ultish.jikangaaruserver.entities.EUser
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface UserRepository : MongoRepository<EUser, String>,
    QuerydslPredicateExecutor<EUser> {
}