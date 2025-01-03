package v1.users.models

import DTO
import Model
import io.vertx.core.json.JsonObject
import io.vertx.json.schema.JsonSchema
import io.vertx.json.schema.common.dsl.Schemas

data class User(
    val userId: String,
    private val profile: UserProfile? = null
) : Model() {

    override fun asJson(): JsonObject =
        JsonObject().put(USER_ID_KEY, userId).mergeIn(profile?.asJson() ?: JsonObject())


    override val schema: JsonSchema
        get() = JsonSchema.of(
            Schemas.objectSchema()
                .requiredProperty(USER_ID_KEY, Schemas.stringSchema())
                .toJson()
        )

    override fun validate(): Boolean {
        return DTO.validate(asJson(), schema)
    }

    companion object {

        val USER_ID_KEY = matrixFields.getString("user.id")

        fun fromJson(jsonObject: JsonObject): User {
            val hasProfileFields = jsonObject.size() > 1 && jsonObject.containsKey(USER_ID_KEY)

            return User(
                jsonObject.getString(USER_ID_KEY),
                if (hasProfileFields) UserProfile.fromJson(jsonObject) else null
            )
        }


    }


}
