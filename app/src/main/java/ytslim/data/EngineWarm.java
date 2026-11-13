package ytslim.data;

import com.liskovsoft.youtubeapi.app.AppService;
import com.liskovsoft.youtubeapi.app.PoTokenGate;
import com.liskovsoft.youtubeapi.common.helpers.AppClient;
import com.liskovsoft.youtubeapi.service.YouTubeSignInService;
import com.liskovsoft.youtubeapi.videoinfo.V2.VideoInfoService;

import java.util.Collections;

/**
 * As peças que o motor só prepara quando o primeiro vídeo é pedido, uma por uma, para o
 * {@link EngineWarmup} chamar antes. Em Java porque algumas são "internal" do Kotlin do motor, que o
 * Java enxerga. Todas bloqueiam (rede, WebView, V8): fora da thread da tela.
 */
final class EngineWarm {
    /** O mesmo valor que o motor usa para conferir o decifrador (checkSigData). */
    private static final String SAMPLE_N = "5cNpZqIJ7ixNqU68Y7S";

    private EngineWarm() {
    }

    /**
     * O cliente do YouTube que o próximo vídeo vai usar. Como Object (aqui e abaixo): a classe do
     * cliente também é "internal" do motor, e o Kotlin do app não pode nomeá-la.
     */
    static Object client() {
        return VideoInfoService.instance().getClient();
    }

    /** A página do YouTube (visitante, endereço do player) e o decifrador, lidos do cache ou da rede. */
    static void core(Object client) {
        AppService.instance().refreshCacheIfNeeded((AppClient) client);
    }

    /** O token da conta, renovado se venceu (vale uma hora). */
    static void auth() {
        YouTubeSignInService.instance().checkAuth();
    }

    static boolean tokenExpired(Object client) {
        return PoTokenGate.isPotExpired((AppClient) client);
    }

    /**
     * O "poToken" que o YouTube exige no pedido de cada vídeo. Na primeira vez o motor abre um
     * WebView e faz três pedidos ao YouTube para gerá-lo; depois vale por horas.
     */
    static boolean token(Object client) {
        return PoTokenGate.getPoToken((AppClient) client) != null;
    }

    /** O endereço do player do YouTube em uso: quando muda, o decifrador é outro. */
    static String playerUrl() {
        return AppService.instance().getPlayerUrl();
    }

    /**
     * Decifra um valor de exemplo. Na primeira vez em cada abertura do app, isso baixa o player do
     * YouTube (uns 2 MB de JavaScript) e carrega o V8; se o YouTube trocou o player, também o prepara
     * de novo (o mais demorado). O motor só faria isso no primeiro vídeo.
     */
    static boolean solver(Object client) {
        kotlin.Pair<java.util.List<String>, java.util.List<String>> solved =
                AppService.instance().bulkSigExtract((AppClient) client, Collections.singletonList(SAMPLE_N), null);
        java.util.List<String> n = solved != null ? solved.getFirst() : null;
        return n != null && !n.isEmpty() && n.get(0) != null && !SAMPLE_N.equals(n.get(0));
    }
}
