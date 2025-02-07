package io.fibril.ganglion.clientServer

import io.vertx.sqlclient.DatabaseException

interface Repository<T> {
    @Throws(DatabaseException::class)
    suspend fun save(dto: DTO): T?

    @Throws(DatabaseException::class)
    suspend fun find(id: String): T?


    @Throws(DatabaseException::class)
    suspend fun findAll(): List<T>


    @Throws(DatabaseException::class)
    suspend fun update(id: String, dto: DTO): T?

    @Throws(DatabaseException::class)
    suspend fun delete(id: String): T?
}