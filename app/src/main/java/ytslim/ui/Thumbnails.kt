package ytslim.ui

import android.content.Context
import android.graphics.Canvas
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil3.Image
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.CachePolicy
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.Options
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import ytslim.data.Video

/**
 * Miniaturas. O Coil já guarda as imagens na memória e no disco; aqui o app adianta o download
 * das miniaturas de cada página que chega, para elas aparecerem na hora ao rolar a lista.
 */
object Thumbnails {
    private const val MAX_REMEMBERED = 3000
    private val requested = HashSet<String>()

    /** Imagem vazia devolvida pelo [DiskOnlyDecoder] (o prefetch não grava na memória, só no disco). */
    private object EmptyImage : Image {
        override val size: Long = 0L
        override val width: Int = 0
        override val height: Int = 0
        override val shareable: Boolean = true
        override fun draw(canvas: Canvas) = Unit
    }

    /**
     * Fecha a fonte depois de o [coil3.network.NetworkFetcher] gravar a resposta no cache de disco,
     * sem decodificar JPEG nem disputar as 4 vagas do BitmapFactoryDecoder com os cards na tela.
     */
    private val DiskOnlyDecoder = Decoder.Factory { result: SourceFetchResult, _: Options, _: ImageLoader ->
        Decoder {
            result.source.close()
            DecodeResult(EmptyImage, isSampled = false)
        }
    }

    /** Baixa para o cache de disco (sem ocupar a memória) as miniaturas que ainda não foram pedidas. */
    fun prefetch(context: Context, urls: List<String?>) {
        enqueue(context, urls)
    }

    internal fun enqueue(context: Context, urls: List<String?>): Map<String, Disposable> {
        val fresh = synchronized(requested) {
            if (requested.size > MAX_REMEMBERED) requested.clear()
            urls.filterNotNull().filter { it.isNotBlank() && requested.add(it) }
        }
        if (fresh.isEmpty()) return emptyMap()
        val loader = SingletonImageLoader.get(context)
        val out = HashMap<String, Disposable>(fresh.size)
        fresh.forEach { url ->
            out[url] = loader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .decoderFactory(DiskOnlyDecoder)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .listener(
                        onCancel = { forget(url) },
                        onError = { _, _ -> forget(url) },
                    )
                    .build(),
            )
        }
        return out
    }

    private fun forget(url: String) {
        synchronized(requested) { requested.remove(url) }
    }
}

/**
 * Adianta as miniaturas dos [AHEAD] vídeos logo depois dos que estão na tela, conforme a lista rola (a
 * tela vem primeiro). Antes, cada página que chegava adiantava a lista inteira: no Watch later, com as
 * páginas carregando à frente, eram centenas de miniaturas de uma vez, disputando a rede e o
 * processador com as que estavam na tela.
 */
@OptIn(FlowPreview::class)
@Composable
fun PrefetchThumbnails(videos: List<Video>, grid: LazyGridState) {
    val context = LocalContext.current.applicationContext
    val current by rememberUpdatedState(videos)
    LaunchedEffect(grid) {
        delay(400)
        val active = HashMap<String, Disposable>()
        try {
            // O índice do último item na tela (cabeçalhos contam, mas a janela é folgada) e a lista atual.
            snapshotFlow { (grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) to current }
                .distinctUntilChanged()
                .debounce(SCROLL_DEBOUNCE_MS)
                .collect { (last, list) ->
                    val from = (last + 1).coerceAtMost(list.size)
                    val to = (from + AHEAD).coerceAtMost(list.size)
                    val urls = ArrayList<String>(to - from)
                    for (i in from until to) list[i].thumbnailUrl?.takeIf { it.isNotBlank() }?.let(urls::add)
                    val wanted = urls.toHashSet()
                    val it = active.entries.iterator()
                    while (it.hasNext()) {
                        val (url, job) = it.next()
                        if (job.isDisposed || url !in wanted) {
                            job.dispose()
                            it.remove()
                        }
                    }
                    active.putAll(Thumbnails.enqueue(context, urls))
                }
        } finally {
            active.values.forEach(Disposable::dispose)
        }
    }
}

/** Quantas miniaturas adiantar além da tela: umas duas telas na TV e no tablet, mais no celular. */
private const val AHEAD = 24
private const val SCROLL_DEBOUNCE_MS = 150L
