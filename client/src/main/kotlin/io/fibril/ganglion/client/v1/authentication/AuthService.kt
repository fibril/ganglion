package io.fibril.ganglion.client.v1.authentication

import com.google.inject.Inject
import io.fibril.ganglion.authentication.GanglionJWTAuthProviderImpl
import io.fibril.ganglion.authentication.TokenType
import io.fibril.ganglion.client.DTO
import io.fibril.ganglion.client.Service
import io.fibril.ganglion.client.errors.ErrorCodes
import io.fibril.ganglion.client.errors.RequestException
import io.fibril.ganglion.client.errors.StandardErrorResponse
import io.fibril.ganglion.client.utils.Utils
import io.fibril.ganglion.client.v1.authentication.dtos.LoginDTO
import io.fibril.ganglion.client.v1.devices.DeviceRepository
import io.fibril.ganglion.client.v1.devices.dtos.CreateDeviceDTO
import io.fibril.ganglion.client.v1.users.UserRepository
import io.fibril.ganglion.client.v1.users.models.MatrixUserId
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import org.mindrot.jbcrypt.BCrypt
import java.util.*

interface AuthService : Service<Any> {
    suspend fun login(loginDTO: LoginDTO): Future<JsonObject>
}

class AuthServiceImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val deviceRepository: DeviceRepository,
    private val ganglionJWTAuthProvider: GanglionJWTAuthProviderImpl
) : AuthService {

    companion object {
        const val IDENTIFIER = "v1.authentication.AuthService"

        fun generatePasswordHash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt());
    }

    override suspend fun login(loginDTO: LoginDTO): Future<JsonObject> {
        val loginType = loginDTO.json.getString("type") ?: return Future.failedFuture(
            RequestException(
                statusCode = 400,
                "Invalid login type",
                StandardErrorResponse(ErrorCodes.M_INVALID_PARAM, error = "Invalid login type").asJson()
            )
        )

        if (!AuthController.supportedLoginTypes.contains(loginType)) {
            return Future.failedFuture(
                RequestException(
                    statusCode = 400,
                    "Invalid login type",
                    StandardErrorResponse(ErrorCodes.M_INVALID_PARAM, error = "Invalid login type").asJson()
                )
            )
        }

        val domain = ResourceBundle.getBundle("application").getString("domain")
        val username = loginDTO.json.getJsonObject("identifier").getString("user")
        val matrixUserId = MatrixUserId(username, domain)
        val user = userRepository.find(matrixUserId.toString())
            ?: return Future.failedFuture(
                RequestException(
                    statusCode = 403,
                    "User does not exist",
                    StandardErrorResponse(ErrorCodes.M_FORBIDDEN, error = "User does not exist").asJson()
                )
            )

        val password =
            authRepository.findPassword(user.asJson().getString("password_id"))
                ?: return Future.failedFuture(
                    RequestException(
                        statusCode = 403,
                        "Invalid password",
                        StandardErrorResponse(ErrorCodes.M_FORBIDDEN, error = "Login failed").asJson()
                    )
                )


        if (BCrypt.checkpw(loginDTO.json.getString("password"), password.hash)) {
            val deviceId = loginDTO.json.getString("device_id")
            val device = if (deviceId != null) {
                deviceRepository.find(deviceId) ?: deviceRepository.save(
                    CreateDeviceDTO(
                        JsonObject()
                            .put("device_id", deviceId)
                            .put(
                                "display_name",
                                loginDTO.json.getString("initial_device_display_name") ?: "Generic Device"
                            )
                            .put("user_id", matrixUserId.toString())
                    )
                )
            } else deviceRepository.save(
                CreateDeviceDTO(
                    JsonObject()
                        .put("device_id", Utils.idGenerator())
                        .put("display_name", loginDTO.json.getString("initial_device_display_name") ?: "Generic Device")
                        .put("user_id", matrixUserId.toString())
                )
            )

            val tokenData = JsonObject.of("sub", matrixUserId.toString())
            val accessToken = ganglionJWTAuthProvider.generateToken(tokenData, TokenType.ACCESS)
            val refreshToken = ganglionJWTAuthProvider.generateToken(tokenData, TokenType.ACCESS)

            return Future.succeededFuture(
                JsonObject()
                    .put("access_token", accessToken)
                    .put("refresh_token", refreshToken)
                    .put("user_id", matrixUserId.toString())
                    .put("device_id", device?.asJson()?.getString("id"))
            )
        }

        return Future.failedFuture(
            RequestException(
                statusCode = 403,
                "Invalid password",
                StandardErrorResponse(ErrorCodes.M_FORBIDDEN, error = "Login failed").asJson()
            )
        )

    }

    override val identifier = IDENTIFIER


    override suspend fun create(dto: DTO): Future<Any> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun findOne(id: String): Future<Any> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun findAll(): Future<List<Any>> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun update(id: String, dto: DTO): Future<Any> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun remove(id: String): Future<Boolean> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }
}