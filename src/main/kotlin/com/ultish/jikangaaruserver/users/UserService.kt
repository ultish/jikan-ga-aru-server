package com.ultish.jikangaaruserver.users

import com.netflix.graphql.dgs.*
import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.exceptions.DgsInvalidInputArgumentException
import com.querydsl.core.BooleanBuilder
import com.ultish.generated.DgsConstants
import com.ultish.generated.types.TrackedDay
import com.ultish.generated.types.User
import com.ultish.jikangaaruserver.contexts.CustomContext
import com.ultish.jikangaaruserver.dataFetchers.delete
import com.ultish.jikangaaruserver.dataFetchers.dgsData
import com.ultish.jikangaaruserver.dataFetchers.dgsQuery
import com.ultish.jikangaaruserver.entities.EUser
import com.ultish.jikangaaruserver.entities.QETrackedDay
import com.ultish.jikangaaruserver.entities.QEUser
import com.ultish.jikangaaruserver.trackedDays.TrackedDayRepository
import graphql.schema.DataFetchingEnvironment
import org.dataloader.MappedBatchLoaderWithContext
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CompletableFuture

@DgsComponent
class UserService {

    companion object {
        const val DATA_LOADER_FOR_TRACKED_DAYS = "trackedDaysForUser"
    }

    @Autowired
    lateinit var repository: UserRepository

    @Autowired()
//   @Qualifier("TrackedDayRepository")
    lateinit var trackedDayRepository: TrackedDayRepository

    @DgsQuery
    fun users(
        dfe: DataFetchingEnvironment,
        @InputArgument username: String?,
    ): List<User> {
        return dgsQuery(dfe) {
            val builder = BooleanBuilder()

            username?.let {
                builder.and(QEUser.eUser.username.equalsIgnoreCase(it))
            }
            repository.findAll(builder)
        }
    }

    @DgsMutation
    fun createUser(
        @InputArgument username: String,
        @InputArgument password: String,
    ): User {
        if (repository.exists(QEUser.eUser.username.eq(username))) {
            throw DgsInvalidInputArgumentException("Username: $username already exists")
        }

        return repository.save(
            EUser(
                username = username,
                password = password, // TODO hash this
                trackedDayIds = listOf()
            )
        )
            .toGqlType()
    }

    @DgsMutation
    fun deleteUser(@InputArgument username: String): String? {
        return delete(repository, QEUser.eUser.username, username)
    }

    @DgsMutation
    fun updateUser(
        @InputArgument userId: String,
        @InputArgument trackedDayIds: List<String>? = null,
    ): User {
        val user = repository.findById(userId)
            .map { it }
            .orElseThrow {
                DgsInvalidInputArgumentException("Couldn't find User[${userId}]")
            }

        return updateUser(user, trackedDayIds)
    }

    fun updateUser(user: EUser, trackedDayIds: List<String>? = null): User {
        println("Updating user[${user.username}] with trackedDayIds[${trackedDayIds}]")
        val copy = user.copy(
            trackedDayIds = trackedDayIds ?: user.trackedDayIds
        )
        return repository.save(copy)
            .toGqlType()
    }

    //
    // Document References (relationships)
    // -------------------------------------------------------------------------
    @DgsData(parentType = DgsConstants.USER.TYPE_NAME, field = DgsConstants.USER.TrackedDays)
    fun trackedDaysForUser(dfe: DataFetchingEnvironment): CompletableFuture<List<TrackedDay>> {
        return dgsData<List<TrackedDay>, User>(dfe, DATA_LOADER_FOR_TRACKED_DAYS) {
            it.id
        }
//      val dataLoader: DataLoader<String, List<TrackedDay>> =
//         dfe.getDataLoader(DATA_LOADER_FOR_TRACKED_DAYS)
//      val user = dfe.getSource<User>()
//      return dataLoader.load(user.id)
    }

    //
    // Data Loaders
    // -------------------------------------------------------------------------
    @DgsDataLoader(name = DATA_LOADER_FOR_TRACKED_DAYS, caching = true)
    val trackedDaysBatchLoader = MappedBatchLoaderWithContext<String, List<TrackedDay>> { userIds, env ->
        CompletableFuture.supplyAsync {

            val customContext = DgsContext.getCustomContext<CustomContext>(env)

            val trackedDays = trackedDayRepository.findAll(QETrackedDay.eTrackedDay.userId.`in`(userIds))
            customContext.entities.addAll(trackedDays)

            trackedDays.groupBy({ it.userId }, { it.toGqlType() })
        }
    }
}