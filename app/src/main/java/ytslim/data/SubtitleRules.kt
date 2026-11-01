package ytslim.data

/** Uma legenda oferecida pelo YouTube para o vídeo. [auto] = gerada automaticamente. */
data class Caption(
    val url: String,
    val mimeType: String?,
    val language: String?,
    val name: String?,
    val auto: Boolean,
)

/**
 * Qual legenda o botão liga. O botão é só liga/desliga, então a escolha é automática:
 * 1. feita por pessoa, no idioma do aparelho;
 * 2. feita por pessoa, no idioma original do vídeo;
 * 3. qualquer uma feita por pessoa;
 * 4. a automática no idioma original;
 * 5. a primeira que houver.
 */
object SubtitleRules {
    fun pick(captions: List<Caption>, deviceLanguage: String?, originalLanguage: String?): Caption? {
        val device = base(deviceLanguage)
        val original = base(originalLanguage)
        val manual = captions.filter { !it.auto }
        return manual.firstOrNull { device != null && base(it.language) == device }
            ?: manual.firstOrNull { original != null && base(it.language) == original }
            ?: manual.firstOrNull()
            ?: captions.firstOrNull { it.auto && original != null && base(it.language) == original }
            ?: captions.firstOrNull()
    }

    /** Idioma original: o da faixa de áudio original ou, sem ela, o da legenda automática (a fala). */
    fun originalLanguage(originalAudioLabel: String?, captions: List<Caption>): String? =
        originalAudioLabel?.substringBefore(' ')?.takeIf { it.isNotBlank() }
            ?: captions.firstOrNull { it.auto }?.language

    /** "pt-BR", "pt_BR", "en‐US (original)" -> "pt", "en". */
    internal fun base(language: String?): String? =
        language?.trim()?.lowercase()?.split('-', '_', '‐', ' ')?.firstOrNull()?.takeIf { it.isNotEmpty() }
}
