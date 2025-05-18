package io.fibril.ganglion.clientServer.v1.filters.models

import io.fibril.ganglion.clientServer.v1.rooms.models.RoomId
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.common.dsl.Schemas
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.Query


class RoomFilter(private val json: JsonObject?) {
    companion object {
        fun roomFilterSchemaBuilder() = Schemas.objectSchema()
            .optionalProperty("account_data", RoomEventFilter.roomEventFilterSchemaBuilder())
            .optionalProperty("ephemeral", RoomEventFilter.roomEventFilterSchemaBuilder())
            .optionalProperty("include_leave", Schemas.booleanSchema())
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
            .optionalProperty("state", RoomEventFilter.roomEventFilterSchemaBuilder())
            .optionalProperty("timeline", RoomEventFilter.roomEventFilterSchemaBuilder())
    }

    val accountData = RoomEventFilter(json?.getJsonObject("account_data"))
    val ephemeral = RoomEventFilter(json?.getJsonObject("ephemeral"))
    val includeLeave = json?.getBoolean("include_leave", false) ?: false
    val notRoomIds: JsonArray = json?.getJsonArray("not_rooms", JsonArray.of()) ?: JsonArray.of()
    val roomIds: JsonArray = json?.getJsonArray("rooms", JsonArray.of()) ?: JsonArray.of()
    val state = RoomEventFilter(json?.getJsonObject("state"))
    val timeline = RoomEventFilter(json?.getJsonObject("timeline"))

    fun toQuery(): Query {
        return Query.of { q ->
            q.bool { b ->
                b
                    .mustNot(
                        Query.of { qq ->
                            qq.terms { t ->
                                t.field("map.room_id.keyword")
                                    .terms { v -> v.value(notRoomIds.list.map { FieldValue.of(it as String) }) }
                            }
                        }
                    )
                    .should(
                        Query.of { qq ->
                            qq.terms { t ->
                                t.field("map.room_id.keyword")
                                    .terms { v -> v.value(roomIds.list.map { FieldValue.of(it as String) }) }
                            }
                        }
                    ).minimumShouldMatch("1")
            }
        }
    }
}
