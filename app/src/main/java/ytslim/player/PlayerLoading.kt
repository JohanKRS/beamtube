package ytslim.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.transformations
import ytslim.R

/** Quando o círculo de carregando aparece ([PlayerLoading]). */
internal object LoadingRules {
    /**
     * No meio do vídeo (pular na barra, a rede atrasar), o círculo espera este tempo: um pulo para um
     * trecho já carregado passa pelo "carregando" por um instante, e o círculo piscaria a cada toque.
     */
    const val MID_VIDEO_DELAY_MS = 400L

    /**
     * O círculo roda enquanto o vídeo é pedido ao YouTube ([requesting]: o player ainda nem tem o que
     * carregar) e enquanto o player carrega. Com erro na tela, não.
     */
    fun busy(requesting: Boolean, failed: Boolean, playbackState: Int): Boolean =
        !failed && (requesting || playbackState == Player.STATE_BUFFERING)
}

/**
 * O que fica por cima do vídeo enquanto ele não começa (pedido do Johan, 2026-11-13: "às vezes a
 * tela fica só preta, sem nenhuma indicação"): a miniatura do vídeo e um círculo girando na cor de
 * destaque, desde o instante em que o player abre. Antes, o círculo só aparecia quando o player
 * começava a baixar o vídeo; a espera pela resposta do YouTube ficava sem sinal.
 *
 * Desde a 1.0.7 (pedido do Johan, 2026-11-14), a miniatura ocupa a tela toda, sem as faixas pretas
 * que algumas trazem, e bem desfocada: ficam só as cores dela ([PosterBlur]).
 *
 * A miniatura some no primeiro quadro, num esmaecer curto; o círculo volta quando o vídeo para para
 * carregar. Fica por baixo do escurecer do modo dormir e dos controles.
 */
internal class PlayerLoading(context: Context, tv: Boolean) : FrameLayout(context) {
    private val poster = ImageView(context).apply {
        // A tela toda, de borda a borda: a miniatura já vem sem as faixas pretas e desfocada.
        scaleType = ImageView.ScaleType.CENTER_CROP
        // Até a miniatura chegar (do cache, em geral num instante), preto.
        setBackgroundColor(Color.BLACK)
        // Um pouco escurecida, para o círculo e os controles aparecerem bem por cima, e com as cores
        // reforçadas: o desfoque mistura tudo e deixa a imagem acinzentada.
        colorFilter = posterTone()
        visibility = View.GONE
    }
    private val ring = LoadingRing(context).apply { visibility = View.GONE }
    private var request: Disposable? = null
    private var shownUrl: String? = null
    private val showRing = Runnable { ring.visibility = View.VISIBLE }

    /** A miniatura está na frente do vídeo (ele ainda não mostrou o primeiro quadro). */
    val posterShown: Boolean get() = poster.visibility == View.VISIBLE && poster.alpha == 1f

    val ringShown: Boolean get() = ring.visibility == View.VISIBLE

    init {
        // Não pega toques nem foco: os toques no vídeo e o D-pad continuam com o PlayerView.
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        addView(poster, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        val size = (resources.displayMetrics.density * (if (tv) RING_TV_DP else RING_DP)).toInt()
        addView(ring, LayoutParams(size, size, Gravity.CENTER))
    }

    /** Um vídeo novo foi pedido: a miniatura dele e o círculo, já. */
    fun start(thumbnailUrl: String?) {
        poster.animate().cancel()
        poster.alpha = 1f
        poster.visibility = View.VISIBLE
        if (thumbnailUrl != shownUrl) {
            request?.dispose()
            shownUrl = thumbnailUrl
            poster.setImageDrawable(null)
            if (thumbnailUrl != null) {
                request = SingletonImageLoader.get(context).enqueue(
                    ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        // Pequena: só as cores importam, e o desfoque roda numa imagem de poucos pontos.
                        .size(DECODE_PX, DECODE_PX)
                        .allowHardware(false)
                        .transformations(PosterBlur.Coil)
                        .target(onSuccess = { image -> if (shownUrl == thumbnailUrl) poster.setImageDrawable(image.asDrawable(resources)) })
                        .build(),
                )
            }
        }
        setBusy(true)
    }

    /** A miniatura de [start] já baixada: desfoca aqui mesmo (para as fotos de tela, sem rede). */
    fun showPoster(thumbnail: Bitmap) {
        poster.setImageBitmap(PosterBlur.blurred(thumbnail))
    }

    /** O primeiro quadro do vídeo apareceu: a miniatura sai. */
    fun firstFrame() {
        if (poster.visibility != View.VISIBLE) return
        poster.animate().cancel()
        poster.animate().alpha(0f).setDuration(POSTER_FADE_MS).setInterpolator(DecelerateInterpolator())
            .withEndAction { hidePoster() }
    }

    /** O vídeo não vai tocar (o erro aparece na tela): sem miniatura e sem círculo. */
    fun stop() {
        poster.animate().cancel()
        hidePoster()
        setBusy(false)
    }

    /**
     * Liga ou desliga o círculo ([LoadingRules.busy]). Antes do primeiro quadro, na hora; no meio do
     * vídeo, só se o carregando durar ([LoadingRules.MID_VIDEO_DELAY_MS]).
     */
    fun setBusy(busy: Boolean) {
        removeCallbacks(showRing)
        when {
            !busy -> ring.visibility = View.GONE
            ringShown -> Unit
            posterShown -> ring.visibility = View.VISIBLE
            else -> postDelayed(showRing, LoadingRules.MID_VIDEO_DELAY_MS)
        }
    }

    private fun hidePoster() {
        poster.visibility = View.GONE
        poster.alpha = 1f
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(showRing)
        request?.dispose()
        super.onDetachedFromWindow()
    }

    private companion object {
        /** O disco inteiro; o anel fica para dentro ([LoadingRing]). */
        const val RING_DP = 64
        const val RING_TV_DP = 80
        /** O vídeo aparece por baixo das cores da miniatura, sem corte seco. */
        const val POSTER_FADE_MS = 300L

        /** A miniatura é decodificada em no máximo isto de cada lado, antes de reduzir e desfocar. */
        const val DECODE_PX = 320

        /** O brilho da miniatura desfocada: 35% mais escura (eram 55% com ela nítida). */
        const val POSTER_BRIGHTNESS = 0.65f

        /** As cores 40% mais fortes que as da miniatura, para não ficarem lavadas depois do desfoque. */
        const val POSTER_SATURATION = 1.4f

        fun posterTone() = ColorMatrixColorFilter(
            ColorMatrix().apply {
                setSaturation(POSTER_SATURATION)
                postConcat(ColorMatrix().apply { setScale(POSTER_BRIGHTNESS, POSTER_BRIGHTNESS, POSTER_BRIGHTNESS, 1f) })
            },
        )
    }
}

/**
 * O círculo de carregando: um anel apagado com um arco na cor de destaque dando a volta, sobre um
 * disco escuro (no meio de um vídeo claro, o anel sozinho quase não aparecia). Só se redesenha
 * enquanto está na tela.
 */
internal class LoadingRing(context: Context) : View(context) {
    private val stroke = resources.displayMetrics.density * STROKE_DP
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = TRACK
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.player_accent)
    }
    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = DISC }
    private val box = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // O anel fica para dentro do disco.
        val inset = minOf(w, h) * RING_INSET + stroke / 2
        box.set(inset, inset, w - inset, h - inset)
    }

    override fun onDraw(canvas: Canvas) {
        val turn = (SystemClock.uptimeMillis() % TURN_MS) / TURN_MS.toFloat()
        canvas.drawCircle(width / 2f, height / 2f, minOf(width, height) / 2f, disc)
        canvas.drawOval(box, track)
        canvas.drawArc(box, turn * 360f - 90f, SWEEP, false, arc)
        // A próxima volta do desenho; escondido ou fora da tela, o onDraw não roda e o giro para.
        postInvalidateOnAnimation()
    }

    private companion object {
        const val STROKE_DP = 3.5f
        const val TURN_MS = 900L
        const val SWEEP = 100f

        /** O anel por trás do arco: o creme dos ícones, quase apagado (como o trecho não tocado da barra). */
        const val TRACK = 0x38F3EFE4

        /** O disco por trás do anel: 50% de preto. */
        const val DISC = 0x80000000.toInt()

        /** Quanto do tamanho o anel fica para dentro do disco, de cada lado. */
        const val RING_INSET = 0.17f
    }
}
