package ytslim.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A primeira página das Inscrições guardada no aparelho (pedido do Johan, 2026-11-02): o app abre
 * com ela na hora e atualiza por trás, em vez de mostrar a tela de carregando a cada abertura.
 */
object FeedCache {
    /** Mais velho que isso, o feed guardado não aparece (os "3 hours ago" já não dizem nada). */
    const val MAX_AGE_MS = 3L * 24 * 60 * 60 * 1000

    @Serializable
    data class Saved(val savedAt: Long, val feed: List<Timed> = emptyList(), val extras: List<Timed> = emptyList())

    private lateinit var file: File
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    fun init(context: Context) {
        file = File(context.filesDir, "feed.json")
    }

    /** O feed guardado, se houver um com menos de [MAX_AGE_MS]. */
    suspend fun read(nowMs: Long = System.currentTimeMillis()): Saved? = mutex.withLock {
        readLocked()?.takeIf { nowMs - it.savedAt in 0..MAX_AGE_MS && it.feed.isNotEmpty() }
    }

    /** Guarda a primeira página que acabou de chegar (os vídeos das playlists ficam os de antes). */
    suspend fun saveFeed(feed: List<Timed>) = update { (it ?: Saved(0)).copy(savedAt = System.currentTimeMillis(), feed = feed) }

    suspend fun saveExtras(extras: List<Timed>) = update { it?.copy(extras = extras) }

    /** Ao sair da conta: o feed era dela. */
    suspend fun clear() = mutex.withLock { io { file.delete() } }

    private suspend fun update(change: (Saved?) -> Saved?) = mutex.withLock {
        val updated = change(readLocked()) ?: return@withLock
        runCatching { io { file.writeText(json.encodeToString(Saved.serializer(), updated)) } }
    }

    private suspend fun readLocked(): Saved? = io {
        runCatching { json.decodeFromString(Saved.serializer(), file.readText()) }.getOrNull()
    }
}
