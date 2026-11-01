package ytslim.data

/**
 * Regra do áudio original: tocar sempre a faixa marcada como original, nunca a dublagem.
 *
 * O motor rotula cada faixa de áudio como "idioma (tipo)", onde o tipo vem do YouTube:
 * original, dubbed, dubbed-auto, descriptive ou secondary. Ex.: "en‐US (original)".
 * Vídeos com uma faixa só costumam vir sem rótulo.
 */
object AudioRules {
    private const val ORIGINAL = "(original)"

    fun isOriginal(label: String?): Boolean = label?.contains(ORIGINAL, ignoreCase = true) == true

    /** O rótulo da faixa original, ou null se nenhuma estiver marcada. */
    fun originalLabel(labels: List<String?>): String? = labels.firstOrNull { isOriginal(it) }

    /**
     * Qual grupo de áudio escolher, dado o rótulo de cada grupo (na ordem do player).
     * Devolve null quando não há o que corrigir (nenhum grupo marcado como original).
     */
    fun pickOriginalGroup(groupLabels: List<String?>): Int? =
        groupLabels.indexOfFirst { isOriginal(it) }.takeIf { it >= 0 }
}
