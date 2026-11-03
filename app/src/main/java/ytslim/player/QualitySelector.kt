package ytslim.player

import android.content.Context
import android.util.Pair
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.trackselection.MappingTrackSelector

/**
 * O seletor de faixas do Media3 com a regra de qualidade do app ([QualityRules]) já na primeira
 * escolha. Antes, o Media3 escolhia a maior faixa que o aparelho declara aceitar, o vídeo começava a
 * baixar por ela, e só depois o app trocava para a da regra (no celular, do 4K para o 1080p): o
 * começo do vídeo era baixado duas vezes.
 *
 * [choose] recebe as faixas de vídeo e devolve a da regra, ou null para ficar com a do Media3. Roda
 * na thread do player. Uma escolha forçada pelo app ou pelo usuário (override) continua valendo
 * por cima desta.
 */
@OptIn(UnstableApi::class)
internal class QualitySelector(
    context: Context,
    private val describe: (format: androidx.media3.common.Format, support: Int) -> VideoTrack,
    private val choose: (List<VideoTrack>) -> VideoTrack?,
) : DefaultTrackSelector(context) {

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
            val target = runCatching { choose(tracks) }.getOrNull() ?: return media3
            return Pair(ExoTrackSelection.Definition(groups[target.group], target.index), renderer)
        }
        return media3
    }
}
