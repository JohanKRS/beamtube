package ytslim.data

import android.content.Context
import com.liskovsoft.mediaserviceinterfaces.ContentService
import com.liskovsoft.mediaserviceinterfaces.MediaItemService
import com.liskovsoft.mediaserviceinterfaces.ServiceManager
import com.liskovsoft.mediaserviceinterfaces.SignInService
import com.liskovsoft.sharedutils.prefs.GlobalPreferences
import com.liskovsoft.sharedutils.rx.RxHelper
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitFirstOrNull
import kotlinx.coroutines.withContext

/**
 * Ponto único de acesso ao motor (MediaServiceCore).
 *
 * O resto do app não conversa com o motor diretamente: usa os repositórios deste pacote,
 * que convertem os tipos do motor nos nossos modelos e o RxJava em corrotinas.
 */
object Engine {
    /** Precisa rodar antes de qualquer chamada ao motor: ele pega o Context daqui. */
    fun init(context: Context) {
        GlobalPreferences.instance(context.applicationContext)
        // Liga o serviço de login já aqui: ele recupera a conta salva numa thread própria, e assim
        // ela está pronta antes de a primeira tela perguntar se há login (ver AuthRepository.isSigned).
        signIn
        // Como no SmartTube: erros do RxJava que chegam depois de um cancelamento não derrubam o app.
        RxHelper.setupGlobalErrorHandler()
    }

    val service: ServiceManager get() = YouTubeServiceManager.instance()
    val signIn: SignInService get() = service.signInService
    val content: ContentService get() = service.contentService
    val mediaItems: MediaItemService get() = service.mediaItemService
}

/** Roda uma chamada bloqueante do motor fora da thread principal. */
internal suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

/** Primeiro valor de um Observable do motor, ou null se ele terminar vazio. */
internal suspend fun <T : Any> Observable<T>.firstOrNull(): T? =
    withContext(Dispatchers.IO) { awaitFirstOrNull() }

/** Espera um Observable<Void> do motor terminar (ações como like ou inscrever). */
internal suspend fun Observable<Void>.complete() {
    withContext(Dispatchers.IO) { ignoreElements().await() }
}
