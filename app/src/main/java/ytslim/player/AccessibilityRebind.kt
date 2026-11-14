package ytslim.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ytslim.data.AppScope

/**
 * Religa o serviço de Acessibilidade do app ([ScreenOffService], o do botão YouTube do controle)
 * quando o Android não o religa sozinho.
 *
 * Medido na TCL do Johan (Android 14, 2026-11-14): cada atualização do app (o Obtainium atualiza com
 * a TV em standby) fecha o processo, e o serviço, mesmo ligado nas configurações de Acessibilidade,
 * ficou horas sem voltar. Abrir o app também não bastava: ele só voltou quando outro app da TV foi
 * atualizado, porque é aí que a Acessibilidade revê a lista e liga o que está faltando. Até lá, o
 * botão YouTube abria o YouTube.
 *
 * O app provoca essa mesma revisão: quando o processo sobe (o app atualizado, a TV reiniciada, o
 * app aberto) ou o app volta para a tela, e o serviço está ligado nas configurações mas não
 * conectado, ele troca o estado "ativado" do próprio pacote entre o padrão e o ativado (nos dois o
 * app continua ativo, e nada é fechado). O Android avisa a mudança de pacote, a Acessibilidade revê
 * a lista e conecta o serviço no processo que já está rodando.
 */
object AccessibilityRebind {
    /** Quanto esperar o Android conectar o serviço sozinho, e depois do empurrão. */
    private const val WAIT_MS = 4_000L

    /** Um empurrão por minuto no máximo. */
    private const val MIN_INTERVAL_MS = 60_000L

    private val lock = Mutex()
    /** Quando foi o último empurrão (elapsedRealtime); 0: nenhum. Os testes zeram. */
    internal var lastNudgeAt = 0L

    /**
     * Confere o serviço e, se preciso, pede ao Android para ligá-lo de novo. Devolve se o serviço
     * está conectado no fim. [reason] vai para o log (tag BeamTube).
     */
    suspend fun ensure(context: Context, reason: String): Boolean = lock.withLock {
        val app = context.applicationContext
        if (!ScreenOffService.isEnabledInSettings(app)) return@withLock false
        if (waitConnected(WAIT_MS)) return@withLock true
        val now = SystemClock.elapsedRealtime()
        if (lastNudgeAt != 0L && now - lastNudgeAt < MIN_INTERVAL_MS) return@withLock false
        lastNudgeAt = now
        Log.i("BeamTube", "remote key: service on in settings but not connected ($reason), asking Android to bind it")
        if (!nudge(app)) return@withLock false
        val connected = waitConnected(WAIT_MS)
        Log.i("BeamTube", "remote key: service ${if (connected) "connected" else "still not connected"} after asking")
        connected
    }

    private suspend fun waitConnected(ms: Long): Boolean {
        repeat((ms / STEP_MS).toInt()) {
            if (ScreenOffService.enabled) return true
            delay(STEP_MS)
        }
        return ScreenOffService.enabled
    }

    private const val STEP_MS = 250L

    /** Troca o estado do pacote entre o padrão e o ativado: o Android avisa todos de uma mudança no app. */
    internal fun nudge(context: Context): Boolean = try {
        val pm = context.packageManager
        val next = if (pm.getApplicationEnabledSetting(context.packageName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        pm.setApplicationEnabledSetting(context.packageName, next, PackageManager.DONT_KILL_APP)
        true
    } catch (e: RuntimeException) {
        Log.w("BeamTube", "remote key: couldn't ask Android to bind the service", e)
        false
    }

    /**
     * O app acabou de ser atualizado, ou a TV acabou de ligar do zero: o aviso do Android sobe o
     * processo do app (sem ele, nada subia até alguém abrir o app) e segura o processo até o serviço
     * estar conectado.
     */
    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED && intent.action != Intent.ACTION_BOOT_COMPLETED) return
            val pending = goAsync()
            AppScope.launch {
                try {
                    ensure(context, if (intent.action == Intent.ACTION_BOOT_COMPLETED) "boot" else "app updated")
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
