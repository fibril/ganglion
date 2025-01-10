package v1.devices

import DTO
import Repository
import com.google.inject.Inject
import io.fibril.ganglion.storage.impl.PGDatabase
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.future.asDeferred
import utils.ResourceBundleConstants
import v1.devices.dtos.CreateDeviceDTO


interface DeviceRepository : Repository<Device>

class DeviceRepositoryImpl @Inject constructor(private val database: PGDatabase) : DeviceRepository {

    override suspend fun save(dto: DTO): Device {
        val client = database.client()
        val queryResult =
            client.preparedQuery(CREATE_DEVICE_QUERY).execute(
                Tuple.of(
                    (dto as CreateDeviceDTO).json.getString("userId"),
                    dto.json.getString("deviceId") ?: null,
                    dto.json.getString("deviceName") ?: "Device"
                )
            )
                .toCompletionStage().asDeferred()

        val rowSet = queryResult.await()

        val json = rowSet.first().toJson()
        return Device(json.getString("id"), json.getString("user_id"), json.getString("name"))

    }

    override suspend fun find(id: String): Device {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    override suspend fun findAll(): List<Device> {
        throw IllegalAccessException("Illegal access of stubbed function")
    }

    companion object {
        val CREATE_DEVICE_QUERY = ResourceBundleConstants.deviceQueries.getString("createDevice")
    }
}