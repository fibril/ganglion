package io.fibril.ganglion.clientServer.v1.users

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.extensions.addRequestRateLimiter
import io.fibril.ganglion.clientServer.extensions.authenticatedRoute
import io.fibril.ganglion.clientServer.extensions.only
import io.fibril.ganglion.clientServer.extensions.useDTOValidation
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.utils.rateLimiters.UsersRequestRateLimiter
import io.fibril.ganglion.clientServer.v1.authentication.RoleType
import io.fibril.ganglion.clientServer.v1.users.dtos.GetUserProfileDTO
import io.fibril.ganglion.clientServer.v1.users.dtos.PutUserDisplayNameDTO
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

internal class UserProfileController @Inject constructor(vertx: Vertx, val userProfileService: UserProfileService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {

        router.get(USER_PROFILE_PATH)
            .useDTOValidation(GetUserProfileDTO::class.java)
            .handler(::getUserProfileByUserId)

        router.get(USER_AVATAR_URL_PATH)
            .useDTOValidation(GetUserProfileDTO::class.java)
            .handler(::getUserAvatarUrl)

        router.put(USER_AVATAR_URL_PATH).handler(::putUserAvatarUrl)

        router.get(GET_USER_DISPLAY_NAME)
            .useDTOValidation(GetUserProfileDTO::class.java)
            .handler(::getUserDisplayName)

        router.put(PUT_USER_DISPLAY_NAME)
            .addRequestRateLimiter(UsersRequestRateLimiter.getInstance())
            .useDTOValidation(PutUserDisplayNameDTO::class.java)
            .authenticatedRoute(RoleType.USER)
            .handler(::putUserDisplayName)

        return router
    }

    private fun getUserProfileByUserId(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val uid = routingContext.pathParam("userId")
            val matrixUserId = MatrixUserId(uid)
            userProfileService.findOneByUserId(matrixUserId)
                .onSuccess { userProfile ->
                    if (userProfile != null) {
                        routingContext.end(
                            userProfile.asJson().only(
                                "display_name",
                                "displayname",
                                "avatar_url"
                            ).toString()
                        )
                    } else {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(
                            StandardErrorResponse(ErrorCodes.M_NOT_FOUND, error = "Not found").asJson().toString()
                        )
                    }
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())

                }
        }
    }

    private fun getUserAvatarUrl(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val uid = routingContext.pathParam("userId")
            val matrixUserId = MatrixUserId(uid)
            userProfileService.findOneByUserId(matrixUserId)
                .onSuccess { userProfile ->
                    if (userProfile != null) {
                        routingContext.end(
                            userProfile.asJson().only(
                                "avatar_url"
                            ).toString()
                        )
                    } else {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(
                            StandardErrorResponse(ErrorCodes.M_NOT_FOUND, error = "Not found").asJson().toString()
                        )
                    }
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())

                }
        }
    }

    private fun putUserAvatarUrl(context: RoutingContext) {

    }

    private fun getUserDisplayName(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val uid = routingContext.pathParam("userId")
            val matrixUserId = MatrixUserId(uid)
            userProfileService.findOneByUserId(matrixUserId)
                .onSuccess { userProfile ->
                    if (userProfile != null) {
                        routingContext.end(
                            userProfile.asJson().only(
                                "displayname"
                            ).toString()
                        )
                    } else {
                        routingContext.response().setStatusCode(404)
                        routingContext.end(
                            StandardErrorResponse(ErrorCodes.M_NOT_FOUND, error = "Not found").asJson().toString()
                        )
                    }

                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())

                }
        }
    }

    private fun putUserDisplayName(routingContext: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val uid = routingContext.pathParam("userId")
            val sender = routingContext.user()
            if (sender.principal().getString("sub") != uid) {
                routingContext.response().setStatusCode(403)
                routingContext.end(
                    RequestException(403, "Forbidden", StandardErrorResponse(ErrorCodes.M_FORBIDDEN).asJson())
                        .json.toString()
                )
                return@usingCoroutineScopeWithIODispatcher
            }
            val matrixUserId = MatrixUserId(uid)
            val updateDTO = PutUserDisplayNameDTO(routingContext.body().asJsonObject(), sender = sender)
            userProfileService.updateUserProfileByUserId(matrixUserId, updateDTO)
                .onSuccess { userProfile ->
                    routingContext.end(
                        userProfile.asJson().only(
                            "displayname"
                        ).toString()
                    )
                }.onFailure {
                    val err = it as RequestException
                    routingContext.response().setStatusCode(err.statusCode)
                    routingContext.end(err.json.toString())

                }
        }
    }

    companion object {
        const val USER_PROFILE_PATH = "/v3/profile/:userId"
        const val USER_AVATAR_URL_PATH = "/v3/profile/:userId/avatar_url"
        const val GET_USER_DISPLAY_NAME = "/v3/profile/:userId/displayname"
        const val PUT_USER_DISPLAY_NAME = "/v3/profile/:userId/displayname"
    }
}