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
 * As abas do início para o usuário aberto. O convidado só tem os Recomendados e a busca (pedido do
 * Johan, 2026-11-14: "nem precisa das outras telas"): sem conta não há Inscrições, Explore,
 * playlists nem Histórico, e os Ajustes são de cada conta.
 */
fun homeTabs(): List<HomeTab> = if (Users.isGuest) GuestTabs else HomeTab.entries

private val GuestTabs = listOf(HomeTab.RECOMMENDED, HomeTab.SEARCH)
