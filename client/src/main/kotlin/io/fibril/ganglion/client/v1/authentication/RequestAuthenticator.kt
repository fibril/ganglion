package io.fibril.ganglion.client.v1.authentication

import io.fibril.ganglion.authentication.GanglionJWTAuthProviderImpl
import io.fibril.ganglion.client.errors.ErrorCodes
import io.fibril.ganglion.client.errors.StandardErrorResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext


enum class UserType {
    USER,
    ADMIN
}

object RequestAuthenticator {
    fun handleRequestAuthentication(routingContext: RoutingContext, forUserType: UserType) {
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

        when (forUserType) {
            UserType.USER -> {
                GanglionJWTAuthProviderImpl(routingContext.vertx()).authenticate(
                    bearerToken
                ).onSuccess {
                    routingContext.next()
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

            UserType.ADMIN -> {
                GanglionJWTAuthProviderImpl(routingContext.vertx()).authenticate(
                    bearerToken
                ).onSuccess {
                    // TODO: Verify the user is admin
                    routingContext.next()
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
        }
    }

}