package io.fibril.ganglion.app
import io.vertx.kotlin.coroutines.CoroutineVerticle

class MainVerticle: CoroutineVerticle() {
    override suspend fun start() {
        vertx.createHttpServer()
            .listen(8888)
            .onComplete { http ->
                if (http.succeeded()) {
                    println("HTTP server started on port 8888")
                } else {
                    println("HTTP server failed to start")
                }

            }
    }
}