package ytslim

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Quando o app inteiro sai da tela (nenhuma tela dele visível: TV desligada, Home, outro app) e
 * quando volta. Passar do início para o player e voltar não conta: as duas são telas do app.
 *
 * [returns] diz, a cada volta, quanto tempo o app ficou fora. As Inscrições usam isso para se
 * atualizar sozinhas depois de um tempo (pedido do Johan, 2026-11-13).
 */
object AppVisibility : Application.ActivityLifecycleCallbacks {
    private val tracker = VisibilityTracker()
    private val _returns = MutableSharedFlow<Long>(extraBufferCapacity = 1)

    /** Quanto tempo (ms) o app ficou fora da tela, emitido quando uma tela dele volta. */
    val returns: SharedFlow<Long> = _returns.asSharedFlow()

    fun init(app: Application) = app.registerActivityLifecycleCallbacks(this)

    override fun onActivityStarted(activity: Activity) {
        tracker.started(SystemClock.elapsedRealtime())?.let { _returns.tryEmit(it) }
    }

    override fun onActivityStopped(activity: Activity) {
        // Girar a tela recria a tela, mas o app não saiu.
        if (!activity.isChangingConfigurations) tracker.stopped(SystemClock.elapsedRealtime())
        else tracker.stoppedForChange()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}

/** A conta de telas visíveis por trás do [AppVisibility] (separada para os testes). */
internal class VisibilityTracker {
    private var visible = 0
    private var awaySinceMs = -1L

    /** Uma tela do app ficou visível. Devolve quanto tempo o app ficou fora, se ele estava fora. */
    fun started(nowMs: Long): Long? {
        val back = visible == 0 && awaySinceMs >= 0
        visible++
        return if (back) (nowMs - awaySinceMs).also { awaySinceMs = -1L } else null
    }

    fun stopped(nowMs: Long) {
        visible = (visible - 1).coerceAtLeast(0)
        if (visible == 0) awaySinceMs = nowMs
    }

    /** A tela parou só para ser recriada (girar): a nova começa logo, e o app não saiu. */
    fun stoppedForChange() {
        visible = (visible - 1).coerceAtLeast(0)
    }
}
