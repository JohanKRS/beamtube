# Doorglow (repo ytslim) — cliente do YouTube enxuto (celular, tablet e TV)

Plano vivo do projeto. Atualize este arquivo sempre que uma decisão mudar.

## Objetivo

Um **único APK** que roda no celular, no tablet e na Android TV, com a conta Google do usuário (YouTube Premium). A interface deve ser bonita, ter **bem menos elementos que o app oficial** e filtrar o conteúdo muito melhor.

## Decisões tomadas

- **App novo, não fork.** Kotlin + Jetpack Compose. A interface de TV usa Compose for TV (navegação por D-pad, foco visível, cards maiores). Celular e tablet usam layout de toque, adaptado ao tamanho da tela.
- **Um APK só.** O manifest declara touchscreen e leanback como opcionais, e o app aparece no launcher da TV (`LEANBACK_LAUNCHER`) e no do celular. Na abertura, o app detecta se está numa TV (`UiModeManager`) e escolhe a interface.
- **Motor: MediaServiceCore do SmartTube** (licença MIT), como submódulo git, junto com o `SharedModules`. É ele que faz login, feeds, playlists e a obtenção dos streams, e é mantido de perto pelo autor do SmartTube.
  - **Não editar os submódulos.** Toda adaptação fica no nosso código, numa camada própria (`data/`).
  - Atualizar os submódulos de tempos em tempos para receber as correções.
- **Login:** pelo código de TV (youtube.com/activate), o mesmo fluxo em todos os aparelhos.
- **Fora do app:** envio de vídeos (upload), Shorts, comentários, chat ao vivo e notificações.
- **Nome e ícone:** **Doorglow**. Ícone: a porta entreaberta (fresta de luz) em fundo azul-noite. `applicationId` = `app.doorglow`. Nome e ID ficam no `gradle.properties`; ícone em `app/src/main/res/drawable/ic_launcher_*.xml`; o banner da TV é gerado por `tools/make_tv_banner.py`.
- **Interface em inglês.** (A conversa com o Claude continua em português.)
- **Interações com a conta:** só **like/dislike** e **inscrever/cancelar inscrição**. O botão de inscrição fica na página do vídeo e vale para o canal do vídeo.
- **Inscrição em playlists**, guardada nas playlists salvas da conta (o YouTube não tem isso; ver a seção própria).
- **Links externos** (WhatsApp, navegador, "Compartilhar"): abrem no Doorglow e tocam **sem ir para o histórico** (ver a seção própria).
- **Atualizações pelo Obtainium**, a partir das releases do GitHub (ver a seção própria).

## Telas

| Tela | Notas |
|---|---|
| **Início = Inscrições** | Feed das inscrições como tela principal. |
| Recomendações | Secundária, com exclusão e preferência de tópicos (ver Filtragem). |
| Busca | "Melhorada": filtros de duração, data, tipo e ordenação. Lives só aparecem aqui. |
| Playlists | Inclui **Assistir mais tarde** (o real, que o motor acessa). Reordenar vídeos, inclusive pela TV. |
| Histórico | Lista do que foi assistido. |
| Canal | Página básica do canal (vídeos, playlists). |
| Configurações | Mínimas: filtros, tópicos, canais bloqueados, conta, **mostrar lives nas Inscrições**, playlists inscritas, comportamento dos links externos. |

## Filtragem

- **Shorts:** nunca aparecem, em lugar nenhum.
- **Lives:** nos resultados de busca e, se o usuário quiser, nas Inscrições; nunca nas recomendações.
  - Inscrições: **toggle nas configurações** ("mostrar lives nas Inscrições"), desligado por padrão.
- **Tópicos excluídos** (lista editável): esportes, política, etc.
- **Tópicos preferidos** (lista editável): engenharia, tecnologia, computação, IA, conteúdo técnico. Ganham prioridade nas recomendações.
- **Como classificar:** combinar a categoria oficial do vídeo (Esportes, Notícias e política, Ciência e tecnologia, Educação…), palavras-chave no título e na descrição, e listas de canais.
  - Opção para obter a categoria: YouTube Data API só com chave de API (sem login), `videos.list` com 50 vídeos por chamada a 1 unidade de cota.
  - Avaliar também se a resposta interna já traz a categoria sem custo extra.
- **Filtros na TV** (e no celular): duração (mínima/máxima, preferência por vídeos longos), data de publicação e ordenação.
- **Bloquear canal**, com uma lista para gerenciar os canais bloqueados.

## Player

- **Títulos sempre no idioma original**, sem as traduções automáticas do YouTube.
- **Áudio original**: escolher sempre a faixa marcada como original, nunca a dublagem automática.
- **Controles com bem menos elementos.** Manter apenas:
  - **Pular as partes menos interessantes** (o "pular para frente" do Premium). *Pesquisar* se a API interna expõe isso. Alternativa: usar o gráfico de "mais repetidos" para pular para o próximo trecho de pico.
  - **Ligar e desligar a legenda** com um toque ou botão.
- **Qualidade sempre na máxima.** Uma mudança manual vale só para o vídeo atual; o próximo vídeo volta para a máxima.

## Playlists

- **Salvar com um toque**: um botão de salvar que vai direto para "Assistir mais tarde" (ou para outra playlist escolhida nas configurações).
- **Reordenar vídeos dentro das playlists**, principalmente o Assistir mais tarde. Na TV, isso funciona com um modo "mover", em que o D-pad sobe e desce o item. As mudanças vão para a conta, então aparecem em todos os aparelhos.
  - O MediaServiceCore já tem adicionar, remover, criar e renomear, mas **não tem mover**. Isso precisa ser implementado na nossa camada usando a ação de mover vídeo do endpoint interno `browse/edit_playlist`.

## Inscrição em playlists

O YouTube só tem inscrição em canais; "salvar playlist" não gera feed. O Doorglow monta esse feed, usando as **playlists salvas da conta** como a lista de inscrições (decisão do Johan, 2026-11-01). Assim a lista é a mesma no celular, no tablet e na TV.

- Botão "Inscrever" na página da playlist: salva a playlist na biblioteca da conta (`MediaItemService.savePlaylistObserve`). "Cancelar inscrição" tira da biblioteca (`removePlaylistObserve`).
- Contam como inscritas as playlists **de outros canais** salvas na conta, inclusive as que já estavam salvas antes do Doorglow. As playlists do próprio usuário (Assistir mais tarde e as que ele criou) ficam de fora do feed.
- Lista para gerenciar nas Configurações.
- O feed de Inscrições junta o feed dos canais com os vídeos das playlists inscritas.
- **Sem duplicados:** um vídeo de playlist inscrita só entra no feed se o usuário **não** estiver inscrito no canal do vídeo (se estiver, o vídeo já vem pelo canal).
- Ordenação junto com o resto do feed, pela data de publicação; vídeos de playlists respeitam os mesmos filtros (Shorts, lives, tópicos, canais bloqueados).
- Validado fora do aparelho (2026-11-01): o motor abre uma playlist pública só pelo id (`VL<id>`), com paginação, e cada vídeo traz o canal, que é o que a regra de duplicados precisa.
- A ver na etapa 2: como o motor separa as playlists salvas de outros canais das criadas pelo usuário.

## Links externos

- O Doorglow abre links `youtube.com/watch`, `youtu.be`, `/live/`, `/embed/` e `music.youtube.com`, e textos com link recebidos por "Compartilhar". Shorts ficam de fora.
- Esses vídeos tocam com a conta (qualidade do Premium), mas **o progresso não é enviado ao histórico** do YouTube. A tela mostra "not saved to history". Confirmado no aparelho em 2026-11-01: o vídeo não aparece no histórico.
- Para virar o app padrão de links do YouTube (Android 12+), o usuário precisa: desligar "Abrir links compatíveis" no app do YouTube e ligar, no Doorglow, Configurações > Apps > Doorglow > Abrir por padrão (youtube.com, m.youtube.com, youtu.be). Só o dono do domínio pode fazer isso automaticamente.
- Depois, na tela de Configurações: opção "Links externos: sem histórico / na minha conta / perguntar", com padrão "sem histórico".

## Atualizações (Obtainium)

- Cada versão sai como **release no GitHub** com o APK assinado; o Johan acompanha pelo Obtainium.
- Para lançar: no PR, somar 1 em `app.versionCode` e mudar `app.versionName` no `gradle.properties`. Quando o PR é mesclado no main, o workflow `release.yml` compila, confere a assinatura e cria a release `v<versionName>`. Se a versão já tem release, não faz nada.
- **A chave de assinatura é sempre a mesma** (a do APK 0.1.0). Ela fica fora do git: no secret `DOORGLOW_KEYSTORE_BASE64` do repositório e em `/mnt/project-files/doorglow/signing/` (com um LEIA-ME). O workflow falha se o APK sair com outro certificado.
- O repositório é privado, então o Obtainium precisa de um token do GitHub com leitura do repositório.
- Em cada PR, o workflow `build.yml` compila e roda os testes de unidade.

## Etapas

0. **Base do projeto:** submódulos, build Gradle e um APK que abre no celular e na TV. *Feito em 2026-11-01.*
1. **Protótipo do motor:** login por código, listar as playlists e tocar um vídeo em qualidade máxima com áudio original. *Valida o MediaServiceCore + Media3 antes de construir telas.*
   - *Validado no aparelho pelo Johan em 2026-11-01:* login, playlists, reprodução, áudio original num vídeo dublado e link externo fora do histórico.
2. **Inscrições (Início) + player completo:** controles mínimos, legenda, regra de qualidade, pular partes. Junto: toggle de lives, inscrição em playlists, like/dislike e inscrever/cancelar no vídeo, e a tela de Configurações mínima.
3. **Playlists:** salvar com um toque, reordenar (inclusive na TV).
4. **Busca com filtros, Histórico e Canal.**
5. **Recomendações** com exclusão e preferência de tópicos.
6. **Polimento visual** e rotina de atualização do motor.

Extensão do Firefox: deixa de ser necessária para o "Assistir mais tarde", porque o motor acessa o real. Fica como ideia opcional para limpar o site.

## Perguntas em aberto

Nenhuma no momento.

## Como testar a etapa 1

1. Instalar o APK no celular e na TV.
2. Entrar com o código (youtube.com/activate).
3. Abrir uma playlist (por exemplo, Watch later) e tocar um vídeo. Na barra de cima do player aparecem a resolução, o codec e a faixa de áudio escolhida.
4. Testar um vídeo dublado (ex.: https://youtu.be/0e3GPea1Tyg, do MrBeast): a faixa deve ser a marcada como "original".
5. Abrir um link do YouTube pelo WhatsApp ou pelo "Compartilhar" do navegador: deve tocar e mostrar "not saved to history".

## Notas técnicas (da análise do SmartTube em 2026-11-01)

- **Versões usadas pelo SmartTube:** v32.68, commit `8759c6d`.
  - MediaServiceCore `40fec843eb24bc264e9c8b429fcee25068e230e2`.
  - SharedModules `799cfae8a2f5c609b99e87fc673fc62de1c0199a`.
- **Ferramentas do SmartTube:** AGP 7.4.2, Gradle 7.5, Kotlin 1.8.10, compileSdk 34, minSdk 17, Java 8.
  - Gradle 7.5 **não roda com JDK 21**. Se ficarmos no AGP 7.x, é preciso usar JDK 17.
- **Estrutura de pastas exigida:** os módulos do MediaServiceCore fazem `apply from: '../../SharedModules/constants.gradle'`. Por isso `SharedModules/` precisa ficar **ao lado** de `MediaServiceCore/`, na raiz do repo, como no SmartTube.
  - O `core_settings.gradle` de cada submódulo inclui `:mediaserviceinterfaces`, `:youtubeapi`, `:sharedutils`, `:sharedtests`, `:appupdatechecker2`, `:commons-io-2.8.0` e `:j2v8`.
- **Namespace no AGP 8:** os módulos declaram `package=` no AndroidManifest e não têm `namespace`. O AGP 8 exige `namespace`. Ou injetamos o namespace pelo build raiz (sem editar os submódulos), ou usamos AGP 7.4.2.
- **Flavors:** o `youtubeapi` tem flavors `stbeta`, `ststable` e `stfdroid` (dimensão `default`).
  - O j2v8 vem do módulo local nos flavors `st*` e do Maven no `stfdroid`.
  - O app precisa de `missingDimensionStrategy 'default', 'ststable'`.
- **Interfaces públicas do motor:** ficam em `mediaserviceinterfaces`: `ServiceManager`, `SignInService`, `ContentService`, `MediaItemService` e outras.
- **Referência de filtro no SmartTube:** todo vídeo passa por `VideoGroup.add()`, onde ficam o bloqueio de canais e os filtros de vistos.
- **Recursos do SmartTube que servem de referência:** esconder Shorts, mixes, lives e agendados, canais bloqueados, SponsorBlock, "adicionar à playlist recente" e fixar playlist na barra lateral.
- **O Android SDK baixa normalmente** de `dl.google.com` (command-line tools, build-tools até 36.1).

## Notas técnicas (do build, 2026-11-01)

- **Ferramentas:** AGP 8.13.2, Gradle 8.14.3 (wrapper), Kotlin 2.3.21, compileSdk/targetSdk 36, minSdk 23. Roda com JDK 17 ou 21.
  - Ficamos no AGP 8 porque o AGP 9 muda o Kotlin embutido e a DSL antiga que os módulos do motor usam.
  - As versões mais novas do AndroidX (Compose 1.12, Navigation 2.10, Activity 1.13+ com core 1.18) já pedem AGP 9 e compileSdk 37. Por isso: Compose BOM 2026.06.01, Navigation 2.9.8. Ver `gradle/libs.versions.toml`.
- **Adaptação do motor ao AGP 8, sem tocar nos submódulos** (`build.gradle.kts` da raiz):
  - `namespace` de cada módulo lido do `package=` do manifest dele;
  - `android.nonTransitiveRClass=false` e BuildConfig ligado (o motor usa os dois);
  - Kotlin dos módulos do motor com `jvmTarget` 1.8, igual ao Java deles;
  - OkHttp forçado em 3.12.13 e cronet-api em 105, como no build do SmartTube (o okhttp-brotli puxaria o OkHttp 4, que quebra a compilação);
  - kotlinx-coroutines forçado (o youtubeapi pede a versão "1.8.10", que não existe).
- **Imagens:** Coil 3 com Ktor (HttpURLConnection). O `coil-network-okhttp` exigiria OkHttp 4+, que conflita com o motor.
- **Player (Media3):** o motor gera um MPD (`createMpdStream`) com os formatos DASH; o app faz o parse com o `DashManifestParser` e toca. Ordem de escolha igual à do SmartTube: lives por HLS ou DASH do YouTube; vídeos pelo MPD; depois arquivo único.
  - **SABR:** o Media3 não toca (o SmartTube usa um fork próprio do ExoPlayer). Se um cliente do YouTube só entrega SABR, ou falha, o app troca de cliente (até 6 tentativas). Erros de rede no player (ex.: 403) também trocam de cliente e retomam do mesmo ponto (até 3 vezes).
  - **Áudio original:** o motor rotula cada faixa como "idioma (tipo)", com tipo original, dubbed, dubbed-auto, descriptive. O Media3 preferiria o idioma do sistema (a dublagem em português), então o app define a faixa original como preferida e, se ainda assim vier outra, força a original.
  - **Qualidade máxima:** `forceHighestSupportedBitrate`, sem limite de tamanho de tela. Os parâmetros voltam ao padrão a cada vídeo.
- **V8 só no aparelho:** o motor decifra as URLs com o j2v8, que só tem binários para Android. Nos testes no JVM (Robolectric) dá para validar busca e playlists, mas não os formatos de vídeo.
  - Teste online: `./gradlew :app:testAppDebugUnitTest -Pytslim.online=true --tests '*EngineOnlineTest*'`.
- **Busca:** playlists vêm com tipo indefinido (-1), sem `playlistId` e com `channelId` = `VL<id da playlist>`.
- **APK:** ~52 MB em debug e ~43 MB em release, só com arm64-v8a e armeabi-v7a (o j2v8 tem ~30 MB por ABI). Para emulador, acrescentar x86_64 em `app.abis`.
- **Compilar:** `./gradlew :app:assembleAppRelease` gera `app/build/outputs/apk/app/release/app-app-release.apk`, assinado com a chave de debug (uso pessoal). Os submódulos precisam estar baixados: `git submodule update --init`.
