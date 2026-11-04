package ytslim.player

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Serviço de Acessibilidade da TV, com duas tarefas:
 * - desligar a TV quando os vídeos acabam, se o usuário escolheu "Turn off" no painel de dormir do
 *   player. O Android não deixa um app comum pôr o aparelho para dormir; com o aparelho dormindo,
 *   a TV ligada por HDMI desliga junto (HDMI-CEC);
 * - abrir o Doorglow pelo botão YouTube do controle ([RemoteKeys]): o serviço vê as teclas do
 *   controle antes do sistema e segura só essa.
 *
 * O serviço não recebe eventos nem lê nada da tela. O nome da classe fica: trocar desligaria o
 * serviço de quem já o ligou. O usuário liga o serviço uma vez, em Configurações > Acessibilidade.
 */
class ScreenOffService : AccessibilityService() {
    override fun onServiceConnected() {
        instance = WeakReference(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    /**
     * Toda tecla do controle passa por aqui antes do sistema, então a resposta tem de ser imediata:
     * false deixa a tecla seguir. O botão YouTube fica com o serviço (a descida, as repetições de
     * segurar e a subida), e o Doorglow abre na descida.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (!RemoteKeys.isYouTubeButton(keyCode)) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 && !RemoteKeys.isOrdinary(keyCode)) {
                Log.i("Doorglow", "remote key: code=$keyCode scan=${event.scanCode} device=${event.deviceId}")
            }
            return false
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) openDoorglow()
        return true
    }

    /** Traz o Doorglow para a frente como o launcher faz: com o player aberto, volta para ele. */
    private fun openDoorglow() {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: packageManager.getLaunchIntentForPackage(packageName)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("Doorglow", "remote key: couldn't open Doorglow", e)
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        private var instance: WeakReference<ScreenOffService>? = null

        /** A ação de pôr o aparelho para dormir existe a partir do Android 9. */
        val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        /** O usuário já ligou o serviço nas configurações de Acessibilidade? */
        val enabled: Boolean get() = instance?.get() != null

        /** Põe o aparelho para dormir. false se o serviço não está ligado. */
        fun turnOff(): Boolean {
            if (!supported) return false
            val service = instance?.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
