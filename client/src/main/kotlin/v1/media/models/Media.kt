package v1.media.models

import Model
import io.vertx.core.json.JsonObject

/**
 * id
 * message_id
 * user_id
 * content_type
 * content_disposition
 * preview_url
 * media_type
 * title
 * description
 *
 * [VERSIONS]
 *
 * id
 * name  [
 *  * original
 *  * crop32x32,
 *  * crop96x96,
 *  * scale320x240,
 *  * scale640x480,
 *  * scale800x600,
 *  * ]
 *
 * uri: external:// || local://
 * size
 * height
 * width
 * animated
 */
data class Media(val matrixContentUrl: String) : Model {
    companion object {
        const val MATRIX_CONTENT_PREFIX = "mxc://"
    }

    override fun asJson(permittedFields: List<String>?): JsonObject {
        TODO("Not yet implemented")
    }
}
