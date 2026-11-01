package ytslim.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ytslim.R
import ytslim.data.ExternalLinks
import ytslim.data.Video

/** Linha de baixo de um vídeo: de onde ele veio (playlist inscrita) ou a linha do YouTube. */
@Composable
fun subtitleOf(video: Video): String? {
    val playlist = video.fromPlaylist ?: return video.info ?: video.channelName
    return video.channelName?.let { stringResource(R.string.from_playlist, it, playlist) }
        ?: stringResource(R.string.from_playlist_no_channel, playlist)
}

/** As opções de "links de outros apps", na ordem da tela de Configurações. */
val externalLinkOptions = listOf(
    ExternalLinks.NO_HISTORY to R.string.settings_links_no_history,
    ExternalLinks.SAVE_HISTORY to R.string.settings_links_save_history,
    ExternalLinks.ASK to R.string.settings_links_ask,
)
