package io.fibril.ganglion.clientServer.v1.media

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.v1.media.models.Media
import io.fibril.ganglion.storage.impl.PGDatabase

interface MediaRepository : Repository<Media> {

}

class MediaRepositoryImpl @Inject constructor(private val database: PGDatabase) : MediaRepository {
    override suspend fun save(dto: DTO): Media {

        TODO("Not yet implemented")
    }

    override suspend fun find(id: String): Media {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(): List<Media> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, dto: DTO): Media? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): Media? {
        TODO("Not yet implemented")
    }

    companion object {
//        val FIND_USER_PROFILE_QUERY = ResourceBundleConstants.mediaQueries.getString("findUserProfile")
    }

}