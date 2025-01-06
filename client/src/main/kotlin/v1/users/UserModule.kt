package v1.users

import com.google.inject.AbstractModule
import com.google.inject.Provides
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Vertx
import v1.users.models.User
import v1.users.models.UserModel
import v1.users.models.UserProfile
import v1.users.models.UserProfileModel
import v1.users.repositories.UserProfileRepository
import v1.users.repositories.UserProfileRepositoryImpl
import v1.users.repositories.UserRepository
import v1.users.repositories.UserRepositoryImpl
import v1.users.services.UserProfileService
import v1.users.services.UserProfileServiceImpl
import v1.users.services.UserService
import v1.users.services.UserServiceImpl

class UserModule(val vertx: Vertx) : AbstractModule() {
    override fun configure() {
        bind(UserModel::class.java).to(User::class.java)

        bind(UserService::class.java).to(UserServiceImpl::class.java)

        bind(UserRepository::class.java).to(UserRepositoryImpl::class.java)

        // UserProfile
        bind(UserProfileModel::class.java).to(UserProfile::class.java)

        bind(UserProfileService::class.java).to(UserProfileServiceImpl::class.java)

        bind(UserProfileRepository::class.java).to(UserProfileRepositoryImpl::class.java)

    }

    @Provides
    fun provideDatabase() = PGDatabase(vertx)

    @Provides
    fun provideVertx() = vertx


}