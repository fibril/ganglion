package io.fibril.ganglion.clientServer.v1.openSearch

import com.google.inject.Inject
import io.fibril.ganglion.clientServer.v1.openSearch.models.PaginatedSearchResult
import io.fibril.ganglion.storage.impl.GanglionOpenSearch
import io.vertx.core.Future

interface OpenSearchService {
    
}

class OpenSearchServiceImpl @Inject constructor(
    private val ganglionOpenSearch: GanglionOpenSearch,
) {
    suspend fun search(): Future<PaginatedSearchResult> {
        return Future.succeededFuture(PaginatedSearchResult())
    }
}