package ytslim.player

import android.content.Context
import android.net.http.HttpEngine
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresExtension
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.HttpEngineDataSource
import java.io.File
import java.util.concurrent.Executors

/**
 * Por onde o player baixa o vídeo. No Android 14 (e no 12 e 13 com o módulo de rede atualizado), pelo
 * HttpEngine do próprio sistema: HTTP/2 e HTTP/3 (QUIC), o mesmo protocolo que o app do YouTube usa
 * com os servidores de vídeo, com menos idas e voltas para começar e para cada pedaço. Antes disso
 * (a TV Hisense tem Android 11), o cliente HTTP padrão do Media3, como era.
 */
@OptIn(UnstableApi::class)
internal object MediaHttp {
    private const val TAG = "Doorglow"
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    fun factory(context: Context, userAgent: String): HttpDataSource.Factory {
        if (modern) {
            runCatching { return Modern.factory(context.applicationContext, userAgent) }
                .onFailure { Log.w(TAG, "HttpEngine unavailable, using the default HTTP client", it) }
        }
        return DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
    }

    /** O HttpEngine chegou no Android 14 e, pelo módulo de rede atualizável, no 12 e no 13. */
    private val modern: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            runCatching { SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 7 }.getOrDefault(false)
    }

    /** Separado para as classes novas só serem carregadas onde existem. */
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    private object Modern {
        @Volatile private var engine: HttpEngine? = null
        private val executor = Executors.newSingleThreadExecutor { Thread(it, "doorglow-http") }

        fun factory(context: Context, userAgent: String): HttpDataSource.Factory =
            HttpEngineDataSource.Factory(engine(context), executor)
                .setUserAgent(userAgent)
                .setConnectionTimeoutMs(CONNECT_TIMEOUT_MS)
                .setReadTimeoutMs(READ_TIMEOUT_MS)

        /** Um só por processo: ele guarda as conexões e o que aprendeu de cada servidor (QUIC). */
        private fun engine(context: Context): HttpEngine = engine ?: synchronized(this) {
            engine ?: run {
                // Guardar no disco o que o motor aprende dos servidores deixa o próximo vídeo já começar por QUIC.
                val dir = File(context.cacheDir, "httpengine").apply { mkdirs() }
                HttpEngine.Builder(context)
                    .setStoragePath(dir.absolutePath)
                    .setEnableHttpCache(HttpEngine.Builder.HTTP_CACHE_DISABLED, 0)
                    .setEnableQuic(true)
                    .setEnableHttp2(true)
                    .setEnableBrotli(true)
                    .build()
                    .also { engine = it }
            }
        }
    }
}
