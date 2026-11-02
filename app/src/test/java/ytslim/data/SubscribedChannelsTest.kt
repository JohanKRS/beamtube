package ytslim.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A lista de canais inscritos lida da página de inscrições da TV. O motor devolve essa lista sem os
 * ids (era por isso que a Explore da 0.5.2 vinha vazia); aqui o id sai dos params de cada aba.
 */
class SubscribedChannelsTest {
    /** Respostas reais da TV guardadas nos testes do motor. */
    private fun fixture(name: String) =
        Json.parseToJsonElement(File("../MediaServiceCore/youtubeapi/src/test/resources/browse/tv/$name").readText())

    @Test
    fun everyChannelTabOfTheTvPageBecomesAChannel() {
        val channels = SubscribedChannels.parse(fixture("2026.02.21_subscriptions.json"))
        assertEquals(956, channels.size)
        assertTrue(channels.all { it.id.matches(Regex("UC[A-Za-z0-9_-]{22}")) && it.title.isNotBlank() })
        assertEquals(channels.size, channels.map { it.id }.toSet().size)
        val first = channels.first()
        assertEquals("UCYrxIIlUsH2xrWiQowOFqHw", first.id)
        assertEquals("FastGoerTV", first.title)
        assertTrue(first.thumbnailUrl.orEmpty().startsWith("https://"))
        // Uma resposta mais antiga, com a aba "All" de params próprios.
        assertTrue(SubscribedChannels.parse(fixture("all_channels.json")).size > 400)
    }

    @Test
    fun theIdComesFromTheBrowseIdOrFromTheParams() {
        fun browse(id: String, params: String? = null) = buildJsonObject {
            put("browseId", JsonPrimitive(id))
            params?.let { put("params", JsonPrimitive(it)) }
        }
        assertEquals("UCYrxIIlUsH2xrWiQowOFqHw", SubscribedChannels.channelId(browse("FEsubscriptions", "igMaChhVQ1lyeElJbFVzSDJ4cldpUW93T0ZxSHc%3D")))
        assertEquals("UCYO_jab_esuFRV4b17AJtAw", SubscribedChannels.channelId(browse("UCYO_jab_esuFRV4b17AJtAw")))
        assertNull(SubscribedChannels.channelId(browse("FEsubscriptions")))
        assertNull(SubscribedChannels.channelId(browse("FEsubscriptions", "igMA")))
    }
}
