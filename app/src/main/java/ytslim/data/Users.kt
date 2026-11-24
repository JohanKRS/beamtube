package ytslim.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.Immutable
import com.liskovsoft.googlecommon.service.oauth.YouTubeAccount
import com.liskovsoft.mediaserviceinterfaces.oauth.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.security.MessageDigest

/**
 * Um usuário do app: uma conta do YouTube que entrou neste aparelho, ou o convidado ([guest]).
 *
 * [slot] é a "vaga" dos dados dele no aparelho (os ajustes, as listas guardadas, onde cada vídeo
 * parou): ver [Users.dir]. O resto é o que aparece no menu (o nome e a foto) e o que acha a conta no
 * motor ([Users.matches]): o e-mail, o @ do canal, o id da página (canais de marca) e um resumo do
 * token da conta (não o token).
 */
@Immutable
@Serializable
data class User(
    val slot: String,
    val name: String? = null,
    val photo: String? = null,
    val email: String? = null,
    val handle: String? = null,
    val pageId: String? = null,
    val token: String? = null,
    /** A conta está no motor. Saiu da conta: false, e os ajustes ficam para quando ela voltar. */
    val active: Boolean = true,
) {
    val guest: Boolean get() = slot == Users.GUEST
}

/**
 * Os usuários do app (pedido do Johan, 2026-11-14): cada conta do YouTube que entrou neste aparelho
 * é um usuário, com os próprios ajustes e listas guardadas, e o convidado usa o app sem conta, só com
 * os Recomendados e a busca ("é só para quando quiser ver alguma coisa fora do histórico"). O app
 * abre no último usuário, sem tela de escolha; a troca é pela foto no alto ou pelos Ajustes.
 *
 * As contas ficam no motor ([Engine.signIn], que já guarda várias e troca a escolhida, como no
 * SmartTube). Aqui fica qual usuário está aberto e a vaga dos dados de cada um. A vaga [LEGACY] usa
 * os nomes de antes da 1.0.7 (a pasta do app, "settings", "first_run"): fica com a conta que já
 * estava no aparelho, que continua com os tópicos, os bloqueios e o resto.
 */
object Users {
    private const val TAG = "BeamTube"

    /** A vaga do convidado. */
    const val GUEST = "guest"

    /** A vaga com os nomes de antes da 1.0.7. */
    const val LEGACY = "0"

    private const val RECORDS = "records"
    private const val CURRENT = "current"
    private const val NEXT = "next"
    private const val LEGACY_TAKEN = "legacy_taken"

    /** Quanto esperar o motor ler as contas salvas ao abrir o app (leva milissegundos). */
    private const val RESTORE_WAIT_MS = 2_000L

    val guestUser = User(GUEST)

    private val serializer = ListSerializer(User.serializer())

    private lateinit var prefs: SharedPreferences
    private lateinit var filesDir: File

    /** Todos os usuários que já entraram neste aparelho, inclusive os que saíram da conta. */
    private var records: List<User> = emptyList()
    private var lastSavedRecords: List<User>? = null
    private var lastSavedCurrent: String? = null
    private var resolved = false

    private val _current = MutableStateFlow<User?>(null)

    /** O usuário aberto; null antes do primeiro login (a tela de entrar, com o convidado). */
    val current: StateFlow<User?> = _current.asStateFlow()

    private val _accounts = MutableStateFlow<List<User>>(emptyList())

    /** As contas deste aparelho, na ordem do motor (o convidado fica de fora). */
    val accounts: StateFlow<List<User>> = _accounts.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("users", Context.MODE_PRIVATE)
        filesDir = context.filesDir
        records = prefs.getString(RECORDS, null)?.let { runCatching { AppJson.decodeFromString(serializer, it) }.getOrNull() }.orEmpty()
        lastSavedRecords = records
        resolved = false
        _accounts.value = records.filter { it.active }
        val slot = prefs.getString(CURRENT, null)
        lastSavedCurrent = slot
        _current.value = when (slot) {
            null -> null
            GUEST -> guestUser
            else -> records.firstOrNull { it.slot == slot && it.active }
        }
    }

    /** O convidado está aberto: sem conta, só os Recomendados e a busca. */
    val isGuest: Boolean get() = _current.value?.guest == true

    /** A vaga dos dados do usuário aberto (a [LEGACY] antes do primeiro login). */
    val slot: String get() = _current.value?.slot ?: LEGACY

    /** A vaga de cada usuário que fica aberto, a começar pelo de agora. */
    val slots: Flow<String> get() = current.map { it?.slot ?: LEGACY }.distinctUntilChanged()

    /** A pasta dos arquivos de uma vaga: a do app na [LEGACY], como antes da 1.0.7. */
    fun dir(slot: String = this.slot): File =
        if (slot == LEGACY) filesDir else File(filesDir, "users/$slot").also { it.mkdirs() }

    /** Um arquivo do usuário aberto. */
    fun file(name: String, slot: String = this.slot): File = File(dir(slot), name)

    /** O nome das preferências de uma vaga: "settings" na [LEGACY], "settings_1" nas outras. */
    fun prefsName(base: String, slot: String = this.slot): String = if (slot == LEGACY) base else "${base}_$slot"

    /**
     * Na abertura do app: espera o motor ler as contas salvas e acerta o usuário aberto com a conta
     * escolhida nele. Na primeira abertura da 1.0.7, a conta que já estava no aparelho vira o
     * primeiro usuário, com a vaga [LEGACY]. Roda uma vez (as próximas chamadas só devolvem).
     */
    fun resolve(): User? {
        if (!resolved) {
            AuthRepository.awaitRestore(RESTORE_WAIT_MS)
            runCatching { sync() }.onFailure { Log.w(TAG, "users: couldn't read the accounts", it) }
            resolved = true
        }
        return _current.value
    }

    /**
     * Junta as contas do motor com os usuários guardados e acerta o usuário aberto: o guardado, se
     * a conta dele ainda estiver aqui; senão a conta escolhida no motor (a que já estava no aparelho
     * antes da 1.0.7), ou a primeira. O motor fica com a conta do usuário aberto escolhida (nenhuma
     * no convidado).
     */
    @Synchronized
    private fun sync() {
        val engineAccounts = engineAccounts()
        val active = reconcile(engineAccounts, persist = false)
        val selected = engineAccounts.indexOfFirst { it.isSelected }.takeIf { it >= 0 }?.let { active[it] }
        val stored = prefs.getString(CURRENT, null)
        val current = when {
            stored == GUEST -> guestUser
            stored != null -> active.firstOrNull { it.slot == stored }
            else -> null
        } ?: selected ?: active.firstOrNull()
        select(current, engineAccounts)
        setCurrent(current)
    }

    /**
     * Cada conta do motor acha o seu usuário (ou ganha uma vaga nova: a [LEGACY] para a primeira, a
     * que já estava no aparelho antes da 1.0.7). Os usuários sem conta no motor ficam guardados, sem
     * aparecer, com os ajustes para quando a conta voltar. Devolve os usuários na ordem das contas.
     */
    @Synchronized
    internal fun reconcile(engineAccounts: List<YouTubeAccount>, persist: Boolean = true): List<User> {
        val list = records.toMutableList()
        val found = arrayOfNulls<User>(engineAccounts.size)
        // Primeiro pelo token (o mais seguro), depois pelo nome, pelo e-mail e pelo @.
        for (byToken in listOf(true, false)) {
            engineAccounts.forEachIndexed { i, account ->
                if (found[i] != null) return@forEachIndexed
                val at = list.indexOfFirst { record -> found.none { it?.slot == record.slot } && matches(record, account, byToken) }
                if (at >= 0) found[i] = list[at].from(account).also { list[at] = it }
            }
        }
        engineAccounts.forEachIndexed { i, account ->
            if (found[i] == null) found[i] = User(newSlot()).from(account).also { list += it }
        }
        val active = found.filterNotNull()
        records = list.map { r -> r.copy(active = active.any { it.slot == r.slot }) }
        _accounts.value = active
        if (persist) save()
        return active
    }

    /** Escolhe no motor a conta de [user] (nenhuma, no convidado ou sem usuário). */
    private fun select(user: User?, engineAccounts: List<YouTubeAccount> = engineAccounts()) {
        val target = user?.takeIf { !it.guest }?.let { u -> engineAccounts.firstOrNull { matches(u, it, byToken = true) || matches(u, it, byToken = false) } }
        val selected = Engine.signIn.selectedAccount
        if (target == null && selected == null) return
        if (target != null && target == selected) return
        Engine.signIn.selectAccount(target)
        Engine.service.invalidateCache()
    }

    private fun setCurrent(user: User?) {
        _current.value = user
        save(user?.slot)
    }

    /** Só para as fotos de tela e os testes: mostra [current] e [accounts] sem o motor e sem guardar. */
    @androidx.annotation.VisibleForTesting
    internal fun show(current: User?, accounts: List<User>) {
        _accounts.value = accounts
        _current.value = current
    }

    private fun save(current: String? = _current.value?.slot) {
        if (records == lastSavedRecords && current == lastSavedCurrent) return
        lastSavedRecords = records
        lastSavedCurrent = current
        prefs.edit()
            .putString(RECORDS, AppJson.encodeToString(serializer, records))
            .apply { if (current != null) putString(CURRENT, current) else remove(CURRENT) }
            .apply()
    }

    private fun newSlot(): String {
        if (!prefs.getBoolean(LEGACY_TAKEN, false)) {
            prefs.edit().putBoolean(LEGACY_TAKEN, true).apply()
            return LEGACY
        }
        val next = prefs.getInt(NEXT, 1)
        prefs.edit().putInt(NEXT, next + 1).apply()
        return next.toString()
    }

    /**
     * Troca para [user]: escolhe a conta dele no motor, esquece o que era do usuário anterior e abre
     * o app nele (as telas são refeitas, ver AppRoot).
     */
    suspend fun switchTo(user: User) {
        if (user.slot == _current.value?.slot && _current.value?.guest == user.guest) return
        io { synchronized(this) { select(user) } }
        forgetSession()
        setCurrent(user)
        Log.i(TAG, "users: switched to ${if (user.guest) "the guest" else "slot ${user.slot}"}")
    }

    /**
     * Depois do login pelo código: as contas que entraram ([before] é a lista de antes) viram
     * usuários, e o app abre na conta nova (ou na que o motor escolheu, se ela já estava aqui).
     */
    suspend fun signedIn(before: List<Account>) {
        val chosen = io {
            synchronized(this) {
                val now = engineAccounts()
                val fresh = now.filter { a -> before.none { it == a } }
                val pick = fresh.firstOrNull { it.isSelected } ?: fresh.firstOrNull() ?: now.firstOrNull { it.isSelected }
                // Os novos entram na lista sem mudar o usuário aberto; a troca vem logo abaixo.
                val active = reconcile(now)
                pick?.let { p -> active.getOrNull(now.indexOfFirst { it === p }) }
            }
        }
        if (chosen != null) {
            forgetSession()
            io { synchronized(this) { select(chosen) } }
            setCurrent(chosen)
        } else {
            io { synchronized(this) { select(_current.value) } }
        }
    }

    /**
     * Sai da conta de [user] neste aparelho: a conta sai do motor, e as listas guardadas e os pontos
     * dos vídeos somem (como o Sign out de antes). Os ajustes ficam para quando a conta voltar. O app
     * abre na próxima conta, ou na tela de entrar.
     */
    suspend fun signOut(user: User) {
        if (user.guest) return
        io {
            synchronized(this) {
                engineAccounts().firstOrNull { matches(user, it, byToken = true) || matches(user, it, byToken = false) }
                    ?.let { Engine.signIn.removeAccount(it) }
                Engine.service.invalidateCache()
                File(dir(user.slot), FeedCache.FILE).delete()
                File(dir(user.slot), ListCache.DIR).deleteRecursively()
                File(dir(user.slot), WatchPositions.FILE).delete()
                File(dir(user.slot), ExploreRepository.NEXT_FILE).delete()
            }
        }
        forgetSession()
        val next = io {
            synchronized(this) {
                val wasCurrent = _current.value?.slot == user.slot
                if (wasCurrent) prefs.edit().remove(CURRENT).commit()
                sync()
                if (wasCurrent) _accounts.value.firstOrNull() else _current.value
            }
        }
        io { synchronized(this) { select(next) } }
        setCurrent(next)
    }

    /** O que fica na memória durante o uso e é do usuário aberto (a fila, o Save, os pedidos adiantados). */
    private fun forgetSession() {
        PlayQueue.clear()
        SavedVideos.reset()
        ChannelFeedback.reset()
        FeedRepository.forgetChannels()
        LibraryRepository.forgetPlaylists()
        PlaybackRepository.forget()
        WatchPositions.forget()
        ExploreRepository.forget()
        // O token da conta nova (ou nenhum, no convidado) fica pronto antes do primeiro vídeo.
        EngineWarmup.start(delayMs = EngineWarmup.AFTER_LISTS_MS)
    }

    private fun engineAccounts(): List<YouTubeAccount> =
        Engine.signIn.accounts.orEmpty().filterIsInstance<YouTubeAccount>()

    /** Os dados de [account] neste usuário (o nome e a foto mudam no YouTube). */
    private fun User.from(account: YouTubeAccount): User = copy(
        name = account.name ?: name,
        photo = account.avatarImageUrl ?: photo,
        email = account.email ?: email,
        handle = account.channelName ?: handle,
        pageId = account.pageIdToken ?: pageId,
        token = account.refreshToken?.let(::digest) ?: token,
        active = true,
    )

    /**
     * [user] é [account]? Como o motor compara as contas: o mesmo token e a mesma página, ou o mesmo
     * nome com o e-mail e o @ que se sabe.
     */
    internal fun matches(user: User, account: YouTubeAccount, byToken: Boolean): Boolean =
        if (byToken) {
            user.token != null && account.refreshToken != null && user.token == digest(account.refreshToken) &&
                user.pageId == account.pageIdToken
        } else {
            user.name != null && user.name == account.name &&
                (user.email == null || account.email == null || user.email == account.email) &&
                (user.handle == null || account.channelName == null || user.handle == account.channelName)
        }

    private val HEX = "0123456789abcdef".toCharArray()

    private fun digest(token: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        val out = CharArray(24)
        for (i in 0 until 12) {
            val b = bytes[i].toInt() and 0xFF
            out[i * 2] = HEX[b ushr 4]
            out[i * 2 + 1] = HEX[b and 0x0F]
        }
        return String(out)
    }
}
