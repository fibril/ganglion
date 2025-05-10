package io.fibril.ganglion.clientServer.v1.filters

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.errors.ErrorCodes
import io.fibril.ganglion.clientServer.errors.RequestException
import io.fibril.ganglion.clientServer.errors.StandardErrorResponse
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.fibril.ganglion.clientServer.v1.filters.dtos.GetFilterDTO
import io.fibril.ganglion.clientServer.v1.filters.models.Filter
import io.vertx.core.Future
import io.vertx.pgclient.PgException


interface FilterService : Service<Filter> {
    suspend fun getFilterForUser(getFilterDTO: GetFilterDTO): Future<Filter>
}

class FilterServiceImpl @Inject constructor(
    private val filterRepository: FilterRepository,
) : FilterService {

    override val identifier = IDENTIFIER

    override suspend fun create(dto: DTO): Future<Filter> {
        try {
            val filter = filterRepository.save(dto)
            return Future.succeededFuture(filter)
        } catch (pgException: PgException) {
            return Future.failedFuture(RequestException.fromPgException(pgException))
        }
    }

    override suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<Filter>> {
        TODO()
    }

    override suspend fun findOne(id: String): Future<Filter?> {
        val filter = try {
            filterRepository.find(id)
        } catch (e: PgException) {
            return Future.failedFuture(RequestException.fromPgException(e))
        }
        return Future.succeededFuture(filter)
    }

    override suspend fun getFilterForUser(getFilterDTO: GetFilterDTO): Future<Filter> {
        val sender = getFilterDTO.sender ?: return Future.failedFuture(
            RequestException(
                statusCode = 401,
                "",
                StandardErrorResponse(ErrorCodes.M_FORBIDDEN, error = "No User").asJson()
            )
        )
        val filter = filterRepository.find(getFilterDTO.params().getString("filterId", ""))
        if (filter?.asJson()?.getString("user_id") != sender.principal().getString("sub")) {
            return Future.failedFuture(
                RequestException(
                    statusCode = 401,
                    "",
                    StandardErrorResponse(ErrorCodes.M_FORBIDDEN, error = "Illegal resource access").asJson()
                )
            )
        }

        return Future.succeededFuture(filter)
    }

    override suspend fun update(id: String, dto: DTO): Future<Filter> {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: String): Future<Filter> {
        TODO("Not yet implemented")
    }

    companion object {
        const val IDENTIFIER = "v1.filters.FilterService"
    }
}