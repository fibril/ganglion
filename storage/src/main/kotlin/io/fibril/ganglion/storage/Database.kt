package io.fibril.ganglion.storage

import io.vertx.pgclient.pubsub.PgSubscriber
import io.vertx.sqlclient.Pool


interface Database<Client> {
    suspend fun client(): Client

    suspend fun pool(): Pool

    suspend fun subscriber(): PgSubscriber
}