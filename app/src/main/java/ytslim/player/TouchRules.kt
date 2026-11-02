package ytslim.player

/**
 * Toques no vídeo (celular e tablet), como no app do YouTube: dois toques do lado esquerdo voltam
 * 10 s, do lado direito avançam 10 s, e no meio pausam ou tocam. Um toque só mostra ou esconde os
 * controles.
 */
internal object TouchRules {
    enum class Zone { BACK, PLAY_PAUSE, FORWARD }

    const val SEEK_STEP_MS = 10_000L

    /** Depois de um pulo, um toque só do mesmo lado ainda soma mais 10 s por este tempo. */
    const val STREAK_MS = 800L

    /** Os lados ocupam 35% da largura cada; o meio, o resto. */
    fun zone(x: Float, width: Int): Zone = when {
        width <= 0 -> Zone.PLAY_PAUSE
        x < width * 0.35f -> Zone.BACK
        x > width * 0.65f -> Zone.FORWARD
        else -> Zone.PLAY_PAUSE
    }

    /** Para onde o pulo leva, sem sair do vídeo. [durationMs] <= 0 quando ainda não se sabe. */
    fun seekTarget(positionMs: Long, durationMs: Long, zone: Zone): Long {
        val step = if (zone == Zone.BACK) -SEEK_STEP_MS else SEEK_STEP_MS
        val end = if (durationMs > 0) durationMs else Long.MAX_VALUE
        return (positionMs + step).coerceIn(0, end)
    }
}
