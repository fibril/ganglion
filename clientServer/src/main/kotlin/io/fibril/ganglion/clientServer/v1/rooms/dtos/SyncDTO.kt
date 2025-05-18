package io.fibril.ganglion.clientServer.v1.rooms.dtos

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.filters.dtos.CreateFilterDTO
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas

data class SyncDTO @Inject constructor(private val json: JsonObject, override val sender: User? = null) :
    DTO(json) {
    override val schema: JsonSchema =
        JsonSchema.of(
            Schemas.objectSchema()
                .optionalProperty("filter", Schemas.oneOf(Schemas.stringSchema(), CreateFilterDTO._schema))
                .optionalProperty("full_state", Schemas.booleanSchema())
                .optionalProperty(
                    "set_presence", Schemas.stringSchema()
                        .with(Keywords.pattern(Regex("""^(online|offline|unavailable)$""").toPattern()))
                )
                .optionalProperty("since", Schemas.stringSchema())
                .optionalProperty("timeout", Schemas.intSchema())
                .toJson()
        )

    override val permittedParams: Set<String>
        get() = json.map.keys

    override val paramNameTransformMapping: Map<String, String>
        get() = mapOf()

    override fun validate(): DTOValidationResult {
        return Helpers.validate(json, schema)
    }

}

