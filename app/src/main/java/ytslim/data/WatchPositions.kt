package ytslim.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Onde cada vídeo parou neste aparelho, para voltar a ele de onde parou (achado do Johan na TV,
 * 2026-11-04: recomeçava do início). Guarda os [MAX_KEPT] mais recentes num arquivo pequeno. O
 * histórico do YouTube recebe o mesmo ponto ([PlaybackRepository.saveProgress]); o tempo que o card
 * do YouTube traz ([Video.resumeMs]) vale para os vídeos vistos em outro aparelho.
 */
object WatchPositions {
    private const val MAX_KEPT = 300

    @Serializable
    data class Spot(val videoId: String, val positionMs: Long, val durationMs: Long)

    /** O arquivo, na pasta de cada usuário ([Users]). */
    const val FILE = "positions.json"

    private val file: File get() = Users.file(FILE)
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    /** Do mais antigo para o mais novo; null até a primeira leitura do arquivo do usuário aberto. */
    private var spots: MutableList<Spot>? = null

    /** Onde [videoId] começa: o ponto guardado aqui ou, sem ele, o do card ([hintMs]), pelas [ResumeRules]. */
    suspend fun startAt(videoId: String, hintMs: Long): Long = mutex.withLock {
        val spot = loaded().lastOrNull { it.videoId == videoId }
        if (spot != null) ResumeRules.startAt(spot.positionMs, spot.durationMs) else ResumeRules.startAt(hintMs, durationMs = 0)
    }

    /**
     * Guarda onde [videoId] parou. O convidado não guarda nada: ele existe para ver alguma coisa fora
     * do histórico (pedido do Johan, 2026-11-14).
     */
    suspend fun save(videoId: String, positionMs: Long, durationMs: Long) {
        if (Users.isGuest) return
        mutex.withLock { saveLocked(videoId, positionMs, durationMs) }
    }

    private suspend fun saveLocked(videoId: String, positionMs: Long, durationMs: Long) {
        val list = loaded()
        list.removeAll { it.videoId == videoId }
        list += Spot(videoId, positionMs, durationMs.coerceAtLeast(0))
        while (list.size > MAX_KEPT) list.removeAt(0)
        runCatching { io { file.writeText(json.encodeToString(ListSerializer(Spot.serializer()), list)) } }
    }

    /** Apaga os pontos do usuário aberto. */
    suspend fun clear() = mutex.withLock {
        spots = mutableListOf()
        io { file.delete() }
    }

    /** Outro usuário abriu: os pontos dele vêm do arquivo dele. */
    fun forget() {
        spots = null
    }

    private suspend fun loaded(): MutableList<Spot> = spots ?: io {
        runCatching { json.decodeFromString(ListSerializer(Spot.serializer()), file.readText()) }.getOrDefault(emptyList())
    }.toMutableList().also { spots = it }
}

/** De onde um vídeo começa, pelo ponto em que parou. */
object ResumeRules {
    /** Menos que isso visto, o vídeo começa do início. */
    const val MIN_MS = 10_000L

    /** Faltando menos que isso (os créditos, a tela final), o vídeo foi visto: começa do início de novo. */
    const val END_MS = 30_000L

    /** @param durationMs 0 quando não se sabe. */
    fun startAt(positionMs: Long, durationMs: Long): Long = when {
        positionMs < MIN_MS -> 0
        durationMs > 0 && durationMs - positionMs < END_MS -> 0
        else -> positionMs
    }
}
