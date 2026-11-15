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

/**
 * Login pelo código de TV (youtube.com/activate), igual em todos os aparelhos. Cada login acrescenta
 * uma conta (um usuário, ver [Users]); a troca e o sair da conta ficam em [Users].
 */
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

    private const val RESTORE_WAIT_MS = 2_000L

    /**
     * Há uma conta aberta? No convidado, não. Na abertura do app "a frio" (depois de uma atualização,
     * por exemplo) o motor ainda pode estar carregando as contas salvas numa thread própria; antes, o
     * app via "sem login" e pedia o código de novo. Agora, havendo contas salvas, espera o motor
     * terminar de lê-las (leva milissegundos).
     */
    fun isSigned(): Boolean {
        if (Engine.signIn.isSigned) return true
        awaitRestore(RESTORE_WAIT_MS)
        return Engine.signIn.isSigned
    }

    /**
     * Espera o motor ler as contas salvas, se houver alguma. Até a 1.0.6 esperava haver login; com o
     * convidado (contas salvas e nenhuma escolhida) isso segurava a abertura pelos [maxMs] inteiros.
     */
    fun awaitRestore(maxMs: Long) {
        if (GlobalPreferences.sInstance?.mediaServiceAccountData.isNullOrBlank()) return
        val deadline = SystemClock.uptimeMillis() + maxMs
        while (Engine.signIn.accounts.isNullOrEmpty() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(10)
    }

    /**
     * Pede um código ao YouTube e espera o usuário confirmar.
     * O fluxo emite [SignInEvent.Code] e depois [SignInEvent.Done].
     */
    fun signIn(): Flow<SignInEvent> =
        Engine.signIn.signInObserve().asFlow()
            .map<String, SignInEvent> { SignInEvent.Code(it) }
            .onCompletion { error -> if (error == null) emit(SignInEvent.Done) }
            .flowOn(Dispatchers.IO)
}
