package io.fibril.ganglion.clientServer.utils.pagination

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas


class PaginationDTO @Inject constructor(val json: JsonObject, override val sender: User? = null) :
    DTO(json) {

    override val schema: JsonSchema =
        JsonSchema.of(
            Schemas.objectSchema()
                .optionalProperty("from", Schemas.stringSchema()) // pagination token
                .optionalProperty("to", Schemas.stringSchema()) // pagination token
                .optionalProperty("at", Schemas.stringSchema()) // pagination token
                .optionalProperty("end", Schemas.stringSchema()) // pagination token
                .optionalProperty("start", Schemas.stringSchema()) // pagination token
                .optionalProperty("since", Schemas.stringSchema()) // pagination token
                .optionalProperty("limit", Schemas.intSchema())
                .optionalProperty(
                    "orderBy",
                    Schemas.stringSchema().with(Keywords.pattern("""^(ASC|DESC)$""".toPattern()))
                )
                .toJson()
        )

    override val permittedParams: Set<String>
        get() = json.map.keys

    override val paramNameTransformMapping: Map<String, String>
        get() = mapOf(
            "from" to CURRENT_TOKEN_KEY,
            "to" to END_TOKEN_KEY,
            "at" to CURRENT_TOKEN_KEY,
            "end" to END_TOKEN_KEY,
            "start" to CURRENT_TOKEN_KEY,
            "since" to CURRENT_TOKEN_KEY,
        )

    override fun validate(): DTOValidationResult {
        return Helpers.validate(json, schema)
    }

    companion object {
        const val CURRENT_TOKEN_KEY = "paginationStartToken"
        const val END_TOKEN_KEY = "paginationEndToken"
    }
}

