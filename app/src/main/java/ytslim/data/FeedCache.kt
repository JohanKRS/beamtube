package ytslim.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
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

    /** O arquivo, na pasta de cada usuário ([Users]). */
    const val FILE = "feed.json"

    private val file: File get() = Users.file(FILE)
    private val mutex = Mutex()
    private var cachedPath: String? = null
    private var cached: Saved? = null

    /** O feed guardado, se houver um com menos de [MAX_AGE_MS]. */
    suspend fun read(nowMs: Long = System.currentTimeMillis()): Saved? = mutex.withLock {
        readLocked()?.takeIf { nowMs - it.savedAt in 0..MAX_AGE_MS && it.feed.isNotEmpty() }
    }

    /** Guarda a primeira página que acabou de chegar (os vídeos das playlists ficam os de antes). */
    suspend fun saveFeed(feed: List<Timed>) = update { (it ?: Saved(0)).copy(savedAt = System.currentTimeMillis(), feed = feed) }

    suspend fun saveExtras(extras: List<Timed>) = update { it?.copy(extras = extras) }

    /** Apaga o feed guardado do usuário aberto. */
    suspend fun clear() = mutex.withLock {
        val target = file
        if (cachedPath == target.absolutePath) {
            cachedPath = null
            cached = null
        }
        io { target.delete() }
    }

    /** Esquece o feed na memória, quando o arquivo some por fora (o Sign out apaga a pasta do usuário). */
    suspend fun forget() = mutex.withLock {
        cachedPath = null
        cached = null
    }

    private suspend fun update(change: (Saved?) -> Saved?) = mutex.withLock {
        val target = file
        val updated = change(readLocked(target)) ?: return@withLock
        cachedPath = target.absolutePath
        cached = updated
        runCatching { io { target.writeJson(Saved.serializer(), updated) } }
    }

    private suspend fun readLocked(target: File = file): Saved? {
        val path = target.absolutePath
        if (cachedPath == path) return cached
        val loaded = io {
            if (!target.exists()) null else runCatching { target.readJson(Saved.serializer()) }.getOrNull()
        }
        cachedPath = path
        cached = loaded
        return loaded
    }
}
