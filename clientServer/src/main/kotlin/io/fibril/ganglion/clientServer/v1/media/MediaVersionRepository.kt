package io.fibril.ganglion.clientServer.v1.media

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.media.dtos.CreateMediaVersionDTO
import io.fibril.ganglion.clientServer.v1.media.models.MediaVersion
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventDatabaseActions
import io.fibril.ganglion.clientServer.v1.roomEvents.RoomEventNames
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.await

interface MediaVersionRepository : Repository<MediaVersion> {
    suspend fun findAllByMediaId(mediaId: String): List<MediaVersion>
}

class MediaVersionRepositoryImpl @Inject constructor(private val database: PGDatabase, private val vertx: Vertx) :
    MediaVersionRepository {
    override suspend fun save(dto: DTO): MediaVersion {
        val createMediaDTO = dto as CreateMediaVersionDTO
        val params = createMediaDTO.params()
        val result: Promise<JsonObject> = Promise.promise()
        val client = database.client()
        client.preparedQuery(CREATE_MEDIA_VERSION_QUERY).execute(
            Tuple.of(
                params.getString("media_id"),
                params.getString("uploaded_filename"),
                params.getString("name"),
                params.getInteger("height"),
                params.getInteger("width"),
                params.getBoolean("animated"),
                params.getInteger("file_size"),
                params.getString("remote_url")
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
        
        val eventBus = vertx.eventBus()
        eventBus.send(MediaDatabaseActions.MEDIA_VERSION_CREATED, resPayload)

        return MediaVersion(resPayload)
    }

    override suspend fun find(id: String): MediaVersion? {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()

        client.preparedQuery(GET_MEDIA_VERSION_QUERY).execute(Tuple.of(id)).onSuccess { res ->
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
        val client = database.client()
        val result: Promise<List<MediaVersion>> = Promise.promise()

        client.preparedQuery(FIND_ALL_MEDIA_VERSION_BY_MEDIA_ID_QUERY).execute(Tuple.of(mediaId)).onSuccess { res ->
            result.complete(res.map { MediaVersion(it.toJson()) })
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

        val mediaVersions = result.future().toCompletionStage().await()

        return mediaVersions
    }

    override suspend fun update(id: String, dto: DTO): MediaVersion? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): MediaVersion? {
        TODO("Not yet implemented")
    }

    companion object {
        val CREATE_MEDIA_VERSION_QUERY = ResourceBundleConstants.mediaQueries.getString("createMediaVersion")
        val GET_MEDIA_VERSION_QUERY = ResourceBundleConstants.mediaQueries.getString("getMediaVersion")
        val FIND_ALL_MEDIA_VERSION_BY_MEDIA_ID_QUERY =
            ResourceBundleConstants.mediaQueries.getString("findAllMediaVersionsByMediaId")
    }

}