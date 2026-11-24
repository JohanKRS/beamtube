package ytslim.player

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.transformations
import kotlinx.coroutines.delay
import ytslim.R
import ytslim.data.UpNext
import ytslim.data.Video
import ytslim.ui.Look
import ytslim.ui.TypeScale
import ytslim.ui.formatDuration
import ytslim.ui.spacedDots

/**
 * A tela do fim do vídeo ([UpNext]): "Up next" com as sugestões numa fileira e o "Back" (com o foco
 * na TV), e embaixo do título quanto falta para ela voltar sozinha.
 *
 * O fundo é a miniatura do vídeo que acabou, desfocada na tela toda, como na tela de carregando
 * (1.0.8, pedido do Johan: "deveria ficar ali também"; até a 1.0.7 era o último quadro, coberto pelo
 * cinza do fundo do app). Um véu escuro por cima deixa os textos e as sugestões legíveis.
 * Qualquer toque ou tecla ([touched]) recomeça a contagem; com "Play a suggestion", a contagem é
 * para tocar a primeira, e o primeiro toque a troca pela de voltar.
 */
internal class EndScreen(
    context: Context,
    private val tv: Boolean,
    private val onPlay: (Video) -> Unit,
    private val onBack: () -> Unit,
) {
    /** A tela está aberta (fechada, nada é composto e a contagem para). */
    private var active by mutableStateOf(false)
    private var videos by mutableStateOf<List<Video>>(emptyList())

    /** A miniatura do vídeo que acabou (a mesma da tela de carregando, [PlayerLoading.posterUrl]). */
    private var posterUrl by mutableStateOf<String?>(null)

    /** A miniatura já baixada, no lugar de [posterUrl] (para as fotos de tela, sem rede). */
    private var posterBitmap by mutableStateOf<Bitmap?>(null)

    /** A contagem leva à primeira sugestão (até o primeiro toque). */
    private var playsNext by mutableStateOf(false)

    /** Muda a cada toque ou tecla, e a contagem recomeça. */
    private var interactions by mutableIntStateOf(0)

    val view: ComposeView = ComposeView(context).apply {
        visibility = View.GONE
        // Os toques não passam para o vídeo por baixo.
        isClickable = true
        // "this@EndScreen": dentro do apply, Content() seria o do próprio ComposeView.
        setContent { this@EndScreen.Content() }
    }

    val shown: Boolean get() = active

    fun show(suggestions: List<Video>, playNext: Boolean, poster: String?) {
        videos = suggestions
        posterUrl = poster
        playsNext = playNext && suggestions.isNotEmpty()
        interactions++
        active = true
        view.visibility = View.VISIBLE
        view.requestFocus()
    }

    fun hide() {
        if (!active) return
        active = false
        view.visibility = View.GONE
        videos = emptyList()
    }

    /** A miniatura já baixada, desfocada aqui mesmo (para as fotos de tela). */
    fun showPoster(thumbnail: Bitmap) {
        posterBitmap = PosterBlur.blurred(thumbnail)
    }

    /** Um toque ou uma tecla com a tela do fim aberta. */
    fun touched() {
        playsNext = false
        interactions++
    }

    @Composable
    internal fun Content() {
        if (!active) return
        val backFocus = remember { FocusRequester() }
        val firstFocus = remember { FocusRequester() }
        val next = playsNext
        LaunchedEffect(videos) {
            // Na TV, o foco começa no "Back" (ou na primeira sugestão, quando é ela que vai tocar).
            runCatching { if (next) firstFocus.requestFocus() else backFocus.requestFocus() }
        }
        val side = if (tv) 48.dp else 20.dp
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Poster()
            Box(Modifier.fillMaxSize().background(SCRIM))
            Column(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(vertical = if (tv) 40.dp else 20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = side), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        if (videos.isNotEmpty()) {
                            BasicText(
                                stringResource(R.string.end_title),
                                style = if (tv) TITLE_TV else TITLE_TOUCH,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Countdown(next = next, interactions = interactions)
                    }
                    Spacer(Modifier.width(16.dp))
                    BackButton(Modifier.focusRequester(backFocus))
                }
                if (videos.isNotEmpty()) {
                    Spacer(Modifier.height(if (tv) 28.dp else 20.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = side),
                        horizontalArrangement = Arrangement.spacedBy(if (tv) 20.dp else 12.dp),
                    ) {
                        itemsIndexed(videos, key = { _, v -> v.id }) { index, video ->
                            val modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier
                            Suggestion(video, modifier.width(if (tv) 240.dp else 220.dp)) { onPlay(video) }
                        }
                    }
                }
            }
        }
    }

    /** A contagem isolada: cada tique de 1 s recompõe só este texto, sem refazer o pôster nem a fileira. */
    @Composable
    private fun Countdown(next: Boolean, interactions: Int) {
        val wait = if (next) UpNext.NEXT_AFTER_MS else UpNext.BACK_AFTER_MS
        // Recomeça junto com a contagem, para o texto não mostrar o número antigo por um quadro.
        var secondsLeft by remember(interactions, next) { mutableIntStateOf(((wait + 999) / 1000).toInt()) }
        LaunchedEffect(interactions, next) {
            var left = wait
            while (left > 0) {
                secondsLeft = ((left + 999) / 1000).toInt()
                delay(minOf(1000L, left))
                left -= 1000L
            }
            if (next) videos.firstOrNull()?.let(onPlay) ?: onBack() else onBack()
        }
        BasicText(
            stringResource(if (next) R.string.end_next_in else R.string.end_back_in, secondsLeft),
            style = COUNTDOWN_STYLE,
        )
    }

    /** A miniatura do vídeo que acabou, desfocada e no tom da tela de carregando ([PosterBlur]). */
    @Composable
    private fun Poster() {
        val tone = remember { ColorFilter.colorMatrix(ColorMatrix(PosterBlur.tone().array)) }
        val still = posterBitmap
        val url = posterUrl
        if (still != null) {
            val image = remember(still) { still.asImageBitmap() }
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, colorFilter = tone, modifier = Modifier.fillMaxSize())
        } else if (url != null) {
            val context = LocalContext.current
            // O mesmo pedido da tela de carregando: a imagem desfocada já está no cache do carregador.
            val request = remember(url) {
                ImageRequest.Builder(context)
                    .data(url)
                    .size(PosterBlur.DECODE_PX, PosterBlur.DECODE_PX)
                    .allowHardware(false)
                    .transformations(PosterBlur.Coil)
                    .build()
            }
            AsyncImage(model = request, contentDescription = null, contentScale = ContentScale.Crop, colorFilter = tone, modifier = Modifier.fillMaxSize())
        }
    }

    @Composable
    private fun BackButton(modifier: Modifier) {
        var focused by remember { mutableStateOf(false) }
        val style = when {
            tv && focused -> BACK_TV_FOCUSED
            tv -> BACK_TV
            focused -> BACK_TOUCH_FOCUSED
            else -> BACK_TOUCH
        }
        Box(
            modifier
                .onFocusChanged { focused = it.isFocused }
                .clip(BACK_SHAPE)
                .background(if (focused) Look.Text else Look.Pill)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onBack)
                .padding(horizontal = if (tv) 28.dp else 22.dp, vertical = if (tv) 12.dp else 10.dp),
        ) {
            BasicText(stringResource(R.string.end_back), style = style)
        }
    }

    @Composable
    private fun Suggestion(video: Video, modifier: Modifier, onClick: () -> Unit) {
        var focused by remember { mutableStateOf(false) }
        val duration = remember(video.durationMs) { video.durationMs.takeIf { it > 0 }?.let(::formatDuration) }
        val channel = remember(video.channelName, video.info) { spacedDots(channelLine(video)) }
        Column(
            modifier
                .onFocusChanged { focused = it.isFocused }
                .scale(if (focused && tv) 1.06f else 1f)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(CARD_SHAPE)
                    .background(Look.Surface)
                    .then(if (focused) Modifier.border(FOCUSED_BORDER, CARD_SHAPE) else Modifier),
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (duration != null) {
                    BasicText(
                        duration,
                        style = BADGE_STYLE,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(BADGE_SHAPE)
                            .background(Look.Badge)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // Todos os cards com a mesma altura (1.0.12, achado do Johan na TV): o título sempre ocupa
            // duas linhas e o canal, uma. Antes, cada card tinha a altura do texto dele, a fileira
            // mudava de altura conforme rolava, e a tela inteira (centrada) pulava.
            BasicText(
                video.title,
                style = SUGGESTION_TITLE,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            BasicText(
                channel,
                style = SUGGESTION_CHANNEL,
                maxLines = 1,
                minLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    /**
     * O canal do card. Só o primeiro (o próximo da lista de onde o vídeo veio) traz o nome do canal
     * separado; os relacionados da página do vídeo trazem só a linha do YouTube, que começa por ele
     * ("Canal • 1,2 mi de visualizações • há 2 anos"): fica essa linha.
     */
    private fun channelLine(video: Video): String =
        video.channelName?.takeIf { it.isNotBlank() } ?: video.info?.takeIf { it.isNotBlank() } ?: ""

    private companion object {
        /** O véu escuro por cima da miniatura: mais leve em cima, mais forte embaixo, onde ficam os títulos. */
        const val SCRIM_TOP = 0.3f
        const val SCRIM_BOTTOM = 0.55f

        val SCRIM = Brush.verticalGradient(listOf(Color.Black.copy(alpha = SCRIM_TOP), Color.Black.copy(alpha = SCRIM_BOTTOM)))
        val BACK_SHAPE = RoundedCornerShape(50)
        val CARD_SHAPE = RoundedCornerShape(12.dp)
        val BADGE_SHAPE = RoundedCornerShape(6.dp)
        val FOCUSED_BORDER = BorderStroke(2.5.dp, Look.Text)
        val TITLE_TV = TypeScale.headlineSmall.copy(color = Look.Text)
        val TITLE_TOUCH = TypeScale.titleLarge.copy(color = Look.Text)
        val COUNTDOWN_STYLE = TypeScale.bodyMedium.copy(color = Look.Sub)
        val BACK_TV = TypeScale.labelLarge.copy(color = Look.Text, fontSize = 16.sp)
        val BACK_TV_FOCUSED = TypeScale.labelLarge.copy(color = Look.Ground, fontSize = 16.sp)
        val BACK_TOUCH = TypeScale.labelLarge.copy(color = Look.Text, fontSize = 15.sp)
        val BACK_TOUCH_FOCUSED = TypeScale.labelLarge.copy(color = Look.Ground, fontSize = 15.sp)
        val BADGE_STYLE = TypeScale.labelMedium.copy(color = Color.White)
        val SUGGESTION_TITLE = TypeScale.titleSmall.copy(color = Look.Text, fontSize = 13.sp, lineHeight = 17.sp)
        val SUGGESTION_CHANNEL = TypeScale.bodySmall.copy(color = Look.Sub)
    }
}
