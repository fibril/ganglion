package io.fibril.ganglion.client

import io.vertx.core.Future

interface Service<T> {

    /**
     * Used to identify this Service in a map of services
     */
    val identifier: String
    suspend fun create(dto: DTO): Future<T>
    suspend fun findOne(id: String): Future<T>
    suspend fun findAll(): Future<List<T>>
    suspend fun update(id: String, dto: DTO): Future<T>
    suspend fun remove(id: String): Future<Boolean>

}