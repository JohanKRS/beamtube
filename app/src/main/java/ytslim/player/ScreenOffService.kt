package ytslim.player

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/**
 * Serviço de Acessibilidade que só serve para desligar a TV (ou apagar a tela do celular) quando
 * os vídeos acabam, se o usuário escolheu "Turn off" no painel de dormir do player. O Android não
 * deixa um app comum pôr o aparelho para dormir; com o aparelho dormindo, a TV ligada por HDMI
 * desliga junto (HDMI-CEC). O serviço não recebe eventos nem lê nada da tela.
 *
 * O usuário liga o serviço uma vez, em Configurações > Acessibilidade.
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
