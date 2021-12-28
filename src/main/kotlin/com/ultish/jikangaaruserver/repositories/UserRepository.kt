package com.ultish.jikangaaruserver.repositories

import com.ultish.jikangaaruserver.entities.User
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface UserRepository : MongoRepository<User, String>,
   QuerydslPredicateExecutor<User> {
}