package ytslim.player

/** Uma faixa de vídeo como a regra de qualidade a vê. */
internal data class VideoTrack(
    val group: Int,
    val index: Int,
    val height: Int,
    /** Quadros por segundo; 0 ou menos quando o manifest não diz. */
    val frameRate: Float,
    val bitrate: Int,
    /** O decodificador declara que dá conta. */
    val handled: Boolean,
    /** Existe decodificador para o codec, mas ele declara que não dá conta (tamanho, taxa ou perfil). */
    val exceeds: Boolean,
    /** O codec tem decodificador de hardware no aparelho. */
    val hardware: Boolean,
    val selected: Boolean,
)

/**
 * Qualidade sempre na máxima, mesmo em aparelhos que declaram limites menores do que realmente
 * decodificam. Com esses limites, o Media3 descarta as faixas boas e fica com uma pequena; numa TV
 * de 32 bits com Android 11, os vídeos tocavam em 360p.
 */
internal object QualityRules {
    /** Até onde o app passa por cima dos limites declarados: quase toda TV decodifica 1080p no hardware. */
    const val MAX_FORCED_HEIGHT = 1080

    private const val MAX_FORCED_FRAME_RATE = 61f

    /**
     * A faixa para tocar no lugar da que o Media3 escolheu, ou null para manter a dele: a maior
     * faixa com decodificador de hardware até [maxHeight], se for maior que a escolhida. Na mesma
     * altura, vale a que o decodificador declara aceitar e, depois, a de maior bitrate.
     */
    fun upgrade(tracks: List<VideoTrack>, maxHeight: Int): VideoTrack? {
        val current = tracks.firstOrNull { it.selected } ?: return null
        val best = tracks
            .filter { (it.handled || it.exceeds) && it.hardware }
            .filter { it.height in 1..maxHeight && it.frameRate <= MAX_FORCED_FRAME_RATE }
            .maxWithOrNull(compareBy<VideoTrack>({ it.height }, { it.handled }, { it.bitrate }))
            ?: return null
        return best.takeIf { it.height > current.height }
    }
}
