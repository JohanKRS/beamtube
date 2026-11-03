package ytslim.ui

import android.content.Context
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import kotlinx.coroutines.delay
import ytslim.data.Video

/**
 * Miniaturas. O Coil já guarda as imagens na memória e no disco; aqui o app adianta o download
 * das miniaturas de cada página que chega, para elas aparecerem na hora ao rolar a lista.
 */
object Thumbnails {
    private const val MAX_REMEMBERED = 3000
    private val requested = HashSet<String>()

    /** Baixa para o cache de disco (sem ocupar a memória) as miniaturas que ainda não foram pedidas. */
    fun prefetch(context: Context, urls: List<String?>) {
        val fresh = synchronized(requested) {
            if (requested.size > MAX_REMEMBERED) requested.clear()
            urls.filterNotNull().filter { it.isNotBlank() && requested.add(it) }
        }
        if (fresh.isEmpty()) return
        val loader = SingletonImageLoader.get(context)
        fresh.forEach { url ->
            loader.enqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    // O disco guarda o arquivo inteiro; decodificar pequeno deixa o adiantamento barato.
                    .size(96, 54)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build(),
            )
        }
    }
}

/**
 * Adianta as miniaturas dos [AHEAD] vídeos logo depois dos que estão na tela, conforme a lista rola (a
 * tela vem primeiro). Antes, cada página que chegava adiantava a lista inteira: no Watch later, com as
 * páginas carregando à frente, eram centenas de miniaturas de uma vez, disputando a rede e o
 * processador com as que estavam na tela.
 */
@Composable
fun PrefetchThumbnails(videos: List<Video>, grid: LazyGridState) {
    val context = LocalContext.current.applicationContext
    val current by rememberUpdatedState(videos)
    LaunchedEffect(grid) {
        delay(400)
        // O índice do último item na tela (cabeçalhos contam, mas a janela é folgada) e a lista atual.
        snapshotFlow { (grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) to current }
            .collect { (last, list) ->
                val from = (last + 1).coerceAtMost(list.size)
                val to = (from + AHEAD).coerceAtMost(list.size)
                Thumbnails.prefetch(context, list.subList(from, to).map { it.thumbnailUrl })
            }
    }
}

/** Quantas miniaturas adiantar além da tela: umas duas telas na TV e no tablet, mais no celular. */
private const val AHEAD = 24
