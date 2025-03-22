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
import io.fibril.ganglion.clientServer.utils.rateLimiters.MediaRequestRateLimiter
import io.fibril.ganglion.clientServer.v1.authentication.RoleType
import io.fibril.ganglion.clientServer.v1.media.dtos.CreateMediaDTO
import io.fibril.ganglion.clientServer.v1.media.dtos.GetMediaPreviewUrlDTO
import io.fibril.ganglion.clientServer.v1.media.dtos.PutMediaDTO
import io.fibril.ganglion.clientServer.v1.media.dtos.UploadMediaDTO
import io.fibril.ganglion.clientServer.v1.media.models.MediaUri
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

internal class MediaController @Inject constructor(vertx: Vertx, val mediaService: MediaService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {
        router.get(MEDIA_CONFIG_PATH)
            .addRequestRateLimiter(MediaRequestRateLimiter.getInstance())
            .authenticatedRoute(RoleType.USER)
            .handler(::getMediaConfig)

        router.get(GET_PREVIEW_URL_PATH)
            .addRequestRateLimiter(MediaRequestRateLimiter.getInstance())
            .useDTOValidation(GetMediaPreviewUrlDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::getMediaPreviewUrl)

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
                JsonObject.mapFrom(routingContext.queryParams())
                    .put(
                        "Content-Type",
                        routingContext.parsedHeaders()?.contentType()?.value() ?: "application/octet-stream"
                    ),
                routingContext.user()
            )
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
                                "og:image:type", mediaJson.getString("media_type"),
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

    companion object {
        const val MEDIA_CONFIG_PATH = "/v1/media/config"
        const val CREATE_MEDIA_PATH = "/_matrix/media/v1/create"
        const val UPLOAD_MEDIA_PATH = "/_matrix/media/v3/upload"
        const val PUT_MEDIA_PATH = "/_matrix/media/v3/upload/:serverName/:mediaId"
        const val GET_PREVIEW_URL_PATH = "/v1/media/preview_url"
    }
}