package ytslim.data

import androidx.test.core.app.ApplicationProvider
import com.liskovsoft.googlecommon.service.oauth.YouTubeAccount
import com.liskovsoft.sharedutils.helpers.Helpers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Os usuários (pedido do Johan, 2026-11-14): cada conta do motor acha a sua vaga de dados no
 * aparelho, a conta que já estava aqui fica com os dados de antes, e uma conta que sai e volta
 * reencontra os ajustes dela.
 */
@RunWith(RobolectricTestRunner::class)
class UsersTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        context.getSharedPreferences("users", 0).edit().clear().commit()
        Users.init(context)
    }

    private fun account(name: String, token: String, email: String? = "$name@example.com", pageId: String? = null, selected: Boolean = false): YouTubeAccount =
        YouTubeAccount.from(Helpers.mergeData(0, name, "https://photo/$name", selected, token, email, true, pageId, "@$name"))

    @Test
    fun theAccountThatWasHereKeepsTheDataOfBefore() {
        val users = Users.reconcile(listOf(account("Johan", "t1", selected = true)))
        assertEquals(listOf(Users.LEGACY), users.map { it.slot })
        // A vaga de antes usa os nomes de antes da 1.0.7.
        assertEquals("settings", Users.prefsName("settings", Users.LEGACY))
        assertEquals(context.filesDir, Users.dir(Users.LEGACY))
        assertEquals("Johan", users.single().name)
        assertEquals("https://photo/Johan", users.single().photo)
    }

    @Test
    fun eachNewAccountGetsItsOwnPlace() {
        Users.reconcile(listOf(account("Johan", "t1")))
        val users = Users.reconcile(listOf(account("Johan", "t1"), account("Maria", "t2")))
        assertEquals(listOf(Users.LEGACY, "1"), users.map { it.slot })
        assertEquals("settings_1", Users.prefsName("settings", "1"))
        assertEquals(File(context.filesDir, "users/1"), Users.dir("1"))
        assertEquals(users, Users.accounts.value)
    }

    @Test
    fun anAccountThatSignsInAgainFindsItsSettings() {
        Users.reconcile(listOf(account("Johan", "t1"), account("Maria", "t2")))
        // Maria saiu da conta: fica guardada, sem aparecer.
        assertEquals(listOf(Users.LEGACY), Users.reconcile(listOf(account("Johan", "t1"))).map { it.slot })
        // E voltou, com outro token: o nome e o e-mail acham a vaga dela.
        val back = Users.reconcile(listOf(account("Johan", "t1"), account("Maria", "t3")))
        assertEquals(listOf(Users.LEGACY, "1"), back.map { it.slot })
        // Uma conta nova depois disso não pega a vaga de ninguém.
        val more = Users.reconcile(listOf(account("Johan", "t1"), account("Maria", "t3"), account("Ana", "t4")))
        assertEquals("2", more.last().slot)
    }

    @Test
    fun brandChannelsOfOneLoginAreSeparateUsers() {
        // Um login com um canal de marca: o mesmo token, páginas diferentes.
        val users = Users.reconcile(
            listOf(
                account("Johan", "t1", pageId = null),
                account("Johan Music", "t1", email = "Johan@example.com", pageId = "page2"),
            ),
        )
        assertEquals(2, users.map { it.slot }.distinct().size)
        // Lidos de novo, cada um continua na sua vaga.
        val again = Users.reconcile(
            listOf(
                account("Johan Music", "t1", email = "Johan@example.com", pageId = "page2"),
                account("Johan", "t1", pageId = null),
            ),
        )
        assertEquals(users.reversed().map { it.slot }, again.map { it.slot })
    }

    @Test
    fun aRenamedAccountKeepsItsPlace() {
        val before = Users.reconcile(listOf(account("Johan", "t1")))
        val after = Users.reconcile(listOf(account("Johan K", "t1")))
        assertEquals(before.single().slot, after.single().slot)
        assertEquals("Johan K", after.single().name)
    }

    @Test
    fun theGuestHasNoAccountAndNoTopicsScreen() {
        assertTrue(Users.guestUser.guest)
        assertFalse(Users.isGuest)
        assertNotEquals(Users.LEGACY, Users.GUEST)
    }

    @Test
    fun theUsersSurviveTheAppClosing() {
        Users.reconcile(listOf(account("Johan", "t1"), account("Maria", "t2")))
        Users.init(context)
        assertEquals(listOf("Johan", "Maria"), Users.accounts.value.map { it.name })
        // A conta da Maria também é achada pelo token, sem o nome.
        val users = Users.reconcile(listOf(account("Johan", "t1"), account("Mari", "t2")))
        assertEquals(listOf(Users.LEGACY, "1"), users.map { it.slot })
    }
}
