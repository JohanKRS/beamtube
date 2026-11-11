package ytslim.player

import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import ytslim.R

/**
 * Tocar com a tela desligada (pedido do Johan, 2026-11-11), no celular e no tablet. O Android só
 * deixa um app continuar tocando com a tela apagada num serviço em primeiro plano, que mostra o
 * controle de mídia na notificação e na tela de bloqueio (tocar/pausar, a barra de tempo). Este é
 * esse serviço: a sessão de mídia do Media3 sobre o player da tela aberta ([PlayerActivity]), que
 * continua dona dele. O serviço começa com a tela do player e termina com ela; na TV ele não existe.
 *
 * Quem decide se o vídeo continua é a tela ([PlayerActivity.onStop], [ScreenOffRules]); aqui fica
 * só o que o Android pede para ele poder continuar.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply { setSmallIcon(R.drawable.ic_notification) },
        )
        val player = attached
        if (player == null) {
            // Recriado pelo Android depois de o app fechar (sem a tela do player): não tem o que tocar.
            stopSelf()
            return
        }
        running = this
        show(player)
    }

    /** A sessão passa a falar com [player] (a tela do player foi aberta de novo, ou é outra). */
    private fun show(player: Player) {
        val current = session
        if (current == null) {
            session = MediaSession.Builder(this, player)
                .setSessionActivity(backToPlayer())
                .build()
                .also(::addSession)
        } else if (current.player !== player) {
            current.player = player
        }
    }

    /** O toque na notificação (ou no controle da tela de bloqueio) volta para a tela do vídeo. */
    private fun backToPlayer(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        PlayerActivity.backIntent(this),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        if (running === this) running = null
        // O player é da tela: a sessão sai, ele fica.
        session?.release()
        session = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Doorglow"

        /** O player da tela aberta agora, e a tarefa (a pilha de telas) em que ela está. */
        private var attached: Player? = null
        private var playerTask = -1
        private var running: PlaybackService? = null

        /**
         * A tela do player ficou visível: o serviço começa (ou passa a usar [player]). Tem de ser
         * com a tela na frente, porque o Android não deixa começar o serviço com o app por trás.
         */
        fun attach(activity: Activity, player: Player) {
            attached = player
            playerTask = activity.taskId
            val service = running
            if (service != null) {
                service.show(player)
                return
            }
            try {
                activity.startService(Intent(activity, PlaybackService::class.java))
            } catch (e: IllegalStateException) {
                // Sem o serviço, o vídeo pausa ao apagar a tela, como antes.
                Log.w(TAG, "couldn't start the playback service", e)
            }
        }

        /** A tela do player fechou: o serviço, a notificação e a sessão saem (antes de o player ser solto). */
        fun detach(context: Context, player: Player) {
            if (attached !== player) return
            attached = null
            playerTask = -1
            // Esquecido já: uma tela do player aberta logo depois (a mesma, recriada) começa um serviço novo.
            running = null
            context.stopService(Intent(context, PlaybackService::class.java))
        }

        /**
         * Traz para a frente a tela do player que está tocando, onde quer que ela esteja (um link
         * aberto pelo WhatsApp fica na tarefa do WhatsApp). false = não há player tocando.
         */
        fun bringPlayerToFront(activity: Activity): Boolean {
            if (attached == null || playerTask == -1) return false
            return runCatching {
                activity.getSystemService(ActivityManager::class.java)?.moveTaskToFront(playerTask, 0)
            }.onFailure { Log.w(TAG, "couldn't bring the player back", it) }.isSuccess
        }
    }
}

/** O que a tela do player faz quando sai de vista ([PlayerActivity.onStop]). */
internal object ScreenOffRules {
    /**
     * Continua tocando (só o som) quando a tela do celular ou do tablet apaga no meio do vídeo.
     * Sair do app para outro (Home, outro app) pausa, como antes; na TV, apagar a tela é desligar
     * a TV, e o vídeo pausa.
     */
    fun keepsPlaying(touch: Boolean, screenOn: Boolean, playWhenReady: Boolean): Boolean =
        touch && !screenOn && playWhenReady
}
