package v1.users.repositories

import Repository
import com.google.inject.Inject
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred
import utils.ResourceBundleConstants
import v1.users.models.User

interface UserRepository : Repository<User> {
}

class UserRepositoryImpl @Inject constructor(private val database: PGDatabase) : UserRepository {

    override suspend fun find(id: String): User? {
        val client = database.client()
        val queryResult = client.preparedQuery(FIND_USER_QUERY).execute(Tuple.of(id))
            .toCompletionStage().asDeferred()

        val rowSet = queryResult.await()

        return try {
            val userJson = rowSet.first().toJson()
            User.fromJson(userJson)
        } catch (e: NoSuchElementException) {
            null
        } finally {
            client.close()
        }
    }

    override suspend fun findAll(): List<User> {
        val client = database.client()
        val queryResult = client.query(FIND_ALL_USERS_QUERY).execute()
            .toCompletionStage().asDeferred()

        val rowSet = queryResult.await()

        return try {
            rowSet.toList().map { User.fromJson(it.toJson()) }
        } finally {
            client.close()
        }
    }

    companion object {
        val FIND_USER_QUERY: String = ResourceBundleConstants.userQueries.getString("findUser")
        val FIND_ALL_USERS_QUERY: String = ResourceBundleConstants.userQueries.getString("findAllUsers")
    }

}