package com.ultish.jikangaaruserver.users

import com.ultish.jikangaaruserver.entities.EUser
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<EUser, String>,
   JpaSpecificationExecutor<EUser> {
}