package io.fibril.ganglion.clientServer.v1.filters.models

import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder
import io.vertx.json.schema.common.dsl.Schemas


class RoomEventFilter(private val json: JsonObject?) : EventFilter(json) {
    val containsUrl = json?.getBoolean("contains_url", false) ?: false

    val includeRedundantMembers = json?.getBoolean("include_redundant_members", false) ?: false

    val lazyLoadMembers = json?.getBoolean("lazy_load_members", false) ?: false

    val unreadThreadNotifications = json?.getBoolean("unread_thread_notifications", false) ?: false


    companion object {

        fun roomEventFilterSchemaBuilder(): ObjectSchemaBuilder = Schemas.objectSchema()
            .optionalProperty("contains_url", Schemas.booleanSchema())
            .optionalProperty("include_redundant_members", Schemas.booleanSchema())
            .optionalProperty("lazy_load_members", Schemas.booleanSchema())
            .optionalProperty("limit", Schemas.intSchema())
            .optionalProperty(
                "not_rooms", Schemas.arraySchema().items(
                    RoomId.createRoomIdStringSchema()
                )
            )
            .optionalProperty(
                "rooms", Schemas.arraySchema().items(
                    RoomId.createRoomIdStringSchema()
                )
            )
            .optionalProperty(
                "not_senders", Schemas.arraySchema().items(
                    MatrixUserId.createMatrixUserIdStringSchema()
                )
            )
            .optionalProperty(
                "not_types", Schemas.arraySchema().items(
                    Schemas.stringSchema()
                )
            )
            .optionalProperty(
                "senders", Schemas.arraySchema().items(
                    MatrixUserId.createMatrixUserIdStringSchema()
                )
            )
            .optionalProperty(
                "types", Schemas.arraySchema().items(
                    Schemas.stringSchema()
                )
            )
            .optionalProperty("unread_thread_notifications", Schemas.booleanSchema())


    }

}
