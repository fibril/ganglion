package io.fibril.ganglion.client.v1.devices

import com.google.inject.Inject
import io.fibril.ganglion.client.DTO
import io.fibril.ganglion.client.Repository
import io.fibril.ganglion.client.utils.ResourceBundleConstants
import io.fibril.ganglion.client.utils.Utils
import io.fibril.ganglion.client.v1.devices.dtos.CreateDeviceDTO
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred


interface DeviceRepository : Repository<Device>

class DeviceRepositoryImpl @Inject constructor(private val database: PGDatabase) : DeviceRepository {

    override suspend fun save(dto: DTO): Device? {
        val client = database.client()
        val queryResult =
            client.preparedQuery(CREATE_DEVICE_QUERY).execute(
                Tuple.of(
                    (dto as CreateDeviceDTO).json.getString("device_id") ?: Utils.idGenerator(),
                    dto.json.getString("user_id"),
                    dto.json.getString("display_name") ?: "Device"
                )
            )
                .toCompletionStage().asDeferred()

        val rowSet = queryResult.await()

        return try {
            Device.fromJson(rowSet.first().toJson())
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
            Device.fromJson(rowSet.first().toJson())
        } catch (e: NoSuchElementException) {
            null
        } finally {
            client.close()
        }
    }

    override suspend fun findAll(): List<Device> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    companion object {
        val CREATE_DEVICE_QUERY = ResourceBundleConstants.deviceQueries.getString("createDevice")
        val FIND_DEVICE_QUERY = ResourceBundleConstants.deviceQueries.getString("findDevice")
    }
}