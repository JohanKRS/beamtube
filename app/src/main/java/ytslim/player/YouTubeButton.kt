package ytslim.player

import android.view.KeyEvent

/**
 * O botão YouTube do controle da TV, para o serviço de Acessibilidade ([ScreenOffService]) abrir o
 * BeamTube no lugar do YouTube.
 *
 * O botão não manda um link: a TV entrega a tecla a um componente fixo do sistema, que abre o app do
 * YouTube direto. A única saída sem root é segurar a tecla antes, no filtro de teclas da
 * Acessibilidade. Cada marca (e cada controle) manda um código diferente, então o app **aprende** o
 * código: quando o YouTube abre logo depois de uma tecla fora das comuns, sem outra tecla no meio,
 * aquela tecla era o botão. Da próxima vez, o serviço segura a tecla e o YouTube nem abre.
 */
class YouTubeButton(learned: Set<Int> = emptySet()) {
    private val codes = learned.toMutableSet()
    private var lastKey: Int? = null
    private var lastKeyAt = 0L

    /** Os códigos já aprendidos (um controle novo pode somar outro). */
    val learned: Set<Int> get() = codes.toSet()

    fun isButton(keyCode: Int): Boolean = keyCode in codes

    /** Uma tecla desceu (a primeira vez, sem as repetições) e seguiu para o sistema. */
    fun onKeyPassed(keyCode: Int, now: Long) {
        if (isOrdinary(keyCode)) {
            lastKey = null
            return
        }
        lastKey = keyCode
        lastKeyAt = now
    }

    /**
     * Uma janela do app do YouTube apareceu. Se a última tecla foi uma fora das comuns, há pouco,
     * ela é o botão: devolve o código (já guardado aqui) para o serviço gravar e abrir o BeamTube.
     */
    fun onYouTubeShown(now: Long): Int? {
        val key = lastKey ?: return null
        lastKey = null
        if (now - lastKeyAt > LEARN_WINDOW_MS) return null
        codes += key
        return key
    }

    companion object {
        /** O app do YouTube para TV (Android TV e Google TV). */
        const val YOUTUBE_PACKAGE = "com.google.android.youtube.tv"

        /** Do botão ao YouTube aparecer. Numa TV fraca, o YouTube frio demora uns segundos. */
        const val LEARN_WINDOW_MS = 5_000L

        /**
         * Teclas que nunca são o botão YouTube: setas, OK, voltar, início, volume, mídia, canais,
         * números, letras, a voz e as teclas do sistema da TV. Só as outras (as de app, como YouTube
         * e Netflix) podem ser aprendidas, e só elas vão para o log.
         */
        fun isOrdinary(keyCode: Int): Boolean = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_MUTE, KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_SLEEP, KeyEvent.KEYCODE_WAKEUP,
            KeyEvent.KEYCODE_TV_POWER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_RECORD, KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK,
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_LAST_CHANNEL,
            KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_ASSIST, KeyEvent.KEYCODE_VOICE_ASSIST,
            KeyEvent.KEYCODE_ALL_APPS, KeyEvent.KEYCODE_SETTINGS, KeyEvent.KEYCODE_TV_INPUT,
            KeyEvent.KEYCODE_GUIDE, KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_CAPTIONS,
            KeyEvent.KEYCODE_BOOKMARK, KeyEvent.KEYCODE_TV, KeyEvent.KEYCODE_DVR,
            KeyEvent.KEYCODE_NOTIFICATION,
            -> true
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> true
            in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> true
            else -> false
        }
    }
}
