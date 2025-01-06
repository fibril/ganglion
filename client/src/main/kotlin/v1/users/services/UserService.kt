package v1.users.services

import DTO
import Service
import com.google.inject.Inject
import v1.users.models.User
import v1.users.repositories.UserRepositoryImpl


interface UserService : Service<User> {

}

class UserServiceImpl @Inject constructor(private val repository: UserRepositoryImpl) : UserService {
    override val identifier = IDENTIFIER

    override suspend fun create(dto: DTO): User {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(): List<User> {
        return repository.findAll()
    }

    override suspend fun findOne(id: String): User? {
        return repository.find(id)
    }

    override suspend fun update(id: String, updateUserDTO: DTO): User {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String) {
        TODO("Not yet implemented")
    }

    companion object {
        const val IDENTIFIER = "UserService"
    }
}