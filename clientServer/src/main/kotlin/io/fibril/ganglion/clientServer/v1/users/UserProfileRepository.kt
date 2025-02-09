package io.fibril.ganglion.clientServer.v1.users

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.users.models.UserProfile
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Promise
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await

interface UserProfileRepository : Repository<UserProfile> {
    suspend fun findByUserId(userId: String): UserProfile?
}

class UserProfileRepositoryImpl @Inject constructor(private val database: PGDatabase) : UserProfileRepository {


    override suspend fun save(dto: DTO): UserProfile {
        TODO("Not yet implemented")
    }

    override suspend fun find(id: String): UserProfile {
        val client = database.client()
        val queryResult = client.preparedQuery(FIND_USER_PROFILE_QUERY).execute(Tuple.of(id))
            .toCompletionStage().asDeferred()

        val rowSet = queryResult.await()

        val userProfileJson = rowSet.first().toJson()
        return UserProfile(userProfileJson)
    }

    override suspend fun findByUserId(userId: String): UserProfile? {
        val client = database.client()
        val result: Promise<UserProfile> = Promise.promise()

        client.preparedQuery(FIND_BY_USER_ID_QUERY).execute(Tuple.of(userId))
            .onSuccess { res ->
                val userProfile: UserProfile? = try {
                    UserProfile(res.first().toJson())
                } catch (e: NoSuchElementException) {
                    result.fail(e)
                    null
                }
                if (userProfile != null) {
                    result.complete(userProfile)
                }
            }.onFailure { err ->
                throw PgException(
                    err.message,
                    "SEVERE",
                    "500",
                    err.message
                )
            }
            .eventually { _ -> client.close() }
        val profile = result.future().toCompletionStage().await()

        return profile
    }

    override suspend fun findAll(): List<UserProfile> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, dto: DTO): UserProfile? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): UserProfile? {
        TODO("Not yet implemented")
    }

    companion object {
        val FIND_USER_PROFILE_QUERY = ResourceBundleConstants.userProfileQueries.getString("findUserProfile")
        val FIND_BY_USER_ID_QUERY = ResourceBundleConstants.userProfileQueries.getString("findByUserId")
    }

}