package io.fibril.ganglion.clientServer.v1.authentication

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.authentication.models.Password
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred
import org.mindrot.jbcrypt.BCrypt


interface AuthRepository : Repository<Any> {
    suspend fun savePasswordForUser(matrixUserId: MatrixUserId, password: String): JsonObject?
    suspend fun retrievePasswordForUser(matrixUserId: MatrixUserId): Password?
    suspend fun findPassword(id: String): Password?
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

    override suspend fun retrievePasswordForUser(matrixUserId: MatrixUserId): Password? {
        val client = database.client()
        val queryResult =
            client.preparedQuery(FETCH_PASSWORD_FOR_USER_QUERY).execute(Tuple.of(matrixUserId.toString()))
                .toCompletionStage().asDeferred()
        val rowSet = queryResult.await()
        return try {
            return Password(rowSet.first().toJson())
        } catch (e: NoSuchElementException) {
            null
        } finally {
            client.close()
        }
    }

    override suspend fun findPassword(id: String): Password? {
        val client = database.client()
        val queryResult =
            client.preparedQuery(FIND_PASSWORD_BY_ID_QUERY).execute(Tuple.of(id))
                .toCompletionStage().asDeferred()
        val rowSet = queryResult.await()
        return try {
            return Password(rowSet.first().toJson())
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

    override suspend fun update(dto: DTO): Any? {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun delete(id: String): Any? {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    companion object {
        val SAVE_PASSWORD_QUERY = ResourceBundleConstants.authQueries.getString("savePassword")
        val FETCH_PASSWORD_FOR_USER_QUERY = ResourceBundleConstants.authQueries.getString("fetchPasswordForUser")
        val FIND_PASSWORD_BY_ID_QUERY = ResourceBundleConstants.authQueries.getString("findPasswordById")
    }
}