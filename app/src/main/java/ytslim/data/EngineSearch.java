package ytslim.data;

import com.liskovsoft.googlecommon.common.helpers.RetrofitHelper;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.youtubeapi.app.AppService;
import com.liskovsoft.youtubeapi.common.models.impl.mediagroup.SearchSectionMediaGroup;
import com.liskovsoft.youtubeapi.search.v2.SearchApi;
import com.liskovsoft.youtubeapi.search.v2.gen.SearchResult;
import com.liskovsoft.youtubeapi.search.v2.gen.SearchResultHelperKt;
import com.liskovsoft.youtubeapi.search.v2.gen.SearchSection;
import com.liskovsoft.youtubeapi.service.YouTubeSignInService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

/**
 * A busca do motor, mas com o pedido montado por nós. O motor só conhece uma tabela pequena de
 * combinações de filtros (duração + tipo, por exemplo, vira "sem filtro"); aqui o app manda os
 * filtros que montou ({@link SearchParams}) e reaproveita a leitura da resposta e a paginação do
 * motor. As classes do motor usadas aqui são "internal" no Kotlin, por isso o atalho em Java
 * (como em {@link EngineHttp}).
 */
final class EngineSearch {
    private static SearchApi api;

    private EngineSearch() {
    }

    /** @param body o corpo do pedido (consulta e filtros), já no formato do cliente de TV. */
    static List<MediaGroup> search(String body) {
        YouTubeSignInService.instance().checkAuth();
        if (api == null) {
            api = RetrofitHelper.create(SearchApi.class);
        }
        Call<SearchResult> call = api.getSearchResult(body, AppService.instance().getVisitorData());
        SearchResult result = RetrofitHelper.get(call);
        if (result == null) {
            // Igual ao motor: uma segunda tentativa, sem login.
            result = RetrofitHelper.get(call.clone(), false);
        }
        List<MediaGroup> groups = new ArrayList<>();
        if (result == null) {
            return groups;
        }
        List<SearchSection> sections = SearchResultHelperKt.getSections(result);
        if (sections != null) {
            for (SearchSection section : sections) {
                if (section != null) {
                    groups.add(new SearchSectionMediaGroup(section));
                }
            }
        }
        return groups;
    }
}
