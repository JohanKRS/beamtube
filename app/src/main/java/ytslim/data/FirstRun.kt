package ytslim.data

import android.content.Context
import android.content.SharedPreferences

/**
 * O que o app mostra só no primeiro uso. Em preferências comuns (e não no DataStore) porque a
 * primeira tela é escolhida na abertura, sem esperar.
 */
object FirstRun {
    private const val TOPICS_CHOSEN = "topics_chosen"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("first_run", Context.MODE_PRIVATE)
    }

    /** A tela de tópicos já passou (escolhidos ou pulados). Antes disso, ela abre depois do login. */
    val topicsChosen: Boolean
        get() = !::prefs.isInitialized || prefs.getBoolean(TOPICS_CHOSEN, false)

    fun setTopicsChosen() {
        if (::prefs.isInitialized) prefs.edit().putBoolean(TOPICS_CHOSEN, true).apply()
    }
}
