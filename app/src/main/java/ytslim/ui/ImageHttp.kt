package ytslim.ui

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.network.DeDupeConcurrentRequestStrategy
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * As miniaturas e as fotos dos canais. Vêm pelo OkHttp que o motor já traz (o 3.12), que fala
 * HTTP/2 com o servidor de imagens do YouTube: muitas miniaturas numa conexão só, em paralelo. Antes
 * vinham pelo HttpURLConnection, em HTTP/1.1, poucas por vez, e ao rolar rápido elas faziam fila.
 *
 * Dois pedidos da mesma imagem ao mesmo tempo (o adiantado de [Thumbnails] e o da tela) baixam uma
 * vez só.
 */
object ImageHttp {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dispatcher(Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 24
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @OptIn(ExperimentalCoilApi::class)
    fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(
                NetworkFetcher.Factory(
                    networkClient = { OkHttp3Client(client) },
                    concurrentRequestStrategy = { DeDupeConcurrentRequestStrategy() },
                ),
            )
        }
        .build()
}

/** O [NetworkClient] do Coil sobre o OkHttp 3 (o adaptador oficial do Coil pede o OkHttp 4+, que quebra o motor). */
private class OkHttp3Client(private val client: OkHttpClient) : NetworkClient {
    override suspend fun <T> executeRequest(request: NetworkRequest, block: suspend (response: NetworkResponse) -> T): T {
        val builder = Request.Builder().url(request.url)
        request.headers.asMap().forEach { (name, values) -> values.forEach { builder.addHeader(name, it) } }
        // O Coil só faz GET para imagens.
        val response = client.newCall(builder.get().build()).await()
        return response.use { r ->
            val headers = NetworkHeaders.Builder().apply {
                val h = r.headers()
                for (i in 0 until h.size()) add(h.name(i), h.value(i))
            }.build()
            block(
                NetworkResponse(
                    code = r.code(),
                    requestMillis = r.sentRequestAtMillis(),
                    responseMillis = r.receivedResponseAtMillis(),
                    headers = headers,
                    body = r.body()?.source()?.let(::NetworkResponseBody),
                    delegate = r,
                ),
            )
        }
    }

    /** Executa o pedido sem prender uma thread; cancelar a corrotina cancela o pedido. */
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { runCatching { cancel() } }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, _, _ -> response.close() }
            }

            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) continuation.resumeWithException(e)
            }
        })
    }
}
