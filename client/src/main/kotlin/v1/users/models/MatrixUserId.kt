package v1.users.models

import com.google.inject.Inject

data class MatrixUserId @Inject constructor(private val matrixUserIdString: String) {
    constructor(username: String, domain: String) : this("@$username:$domain")

    companion object {
        val MatrixUserIdStringRegex = Regex("""^@[a-zA-Z0-9_\-=./]+:[a-zA-Z0-9\-._~]+$""")
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
