package io.fibril.ganglion.client.v1.media.models

import io.fibril.ganglion.client.Model
import com.google.inject.Inject
import io.vertx.core.json.JsonObject
import java.util.*


interface MediaModel : Model

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
data class Media @Inject constructor(
    val mediaId: String,
    val additionalKeyValues: JsonObject = JsonObject()
) : MediaModel {
    companion object {
        const val MATRIX_CONTENT_PROTOCOL = "mxc://"
    }

    override fun asJson(): JsonObject =
        JsonObject().put("id", mediaId).put("content_uri", uri).mergeIn(additionalKeyValues)

    private val domain = ResourceBundle.getBundle("application").getString("domain")

    val uri = MATRIX_CONTENT_PROTOCOL + domain + "/" + mediaId
}
