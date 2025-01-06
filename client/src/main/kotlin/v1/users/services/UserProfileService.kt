package v1.users.services

import DTO
import Service
import com.google.inject.Inject
import v1.users.models.MatrixUserId
import v1.users.models.UserProfile
import v1.users.repositories.UserProfileRepositoryImpl


interface UserProfileService : Service<UserProfile> {
    suspend fun findOneByUserId(userId: MatrixUserId): UserProfile?
}

class UserProfileServiceImpl @Inject constructor(
    private val repository: UserProfileRepositoryImpl,
    private val userServiceImpl: UserServiceImpl
) :
    UserProfileService {
    override val identifier = IDENTIFIER

    override suspend fun create(dto: DTO): UserProfile {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(): List<UserProfile> {
        return repository.findAll()
    }

    override suspend fun findOne(id: String): UserProfile? {
        return repository.find(id)
    }

    override suspend fun findOneByUserId(userId: MatrixUserId): UserProfile? {
        val user = userServiceImpl.findOne(userId.toString())
        if (user != null) {
            val userProfileId = user.otherJsonObject?.getString("user_profile_id")
            return if (userProfileId != null) findOne(userProfileId) else null
        }
        return null
    }

    override suspend fun update(id: String, updateUserDTO: DTO): UserProfile {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String) {
        TODO("Not yet implemented")
    }

    companion object {
        const val IDENTIFIER = "UserProfileService"
    }
}