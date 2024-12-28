package com.ultish.jikangaaruserver.chargeCodes

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.types.ChargeCode
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.dataFetchers.dgsMutate
import com.ultish.jikangaaruserver.dataFetchers.dgsQuery
import com.ultish.jikangaaruserver.entities.EChargeCode
import com.ultish.jikangaaruserver.entities.QEChargeCode
import graphql.schema.DataFetchingEnvironment
import org.springframework.beans.factory.annotation.Autowired

@DgsComponent
class ChargeCodeService {

    @Autowired
    lateinit var repository: ChargeCodeRepository

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
            repository.findAll(builder)
        }
    }

    @DgsMutation
    fun createChargeCode(
        dfe: DataFetchingEnvironment,
        @InputArgument name: String,
        @InputArgument code: String,
        @InputArgument description: String?,
        @InputArgument expired: Boolean = false,
        @InputArgument group: String?,
        @InputArgument sortOrder: Int?
    ): ChargeCode {
        return dgsMutate(dfe) {
            repository.save(
                EChargeCode(
                    name = name,
                    code = code,
                    description = description,
                    expired = expired,
                    group = group,
                    sortOrder = sortOrder
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
        @InputArgument group: String?,
        @InputArgument sortOrder: Int?
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
            expired = expired ?: record.expired,
            group = group ?: record.group,
            sortOrder = sortOrder ?: record.sortOrder
        )
        return dgsMutate(dfe) {
            repository.save(copy)
        }
    }

    @DgsMutation
    fun deleteChargeCode(@InputArgument id: String): String? {
        // TODO validation, can't delete if it's in use
        return delete(repository, QEChargeCode.eChargeCode.id, id)
    }
}