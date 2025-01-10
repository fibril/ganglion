package io.fibril.ganglion.client

import io.fibril.ganglion.authentication.JWTAuthProviderImpl
import com.google.inject.AbstractModule
import com.google.inject.Provides
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Vertx
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

class ClientModule(val vertx: Vertx) : AbstractModule() {
    override fun configure() {
        // User
        bind(UserModel::class.java).to(User::class.java)
        bind(UserService::class.java).to(UserServiceImpl::class.java)
        bind(UserRepository::class.java).to(UserRepositoryImpl::class.java)

        // UserProfile
        bind(UserProfileModel::class.java).to(UserProfile::class.java)
        bind(UserProfileService::class.java).to(UserProfileServiceImpl::class.java)
        bind(UserProfileRepository::class.java).to(UserProfileRepositoryImpl::class.java)

        // Media
        bind(MediaModel::class.java).to(Media::class.java)
        bind(MediaService::class.java).to(MediaServiceImpl::class.java)
        bind(MediaRepository::class.java).to(MediaRepositoryImpl::class.java)

    }

    @Provides
    fun provideDatabase() = PGDatabase(vertx)

    @Provides
    fun provideVertx() = vertx

    @Provides
    fun provideJWTAuthProviderImpl() = JWTAuthProviderImpl(vertx)
}