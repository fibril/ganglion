import com.google.inject.Inject
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import java.util.*


interface JWTAuthProvider {
    val provider: JWTAuth
    fun generateToken(tokenData: JsonObject): String
}

class JWTAuthProviderImpl @Inject constructor(private val vertx: Vertx) : JWTAuthProvider {
    companion object {
        private val secretsBundle = ResourceBundle.getBundle("secrets")

        private val applicationBundle = ResourceBundle.getBundle("application")

        const val ALGORITHM = "RS256"

        private val defaultTokenData = JsonObject()
            .put("iat", Date().time)
            .put("exp", Date().time + applicationBundle.getString("jwtTokenLifetimeSecs").toLong())
            .put("iss", applicationBundle.getString("domain"))
            .put("jti", UUID.randomUUID().toString())
    }

    override val provider: JWTAuth
        get() = JWTAuth.create(
            vertx, JWTAuthOptions()
                .addPubSecKey(
                    PubSecKeyOptions()
                        .setAlgorithm(ALGORITHM)
                        .setBuffer(secretsBundle.getString("publicKey"))
                )
                .addPubSecKey(
                    PubSecKeyOptions()
                        .setAlgorithm(ALGORITHM)
                        .setBuffer(secretsBundle.getString("privateKey"))
                )
        )

    override fun generateToken(tokenData: JsonObject): String =
        provider.generateToken(defaultTokenData.mergeIn(tokenData), JWTOptions().setAlgorithm(ALGORITHM))


    fun generateToken() = generateToken(JsonObject())
}