package com.ultish.jikangaaruserver.chargeCodes

import com.ultish.jikangaaruserver.entities.EChargeCode
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.mongodb.repository.MongoRepository

interface ChargeCodeRepository : MongoRepository<EChargeCode, String>, JpaSpecificationExecutor<EChargeCode> {
}