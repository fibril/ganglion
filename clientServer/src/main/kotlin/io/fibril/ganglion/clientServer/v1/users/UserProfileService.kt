package io.fibril.ganglion.clientServer.v1.users

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.utils.pagination.KeySetPagination
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.fibril.ganglion.clientServer.v1.users.models.UserProfile
import io.vertx.core.Future
import io.vertx.pgclient.PgException
import kotlinx.coroutines.future.await


interface UserProfileService : Service<UserProfile> {
    suspend fun findOneByUserId(userId: MatrixUserId): Future<UserProfile?>
    suspend fun userDirectorySearch(paginationDTO: PaginationDTO): Future<PaginatedResult<UserProfile>>
    suspend fun updateUserProfileByUserId(userId: MatrixUserId, updateUserProfileDTO: DTO): Future<UserProfile>
}

class UserProfileServiceImpl @Inject constructor(
    private val userProfileRepository: UserProfileRepositoryImpl
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

    override suspend fun findOneByUserId(userId: MatrixUserId): Future<UserProfile?> {
        val userProfile = try {
            userProfileRepository.findByUserId(userId.toString())
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

    override suspend fun update(id: String, dto: DTO): Future<UserProfile> {
        val userProfile = try {
            userProfileRepository.update(id, dto)
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

    override suspend fun remove(id: String): Future<UserProfile> {
        TODO("Not yet implemented")
    }


    override suspend fun userDirectorySearch(paginationDTO: PaginationDTO): Future<PaginatedResult<UserProfile>> {
        val keySetPagination = KeySetPagination(paginationDTO)
        val results = try {
            userProfileRepository.findAll(keySetPagination.prepareQuery(UserProfileRepositoryImpl.USER_DIRECTORY_SEARCH_QUERY))
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }
        val paginatedResult = keySetPagination.usingQueryResult<UserProfile>(results).paginatedResult
        return Future.succeededFuture(paginatedResult)
    }

    override suspend fun updateUserProfileByUserId(
        userId: MatrixUserId,
        updateUserProfileDTO: DTO
    ): Future<UserProfile> {
        val userProfile = findOneByUserId(userId).toCompletionStage().await() ?: return Future.failedFuture(
            RequestException(404, "User Not found", StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson())
        )
        val profileId = userProfile.id
        return update(profileId, updateUserProfileDTO)
    }

    companion object {
        const val IDENTIFIER = "v1.users.UserProfileService"
    }
}