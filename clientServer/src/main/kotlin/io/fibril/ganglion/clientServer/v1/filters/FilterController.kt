package io.fibril.ganglion.clientServer.v1.filters

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.extensions.authenticatedRoute
import io.fibril.ganglion.clientServer.extensions.useDTOValidation
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.v1.authentication.RoleType
import io.fibril.ganglion.clientServer.v1.filters.dtos.CreateFilterDTO
import io.fibril.ganglion.clientServer.v1.filters.dtos.GetFilterDTO
import io.fibril.ganglion.clientServer.v1.presence.dtos.PutPresenceDTO
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

internal class FilterController @Inject constructor(vertx: Vertx, val filterService: FilterService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {

        router.route().handler(BodyHandler.create())

        router.post(CREATE_FILTER_PATH)
            .useDTOValidation(CreateFilterDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::createFilter)

        router.get(GET_FILTER_PATH)
            .useDTOValidation(GetFilterDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::getFilter)
        return router
    }

    private fun createFilter(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val json = JsonObject.mapFrom(routingContext.pathParams())
                .mergeIn(routingContext.body()?.asJsonObject() ?: JsonObject())
            val createFilterDTO = CreateFilterDTO(json, routingContext.user())
            filterService.create(createFilterDTO)
                .onSuccess { filter ->
                    routingContext.end(filter.asJson().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }

    }

    private fun getFilter(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val json = JsonObject.mapFrom(routingContext.pathParams())
            val getFilterDTO = GetFilterDTO(json, routingContext.user())
            filterService.getFilterForUser(getFilterDTO)
                .onSuccess { filter ->
                    routingContext.end(filter.asJson().toString())
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())
                }
        }

    }
    
    companion object {
        const val CREATE_FILTER_PATH = "/v3/user/:userId/filter"
        const val GET_FILTER_PATH = "/v3/user/:userId/filter/:filterId"
    }
}