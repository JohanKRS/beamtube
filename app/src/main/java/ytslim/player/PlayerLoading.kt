package ytslim.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.AccelerateInterpolator
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
 * que algumas trazem, e desfocada ([PosterBlur]; mais leve desde a 1.0.8).
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
        colorFilter = ColorMatrixColorFilter(PosterBlur.tone())
        visibility = View.GONE
    }
    /** O preto do arrastar para fechar (1.0.9), por cima da miniatura e por baixo do círculo. */
    private val shade = View(context).apply {
        setBackgroundColor(Color.BLACK)
        alpha = 0f
        visibility = View.GONE
    }
    private val ring = LoadingRing(context).apply { visibility = View.GONE }

    /** Arrastando para fechar: se a miniatura já estava na frente do vídeo quando o arrastar começou. */
    private var dragFrom: Boolean? = null
    private var request: Disposable? = null
    private var shownUrl: String? = null
    private val showRing = Runnable { ring.visibility = View.VISIBLE }

    /** A miniatura está na frente do vídeo (ele ainda não mostrou o primeiro quadro). */
    val posterShown: Boolean get() = poster.visibility == View.VISIBLE && poster.alpha == 1f

    val ringShown: Boolean get() = ring.visibility == View.VISIBLE

    /** O endereço da miniatura do vídeo de agora (a tela do fim usa a mesma, desfocada). */
    val posterUrl: String? get() = shownUrl

    init {
        // Não pega toques nem foco: os toques no vídeo e o D-pad continuam com o PlayerView.
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        addView(poster, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(shade, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        val size = (resources.displayMetrics.density * (if (tv) RING_TV_DP else RING_DP)).toInt()
        addView(ring, LayoutParams(size, size, Gravity.CENTER))
    }

    /** Um vídeo novo foi pedido: a miniatura dele e o círculo, já. */
    fun start(thumbnailUrl: String?) {
        poster.animate().cancel()
        poster.alpha = 1f
        poster.visibility = View.VISIBLE
        dragFrom = null
        shade.animate().cancel()
        shade.alpha = 0f
        shade.visibility = View.GONE
        if (thumbnailUrl != shownUrl) {
            request?.dispose()
            shownUrl = thumbnailUrl
            poster.setImageDrawable(null)
            if (thumbnailUrl != null) {
                request = SingletonImageLoader.get(context).enqueue(
                    ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        // Pequena: só as cores importam, e o desfoque roda numa imagem de poucos pontos.
                        .size(PosterBlur.DECODE_PX, PosterBlur.DECODE_PX)
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
        // No meio de um arrastar para fechar, a miniatura segue o dedo e sai ao soltar ([undrag]).
        if (dragFrom != null) {
            dragFrom = false
            return
        }
        poster.animate().cancel()
        poster.animate().alpha(0f).setDuration(POSTER_FADE_MS).setInterpolator(DecelerateInterpolator())
            .withEndAction { hidePoster() }
    }

    /**
     * O dedo arrastando para fechar (1.0.9, pedido do Johan: "dar um blurzinho e daí já fechar. Um
     * blur, um fade to black"): o vídeo fica parado e, conforme o dedo desce, a miniatura desfocada
     * aparece por cima dele e a tela escurece. [progress] vai de 0 a 1 no ponto em que soltar fecha.
     */
    fun drag(progress: Float) {
        val from = dragFrom ?: posterShown.also { dragFrom = it }
        poster.animate().cancel()
        shade.animate().cancel()
        if (!from) {
            poster.visibility = View.VISIBLE
            // O desfoque vem antes do escuro: na metade do caminho, já cobre quase todo o vídeo.
            poster.alpha = (progress * DRAG_BLUR_SPEED).coerceAtMost(1f)
        }
        shade.visibility = View.VISIBLE
        shade.alpha = progress * DRAG_SHADE
    }

    /** Soltou antes de fechar: o vídeo volta, sem a miniatura e sem o escuro. */
    fun undrag() {
        val from = dragFrom ?: return
        dragFrom = null
        if (!from) {
            poster.animate().alpha(0f).setDuration(UNDRAG_MS).setInterpolator(DecelerateInterpolator())
                .withEndAction { hidePoster() }
        }
        shade.animate().alpha(0f).setDuration(UNDRAG_MS).setInterpolator(DecelerateInterpolator())
            .withEndAction { shade.visibility = View.GONE }
    }

    /**
     * Soltou para fechar: a miniatura desfocada termina de cobrir o vídeo e tudo escurece até o
     * preto em [durationMs], sem o círculo. Depois disso o player fecha.
     */
    fun fadeOut(durationMs: Long) {
        dragFrom = null
        setBusy(false)
        poster.animate().cancel()
        shade.animate().cancel()
        if (poster.visibility != View.VISIBLE) {
            poster.alpha = 0f
            poster.visibility = View.VISIBLE
        }
        shade.visibility = View.VISIBLE
        poster.animate().alpha(1f).setDuration(durationMs).setInterpolator(DecelerateInterpolator())
        shade.animate().alpha(1f).setDuration(durationMs).setInterpolator(AccelerateInterpolator())
    }

    /** Quanto a tela escurece arrastando, no ponto em que soltar fecha (fechando, vai ao preto). */
    val shadeAlpha: Float get() = if (shade.visibility == View.VISIBLE) shade.alpha else 0f

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

        /** Arrastando, a tela chega a este escuro no ponto em que soltar fecha. */
        const val DRAG_SHADE = 0.6f

        /** A miniatura desfocada cobre o vídeo inteiro com 60% do caminho (1 / 1,6). */
        const val DRAG_BLUR_SPEED = 1.6f

        /** Soltou cedo: o vídeo volta nisto. */
        const val UNDRAG_MS = 180L
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
