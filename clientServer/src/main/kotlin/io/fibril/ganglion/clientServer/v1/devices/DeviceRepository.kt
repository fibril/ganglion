package io.fibril.ganglion.clientServer.v1.devices

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.DTO
import io.fibril.ganglion.clientServer.Repository
import io.fibril.ganglion.clientServer.utils.ResourceBundleConstants
import io.fibril.ganglion.clientServer.utils.Utils
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred


interface DeviceRepository : Repository<Device>

class DeviceRepositoryImpl @Inject constructor(private val database: PGDatabase) : DeviceRepository {

    override suspend fun save(dto: DTO): Device? {
        val params = dto.params()
        val client = database.client()
        val queryResult =
            client.preparedQuery(CREATE_DEVICE_QUERY).execute(
                Tuple.of(
                    params.getString("device_id") ?: Utils.idGenerator(),
                    params.getString("user_id"),
                    params.getString("display_name") ?: "Device"
                )
            )
                .toCompletionStage().asDeferred()

        val rowSet = queryResult.await()

        return try {
            Device(rowSet.first().toJson())
        } catch (e: NoSuchElementException) {
            null
        } finally {
            client.close()
        }
    }

    override suspend fun find(id: String): Device? {
        val client = database.client()
        val queryResult =
            client.preparedQuery(FIND_DEVICE_QUERY).execute(
                Tuple.of(id)
            )
                .toCompletionStage().asDeferred()

        val rowSet = queryResult.await()

        return try {
            Device(rowSet.first().toJson())
        } catch (e: NoSuchElementException) {
            null
        } finally {
            client.close()
        }
    }

    override suspend fun findAll(): List<Device> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun update(dto: DTO): Device? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: String): Device? {
        TODO("Not yet implemented")
    }

    companion object {
        val CREATE_DEVICE_QUERY = ResourceBundleConstants.deviceQueries.getString("createDevice")
        val FIND_DEVICE_QUERY = ResourceBundleConstants.deviceQueries.getString("findDevice")
    }
}