package io.fibril.ganglion.clientServer.v1.rooms.models

import com.google.inject.Inject
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas

data class RoomAliasId @Inject constructor(private val id: String) {
    internal constructor(alias: String, domain: String) : this("#$alias:$domain")

    companion object {
        val RoomAliasIdStringRegex = Regex("""^#[a-zA-Z0-9_\-=./]+:[a-zA-Z0-9\-._~]+$""")
        val RoomAliasIdStringSchema = Schemas.stringSchema()
            .with(Keywords.pattern(RoomAliasIdStringRegex.toPattern()))
            .with(Keywords.minLength(5))
            .with(Keywords.maxLength(510))
    }

    init {
        check(RoomAliasIdStringRegex.matches(id)) {
            "Invalid RoomAliasId format: $id"
        }
    }

    val domain = id.substringAfter(":")

    override fun toString() = id
}
