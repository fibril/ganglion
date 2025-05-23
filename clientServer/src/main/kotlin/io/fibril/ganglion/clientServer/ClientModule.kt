package io.fibril.ganglion.clientServer

import com.google.inject.AbstractModule
import com.google.inject.Provides
import io.fibril.ganglion.authentication.GanglionJWTAuthProviderImpl
import io.fibril.ganglion.clientServer.v1.authentication.AuthRepository
import io.fibril.ganglion.clientServer.v1.authentication.AuthRepositoryImpl
import io.fibril.ganglion.clientServer.v1.authentication.AuthService
import io.fibril.ganglion.clientServer.v1.authentication.AuthServiceImpl
import io.fibril.ganglion.clientServer.v1.authentication.models.Password
import io.fibril.ganglion.clientServer.v1.authentication.models.PasswordModel
import io.fibril.ganglion.clientServer.v1.devices.*
import io.fibril.ganglion.clientServer.v1.filters.FilterRepository
import io.fibril.ganglion.clientServer.v1.filters.FilterRepositoryImpl
import io.fibril.ganglion.clientServer.v1.filters.FilterService
import io.fibril.ganglion.clientServer.v1.filters.FilterServiceImpl
import io.fibril.ganglion.clientServer.v1.filters.models.Filter
import io.fibril.ganglion.clientServer.v1.filters.models.FilterModel
import io.fibril.ganglion.clientServer.v1.media.*
import io.fibril.ganglion.clientServer.v1.media.models.Media
import io.fibril.ganglion.clientServer.v1.media.models.MediaModel
import io.fibril.ganglion.clientServer.v1.media.models.MediaVersion
import io.fibril.ganglion.clientServer.v1.media.models.MediaVersionModel
import io.fibril.ganglion.clientServer.v1.presence.Presence
import io.fibril.ganglion.clientServer.v1.presence.PresenceModel
import io.fibril.ganglion.clientServer.v1.presence.PresenceService
import io.fibril.ganglion.clientServer.v1.presence.PresenceServiceImpl
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventRepository
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventRepositoryImpl
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventService
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventServiceImpl
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEvent
import io.fibril.ganglion.clientServer.v1.roomEvents.models.RoomEventModel
import io.fibril.ganglion.clientServer.v1.rooms.*
import io.fibril.ganglion.clientServer.v1.rooms.models.Room
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAlias
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomAliasModel
import io.fibril.ganglion.clientServer.v1.rooms.models.RoomModel
import io.fibril.ganglion.clientServer.v1.typing.Typing
import io.fibril.ganglion.clientServer.v1.typing.TypingModel
import io.fibril.ganglion.clientServer.v1.typing.TypingService
import io.fibril.ganglion.clientServer.v1.typing.TypingServiceImpl
import io.fibril.ganglion.clientServer.v1.users.*
import io.fibril.ganglion.clientServer.v1.users.models.User
import io.fibril.ganglion.clientServer.v1.users.models.UserModel
import io.fibril.ganglion.clientServer.v1.users.models.UserProfile
import io.fibril.ganglion.clientServer.v1.users.models.UserProfileModel
import io.fibril.ganglion.storage.impl.GanglionOpenSearch
import io.fibril.ganglion.storage.impl.GanglionRedisClient
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
        // MediaVersion
        bind(MediaVersionModel::class.java).to(MediaVersion::class.java)
        bind(MediaVersionService::class.java).to(MediaVersionServiceImpl::class.java)
        bind(MediaVersionRepository::class.java).to(MediaVersionRepositoryImpl::class.java)

        // User
        bind(UserModel::class.java).to(User::class.java)
        bind(UserService::class.java).to(UserServiceImpl::class.java)
        bind(UserRepository::class.java).to(UserRepositoryImpl::class.java)

        // UserProfile
        bind(UserProfileModel::class.java).to(UserProfile::class.java)
        bind(UserProfileService::class.java).to(UserProfileServiceImpl::class.java)
        bind(UserProfileRepository::class.java).to(UserProfileRepositoryImpl::class.java)

        // Room
        bind(RoomModel::class.java).to(Room::class.java)
        bind(RoomService::class.java).to(RoomServiceImpl::class.java)
        bind(RoomRepository::class.java).to(RoomRepositoryImpl::class.java)

        // Room Alias
        bind(RoomAliasModel::class.java).to(RoomAlias::class.java)
        bind(RoomAliasService::class.java).to(RoomAliasServiceImpl::class.java)
        bind(RoomAliasRepository::class.java).to(RoomAliasRepositoryImpl::class.java)

        // RoomEvents
        bind(RoomEventModel::class.java).to(RoomEvent::class.java)
        bind(RoomEventService::class.java).to(RoomEventServiceImpl::class.java)
        bind(RoomEventRepository::class.java).to(RoomEventRepositoryImpl::class.java)

        // Presence
        bind(PresenceModel::class.java).to(Presence::class.java)
        bind(PresenceService::class.java).to(PresenceServiceImpl::class.java)

        // Typing
        bind(TypingModel::class.java).to(Typing::class.java)
        bind(TypingService::class.java).to(TypingServiceImpl::class.java)

        // Filter
        bind(FilterModel::class.java).to(Filter::class.java)
        bind(FilterService::class.java).to(FilterServiceImpl::class.java)
        bind(FilterRepository::class.java).to(FilterRepositoryImpl::class.java)
    }

    @Provides
    fun provideDatabase() = PGDatabase(vertx)


    @Provides
    fun provideRedisClient() = GanglionRedisClient(vertx)

    @Provides
    fun provideVertx() = vertx

    @Provides
    fun provideGanglionJWTAuthProviderImpl() = GanglionJWTAuthProviderImpl(vertx)

    @Provides
    fun providesOpenSearch() = GanglionOpenSearch()
}