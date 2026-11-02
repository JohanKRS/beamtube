package ytslim.data

import com.liskovsoft.youtubeapi.service.data.YouTubeMediaItem

/** Playlists da conta e os vídeos de cada uma. */
object LibraryRepository {
    /** Todas as playlists da conta, com o "Assistir mais tarde" (o real, da conta). */
    suspend fun playlists(): List<Playlist> {
        val group = Engine.content.playlistsObserve.firstOrNull()
        return group?.mediaItems.orEmpty().mapNotNull { it.toPlaylist() }
    }

    /** Primeira página de vídeos de uma playlist. */
    suspend fun playlistVideos(playlist: Playlist): VideoPage {
        val key = YouTubeMediaItem().apply {
            setChannelId(playlist.browseId)
            setParams(playlist.params)
            setTitle(playlist.title)
            setPlaylistId(playlist.playlistId)
        }
        val groups = Engine.content.getChannelObserve(key).firstOrNull()
        return groups?.firstOrNull { !it.isEmpty }.toPage()
    }

    /** Histórico da conta (o que foi assistido), do mais recente para o mais antigo. */
    suspend fun history(): VideoPage = Engine.content.historyObserve.firstOrNull().toPage()

    /**
     * Tira [video] do histórico da conta. No Histórico, o menu de cada cartão traz o pedido
     * "Remove from watch history" do YouTube; o motor manda esse pedido como manda o "Not
     * interested" (o mesmo endpoint de feedback).
     */
    suspend fun removeFromHistory(video: Video) {
        val token = checkNotNull(video.feedbackToken) { "YouTube didn't send a way to remove this video" }
        io { Engine.mediaItems.markAsNotInterested(token) }
    }

    /** Recomendações da conta (o "Início" do YouTube), ainda sem os nossos filtros. */
    suspend fun recommended(): VideoPage = Engine.content.recommendedObserve.firstOrNull().toPage()

    /** Próxima página de uma lista (playlist, feed etc.). */
    suspend fun more(page: VideoPage): VideoPage {
        val next = page.next ?: return VideoPage(emptyList(), null)
        return io { Engine.content.continueGroup(next) }.toPage()
    }
}
