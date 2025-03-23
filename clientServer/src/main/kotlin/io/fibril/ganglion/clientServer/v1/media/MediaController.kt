package io.fibril.ganglion.clientServer.v1.media

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.extensions.addRequestRateLimiter
import io.fibril.ganglion.clientServer.extensions.authenticatedRoute
import io.fibril.ganglion.clientServer.extensions.useDTOValidation
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.utils.Utils
import io.fibril.ganglion.clientServer.utils.rateLimiters.MediaRequestRateLimiter
import io.fibril.ganglion.clientServer.v1.authentication.RoleType
import io.fibril.ganglion.clientServer.v1.media.dtos.*
import io.fibril.ganglion.clientServer.v1.media.models.MediaUri
import io.fibril.ganglion.clientServer.v1.media.models.MediaVersion
import io.vertx.core.Vertx
import io.vertx.core.file.AsyncFile
import io.vertx.core.file.OpenOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.streams.Pump
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

internal class MediaController @Inject constructor(val vertx: Vertx, val mediaService: MediaService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {
        router.route().handler(BodyHandler.create())

        router.get(MEDIA_CONFIG_PATH)
            .addRequestRateLimiter(MediaRequestRateLimiter.getInstance())
            .authenticatedRoute(RoleType.USER)
            .handler(::getMediaConfig)

        router.get(GET_PREVIEW_URL_PATH)
            .addRequestRateLimiter(MediaRequestRateLimiter.getInstance())
            .useDTOValidation(GetMediaPreviewUrlDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::getMediaPreviewUrl)

        router.get(DOWNLOAD_ORIGINAL_MEDIA_PATH)
            .addRequestRateLimiter(MediaRequestRateLimiter.getInstance())
            .useDTOValidation(DownloadMediaDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::downloadMedia)

        router.get(DOWNLOAD_ORIGINAL_MEDIA_WITH_FILENAME_PATH)
            .addRequestRateLimiter(MediaRequestRateLimiter.getInstance())
            .useDTOValidation(DownloadMediaDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::downloadMedia)

        router.get(DOWNLOAD_THUMBNAIL_MEDIA_PATH)
            .addRequestRateLimiter(MediaRequestRateLimiter.getInstance())
            .useDTOValidation(DownloadThumbnailDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::downloadThumbnail)

        return router
    }

    fun mountMediaCreateAndUploadRoutes(): Router {
        router.post(CREATE_MEDIA_PATH)
            .addRequestRateLimiter(MediaRequestRateLimiter.getInstance())
            .authenticatedRoute(RoleType.USER)
            .handler(::createMedia)

        router.post(UPLOAD_MEDIA_PATH)
            .addRequestRateLimiter(MediaRequestRateLimiter.getInstance())
            .authenticatedRoute(RoleType.USER)
            .handler(::uploadMedia)

        router.put(PUT_MEDIA_PATH)
            .addRequestRateLimiter(MediaRequestRateLimiter.getInstance())
            .useDTOValidation(PutMediaDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::putMediaData)
        return router
    }

    private fun getMediaConfig(routingContext: RoutingContext) {
        routingContext.end(
            mediaService.getMediaConfig().toString()
        )
    }

    private fun createMedia(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val createMediaDTO = CreateMediaDTO(JsonObject(), routingContext.user())
            mediaService.create(createMediaDTO)
                .onSuccess { media ->
                    routingContext.end(
                        JsonObject.of(
                            "content_uri", media.uri,
                            "unused_expires_at", media.unused_expires_at
                        )
                            .toString()
                    )
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun uploadMedia(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val uploadMediaDTO = UploadMediaDTO(
                JsonObject.mapFrom(routingContext.queryParams())
                    .put(
                        "Content-Type",
                        routingContext.parsedHeaders()?.contentType()?.value() ?: "application/octet-stream"
                    ),
                routingContext.user()
            )
            val fileUploads = routingContext.fileUploads()
            if (fileUploads.isEmpty()) {
                routingContext.response().setStatusCode(400)
                routingContext.end(
                    StandardErrorResponse(
                        ErrorCodes.M_INVALID_PARAM,
                        additionalKeyValues = JsonObject().put("error", "No file attached")
                    ).toString()
                )
                return@usingCoroutineScopeWithIODispatcher
            }
            println("fileUploads ${fileUploads.map { it.fileName() + ":" + it.name() }}")
            mediaService.uploadMedia(fileUploads, uploadMediaDTO)
                .onSuccess { mediaList ->
                    if (mediaList.size == 1) {
                        val media = mediaList.first()
                        routingContext.end(
                            JsonObject.of(
                                "content_uri", media.uri,
                                "unused_expires_at", media.unused_expires_at
                            )
                                .toString()

                        )
                    } else {
                        routingContext.end(
                            JsonArray(mediaList.map {
                                JsonObject.of(
                                    "content_uri", it.uri,
                                    "unused_expires_at", it.unused_expires_at
                                )
                            }).toString()
                        )
                    }
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun putMediaData(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val putMediaDTO = PutMediaDTO(
                JsonObject.mapFrom(routingContext.pathParams()),
                routingContext.user()
            )
            println(putMediaDTO.json)
            val fileUploads = routingContext.fileUploads()

            if (fileUploads.isEmpty()) {
                routingContext.end(JsonObject().toString())
                return@usingCoroutineScopeWithIODispatcher
            }

            val fileUpload = fileUploads.first()

            mediaService.updateMediaFileData(fileUpload, putMediaDTO)
                .onSuccess { media ->
                    routingContext.end(JsonObject.of("content_uri", media.uri).toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun getMediaPreviewUrl(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val mediaUri = MediaUri(routingContext.queryParams().get("url"))
            mediaService.findOne(mediaUri.id)
                .onSuccess { media ->
                    if (media == null) {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString())
                    } else {
                        val mediaJson = media.asJson()
                        routingContext.end(
                            JsonObject.of(
                                "matrix:image:size", mediaJson.getInteger("size"),
                                "og:description", mediaJson.getString("description"),
                                "og:image", mediaUri.toString(),
                                "og:image:height", mediaJson.getInteger("height"),
                                "og:image:type", mediaJson.getString("content_type"),
                                "og:image:width", mediaJson.getInteger("width"),
                                "og:title", mediaJson.getString("title")

                            ).toString()
                        )
                    }
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    private fun downloadMedia(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val downloadMediaDTO = DownloadMediaDTO(
                JsonObject.mapFrom(routingContext.pathParams()),
                routingContext.user()
            )

            if (downloadMediaDTO.json.getString("serverName") != ResourceBundleConstants.domain) {
                routingContext.response().setStatusCode(404)
                routingContext.end(StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString())
                return@usingCoroutineScopeWithIODispatcher
            }

            mediaService.findOne(downloadMediaDTO.json.getString("mediaId"))
                .onSuccess { media ->
                    if (media == null) {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString())
                        return@onSuccess
                    }
                    val mediaJson = media.asJson()
                    val remoteUrl = mediaJson.getString("remote_url")
                    val uploadedFileNamePath = mediaJson.getString("uploaded_filename")
                    if (remoteUrl != null) {
                        routingContext.response().setStatusCode(307)
                        routingContext.response().headers().add("Location", remoteUrl)
                        routingContext.end()
                        return@onSuccess
                    }
                    if (uploadedFileNamePath != null) {
                        vertx.fileSystem().open(uploadedFileNamePath, OpenOptions()) { res ->
                            if (res.succeeded()) {
                                val file: AsyncFile = res.result()
                                routingContext.response().setChunked(true)
                                routingContext.response().headers().add(
                                    "Content-Disposition",
                                    "${
                                        mediaJson.getString(
                                            "content_disposition",
                                            "attachment"
                                        )
                                    };filename=${
                                        downloadMediaDTO.json.getString(
                                            ("fileName"),
                                            null
                                        ) ?: mediaJson.getString("filename")
                                    }"
                                )
                                val pump = Pump.pump(file, routingContext.response())
                                pump.start()
                                file.endHandler {
                                    file.close()
                                    routingContext.response().end()
                                }
                            }
                        }
                    } else {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString())
                    }
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }


    private fun downloadThumbnail(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val downloadThumbnailDTO = DownloadThumbnailDTO(
                JsonObject.mapFrom(routingContext.pathParams()).apply {
                    for (kv in routingContext.queryParams()) {
                        put(kv.key, kv.value)
                    }
                },
                routingContext.user()
            )

            if (downloadThumbnailDTO.json.getString("serverName") != ResourceBundleConstants.domain) {
                routingContext.response().setStatusCode(404)
                routingContext.end(StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString())
                return@usingCoroutineScopeWithIODispatcher
            }

            val mediaVersionName = MediaVersion.approximateVersionName(
                downloadThumbnailDTO.json.getString("method", ""),
                downloadThumbnailDTO.json.getString("height", "0").toInt()
            )

            println("mediaVersionName $mediaVersionName")
            mediaService.findOneForVersion(downloadThumbnailDTO.json.getString("mediaId"), mediaVersionName)
                .onSuccess { media ->
                    if (media == null) {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString())
                        return@onSuccess
                    }
                    val mediaJson = media.asJson()

                    if (!media.isImage) {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString())
                        return@onSuccess
                    }

                    val remoteUrl = mediaJson.getString("remote_url")
                    val uploadedFileNamePath = mediaJson.getString("uploaded_filename")
                    if (remoteUrl != null) {
                        routingContext.response().setStatusCode(307)
                        routingContext.response().headers().add("Location", remoteUrl)
                        routingContext.end()
                        return@onSuccess
                    }
                    if (uploadedFileNamePath != null) {
                        vertx.fileSystem().open(uploadedFileNamePath, OpenOptions()) { res ->
                            if (res.succeeded()) {
                                val file: AsyncFile = res.result()
                                routingContext.response().setChunked(true)
                                routingContext.response().headers().add(
                                    "Content-Disposition",
                                    "${
                                        mediaJson.getString(
                                            "content_disposition",
                                            "attachment"
                                        )
                                    };filename=${
                                        mediaJson.getString("filename", Utils.idGenerator())
                                    }"
                                )
                                val pump = Pump.pump(file, routingContext.response())
                                pump.start()
                                file.endHandler {
                                    file.close()
                                    routingContext.response().end()
                                }
                            }
                        }
                    } else {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(StandardErrorResponse(ErrorCodes.M_NOT_FOUND).asJson().toString())
                    }
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }
    }

    companion object {
        const val MEDIA_CONFIG_PATH = "/v1/media/config"
        const val CREATE_MEDIA_PATH = "/_matrix/media/v1/create"
        const val UPLOAD_MEDIA_PATH = "/_matrix/media/v3/upload"
        const val PUT_MEDIA_PATH = "/_matrix/media/v3/upload/:serverName/:mediaId"
        const val GET_PREVIEW_URL_PATH = "/v1/media/preview_url"
        const val DOWNLOAD_ORIGINAL_MEDIA_PATH = "/v1/media/download/:serverName/:mediaId"
        const val DOWNLOAD_ORIGINAL_MEDIA_WITH_FILENAME_PATH = "/v1/media/download/:serverName/:mediaId/:fileName"
        const val DOWNLOAD_THUMBNAIL_MEDIA_PATH = "/v1/media/thumbnail/:serverName/:mediaId"
    }
}