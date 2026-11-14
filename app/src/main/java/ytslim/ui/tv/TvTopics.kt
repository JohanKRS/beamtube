package ytslim.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import ytslim.R
import ytslim.ui.JustifiedFlow
import ytslim.ui.Look
import ytslim.ui.TopicsViewModel
import ytslim.ui.TopicsViewModel.Step
import ytslim.ui.Wordmark

/** Folga acima da primeira linha de sugestões para o botão em foco, que cresce, não sair cortado. */
private val ChipFocusRoom = 8.dp

/** O visto de uma sugestão marcada e o espaço até o nome. */
private val CheckSize = 18.dp
private val CheckGap = 6.dp
private val ChipPadding = 10.dp

/**
 * A tela de tópicos na TV: a pergunta em cima, as sugestões em botões que OK marca e desmarca (com
 * um visto, que continua à vista com o foco em cima), e embaixo pular/voltar e seguir. O foco começa
 * na primeira sugestão de cada passo. Ver [TopicsViewModel].
 */
@Composable
internal fun TvTopics(vm: TopicsViewModel, firstRun: Boolean, onDone: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val res = LocalContext.current.resources
    val more = state.step == Step.MORE
    BackHandler(enabled = !more) { vm.back() }
    val firstChip = remember { FocusRequester() }
    val scroll = rememberScrollState()
    LaunchedEffect(state.step, state.loaded) {
        if (!state.loaded) return@LaunchedEffect
        scroll.scrollTo(0)
        runCatching { firstChip.requestFocus() }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 26.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.topics_step, if (more) 1 else 2, 2), style = MaterialTheme.typography.labelLarge, color = Look.Sub)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(if (more) R.string.topics_title_more else R.string.topics_title_less), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(if (more) R.string.topics_body_more else R.string.topics_body_less), style = MaterialTheme.typography.bodyMedium, color = Look.Sub)
            }
            if (firstRun) Wordmark(16.sp)
        }
        Spacer(Modifier.height(20.dp - ChipFocusRoom))
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
            if (state.loaded) {
                // Cada linha vai até a borda direita, menos a última (ver JustifiedFlow).
                JustifiedFlow(
                    spacing = 10.dp,
                    lineSpacing = 10.dp,
                    // O botão em foco cresce 10% e a rolagem corta o que passa da borda de cima: a primeira
                    // linha começa um pouco abaixo dela, no mesmo lugar de antes na tela.
                    modifier = Modifier.padding(top = ChipFocusRoom),
                ) {
                    state.shown.forEachIndexed { index, topic ->
                        val selected = topic in state.chosen
                        // A mesma largura marcado ou não: sem o visto, o espaço dele fica dos dois lados do
                        // nome. Assim marcar não empurra as outras sugestões para a linha de baixo.
                        val side = if (selected) ChipPadding else ChipPadding + (CheckSize + CheckGap) / 2
                        Button(
                            onClick = { vm.toggle(topic) },
                            modifier = if (index == 0) Modifier.focusRequester(firstChip) else Modifier,
                            colors = ButtonDefaults.colors(
                                containerColor = if (selected) Look.Glow else Look.Surface,
                                contentColor = if (selected) Look.OnGlow else Look.Text,
                                focusedContainerColor = Look.Text,
                                focusedContentColor = Look.Ground,
                            ),
                            contentPadding = PaddingValues(horizontal = side, vertical = 8.dp),
                        ) {
                            // No meio do botão, que pode estar mais largo que o nome (a linha vai até a borda).
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                if (selected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(CheckSize))
                                    Spacer(Modifier.width(CheckGap))
                                }
                                Text(stringResource(topic.label), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.topics_later), style = MaterialTheme.typography.bodySmall, color = Look.Sub, modifier = Modifier.weight(1f))
            when {
                !more -> OutlinedButton(onClick = { vm.back() }) { Text(stringResource(R.string.topics_back)) }
                firstRun -> OutlinedButton(onClick = { vm.skip(onDone) }) { Text(stringResource(R.string.topics_skip)) }
                else -> OutlinedButton(onClick = onDone) { Text(stringResource(R.string.cancel)) }
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { if (more) vm.next() else vm.save({ res.getString(it.label) }, onDone) },
                colors = ButtonDefaults.colors(
                    containerColor = Look.Glow,
                    contentColor = Look.OnGlow,
                    focusedContainerColor = Look.Text,
                    focusedContentColor = Look.Ground,
                ),
            ) {
                Text(stringResource(if (more) R.string.topics_next else R.string.topics_done))
            }
        }
    }
}
