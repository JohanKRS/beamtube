package ytslim.data

import kotlinx.serialization.Serializable
import java.text.Normalizer

/**
 * Um tópico: um nome e as palavras-chave que o identificam no título ou no nome do canal.
 * [id] é o de um [SuggestedTopic] quando ele veio das sugestões; null nos criados à mão.
 */
@Serializable
data class Topic(val name: String, val keywords: List<String>, val id: String? = null)

/**
 * Um canal bloqueado: some das recomendações, da busca e dos vídeos de playlists inscritas, neste
 * aparelho. [onAccount]: o YouTube já aceitou o "Don't recommend channel" dele na conta, o que vale
 * em todo aparelho ([ChannelFeedback]).
 */
@Serializable
data class BlockedChannel(val id: String, val name: String, val onAccount: Boolean = false)

/** O que decide o que aparece nas Recomendações (e o que fica de fora em outros lugares). */
data class ContentFilters(
    val excluded: List<Topic> = emptyList(),
    val preferred: List<Topic> = emptyList(),
    val blocked: List<BlockedChannel> = emptyList(),
) {
    val blockedIds: Set<String> by lazy { blocked.mapTo(HashSet()) { it.id } }

    /** Os nomes, normalizados: na busca o YouTube nem sempre manda o ID do canal do vídeo. */
    val blockedNames: Set<String> by lazy { blocked.mapNotNullTo(HashSet()) { TopicRules.normalize(Handles.channelName(it.name).orEmpty()).ifEmpty { null } } }

    /** As palavras-chave já normalizadas (" futebol "), feitas uma vez só e não a cada vídeo. */
    internal val excludedNeedles: List<String> by lazy { TopicRules.needles(excluded) }
    internal val preferredNeedles: List<String> by lazy { TopicRules.needles(preferred) }
}

/**
 * Classificação por palavras-chave. A categoria oficial do YouTube só viria com uma chamada a mais
 * por vídeo (ou com chave da Data API), então o app olha o título e o nome do canal.
 * A comparação ignora maiúsculas e acentos e só casa palavras inteiras ("ai" não casa com "aim").
 */
object TopicRules {
    /** Minúsculas, sem acentos, só letras e números separados por um espaço. "+" vira palavra ("c++"). */
    fun normalize(text: String): String {
        val noAccents = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(DIACRITICS, "")
        return noAccents.replace("+", " plus ").replace(NON_WORD, " ").trim().replace(SPACES, " ")
    }

    fun matches(text: String, topic: Topic): Boolean {
        val haystack = haystack(text)
        return needles(listOf(topic)).any { haystack.contains(it) }
    }

    /** Cada palavra-chave normalizada e entre espaços, para casar só palavras inteiras. */
    internal fun needles(topics: List<Topic>): List<String> =
        topics.flatMap { it.keywords }.mapNotNull { k -> normalize(k).takeIf { it.isNotEmpty() }?.let { " $it " } }.distinct()

    private fun haystack(text: String) = " ${normalize(text)} "

    private fun Video.haystack() = haystack(title + " " + channelName.orEmpty())

    /**
     * O vídeo é de um canal bloqueado? Pelo ID ou, quando ele não vem, pelo nome do canal
     * (o começo da linha "Canal • 2M views • 1 year ago").
     */
    fun isBlocked(video: Video, filters: ContentFilters): Boolean {
        if (filters.blocked.isEmpty()) return false
        if (video.channelId != null && video.channelId in filters.blockedIds) return true
        val name = Handles.channelName(video.channelName)?.let(::normalize) ?: return false
        return name.isNotEmpty() && name in filters.blockedNames
    }

    fun isExcluded(video: Video, filters: ContentFilters): Boolean = isExcluded(video, video.haystack(), filters)

    fun isPreferred(video: Video, filters: ContentFilters): Boolean = isPreferred(video.haystack(), filters)

    private fun isExcluded(video: Video, haystack: String, filters: ContentFilters): Boolean =
        isBlocked(video, filters) || filters.excludedNeedles.any { haystack.contains(it) }

    private fun isPreferred(haystack: String, filters: ContentFilters): Boolean =
        filters.preferredNeedles.any { haystack.contains(it) }

    /**
     * As Recomendações: sem Shorts, sem lives, sem canais bloqueados e sem tópicos excluídos.
     * Dentro de cada página, os vídeos de tópicos preferidos sobem para o começo; a ordem do
     * YouTube se mantém entre eles. Uma página nova não mexe na ordem das que já estão na tela.
     */
    fun recommend(pages: List<List<Video>>, filters: ContentFilters): List<Video> {
        val seen = HashSet<String>()
        return pages.flatMap { page ->
            val preferred = ArrayList<Video>()
            val others = ArrayList<Video>()
            for (video in page) {
                if (video.isShorts || video.isLive || video.isUpcoming) continue
                // O título normalizado uma vez por vídeo serve para os dois testes.
                val haystack = video.haystack()
                if (isExcluded(video, haystack, filters) || !seen.add(video.id)) continue
                if (isPreferred(haystack, filters)) preferred += video else others += video
            }
            preferred + others
        }
    }

    /** Palavras-chave digitadas pelo usuário, separadas por vírgula. */
    fun parseKeywords(text: String): List<String> = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val NON_WORD = Regex("[^a-z0-9]+")
    private val SPACES = Regex(" +")
}
