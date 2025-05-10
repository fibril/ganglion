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
    fun generateToken(tokenData: JsonObject, tokenType: TokenType, notificationChannelName: String? = null): String
}

class GanglionJWTAuthProviderImpl @Inject constructor(private val vertx: Vertx) : GanglionJWTAuthProvider {
    companion object {
        private val ENVIRONMENT = System.getProperty("ganglion.environment")
        private val secretsBundle = ResourceBundle.getBundle("${ENVIRONMENT}.secrets")

        private val applicationBundle = ResourceBundle.getBundle("${ENVIRONMENT}.application")

        const val ALGORITHM = "RS256"

        private val defaultAccessTokenData = JsonObject()
            .put("nbf", Date().time / 1000L)
            .put("iat", Date().time / 1000L)
            .put("exp", (Date().time / 1000L) + (applicationBundle.getString("accessTokenLifetimeSecs").toLong()))
            .put("iss", applicationBundle.getString("domain"))
            .put("jti", UUID.randomUUID().toString().replace("-", ""))
            .put("type", TokenType.ACCESS.name)

        private val defaultRefreshTokenData = defaultAccessTokenData.copy().apply {
            put("exp", (Date().time / 1000L) + (applicationBundle.getString("refreshTokenLifetimeSecs").toLong()))
            put("type", TokenType.REFRESH.name)
            put("jti", UUID.randomUUID().toString().replace("-", ""))
        }

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

    override fun generateToken(tokenData: JsonObject, tokenType: TokenType, notificationChannelName: String?): String {
        val defaultTokenData = if (tokenType == TokenType.ACCESS) defaultAccessTokenData else defaultRefreshTokenData
        val tokenDataObject = defaultTokenData.copy().mergeIn(tokenData)
        val token = provider.generateToken(tokenDataObject, JWTOptions().setAlgorithm(ALGORITHM))
        notifyTokenGenerated(tokenDataObject, token, notificationChannelName)
        return token
    }

    fun authenticate(bearerToken: String) =
        provider.authenticate(TokenCredentials(bearerToken.substringAfter("Bearer ")))


    /**
     * Notify any listening worker verticle about the generated token.
     * Said worker verticle should save it to the database
     */
    private fun notifyTokenGenerated(tokenDataObject: JsonObject, token: String, channel: String?) {
        if (channel != null)
            vertx.eventBus().send(channel, tokenDataObject.put("token", token))
    }
}