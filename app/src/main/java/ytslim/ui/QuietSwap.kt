package ytslim.ui

/**
 * Uma lista não troca enquanto está na tela (1.0.11, pedido do Johan: a lista guardada aparecia e
 * logo era trocada pela da conta, bem na hora de escolher um vídeo). A lista nova que chega com a
 * aba na tela fica esperando aqui, e entra quando a aba sai da tela ou quando quem a mostra a pega
 * ([take]) para continuar a lista no fim. Com a aba fora da tela, entra na hora.
 */
class QuietSwap<T : Any> {
    /** A aba está na tela. */
    var onScreen = false
        private set

    private var waiting: T? = null

    /** A lista nova chegou: devolve-a se já pode entrar; senão, guarda-a e devolve null. */
    fun arrived(fresh: T): T? {
        if (!onScreen) return fresh
        waiting = fresh
        return null
    }

    /** A aba apareceu ([now]) ou saiu da tela: ao sair, devolve a lista que estava esperando. */
    fun onScreen(now: Boolean): T? {
        onScreen = now
        return if (now) null else take()
    }

    /** A lista que estava esperando, que deixa de esperar (outra carga começou, ou ela vai para o fim da lista). */
    fun take(): T? = waiting.also { waiting = null }
}
