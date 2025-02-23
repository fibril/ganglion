package io.fibril.ganglion.clientServer.v1.devices

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Service
import io.fibril.ganglion.clientServer.utils.pagination.PaginatedResult
import io.fibril.ganglion.clientServer.utils.pagination.PaginationDTO
import io.vertx.core.Future

interface DeviceService : Service<Device> {
}

class DeviceServiceImpl @Inject constructor(
    val deviceRepository: DeviceRepository
) : DeviceService {

    companion object {
        private const val IDENTIFIER = "v1.devices.DeviceService"
    }

    override val identifier = IDENTIFIER

    override suspend fun create(dto: DTO): Future<Device> {
        TODO()
    }

    override suspend fun findOne(id: String): Future<Device?> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun findAll(paginationDTO: PaginationDTO): Future<PaginatedResult<Device>> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun update(id: String, dto: DTO): Future<Device> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun remove(id: String): Future<Device> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }
}