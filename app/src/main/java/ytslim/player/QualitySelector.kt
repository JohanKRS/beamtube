package ytslim.player

import android.content.Context
import android.util.Pair
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Timeline
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.chunk.MediaChunk
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.BaseTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelection
import androidx.media3.exoplayer.upstream.BandwidthMeter

/** A escolha da regra para um vídeo: a faixa [target] e, quando vale, a [start] para o primeiro pedaço. */
internal data class QualityPick(val target: VideoTrack, val start: VideoTrack? = null)

/**
 * O seletor de faixas do Media3 com a regra de qualidade do app ([QualityRules]) já na primeira
 * escolha. Antes, o Media3 escolhia a maior faixa que o aparelho declara aceitar, o vídeo começava a
 * baixar por ela, e só depois o app trocava para a da regra (no celular, do 4K para o 1080p): o
 * começo do vídeo era baixado duas vezes.
 *
 * [choose] recebe as faixas de vídeo e devolve a escolha da regra, ou null para ficar com a do
 * Media3. Roda na thread do player. Uma escolha forçada pelo app ou pelo usuário (override)
 * continua valendo por cima desta.
 *
 * Com uma faixa de partida ([QualityPick.start], na TV em 4K), o primeiro pedaço do vídeo vem
 * nela e o resto no alvo ([StartLowSelection]).
 */
@OptIn(UnstableApi::class)
internal class QualitySelector(
    context: Context,
    private val describe: (format: Format, support: Int) -> VideoTrack,
    private val choose: (List<VideoTrack>) -> QualityPick?,
) : DefaultTrackSelector(context, StartLowSelection.Factory()) {

    override fun selectVideoTrack(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        rendererFormatSupports: Array<Array<IntArray>>,
        mixedMimeTypeSupports: IntArray,
        params: Parameters,
        selectedAudioLanguage: String?,
    ): Pair<ExoTrackSelection.Definition, Int>? {
        val media3 = super.selectVideoTrack(mappedTrackInfo, rendererFormatSupports, mixedMimeTypeSupports, params, selectedAudioLanguage)
        for (renderer in 0 until mappedTrackInfo.rendererCount) {
            if (mappedTrackInfo.getRendererType(renderer) != C.TRACK_TYPE_VIDEO) continue
            val groups = mappedTrackInfo.getTrackGroups(renderer)
            if (groups.length == 0) continue
            val tracks = ArrayList<VideoTrack>()
            for (g in 0 until groups.length) {
                val group = groups[g]
                for (i in 0 until group.length) {
                    val support = RendererCapabilities.getFormatSupport(rendererFormatSupports[renderer][g][i])
                    tracks += describe(group.getFormat(i), support).copy(group = g, index = i, selected = false)
                }
            }
            val pick = runCatching { choose(tracks) }.getOrNull() ?: return media3
            val group = groups[pick.target.group]
            val start = pick.start?.takeIf { it.group == pick.target.group && it.index != pick.target.index }
            val definition = if (start == null) {
                ExoTrackSelection.Definition(group, pick.target.index)
            } else {
                ExoTrackSelection.Definition(group, intArrayOf(pick.target.index, start.index), StartLowSelection.TYPE)
            }
            return Pair(definition, renderer)
        }
        return media3
    }
}

/**
 * Duas faixas do mesmo codec: a menor ([start]) só no primeiro pedaço de vídeo (uns 5 s), a maior
 * ([target]) em todos os outros. Pede bem menos dados até o primeiro quadro (no 4K, uns 5 vezes
 * menos), e a troca é sem parar: os pedaços seguintes já vêm no alvo, e o decodificador já é
 * configurado para o tamanho maior. Depois de pular para um trecho que não estava carregado, vale
 * de novo: o primeiro pedaço dali vem na menor.
 *
 * Nunca desce sozinha por causa da rede (isso é com o [PlaybackController]).
 */
@OptIn(UnstableApi::class)
internal class StartLowSelection(group: TrackGroup, tracks: IntArray, type: Int) : BaseTrackSelection(group, tracks, type) {
    // Na seleção as faixas ficam em ordem de bitrate; o alvo é a de mais pixels, a partida a de menos.
    private val target = (0 until length).maxBy { pixels(getFormat(it)) }
    private val start = (0 until length).minBy { pixels(getFormat(it)) }
    private var selected = start

    override fun getSelectedIndex(): Int = selected

    override fun getSelectionReason(): Int =
        if (selected == start) C.SELECTION_REASON_INITIAL else C.SELECTION_REASON_ADAPTIVE

    override fun getSelectionData(): Any? = null

    override fun updateSelectedTrack(
        playbackPositionUs: Long,
        bufferedDurationUs: Long,
        availableDurationUs: Long,
        queue: List<MediaChunk>,
        mediaChunkIterators: Array<MediaChunkIterator>,
    ) {
        // Fila vazia: começo do vídeo, ou um pulo para fora do que já estava carregado.
        selected = if (queue.isEmpty()) start else target
    }

    private fun pixels(format: Format): Long = format.width.toLong() * format.height

    /** Cria esta seleção para as definições do [QualitySelector]; as outras ficam com o Media3. */
    class Factory : ExoTrackSelection.Factory {
        private val media3 = AdaptiveTrackSelection.Factory()

        override fun createTrackSelections(
            definitions: Array<out ExoTrackSelection.Definition?>,
            bandwidthMeter: BandwidthMeter,
            mediaPeriodId: MediaSource.MediaPeriodId,
            timeline: Timeline,
        ): Array<ExoTrackSelection?> {
            val ours = definitions.map { it != null && it.type == TYPE && it.tracks.size > 1 }
            val rest = Array(definitions.size) { i -> if (ours[i]) null else definitions[i] }
            val selections = media3.createTrackSelections(rest, bandwidthMeter, mediaPeriodId, timeline)
            definitions.forEachIndexed { i, definition ->
                if (ours[i] && definition != null) selections[i] = StartLowSelection(definition.group, definition.tracks, definition.type)
            }
            return selections
        }
    }

    companion object {
        const val TYPE = TrackSelection.TYPE_CUSTOM_BASE + 1
    }
}
