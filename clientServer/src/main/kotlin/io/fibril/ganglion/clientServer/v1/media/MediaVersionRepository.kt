package io.fibril.ganglion.clientServer.v1.media

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.media.dtos.CreateMediaVersionDTO
import io.fibril.ganglion.clientServer.v1.media.models.MediaVersion
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.await

interface MediaVersionRepository : Repository<MediaVersion> {
    suspend fun findAllByMediaId(mediaId: String): List<MediaVersion>
}

class MediaVersionRepositoryImpl @Inject constructor(private val database: PGDatabase) : MediaVersionRepository {
    override suspend fun save(dto: DTO): MediaVersion {
        val createMediaDTO = dto as CreateMediaVersionDTO
        val params = createMediaDTO.params()
        val result: Promise<JsonObject> = Promise.promise()
        val client = database.client()
        client.preparedQuery(CREATE_MEDIA_VERSION_QUERY).execute(
            Tuple.of(
                "media_id", params.getString("media_id"),
                "uploaded_filename", params.getString("uploaded_filename"),
                "name", params.getString("name"),
                "height", params.getString("height"),
                "width", params.getString("width"),
                "animated", params.getString("animated"),
                "file_size", params.getString("file_size"),
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
        return MediaVersion(resPayload)
    }

    override suspend fun find(id: String): MediaVersion? {
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

        return MediaVersion(resPayload)
    }

    override suspend fun findAll(query: String): List<MediaVersion> {
        TODO("Not yet implemented")
    }

    override suspend fun findAllByMediaId(mediaId: String): List<MediaVersion> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, dto: DTO): MediaVersion? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): MediaVersion? {
        TODO("Not yet implemented")
    }

    companion object {
        val CREATE_MEDIA_QUERY = ResourceBundleConstants.mediaQueries.getString("createMedia")
        val CREATE_MEDIA_VERSION_QUERY = ResourceBundleConstants.mediaQueries.getString("createMediaVersion")
        val GET_MEDIA_QUERY = ResourceBundleConstants.mediaQueries.getString("getMedia")
    }

}