package io.fibril.ganglion.clientServer.v1.users

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.fibril.ganglion.clientServer.v1.users.models.UserProfile
import io.vertx.core.Future
import io.vertx.pgclient.PgException


interface UserProfileService : Service<UserProfile> {
    suspend fun findOneByUserId(userId: MatrixUserId): Future<UserProfile>

}

class UserProfileServiceImpl @Inject constructor(
    private val repository: UserProfileRepositoryImpl,
    private val userServiceImpl: UserServiceImpl
) :
    UserProfileService {
    override val identifier = IDENTIFIER

    override suspend fun create(dto: DTO): Future<UserProfile> {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<UserProfile>> {
        TODO()
    }

    override suspend fun findOne(id: String): Future<UserProfile?> {
        TODO()
    }

    override suspend fun findOneByUserId(userId: MatrixUserId): Future<UserProfile> {
        val userProfile = try {
            repository.findByUserId(userId.toString())
        } catch (e: PgException) {
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "Unknown Exception",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }

        return Future.succeededFuture(userProfile)
    }

    override suspend fun update(id: String, updateUserDTO: DTO): Future<UserProfile> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String): Future<UserProfile> {
        TODO("Not yet implemented")
    }

    companion object {
        const val IDENTIFIER = "v1.users.UserProfileService"
    }
}