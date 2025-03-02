package io.fibril.ganglion.clientServer.v1.roomEvents.models

import com.google.inject.Inject
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas

data class RoomEventId @Inject constructor(private val id: String) {
    internal constructor(localPart: String, domain: String) : this("$$localPart:$domain")

    companion object {
        val RoomEventIdStringRegex = Regex("""^$[a-zA-Z0-9_\-=./]+:[a-zA-Z0-9\-._~]+$""")
        fun createRoomEventIdStringSchema() = Schemas.stringSchema()
            .with(Keywords.pattern(RoomEventIdStringRegex.toPattern()))
            .with(Keywords.minLength(5))
            .with(Keywords.maxLength(510))
    }

    init {
        check(RoomEventIdStringRegex.matches(id)) {
            "Invalid RoomAliasId format: $id"
        }
    }

    val domain = id.substringAfter(":")

    override fun toString() = id
}
