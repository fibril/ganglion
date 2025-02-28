package io.fibril.ganglion.clientServer.v1.users

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.users.models.UserDatabaseActions
import io.fibril.ganglion.clientServer.v1.users.models.UserProfile
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await

interface UserProfileRepository : Repository<UserProfile> {
    suspend fun findByUserId(userId: String): UserProfile?
}

class UserProfileRepositoryImpl @Inject constructor(private val database: PGDatabase, private val vertx: Vertx) :
    UserProfileRepository {


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
        val result: Promise<UserProfile?> = Promise.promise()

        client.preparedQuery(FIND_BY_USER_ID_QUERY).execute(Tuple.of(userId))
            .onSuccess { res ->
                val userProfile: UserProfile? = run {
                    val profile = res.firstOrNull()
                    if (profile != null) UserProfile(profile.toJson()) else null
                }
                result.complete(userProfile)

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

    override suspend fun findAll(query: String): List<UserProfile> {
        println("query $query")
        val client = database.client()
        val result: Promise<List<UserProfile>> = Promise.promise()

        client.query(query)
            .execute()
            .onSuccess { res ->
                result.complete(res.map { UserProfile(it.toJson()) })
            }
            .onFailure { err ->
                throw PgException(
                    err.message,
                    "SEVERE",
                    "500",
                    err.message
                )
            }
            .eventually { _ -> client.close() }

        val profiles = result.future().toCompletionStage().await()
        return profiles
    }

    override suspend fun update(id: String, dto: DTO): UserProfile? {
        val client = database.client()
        val result: Promise<UserProfile?> = Promise.promise()

        val params = dto.params()
        println("para,s $params")
        val keys = params.map.keys

        val values = mutableListOf<Any>().apply {
            for (key in keys) {
                add("$key = '${params.getValue(key)}'")
            }
        }

        val queryString = """
                UPDATE user_profiles SET ${values.joinToString(", ")}
                WHERE id = '$id' 
                RETURNING *;
            """.trimIndent()

        println("queryString $queryString")

        val displayNameUpdated = keys.contains("display_name")

        client.query(
            queryString
        ).execute()
            .onSuccess { res ->
                val response = res.firstOrNull()?.toJson()
                result.complete(if (response != null) UserProfile(response) else null)
            }
            .onFailure { err ->
                throw PgException(
                    err.message,
                    "SEVERE",
                    "500",
                    err.message
                )
            }
            .eventually { _ -> client.close() }

        val userProfile = result.future().toCompletionStage().await()

        if (userProfile != null && displayNameUpdated) {
            vertx.eventBus().send(UserDatabaseActions.USER_DISPLAY_NAME_CHANGED, userProfile.asJson())
        }

        return userProfile
    }

    override suspend fun delete(id: String): UserProfile? {
        TODO("Not yet implemented")
    }

    companion object {
        val FIND_USER_PROFILE_QUERY = ResourceBundleConstants.userProfileQueries.getString("findUserProfile")
        val FIND_BY_USER_ID_QUERY = ResourceBundleConstants.userProfileQueries.getString("findByUserId")
        val USER_DIRECTORY_SEARCH_QUERY = ResourceBundleConstants.userProfileQueries.getString("userDirectorySearch")
    }

}