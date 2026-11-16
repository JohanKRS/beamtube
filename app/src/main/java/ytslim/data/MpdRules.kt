package ytslim.data

/** Ajustes no MPD que o motor monta para os vídeos normais. */
internal object MpdRules {
    private val CAPTION_SET = Regex(
        """<AdaptationSet\b[^>]*\bmimeType="text/[^"]*"[^>]*>.*?</AdaptationSet>\s*""",
        RegexOption.DOT_MATCHES_ALL,
    )

    /**
     * Tira as legendas do MPD (1.0.12). O motor põe nele uma faixa para cada legenda do vídeo e para
     * cada tradução automática do YouTube (passam de cem), cada uma com a URL inteira. O player não usa
     * nenhuma: a legenda vai à parte, só a do idioma escolhido ([SubtitleRules]). Sem elas o MPD fica
     * bem menor, e a única legenda que o player vê é a escolhida.
     */
    fun withoutCaptions(xml: ByteArray): ByteArray {
        val text = xml.toString(Charsets.UTF_8)
        val stripped = CAPTION_SET.replace(text, "")
        return if (stripped.length == text.length) xml else stripped.toByteArray(Charsets.UTF_8)
    }
}
