package ytslim.player

import android.view.KeyEvent

/**
 * Teclas do controle da TV que o serviço de Acessibilidade ([ScreenOffService]) segura.
 *
 * O botão YouTube não manda um link: a TV entrega a tecla a um componente fixo do sistema, que abre
 * o app do YouTube direto. A única saída sem root é segurar a tecla antes, no filtro de teclas da
 * Acessibilidade, e abrir o Doorglow no lugar. Cada marca manda um código diferente.
 */
object RemoteKeys {
    /** Códigos do botão YouTube, medidos pelo ADB nas TVs do Johan. */
    // TODO(medição): preencher com o código da Hisense (Android 11) e o da TCL (Android 14).
    private val youTubeButton: Set<Int> = emptySet()

    fun isYouTubeButton(keyCode: Int): Boolean = keyCode in youTubeButton

    /**
     * Teclas de todo dia (setas, OK, voltar, volume, mídia, números). As outras vão para o log
     * (tag Doorglow), para achar o código de um botão de app num controle novo.
     */
    fun isOrdinary(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_REWIND,
        KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_STOP,
        KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN,
        -> true
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> true
        else -> false
    }
}
