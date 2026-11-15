package ytslim.data

/**
 * Uma legenda oferecida pelo YouTube para o vídeo. [auto] = gerada automaticamente (a fala).
 * [translated] = uma tradução automática que o motor já monta (a URL leva `tlang`); [translatable] =
 * o YouTube aceita traduzir esta legenda.
 */
data class Caption(
    val url: String,
    val mimeType: String?,
    val language: String?,
    val name: String?,
    val auto: Boolean,
    val translatable: Boolean = false,
    val translated: Boolean = false,
)

/**
 * Em que idioma a legenda aparece (Settings > Captions; até a 1.0.9 era um painel no player): no
 * idioma do app, no idioma original do vídeo ou em inglês. O que se guarda é o tipo, não o idioma
 * (o original muda de vídeo para vídeo).
 */
enum class CaptionLanguage { APP, ORIGINAL, ENGLISH }

/**
 * Uma legenda possível para o vídeo: [language] (o código base, "pt"), as escolhas que ela atende (no
 * vídeo em inglês, "original" e "inglês" são a mesma) e a legenda que ela carrega.
 */
data class CaptionOption(val language: String, val choices: Set<CaptionLanguage>, val caption: Caption)

/**
 * As legendas possíveis para um vídeo, nesta ordem: no idioma do app, no original e em inglês, sem
 * repetir (se o original for o do app ou o inglês, ficam só essas). Para cada idioma:
 * 1. uma feita por pessoa nele;
 * 2. a automática, se ele for o original (a fala do vídeo);
 * 3. a tradução automática do YouTube, menos para o original: da feita por pessoa no original, da
 *    automática do original ou de outra que aceite tradução.
 * Um idioma sem nenhuma dessas não aparece.
 */
object SubtitleRules {
    const val ENGLISH = "en"

    fun options(captions: List<Caption>, appLanguage: String?, originalLanguage: String?): List<CaptionOption> {
        val original = base(originalLanguage)
        val real = captions.filterNot { it.translated }
        val wanted = LinkedHashMap<String, MutableSet<CaptionLanguage>>()
        base(appLanguage)?.let { wanted.getOrPut(it) { mutableSetOf() } += CaptionLanguage.APP }
        original?.let { wanted.getOrPut(it) { mutableSetOf() } += CaptionLanguage.ORIGINAL }
        wanted.getOrPut(ENGLISH) { mutableSetOf() } += CaptionLanguage.ENGLISH
        val options = wanted.mapNotNull { (language, choices) ->
            val canTranslate = CaptionLanguage.APP in choices || CaptionLanguage.ENGLISH in choices
            captionIn(language, real, original, canTranslate)?.let { CaptionOption(language, choices, it) }
        }
        if (options.isNotEmpty()) return options
        // Nenhuma dessas (sem tradução, e o original sem legenda): a que houver, para todas as escolhas.
        val any = real.firstOrNull { !it.auto } ?: real.firstOrNull() ?: return emptyList()
        return listOf(CaptionOption(base(any.language).orEmpty(), CaptionLanguage.entries.toSet(), any))
    }

    /**
     * A legenda que vai com o vídeo: a de [choice] (Settings > Captions) ou, se o vídeo não tiver
     * nenhuma nesse idioma (nem a tradução automática), a do original, ou a que houver.
     */
    fun choose(options: List<CaptionOption>, choice: CaptionLanguage): CaptionOption? =
        options.firstOrNull { choice in it.choices }
            ?: options.firstOrNull { CaptionLanguage.ORIGINAL in it.choices }
            ?: options.firstOrNull()

    private fun captionIn(language: String, real: List<Caption>, original: String?, canTranslate: Boolean): Caption? {
        real.firstOrNull { !it.auto && base(it.language) == language }?.let { return it }
        // A automática é a fala do vídeo: só vale no idioma original (as dos idiomas dublados são da dublagem).
        if (original == null || language == original) {
            real.firstOrNull { it.auto && base(it.language) == language }?.let { return it }
        }
        if (!canTranslate) return null
        val sources = real.filter { it.translatable }
        val source = sources.firstOrNull { !it.auto && original != null && base(it.language) == original }
            ?: sources.firstOrNull { it.auto && original != null && base(it.language) == original }
            ?: sources.firstOrNull { !it.auto }
            ?: sources.firstOrNull()
            ?: return null
        return source.copy(url = translatedUrl(source.url, language), language = language, translated = true)
    }

    /** A URL da legenda com `tlang`: o YouTube devolve a tradução automática nesse idioma. */
    internal fun translatedUrl(url: String, language: String): String {
        val clean = url.replace(Regex("[?&]tlang=[^&]*"), "")
        val separator = if ('?' in clean) '&' else '?'
        return "$clean${separator}tlang=$language"
    }

    /** Idioma original: o da faixa de áudio original ou, sem ela, o da legenda automática (a fala). */
    fun originalLanguage(originalAudioLabel: String?, captions: List<Caption>): String? =
        originalAudioLabel?.substringBefore(' ')?.takeIf { it.isNotBlank() }
            ?: captions.firstOrNull { it.auto && !it.translated }?.language

    /** "pt-BR", "pt_BR", "en‐US (original)" -> "pt", "en". */
    internal fun base(language: String?): String? =
        language?.trim()?.lowercase()?.split('-', '_', '‐', ' ')?.firstOrNull()?.takeIf { it.isNotEmpty() }
}
