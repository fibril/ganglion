package io.fibril.ganglion.clientServer.v1.filters

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.extensions.exclude
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.v1.filters.models.Filter
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgException
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.await

interface FilterRepository : Repository<Filter>

class FilterRepositoryImpl @Inject constructor(private val database: PGDatabase) : FilterRepository {
    override suspend fun save(dto: DTO): Filter {
        val params = dto.params()
        val userId = params.getString("userId")
        val result: Promise<JsonObject> = Promise.promise()
        val client = database.client()
        client.preparedQuery(CREATE_FILTER_QUERY).execute(
            Tuple.of(
                userId,
                params.exclude("userId")
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
        return Filter(resPayload)
    }

    override suspend fun find(id: String): Filter? {
        val client = database.client()
        val result: Promise<JsonObject?> = Promise.promise()

        client.preparedQuery(GET_FILTER_QUERY).execute(Tuple.of(id)).onSuccess { res ->
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

        return Filter(resPayload)
    }

    override suspend fun findAll(query: String): List<Filter> {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: String, dto: DTO): Filter? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): Filter? {
        TODO("Not yet implemented")
    }

    companion object {
        val CREATE_FILTER_QUERY = ResourceBundleConstants.filterQueries.getString("createFilter")
        val GET_FILTER_QUERY = ResourceBundleConstants.filterQueries.getString("getFilter")
    }

}