package ytslim.player

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import java.lang.ref.WeakReference

/**
 * Serviço de Acessibilidade da TV, com duas tarefas:
 * - desligar a TV quando os vídeos acabam, se o usuário escolheu "Turn off" no painel de dormir do
 *   player. O Android não deixa um app comum pôr o aparelho para dormir; com o aparelho dormindo,
 *   a TV ligada por HDMI desliga junto (HDMI-CEC);
 * - abrir o BeamTube pelo botão YouTube do controle ([YouTubeButton]): o serviço vê as teclas do
 *   controle antes do sistema e segura só essa. Para aprender qual é, ele fica sabendo quando uma
 *   janela do app do YouTube aparece (só disso, de nenhum outro app).
 *
 * O serviço não lê nada da tela. O nome da classe fica: trocar desligaria o serviço de quem já o
 * ligou. O usuário liga o serviço uma vez, em Configurações > Acessibilidade; os Ajustes da TV
 * explicam e levam até lá (grupo "YouTube button on the remote", 1.0.4).
 */
class ScreenOffService : AccessibilityService() {
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private val button by lazy {
        YouTubeButton(prefs.getStringSet(PREF_YOUTUBE_BUTTON, null).orEmpty().mapNotNull(String::toIntOrNull).toSet())
    }

    override fun onServiceConnected() {
        instance = WeakReference(this)
        // Lê os códigos já aprendidos agora, e não na primeira tecla, que precisa de resposta imediata.
        button
        // No log, para saber pelo ADB quando o Android ligou o serviço (ver AccessibilityRebind).
        Log.i("BeamTube", "remote key: service connected, ${button.learned.size} button code(s) known")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        Log.i("BeamTube", "remote key: service unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /**
     * Só chegam as janelas do app do YouTube (`packageNames` no XML do serviço), sem o conteúdo
     * delas: é o sinal de que o botão YouTube acabou de ser apertado, para aprender o código dele.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != YouTubeButton.YOUTUBE_PACKAGE) return
        val code = button.onYouTubeShown(SystemClock.uptimeMillis()) ?: return
        Log.i("BeamTube", "remote key: learned the YouTube button, code=$code")
        prefs.edit().putStringSet(PREF_YOUTUBE_BUTTON, button.learned.map(Int::toString).toSet()).apply()
        // Desta vez o YouTube já abriu: o BeamTube vem por cima. Das próximas, ele nem abre.
        openApp()
    }

    /**
     * Toda tecla do controle passa por aqui antes do sistema, então a resposta tem de ser imediata:
     * false deixa a tecla seguir. O botão YouTube fica com o serviço (a descida, as repetições de
     * segurar e a subida), e o BeamTube abre na descida.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val firstDown = event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0
        if (button.isButton(keyCode)) {
            if (firstDown) openApp()
            return true
        }
        if (firstDown) {
            button.onKeyPassed(keyCode, SystemClock.uptimeMillis())
            if (!YouTubeButton.isOrdinary(keyCode)) {
                Log.i("BeamTube", "remote key: code=$keyCode scan=${event.scanCode} device=${event.deviceId}")
            }
        }
        return false
    }

    /** Traz o BeamTube para a frente como o launcher faz: com o player aberto, volta para ele. */
    private fun openApp() {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
            ?: packageManager.getLaunchIntentForPackage(packageName)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("BeamTube", "remote key: couldn't open BeamTube", e)
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val PREFS = "remote_keys"
        private const val PREF_YOUTUBE_BUTTON = "youtube_button"

        private var instance: WeakReference<ScreenOffService>? = null

        /** A ação de pôr o aparelho para dormir existe a partir do Android 9. */
        val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        /** O usuário já ligou o serviço nas configurações de Acessibilidade? */
        val enabled: Boolean get() = instance?.get() != null

        /**
         * Como [enabled], mas também pergunta ao Android: logo depois de a pessoa ligar o serviço,
         * o sistema pode ainda não ter se conectado a ele.
         */
        fun isOn(context: Context): Boolean {
            if (enabled) return true
            val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
            return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
                val service = it.resolveInfo?.serviceInfo ?: return@any false
                service.packageName == context.packageName && service.name == ScreenOffService::class.java.name
            }
        }

        /**
         * O serviço está ligado nas configurações de Acessibilidade, conectado ou não. Depois de uma
         * atualização do app, o Android pode deixá-lo ligado lá e desconectado ([AccessibilityRebind]).
         */
        fun isEnabledInSettings(context: Context): Boolean {
            val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            val me = ComponentName(context, ScreenOffService::class.java)
            return list.split(':').any { ComponentName.unflattenFromString(it.trim()) == me }
        }

        /** O código do botão YouTube do controle já foi aprendido neste aparelho? */
        fun knowsYouTubeButton(context: Context): Boolean =
            !context.getSharedPreferences(PREFS, MODE_PRIVATE).getStringSet(PREF_YOUTUBE_BUTTON, null).isNullOrEmpty()

        /**
         * Abre os ajustes de Acessibilidade do Android, onde a pessoa liga o serviço. Se a TV não
         * tiver essa tela, abre os ajustes gerais. false se nenhuma das duas abriu.
         *
         * Cada tela é aberta pelo nome ([screensFor]), e não pelo pedido genérico do Android (1.0.8):
         * numa TV do Johan o pedido genérico caía na janela "nenhum app pode executar esta ação" do
         * sistema, sem erro para o app, e a tela não abria.
         */
        fun openSettings(context: Context): Boolean {
            val actions = listOf(Settings.ACTION_ACCESSIBILITY_SETTINGS, Settings.ACTION_SETTINGS)
            val screens = actions.flatMap { screensFor(context, it) }
            // Sem nenhuma tela à vista (não devia acontecer com o <queries> do manifesto), o pedido genérico.
            val intents = screens.ifEmpty { actions.map(::Intent) }
            for (intent in intents) {
                if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                    return true
                } catch (_: ActivityNotFoundException) {
                } catch (_: SecurityException) {
                }
            }
            return false
        }

        /**
         * As telas do sistema que atendem [action] e que o app pode abrir (exportadas, ligadas e sem
         * permissão que ele não tenha), cada uma pelo nome: primeiro as dos apps do sistema, os
         * ajustes da TV antes dos do celular.
         */
        private fun screensFor(context: Context, action: String): List<Intent> {
            val manager = context.packageManager
            val found = runCatching {
                manager.queryIntentActivities(Intent(action), PackageManager.MATCH_DEFAULT_ONLY)
            }.getOrDefault(emptyList())
            return found.map { it.activityInfo }
                .filter { info ->
                    info.exported && info.enabled && info.applicationInfo.enabled &&
                        (info.permission == null || context.checkSelfPermission(info.permission) == PackageManager.PERMISSION_GRANTED)
                }
                .sortedWith(
                    compareBy<ActivityInfo>(
                        { if (it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) 0 else 1 },
                        { SETTINGS_APPS.indexOf(it.packageName).let { index -> if (index < 0) SETTINGS_APPS.size else index } },
                    ),
                )
                .map { Intent(action).setClassName(it.packageName, it.name) }
        }

        /** Os apps de ajustes conhecidos, na ordem de preferência: o da TV e o do celular. */
        private val SETTINGS_APPS = listOf("com.android.tv.settings", "com.android.settings")

        /** Põe o aparelho para dormir. false se o serviço não está ligado. */
        fun turnOff(): Boolean {
            if (!supported) return false
            val service = instance?.get() ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
