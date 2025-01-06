package v1.users.repositories

import Repository
import com.google.inject.Inject
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred
import utils.ResourceBundleConstants
import v1.users.models.UserProfile

interface UserProfileRepository : Repository<UserProfile> {

}

class UserProfileRepositoryImpl @Inject constructor(private val database: PGDatabase) : UserProfileRepository {
    override suspend fun find(id: String): UserProfile? {
        val client = database.client()
        val queryResult = client.preparedQuery(FIND_USER_PROFILE_QUERY).execute(Tuple.of(id))
            .toCompletionStage().asDeferred()

        val rowSet = queryResult.await()

        return try {
            val userProfileJson = rowSet.first().toJson()
            UserProfile.fromJson(userProfileJson)
        } catch (e: NoSuchElementException) {
            null
        } finally {
            client.close()
        }
    }

    override suspend fun findAll(): List<UserProfile> {
        TODO("Not yet implemented")
    }

    companion object {
        val FIND_USER_PROFILE_QUERY = ResourceBundleConstants.userProfileQueries.getString("findUserProfile")
    }

}