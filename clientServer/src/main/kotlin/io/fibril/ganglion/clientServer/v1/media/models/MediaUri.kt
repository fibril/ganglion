package io.fibril.ganglion.clientServer.v1.media.models

import com.google.inject.Inject
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.json.schema.common.dsl.StringSchemaBuilder

data class MediaUri @Inject constructor(private val uri: String) {
    constructor(id: String, domain: String) : this("$MATRIX_CONTENT_PROTOCOL$domain/$id")

    companion object {
        const val MATRIX_CONTENT_PROTOCOL = "mxc://"

        val MediaUriStringRegex = Regex("""^mxc:\/\/.{1,}""")

        fun createMediaUriStringSchema(): StringSchemaBuilder = Schemas.stringSchema()
            .with(Keywords.pattern(MediaUriStringRegex.toPattern()))
            .with(Keywords.maxLength(510))
    }

    init {
        check(MediaUriStringRegex.matches(uri)) {
            "Invalid RoomId format"
        }
    }

    override fun toString() = uri

    val id = uri.substringAfterLast(':')
}
