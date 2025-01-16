package io.fibril.ganglion.client

import com.google.inject.AbstractModule
import com.google.inject.Provides
import io.fibril.ganglion.authentication.GanglionJWTAuthProviderImpl
import io.fibril.ganglion.client.v1.authentication.AuthRepository
import io.fibril.ganglion.client.v1.authentication.AuthRepositoryImpl
import io.fibril.ganglion.client.v1.authentication.AuthService
import io.fibril.ganglion.client.v1.authentication.AuthServiceImpl
import io.fibril.ganglion.client.v1.authentication.models.Password
import io.fibril.ganglion.client.v1.authentication.models.PasswordModel
import io.fibril.ganglion.client.v1.devices.*
import io.fibril.ganglion.client.v1.media.MediaRepository
import io.fibril.ganglion.client.v1.media.MediaRepositoryImpl
import io.fibril.ganglion.client.v1.media.MediaService
import io.fibril.ganglion.client.v1.media.MediaServiceImpl
import io.fibril.ganglion.client.v1.media.models.Media
import io.fibril.ganglion.client.v1.media.models.MediaModel
import io.fibril.ganglion.client.v1.users.*
import io.fibril.ganglion.client.v1.users.models.User
import io.fibril.ganglion.client.v1.users.models.UserModel
import io.fibril.ganglion.client.v1.users.models.UserProfile
import io.fibril.ganglion.client.v1.users.models.UserProfileModel
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Vertx

class ClientModule(val vertx: Vertx) : AbstractModule() {
    override fun configure() {

        // Authentication
        bind(AuthService::class.java).to(AuthServiceImpl::class.java)
        bind(AuthRepository::class.java).to(AuthRepositoryImpl::class.java)
        bind(PasswordModel::class.java).to(Password::class.java)


        // Devices
        bind(DeviceModel::class.java).to(Device::class.java)
        bind(DeviceService::class.java).to(DeviceServiceImpl::class.java)
        bind(DeviceRepository::class.java).to(DeviceRepositoryImpl::class.java)


        // Media
        bind(MediaModel::class.java).to(Media::class.java)
        bind(MediaService::class.java).to(MediaServiceImpl::class.java)
        bind(MediaRepository::class.java).to(MediaRepositoryImpl::class.java)

        // User
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

    @Provides
    fun provideGanglionJWTAuthProviderImpl() = GanglionJWTAuthProviderImpl(vertx)
}