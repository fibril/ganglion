package io.fibril.ganglion.clientServer.v1.media

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.fibril.ganglion.clientServer.v1.media.dtos.CreateMediaDTO
import io.fibril.ganglion.clientServer.v1.media.dtos.PutMediaDTO
import io.fibril.ganglion.clientServer.v1.media.dtos.UpdateMediaDTO
import io.fibril.ganglion.clientServer.v1.media.dtos.UploadMediaDTO
import io.fibril.ganglion.clientServer.v1.media.models.Media
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.FileUpload
import io.vertx.pgclient.PgException
import kotlinx.coroutines.future.await


interface MediaService : Service<Media> {
    fun getMediaConfig(): JsonObject
    suspend fun uploadMedia(fileUploads: List<FileUpload>, uploadMediaDTO: UploadMediaDTO): Future<List<Media>>
    suspend fun updateMediaFileData(fileUpload: FileUpload, putMediaDTO: PutMediaDTO): Future<Media>
    suspend fun findOneForVersion(id: String, versionName: String): Future<Media?>
}

class MediaServiceImpl @Inject constructor(
    private val mediaRepository: MediaRepositoryImpl,
    private val mediaVersionService: MediaVersionServiceImpl
) : MediaService {

    override val identifier = IDENTIFIER


    override fun getMediaConfig(): JsonObject {
        return JsonObject().put(
            MAX_UPLOAD_SIZE_KEY, ResourceBundleConstants.applicationBundle.getString(
                MAX_UPLOAD_SIZE_KEY
            )
        )
    }

    override suspend fun uploadMedia(
        fileUploads: List<FileUpload>,
        uploadMediaDTO: UploadMediaDTO
    ): Future<List<Media>> {
        val savedMediaList = mutableListOf<Media>()
        for (fileUpload in fileUploads) {
            val createMediaDTO = CreateMediaDTO(
                JsonObject()
                    .put("filename", fileUpload.name())
                    .put("content_type", fileUpload.contentType())
                    .put(
                        "content_disposition",
                        if (Media.INLINE_CONTENT_TYPE.contains(fileUpload.contentType())) "inline" else "attachment"
                    )
                    .put("charset", fileUpload.charSet())
                    .put("content_transfer_encoding", fileUpload.contentTransferEncoding()),
                uploadMediaDTO.sender
            )
            val media = try {
                mediaRepository.save(createMediaDTO)
            } catch (e: PgException) {
                return Future.failedFuture(RequestException.fromPgException(e))
            }
            val mediaVersion =
                mediaVersionService.saveOriginalMediaVersion(media.id, fileUpload).toCompletionStage().await()
            if (mediaVersion != null) {
                savedMediaList.add(media)
            } else {
                return Future.failedFuture(
                    RequestException(
                        500,
                        "Failed to save file",
                        StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                    )
                )
            }
        }
        return Future.succeededFuture(savedMediaList)
    }

    override suspend fun updateMediaFileData(
        fileUpload: FileUpload,
        putMediaDTO: PutMediaDTO
    ): Future<Media> {
        val mediaId = putMediaDTO.params().getString("mediaId")
        val alreadyUpdated = contentAlreadyUploaded(mediaId)
        if (alreadyUpdated) {
            return Future.failedFuture(
                RequestException(403, "Forbidden", StandardErrorResponse(ErrorCodes.M_FORBIDDEN).asJson())
            )
        }

        val updateMediaDTO = UpdateMediaDTO(
            JsonObject()
                .put("filename", fileUpload.name())
                .put("content_type", fileUpload.contentType())
                .put(
                    "content_disposition",
                    if (Media.INLINE_CONTENT_TYPE.contains(fileUpload.contentType())) "inline" else "attachment"
                )
                .put("charset", fileUpload.charSet())
                .put("content_transfer_encoding", fileUpload.contentTransferEncoding())
                .put("title", putMediaDTO.json.getString("title"))
                .put("description", putMediaDTO.json.getString("description")),
            putMediaDTO.sender
        )

        update(putMediaDTO.json.getString("mediaId"), updateMediaDTO).toCompletionStage().await()

        return saveFileData(
            mediaId,
            fileUpload
        )
    }


    override suspend fun create(dto: DTO): Future<Media> {
        try {
            val media = mediaRepository.save(dto)
            return Future.succeededFuture(media)
        } catch (pgException: PgException) {
            return Future.failedFuture(RequestException.fromPgException(pgException))
        }
    }

    override suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<Media>> {
        TODO()
    }

    override suspend fun findOne(id: String): Future<Media?> {
        val media = try {
            mediaRepository.find(id)
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }
        return Future.succeededFuture(media)
    }

    override suspend fun findOneForVersion(id: String, versionName: String): Future<Media?> {
        val media = try {
            mediaRepository.findOneForVersion(id, versionName)
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }
        return Future.succeededFuture(media)
    }

    override suspend fun update(id: String, dto: DTO): Future<Media> {
        val media = try {
            mediaRepository.update(id, dto)
        } catch (e: PgException) {
            return Future.failedFuture(
                RequestException(
                    500,
                    e.message ?: "Unknown Exception",
                    StandardErrorResponse(ErrorCodes.M_UNKNOWN).asJson()
                )
            )
        }
        return Future.succeededFuture(media)
    }

    override suspend fun remove(id: String): Future<Media> {
        TODO("Not yet implemented")
    }

    private suspend fun contentAlreadyUploaded(id: String): Boolean {
        val mediaVersions = mediaVersionService.findAllByMediaId(id).toCompletionStage().await()
        println(mediaVersions.map { it.asJson() })
        if (mediaVersions == null) {
            return true
        }
        return mediaVersions.isNotEmpty()
    }

    private suspend fun saveFileData(mediaId: String, fileUpload: FileUpload): Future<Media> {
        val promise = Promise.promise<Media>()
        mediaVersionService.saveOriginalMediaVersion(mediaId, fileUpload).onSuccess { mediaVersion ->
            promise.complete(Media(mediaVersion.asJson().getString("media_id")))
        }.onFailure { err ->
            promise.fail(err)
        }
        return promise.future()
    }

    companion object {
        const val IDENTIFIER = "v1.media.MediaService"
        private const val MAX_UPLOAD_SIZE_KEY = "m.upload.size"
    }
}