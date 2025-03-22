package io.fibril.ganglion.clientServer.v1.media.models

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
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
    val id: String,
    val fullJsonObject: JsonObject? = null
) : MediaModel {

    internal constructor(json: JsonObject) : this(json.getString("id"), json)

    override fun asJson() = JsonObject().put("id", id).mergeIn(fullJsonObject ?: JsonObject())

    private val domain = ResourceBundleConstants.domain

    val uri = MediaUri(id, domain).toString()

    val unused_expires_at =
        (Date(asJson().getString("created_at")).time +
                (ResourceBundleConstants.applicationBundle.getString("m.media.unused.lifetimeMs")
                    ?: "86400000").toLong())
}
