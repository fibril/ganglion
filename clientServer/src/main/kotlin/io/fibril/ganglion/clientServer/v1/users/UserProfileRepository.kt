package io.fibril.ganglion.clientServer.v1.users

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.users.models.UserProfile
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred

interface UserProfileRepository : Repository<UserProfile> {

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

    override suspend fun findAll(): List<UserProfile> {
        TODO("Not yet implemented")
    }

    override suspend fun update(dto: DTO): UserProfile? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): UserProfile? {
        TODO("Not yet implemented")
    }

    companion object {
        val FIND_USER_PROFILE_QUERY = ResourceBundleConstants.userProfileQueries.getString("findUserProfile")
    }

}