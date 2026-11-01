package ytslim.data;

import com.liskovsoft.googlecommon.common.helpers.RetrofitOkHttpHelper;

import okhttp3.OkHttpClient;

/**
 * O cliente HTTP do motor, que já põe o login da conta (e os cabeçalhos do YouTube) nas chamadas.
 * Em Java porque o motor declara o helper como "internal" do Kotlin, que o Java enxerga.
 */
final class EngineHttp {
    private EngineHttp() {
    }

    static OkHttpClient client() {
        return RetrofitOkHttpHelper.getClient();
    }
}
