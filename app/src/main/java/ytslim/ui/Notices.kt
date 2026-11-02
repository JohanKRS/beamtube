package ytslim.ui

import android.content.res.Resources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ytslim.R
import ytslim.data.Channel
import ytslim.data.SavedVideos
import ytslim.data.Video

/** Um aviso curto em cima da tela (a pílula de vidro), como "Saved to Watch later". */
data class Notice(val text: String, val id: Long)

object Notices {
    private val _current = MutableStateFlow<Notice?>(null)
    val current: StateFlow<Notice?> = _current.asStateFlow()

    fun show(text: String) {
        _current.value = Notice(text, System.nanoTime())
    }

    /** Some com o aviso [id], se ele ainda for o da tela. */
    fun dismiss(id: Long) {
        if (_current.value?.id == id) _current.value = null
    }
}

/**
 * Toque longo na miniatura (OK longo na TV): põe ou tira o vídeo da playlist do botão Save. O
 * marcador e o aviso mudam na hora; a conta é atualizada logo depois (ver [SavedVideos.toggle]).
 */
fun toggleSaved(res: Resources, video: Video) {
    val saved = SavedVideos.toggle(video.id) { error ->
        Notices.show(res.getString(R.string.action_failed, error.message ?: error.javaClass.simpleName))
    }
    val name = SavedVideos.target.value.title ?: res.getString(R.string.watch_later)
    Notices.show(res.getString(if (saved) R.string.player_saved_to else R.string.player_removed_from, name))
}

/** O canal de um vídeo, para abrir a página dele. null quando o YouTube não manda o ID. */
fun channelOf(video: Video): Channel? =
    video.channelId?.takeIf { it.isNotBlank() }?.let { Channel(it, video.channelName.orEmpty(), info = null, thumbnailUrl = null) }

/**
 * O resto da linha do YouTube depois do nome do canal: "Bench Notes • 12K views • 2 hours ago"
 * vira "12K views · 2 hours ago" (o nome do canal aparece à parte, com o avatar).
 */
fun metaAfterChannel(info: String?, channelName: String?, published: String?): String? {
    val parts = info.orEmpty().split('•', '·').map { it.trim() }.filter { it.isNotEmpty() }
    val rest = parts.filterNot { channelName != null && it.equals(channelName.trim(), ignoreCase = true) }
    return rest.joinToString(" · ").ifEmpty { published?.takeIf { it.isNotBlank() } }
}
