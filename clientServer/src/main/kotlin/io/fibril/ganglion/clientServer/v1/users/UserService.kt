package io.fibril.ganglion.clientServer.v1.users

import com.google.inject.Inject
import io.fibril.ganglion.authentication.GanglionJWTAuthProviderImpl
import io.fibril.ganglion.authentication.TokenType
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.fibril.ganglion.clientServer.v1.authentication.models.AuthDatabaseActions
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.fibril.ganglion.clientServer.v1.users.models.User
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
        val params = dto.params()
        val username = params.getString("username")
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

            val userJson = user.asJson()

            if (params.getBoolean("inhibit_login") ?: true) {
                return Future.succeededFuture(user)
            }

            val tokenData = JsonObject().put("sub", matrixUserId.toString())
                .put("role", userJson.getString("role"))

            return Future.succeededFuture(
                User(
                    userJson.put(
                        "access_token",
                        jwtAuthProvider.generateToken(
                            tokenData,
                            TokenType.ACCESS,
                            notificationChannelName = AuthDatabaseActions.TOKEN_CREATED
                        )
                    ).put(
                        "refresh_token",
                        jwtAuthProvider.generateToken(
                            tokenData,
                            TokenType.REFRESH,
                            notificationChannelName = AuthDatabaseActions.TOKEN_CREATED
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

    override suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<User>> {
        TODO("Not yet implemented")
    }

    override suspend fun findOne(id: String): Future<User?> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, updateUserDTO: DTO): Future<User> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String): Future<User> {
        TODO("Not yet implemented")
    }

    companion object {
        const val IDENTIFIER = "v1.users.UserService"
    }
}