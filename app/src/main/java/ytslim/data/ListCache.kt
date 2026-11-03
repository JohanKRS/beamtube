package ytslim.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * As listas da biblioteca (playlists e Histórico) guardadas no aparelho (pedido do Johan,
 * 2026-11-03): abrem na hora com o que apareceu da última vez, e a lista da conta chega por trás.
 */
object ListCache {
    /** Mais velha que isso, a lista guardada não aparece. */
    const val MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000

    /** Quantos vídeos de cada lista ficam guardados (o começo dela). */
    const val MAX_VIDEOS = 300

    @Serializable
    data class Saved(val savedAt: Long, val title: String? = null, val videos: List<Video> = emptyList())

    private lateinit var dir: File
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    fun init(context: Context) {
        dir = File(context.filesDir, "lists")
    }

    /** A lista [key] guardada, se houver uma com menos de [MAX_AGE_MS]. */
    suspend fun read(key: String, nowMs: Long = System.currentTimeMillis()): Saved? = mutex.withLock {
        io {
            runCatching { json.decodeFromString(Saved.serializer(), file(key).readText()) }.getOrNull()
        }?.takeIf { nowMs - it.savedAt in 0..MAX_AGE_MS && it.videos.isNotEmpty() }
    }

    suspend fun save(key: String, title: String?, videos: List<Video>, nowMs: Long = System.currentTimeMillis()) = mutex.withLock {
        val saved = Saved(nowMs, title, videos.take(MAX_VIDEOS))
        runCatching {
            io {
                dir.mkdirs()
                file(key).writeText(json.encodeToString(Saved.serializer(), saved))
            }
        }
    }

    /** Ao sair da conta: as listas eram dela. */
    suspend fun clear() = mutex.withLock { io { dir.deleteRecursively() } }

    private fun file(key: String) = File(dir, key.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")
}
