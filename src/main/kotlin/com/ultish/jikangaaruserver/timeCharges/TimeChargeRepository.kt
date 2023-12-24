package com.ultish.jikangaaruserver.timeCharges

import com.ultish.jikangaaruserver.entities.ETimeCharge
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.mongodb.repository.MongoRepository

interface TimeChargeRepository : MongoRepository<ETimeCharge, String>,
   JpaSpecificationExecutor<ETimeCharge> {
}