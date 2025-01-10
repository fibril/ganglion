package io.fibril.ganglion.client.v1.users

import io.fibril.ganglion.client.DTO
import io.fibril.ganglion.client.Repository
import com.google.inject.Inject
import io.fibril.ganglion.client.extensions.exclude
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.DatabaseException
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import io.fibril.ganglion.client.utils.ResourceBundleConstants
import io.fibril.ganglion.client.utils.Utils
import io.fibril.ganglion.client.v1.authentication.AuthServiceImpl
import io.fibril.ganglion.client.v1.users.dtos.CreateUserDTO
import io.fibril.ganglion.client.v1.users.models.MatrixUserId
import io.fibril.ganglion.client.v1.users.models.User
import java.util.*


interface UserRepository : Repository<User>

class UserRepositoryImpl @Inject constructor(private val database: PGDatabase) : UserRepository {
    override suspend fun save(dto: DTO): User {
        val pool = database.pool()

        val userId = MatrixUserId(
            (dto as CreateUserDTO).json.getString("username"),
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
                                            AuthServiceImpl.generatePasswordHash(dto.json.getString("password"))
                                        )
                                    )
                                    .compose { createPasswordResponse ->
                                        conn
                                            .preparedQuery(CREATE_DEVICE_QUERY)
                                            .execute(
                                                Tuple.of(
                                                    dto.json.getString("device_id") ?: Utils.idGenerator(),
                                                    userId,
                                                    dto.json.getString("initial_device_display_name")
                                                )
                                            )
                                    }

                            } // Commit the transaction
                            .compose { createDeviceResponse ->
                                run {
                                    val txResponse = tx.commit()
                                    val obj =
                                        User(
                                            MatrixUserId(userId),
                                            createDeviceResponse.first().toJson()
                                                .apply { this.put("device_id", this.getValue("id")) }
                                                .exclude(setOf("id"))
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

        return User.fromJson(jsonObject = resPayload)
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

        return User.fromJson(jsonObject = resPayload)
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
        val CREATE_USER_QUERY: String = ResourceBundleConstants.userQueries.getString("createUser")
        val CREATE_PASSWORD_QUERY: String = ResourceBundleConstants.authQueries.getString("savePassword")
        val CREATE_DEVICE_QUERY: String = ResourceBundleConstants.deviceQueries.getString("createDevice")

        val FIND_USER_QUERY: String = ResourceBundleConstants.userQueries.getString("findUser")
        val FIND_ALL_USERS_QUERY: String = ResourceBundleConstants.userQueries.getString("findAllUsers")
    }

}