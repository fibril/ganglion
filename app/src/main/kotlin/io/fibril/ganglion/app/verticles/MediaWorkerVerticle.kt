package io.fibril.ganglion.app.verticles

import com.google.inject.Guice
import io.fibril.ganglion.clientServer.ClientModule
import io.fibril.ganglion.clientServer.utils.CoroutineHelpers
import io.fibril.ganglion.clientServer.utils.Utils
import io.fibril.ganglion.clientServer.v1.media.MediaDatabaseActions
import io.fibril.ganglion.clientServer.v1.media.MediaService
import io.fibril.ganglion.clientServer.v1.media.MediaVersionService
import io.fibril.ganglion.clientServer.v1.media.dtos.CreateMediaVersionDTO
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.ThreadingModel
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.file.AsyncFile
import io.vertx.core.file.OpenOptions
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.future.await
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

class MediaWorkerVerticle : CoroutineVerticle() {
    private lateinit var mediaVersionService: MediaVersionService
    private lateinit var mediaService: MediaService

    override suspend fun start() {
        val injector = Guice.createInjector(ClientModule(vertx))
        mediaVersionService = injector.getInstance(MediaVersionService::class.java)
        mediaService = injector.getInstance(MediaService::class.java)

        val eventBus = vertx.eventBus()

        eventBus.consumer(MediaDatabaseActions.MEDIA_VERSION_CREATED) { message ->
            CoroutineHelpers.usingCoroutineScopeWithIODispatcher {
                mediaVersionCreated(message)
                    .onSuccess { isSaved ->
                        println(if (isSaved) "media Version handled" else "Media Version was not original")
                    }
                    .onFailure { e ->
                        println("mediaVersion Failed from Verticle ${e.message}")
                    }
            }

        }
    }

    private suspend fun mediaVersionCreated(message: Message<JsonObject>): Future<Boolean> {
        try {
            val body = message.body()

            println("body $body")
            val versionName = body.getString("name")


            if (versionName != "original") return Future.succeededFuture(false)

            val media = mediaService.findOne(body.getString("media_id")).toCompletionStage().await()
                ?: return Future.succeededFuture(false)

            if (!media.isImage) return Future.succeededFuture(false)

            val mediaJson = media.asJson()
            println("mediajson $mediaJson")
            val croppedVersionNames = setOf(
                "crop32x32",
                "crop96x96",
                "scale320x240",
                "scale640x480",
                "scale800x600"
            )

            val imagePromise = Promise.promise<BufferedImage>()

            vertx.fileSystem().open(mediaJson.getString("uploaded_filename"), OpenOptions()) { res ->
                if (res.succeeded()) {
                    val asyncFile: AsyncFile = res.result()
                    asyncFile.endHandler {
                        asyncFile.close()
                    }
                    asyncFile.read(Buffer.buffer(), 0, 0, 4096) { readResult ->
                        if (readResult.succeeded()) {
                            val buffer: Buffer = readResult.result()
                            val imageBytes: ByteArray = buffer.bytes

                            try {
                                ByteArrayInputStream(imageBytes).use { bis ->
                                    imagePromise.complete(ImageIO.read(bis))
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        } else {
                            System.err.println("Failed to read file: ${readResult.cause().message}")
                        }
                    }
                }
            }

            val image = imagePromise.future().toCompletionStage().await()
            if (image != null) {
                val imageVersionMaps = createImageVersions(image, croppedVersionNames)
                for ((name, bImage) in imageVersionMaps) {
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    ImageIO.write(
                        bImage,
                        mediaJson.getString("content_type", "").substringAfter('/'),
                        byteArrayOutputStream
                    )
                    val imageBytes = byteArrayOutputStream.toByteArray()

                    val uploadedFileName = "file-uploads/${Utils.idGenerator()}"
                    val tempFile = File(uploadedFileName)
//                    tempFile.deleteOnExit() // Ensure the file is deleted when the JVM exits
                    vertx.fileSystem().writeFileBlocking(tempFile.absolutePath, Buffer.buffer(imageBytes))

                    val createMediaVersionDTO = CreateMediaVersionDTO(
                        JsonObject.of(
                            "media_id",
                            mediaJson.getString("media_id"),
                            "uploaded_filename",
                            uploadedFileName,
                            "name",
                            name,
                            "animated",
                            setOf(
                                "image/gif",
                                "image/apng",
                                "image/webp"
                            ).contains(mediaJson.getString("content_type")),
                            "file_size",
                            tempFile.length(),
                        ),
                        null
                    )
                    mediaVersionService.create(createMediaVersionDTO).toCompletionStage().await()
                }
            }

            return Future.succeededFuture(true)
        } catch (e: Exception) {
            return Future.failedFuture(e)
        }
    }

    private fun createImageVersions(
        bufferedImage: BufferedImage,
        versions: Set<String>
    ): Map<String, BufferedImage> {
        val result = mutableMapOf<String, BufferedImage>()
        for (version in versions) {
            var img: BufferedImage
            when (version) {
                "crop32x32" -> {
                    img = Scalr.resize(bufferedImage, 32, 32)
                }

                "crop96x96" -> {
                    img = Scalr.resize(bufferedImage, 96, 96)
                }

                "scale320x240" -> {
                    img = Scalr.resize(bufferedImage, 320, 240)
                }

                "scale640x480" -> {
                    img = Scalr.resize(bufferedImage, 640, 480)
                }

                "scale800x600" -> {
                    img = Scalr.resize(bufferedImage, 800, 600)
                }

                else -> img = bufferedImage
            }
            result[version] = img
        }
        return result
    }

    companion object {
        val deploymentOptions = DeploymentOptions()
            .setThreadingModel(ThreadingModel.WORKER).setInstances(2)
            .setWorkerPoolName("media-worker-pool")
            .setWorkerPoolSize(2)

    }
}