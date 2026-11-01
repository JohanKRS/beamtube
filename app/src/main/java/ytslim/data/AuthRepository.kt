package ytslim.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.rx2.asFlow

/** Login pelo código de TV (youtube.com/activate), igual em todos os aparelhos. */
object AuthRepository {
    const val ACTIVATE_URL = "https://www.youtube.com/activate"

    sealed interface SignInEvent {
        /** Mostrar o código para o usuário digitar em [ACTIVATE_URL]. */
        data class Code(val code: String) : SignInEvent

        /** O usuário confirmou o código; a conta já está salva no motor. */
        data object Done : SignInEvent
    }

    fun isSigned(): Boolean = Engine.signIn.isSigned

    fun accountName(): String? = Engine.signIn.selectedAccount?.name

    /**
     * Pede um código ao YouTube e espera o usuário confirmar.
     * O fluxo emite [SignInEvent.Code] e depois [SignInEvent.Done].
     */
    fun signIn(): Flow<SignInEvent> =
        Engine.signIn.signInObserve().asFlow()
            .map<String, SignInEvent> { SignInEvent.Code(it) }
            .onCompletion { error -> if (error == null) emit(SignInEvent.Done) }
            .flowOn(Dispatchers.IO)

    suspend fun signOut() = io {
        Engine.signIn.selectedAccount?.let { Engine.signIn.removeAccount(it) }
        Engine.service.invalidateCache()
    }
}
