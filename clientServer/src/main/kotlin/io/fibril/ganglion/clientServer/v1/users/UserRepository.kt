package io.fibril.ganglion.clientServer.v1.users

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.extensions.exclude
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.utils.Utils
import io.fibril.ganglion.clientServer.v1.authentication.AuthServiceImpl
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.fibril.ganglion.clientServer.v1.users.models.User
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.DatabaseException
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import java.util.*


interface UserRepository : Repository<User>

class UserRepositoryImpl @Inject constructor(private val database: PGDatabase) : UserRepository {
    override suspend fun save(dto: DTO): User {
        val pool = database.pool()

        val params = dto.params()

        val userId = MatrixUserId(
            params.getString("username"),
            ResourceBundle.getBundle("application").getString("domain")
        ).toString()

        val result: Promise<JsonObject> = Promise.promise()
        var error: DatabaseException? = null

        pool.connection
            .onSuccess { conn ->
                conn.begin()
                    .compose { tx ->
                        conn
                            .preparedQuery(CREATE_USER_QUERY)
                            .execute(Tuple.of(userId))
                            .compose { createUserResponse ->

                                conn
                                    .preparedQuery(CREATE_PASSWORD_QUERY)
                                    .execute(
                                        Tuple.of(
                                            userId,
                                            AuthServiceImpl.generatePasswordHash(params.getString("password"))
                                        )
                                    )
                                    .compose { createPasswordResponse ->
                                        conn
                                            .preparedQuery(CREATE_DEVICE_QUERY)
                                            .execute(
                                                Tuple.of(
                                                    params.getString("device_id") ?: Utils.idGenerator(),
                                                    userId,
                                                    params.getString("initial_device_display_name")
                                                )
                                            )
                                    }

                            } // Commit the transaction
                            .compose { createDeviceResponse ->
                                run {
                                    val txResponse = tx.commit()
                                    val obj =
                                        User(
                                            userId,
                                            createDeviceResponse.first().toJson()
                                                .apply {
                                                    put("device_id", this.getValue("id"))
                                                }
                                                .exclude("id") //exclude device.id
                                        ).asJson()
                                    result.complete(obj)
                                    txResponse
                                }
                            }


                    }
                    .eventually { v -> conn.close() }
                    .onSuccess { }
                    .onFailure { err ->
                        run {
                            error = PgException(
                                err.message,
                                "SEVERE",
                                "500",
                                err.message
                            )
                            result.fail(error);
                        }
                    }
            }

        val resPayload = result.future().toCompletionStage().await()

        if (resPayload == null && error != null) {
            throw error!!
        }

        return User(resPayload)
    }

    override suspend fun find(id: String): User? {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()
        client.preparedQuery(FIND_USER_QUERY).execute(Tuple.of(id))
            .eventually { v -> client.close() }
            .onFailure { err -> throw err }
            .onSuccess { res ->
                run {
                    try {
                        result.complete(res.first().toJson())
                    } catch (e: NoSuchElementException) {
                        result.complete(null)
                    }
                }
            }


        val resPayload = result.future().toCompletionStage().await() ?: return null

        return User(resPayload)
    }

    override suspend fun findAll(): List<User> {
        val client = database.client()
        val queryResult = client.query(FIND_ALL_USERS_QUERY).execute()
            .toCompletionStage().asDeferred()

        val rowSet = queryResult.await()

        return try {
            rowSet.toList().map { User(it.toJson()) }
        } finally {
            client.close()

        }
    }

    override suspend fun update(id: String, dto: DTO): User? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): User? {
        TODO("Not yet implemented")
    }

    companion object {
        val CREATE_USER_QUERY: String = ResourceBundleConstants.userQueries.getString("createUser")
        val CREATE_PASSWORD_QUERY: String = ResourceBundleConstants.authQueries.getString("savePassword")
        val CREATE_DEVICE_QUERY: String = ResourceBundleConstants.deviceQueries.getString("createDevice")

        val FIND_USER_QUERY: String = ResourceBundleConstants.userQueries.getString("findUser")
        val FIND_ALL_USERS_QUERY: String = ResourceBundleConstants.userQueries.getString("findAllUsers")
    }

}