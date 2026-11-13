package ytslim.data

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Deixa o motor pronto para o primeiro vídeo (2026-11-13, pedido do Johan: o vídeo demorava a
 * começar e às vezes a tela ficava só preta). O motor prepara várias coisas só quando o primeiro
 * vídeo é pedido, uma depois da outra, antes mesmo de falar com o YouTube sobre o vídeo:
 *
 * - o "poToken" do YouTube, gerado num WebView com três pedidos de rede (segundos numa TV);
 * - o player do YouTube (uns 2 MB de JavaScript), baixado de novo a cada abertura do app, e o V8
 *   que roda o decifrador dele;
 * - o token da conta, quando venceu.
 *
 * Aqui isso roda por trás assim que as listas aparecem, e de novo a cada volta ao app (o poToken e o
 * token da conta vencem). O que já está pronto não é refeito. Um vídeo pedido no meio disso só
 * espera a peça que estiver em andamento, como já esperaria.
 */
object EngineWarmup {
    private const val TAG = "BeamTube"

    /**
     * Quanto esperar depois que as listas aparecem: o WebView do poToken é criado na thread da tela,
     * e junto com a entrada das listas ele travaria a rolagem por um instante.
     */
    const val AFTER_LISTS_MS = 1_500L

    private var job: Job? = null

    /** O player do YouTube para o qual o decifrador já rodou nesta abertura do app. */
    @Volatile private var solvedFor: String? = null

    /** Cada peça do motor, para os testes trocarem. */
    internal interface Parts {
        fun client(): Any
        fun core(client: Any)
        fun auth()
        fun tokenExpired(client: Any): Boolean
        fun token(client: Any): Boolean
        fun playerUrl(): String?
        fun solver(client: Any): Boolean
    }

    private object EngineParts : Parts {
        override fun client(): Any = EngineWarm.client()
        override fun core(client: Any) = EngineWarm.core(client)
        override fun auth() = EngineWarm.auth()
        override fun tokenExpired(client: Any) = EngineWarm.tokenExpired(client)
        override fun token(client: Any) = EngineWarm.token(client)
        override fun playerUrl(): String? = EngineWarm.playerUrl()
        override fun solver(client: Any) = EngineWarm.solver(client)
    }

    /** Prepara o que faltar, por trás, daqui a [delayMs]. Chamado de novo enquanto ainda roda, não faz nada. */
    fun start(scope: CoroutineScope = AppScope, delayMs: Long = 0L) {
        if (job?.isActive == true) return
        job = scope.launch {
            if (delayMs > 0) delay(delayMs)
            io { Log.i(TAG, "engine warm-up: " + run(EngineParts)) }
        }
    }

    /**
     * Roda as peças na ordem em que o pedido de um vídeo as usa. Devolve o que foi feito e quanto
     * levou, para o log. Uma peça que falha não impede as outras (o vídeo tenta de novo sozinho).
     */
    internal fun run(parts: Parts): String {
        val done = ArrayList<String>()
        fun step(name: String, block: () -> Any?) {
            val from = SystemClock.elapsedRealtime()
            val result = runCatching(block)
            val ms = SystemClock.elapsedRealtime() - from
            done += when {
                result.isFailure -> "$name failed in $ms ms (${result.exceptionOrNull()?.javaClass?.simpleName})"
                result.getOrNull() == false -> "$name not ready in $ms ms"
                else -> "$name $ms ms"
            }
        }
        val client = runCatching { parts.client() }.getOrElse { return "no client (${it.javaClass.simpleName})" }
        step("core") { parts.core(client) }
        step("account") { parts.auth() }
        if (runCatching { parts.tokenExpired(client) }.getOrDefault(true)) step("po token") { parts.token(client) }
        val player = runCatching { parts.playerUrl() }.getOrNull()
        if (player == null || player != solvedFor) {
            step("solver") { parts.solver(client).also { ok -> if (ok) solvedFor = player } }
        }
        return "client $client, " + done.joinToString(", ")
    }

    /** Só para os testes: esquece o que já foi preparado. */
    internal fun reset() {
        solvedFor = null
    }
}
