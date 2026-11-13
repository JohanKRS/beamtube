package ytslim.data

import android.util.Log
import com.liskovsoft.mediaserviceinterfaces.data.FeedbackEndpoint
import com.liskovsoft.youtubeapi.common.helpers.PostDataHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * O "Don't recommend channel" do YouTube para os canais bloqueados no BeamTube (pedido do Johan,
 * 2026-11-13). O bloqueio do BeamTube fica no aparelho; este pedido vai para a conta, e o YouTube
 * para de recomendar o canal em todo aparelho, no app oficial e no site também.
 *
 * O YouTube só dá esse pedido no menu dos cards do Início (os Recomendados), um por vídeo, e não
 * tem como ler a lista que a conta já tem. Então:
 * - cada página dos Recomendados deixa anotado, por canal, o pedido dos cards ([note]);
 * - bloquear um canal manda o pedido na hora, se um vídeo dele passou pelos Recomendados desde que
 *   o app abriu ([dontRecommend]);
 * - senão, o pedido vai na próxima vez que o YouTube recomendar o canal neste aparelho: o vídeo nem
 *   aparece (o canal está bloqueado), mas o card dele traz o pedido ([note]).
 *
 * Desbloquear não desfaz o pedido: o YouTube só apaga esses pedidos todos de uma vez, na Minha
 * atividade do Google.
 */
object ChannelFeedback {
    private const val TAG = "BeamTube"

    private val known = Registry()

    /** Canais que já tiveram um pedido mandado sozinho desde que o app abriu (um por abertura). */
    private val tried: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** Uma página dos Recomendados chegou: anota os pedidos e manda os dos canais bloqueados. */
    fun note(videos: List<Video>) {
        if (!known.note(videos)) return
        AppScope.launch {
            val blocked = runCatching { SettingsRepository.current().filters.blocked }.getOrElse { return@launch }
            for (channel in blocked) {
                if (channel.onAccount) continue
                val request = known.find(channel) ?: continue
                if (!tried.add(channel.id)) continue
                if (send(request, channel)) SettingsRepository.markOnAccount(channel.id)
            }
        }
    }

    /**
     * Manda o "Don't recommend channel" de [channel], que acabou de ser bloqueado. true se o YouTube
     * aceitou; false se nenhum vídeo do canal passou pelos Recomendados ainda, ou se não deu certo
     * (o pedido vai na próxima vez que o YouTube recomendar o canal).
     */
    suspend fun dontRecommend(channel: BlockedChannel): Boolean {
        val request = known.find(channel) ?: return false
        tried += channel.id
        val sent = send(request, channel)
        if (sent) SettingsRepository.markOnAccount(channel.id)
        return sent
    }

    private suspend fun send(request: NotRecommend, channel: BlockedChannel): Boolean = try {
        val token = when (request) {
            is NotRecommend.Token -> request.value
            // O painel traz o menu inteiro; como no SmartTube, o segundo pedido é o do canal.
            is NotRecommend.Panel -> io { Engine.mediaItems.getFeedbackTokens(request.endpoint()) }
                ?.takeIf { it.size == 2 }?.get(1)
        }
        if (token == null) {
            Log.i(TAG, "don't recommend ${channel.id}: YouTube didn't send the request")
            false
        } else {
            val body = PostDataHelper.createQueryTV("\"feedbackTokens\":[${JsonPrimitive(token)}]")
            val processed = FeedbackResponse.processed(Innertube.post("feedback", body, Innertube.TV_HEADERS))
            Log.i(TAG, "don't recommend ${channel.id} (${channel.name}): " + if (processed) "accepted" else "refused")
            processed
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Log.w(TAG, "don't recommend ${channel.id}: failed", e)
        false
    }

    private fun NotRecommend.Panel.endpoint(): FeedbackEndpoint {
        val id = panelId
        val values = params
        return object : FeedbackEndpoint {
            override fun getPanelId(): String = id
            override fun getParams(): String = values
        }
    }

    /**
     * O pedido de cada canal visto nos Recomendados. Os cards do Início costumam vir sem o ID do
     * canal, então vale também o nome, como no bloqueio ([TopicRules.isBlocked]).
     */
    class Registry {
        private val requests = ConcurrentHashMap<String, NotRecommend>()

        /** Anota os pedidos de [videos]. true se algum veio. */
        fun note(videos: List<Video>): Boolean {
            var any = false
            for (video in videos) {
                val request = video.notRecommend ?: continue
                keys(video.channelId, video.channelName).forEach { requests[it] = request }
                any = true
            }
            return any
        }

        fun find(channel: BlockedChannel): NotRecommend? =
            keys(channel.id, channel.name).firstNotNullOfOrNull { requests[it] }

        private fun keys(id: String?, name: String?): List<String> = listOfNotNull(
            id?.takeIf { it.isNotBlank() }?.let { "id:$it" },
            Handles.channelName(name)?.let(TopicRules::normalize)?.takeIf { it.isNotEmpty() }?.let { "name:$it" },
        )
    }
}

/** A resposta do YouTube a um pedido de feedback: `{"feedbackResponses":[{"isProcessed":true}]}`. */
object FeedbackResponse {
    fun processed(root: JsonElement): Boolean {
        val responses = (root as? JsonObject)?.get("feedbackResponses") as? JsonArray ?: return false
        val first = responses.firstOrNull() as? JsonObject ?: return false
        return (first["isProcessed"] as? JsonPrimitive)?.booleanOrNull == true
    }
}
