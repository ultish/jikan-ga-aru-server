package com.ultish.jikangaaruserver.chargeCodes

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.ultish.generated.types.ChargeCode
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.dataFetchers.dgsMutate
import com.ultish.jikangaaruserver.dataFetchers.dgsQuery
import com.ultish.jikangaaruserver.entities.EChargeCode
import com.ultish.jikangaaruserver.entities.QEChargeCode
import graphql.schema.DataFetchingEnvironment
import jakarta.persistence.criteria.Predicate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.domain.Specification

/**
 * An empty Specification so we can build upon it
 */
inline fun <reified T> emptySpecification(): Specification<T> = Specification { _, _, _ -> null }

/**
 * A common Specification for a list of IDS, will split the list into chunks
 */
inline fun <reified T> specByIds(ids: List<String>): Specification<T> = Specification { root, _, builder ->
   val chunked = ids.chunked(1)
   val predicates = mutableListOf<Predicate>()
   chunked.forEach { chunk ->
      val inIds = builder.`in`(root.get<String>("id"))
      chunk.forEach { id -> inIds.value(id) }
      predicates.add(inIds)
   }
   builder.or(*predicates.toTypedArray())
}

@DgsComponent
class ChargeCodeService {

   @Autowired
   lateinit var repository: ChargeCodeRepository

   /**
    * You can combine all the expressions into one, or define a Specification for each type individually and combine
    * them that way
    */
   fun specChargeCodeIdName(ids: List<String>?, name: String?): Specification<EChargeCode> {
      return Specification { root, _, builder ->
         val predicates = mutableListOf<Predicate>()

         ids?.let { ids ->
            val inIds = builder.`in`(root.get<String>("id"))
            ids.forEach { id -> inIds.value(id) }
            predicates.add(inIds)
         }

         name?.let {
            predicates.add(builder.equal(root.get<String>("name"), name))
         }
         builder.and(*predicates.toTypedArray())
      }
   }

   fun specName(name: String): Specification<EChargeCode> {
      return Specification { root, _, builder ->
         builder.equal(root.get<String>("name"), name)
      }
   }

   @DgsQuery
   fun chargeCodes(
      dfe: DataFetchingEnvironment,
      @InputArgument ids: List<String>?,
      @InputArgument name: String?,
      @InputArgument code: String?,
      @InputArgument description: String?,
      @InputArgument expired: Boolean?,
   ): List<ChargeCode> {

      return dgsQuery(dfe) {
         val spec = emptySpecification<EChargeCode>()

         ids?.let {
            spec.and(specByIds(it))
         }
         name?.let {
            spec.and(specName(it))
         }
//         val builder = BooleanBuilder()
//
//         ids?.let {
//            builder.and(QEChargeCode.eChargeCode.id.`in`(it))
//         }
//         code?.let {
//            builder.and(QEChargeCode.eChargeCode.code.equalsIgnoreCase(it))
//         }
//         description?.let {
//            builder.and(QEChargeCode.eChargeCode.description.containsIgnoreCase(it))
//         }
//         expired?.let {
//            builder.and(QEChargeCode.eChargeCode.expired.eq(it))
//         }
//         repository.findAll(builder)

//         repository.findAll(this.specChargeCodeIdName(ids, name))
         repository.findAll(spec)
      }
   }

   @DgsMutation
   fun createChargeCode(
      dfe: DataFetchingEnvironment,
      @InputArgument name: String,
      @InputArgument code: String,
      @InputArgument description: String?,
      @InputArgument expired: Boolean = false,
   ): ChargeCode {
      return dgsMutate(dfe) {
         repository.save(
            EChargeCode(
               name = name,
               code = code,
               description = description,
               expired = expired
            )
         )
      }
   }

   @DgsMutation
   fun updateChargeCode(
      dfe: DataFetchingEnvironment,
      @InputArgument id: String,
      @InputArgument name: String?,
      @InputArgument code: String?,
      @InputArgument description: String?,
      @InputArgument expired: Boolean?,
   ): ChargeCode {
      val record = repository.findById(id)
         .map { it }
         .orElseThrow {
            DgsInvalidInputArgumentException(
               "Couldn't find " +
                  "ChargeCode[${id}]"
            )
         }

      val copy = record.copy(
         name = name ?: record.name,
         code = code ?: record.code,
         description = description ?: record.description,
         expired = expired ?: record.expired
      )
      return dgsMutate(dfe) {
         repository.save(copy)
      }
   }

   @DgsMutation
   fun deleteChargeCode(@InputArgument id: String): Boolean {
      // TODO validation, can't delete if it's in use
      return delete(repository, QEChargeCode.eChargeCode.id, id)
   }
}