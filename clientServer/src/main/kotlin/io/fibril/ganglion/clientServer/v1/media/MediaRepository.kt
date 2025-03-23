package io.fibril.ganglion.clientServer.v1.media

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.media.dtos.UpdateMediaDTO
import io.fibril.ganglion.clientServer.v1.media.models.Media
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.await

interface MediaRepository : Repository<Media> {
    suspend fun findOneForVersion(id: String, versionName: String): Media?
}

class MediaRepositoryImpl @Inject constructor(private val database: PGDatabase) : MediaRepository {
    override suspend fun save(dto: DTO): Media {
        val userId = dto.sender?.principal()?.getString("sub")
        val params = dto.params()
        val result: Promise<JsonObject> = Promise.promise()
        val client = database.client()
        client.preparedQuery(CREATE_MEDIA_QUERY).execute(
            Tuple.of(
                userId,
                params.getString("content_type"),
                params.getString("content_disposition"),
                params.getString("content_transfer_encoding"),
                params.getString("title"),
                params.getString("description"),
                params.getString("filename"),
                params.getString("charset")
            )
        )
            .eventually { _ -> client.close() }
            .onSuccess {
                try {
                    result.complete(it.first().toJson())
                } catch (e: NoSuchElementException) {
                    throw PgException(
                        e.message,
                        "SEVERE",
                        "500",
                        e.message
                    )
                }
            }
            .onFailure { err ->
                throw PgException(
                    err.message,
                    "SEVERE",
                    "500",
                    err.message
                )
            }

        val resPayload = result.future().toCompletionStage().await()
        return Media(resPayload)
    }

    override suspend fun find(id: String): Media? {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()

        client.preparedQuery(GET_MEDIA_QUERY).execute(Tuple.of(id)).onSuccess { res ->
            run {
                try {
                    result.complete(res.first().toJson())
                } catch (e: NoSuchElementException) {
                    result.complete(null)
                }
            }
        }
            .onFailure { err ->
                run {
                    throw PgException(
                        err.message,
                        "SEVERE",
                        "500",
                        err.message
                    )
                }
            }

        val resPayload = result.future().toCompletionStage().await() ?: return null

        return Media(resPayload)
    }

    override suspend fun findOneForVersion(id: String, versionName: String): Media? {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()

        client.preparedQuery(GET_MEDIA_FOR_VERSION_QUERY).execute(Tuple.of(id, versionName)).onSuccess { res ->
            run {
                try {
                    result.complete(res.first().toJson())
                } catch (e: NoSuchElementException) {
                    result.complete(null)
                }
            }
        }
            .onFailure { err ->
                run {
                    throw PgException(
                        err.message,
                        "SEVERE",
                        "500",
                        err.message
                    )
                }
            }

        val resPayload = result.future().toCompletionStage().await() ?: return null

        return Media(resPayload)
    }

    override suspend fun findAll(query: String): List<Media> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, dto: DTO): Media? {
        val client = database.client()
        val result: Promise<Media> = Promise.promise()

        val updateMediaDTO = (dto as UpdateMediaDTO)

        val params = updateMediaDTO.params()
        val keys = params.map.keys

        if (keys.size == 0) return null

        val values = mutableListOf<Any>().apply {
            for (key in keys) {
                add("$key = '${params.getValue(key)}'")
            }
        }

        val queryString = """
                UPDATE media SET ${values.joinToString(", ")}
                WHERE id = '$id' 
                RETURNING *;
            """.trimIndent()

        client.query(
            queryString
        ).execute()
            .onSuccess { res ->
                val response = res.firstOrNull()?.toJson()
                result.complete(if (response != null) Media(response) else null)
            }
            .onFailure { err ->
                throw PgException(
                    err.message,
                    "SEVERE",
                    "500",
                    err.message
                )
            }
            .eventually { _ -> client.close() }

        val media = result.future().toCompletionStage().await()

        return media
    }

    override suspend fun delete(id: String): Media? {
        TODO("Not yet implemented")
    }

    companion object {
        val CREATE_MEDIA_QUERY = ResourceBundleConstants.mediaQueries.getString("createMedia")
        val GET_MEDIA_QUERY = ResourceBundleConstants.mediaQueries.getString("getMedia")
        val GET_MEDIA_FOR_VERSION_QUERY = ResourceBundleConstants.mediaQueries.getString("getMediaForVersion")
    }

}