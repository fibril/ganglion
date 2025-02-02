package io.fibril.ganglion.clientServer.v1.media

import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.DTO
import com.google.inject.Inject
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.v1.media.dtos.CreateMediaDTO

internal class MediaController @Inject constructor(vertx: Vertx, val mediaService: MediaService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {
        router.get(MEDIA_CONFIG_PATH).handler(::getMediaConfig)

        return router
    }

    fun mountMediaCreateAndUploadRoutes(): Router {
        router.get(MEDIA_CREATE_PATH).handler(::createMedia)
        return router
    }

    private fun getMediaConfig(routingContext: RoutingContext) {
        routingContext.end(
            mediaService.getMediaConfig().toString()
        )
    }

    private fun createMedia(routingContext: RoutingContext) {
        val createMediaDTO = CreateMediaDTO(JsonObject())
        DTO.Helpers.useDTOValidation(dto = createMediaDTO, routingContext) {
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                val paramsJson = JsonObject.mapFrom(routingContext.pathParams())
            }
        }
    }

    companion object {
        const val MEDIA_CONFIG_PATH = "/io/fibril/ganglion/clientServer/v1/media/config"
        const val MEDIA_CREATE_PATH = "/_matrix/media/v1/create"
    }
}