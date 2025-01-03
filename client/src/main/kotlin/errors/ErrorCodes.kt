package errors

/**
 * Error codes used in the client.
 * Each Error code must have an associated localization key definition in an appropriate properties file
 */
object ErrorCodes {
    const val M_FORBIDDEN = "M_FORBIDDEN"

    const val M_UNKNOWN_TOKEN = "M_UNKNOWN_TOKEN" // The access or refresh token specified was not recognised.

    const val M_MISSING_TOKEN = "M_MISSING_TOKEN" // No access token was specified for the request.

    const val M_USER_LOCKED = "M_USER_LOCKED" // The account has been locked and cannot be used at this time.

    const val M_USER_SUSPENDED =
        "M_USER_SUSPENDED" // The account has been suspended and can only be used for limited actions at this time.

    const val M_BAD_JSON =
        "M_BAD_JSON" // Request contained valid JSON, but it was malformed in some way, e.g. missing required keys, invalid values for keys.

    const val M_NOT_JSON = "M_NOT_JSON" // Request did not contain valid JSON.

    const val M_NOT_FOUND = "M_NOT_FOUND" // No resource was found for this request.

    const val M_LIMIT_EXCEEDED =
        "M_LIMIT_EXCEEDED" // Too many requests have been sent in a short period of time. Wait a while then try again. See Rate limiting.

    const val M_UNRECOGNIZED =
        "M_UNRECOGNIZED" // The server did not understand the request. This is expected to be returned with a 404 HTTP status code if the endpoint is not implemented or a 405 HTTP status code if the endpoint is implemented, but the incorrect HTTP method is used.

    const val M_UNKNOWN = "M_UNKNOWN" // An unknown error has occurred.


    const val M_UNAUTHORIZED =
        "M_UNAUTHORIZED" // The request was not correctly authorized. Usually due to login failures.

    const val M_USER_DEACTIVATED =
        "M_USER_DEACTIVATED" // The user ID associated with the request has been deactivated. Typically for endpoints that prove authentication, such as /login.

    const val M_USER_IN_USE = "M_USER_IN_USE" // Encountered when trying to register a user ID which has been taken.

    const val M_INVALID_USERNAME =
        "M_INVALID_USERNAME" // Encountered when trying to register a user ID which is not valid.

    const val M_ROOM_IN_USE = "M_ROOM_IN_USE" // Sent when the room alias given to the createRoom API is already in use.

    const val M_INVALID_ROOM_STATE =
        "M_INVALID_ROOM_STATE" // Sent when the initial state given to the createRoom API is invalid.

    const val M_THREEPID_IN_USE =
        "M_THREEPID_IN_USE" // Sent when a threepid given to an API cannot be used because the same threepid is already in use.

    const val M_THREEPID_NOT_FOUND =
        "M_THREEPID_NOT_FOUND" // Sent when a threepid given to an API cannot be used because no record matching the threepid was found.

    const val M_THREEPID_AUTH_FAILED =
        "M_THREEPID_AUTH_FAILED" // Authentication could not be performed on the third-party identifier.

    const val M_THREEPID_DENIED =
        "M_THREEPID_DENIED" // The server does not permit this third-party identifier. This may happen if the server only permits, for example, email addresses from a particular domain.

    const val M_SERVER_NOT_TRUSTED =
        "M_SERVER_NOT_TRUSTED" // The client’s request used a third-party server, e.g. identity server, that this server does not trust.

    const val M_UNSUPPORTED_ROOM_VERSION =
        "M_UNSUPPORTED_ROOM_VERSION" // The client’s request to create a room used a room version that the server does not support.

    const val M_INCOMPATIBLE_ROOM_VERSION =
        "M_INCOMPATIBLE_ROOM_VERSION" // The client attempted to join a room that has a version the server does not support. Inspect the room_version property of the error response for the room’s version.

    const val M_BAD_STATE =
        "M_BAD_STATE" // The state change requested cannot be performed, such as attempting to unban a user who is not banned.

    const val M_GUEST_ACCESS_FORBIDDEN =
        "M_GUEST_ACCESS_FORBIDDEN" // The room or resource does not permit guests to access it.

    const val M_CAPTCHA_NEEDED = "M_CAPTCHA_NEEDED" // A Captcha is required to complete the request.

    const val M_CAPTCHA_INVALID = "M_CAPTCHA_INVALID" // The Captcha provided did not match what was expected.

    const val M_MISSING_PARAM = "M_MISSING_PARAM" // A required parameter was missing from the request.

    const val M_INVALID_PARAM =
        "M_INVALID_PARAM" // A parameter that was specified has the wrong value. For example, the server expected an integer and instead received a string.

    const val M_TOO_LARGE = "M_TOO_LARGE" // The request or entity was too large.

    const val M_EXCLUSIVE =
        "M_EXCLUSIVE" // The resource being requested is reserved by an application service, or the application service making the request has not created the resource.

    const val M_RESOURCE_LIMIT_EXCEEDED =
        "M_RESOURCE_LIMIT_EXCEEDED" // The request cannot be completed because the homeserver has reached a resource limit imposed on it. For example, a homeserver held in a shared hosting environment may reach a resource limit if it starts using too much memory or disk space. The error MUST have an admin_contact field to provide the user receiving the error a place to reach out to. Typically, this error will appear on routes which attempt to modify state (e.g.: sending messages, account data, etc) and not routes which only read state (e.g.: /sync, /user/{userId}/account_data/{type}, etc).

    const val M_CANNOT_LEAVE_SERVER_NOTICE_ROOM =
        "M_CANNOT_LEAVE_SERVER_NOTICE_ROOM" // The user is unable to reject an invite to join the server notices room. See the Server Notices module for more information.

    const val M_THREEPID_MEDIUM_NOT_SUPPORTED =
        "M_THREEPID_MEDIUM_NOT_SUPPORTED" // The homeserver does not support adding a third party identifier of the given medium.

}