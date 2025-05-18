package io.fibril.ganglion.clientServer.v1.filters.models

import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder
import io.vertx.json.schema.common.dsl.Schemas
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.query_dsl.Query


open class EventFilter(json: JsonObject?) {


    val limit =
        json?.getInteger("limit", DEFAULT_EVENT_RESPONSE_SIZE) ?: DEFAULT_EVENT_RESPONSE_SIZE

    val notRoomIds: JsonArray = json?.getJsonArray("not_rooms", JsonArray.of()) ?: JsonArray.of()
    val roomIds: JsonArray = json?.getJsonArray("rooms", JsonArray.of()) ?: JsonArray.of()

    val notSenderIds: JsonArray = json?.getJsonArray("not_senders", JsonArray.of()) ?: JsonArray.of()
    val senderIds: JsonArray = json?.getJsonArray("senders", JsonArray.of()) ?: JsonArray.of()

    val notTypes: JsonArray = json?.getJsonArray("not_types", JsonArray.of()) ?: JsonArray.of()
    val types: JsonArray = json?.getJsonArray("types", JsonArray.of()) ?: JsonArray.of()

    open fun toQuery(
        lazyLoadMembersForUserId: String? = null,
        additionalMustQueries: List<Query>? = listOf(),
        additionalShouldQueries: List<Query>? = listOf(),
        additionalMustNotQueries: List<Query>? = listOf()
    ): Query {
        val _notSenderIds: List<String> =
            if (lazyLoadMembersForUserId != null) notSenderIds.list.filterNot {
                it == lazyLoadMembersForUserId
            } as List<String> else notSenderIds.list as List<String>

        val _senderIds =
            if (lazyLoadMembersForUserId != null) listOf(lazyLoadMembersForUserId) else senderIds.list


        return Query.of { q ->
            q.bool { b ->
                b
                    .mustNot(
                        Query.of { qq ->
                            qq.terms { t ->
                                t.field("map.room_id.keyword")
                                    .terms { v -> v.value(notRoomIds.list.map { FieldValue.of(it as String) }) }
                            }
                        },
                        Query.of { qq ->
                            qq.terms { t ->
                                t.field("map.sender.keyword")
                                    .terms { v -> v.value(_notSenderIds.map { FieldValue.of(it as String) }) }
                            }
                        },
                        Query.of { qq ->
                            qq.terms { t ->
                                t.field("map.type.keyword")
                                    .terms { v -> v.value(notTypes.list.map { FieldValue.of(it as String) }) }
                            }
                        },
                        *additionalMustNotQueries!!.toTypedArray()
                    )
                    .must(
                        if (lazyLoadMembersForUserId != null)
                            Query.of { qq ->
                                qq.term { t ->
                                    t.field("map.type.sender")
                                        .value(FieldValue.of(lazyLoadMembersForUserId))
                                }
                            }
                        // dummy id exists query
                        else Query.of { qq ->
                            qq.exists { t ->
                                t.field("map.id")
                            }
                        },
                        *additionalMustQueries!!.toTypedArray()
                    )
                    .should(
                        Query.of { qq ->
                            qq.terms { t ->
                                t.field("map.room_id.keyword")
                                    .terms { v -> v.value(roomIds.list.map { FieldValue.of(it as String) }) }
                            }
                        },
                        Query.of { qq ->
                            qq.terms { t ->
                                t.field("map.sender.keyword")
                                    .terms { v -> v.value(_senderIds.map { FieldValue.of(it as String) }) }
                            }
                        },
                        Query.of { qq ->
                            qq.terms { t ->
                                t.field("map.type.keyword")
                                    .terms { v -> v.value(types.list.map { FieldValue.of(it as String) }) }
                            }
                        },
                        *additionalShouldQueries!!.toTypedArray()
                    ).minimumShouldMatch("1")
            }
        }
    }

    companion object {
        fun eventFilterSchemaBuilder(): ObjectSchemaBuilder = Schemas.objectSchema()
            .optionalProperty("limit", Schemas.intSchema())
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

        const val DEFAULT_EVENT_RESPONSE_SIZE = 100
    }

}

