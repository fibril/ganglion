package io.fibril.ganglion.clientServer.utils.pagination

import io.fibril.ganglion.clientServer.Model
import io.fibril.ganglion.clientServer.utils.QueryUtils
import io.vertx.core.json.JsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


class KeySetPagination(val paginationDTO: PaginationDTO) {
    companion object {
        const val DEFAULT_LIMIT = 10
        const val DEFAULT_ORDER_BY = "DESC"

        @OptIn(ExperimentalEncodingApi::class)
        fun decodePaginationToken(paginationToken: String): String =
            Base64.decode(paginationToken).decodeToString()

        @OptIn(ExperimentalEncodingApi::class)
        fun encodeToken(cursor: String): String =
            Base64.encode(cursor.toByteArray())
    }

    val params = paginationDTO.params()


    fun prepareQuery(query: String): String {
        val orderBy = params.getString("orderBy") ?: DEFAULT_ORDER_BY
        val map = mutableMapOf("orderBy" to orderBy)
        // increase the limit by 1. If we get the entire limit as count of objects in dbResult, it means
        // that there must be next page
        map.put("limit", ((params.getString("limit")?.toInt() ?: DEFAULT_LIMIT) + 1).toString())

        val where =
            if (params.getString(PaginationDTO.CURRENT_TOKEN_KEY) != null && params.getString(PaginationDTO.END_TOKEN_KEY) != null) {
                val startId = decodePaginationToken(params.getString(PaginationDTO.CURRENT_TOKEN_KEY))
                val endId = decodePaginationToken(params.getString(PaginationDTO.END_TOKEN_KEY))
                """
                id ${if (orderBy == "DESC") "<=" else ">="} '${startId}' AND id < '${endId}'
            """.trimIndent()
            } else if (params.getString(PaginationDTO.CURRENT_TOKEN_KEY) != null) {
                val startId = decodePaginationToken(params.getString(PaginationDTO.CURRENT_TOKEN_KEY))
                println(startId)
                """
                id ${if (orderBy == "DESC") "<=" else ">="} '${startId}'
            """.trimIndent()
            } else "TRUE"

        map.put("where", where)

        return QueryUtils.prepareQueryFromMap(query, map)
    }

    inline fun <reified T : Model> usingQueryResult(dbResult: List<T>) =
        UnsafeForDirectUsageWithQueryResult(dbResult, paginationDTO)

    class UnsafeForDirectUsageWithQueryResult<T : Model>(
        private val dbResult: List<T>,
        originalPaginationDTO: PaginationDTO
    ) {
        val params = originalPaginationDTO.params()

        fun hasNextPage(): Boolean = dbResult.size > (params.getString("limit")?.toInt() ?: DEFAULT_LIMIT)

        val nextCursor: String?
            get() = if (hasNextPage()) encodeToken(dbResult.last().asJson().getString("id")) else null

        val prevCursor: String? = params.getString(PaginationDTO.CURRENT_TOKEN_KEY)


        val paginatedResult: PaginatedResult<T> =
            PaginatedResult(
                // exclude the extra fetched value
                chunk = if (hasNextPage()) dbResult.subList(0, dbResult.size - 1) else dbResult,
                next_batch = nextCursor,
                // TODO:- Revisit prev_batch logic
                prev_batch = if (prevCursor != nextCursor) prevCursor else null,
                additionalPayload = JsonObject()
            )
    }
}


enum class PaginationSortOrder {
    ASC,
    DESC
}
