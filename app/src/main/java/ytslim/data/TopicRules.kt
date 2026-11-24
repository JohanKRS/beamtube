package ytslim.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.text.Normalizer

/**
 * Um tópico: um nome e as palavras-chave que o identificam no título ou no nome do canal.
 * [id] é o de um [SuggestedTopic] quando ele veio das sugestões; null nos criados à mão.
 */
@Immutable
@Serializable
data class Topic(val name: String, val keywords: List<String>, val id: String? = null)

/**
 * Um canal bloqueado: some das recomendações, da busca e dos vídeos de playlists inscritas, neste
 * aparelho. [onAccount]: o YouTube já aceitou o "Don't recommend channel" dele na conta, o que vale
 * em todo aparelho ([ChannelFeedback]).
 */
@Immutable
@Serializable
data class BlockedChannel(val id: String, val name: String, val onAccount: Boolean = false)

/** Índice de palavras-chave normalizadas: consulta em O(palavras no título) em vez de O(palavras-chave). */
internal class NeedleIndex(topics: List<Topic>) {
    private val single = HashSet<String>()
    private val multi = HashMap<String, MutableList<String>>()
    val isEmpty: Boolean

    init {
        for (topic in topics) {
            for (keyword in topic.keywords) {
                val norm = TopicRules.normalize(keyword)
                if (norm.isEmpty()) continue
                val sp = norm.indexOf(' ')
                if (sp < 0) {
                    single.add(norm)
                } else {
                    val first = norm.substring(0, sp)
                    val list = multi.getOrPut(first) { ArrayList(2) }
                    if (norm !in list) list.add(norm)
                }
            }
        }
        isEmpty = single.isEmpty() && multi.isEmpty()
    }

    /** Alguma palavra-chave casa com palavras inteiras dentro de [normalizedText]? */
    fun matches(normalizedText: String): Boolean {
        if (isEmpty || normalizedText.isEmpty()) return false
        val len = normalizedText.length
        var start = 0
        while (start < len) {
            val sp = normalizedText.indexOf(' ', start)
            val end = if (sp < 0) len else sp
            val word = normalizedText.substring(start, end)
            if (word in single) return true
            val phrases = multi[word]
            if (phrases != null) {
                for (i in 0 until phrases.size) {
                    val p = phrases[i]
                    val pEnd = start + p.length
                    if (pEnd <= len && (pEnd == len || normalizedText[pEnd] == ' ') && normalizedText.startsWith(p, start)) {
                        return true
                    }
                }
            }
            if (sp < 0) break
            start = sp + 1
        }
        return false
    }
}

/** O que decide o que aparece nas Recomendações (e o que fica de fora em outros lugares). */
@Immutable
data class ContentFilters(
    val excluded: List<Topic> = emptyList(),
    val preferred: List<Topic> = emptyList(),
    val blocked: List<BlockedChannel> = emptyList(),
) {
    val blockedIds: Set<String> by lazy { blocked.mapTo(HashSet()) { it.id } }

    /** Os nomes, normalizados: na busca o YouTube nem sempre manda o ID do canal do vídeo. */
    val blockedNames: Set<String> by lazy { blocked.mapNotNullTo(HashSet()) { TopicRules.normalize(Handles.channelName(it.name).orEmpty()).ifEmpty { null } } }

    /** Índices de palavras-chave já normalizadas, feitos uma vez só e não a cada vídeo. */
    internal val excludedIndex: NeedleIndex by lazy { NeedleIndex(excluded) }
    internal val preferredIndex: NeedleIndex by lazy { NeedleIndex(preferred) }
}

/**
 * Classificação por palavras-chave. A categoria oficial do YouTube só viria com uma chamada a mais
 * por vídeo (ou com chave da Data API), então o app olha o título e o nome do canal.
 * A comparação ignora maiúsculas e acentos e só casa palavras inteiras ("ai" não casa com "aim").
 */
object TopicRules {
    /** Minúsculas, sem acentos, só letras e números separados por um espaço. "+" vira palavra ("c++"). */
    fun normalize(text: String): String {
        val len = text.length
        if (len == 0) return ""
        var ascii = true
        for (i in 0 until len) {
            if (text[i].code >= 0x80) {
                ascii = false
                break
            }
        }
        val source = if (ascii) text else Normalizer.normalize(text, Normalizer.Form.NFD)
        val srcLen = source.length
        val out = StringBuilder(srcLen)
        var space = false
        for (i in 0 until srcLen) {
            val ch = source[i]
            if (!ascii && Character.getType(ch) == Character.NON_SPACING_MARK.toInt()) continue
            if (ch == '+') {
                if (out.isNotEmpty()) out.append(' ')
                out.append("plus")
                space = true
            } else {
                val c = if (ch in 'A'..'Z') (ch.code + 32).toChar() else if (!ascii && ch.code >= 0x80) ch.lowercaseChar() else ch
                if (c in 'a'..'z' || c in '0'..'9') {
                    if (space) {
                        out.append(' ')
                        space = false
                    }
                    out.append(c)
                } else if (out.isNotEmpty()) {
                    space = true
                }
            }
        }
        return out.toString()
    }

    fun matches(text: String, topic: Topic): Boolean = NeedleIndex(listOf(topic)).matches(normalize(text))

    private fun Video.normalizedText(): String =
        if (channelName.isNullOrEmpty()) normalize(title) else normalize("$title $channelName")

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

    fun isExcluded(video: Video, filters: ContentFilters): Boolean =
        isBlocked(video, filters) || (!filters.excludedIndex.isEmpty && filters.excludedIndex.matches(video.normalizedText()))

    fun isPreferred(video: Video, filters: ContentFilters): Boolean =
        !filters.preferredIndex.isEmpty && filters.preferredIndex.matches(video.normalizedText())

    /**
     * As Recomendações: sem Shorts, sem lives, sem canais bloqueados e sem tópicos excluídos.
     * Dentro de cada página, os vídeos de tópicos preferidos sobem para o começo; a ordem do
     * YouTube se mantém entre eles. Uma página nova não mexe na ordem das que já estão na tela.
     */
    fun recommend(pages: List<List<Video>>, filters: ContentFilters): List<Video> {
        val seen = HashSet<String>()
        val excluded = filters.excludedIndex
        val preferredIdx = filters.preferredIndex
        val needText = !excluded.isEmpty || !preferredIdx.isEmpty
        return pages.flatMap { page ->
            val preferred = ArrayList<Video>()
            val others = ArrayList<Video>(page.size)
            for (video in page) {
                if (video.isShorts || video.isLive || video.isUpcoming) continue
                if (isBlocked(video, filters)) continue
                val norm = if (needText) video.normalizedText() else ""
                if ((!excluded.isEmpty && excluded.matches(norm)) || !seen.add(video.id)) continue
                if (!preferredIdx.isEmpty && preferredIdx.matches(norm)) preferred += video else others += video
            }
            if (preferred.isEmpty()) others else preferred + others
        }
    }

    /** Palavras-chave digitadas pelo usuário, separadas por vírgula. */
    fun parseKeywords(text: String): List<String> = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
