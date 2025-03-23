package io.fibril.ganglion.clientServer.v1.media

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.fibril.ganglion.clientServer.v1.media.dtos.CreateMediaVersionDTO
import io.fibril.ganglion.clientServer.v1.media.models.MediaVersion
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.FileUpload
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.DatabaseException


interface MediaVersionService : Service<MediaVersion> {
    suspend fun saveOriginalMediaVersion(mediaId: String, fileUpload: FileUpload): Future<MediaVersion>
    suspend fun findAllByMediaId(mediaId: String): Future<List<MediaVersion>>
}

class MediaVersionServiceImpl @Inject constructor(
    private val mediaVersionRepository: MediaVersionRepositoryImpl
) : MediaVersionService {

    override val identifier = IDENTIFIER

    override suspend fun saveOriginalMediaVersion(
        mediaId: String,
        fileUpload: FileUpload
    ): Future<MediaVersion> {
        val createMediaVersionDTO = CreateMediaVersionDTO(
            JsonObject.of(
                "media_id", mediaId,
                "uploaded_filename", fileUpload.uploadedFileName(),
                "name", "original",
                "animated", setOf("image/gif", "image/apng", "image/webp").contains(fileUpload.contentType()),
                "file_size", fileUpload.size(),
            ),
            null
        )
        return create(createMediaVersionDTO)
    }


    override suspend fun create(dto: DTO): Future<MediaVersion> {
        val mediaVersion = try {
            mediaVersionRepository.save(dto)
        } catch (e: DatabaseException) {
            return Future.failedFuture(
                RequestException(
                    statusCode = 500,
                    e.message ?: ErrorCodes.M_UNKNOWN.name,
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN, e.message).asJson()
                )
            )
        }
        return Future.succeededFuture(mediaVersion)
    }

    override suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<MediaVersion>> {
        TODO()
    }

    override suspend fun findAllByMediaId(mediaId: String): Future<List<MediaVersion>> {
        val mediaVersions = try {
            mediaVersionRepository.findAllByMediaId(mediaId)
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }

        return Future.succeededFuture(mediaVersions)
    }

    override suspend fun findOne(id: String): Future<MediaVersion?> {
        val mediaVersion = try {
            mediaVersionRepository.find(id)
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }
        return Future.succeededFuture(mediaVersion)
    }

    override suspend fun update(id: String, dto: DTO): Future<MediaVersion> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String): Future<MediaVersion> {
        TODO("Not yet implemented")
    }

    companion object {
        const val IDENTIFIER = "v1.media.MediaVersionService"
    }
}