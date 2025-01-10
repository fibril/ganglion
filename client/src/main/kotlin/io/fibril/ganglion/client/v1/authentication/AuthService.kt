package io.fibril.ganglion.client.v1.authentication

import io.fibril.ganglion.client.DTO
import io.fibril.ganglion.authentication.JWTAuthProvider
import io.fibril.ganglion.client.Service
import com.google.inject.Inject
import io.vertx.core.Future
import org.mindrot.jbcrypt.BCrypt
import io.fibril.ganglion.client.v1.devices.DeviceRepository
import io.fibril.ganglion.client.v1.users.UserRepository

interface AuthService : Service<Any> {
}

class AuthServiceImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val deviceRepository: DeviceRepository,
    private val jwtAuthProvider: JWTAuthProvider
) : AuthService {

    companion object {
        private const val IDENTIFIER = "v1.authentication.AuthService"

        fun generatePasswordHash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt());
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