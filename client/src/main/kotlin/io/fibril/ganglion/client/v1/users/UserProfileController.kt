package io.fibril.ganglion.client.v1.users

import io.fibril.ganglion.client.Controller
import io.fibril.ganglion.client.DTO
import com.google.inject.Inject
import io.fibril.ganglion.client.extensions.only
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.fibril.ganglion.client.utils.CoroutineHelpers
import io.fibril.ganglion.client.v1.users.dtos.GetUserProfileDTO
import io.fibril.ganglion.client.v1.users.models.MatrixUserId

internal class UserProfileController @Inject constructor(vertx: Vertx, val userProfileService: UserProfileService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {

        router.get(USER_PROFILE_PATH).handler(::getUserProfileByUserId)

        router.get(USER_AVATAR_URL_PATH).handler(::getUserAvatarUrl)

        router.put(USER_AVATAR_URL_PATH).handler(::putUserAvatarUrl)

        return router
    }

    private fun getUserProfileByUserId(context: RoutingContext) {
        val getUserProfileDTO = GetUserProfileDTO(context)

        DTO.Helpers.useDTOValidation(
            dto = getUserProfileDTO,
            context
        ) {
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                val uid = context.pathParam("userId")
                val matrixUserId = MatrixUserId(uid)
                val userProfile = userProfileService.findOneByUserId(matrixUserId)
                context.end(
                    userProfile?.asJson()?.only(setOf()).toString()
                )

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