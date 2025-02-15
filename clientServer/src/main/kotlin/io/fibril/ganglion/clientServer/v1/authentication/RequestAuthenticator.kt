package io.fibril.ganglion.clientServer.v1.authentication

import io.fibril.ganglion.authentication.GanglionJWTAuthProviderImpl
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext


object RequestAuthenticator {
    fun handleRequestAuthentication(routingContext: RoutingContext, minimumRoleType: RoleType) {
        val bearerToken = routingContext.request().headers().get("Authorization")

        if (bearerToken == null) {
            routingContext.response()
                .setStatusCode(401)
                .end(
                    StandardErrorResponse(
                        ErrorCodes.M_MISSING_TOKEN
                    ).toString()
                )
            return
        }

        GanglionJWTAuthProviderImpl(routingContext.vertx()).authenticate(
            bearerToken
        ).onSuccess { vertxUser ->
            val role = vertxUser.principal().getString("role") ?: ""
            val roleType: RoleType? = RoleType.entries.find { roleType -> roleType.name.lowercase() == role }
            if (roleHasAbility(roleType, minimumRoleType)) {
                routingContext.setUser(vertxUser)
                routingContext.next()
            } else {
                routingContext.response()
                    .setStatusCode(401)
                    .end(
                        StandardErrorResponse(
                            errCode = ErrorCodes.M_UNKNOWN_TOKEN,
                            error = "Unauthorized",
                            JsonObject.of("soft_logout", true)
                        ).toString()
                    )
            }
        }.onFailure { err ->
            routingContext.response()
                .setStatusCode(401)
                .end(
                    StandardErrorResponse(
                        errCode = ErrorCodes.M_UNKNOWN_TOKEN,
                        error = err.message,
                        JsonObject.of("soft_logout", true)
                    ).toString()
                )
        }
    }

    private fun roleHasAbility(roleType: RoleType?, minimumRoleType: RoleType): Boolean {
        return roleType != null && roleHierarchy[roleType]!! >= roleHierarchy[minimumRoleType]!!
    }

}