package io.fibril.ganglion.clientServer.v1.users

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Controller
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.extensions.only
import io.fibril.ganglion.clientServer.extensions.useDTOValidation
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.v1.users.dtos.GetUserProfileDTO
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

        router.get(USER_AVATAR_URL_PATH).handler(::getUserAvatarUrl)

        router.put(USER_AVATAR_URL_PATH).handler(::putUserAvatarUrl)

        return router
    }

    private fun getUserProfileByUserId(context: RoutingContext) {
        CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
            val uid = context.pathParam("userId")
            val matrixUserId = MatrixUserId(uid)
            userProfileService.findOneByUserId(matrixUserId)
                .onSuccess { userProfile ->
                    context.end(
                        userProfile.asJson().only(
                            "display_name",
                            "displayname",
                            "avatar_url"
                        ).toString()
                    )
                }.onFailure {
                    val err = it as RequestException
                    context.response().setStatusCode(err.statusCode)
                    context.end(err.json.toString())

                }
        }
    }

    private fun getUserAvatarUrl(context: RoutingContext) {

    }

    private fun putUserAvatarUrl(context: RoutingContext) {

    }

    companion object {
        const val USER_PROFILE_PATH = "/v3/profile/:userId"
        const val USER_AVATAR_URL_PATH = "/v3/profile/:userId/avatar_url"
    }
}