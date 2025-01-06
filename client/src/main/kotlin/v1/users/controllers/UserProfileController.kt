package v1.users.controllers

import Controller
import DTO
import com.google.inject.Inject
import errors.ErrorCodes
import errors.StandardErrorResponse
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import utils.CoroutineHelpers
import utils.rateLimiters.MatrixRequestRateLimiter
import v1.users.dtos.GetUserProfileDTO
import v1.users.models.MatrixUserId
import v1.users.services.UserProfileService

internal class UserProfileController @Inject constructor(vertx: Vertx, val userProfileService: UserProfileService) :
    Controller(vertx) {
    override fun mountSubRoutes(): Router {

        MatrixRequestRateLimiter.getInstance().useRateLimiting(
            router.get(USER_PROFILE_PATH), ::getUserProfileByUserId
        )

        router.get(USER_AVATAR_URL_PATH).handler(::getUserAvatarUrl)

        router.put(USER_AVATAR_URL_PATH).handler(::putUserAvatarUrl)

        return router
    }

    private fun getUserProfileByUserId(context: RoutingContext) {
        val getUserProfileDTO = GetUserProfileDTO(context)

        DTO.Helpers.useDTOValidation(dto = getUserProfileDTO, onValidationFailure = {
            context.response().setStatusCode(400)
            context.end(
                StandardErrorResponse(
                    ErrorCodes.M_INVALID_PARAM,
                ).toString()
            )
        }) {
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                val uid = context.pathParam("userId")
                val matrixUserId = MatrixUserId(uid)
                val userProfile = userProfileService.findOneByUserId(matrixUserId)
                context.end(
                    userProfile?.asJson(permittedFields = listOf("displayname", "avatar_url")).toString()
                )

            }
        }


    }

    private fun getUserAvatarUrl(context: RoutingContext) {

    }

    private fun putUserAvatarUrl(context: RoutingContext) {

    }

    companion object {
        const val USER_PROFILE_PATH = "/profile/:userId"
        const val USER_AVATAR_URL_PATH = "/profile/:userId/avatar_url"
    }
}