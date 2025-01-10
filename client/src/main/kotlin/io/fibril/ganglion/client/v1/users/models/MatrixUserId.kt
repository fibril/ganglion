package io.fibril.ganglion.client.v1.users.models

import com.google.inject.Inject
import io.vertx.json.schema.common.dsl.Keywords
import io.vertx.json.schema.common.dsl.Schemas

data class MatrixUserId @Inject constructor(private val matrixUserIdString: String) {
    constructor(username: String, domain: String) : this("@$username:$domain")

    companion object {
        val MatrixUserIdStringRegex = Regex("""^@[a-zA-Z0-9_\-=./]+:[a-zA-Z0-9\-._~]+$""")
        val MatrixUserIdStringSchema = Schemas.stringSchema()
            .with(Keywords.pattern(MatrixUserIdStringRegex.toPattern()))
            .with(Keywords.minLength(5))
            .with(Keywords.maxLength(510))
    }

    init {
        check(MatrixUserIdStringRegex.matches(matrixUserIdString)) {
            "Invalid UserId format"
        }
    }

    val localPart = matrixUserIdString.substringAfter("@").substringBefore(":")
    val domain = matrixUserIdString.substringAfter(":")

    override fun toString() = matrixUserIdString
}
