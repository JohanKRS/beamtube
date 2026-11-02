package ytslim.data

import android.net.Uri
import android.os.SystemClock
import com.liskovsoft.sharedutils.prefs.GlobalPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.rx2.asFlow

/** Login pelo código de TV (youtube.com/activate), igual em todos os aparelhos. */
object AuthRepository {
    const val ACTIVATE_URL = "https://www.youtube.com/activate"

    /** O mesmo endereço, com o código já preenchido (o do QR code na TV e o do botão no celular). */
    fun activateUrl(code: String): String = "$ACTIVATE_URL?user_code=" + Uri.encode(code)

    sealed interface SignInEvent {
        /** Mostrar o código para o usuário digitar em [ACTIVATE_URL]. */
        data class Code(val code: String) : SignInEvent

        /** O usuário confirmou o código; a conta já está salva no motor. */
        data object Done : SignInEvent
    }

    /**
     * Há login? Na abertura do app "a frio" (depois de uma atualização, por exemplo) o motor ainda
     * pode estar carregando a conta salva numa thread própria; antes, o app via "sem login" e pedia
     * o código de novo. Agora, havendo conta salva, espera o motor terminar (leva milissegundos).
     */
    fun isSigned(): Boolean {
        if (Engine.signIn.isSigned) return true
        if (GlobalPreferences.sInstance?.mediaServiceAccountData.isNullOrBlank()) return false
        val deadline = SystemClock.uptimeMillis() + RESTORE_WAIT_MS
        while (!Engine.signIn.isSigned && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(10)
        return Engine.signIn.isSigned
    }

    private const val RESTORE_WAIT_MS = 2_000L

    fun accountName(): String? = Engine.signIn.selectedAccount?.name

    /** A conta com login: o nome e a foto (o avatar da barra de cima no celular). */
    data class Account(val name: String?, val photo: String?)

    fun account(): Account? = Engine.signIn.selectedAccount?.let { Account(it.name, it.avatarImageUrl) }

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
