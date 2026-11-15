package ytslim.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import ytslim.data.Users

/**
 * Abre a tela de entrar com outra conta (o "Add account" do menu da foto e dos Ajustes). Vem do
 * AppRoot; nas fotos de tela e nos testes, não faz nada.
 */
val LocalAddAccount = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * Um ViewModelStore por usuário ([Users]): os ViewModels das telas (as listas, a busca, os Ajustes)
 * são do usuário aberto. Sobrevive a girar a tela (fica num ViewModel da Activity) e some quando
 * outro usuário abre, junto com tudo o que ele carregou.
 */
class UserStores : ViewModel() {
    private val stores = HashMap<String, ViewModelStore>()

    fun owner(slot: String?): ViewModelStoreOwner {
        val store = stores.getOrPut(slot ?: NONE) { ViewModelStore() }
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    fun drop(slot: String?) {
        stores.remove(slot ?: NONE)?.clear()
    }

    override fun onCleared() {
        stores.values.forEach { it.clear() }
        stores.clear()
    }

    private companion object {
        /** Antes do primeiro login (a tela de entrar). */
        const val NONE = "-"
    }
}

/**
 * As abas do início para o usuário aberto. O convidado só tem a busca (1.0.8, pedido do Johan): sem
 * conta não há Inscrições, Explore, playlists nem Histórico, e os Ajustes são de cada conta. Na 1.0.7
 * ele tinha também os Recomendados, mas sem conta o Início do YouTube vem vazio (conferido em
 * 2026-11-15 pelos clientes de TV e do site: a TV responde só "Deixe o YouTube com a sua cara",
 * e o site, "Que tal uma pesquisa para começar?").
 */
fun homeTabs(): List<HomeTab> = if (Users.isGuest) GuestTabs else HomeTab.entries

private val GuestTabs = listOf(HomeTab.SEARCH)
