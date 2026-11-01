package ytslim.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Quando cada vídeo apareceu numa playlist inscrita, visto por este aparelho.
 *
 * Os vídeos de playlist não trazem data, então "novo" quer dizer "entrou na playlist depois
 * que ela foi inscrita". O que já estava lá na primeira leitura fica como [BASELINE] e não
 * vai para o feed.
 */
@Serializable
data class LedgerEntry(val seen: Map<String, Long> = emptyMap())

object Ledger {
    const val BASELINE = 0L

    fun update(entry: LedgerEntry?, videoIds: List<String>, nowMs: Long): LedgerEntry {
        if (entry == null) return LedgerEntry(videoIds.associateWith { BASELINE })
        val added = videoIds.filter { it !in entry.seen }
        if (added.isEmpty()) return entry
        return LedgerEntry(entry.seen + added.associateWith { nowMs })
    }

    /** Vídeos que entraram depois da inscrição, com o momento em que foram vistos. */
    fun newSince(entry: LedgerEntry): Map<String, Long> = entry.seen.filterValues { it != BASELINE }
}

/** Guarda o [LedgerEntry] de cada playlist num arquivo JSON do app. */
class LedgerStore(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: MutableMap<String, LedgerEntry>? = null

    suspend fun get(playlistId: String): LedgerEntry? = mutex.withLock { load()[playlistId] }

    /** Atualiza a playlist com os vídeos que estão nela agora e devolve o resultado. */
    suspend fun record(playlistId: String, videoIds: List<String>, nowMs: Long): LedgerEntry = mutex.withLock {
        val all = load()
        val updated = Ledger.update(all[playlistId], videoIds, nowMs)
        if (updated != all[playlistId]) {
            all[playlistId] = updated
            io { file.writeText(json.encodeToString(all.toMap())) }
        }
        updated
    }

    private suspend fun load(): MutableMap<String, LedgerEntry> = cache ?: io {
        runCatching { json.decodeFromString<Map<String, LedgerEntry>>(file.readText()) }.getOrDefault(emptyMap())
    }.toMutableMap().also { cache = it }
}
