package io.fibril.ganglion.client.v1.users

import io.fibril.ganglion.client.DTO
import io.fibril.ganglion.client.Service
import com.google.inject.Inject
import io.vertx.core.Future
import kotlinx.coroutines.future.asDeferred
import io.fibril.ganglion.client.v1.users.models.MatrixUserId
import io.fibril.ganglion.client.v1.users.models.UserProfile


interface UserProfileService : Service<UserProfile> {
    suspend fun findOneByUserId(userId: MatrixUserId): UserProfile?
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

    override suspend fun findAll(): Future<List<UserProfile>> {
        TODO()
    }

    override suspend fun findOne(id: String): Future<UserProfile> {
        TODO()
    }

    override suspend fun findOneByUserId(userId: MatrixUserId): UserProfile? {
        val userDeferred = userServiceImpl.findOne(userId.toString()).toCompletionStage().asDeferred()
        val user = userDeferred.await()
        if (user != null) {
            val userProfileId = user.asJson().getString("user_profile_id")
            return if (userProfileId != null)
                findOne(userProfileId).toCompletionStage().asDeferred().await()
            else null
        }
        return null
    }

    override suspend fun update(id: String, updateUserDTO: DTO): Future<UserProfile> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String): Future<Boolean> {
        TODO("Not yet implemented")
    }

    companion object {
        const val IDENTIFIER = "v1.users.UserProfileService"
    }
}