package io.fibril.ganglion.client.v1.media

import io.fibril.ganglion.client.DTO
import io.fibril.ganglion.client.Repository
import com.google.inject.Inject
import io.fibril.ganglion.storage.impl.PGDatabase
import io.fibril.ganglion.client.v1.media.models.Media

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

    companion object {
//        val FIND_USER_PROFILE_QUERY = ResourceBundleConstants.mediaQueries.getString("findUserProfile")
    }

}