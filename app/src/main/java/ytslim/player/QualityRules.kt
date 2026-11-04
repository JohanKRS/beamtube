package ytslim.player

/** Uma faixa de vídeo como a regra de qualidade a vê. */
internal data class VideoTrack(
    val group: Int,
    val index: Int,
    /** O lado menor do quadro (o "1080" de 1080p, também em vídeo vertical). */
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
    /** HDR (10 bits): só vale se o decodificador aceitar de verdade. */
    val hdr: Boolean = false,
    val selected: Boolean,
    /** O tipo do vídeo (ex.: video/x-vnd.on2.vp9); só faixas do mesmo tipo trocam sem parar. */
    val mime: String? = null,
)

/**
 * Qualidade sempre na máxima (pedido do Johan): na TV o app tenta 4K primeiro; no celular e no
 * tablet, 1080p (também pedido dele). Só desce quando o aparelho realmente não dá conta. Os limites que o decodificador declara não bastam para descer:
 * muitas TVs declaram menos do que decodificam, e o Media3, que confia neles, ficava em 360p numa
 * TV de 32 bits com Android 11. Quem faz descer é o aparelho falhar de verdade (erro do
 * decodificador, quadros perdidos ou o vídeo parando para carregar), e aí o teto cai um degrau.
 */
internal object QualityRules {
    /** O teto inicial na TV: 4K. */
    const val MAX_HEIGHT = 2160

    /** O teto inicial no celular e no tablet: 1080p (cai para 720p e abaixo se não der conta). */
    const val TOUCH_HEIGHT = 1080

    fun top(tv: Boolean): Int = if (tv) MAX_HEIGHT else TOUCH_HEIGHT

    private const val MAX_FRAME_RATE = 61f

    /**
     * A faixa para tocar com o teto [ceiling]: a maior com decodificador de hardware. Na mesma
     * altura, vale a que o decodificador declara aceitar e, depois, a de maior bitrate. null se
     * não houver nenhuma (aí vale a escolha do Media3).
     */
    fun best(tracks: List<VideoTrack>, ceiling: Int): VideoTrack? = tracks
        .filter { it.hardware && (it.handled || (it.exceeds && !it.hdr)) }
        .filter { it.height in 1..ceiling && it.frameRate <= MAX_FRAME_RATE }
        .maxWithOrNull(compareBy<VideoTrack>({ it.height }, { it.handled }, { it.bitrate }))

    /** A maior faixa de partida ([start]): o primeiro pedaço do vídeo vem em até 1080p. */
    const val START_HEIGHT = 1080

    /**
     * A faixa para o primeiro pedaço do vídeo (uns 5 s) quando o alvo passa de 1080p (o 4K da TV):
     * menos dados até o primeiro quadro, e o resto já vem no alvo (pedido do Johan em 2026-11-04).
     * A maior até 1080p que troca com o alvo sem parar: mesmo grupo, mesmo codec, mesmo HDR (trocar
     * o HDR mexe no modo da tela) e, se houver, os mesmos quadros por segundo. null = começa no alvo.
     */
    fun start(tracks: List<VideoTrack>, target: VideoTrack): VideoTrack? {
        if (target.height <= START_HEIGHT) return null
        return tracks
            .filter { it.group == target.group && it.index != target.index && it.mime == target.mime && it.hdr == target.hdr }
            .filter { it.hardware && (it.handled || it.exceeds) }
            .filter { it.height in 1..START_HEIGHT && it.frameRate <= MAX_FRAME_RATE }
            .maxWithOrNull(
                compareBy<VideoTrack>(
                    { kotlin.math.abs(it.frameRate - target.frameRate) < 1f },
                    { it.height },
                    { it.handled },
                    { it.bitrate },
                ),
            )
    }

    /** O teto depois de o aparelho não dar conta de [failedHeight]: o degrau de baixo. */
    fun lowerCeiling(failedHeight: Int): Int = failedHeight - 1

    /** As resoluções do botão de qualidade: todas que têm decodificador, da maior para a menor. */
    fun heights(tracks: List<VideoTrack>): List<Int> = tracks
        .filter { (it.handled || it.exceeds) && it.height > 0 }
        .map { it.height }
        .distinct()
        .sortedDescending()

    /**
     * A faixa para a resolução escolhida à mão: o usuário pediu para tentar, então vale mesmo acima
     * do teto. Entre as faixas dessa altura, a com decodificador de hardware, que o decodificador
     * aceita, sem HDR, até 60 fps e de maior bitrate, nessa ordem.
     */
    fun manual(tracks: List<VideoTrack>, height: Int): VideoTrack? = tracks
        .filter { (it.handled || it.exceeds) && it.height == height }
        .maxWithOrNull(
            compareBy<VideoTrack>(
                { it.hardware },
                { it.handled },
                { !it.hdr },
                { it.frameRate <= MAX_FRAME_RATE },
                { it.bitrate },
            ),
        )
}
