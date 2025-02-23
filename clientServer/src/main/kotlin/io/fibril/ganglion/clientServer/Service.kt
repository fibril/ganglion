package io.fibril.ganglion.clientServer

import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.vertx.core.Future

interface Service<T : Model> {

    /**
     * Used to identify this Service in a map of services
     */
    val identifier: String
    suspend fun create(dto: DTO): Future<T>
    suspend fun findOne(id: String): Future<T?>
    suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<T>>
    suspend fun update(id: String, dto: DTO): Future<T>
    suspend fun remove(id: String): Future<T>

}