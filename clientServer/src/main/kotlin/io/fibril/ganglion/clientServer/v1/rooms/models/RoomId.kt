package io.fibril.ganglion.clientServer.v1.rooms.models

import com.google.inject.Inject
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.json.schema.common.dsl.StringSchemaBuilder

data class RoomId @Inject constructor(private val id: String) {

    companion object {
        val RoomIdStringRegex = Regex("""^![a-zA-Z0-9_\-=./]+:[a-zA-Z0-9\-._~]+$""")
        val RoomIdStringSchema: StringSchemaBuilder = Schemas.stringSchema()
            .with(Keywords.pattern(RoomIdStringRegex.toPattern()))
            .with(Keywords.minLength(5))
            .with(Keywords.maxLength(510))
    }

    init {
        check(RoomIdStringRegex.matches(id)) {
            "Invalid RoomId format"
        }
    }

    val domain = id.substringAfter(":")

    override fun toString() = id
}
