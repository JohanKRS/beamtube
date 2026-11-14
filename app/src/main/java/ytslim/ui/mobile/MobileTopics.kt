package ytslim.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ytslim.R
import ytslim.ui.JustifiedFlow
import ytslim.ui.Look
import ytslim.ui.TopicsViewModel
import ytslim.ui.TopicsViewModel.Step
import ytslim.ui.Wordmark

/**
 * A tela de tópicos no celular e no tablet: o passo (1 de 2), a pergunta, as sugestões em pílulas
 * que se marcam com um toque, e embaixo os botões de pular/voltar e seguir. Ver [TopicsViewModel].
 */
@Composable
internal fun MobileTopics(vm: TopicsViewModel, firstRun: Boolean, onDone: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val res = LocalContext.current.resources
    val more = state.step == Step.MORE
    BackHandler(enabled = !more) { vm.back() }
    val scroll = rememberScrollState()
    LaunchedEffect(state.step) { scroll.scrollTo(0) }

    Column(Modifier.fillMaxSize().background(Look.Ground).windowInsetsPadding(WindowInsets.systemBars)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                Spacer(Modifier.height(28.dp))
                if (firstRun) {
                    Wordmark(24.sp)
                    Spacer(Modifier.height(28.dp))
                }
                Text(stringResource(R.string.topics_step, if (more) 1 else 2, 2), style = MaterialTheme.typography.labelLarge, color = Look.Sub)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(if (more) R.string.topics_title_more else R.string.topics_title_less),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(if (more) R.string.topics_body_more else R.string.topics_body_less),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Look.Sub,
                )
                Spacer(Modifier.height(22.dp))
                if (state.loaded) {
                    // Cada linha vai até a borda direita, menos a última (ver JustifiedFlow).
                    JustifiedFlow(spacing = 8.dp, lineSpacing = 2.dp) {
                        state.shown.forEach { topic ->
                            TopicChip(stringResource(topic.label), selected = topic in state.chosen) { vm.toggle(topic) }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.topics_later), style = MaterialTheme.typography.bodySmall, color = Look.Sub)
                Spacer(Modifier.height(24.dp))
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Look.Surface).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                !more -> TextButton(onClick = { vm.back() }) { Text(stringResource(R.string.topics_back)) }
                firstRun -> TextButton(onClick = { vm.skip(onDone) }) { Text(stringResource(R.string.topics_skip)) }
                else -> TextButton(onClick = onDone) { Text(stringResource(R.string.cancel)) }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (more) vm.next() else vm.save({ res.getString(it.label) }, onDone) },
                colors = ButtonDefaults.buttonColors(containerColor = Look.Glow, contentColor = Look.OnGlow),
            ) { Text(stringResource(if (more) R.string.topics_next else R.string.topics_done)) }
        }
    }
}

/** O visto de uma sugestão marcada, o espaço até o nome e a borda da pílula. */
private val CheckSize = 18.dp
private val CheckGap = 6.dp
private val ChipPadding = 10.dp

/**
 * Uma sugestão. Tem a mesma largura marcada ou não (sem o visto, o espaço dele fica dos dois lados do
 * nome), para marcar não empurrar as outras para a linha de baixo. O conteúdo fica no meio, porque a
 * linha pode esticar a pílula até a borda.
 */
@Composable
private fun TopicChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val side = if (selected) ChipPadding else ChipPadding + (CheckSize + CheckGap) / 2
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.semantics { role = Role.Checkbox }.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Look.Glow else Color.Transparent,
        contentColor = if (selected) Look.OnGlow else Look.Text,
        border = if (selected) null else BorderStroke(1.dp, Look.Outline),
    ) {
        Row(
            Modifier.padding(horizontal = side),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(CheckSize))
                Spacer(Modifier.width(CheckGap))
            }
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), maxLines = 1)
        }
    }
}
