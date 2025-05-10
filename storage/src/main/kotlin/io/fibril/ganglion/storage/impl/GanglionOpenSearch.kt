package io.fibril.ganglion.storage.impl

import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.reactor.ssl.TlsDetails
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext


class GanglionOpenSearch {
    companion object {
        private val environment = System.getProperty("ganglion.environment")
        private val dbBundle = ResourceBundle.getBundle("${environment}.database")
        private val hostScheme = dbBundle.getString("ganglion.opensearch.hostScheme")
    }


    private val hostNames = dbBundle.getString("ganglion.opensearch.hostnames").split(",").map { it.trim() }
    private var hosts = hostNames.map { hostName -> (HttpHost(hostScheme, hostName, 9200)) }
    private val credentialsProvider: BasicCredentialsProvider = BasicCredentialsProvider()
    private val sslContext: SSLContext = SSLContextBuilder
        .create()
        .loadTrustMaterial(
            null
        ) { chains: Array<X509Certificate?>?, authType: String? -> true }
        .build()

    private val builder: ApacheHttpClient5TransportBuilder =
        ApacheHttpClient5TransportBuilder.builder(*hosts.toTypedArray())

    init {

        val environment = System.getProperty("ganglion.environment")
        val dbBundle = ResourceBundle.getBundle("${environment}.database")

        credentialsProvider.setCredentials(
            AuthScope(hosts.first()),
            UsernamePasswordCredentials(
                dbBundle.getString("ganglion.opensearch.username"),
                dbBundle.getString("ganglion.opensearch.password").toCharArray()
            )
        )

        builder.setHttpClientConfigCallback { httpClientBuilder: HttpAsyncClientBuilder ->
            val tlsStrategy = ClientTlsStrategyBuilder.create()
                .setSslContext(sslContext)
                .setTlsDetailsFactory({ sslEngine ->
                    TlsDetails(sslEngine.session, sslEngine.applicationProtocol)
                })
                .build()
            val connectionManager = PoolingAsyncClientConnectionManagerBuilder
                .create()
                .setTlsStrategy(tlsStrategy)
                .build()
            httpClientBuilder
                .setDefaultCredentialsProvider(credentialsProvider)
                .setConnectionManager(connectionManager)
        }

    }

    fun client(): OpenSearchClient {
        val transport = builder.build()
        return OpenSearchClient(transport)
    }
}