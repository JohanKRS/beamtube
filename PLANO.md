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
  - Na TV, a tela mostra também um **QR code** (pedido do Johan, 2026-11-01) com `youtube.com/activate?user_code=<código>`, que já abre a página do Google com o código preenchido. No celular, o botão "Copy code and open the site" abre esse mesmo endereço.
  - O login fica salvo entre atualizações. Até a 0.2.1 o app às vezes pedia o código de novo depois de atualizar (ver as notas técnicas).
- **Fora do app:** envio de vídeos (upload), Shorts, comentários, chat ao vivo e notificações.
- **Nome e ícone:** **Doorglow**. Ícone: a porta entreaberta (fresta de luz) em fundo azul-noite. `applicationId` = `app.doorglow`. Nome e ID ficam no `gradle.properties`.
  - **Cor do ícone à escolha** (decidido em 2026-11-01): o desenho é sempre a porta original, e nas Configurações ("App icon") dá para escolher uma de seis cores: azul-noite (padrão), preto, verde-azulado, ameixa, mostarda e gradiente vivo. São as versões 01 a 06 de `/mnt/project-files/nome-e-icone/versoes/`.
  - Ícones, banners da TV e cores saem de `tools/make_icons.py` (não editar os XMLs gerados à mão). A tabela de cores fica no script.
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
| Busca | "Melhorada": filtros de duração, data, tipo e ordenação, e sugestões enquanto se digita. Resultados misturam vídeos, canais e playlists. Lives só aparecem aqui. |
| Playlists | Inclui **Assistir mais tarde** (o real, que o motor acessa). Reordenar vídeos, inclusive pela TV. |
| Histórico | Lista do que foi assistido. |
| Canal | Página básica do canal: vídeos e playlists (duas abas) e o botão de bloquear. Abre pela busca ou pelo nome do canal no player. |
| Configurações | Mínimas: **mostrar lives nas Inscrições**, playlist do botão Save, tópicos excluídos e preferidos, canais bloqueados, links externos, playlists inscritas, **cor do ícone** e conta. |

- **Abas:** no celular, a barra de baixo tem Subscriptions, Recommended, Playlists, History e Settings, e a Busca abre pela lupa da barra de cima (seis abas não cabem). Na TV, as seis abas ficam na fileira de cima, com Search em terceiro.

## Filtragem

- **Shorts:** nunca aparecem, em lugar nenhum.
- **Lives:** nos resultados de busca e, se o usuário quiser, nas Inscrições; nunca nas recomendações.
  - Inscrições: **toggle nas configurações** ("mostrar lives nas Inscrições"), desligado por padrão.
- **Tópicos excluídos** (lista editável): esportes, política, etc. Saem das Recomendações e dos vídeos das playlists inscritas. O feed dos canais inscritos não é filtrado por tópico: foi o usuário que escolheu o canal.
- **Tópicos preferidos** (lista editável): engenharia, tecnologia, computação, IA, conteúdo técnico. Sobem para o começo de cada página das Recomendações, mantendo a ordem do YouTube entre eles (uma página nova não mexe no que já está na tela).
- **Como classificar (decidido em 2026-11-01): por palavras-chave**, no título e no nome do canal. Cada tópico é um nome e uma lista de palavras-chave, editável nas Configurações. A comparação ignora maiúsculas e acentos e só casa palavras inteiras ("ai" não casa com "aim"). As listas começam com Sports e Politics (excluídos) e Engineering, Technology, Computing e AI (preferidos), com palavras em inglês e português (`TopicRules`).
  - A categoria oficial ficou de fora: as listas do motor não trazem a categoria nem a descrição, e buscá-las custaria uma chamada a mais por vídeo (ou uma chave da Data API). Dá para acrescentar depois, se as palavras-chave não bastarem.
- **Filtros da busca** (TV e celular): data de publicação, duração (menos de 4 min, 4 a 20, mais de 20), tipo (vídeos, canais, playlists) e ordenação (relevância, mais novos, mais vistos, nota). Qualquer combinação vale. No celular são menus; na TV, cada botão passa para o próximo valor.
- **Bloquear canal**, pelo botão na página do canal, com a lista para desbloquear nas Configurações. Canal bloqueado some das Recomendações, da Busca, das Inscrições e dos vídeos de playlists inscritas. Quando o YouTube não manda o ID do canal (acontece na busca), o app compara pelo nome.

## Player

- **Títulos sempre no idioma original**, sem as traduções automáticas do YouTube.
- **Áudio original**: escolher sempre a faixa marcada como original, nunca a dublagem automática.
- **Controles com bem menos elementos.** Manter apenas:
  - **Pular as partes menos interessantes** (botão "Skip ahead"). O motor não expõe o "pular para frente" do Premium nem o gráfico de "mais repetidos", então o app pede o gráfico à página "next" do cliente WEB, sem login (`HeatmapRepository`). O botão vai para o começo do próximo trecho "quente": acima do meio do caminho entre a mediana e o topo (95%) do gráfico, a pelo menos 5 s de distância (`SkipRules`). No vídeo de teste, cai exatamente onde o YouTube marca "Most replayed". Sem gráfico (vídeos com poucas visualizações), vai para o próximo capítulo; sem os dois, o botão não aparece.
  - **Ligar e desligar a legenda** com um toque. O botão liga uma legenda escolhida sozinha (`SubtitleRules`): feita por pessoa no idioma do aparelho, depois feita por pessoa no idioma original, depois qualquer uma feita por pessoa, e por último a automática no idioma original. A escolha (ligada/desligada) vale para os próximos vídeos. Se o download da legenda falhar, o vídeo toca sem ela.
  - Além disso: tocar/pausar e a barra de tempo. Na barra de cima ficam like, dislike e "Subscribe" (vale para o canal do vídeo); só aparecem com login.
- **Qualidade sempre na máxima.** Uma mudança manual vale só para o vídeo atual; o próximo vídeo volta para a máxima.

## Playlists

- **Salvar com um toque**: o botão de marcador na barra de cima do player põe o vídeo direto no "Assistir mais tarde" (ou na playlist escolhida em Settings > Save button) e tira de lá no segundo toque. O ícone cheio mostra que o vídeo já está na playlist.
- **Reordenar vídeos dentro das playlists do usuário**, principalmente o Assistir mais tarde. As mudanças vão para a conta, então aparecem em todos os aparelhos. O botão "Reorder" só aparece nas playlists do próprio usuário.
  - O app carrega a playlist inteira antes de abrir o modo (até umas 5000 vídeos), para que "mover para o fim" seja o fim de verdade. Nada vai para a conta até tocar "Save order".
  - No celular: setas para subir e descer cada vídeo. Na TV: o modo "mover", em que OK pega o vídeo, cima e baixo levam ele pela lista e OK solta. "Voltar" solta o vídeo pego, e de novo cancela.
  - O MediaServiceCore já tem adicionar, remover, criar e renomear, mas **não tem mover**. O app manda os movimentos para o endpoint interno `browse/edit_playlist` (ação `ACTION_MOVE_VIDEO_BEFORE`, como o ytmusicapi), com o mínimo de movimentos (`ReorderPlan`).
  - Cada movimento usa o `setVideoId` do vídeo (a posição dele na playlist), que o app lê pedindo a playlist ao YouTube (cliente de TV e, se faltar, WEB). Uma playlist pública não mostra o `setVideoId` sem ser do dono, então **essa parte só dá para validar no aparelho**, com o login.

## Inscrição em playlists

O YouTube só tem inscrição em canais; "salvar playlist" não gera feed. O Doorglow monta esse feed, usando as **playlists salvas da conta** como a lista de inscrições (decisão do Johan, 2026-11-01). Assim a lista é a mesma no celular, no tablet e na TV.

- Botão "Inscrever" na página da playlist: salva a playlist na biblioteca da conta (`MediaItemService.savePlaylistObserve`). "Cancelar inscrição" tira da biblioteca (`removePlaylistObserve`).
- Contam como inscritas as playlists **de outros canais** salvas na conta, inclusive as que já estavam salvas antes do Doorglow. As playlists do próprio usuário (Assistir mais tarde e as que ele criou) ficam de fora do feed.
- Lista para gerenciar nas Configurações, com "Unsubscribe". Para se inscrever: salvar a playlist no YouTube, ou abrir um link `youtube.com/playlist?list=` no Doorglow (abre a página da playlist com o botão).
- **O que é "novo" numa playlist:** os vídeos de playlist não trazem data. Por isso o app anota, neste aparelho, quando cada vídeo apareceu na playlist (`PlaylistLedger`). O que já estava lá na primeira leitura não entra no feed; o que entrar depois aparece com a data em que foi visto. Por isso, logo depois de se inscrever, a playlist ainda não mostra nada nas Inscrições.
- O feed de Inscrições junta o feed dos canais com os vídeos das playlists inscritas.
- **Sem duplicados:** um vídeo de playlist inscrita só entra no feed se o usuário **não** estiver inscrito no canal do vídeo (se estiver, o vídeo já vem pelo canal).
- Ordenação junto com o resto do feed: a data dos vídeos dos canais vem do texto "há 3 horas" do YouTube; os de playlist usam a data em que foram vistos. Vídeos de playlists respeitam os mesmos filtros (Shorts, lives e, quando existirem, tópicos e canais bloqueados).
- Limite: o app lê até 10 páginas de cada playlist. Numa playlist enorme em ordem "mais antigos primeiro", vídeos novos além disso não aparecem.
- Validado fora do aparelho (2026-11-01): o motor abre uma playlist pública só pelo id (`VL<id>`), com paginação, e cada vídeo traz o canal, que é o que a regra de duplicados precisa.
- **Próprias × de outros canais:** a lista "salvar em" do motor (`getPlaylistsInfo`) traz só as playlists do próprio usuário; as de outros canais são as da biblioteca que não estão nela. Mixes ("RD…") ficam de fora. Cuidado: se o motor não consegue tirar uma playlist alheia da biblioteca, ele tenta *apagar* a playlist como se fosse do usuário. Por isso o app só cancela a inscrição depois de confirmar, na hora, que a playlist não é do usuário.

## Links externos

- O Doorglow abre links `youtube.com/watch`, `youtu.be`, `/live/`, `/embed/` e `music.youtube.com`, e textos com link recebidos por "Compartilhar". Shorts ficam de fora.
- Esses vídeos tocam com a conta (qualidade do Premium), mas **o progresso não é enviado ao histórico** do YouTube. A tela mostra "not saved to history". Confirmado no aparelho em 2026-11-01: o vídeo não aparece no histórico.
- Para virar o app padrão de links do YouTube (Android 12+), o usuário precisa: desligar "Abrir links compatíveis" no app do YouTube e ligar, no Doorglow, Configurações > Apps > Doorglow > Abrir por padrão (youtube.com, m.youtube.com, youtu.be). Só o dono do domínio pode fazer isso automaticamente.
- Nas Configurações, "Links from other apps": sem histórico (padrão), salvar no histórico, ou perguntar a cada link.
- Links de playlist (`/playlist?list=`) abrem a página da playlist, com o botão de inscrição.

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
   - *Implementado em 2026-11-01 (versão 0.2.0), aguardando o teste no aparelho* (ver "Como testar a etapa 2"). O início tem três abas: Subscriptions, Playlists e Settings. Configurações: mostrar lives (inclui as agendadas), links de outros apps, playlists inscritas e conta.
   - *Versão 0.2.1:* seletor da cor do ícone nas Configurações.
   - *Testado no celular pelo Johan em 2026-11-01.* Falta testar na TV.
3. **Playlists:** salvar com um toque, reordenar (inclusive na TV).
4. **Busca com filtros, Histórico e Canal.**
5. **Recomendações** com exclusão e preferência de tópicos.
   - *Etapas 3, 4 e 5 implementadas juntas em 2026-11-01 (versão 0.3.0), a pedido do Johan, aguardando o teste no aparelho* (ver "Como testar as etapas 3 a 5"). Junto: QR code no login da TV e a correção do login perdido nas atualizações.
   - Validado fora do aparelho, sem login: busca com filtros combinados, sugestões, playlists na busca e a página do canal. Precisam do aparelho: Recomendações e Histórico (só existem com login), o botão Save e o reordenar.
6. **Polimento visual** e rotina de atualização do motor.

Extensão do Firefox: deixa de ser necessária para o "Assistir mais tarde", porque o motor acessa o real. Fica como ideia opcional para limpar o site.

## Perguntas em aberto

Nenhuma no momento. Próxima etapa: polimento visual (6), junto com o Johan.

## Como testar as etapas 3 a 5

1. Atualizar pelo Obtainium. **O app deve abrir já com o login** (antes pedia o código de novo depois de atualizar). Se ainda pedir, avise: aí a causa é outra.
2. Recomendações: aba "Recommended". Não deve ter Shorts, lives, esportes nem política, e vídeos de tecnologia, engenharia, computação e IA vêm primeiro.
3. Busca: lupa no celular, aba "Search" na TV. Testar as sugestões, e os filtros juntos (por exemplo "Over 20 minutes" + "Videos" + "Most viewed"). Abrir um canal e uma playlist dos resultados.
4. Canal: na página do canal, as abas Videos e Playlists. Tocar "Block channel" e ver o canal sumir da busca e das Recomendações; desbloquear em Settings > Blocked channels.
5. Histórico: aba "History".
6. Salvar: no player, o marcador ao lado de "Subscribe" põe o vídeo no Watch later (confira no app do YouTube). Em Settings > Save button dá para trocar a playlist.
7. Reordenar: abrir o Watch later (ou uma playlist sua), tocar no ícone de setas (celular) ou "Reorder" (TV), mudar a ordem e tocar "Save order". Conferir no app do YouTube. Na TV: OK pega, cima e baixo movem, OK solta.
8. Tópicos: em Settings, editar um tópico (nome e palavras separadas por vírgula), criar um novo e remover outro, e ver o efeito nas Recomendações.
9. TV: o login mostra o QR code ao lado do código (para testar, é preciso sair da conta em Settings).
10. Player: o nome do canal na barra de cima abre a página do canal.

## Como testar a etapa 2

1. Atualizar pelo Obtainium (ou instalar o APK por cima).
2. Inscrições: abre como tela inicial, sem Shorts e sem lives. Ligar "Show live streams" em Settings e ver as lives aparecerem.
3. Player: tocar um vídeo popular e usar "Skip ahead" (deve ir para um trecho mais assistido); ligar e desligar a legenda; dar like, tirar o like e inscrever/cancelar no canal (conferir no app do YouTube).
4. Playlists inscritas: abrir um link `youtube.com/playlist?list=` de uma playlist de outro canal, tocar "Subscribe" e ver a playlist em Settings. Os vídeos que entrarem nela depois aparecem nas Inscrições (só os de canais em que você não é inscrito).
5. Links de outros apps: trocar para "Ask every time" e abrir um link pelo WhatsApp.
6. Na TV: andar pelas abas com o D-pad, voltar do conteúdo para as abas com "voltar", e usar os botões do player.
7. Cor do ícone (0.2.1): em Settings > App icon, escolher outra cor. Em alguns segundos o ícone do celular e o banner da TV mudam. Se o atalho da tela inicial sumir, é só pôr de novo pela lista de apps.
   - A própria atualização para a 0.2.1 pode tirar o atalho da tela inicial uma vez, porque a entrada do launcher mudou de lugar (ver as notas técnicas).

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
- **Busca:** todos os itens vêm com tipo indefinido (-1). Canal: `channelId` "UC…" e sem `videoId`. Playlist: ou `channelId` = `VL<id>`, ou o `playlistId` junto com o `videoId` do primeiro vídeo (e duração 0, com "16 lessons" no badge); por isso o app trata como playlist todo item com `playlistId`. Vídeos da busca vêm sem `channelId`, e o `author` traz "Canal • views • data".
  - O primeiro grupo da resposta é o dos resultados (o único com continuação); os outros são prateleiras ("People also watched", Shorts…), que ficam de fora.
  - **Filtros:** o motor só conhece uma tabela pequena de combinações (`SearchFilterHelper`); duração + tipo, por exemplo, virava "sem filtro". O app monta o parâmetro de filtros ele mesmo (`SearchParams`, um protobuf em base64, o mesmo `sp` do site) e faz a busca pela API do motor por um atalho em Java (`EngineSearch.java`), reaproveitando a leitura da resposta e a paginação do motor. Esse atalho usa classes "internal" do motor: se uma atualização do motor quebrar a compilação ali, é o primeiro lugar a olhar.
- **Canal:** o motor devolve a página em grupos: "Videos" (com paginação), "Shorts", "For You" e seções que costumam ser playlists (todos os vídeos com o mesmo `playlistId`, ex.: "Physics"). O app usa o primeiro grupo com vídeos que não são Shorts como "Videos" e as seções-playlist como "Playlists".
- **Chamadas que o motor não tem** (mover vídeo, ler `setVideoId`): `Innertube.post` usa o cliente HTTP do próprio motor (`EngineHttp.java`), que já põe o login nos cabeçalhos, e os corpos de pedido do motor (`PostDataHelper.createQueryTV`/`createQueryWeb`).
- **Login perdido nas atualizações (corrigido na 0.3.0):** o serviço de login do motor só é criado quando alguém pede, e aí carrega a conta salva numa thread própria. Na primeira abertura depois de uma atualização, o app perguntava "tem login?" antes de a conta carregar, ouvia "não" e mostrava o código. Agora o `Engine.init` cria o serviço logo no começo, e o `AuthRepository.isSigned()`, se há conta salva, espera o motor terminar de carregar (até 2 s).
- **APK:** ~52 MB em debug e ~43 MB em release, só com arm64-v8a e armeabi-v7a (o j2v8 tem ~30 MB por ABI). Para emulador, acrescentar x86_64 em `app.abis`.
- **Cor do ícone:** cada cor é um `activity-alias` da `MainActivity` no manifest, com o próprio ícone e o banner da TV, e só um fica ligado (`data/AppIcons.kt`, com `PackageManager.setComponentEnabledSetting` e `DONT_KILL_APP`). O alias ligado é a própria escolha: o app não guarda a cor em outro lugar.
  - **Nunca renomear nem tirar um alias:** quem estivesse com ele ligado ficaria sem o app no launcher.
  - Até a 0.2.0 a entrada do launcher era a própria `MainActivity`; a partir da 0.2.1 é o alias. Por isso alguns launchers tiram o atalho da tela inicial nessa atualização, e também a cada troca de cor.
- **Compilar:** `./gradlew :app:assembleAppRelease` gera `app/build/outputs/apk/app/release/app-app-release.apk`, assinado com a chave de debug (uso pessoal). Os submódulos precisam estar baixados: `git submodule update --init`.
