package ytslim.data

import android.content.Context
import android.content.SharedPreferences

/**
 * O que o app mostra só no primeiro uso de cada usuário ([Users]). Em preferências comuns (e não no
 * DataStore) porque a primeira tela é escolhida na abertura, sem esperar.
 */
object FirstRun {
    private const val TOPICS_CHOSEN = "topics_chosen"
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    private fun prefs(): SharedPreferences? =
        context?.getSharedPreferences(Users.prefsName("first_run"), Context.MODE_PRIVATE)

    /**
     * A tela de tópicos já passou (escolhidos ou pulados). Antes disso, ela abre depois do login de
     * cada conta. O convidado não tem tópicos (nem Ajustes): para ele, já passou.
     */
    val topicsChosen: Boolean
        get() = Users.isGuest || prefs()?.getBoolean(TOPICS_CHOSEN, false) ?: true

    fun setTopicsChosen() {
        prefs()?.edit()?.putBoolean(TOPICS_CHOSEN, true)?.apply()
    }
}
