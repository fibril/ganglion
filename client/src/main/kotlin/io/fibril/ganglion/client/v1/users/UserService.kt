package io.fibril.ganglion.client.v1.users

import com.google.inject.Inject
import io.fibril.ganglion.authentication.GanglionJWTAuthProviderImpl
import io.fibril.ganglion.authentication.TokenType
import io.fibril.ganglion.client.DTO
import io.fibril.ganglion.client.Service
import io.fibril.ganglion.client.errors.ErrorCodes
import io.fibril.ganglion.client.errors.RequestException
import io.fibril.ganglion.client.errors.StandardErrorResponse
import io.fibril.ganglion.client.v1.users.dtos.CreateUserDTO
import io.fibril.ganglion.client.v1.users.models.MatrixUserId
import io.fibril.ganglion.client.v1.users.models.User
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.sqlclient.DatabaseException
import java.util.*


interface UserService : Service<User> {
    suspend fun isMatrixUserIdAvailable(matrixUserId: MatrixUserId): Boolean
}

class UserServiceImpl @Inject constructor(
    private val userRepository: UserRepositoryImpl,
    private val jwtAuthProvider: GanglionJWTAuthProviderImpl
) : UserService {
    override val identifier = IDENTIFIER

    override suspend fun isMatrixUserIdAvailable(matrixUserId: MatrixUserId): Boolean {
        return userRepository.find(matrixUserId.toString()) == null
    }


    override suspend fun create(dto: DTO): Future<User> {
        val createUserDTO = dto as CreateUserDTO
        val username = createUserDTO.json.getString("username")
        val domain = ResourceBundle.getBundle("application").getString("domain")

        val matrixUserId = try {
            MatrixUserId(username, domain)
        } catch (e: IllegalStateException) {
            return Future.failedFuture(
                RequestException(
                    statusCode = 400,
                    e.message ?: "Invalid UserId",
                    StandardErrorResponse(ErrorCodes.M_INVALID_USERNAME).asJson()
                )
            )
        }
        if (isMatrixUserIdAvailable(matrixUserId)) {
            // user can be created
            val user = try {
                userRepository.save(dto)
            } catch (e: DatabaseException) {
                return Future.failedFuture(
                    RequestException(
                        statusCode = 500,
                        e.message ?: ErrorCodes.M_UNKNOWN.name,
                        StandardErrorResponse(ErrorCodes.M_UNKNOWN, e.message).asJson()
                    )
                )
            }

            if (createUserDTO.json.getBoolean("inhibit_login")) {
                return Future.succeededFuture(user)
            }
            return Future.succeededFuture(
                User.fromJson(
                    user.asJson().put(
                        "access_token",
                        jwtAuthProvider.generateToken(
                            JsonObject().put("sub", matrixUserId.toString()),
                            TokenType.ACCESS
                        )
                    ).put(
                        "refresh_token",
                        jwtAuthProvider.generateToken(
                            JsonObject().put("sub", matrixUserId.toString()),
                            TokenType.REFRESH
                        )
                    )
                )
            )

        }
        return Future.failedFuture(
            RequestException(
                statusCode = 400,
                ErrorCodes.M_INVALID_USERNAME.name,
                StandardErrorResponse(ErrorCodes.M_INVALID_USERNAME).asJson()
            )
        )

    }

    override suspend fun findAll(): Future<List<User>> {
        TODO("Not yet implemented")
    }

    override suspend fun findOne(id: String): Future<User> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, updateUserDTO: DTO): Future<User> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String): Future<Boolean> {
        TODO("Not yet implemented")
    }

    companion object {
        const val IDENTIFIER = "v1.users.UserService"
    }
}