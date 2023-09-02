package com.ultish.jikangaaruserver.timeChargeTotals

import com.ultish.jikangaaruserver.entities.ETimeChargeTotal
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.mongodb.repository.MongoRepository

interface TimeChargeTotalRepository : MongoRepository<ETimeChargeTotal, String>,
   JpaSpecificationExecutor<ETimeChargeTotal> {
}