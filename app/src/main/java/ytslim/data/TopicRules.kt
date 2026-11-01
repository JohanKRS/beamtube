package ytslim.data

import kotlinx.serialization.Serializable
import java.text.Normalizer

/** Um tópico: um nome e as palavras-chave que o identificam no título ou no nome do canal. */
@Serializable
data class Topic(val name: String, val keywords: List<String>)

/** Um canal bloqueado: some das recomendações, da busca e dos vídeos de playlists inscritas. */
@Serializable
data class BlockedChannel(val id: String, val name: String)

/** O que decide o que aparece nas Recomendações (e o que fica de fora em outros lugares). */
data class ContentFilters(
    val excluded: List<Topic> = TopicRules.DEFAULT_EXCLUDED,
    val preferred: List<Topic> = TopicRules.DEFAULT_PREFERRED,
    val blocked: List<BlockedChannel> = emptyList(),
) {
    val blockedIds: Set<String> by lazy { blocked.mapTo(HashSet()) { it.id } }

    /** Os nomes, normalizados: na busca o YouTube nem sempre manda o ID do canal do vídeo. */
    val blockedNames: Set<String> by lazy { blocked.mapNotNullTo(HashSet()) { TopicRules.normalize(it.name).ifEmpty { null } } }
}

/**
 * Classificação por palavras-chave. A categoria oficial do YouTube só viria com uma chamada a mais
 * por vídeo (ou com chave da Data API), então o app olha o título e o nome do canal.
 * A comparação ignora maiúsculas e acentos e só casa palavras inteiras ("ai" não casa com "aim").
 */
object TopicRules {
    val DEFAULT_EXCLUDED = listOf(
        Topic(
            "Sports",
            listOf(
                "sports", "esporte", "esportes", "futebol", "football", "soccer", "nba", "nfl", "mlb", "nhl", "ufc", "fifa",
                "brasileirao", "libertadores", "copa do mundo", "world cup", "champions league", "premier league",
                "la liga", "formula 1", "f1", "olympics", "olimpiadas", "basquete", "basketball", "volei", "volleyball",
                "baseball", "flamengo", "corinthians", "palmeiras", "sao paulo fc", "gremio", "real madrid",
            ),
        ),
        Topic(
            "Politics",
            listOf(
                "politics", "politica", "political", "eleicao", "eleicoes", "election", "elections", "lula", "bolsonaro",
                "trump", "biden", "congresso", "congress", "senado", "senate", "stf", "deputado", "senador",
                "democrats", "republicans", "governo federal", "white house", "planalto",
            ),
        ),
    )

    val DEFAULT_PREFERRED = listOf(
        Topic(
            "Engineering",
            listOf(
                "engineering", "engenharia", "engineer", "engenheiro", "mechanical", "mecanica", "machining", "cnc",
                "3d printing", "impressao 3d", "electronics", "eletronica", "circuit", "circuito", "pcb", "arduino",
                "robotics", "robotica", "robot", "robo",
            ),
        ),
        Topic(
            "Technology",
            listOf("technology", "tecnologia", "tech", "hardware", "semiconductor", "chip", "chips", "linux", "gadget"),
        ),
        Topic(
            "Computing",
            listOf(
                "programming", "programacao", "programar", "coding", "software", "computer", "computers", "computador",
                "computacao", "python", "javascript", "typescript", "kotlin", "rust", "java", "c++", "algorithm",
                "algoritmo", "developer", "desenvolvedor", "database", "compiler", "operating system",
            ),
        ),
        Topic(
            "AI",
            listOf(
                "ai", "a i", "artificial intelligence", "inteligencia artificial", "ia generativa", "machine learning",
                "deep learning", "neural network", "redes neurais", "llm", "llms", "gpt", "chatgpt", "openai",
                "anthropic", "claude", "gemini",
            ),
        ),
    )

    /** Minúsculas, sem acentos, só letras e números separados por um espaço. "+" vira palavra ("c++"). */
    fun normalize(text: String): String {
        val noAccents = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(DIACRITICS, "")
        return noAccents.replace("+", " plus ").replace(NON_WORD, " ").trim().replace(SPACES, " ")
    }

    fun matches(text: String, topic: Topic): Boolean {
        val haystack = " ${normalize(text)} "
        return topic.keywords.any { keyword ->
            val needle = normalize(keyword)
            needle.isNotEmpty() && haystack.contains(" $needle ")
        }
    }

    private fun Video.searchText() = title + " " + channelName.orEmpty()

    /**
     * O vídeo é de um canal bloqueado? Pelo ID ou, quando ele não vem, pelo nome do canal
     * (o começo da linha "Canal • 2M views • 1 year ago").
     */
    fun isBlocked(video: Video, filters: ContentFilters): Boolean {
        if (filters.blocked.isEmpty()) return false
        if (video.channelId != null && video.channelId in filters.blockedIds) return true
        val name = video.channelName?.substringBefore(" • ")?.let(::normalize) ?: return false
        return name.isNotEmpty() && name in filters.blockedNames
    }

    fun isExcluded(video: Video, filters: ContentFilters): Boolean =
        isBlocked(video, filters) || filters.excluded.any { matches(video.searchText(), it) }

    fun isPreferred(video: Video, filters: ContentFilters): Boolean =
        filters.preferred.any { matches(video.searchText(), it) }

    /**
     * As Recomendações: sem Shorts, sem lives, sem canais bloqueados e sem tópicos excluídos.
     * Dentro de cada página, os vídeos de tópicos preferidos sobem para o começo; a ordem do
     * YouTube se mantém entre eles. Uma página nova não mexe na ordem das que já estão na tela.
     */
    fun recommend(pages: List<List<Video>>, filters: ContentFilters): List<Video> {
        val seen = HashSet<String>()
        return pages.flatMap { page ->
            val kept = page.filter { !it.isShorts && !it.isLive && !it.isUpcoming && !isExcluded(it, filters) && seen.add(it.id) }
            val (preferred, others) = kept.partition { isPreferred(it, filters) }
            preferred + others
        }
    }

    /** Palavras-chave digitadas pelo usuário, separadas por vírgula. */
    fun parseKeywords(text: String): List<String> = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val NON_WORD = Regex("[^a-z0-9]+")
    private val SPACES = Regex(" +")
}
