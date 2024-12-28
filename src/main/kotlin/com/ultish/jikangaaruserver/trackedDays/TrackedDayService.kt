package com.ultish.jikangaaruserver.trackedDays

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.*
import com.ultish.jikangaaruserver.contexts.CustomContext
import com.ultish.jikangaaruserver.dataFetchers.*
import com.ultish.jikangaaruserver.entities.*
import com.ultish.jikangaaruserver.timeChargeTotals.TimeChargeTotalRepository
import com.ultish.jikangaaruserver.timeCharges.TimeChargeRepository
import com.ultish.jikangaaruserver.trackedTasks.TrackedTaskRepository
import com.ultish.jikangaaruserver.users.UserRepository
import graphql.relay.Connection
import graphql.schema.DataFetchingEnvironment
import jakarta.annotation.PostConstruct
import org.dataloader.MappedBatchLoaderWithContext
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import reactor.core.publisher.ConnectableFlux
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.time.ZoneId
import java.util.*
import java.util.concurrent.CompletableFuture

@DgsComponent
class TrackedDayService {

    private companion object {
        const val DATA_LOADER_FOR_TRACKED_TASKS = "trackedTasksForTrackedDay"
        const val DATA_LOADER_FOR_USERS = "usersForTrackedDay"
        const val DATA_LOADER_FOR_TIME_CHARGES = "timeChargesForTrackedDay"
        const val DATA_LOADER_FOR_TIME_CHARGE_TOTALS = "timeChargeTotalsForTrackedDay"
    }

    @Autowired
    lateinit var repository: TrackedDayRepository

    @Autowired
    lateinit var trackedTaskRepository: TrackedTaskRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var timeChargeRepository: TimeChargeRepository

    @Autowired
    lateinit var timeChargeTotalRepository: TimeChargeTotalRepository

    private lateinit var trackedDayStream: FluxSink<ETrackedDay>
    private lateinit var trackedDayPublisher: ConnectableFlux<ETrackedDay>


    @PostConstruct
    fun initialise() {
        val publisher = Flux.create<ETrackedDay> { emitter ->
            trackedDayStream = emitter
        }
        trackedDayPublisher = publisher.publish()
        trackedDayPublisher.connect()
    }

    @DgsSubscription
    fun trackedDayChanged(
        dfe: DataFetchingEnvironment,
        @InputArgument month: Int,
        @InputArgument year: Int
    ): Publisher<TrackedDay> {
        val userId = getUser(dfe)
        return trackedDayPublisher.filter {
            // Convert java.util.Date to LocalDate
            val localDate = it.date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            // Extract month and year
            val dMonth = localDate.monthValue // 1 to 12
            val dYear = localDate.year
            it.userId == userId && month == dMonth && year == dYear
        }
            .map { it.toGqlType() }
    }

    @DgsQuery
    fun trackedDay(
        dfe: DataFetchingEnvironment,
        @InputArgument id: String? = null,
    ): TrackedDay? {

        val userId = getUser(dfe)

        val builder = BooleanBuilder()
            .and(QETrackedDay.eTrackedDay.userId.eq(userId))

        id?.let {
            builder.and(QETrackedDay.eTrackedDay.id.eq(id))
        }

        return repository.findByIdOrNull(id)
            ?.toGqlType()
    }

    @DgsQuery
    fun trackedDaysForMonthYear(
        dfe: DataFetchingEnvironment,
        @InputArgument month: Int,
        @InputArgument year: Int
    ): List<TrackedDay> {

        val userId = getUser(dfe)

//        val builder = BooleanBuilder()
//            .and(QETrackedDay.eTrackedDay.userId.eq(userId))
//
//        builder.and(
//            Expressions.dateTemplate(String::class.java, "{0}", QETrackedDay.eTrackedDay.date)
//                .month()
//                .eq(month)
//        )


        val result = repository.findByMonthAndYear(month, year, userId)

        return dgsQuery(dfe) {
            result

        }

//        return dgsQuery(dfe) {
//            repository.findAll(builder)
//        }

    }

    @DgsQuery
    fun trackedDaysPaginated(
        dfe: DataFetchingEnvironment,
        @InputArgument after: String?,
        @InputArgument first: Int?,
    ): Connection<TrackedDay> {

        // TODO make sort direction an InputArgument

        val userId = getUser(dfe)

        return fetchPaginated(
            dfe,
            repository,
            "date",
            Sort.Direction.DESC,
            after,
            first,
            QETrackedDay.eTrackedDay.userId.eq(userId)
        )
    }


    @DgsMutation
    fun createTrackedDay(
        dfe: DataFetchingEnvironment,
        @InputArgument date: Double, // not confusing at all, graphql's Float is passed in as a Double
        @InputArgument mode: String?,
    ): TrackedDay {
        val userId = getUser(dfe)

        if (!userRepository.existsById(userId)) {
            throw DgsInvalidInputArgumentException("Couldn't find User[${userId}]")
        }

        // make sure we can't re-create a TrackedDay for a user with an existing date
        if (repository.exists(
                BooleanBuilder()
                    .and(QETrackedDay.eTrackedDay.userId.eq(userId))
                    .and(QETrackedDay.eTrackedDay.date.eq(Date(date.toLong())))
            )
        ) {
            throw DgsInvalidInputArgumentException("Date[${Date(date.toLong())} already exists")
        }

        val d = Date(date.toLong())
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.time = d
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)

        val dayMode = if (mode != null) DayMode.valueOf(mode) else DayMode.NORMAL


        return dgsMutate(dfe) {
            val result = repository.save(
                ETrackedDay(
                    date = d,
                    week = week,
                    year = year,
                    mode = dayMode,
                    userId = userId,
                )
            )
            trackedDayStream.next(result)

            result
        }
    }

    @DgsMutation
    fun deleteTrackedDay(@InputArgument id: String): String? {
        return delete(repository, QETrackedDay.eTrackedDay.id, id)
    }

    @DgsMutation
    fun updateTrackedDay(
        dfe: DataFetchingEnvironment,
        @InputArgument id: String,
        @InputArgument mode: DayMode? = null,
        @InputArgument date: Double? = null,
        @InputArgument trackedTaskIds: List<String>? = null,
    ): TrackedDay {
        val record = repository.findById(id)
            .map { it }
            .orElseThrow {
                DgsInvalidInputArgumentException("Couldn't find TrackedDay[${id}]")
            }

        return dgsMutate(dfe) {
            updateTrackedDay(record, mode, date, trackedTaskIds)
        }
    }

    fun updateTrackedDay(
        trackedDay: ETrackedDay,
        mode: DayMode? = null,
        date: Double? = null,
        trackedTaskIds: List<String>? = null,
    ): ETrackedDay {

        val dateCopy = if (date != null) Date(date.toLong()) else trackedDay.date

        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.time = dateCopy
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)

        val copy = trackedDay.copy(
            mode = mode ?: trackedDay.mode,
            date = dateCopy,
            week = week,
            year = year,
            trackedTaskIds = trackedTaskIds ?: trackedDay.trackedTaskIds
        )
        val result = repository.save(copy)

        trackedDayStream.next(result)

        return result
    }

    //
    // Document References (relationships)
    // -------------------------------------------------------------------------
    /**
     * This Data Fetcher is for the TrackedDay type's User field. When graphQl requests the
     * User field on a TrackedDay type we'll pass it onto the "users" DataLoader in order
     * to batch the fetches
     */
    @DgsData(
        parentType = DgsConstants.TRACKEDDAY.TYPE_NAME,
        field = DgsConstants.TRACKEDDAY.User
    )
    fun relatedUsers(dfe: DataFetchingEnvironment): CompletableFuture<User> {
        return dgsData<User, TrackedDay/*, ETrackedDay*/>(
            dfe,
            DATA_LOADER_FOR_USERS
        ) { trackedDay ->
            trackedDay.id
        }
    }

    @DgsData(
        parentType = DgsConstants.TRACKEDDAY.TYPE_NAME,
        field = DgsConstants.TRACKEDDAY.TrackedTasks
    )
    fun relatedTrackedTasks(dfe: DataFetchingEnvironment): CompletableFuture<List<TrackedTask>> {
        return dgsData<List<TrackedTask>, TrackedDay>(
            dfe,
            DATA_LOADER_FOR_TRACKED_TASKS
        ) { trackedDay ->
            trackedDay.id
        }
    }

    @DgsData(
        parentType = DgsConstants.TRACKEDDAY.TYPE_NAME,
        field = DgsConstants.TRACKEDDAY.TimeCharges
    )
    fun relatedTimeCharges(dfe: DataFetchingEnvironment): CompletableFuture<List<TimeCharge>> {
        return dgsData<List<TimeCharge>, TrackedDay>(
            dfe,
            DATA_LOADER_FOR_TIME_CHARGES
        ) { trackedDay ->
            trackedDay.id
        }
    }

    @DgsData(
        parentType = DgsConstants.TRACKEDDAY.TYPE_NAME,
        field = DgsConstants.TRACKEDDAY.TimeChargeTotals
    )
    fun relatedTimeChargeTotals(dfe: DataFetchingEnvironment): CompletableFuture<List<TimeChargeTotal>> {
        return dgsData<List<TimeChargeTotal>, TrackedDay>(
            dfe,
            DATA_LOADER_FOR_TIME_CHARGE_TOTALS
        ) { trackedDay ->
            trackedDay.id
        }
    }

    //
    // Data Loaders. These functions will batch request other entities, if these
    // entities were deployed on different servers, we'd be fetching them
    // remotely from here
    // -------------------------------------------------------------------------
    /**
     * TODO unsure if use of DgsDataLoaders is useful in this small app vs just using Mongo/Spring
     *  to eagerly load all relationships every time. Probably no noticeable performance for this
     *  application. But a good study exercise.
     */
    /**
     * This data-loader will batch load User objects from a list of trackedDay IDs. We need to use
     * MappedBatchLoader as not every user may have a tracked day
     */
    @DgsDataLoader(name = DATA_LOADER_FOR_USERS, caching = true)
    val userBatchLoader = MappedBatchLoaderWithContext<String, User?> { trackedDayIds, env ->
        CompletableFuture.supplyAsync {

            // Relationship: Many to One

            val customContext = DgsContext.getCustomContext<CustomContext>(env)

            val trackedDayToUserIdMap = getEntitiesFromEnv<String, ETrackedDay>(env, trackedDayIds) {
                it.userId
            }
            val userMap = userRepository.findAllById(
                trackedDayToUserIdMap.values.toList()
            )
                .associateBy { it.id }

            // TODO make this common somehow, probably by moving everything into the common supplier and have it return
            //  Pair<Collection<E>, R>
            // pass down to next level if needed
            customContext.entities.addAll(userMap.values)

            val result =
                trackedDayToUserIdMap.keys.associateWith { trackedDayId ->
                    val user =
                        trackedDayToUserIdMap[trackedDayId]?.let { userMap[it] }
                    user?.toGqlType()
                }

            // LEARN: @ is a label marker and @supplyAsync is an implicit label that has the same
            //  name as the function to which the lambda is passed. We can omit the return statement
            //  altogether as well and simply have 'assocateBy', or go further and remove the
            //  associateBy val
            return@supplyAsync result
        }
    }

    @DgsDataLoader(name = DATA_LOADER_FOR_TRACKED_TASKS, caching = true)
    val loadForTrackedDayBatchLoader =
        MappedBatchLoaderWithContext<String, List<TrackedTask>> { trackedDayIds, environment ->
            CompletableFuture.supplyAsync {
                // Relationship: One to Many

                val customContext = DgsContext.getCustomContext<CustomContext>(environment)

                // For One-To-Many relationships it's quicker to just look up the One side and groupBy at the end
                // instead of using the context object
                val trackedTasks = trackedTaskRepository
                    .findAll(
                        QETrackedTask.eTrackedTask.trackedDayId
                            .`in`(trackedDayIds)

                    )

                customContext.entities.addAll(trackedTasks)

                trackedTasks.groupBy({ it.trackedDayId }, { it.toGqlType() })
            }
        }

    @DgsDataLoader(name = DATA_LOADER_FOR_TIME_CHARGES, caching = true)
    val loadTimeChargesForTrackedDayBatchLoader =
        MappedBatchLoaderWithContext<String, List<TimeCharge>> { trackedDayIds, environment ->
            CompletableFuture.supplyAsync {
                // Relationship: One to Many
                val customContext = DgsContext.getCustomContext<CustomContext>(environment)

                val timeCharges =
                    timeChargeRepository.findAll(QETimeCharge.eTimeCharge.trackedDayId.`in`(trackedDayIds))

                customContext.entities.addAll(timeCharges)

                timeCharges.groupBy({ it.trackedDayId }, { it.toGqlType() })
            }
        }

    @DgsDataLoader(name = DATA_LOADER_FOR_TIME_CHARGE_TOTALS, caching = true)
    val loadTimeChargeTotalsForTrackedDayBatchLoader =
        MappedBatchLoaderWithContext<String, List<TimeChargeTotal>> { trackedDayIds, environment ->
            CompletableFuture.supplyAsync {
                // Relationship: One to Many
                val customContext = DgsContext.getCustomContext<CustomContext>(environment)

                val timeChargeTotals = timeChargeTotalRepository.findAll(
                    QETimeChargeTotal.eTimeChargeTotal.trackedDayId
                        .`in`
                            (trackedDayIds)
                )

                customContext.entities.addAll(timeChargeTotals)

                timeChargeTotals.groupBy({ it.trackedDayId }, { it.toGqlType() })
            }
        }
}