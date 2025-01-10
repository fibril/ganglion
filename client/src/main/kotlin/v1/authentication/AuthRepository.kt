package v1.authentication

import DTO
import Repository
import com.google.inject.Inject
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred
import org.mindrot.jbcrypt.BCrypt
import utils.ResourceBundleConstants
import v1.users.models.MatrixUserId


interface AuthRepository : Repository<Any> {
    suspend fun savePasswordForUser(matrixUserId: MatrixUserId, password: String): JsonObject?
}

class AuthRepositoryImpl @Inject constructor(private val database: PGDatabase) : AuthRepository {
    override suspend fun savePasswordForUser(matrixUserId: MatrixUserId, password: String): JsonObject? {
        val client = database.client()
        val passwordHash: String = BCrypt.hashpw(password, BCrypt.gensalt());
        val queryResult =
            client.preparedQuery(SAVE_PASSWORD_QUERY).execute(Tuple.of(matrixUserId.toString(), passwordHash))
                .toCompletionStage().asDeferred()

        val rowSet = queryResult.await()

        return try {
            return rowSet.first().toJson()
        } catch (e: NoSuchElementException) {
            null
        } finally {
            client.close()
        }
    }

    override suspend fun save(dto: DTO): Any {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun find(id: String): Any {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun findAll(): List<Any> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    companion object {
        val SAVE_PASSWORD_QUERY = ResourceBundleConstants.authQueries.getString("savePassword")
    }
}