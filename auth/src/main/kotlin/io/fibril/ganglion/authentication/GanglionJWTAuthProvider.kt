package io.fibril.ganglion.authentication

import com.google.inject.Inject
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import java.util.*

enum class TokenType {
    ACCESS,
    REFRESH
}

interface GanglionJWTAuthProvider {
    val provider: JWTAuth
    fun generateToken(tokenData: JsonObject, tokenType: TokenType): String
}

class GanglionJWTAuthProviderImpl @Inject constructor(private val vertx: Vertx) : GanglionJWTAuthProvider {
    companion object {
        private val secretsBundle = ResourceBundle.getBundle("secrets")

        private val applicationBundle = ResourceBundle.getBundle("application")

        const val ALGORITHM = "RS256"

        private val defaultAccessTokenData = JsonObject()
            .put("nbf", Date().time)
            .put("iat", Date().time)
            .put("exp", Date().time + (applicationBundle.getString("accessTokenLifetimeSecs").toLong() * 1000))
            .put("iss", applicationBundle.getString("domain"))
            .put("jti", UUID.randomUUID().toString())
            .put("type", TokenType.ACCESS.name)

        private val defaultRefreshTokenData = defaultAccessTokenData.mergeIn(
            JsonObject()
                .put("exp", Date().time + (applicationBundle.getString("refreshTokenLifetimeSecs").toLong() * 1000))
                .put("type", TokenType.REFRESH.name)
        )
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

    override fun generateToken(tokenData: JsonObject, tokenType: TokenType): String {
        val defaultTokenData = if (tokenType == TokenType.ACCESS) defaultAccessTokenData else defaultRefreshTokenData
        val tokenDataObject = defaultTokenData.mergeIn(tokenData)
        val token = provider.generateToken(tokenDataObject, JWTOptions().setAlgorithm(ALGORITHM))
        notifyTokenGenerated(tokenDataObject, token)
        return token
    }

    fun authenticate(bearerToken: String) = provider.authenticate(TokenCredentials(bearerToken))


    fun generateToken() = generateToken(JsonObject(), TokenType.ACCESS)

    /**
     * Notify any listening worker verticle about the token.
     * Said worker verticle should save it to the database
     */
    private fun notifyTokenGenerated(tokenDataObject: JsonObject, token: String) {
        // TODO:
    }
}