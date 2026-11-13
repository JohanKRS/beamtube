package ytslim.ui.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ytslim.R
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
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

@Composable
private fun TopicChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = Look.Text,
            selectedContainerColor = Look.Glow,
            selectedLabelColor = Look.OnGlow,
            selectedLeadingIconColor = Look.OnGlow,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Look.Outline, selectedBorderColor = Color.Transparent),
        modifier = Modifier.height(40.dp),
    )
}
