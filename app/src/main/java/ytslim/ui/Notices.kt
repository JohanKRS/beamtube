package ytslim.ui

import android.content.res.Resources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ytslim.R
import ytslim.data.Channel
import ytslim.data.SavedVideos
import ytslim.data.Video
import ytslim.data.VideoActions

/**
 * Um aviso curto em cima da tela (a pílula de vidro), como "Saved to Watch later". Com [action], a
 * pílula ganha um botão (o "Undo") e fica [Notices.UNDO_MS] na tela.
 */
data class Notice(val text: String, val id: Long, val action: NoticeAction? = null)

/** O botão da pílula: [label] e o que ele faz. */
class NoticeAction(val label: String, val run: () -> Unit)

object Notices {
    /**
     * Quanto tempo dá para desfazer (pedido do Johan, 2026-11-03): a pílula com "Undo" fica esse
     * tempo na tela, e a conta só muda quando ele passa ([PendingEdit]).
     */
    const val UNDO_MS = 5_000L

    /** Quanto tempo um aviso sem botão fica na tela. */
    const val SHOW_MS = 2_600L

    private val _current = MutableStateFlow<Notice?>(null)
    val current: StateFlow<Notice?> = _current.asStateFlow()

    fun show(text: String, action: NoticeAction? = null) {
        _current.value = Notice(text, System.nanoTime(), action)
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
 * Abre o canal de um vídeo. Quando o card veio sem o ID do canal (as Recomendações vêm assim), ele
 * é procurado antes na página do vídeo: o "Go to channel" aparece sempre (pedido do Johan, 2026-11-13).
 */
fun CoroutineScope.openChannelOf(video: Video, res: Resources, onOpenChannel: (Channel) -> Unit) {
    channelOf(video)?.let { return onOpenChannel(it) }
    launch {
        val found = VideoActions.channel(video.id)
            ?: return@launch Notices.show(res.getString(R.string.channel_not_found))
        // O nome do card, quando há: é o que a pessoa viu.
        onOpenChannel(found.copy(title = video.channelName?.takeIf { it.isNotBlank() } ?: found.title))
    }
}

/**
 * O resto da linha do YouTube depois do nome do canal: "Bench Notes • 12K views • 2 hours ago"
 * vira "12K views · 2 hours ago" (o nome do canal aparece à parte, com o avatar).
 */
fun metaAfterChannel(info: String?, channelName: String?, published: String?): String? {
    val parts = info.orEmpty().split('•', '·').map { it.trim() }.filter { it.isNotEmpty() }
    val rest = parts.filterNot { channelName != null && it.equals(channelName.trim(), ignoreCase = true) }
    return rest.joinToString(" · ").ifEmpty { published?.takeIf { it.isNotBlank() } }
}
