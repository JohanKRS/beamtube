package ytslim.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.io.File
import java.util.concurrent.ConcurrentHashMap

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

    /** A pasta, dentro da de cada usuário ([Users]). */
    const val DIR = "lists"

    private val SAFE_KEY = Regex("[^A-Za-z0-9_-]")
    private val dir: File get() = Users.file(DIR)
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val clearLock = Mutex()

    private fun lockFor(file: File): Mutex = locks.computeIfAbsent(file.absolutePath) { Mutex() }

    /** A lista [key] guardada, se houver uma com menos de [MAX_AGE_MS]. */
    suspend fun read(key: String, nowMs: Long = System.currentTimeMillis()): Saved? {
        val target = file(key)
        return lockFor(target).withLock {
            io {
                if (!target.exists()) return@io null
                val saved = runCatching { target.readJson(Saved.serializer()) }.getOrNull()
                    ?.takeIf { nowMs - it.savedAt in 0..MAX_AGE_MS && it.videos.isNotEmpty() }
                    ?: return@io null
                // Listas guardadas antes da correção dos Recomendados podem estar sem channelName.
                saved.copy(videos = saved.videos.map { v ->
                    val name = Handles.resolveChannelName(v.channelName, v.info)
                    val info = Handles.strip(v.info)
                    if (name == v.channelName && info == v.info) v else v.copy(channelName = name, info = info)
                })
            }
        }
    }

    suspend fun save(key: String, title: String?, videos: List<Video>, nowMs: Long = System.currentTimeMillis()) {
        val targetDir = dir
        val target = file(key, targetDir)
        val saved = Saved(nowMs, title, videos.take(MAX_VIDEOS))
        lockFor(target).withLock {
            runCatching {
                io {
                    targetDir.mkdirs()
                    target.writeJson(Saved.serializer(), saved)
                }
            }
        }
    }

    /** Apaga as listas guardadas do usuário aberto. */
    suspend fun clear() = clearLock.withLock {
        val targetDir = dir
        io { targetDir.deleteRecursively() }
    }

    private fun file(key: String, dir: File = this.dir) = File(dir, key.replace(SAFE_KEY, "_") + ".json")
}
