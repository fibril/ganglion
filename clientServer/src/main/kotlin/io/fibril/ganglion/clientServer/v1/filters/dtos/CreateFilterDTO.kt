package io.fibril.ganglion.clientServer.v1.filters.dtos

import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.DTOValidationResult
import io.fibril.ganglion.clientServer.v1.filters.models.EventFilter
import io.fibril.ganglion.clientServer.v1.filters.models.RoomFilter
import io.fibril.ganglion.clientServer.v1.users.models.MatrixUserId
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.ext.auth.User as VertxUser

data class CreateFilterDTO(val json: JsonObject, override val sender: VertxUser?) : DTO(json) {
    companion object {
        val _schema = Schemas.objectSchema()
            .requiredProperty("userId", MatrixUserId.createMatrixUserIdStringSchema())
            .optionalProperty("account_data", EventFilter.eventFilterSchemaBuilder())
            .optionalProperty("event_fields", Schemas.arraySchema().items(Schemas.stringSchema()))
            .optionalProperty(
                "event_format",
                Schemas.stringSchema().with(Keywords.pattern("""^(client|federation)$""".toPattern()))
            )
            .optionalProperty("presence", EventFilter.eventFilterSchemaBuilder())
            .optionalProperty("room", RoomFilter.roomFilterSchemaBuilder())
    }


    override val schema: JsonSchema = JsonSchema.of(_schema.toJson())

    override val permittedParams: Set<String> = json.map.keys
    override val paramNameTransformMapping: Map<String, String> = mapOf()

    override fun validate(): DTOValidationResult {
        return DTO.Helpers.validate(json, schema)
    }

}