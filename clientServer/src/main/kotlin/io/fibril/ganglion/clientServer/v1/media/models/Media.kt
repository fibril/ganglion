package io.fibril.ganglion.clientServer.v1.media.models

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.vertx.core.json.JsonObject
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


interface MediaModel : Model

data class Media @Inject constructor(
    val id: String,
    val fullJsonObject: JsonObject? = null
) : MediaModel {

    internal constructor(json: JsonObject) : this(json.getString("id"), json)

    override fun asJson() = JsonObject().put("id", id).mergeIn(fullJsonObject ?: JsonObject())

    private val domain = ResourceBundleConstants.domain

    val uri = MediaUri(id, domain).toString()

    val unused_expires_at: Long?
        get() {
            if (asJson().getString("created_at") == null) return null
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val localDateTime = LocalDateTime.parse(asJson().getString("created_at"), formatter)
            val instant = localDateTime.toInstant(ZoneOffset.UTC)
            return instant.toEpochMilli() +
                    (ResourceBundleConstants.applicationBundle.getString("m.media.unused.lifetimeMs")
                        ?: "86400000").toLong()
        }

    val isImage: Boolean = asJson()
        .getString("content_type", "")
        .substringBefore('/').lowercase() == "image"

    companion object {
        val INLINE_CONTENT_TYPE = setOf(
            "text/css",
            "text/plain",
            "text/csv",
            "application/json",
            "application/ld+json",
            "image/jpeg",
            "image/gif",
            "image/png",
            "image/apng",
            "image/webp",
            "image/avif",
            "video/mp4",
            "video/webm",
            "video/ogg",
            "video/quicktime",
            "audio/mp4",
            "audio/webm",
            "audio/aac",
            "audio/mpeg",
            "audio/ogg",
            "audio/wave",
            "audio/wav",
            "audio/x-wav",
            "audio/x-pn-wav",
            "audio/flac",
            "audio/x-flac",
        )
    }
}
