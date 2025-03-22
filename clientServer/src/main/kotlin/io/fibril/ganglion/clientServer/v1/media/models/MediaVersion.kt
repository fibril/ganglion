package io.fibril.ganglion.clientServer.v1.media.models

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.vertx.core.json.JsonObject
import java.util.*


interface MediaVersionModel : Model

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
data class MediaVersion @Inject constructor(
    val id: String,
    val fullJsonObject: JsonObject? = null
) : MediaVersionModel {

    internal constructor(json: JsonObject) : this(json.getString("id"), json)

    override fun asJson() = JsonObject().put("id", id).mergeIn(fullJsonObject ?: JsonObject())

    companion object {
        val permittedVersionNames = setOf(
            "original",
            "crop32x32",
            "crop96x96",
            "scale320x240",
            "scale640x480",
            "scale800x600"
        )

        val versionNameRegex = Regex("""^(original|crop32x32|crop96x96|scale320x240|scale640x480|scale800x600)$""")
    }
}
