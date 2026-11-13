package ytslim.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Pedidos de vídeo feitos ao motor antes de a tela do player precisar deles:
 *
 * - **no toque** ([asked]): abrir a tela do player leva de 0,2 a 0,5 s, e o pedido corre nesse tempo;
 * - **na TV, com o foco parado num card** ([focused], 2026-11-13): quem para num card costuma apertar
 *   OK depois de ler o título, e o pedido ao YouTube leva um ou dois segundos. Depois de
 *   [dwellMs] com o foco no mesmo card, o vídeo é pedido; no OK, ele já está resolvido (ou quase).
 *
 * Os últimos [kept] ficam guardados por [maxAgeMs]: voltar a um card de há pouco não pede de novo.
 * Cada um vale uma vez só ([take]): depois de um erro do player, o vídeo é pedido de novo.
 *
 * Um palpite de cada vez: o motor atende um pedido por vez, e uma fila de palpites atrasaria o
 * vídeo que o usuário escolher de verdade. Um palpite que não deu certo (rede, cliente do YouTube)
 * não vale como resposta: o pedido de verdade tenta de novo. E depois de um palpite que falhou, os
 * palpites param por [pauseMs]: se o YouTube começou a recusar pedidos (ou a rede caiu), insistir só
 * pioraria, e o vídeo volta a ser pedido só no OK (pergunta do Johan, 2026-11-13).
 */
internal class EarlyRequests<T : Any>(
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val maxAgeMs: Long,
    private val kept: Int,
    private val dwellMs: Long,
    /** Quanto os palpites ficam parados depois de um que falhou. */
    private val pauseMs: Long,
    /** Um palpite com este resultado pode ser usado? (false = pedir de novo) */
    private val usable: (T) -> Boolean,
    /** O pedido ao motor; guess = true quando é um palpite (o foco parado), e não o usuário pedindo. */
    private val request: suspend (id: String, guess: Boolean) -> T,
    /** Um palpite falhou e os palpites pararam por [pauseMs] (para o log). */
    private val onPaused: (id: String) -> Unit = {},
) {
    private class Early<T>(val atMs: Long, val result: Deferred<T>, val guess: Boolean)

    /** Do mais antigo para o mais novo. */
    private val early = LinkedHashMap<String, Early<T>>()
    private var dwell: Job? = null
    private var dwellId: String? = null

    /** Até quando os palpites ficam parados ([pauseMs]); 0 = não estão. */
    private var pausedUntilMs = 0L

    /** O último vídeo que o usuário pediu: de volta do player, o foco cai no card dele, e isso não é um palpite. */
    private var lastAsked: String? = null

    /** O usuário pediu o vídeo (toque ou OK): começa a pedir já, ou aproveita o que já foi pedido. */
    fun asked(id: String) {
        synchronized(this) {
            cancelDwell()
            lastAsked = id
            if (valid(id) == null) put(id, guess = false)
        }
    }

    /** TV: o card de [id] pegou o foco. Se o foco ficar nele, o vídeo é pedido antes do OK. */
    fun focused(id: String) {
        synchronized(this) {
            cancelDwell()
            if (id == lastAsked || paused()) return
            dwellId = id
            dwell = scope.launch {
                delay(dwellMs)
                // Um palpite de cada vez: espera o que estiver em andamento (o foco saindo cancela a espera).
                val running = synchronized(this@EarlyRequests) { early.entries.firstOrNull { it.value.result.isActive } }
                if (running != null && running.key != id) running.value.result.join()
                synchronized(this@EarlyRequests) {
                    if (dwellId == id && !paused() && valid(id) == null) put(id, guess = true)
                }
            }
        }
    }

    /** O foco saiu do card de [id] antes do tempo: nada é pedido. */
    fun unfocused(id: String) {
        synchronized(this) { if (dwellId == id) cancelDwell() }
    }

    /**
     * O resultado adiantado de [id], se houver, esperando o pedido terminar. Sai da lista: vale uma
     * vez. null = não há (ou era um palpite que não deu certo): quem chama pede ao motor.
     */
    suspend fun take(id: String): T? {
        val taken = synchronized(this) { early.remove(id) } ?: return null
        if (stale(taken)) {
            taken.result.cancel()
            return null
        }
        return try {
            // O que o usuário pediu vale como veio (um vídeo que não toca não é pedido duas vezes).
            taken.result.await().takeIf { !taken.guess || usable(it) }
        } catch (e: CancellationException) {
            // Se quem foi cancelado é o pedido adiantado (trocado por outro), e não quem espera, pede de novo.
            if (!currentCoroutineContext().isActive) throw e
            null
        } catch (e: Exception) {
            if (taken.guess) null else throw e
        }
    }

    private fun stale(have: Early<T>) = now() - have.atMs >= maxAgeMs

    /**
     * O pedido de [id] que dispensa pedir de novo: recente e, se já terminou, com resultado
     * aproveitável. O que não serve mais sai da lista.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun valid(id: String): Early<T>? {
        val have = early[id] ?: return null
        val failed = have.result.isCompleted && (have.result.isCancelled || runCatching { !usable(have.result.getCompleted()) }.getOrDefault(true))
        if (!stale(have) && !failed) return have
        early.remove(id)
        have.result.cancel()
        return null
    }

    private fun paused() = now() < pausedUntilMs

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun put(id: String, guess: Boolean) {
        val result = scope.async { request(id, guess) }
        if (guess) {
            result.invokeOnCompletion { error ->
                // Cancelado (o usuário escolheu outro vídeo, ou saiu da lista de guardados) não é falha.
                val failed = when (error) {
                    null -> !usable(result.getCompleted())
                    is CancellationException -> false
                    else -> true
                }
                if (failed) {
                    synchronized(this) { pausedUntilMs = now() + pauseMs }
                    onPaused(id)
                }
            }
        }
        early[id] = Early(now(), result, guess)
        while (early.size > kept) {
            val oldest = early.entries.first()
            early.remove(oldest.key)
            oldest.value.result.cancel()
        }
    }

    private fun cancelDwell() {
        dwell?.cancel()
        dwell = null
        dwellId = null
    }
}
