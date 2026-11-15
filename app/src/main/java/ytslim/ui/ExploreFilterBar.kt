package ytslim.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import ytslim.data.SearchFilters

/**
 * Os filtros da Explore, escondidos atrás de um botão ao lado do nome da aba (1.0.8, pedido do
 * Johan: sempre abertos, eles empurravam a lista para baixo, e a Explore era a única aba
 * desalinhada das outras). O botão fica na moldura do início (a barra de cima, a linha das abas da
 * TV ou a barra do título do menu lateral); a fileira, onde cada interface a põe.
 *
 * A Explore conta aqui os filtros dela ([filters]) e como mudá-los ([onChange]). Um filtro fora do
 * padrão ([active]) deixa o botão na cor de destaque, também com a fileira fechada. Começa fechada
 * e vale até o app fechar, como os filtros; cada usuário tem a sua (é criada com o início dele).
 */
@Stable
class ExploreFilterBar {
    var open by mutableStateOf(false)

    var filters by mutableStateOf(SearchFilters())

    var onChange: (SearchFilters) -> Unit = {}

    val active: Boolean get() = !filters.isDefault

    fun toggle() {
        open = !open
    }
}

/** A [ExploreFilterBar] do início aberto (o AppRoot cria uma por usuário). */
val LocalExploreFilterBar = staticCompositionLocalOf { ExploreFilterBar() }
