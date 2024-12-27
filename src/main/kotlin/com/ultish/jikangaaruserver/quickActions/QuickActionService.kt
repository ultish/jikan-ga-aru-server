package com.ultish.jikangaaruserver.quickActions

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.ChargeCode
import com.ultish.generated.types.QuickAction
import com.ultish.generated.types.TrackedTask
import com.ultish.jikangaaruserver.chargeCodes.ChargeCodeRepository
import com.ultish.jikangaaruserver.contexts.CustomContext
import com.ultish.jikangaaruserver.dataFetchers.*
import com.ultish.jikangaaruserver.entities.EQuickAction
import com.ultish.jikangaaruserver.entities.QEChargeCode
import com.ultish.jikangaaruserver.entities.QEQuickAction
import graphql.schema.DataFetchingEnvironment
import org.dataloader.MappedBatchLoaderWithContext
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture


@DgsComponent
class QuickActionService {
    private companion object {
        const val DATA_LOADER_FOR_CHARGE_CODES = "chargeCodesForQuickActions"
    }

    @Autowired
    lateinit var repository: QuickActionRepository


    @Autowired
    lateinit var chargeCodeRepository: ChargeCodeRepository

    @DgsQuery
    fun quickAction(
        dfe: DataFetchingEnvironment,
        @InputArgument id: String
    ): QuickAction? {

        val builder = BooleanBuilder()
            .and(QEQuickAction.eQuickAction.id.eq(id))

        return repository.findOne(builder)
            .map { it.toGqlType() }
            .orElse(null)
    }

    @DgsQuery
    fun quickActions(
        dfe: DataFetchingEnvironment,
        @InputArgument ids: List<String>?
    ): List<QuickAction> {
        return dgsQuery(dfe) {
            val builder = BooleanBuilder()
            ids?.let {
                builder.and(QEChargeCode.eChargeCode.id.`in`(it))
            }
            repository.findAll(builder)
        }
    }


    @DgsMutation
    fun createQuickAction(
        dfe: DataFetchingEnvironment,
        @InputArgument name: String,
        @InputArgument description: String?,
        @InputArgument chargeCodeIds: List<String>,
        @InputArgument timeSlots: List<Int> = listOf(),
    ): QuickAction {
        val userId = getUser(dfe)
        return dgsMutate(dfe) {
            repository.save(
                EQuickAction(
                    name = name,
                    description = description,
                    chargeCodeIds = chargeCodeIds,
                    timeSlots = timeSlots,
                    userId = userId
                )
            )
        }
    }

    @DgsMutation
    fun updateQuickAction(
        dfe: DataFetchingEnvironment,
        @InputArgument id: String,
        @InputArgument name: String?,
        @InputArgument description: String?,
        @InputArgument chargeCodeIds: List<String>? = null,
        @InputArgument timeSlots: List<Int>? = null,
    ): QuickAction {
        val record = repository.findById(id)
            .map { it }
            .orElseThrow {
                DgsInvalidInputArgumentException("Couldn't find QuickAction[${id}]")
            }

        return dgsMutate(dfe) {
            repository.save(
                record.copy(
                    name = name ?: record.name,
                    description = description ?: record.description,
                    chargeCodeIds = chargeCodeIds ?: record.chargeCodeIds,
                    timeSlots = timeSlots ?: record.timeSlots
                )
            )
        }
    }


    @DgsMutation
    fun deleteQuickAction(@InputArgument id: String): Boolean {
        return delete(repository, QEQuickAction.eQuickAction.id, id)
    }


    @DgsData(
        parentType = DgsConstants.QUICKACTION.TYPE_NAME,
        field = DgsConstants.QUICKACTION.ChargeCodes
    )
    fun relatedChargeCodes(dfe: DataFetchingEnvironment): CompletableFuture<List<ChargeCode>> {
        return dgsData<List<ChargeCode>, TrackedTask>(
            dfe,
            DATA_LOADER_FOR_CHARGE_CODES
        ) { qa ->
            qa.id
        }
    }

    @DgsDataLoader(name = DATA_LOADER_FOR_CHARGE_CODES, caching = true)
    val loadForChargeCodes = MappedBatchLoaderWithContext<String, List<ChargeCode>> { qaIds, env ->
        CompletableFuture.supplyAsync {
            // relationship: Many-To-Many

            val customContext = DgsContext.getCustomContext<CustomContext>(env)

            val quickActionsToChargeCodesMap = customContext.entities.mapNotNull {
                if (it is EQuickAction && qaIds.contains(it.id)) {
                    it
                } else {
                    null
                }
            }
                .associateBy({ it.id }, { it.chargeCodeIds })

            val chargeCodeIds = quickActionsToChargeCodesMap.values.flatten()
                .distinct()
            val chargeCodeMap = chargeCodeRepository.findAllById(chargeCodeIds)
                .associateBy { it.id }

            // add to context for others to use
            customContext.entities.addAll(chargeCodeMap.values)

            quickActionsToChargeCodesMap.keys.associateWith { qaId ->
                val chargeCodes = quickActionsToChargeCodesMap[qaId]?.let { chargeCodeIds ->
                    chargeCodeIds.mapNotNull {
                        chargeCodeMap[it]?.toGqlType()
                    }
                }
                chargeCodes ?: listOf()
            }

        }
    }
}