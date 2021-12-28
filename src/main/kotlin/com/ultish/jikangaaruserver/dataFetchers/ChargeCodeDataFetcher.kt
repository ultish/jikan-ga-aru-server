package com.ultish.jikangaaruserver.dataFetchers

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.types.ChargeCode
import com.ultish.jikangaaruserver.entities.QEChargeCode
import com.ultish.jikangaaruserver.repositories.ChargeCodeRepository
import org.springframework.beans.factory.annotation.Autowired

@DgsComponent
class ChargeCodeDataFetcher {

   @Autowired
   lateinit var repository: ChargeCodeRepository

   @DgsQuery
   fun chargeCodes(
      @InputArgument ids: List<String>?,
      @InputArgument name: String?,
      @InputArgument code: String?,
      @InputArgument description: String?,
      @InputArgument expired: Boolean?
   ): List<ChargeCode> {
      val builder = BooleanBuilder()

      ids?.let {
         builder.and(QEChargeCode.eChargeCode.id.`in`(it))
      }
      code?.let {
         builder.and(QEChargeCode.eChargeCode.code.equalsIgnoreCase(it))
      }
      description?.let {
         builder.and(QEChargeCode.eChargeCode.description.containsIgnoreCase(it))
      }
      expired?.let {
         builder.and(QEChargeCode.eChargeCode.expired.eq(it))
      }

      return repository.findAll(builder).map { it.toGqlType() }
   }

   @DgsMutation
   fun deleteChargeCode(@InputArgument code: String): Boolean {
      val toDelete = repository.findOne(
         QEChargeCode.eChargeCode.code.equalsIgnoreCase(code)
      )

      if (toDelete.isPresent) {
         repository.delete(toDelete.get())
         return true
      }
      return false
   }
}