package ytslim.player

import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import androidx.annotation.OptIn
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat

/**
 * Como a legenda aparece (pedido do Johan): centralizada, texto branco com um leve contorno preto
 * e sem fundo, como no YouTube. O YouTube manda a legenda automática alinhada à esquerda.
 */
@OptIn(UnstableApi::class)
internal object CaptionRules {
    fun style(typeface: Typeface?) = CaptionStyleCompat(
        Color.WHITE,
        Color.TRANSPARENT,
        Color.TRANSPARENT,
        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
        Color.BLACK,
        typeface,
    )

    /**
     * Centraliza cada linha na horizontal. A altura fica como veio (a automática do YouTube chega
     * em blocos de duas linhas, [CaptionBlocks]).
     */
    fun centered(cues: List<Cue>): List<Cue> = cues.map { cue ->
        if (cue.bitmap != null) {
            cue
        } else {
            cue.buildUpon()
                .setTextAlignment(Layout.Alignment.ALIGN_CENTER)
                .setPosition(Cue.DIMEN_UNSET)
                .setPositionAnchor(Cue.TYPE_UNSET)
                .setSize(Cue.DIMEN_UNSET)
                .build()
        }
    }
}
