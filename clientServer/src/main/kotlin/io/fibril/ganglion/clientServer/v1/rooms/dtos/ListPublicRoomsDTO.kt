package io.fibril.ganglion.clientServer.v1.rooms.dtos

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas

data class ListPublicRoomsDTO @Inject constructor(override val json: JsonObject, override val sender: User? = null) :
    PaginationDTO(json) {
    override val schema: JsonSchema =
        JsonSchema.of(
            Schemas.objectSchema()
                .optionalProperty("server", Schemas.stringSchema())
                .optionalProperty(
                    "filter", Schemas.objectSchema()
                        .optionalProperty("generic_search_term", Schemas.stringSchema())
                        .optionalProperty("room_types", Schemas.arraySchema().items(Schemas.stringSchema()))
                )
                .optionalProperty("include_all_networks", Schemas.booleanSchema())
                .optionalProperty("limit", Schemas.intSchema())
                .optionalProperty("since", Schemas.stringSchema())
                .optionalProperty("third_party_instance_id", Schemas.stringSchema())
                .toJson()
        )

    override val permittedParams: Set<String>
        get() = json.map.keys

    override val paramNameTransformMapping: Map<String, String>
        get() = mapOf()

    override val filterWhereClauseGeneratorMap: Map<String, (value: Any) -> String>
        get() = mapOf(
            "generic_search_term" to ::genericSearchTermWhereClause,
            "room_types" to ::roomTypesWhereClause
        )

    override fun validate(): DTOValidationResult {
        return Helpers.validate(json, schema)
    }

    private fun genericSearchTermWhereClause(value: Any): String {
        return "room.canonical_alias ILIKE '%${value}%' " +
                "OR room.name ILIKE '%${value}%' " +
                "OR room.topic ILIKE '%${value}%'"
    }

    private fun roomTypesWhereClause(value: Any): String {
        val roomTypes = (value as JsonArray).list.joinToString(
            prefix = "(",
            postfix = ")",
            transform = { "\'$it\'" })
        return "room.type IN ${roomTypes}"
    }

}

