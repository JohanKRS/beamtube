package ytslim.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

/** Adianta as miniaturas de uma lista de vídeos, um pouco depois de ela aparecer (a tela vem primeiro). */
@Composable
fun PrefetchThumbnails(videos: List<Video>) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(videos.size) {
        delay(400)
        Thumbnails.prefetch(context, videos.map { it.thumbnailUrl })
    }
}
