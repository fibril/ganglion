package io.fibril.ganglion.clientServer.extensions

import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.utils.RequestRateLimiter
import io.fibril.ganglion.clientServer.v1.authentication.RequestAuthenticator
import io.fibril.ganglion.clientServer.v1.authentication.RoleType
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext

fun Route.addRequestRateLimiter(requestRateLimiter: RequestRateLimiter): Route {
    this.handler(requestRateLimiter::handle)
    return this
}

fun Route.authenticatedRoute(minimumRoleType: RoleType): Route {
    this.handler { context ->
        RequestAuthenticator.handleRequestAuthentication(context, minimumRoleType)
    }
    return this
}

fun <T : DTO> Route.useDTOValidation(
    clazz: Class<T>
): Route {
    this.handler { routingContext ->
        val params = (routingContext.body().asJsonObject() ?: JsonObject())
            .mergeIn(JsonObject.mapFrom(routingContext.pathParams()))
            .mergeIn(JsonObject.mapFrom(routingContext.queryParams() ?: mapOf<String, String>()))
        val sender = null
        val dto = clazz.constructors[0].newInstance(params, sender) as DTO
        val validationResult = dto.validate()
        if (validationResult.valid) {
            routingContext.next()
        } else {
            defaultOnValidationFailure(routingContext, validationResult.errors)
        }
    }
    return this

}


private fun defaultOnValidationFailure(context: RoutingContext, errors: JsonObject) {
    context.response().setStatusCode(400)
    context.end(
        StandardErrorResponse(
            ErrorCodes.M_INVALID_PARAM,
            additionalKeyValues = JsonObject().apply { if (!errors.isEmpty) put("errors", errors) }
        ).toString()
    )
}