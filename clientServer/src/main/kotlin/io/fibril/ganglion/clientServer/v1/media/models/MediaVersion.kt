package io.fibril.ganglion.clientServer.v1.media.models

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.Model
import io.vertx.core.json.JsonObject


interface MediaVersionModel : Model

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

        fun approximateVersionName(cropOrScale: String, height: Int): String {
            if (cropOrScale != "crop" && cropOrScale != "scale") return "original"

            println("cropOrScale $cropOrScale $height")

            when (cropOrScale) {
                "crop" -> {
                    return if (height <= 32) "crop32x32"
                    else "crop96x96"
                }

                "scale" -> {
                    if (height <= 320) return "scale320x240"
                    return if (height <= 640) "scale640x480"
                    else "scale800x600"
                }

                else -> return "original"
            }
        }

        val versionNameRegex = Regex("""^(original|crop32x32|crop96x96|scale320x240|scale640x480|scale800x600)$""")
    }
}
