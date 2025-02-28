package io.fibril.ganglion.clientServer.v1.users.dtos

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas

data class UsersDirectorySearchDTO @Inject constructor(
    override val json: JsonObject,
    override val sender: User? = null
) :
    PaginationDTO(json) {
    override val schema: JsonSchema =
        JsonSchema.of(
            Schemas.objectSchema()
                .optionalProperty("limit", Schemas.intSchema())
                .optionalProperty("search_term", Schemas.stringSchema())
                .toJson()
        )

    override val permittedParams: Set<String>
        get() = json.map.keys

    override val paramNameTransformMapping: Map<String, String>
        get() = mapOf()

    override val filterWhereClauseGeneratorMap: Map<String, (value: Any) -> String>
        get() = mapOf(
            "search_term" to ::genericSearchTermWhereClause
        )

    override fun validate(): DTOValidationResult {
        return Helpers.validate(json, schema)
    }

    private fun genericSearchTermWhereClause(value: Any): String {
        return "display_name ILIKE '%${value}%'"
    }

}

