package io.fibril.ganglion.clientServer.v1.media

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.fibril.ganglion.clientServer.v1.media.models.Media
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import java.util.*


interface MediaService : Service<Media> {
    fun getMediaConfig(): JsonObject
}

class MediaServiceImpl @Inject constructor(
    private val repository: MediaRepositoryImpl,
) :
    MediaService {
    override fun getMediaConfig(): JsonObject {
        return JsonObject().put(
            MAX_UPLOAD_SIZE_KEY, ResourceBundle.getBundle("application").getString(
                MAX_UPLOAD_SIZE_KEY
            )
        )
    }

    override val identifier = IDENTIFIER

    override suspend fun create(dto: DTO): Future<Media> {
        TODO("Not yet implemented")
    }

    override suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<Media>> {
        TODO()
    }

    override suspend fun findOne(id: String): Future<Media?> {
        TODO()
    }

    override suspend fun update(id: String, dto: DTO): Future<Media> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String): Future<Media> {
        TODO("Not yet implemented")
    }

    companion object {
        const val IDENTIFIER = "v1.media.MediaService"
        private const val MAX_UPLOAD_SIZE_KEY = "m.upload.size"
    }
}