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
  - **Cor do ícone à escolha** (decidido em 2026-11-01): o desenho é sempre a porta original, e nas Configurações ("App icon") dá para escolher uma de sete cores: azul-noite (padrão), preto, verde-azulado, ameixa, mostarda, gradiente vivo e vermelho. São as versões 01 a 07 de `/mnt/project-files/nome-e-icone/versoes/`. O vermelho (luz branca em #E53935, no tom do YouTube) entrou em 2026-11-02, a pedido do Johan.
  - Ícones, banners da TV e cores saem de `tools/make_icons.py` (não editar os XMLs gerados à mão). A tabela de cores fica no script.
- **Interface em inglês.** (A conversa com o Claude continua em português.)
- **Interações com a conta:** só **like/dislike** e **inscrever/cancelar inscrição**. O botão de inscrição fica na página do vídeo e vale para o canal do vídeo.
- **Inscrição em playlists**, guardada nas playlists salvas da conta (o YouTube não tem isso; ver a seção própria).
- **Links externos** (WhatsApp, navegador, "Compartilhar"): abrem no Doorglow e tocam **sem ir para o histórico** (ver a seção própria).
- **Atualizações pelo Obtainium**, a partir das releases do GitHub (ver a seção própria).
- **Aparência (etapa 6, aprovada pelo Johan em 2026-11-02, a partir do canvas de mockups):**
  - Fundo grafite (#18191B), não preto nem azul; textos creme; fonte **Figtree**. A luz âmbar da porta aparece só no que importa: a aba ativa (uma "fresta" de luz), a barra de progresso, o foco na TV e o marcador de salvo.
  - **Barras de vidro de ponta a ponta** no celular e no tablet: a barra de cima e a de baixo passam por trás da barra de status, da barra de gestos e das laterais, e o conteúdo rola por baixo delas. O desfoque só existe no Android 12 ou mais novo; nos mais antigos a barra fica quase opaca.
  - **O recorte da câmera é ignorado:** o conteúdo passa por baixo dele, e só as barras do sistema afastam os botões.
  - Barra de cima compacta (o nome da aba e a lupa). A barra de baixo tem **só ícones**, sem nomes.
  - **Deitado** (largura maior que a altura e pelo menos 600 dp), a barra de baixo vira uma coluna de ícones à esquerda, que inclui a Busca. O tablet em pé usa o mesmo layout do celular, com 3 colunas (até a 0.5.0 ele mostrava 2; a grade agora conta pela largura: 1 coluna abaixo de 560 dp, 3 abaixo de 960 dp e, acima disso, quantas couberem com 240 dp, no mínimo 4).
  - Na TV não tem desfoque: o menu e os avisos usam um cinza quase opaco. O foco aumenta o card em 6%, com borda creme e brilho âmbar.

## Telas

| Tela | Notas |
|---|---|
| **Início = Inscrições** | Feed das inscrições como tela principal. |
| Recomendações | Secundária, com exclusão e preferência de tópicos (ver Filtragem) e os filtros de data, duração e ordem da busca. |
| Busca | "Melhorada": filtros de duração, data, tipo e ordenação, e sugestões enquanto se digita. Resultados misturam vídeos, canais e playlists. Lives só aparecem aqui. |
| Playlists | Inclui **Assistir mais tarde** (o real, que o motor acessa). Reordenar e tirar vídeos, inclusive pela TV. |
| Histórico | Lista do que foi assistido, **no formato das playlists** (miniatura à esquerda e o título ao lado; 0.5.1, pedido do Johan). Tirar vídeos dela (0.4.2). |
| Explore | **Vídeos dos canais que os canais inscritos destacam**, recentes e antigos, num sorteio diferente a cada atualização (0.5.2; na 0.5.0 eram categorias feitas à mão e na 0.5.1 temas do histórico; ver "Explore"). Aba a mais: **Recommended continua**. |
| Canal | Página básica do canal: vídeos e playlists (duas abas) e o bloquear, que é só um ícone pequeno (na barra de cima no celular, ao lado do nome na TV). Abre pela busca, pelo nome do canal no player, pelo avatar e nome do canal nos cards (celular e tablet) e pelo menu do OK longo (TV). |
| Configurações | Mínimas: **mostrar lives nas Inscrições**, playlist do botão Save, tópicos excluídos e preferidos, canais bloqueados, links externos, playlists inscritas, **cor do ícone** e conta (com o Sign out). |

- **Abas:** no celular e no tablet (0.4.2, pedido do Johan), a barra de baixo tem os ícones de Subscriptions, Recommended, Explore (0.5.0), Playlists e History; a Busca abre pela lupa e as **Configurações pelo avatar da conta**, os dois na barra de cima (como nos apps do Google). "Voltar" na Busca e nas Configurações leva à aba de onde vieram. Deitado, a coluna da esquerda tem as abas e a Busca, e o avatar fica na barra de cima. Na TV, as sete abas ficam na fileira de cima, com Explore em terceiro e Search em quarto; "voltar" no conteúdo leva às abas, e de novo sai do app.
- **Deslizar para o lado** troca de aba no celular e no tablet (0.4.2, pedido do Johan), na ordem da barra de baixo. Busca e Configurações ficam fora do deslizar.
- As páginas de playlist e de canal não têm a barra de abas: só a barra de cima, com "voltar".
- **Trocar de aba** não perde a posição da lista, e Recommended, Playlists e History carregam logo depois das Inscrições, ao abrir o app, para já estarem prontas (0.4.1).
- **Atualizar as listas** (pedido do Johan, 2026-11-02): no celular e no tablet, **puxar para baixo** no topo da lista recarrega Subscriptions, Recommended, Playlists, History, a página da playlist e a do canal. A lista continua na tela até a nova chegar. Na TV, **segurar OK na aba** Subscriptions, Recommended, Playlists ou History recarrega a lista (quando o OK é solto). Nada recarrega sozinho: as abas carregam quando o app abre e depois só quando o usuário pede.
- **Cards de vídeo:** a miniatura com a duração e o progresso; embaixo, o título, o avatar e o nome do canal (abrem o canal) e o resto da linha (visualizações e data). Canal sem foto (em que o usuário não é inscrito) mostra a inicial num círculo colorido.
- **Sem o @ do canal** (0.4.2, pedido do Johan): o YouTube manda o @ junto com o nome nos cartões da TV ("Veritasium • @veritasium") e na linha dos canais da busca; o app tira o @ das listas, do player e da busca (`Handles`).
- **Menu do OK longo na TV** (0.4.2, pedido do Johan): o menu aparece assim que o toque longo é reconhecido, com o OK ainda apertado. Ele só pega o foco quando o OK é solto, para o "soltar" não escolher a primeira opção sozinho.
- **Tirar vídeos das listas** (0.4.2, pedido do Johan): no Histórico e nas playlists do próprio usuário (inclui Watch later), **deslizar o vídeo para a esquerda** tira ele da lista, com um aviso. Só para a esquerda, para não brigar com o deslizar entre abas (o Histórico é a última aba). Na TV, é uma opção no menu do OK longo ("Remove from history" ou "Remove from this playlist"). O vídeo some na hora e a conta muda logo depois; se a conta recusar, aparece "Couldn't…" e a lista recarrega.
  - Histórico: o menu de cada cartão do YouTube traz o pedido "Remove from watch history" (um `feedbackToken`), que o motor manda pelo mesmo caminho do "Not interested". **Só dá para validar no aparelho**, com o login.

## Filtragem

- **Shorts:** nunca aparecem, em lugar nenhum.
- **Lives:** nos resultados de busca e, se o usuário quiser, nas Inscrições; nunca nas recomendações.
  - Inscrições: **toggle nas configurações** ("mostrar lives nas Inscrições"), desligado por padrão.
- **Tópicos excluídos** (lista editável): esportes, política, etc. Saem das Recomendações e dos vídeos das playlists inscritas. O feed dos canais inscritos não é filtrado por tópico: foi o usuário que escolheu o canal.
- **Tópicos preferidos** (lista editável): engenharia, tecnologia, computação, IA, conteúdo técnico. Sobem para o começo de cada página das Recomendações, mantendo a ordem do YouTube entre eles (uma página nova não mexe no que já está na tela).
- **Como classificar (decidido em 2026-11-01): por palavras-chave**, no título e no nome do canal. Cada tópico é um nome e uma lista de palavras-chave, editável nas Configurações. A comparação ignora maiúsculas e acentos e só casa palavras inteiras ("ai" não casa com "aim"). As listas começam com Sports e Politics (excluídos) e Engineering, Technology, Computing e AI (preferidos), com palavras em inglês e português (`TopicRules`).
  - A categoria oficial ficou de fora: as listas do motor não trazem a categoria nem a descrição, e buscá-las custaria uma chamada a mais por vídeo (ou uma chave da Data API). Dá para acrescentar depois, se as palavras-chave não bastarem.
- **Filtros da busca** (TV e celular): data de publicação, duração, tipo (vídeos, canais, playlists) e ordenação (relevância, mais novos, mais vistos, nota). Qualquer combinação vale. No celular e na TV, cada filtro abre a lista dos valores (na TV, até a 0.4.0, o botão passava para o próximo valor).
  - **Data:** última hora, hoje, esta semana, este mês, este ano, ou **"Before <ano>"** (do ano atual até 2006): só vídeos publicados antes de 1º de janeiro daquele ano (pedido do Johan, 2026-11-02). Na busca, o app manda o operador `before:AAAA-01-01` no texto, que o YouTube entende.
  - **Duração:** menos de 4 min, 4 a 20, mais de 20, e **30, 45 e 60 minutos ou mais** (pedido do Johan, 2026-11-02). Para essas três, o app pede "mais de 20" ao YouTube e corta o resto pela duração de cada vídeo, buscando mais páginas se sobrar pouco.
- **Filtros nas Recomendações** (pedido do Johan, 2026-11-02): data, duração e ordem (relevância, mais novos, mais vistos), num chip no topo da lista (celular) ou numa fileira de botões (TV). O YouTube não filtra essa lista, então o app aplica os filtros em cada vídeo (`FeedFilter`): a duração, o tempo relativo ("3 days ago") e as visualizações. O "Before <ano>" só mostra o que com certeza é anterior: "4 years ago" pode ser de dois anos diferentes e fica de fora. Se os filtros deixarem poucos vídeos, o app busca mais páginas (até 15). Os filtros valem até o app fechar.
- **Bloquear canal**, pelo botão na página do canal, com a lista para desbloquear nas Configurações. Canal bloqueado some das Recomendações, da Busca, das Inscrições e dos vídeos de playlists inscritas. Quando o YouTube não manda o ID do canal (acontece na busca), o app compara pelo nome.

## Player

- **Títulos sempre no idioma original**, sem as traduções automáticas do YouTube.
  - O cliente de TV do YouTube (o primeiro que o motor usa) manda o vídeo **sem título e sem autor**. Até a 0.4.0 o player trocava o título do card por esse vazio. Agora o título vem do stream, se vier; senão do card que abriu o vídeo; e, nos links de outros apps, da página do vídeo (`getMetadata`).
- **Áudio original**: escolher sempre a faixa marcada como original, nunca a dublagem automática.
- **Controles com bem menos elementos.** Manter apenas:
  - **Pular as partes menos interessantes** (botão "Skip ahead"). O Johan queria o "Jump ahead" do YouTube Premium, que pula a parte de merchandising; esse dado não chega a apps de fora (a página "next" sem login só traz o gráfico de "mais repetidos", conferido em 2026-11-02), e ele recusou o SponsorBlock por ser de terceiros. Então o app usa o gráfico do próprio YouTube, pedido à página "next" do cliente WEB, sem login (`HeatmapRepository`), e procura **vales**: um trecho em que a audiência cai abaixo de 30% da mediana do vídeo por 15 s a 5 min (`SkipRules`, 0.5.1).
    - O botão pula do começo do vale (até 20 s à frente) para o ponto em que a audiência volta à mediana. Sem vale por perto, e se o ponto atual estiver abaixo da mediana, vai para o próximo pico em até 60 s. Fora disso, avisa "Nothing to skip here." e não pula.
    - Calibrado com 25 vídeos com patrocínio (o gráfico do YouTube comparado com os trechos que o SponsorBlock marca, só para medir): acerta 33 dos 45 merchandisings, e no meio de conteúdo normal pula em 20% dos toques (a regra da 0.5.0, que ia para o próximo pico, pulava em 91%).
    - **Marcas na barra** (0.5.1, pedido do Johan, como as do app do YouTube): onde começa cada vale, um traço claro na barra de tempo.
    - Sem gráfico (vídeos com poucas visualizações), vai para o próximo capítulo; sem os dois, o botão não aparece.
  - **Ligar e desligar a legenda** com um toque. O botão liga uma legenda escolhida sozinha (`SubtitleRules`): feita por pessoa no idioma do aparelho, depois feita por pessoa no idioma original, depois qualquer uma feita por pessoa, e por último a automática no idioma original. A escolha (ligada/desligada) vale para os próximos vídeos. Se o download da legenda falhar, o vídeo toca sem ela.
    - **Visual da legenda** (0.5.1, pedido do Johan): sempre centralizada, texto branco com um contorno preto fino e sem fundo, na fonte do app (`CaptionRules`). A posição e o fundo que vêm do YouTube (a automática vem alinhada à esquerda) são ignorados; só a altura da linha é mantida.
  - Além disso: tocar/pausar e a barra de tempo. Em cima ficam like, dislike, Save e "Subscribe" (vale para o canal do vídeo); só aparecem com login.
- **Visual do player: opção A** (0.4.2; o Johan pediu um player "mais elegante, mais compacto, mais moderno" e escolheu a A no canvas, entre três opções):
  - Em cima, à esquerda: o título e, embaixo dele, o nome do canal (um toque, ou OK na TV, abre a página do canal) e o aviso "not saved to history" quando for o caso. À direita, uma **pílula de vidro** com like, dislike, Save (âmbar quando salvo) e "Subscribe" (cheio e claro; inscrito, só o contorno).
  - Embaixo, uma **ilha de vidro** arredondada: tocar/pausar num círculo claro, o tempo, a barra (âmbar, com o carregado mais claro), a duração e, depois de um divisor, Skip ahead, legenda, a etiqueta da resolução ("2160p", com contorno), girar a tela (só celular e tablet) e a lua.
    - Numa tela com menos de 720 dp de largura (o celular em pé), a ilha vira **duas linhas** (0.5.1): o tempo e a barra em cima, e embaixo o tocar/pausar e os botões, com o Skip ahead só no ícone (`PlayerIsland`). Numa linha só, os botões não cabiam.
  - Os painéis da resolução e do modo dormir abrem num cartão **à direita, logo acima da ilha**, com as opções em **trilhos segmentados** (a escolhida fica âmbar e cheia). Os trilhos de um painel ficam da mesma largura.
  - Na TV: o foco é um anel âmbar em todos os botões (o tocar/pausar fica âmbar inteiro), e os controles ficam a 48 dp das bordas, dentro da área segura da tela (`values-television`). O título cresce um pouco.
  - Saiu da tela a linha técnica com o codec e o idioma do áudio ("1080p avc1 · audio: en"): a resolução já aparece na etiqueta, e o resto continua no log (tag Doorglow).
- **Toques no vídeo** (celular e tablet, 0.5.1, pedido do Johan, como no app do YouTube): **um toque** mostra ou esconde os controles; **dois toques** no terço da esquerda ou da direita voltam ou avançam 10 s, e no meio pausam ou continuam. Depois de um pulo, cada toque no mesmo lado (por 0,8 s) pula mais 10 s, e um aviso do lado do toque mostra o total ("+30 s"). O toque único espera um instante para ter certeza de que não vem o segundo, então os controles não piscam num toque duplo (`TouchRules`).
- **Girar a tela** (celular e tablet, 0.5.1, pedido do Johan, que queria as duas coisas): o player **gira sozinho** conforme o vídeo (deitado para vídeos largos, em pé para os verticais, seguindo o sensor), e o **botão de girar** na ilha troca entre deitado e em pé; a escolha vale até fechar o player.
  - O Android 16 ignora a orientação pedida pelos apps em telas grandes (tablets). O app declara a exceção que o Android 16 ainda aceita (`PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` no manifest), que deixa de valer quando o app mirar o SDK 37. Não foi testado num tablet.
- **Qualidade sempre na máxima.** Uma mudança manual vale só para o vídeo atual; o próximo vídeo volta para a máxima.
  - **No celular e no tablet o topo é 1080p** (0.5.1, pedido do Johan): o vídeo começa em 1080p e pode cair para 720p. O 4K primeiro é só da TV.
  - **Botão de resolução** (0.4.1, pedido do Johan), na ilha de baixo: mostra a resolução tocando (em âmbar quando escolhida à mão) e abre um painel com "Auto" e as resoluções do vídeo. A escolhida à mão **fica** mesmo com a rede oscilando (o Johan não gosta de o app oficial cair do 4K e não voltar); só sai se o decodificador falhar, com o aviso "Couldn't play …p on this device. Back to Auto."
  - **TV em 360p** (relato do Johan na 0.4.0, TV Hisense com Android 11 de 32 bits; corrigido na 0.4.1 sem log da TV, precisa confirmar nela): duas causas possíveis, as duas tratadas. (1) Um cliente do YouTube que só entrega o arquivo único (360p, o itag 18) era aceito na hora; agora o app tenta os outros clientes antes e prefere o HLS ao arquivo único. (2) Algumas TVs declaram limites de decodificador mais baixos do que decodificam, e o Media3 respeita (`isFormatSupported` com os "performance points" do Android 10+).
  - **4K primeiro na TV** (decisão do Johan, 2026-11-02: "tentar sempre 4K antes, só cair para uma resolução inferior se realmente necessário"; era para a TV): o app escolhe a faixa sozinho (`QualityRules`), a maior com decodificador de hardware até 4K, mesmo que o decodificador declare menos. Fica de fora só o que não tem como dar certo: codec sem decodificador de hardware, mais de 60 quadros por segundo e HDR que o decodificador não aceita. Na mesma resolução, vale a faixa que o decodificador aceita.
  - **Descer um degrau** só quando o aparelho falha de verdade: erro do decodificador ou muitos quadros perdidos duas vezes (o teto cai para a resolução de baixo até o app fechar), ou o vídeo parar para carregar três vezes sem o usuário pular (só naquele vídeo, porque é a rede). Depois de descer pela rede, o app tenta o topo de novo após 2 minutos sem parar para carregar.
  - **Demora para começar:** o app agora prepara o player do YouTube (o JavaScript que decifra os links) ao abrir, como o SmartTube faz (`refreshCacheIfNeeded`). Numa TV fraca, isso levava segundos no primeiro vídeo.
- **Modo dormir** (0.4.1, pedido do Johan, que usa playlists para dormir, principalmente na TV): o botão da lua, na ilha de baixo, abre um painel acima dela com três linhas:
  - **Brightness: 100% / 50% / 25% / 10% / 0%** escurece o vídeo e a legenda, sem mexer no brilho da TV (uma camada preta por cima do vídeo e por baixo dos controles). Escurecido, os controles não acendem sozinhos na troca de vídeo nem no fim. Volta a 100% a cada player aberto, para não pegar a tela escura de dia.
  - **Then play: None / 1 / 2 / 3 / 4 / 5 / All**: quantos vídeos da playlist ainda tocam depois do atual (ver "Tocar em sequência"). Só aparece quando o vídeo veio da página de uma playlist. O número desce a cada vídeo. Começa em All a cada vez. Por decisão do Johan, essa escolha fica **só no player**, não na lista.
  - **When done: Stay on / Turn off** (**só na TV**; no celular e no tablet a linha e o serviço de Acessibilidade saem do app, pedido do Johan na 0.4.2): o que fazer quando os vídeos acabam. A escolha **fica guardada** (pedido do Johan) e vale para todo vídeo ou sequência que terminar. Com "Turn off", o app avisa e espera 10 s (qualquer botão ou toque cancela) e põe o aparelho para dormir. Pelo HDMI-CEC, a TV desliga junto.
  - O Android não deixa um app comum pôr o aparelho para dormir. Por isso o app tem um serviço de Acessibilidade (`ScreenOffService`) que só faz isso (não recebe eventos nem lê a tela) e que o usuário liga uma vez, em Configurações > Acessibilidade. Ao escolher "Turn off" sem o serviço ligado, o app abre essa tela. Precisa do Android 9 ou mais novo; antes disso, a linha não aparece.
  - Com "Stay on", ou sem o serviço, o player só para de manter a tela acesa quando os vídeos acabam, e quem apaga é o timer do aparelho.
  - Com o painel aberto, os controles não somem sozinhos; "voltar" ou a lua fecham o painel.

## Playlists

- **Tocar em sequência** (0.4.1): um vídeo aberto pela página de uma playlist (Watch later ou qualquer outra) é seguido pelos próximos da lista, carregando mais páginas se precisar (`PlayQueue`). Vídeos abertos de outras listas (feed, busca, histórico, canal) ou por link tocam sozinhos. O limite de quantos seguem fica no painel de dormir do player. Um vídeo da sequência que não toca (apagado, privado) mostra o erro por 3 s e é pulado sem gastar o limite; agendados ficam de fora.

- **Salvar com um toque**: o botão de marcador na barra de cima do player põe o vídeo direto no "Assistir mais tarde" (ou na playlist escolhida em Settings > Save button) e tira de lá no segundo toque. O ícone cheio mostra que o vídeo já está na playlist.
- **Salvar pelas listas** (etapa 6): no celular e no tablet, segurar o dedo numa miniatura põe o vídeo na playlist do botão Save, ou tira se já estiver, com um aviso embaixo ("Saved to…"/"Removed from…"). Na TV, segurar o OK abre um menu com duas opções: "Save to…"/"Remove from…" e "Go to channel".
  - **Resposta na hora** (0.4.1, o Johan achou lento na 0.4.0): o marcador e o aviso mudam no toque, e a conta é atualizada logo depois (`SavedVideos.toggle`). Tocar de novo antes de a conta responder só muda o que o usuário quer; no fim a conta fica como o último toque, sem pedido repetido. Se a conta recusar, o marcador volta e aparece "Couldn't…". O botão Save do player usa o mesmo caminho.
  - Um marcador discreto (âmbar) na miniatura mostra os vídeos que já estão nessa playlist. Ele não aparece dentro da própria playlist.
  - O app guarda a lista dos vídeos da playlist do Save (até 10 páginas), lida ao abrir e ao trocar a playlist em Settings. Na hora de salvar ou tirar, ele confere na conta se o vídeo está lá, então o marcador pode estar desatualizado numa playlist enorme, mas o toque faz sempre a coisa certa.
- **Reordenar vídeos dentro das playlists do usuário**, principalmente o Assistir mais tarde. As mudanças vão para a conta, então aparecem em todos os aparelhos. O botão "Reorder" só aparece nas playlists do próprio usuário.
  - O app carrega a playlist inteira antes de abrir o modo (até umas 5000 vídeos), para que "mover para o fim" seja o fim de verdade. Nada vai para a conta até tocar "Save order".
  - No celular e no tablet (0.4.2; as setas da 0.4.0 não funcionaram para o Johan): **arrastar o vídeo pela alça**, ou segurar o vídeo e arrastar; perto da borda a lista rola sozinha. Na TV: o modo "mover", em que OK pega o vídeo, cima e baixo levam ele pela lista e OK solta. "Voltar" solta o vídeo pego, e de novo cancela.
  - O MediaServiceCore já tem adicionar, remover, criar e renomear, mas **não tem mover**. O app manda os movimentos para o endpoint interno `browse/edit_playlist` (ação `ACTION_MOVE_VIDEO_BEFORE`, como o ytmusicapi), com o mínimo de movimentos (`ReorderPlan`).
  - Cada movimento usa o `setVideoId` do vídeo (a posição dele na playlist), que o app lê pedindo a playlist ao YouTube (cliente de TV e, se faltar, WEB). Uma playlist pública não mostra o `setVideoId` sem ser do dono, então **essa parte só dá para validar no aparelho**, com o login.

## Explore

Pedido do Johan (2026-11-02): os filtros das Recomendações só filtram o que o YouTube já mandou, e muita coisa é difícil de descobrir. Ele quer descobrir canais, sem perder as Recomendações.

- Histórico da ideia: na 0.5.0, categorias feitas à mão (buscas por palavra-chave); na 0.5.1, temas achados nos títulos do histórico, que trouxeram "muita recomendação, nada a ver". Na **0.5.2**, o pedido dele: **vídeos dos canais que os canais que ele segue destacam**, misturando vídeos recentes e antigos, e **um subconjunto bem diferente a cada atualização**, com a lista dos canais guardada para não ficar lento.
- **De onde vêm os canais:** a página de cada canal inscrito traz os canais que o criador destaca ("Featured channels", "Other channels to check out"). O app lê essa página pelo motor (o mesmo pedido da página do canal), um por canal inscrito, e guarda o resultado num arquivo do aparelho (`explore.json`). Cada página é lida de novo depois de 30 dias.
  - Na primeira vez, o app lê 12 canais inscritos antes de mostrar a lista (mais 12, até 3 vezes, se aparecerem menos de 30 canais), e o resto em segundo plano, 2 por vez.
  - Ficam de fora os canais que o usuário já segue, os bloqueados e os **canais-irmãos do mesmo criador** (traduções, segundo canal, podcast: "Veritasium em Português", "2veritasium", "Dinge Erklärt – Kurzgesagt", "Mentour Now!"), pelo nome e pelo @ (`ExploreRules.isSibling`). O irmão sai só para quem o destacou.
- **O sorteio** (`ExploreRules`): 10 canais por vez, com mais chance para os destacados por mais canais inscritos. Os canais dos últimos sorteios (os 40 mais recentes) ficam de fora enquanto houver outros, então cada atualização traz canais diferentes.
- **Os vídeos de cada canal sorteado:** a página dele traz a aba de vídeos (os recentes) e as prateleiras (os populares e os escolhidos pelo criador, quase sempre antigos). O app sorteia 2 entre os 12 mais recentes e 2 das prateleiras; se faltar de um lado, o outro completa. Só vídeos do próprio canal (as prateleiras também mostram participações em outros canais). Sem Shorts, lives, vídeos já assistidos (90% ou mais) e tópicos excluídos.
- A lista junta os canais em rodadas (um vídeo de cada canal, depois o segundo…), para não ter vídeos do mesmo canal seguidos. Rolar até o fim sorteia mais 10 canais. **Puxar para atualizar** (OK longo na aba, na TV) faz um sorteio novo.
- Os pedidos vão com a conta logada (o motor põe o login em todo pedido), como abrir a página de um canal no app. Sem login (ou sem as inscrições), a aba mostra o erro.
- Conferido contra o YouTube em 2026-11-02, com 8 canais de exemplo: 41 canais candidatos, e cada sorteio de 10 canais com 40 vídeos levou 2 a 3 s.
- Descartado: as seções prontas do YouTube (Gaming, News, Sports, Live, Podcasts), que trazem conteúdo popular e pouco controle.

## Inscrição em playlists

O YouTube só tem inscrição em canais; "salvar playlist" não gera feed. O Doorglow monta esse feed, usando as **playlists salvas da conta** como a lista de inscrições (decisão do Johan, 2026-11-01). Assim a lista é a mesma no celular, no tablet e na TV.

- Botão "Inscrever" na página da playlist: salva a playlist na biblioteca da conta (`MediaItemService.savePlaylistObserve`). "Cancelar inscrição" tira da biblioteca (`removePlaylistObserve`).
  - Inscrito, o botão fica contornado em âmbar, com "Subscribed ✓", e o texto ao lado diz que tocar de novo cancela (etapa 6).
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
   - *Versão 0.5.2 (2026-11-02), a Explore pelos canais que os canais inscritos destacam, pedida pelo Johan depois de testar a 0.5.1, aguardando o teste no aparelho* (ver "Como testar a 0.5.2"). A leitura dos destaques e dos vídeos foi conferida contra o YouTube.
   - *Versão 0.5.1 (2026-11-02), ajustes pedidos pelo Johan depois de testar a 0.5.0; o Johan testou e aprovou tudo menos a Explore* (ver "Como testar a 0.5.1"): legendas centralizadas com contorno, toques no vídeo, Skip ahead pelos vales do gráfico com marcas na barra, girar a tela, 1080p no celular e no tablet, 3 colunas no tablet em pé, Histórico em linhas e a Explore automática.
   - *Versão 0.5.0 (2026-11-02), a aba Explore pedida pelo Johan* (ver "Como testar a 0.5.0"). A busca das categorias foi conferida contra o YouTube, sem login.
   - *Versão 0.4.2 (2026-11-02), pedidos do Johan depois de testar a 0.4.1, aguardando o teste no aparelho* (ver "Como testar a 0.4.2"): sem o @ do canal, "When done" só na TV, reordenar arrastando no celular, tirar vídeos do Histórico e das playlists, deslizar entre abas, as Configurações pelo avatar, o menu do OK longo abrindo antes de soltar (TV) e o **player novo (opção A)**. A aba Explore vem na versão seguinte.
   - *Versão 0.4.1 (2026-11-02), ajustes pedidos pelo Johan depois da 0.4.0, aguardando o teste no aparelho* (ver "Como testar a 0.4.1"): filtros nas Recomendações, duração de 30/45/60 min ou mais, "Before <ano>", salvar com resposta na hora, título no player, bloquear discreto, desempenho, playlists em sequência, o modo dormir no player (escurecer, quantos vídeos seguem e desligar a TV no fim) e a correção dos vídeos em 360p e lentos para começar na TV.
   - *Polimento visual implementado em 2026-11-02 (versão 0.4.0), aguardando o teste no aparelho* (ver "Como testar a etapa 6"). Junto, a pedido do Johan: salvar pelas listas (toque longo, e o menu do OK longo na TV), o marcador de salvo, o canal pelos cards, o estado de inscrito mais claro na playlist, puxar para atualizar (OK longo na aba, na TV) e o ícone vermelho. As Configurações só ganharam o visual novo.
   - Falta a rotina de atualização do motor.

Extensão do Firefox: deixa de ser necessária para o "Assistir mais tarde", porque o motor acessa o real. Fica como ideia opcional para limpar o site.

## Perguntas em aberto

Nenhuma no momento. Falta a rotina de atualização do motor (resto da etapa 6) e os testes no aparelho das etapas 3 a 6.

## Como testar a 0.5.2

1. Abrir a Explore pela primeira vez: alguns segundos com o indicador de carregar, e então vídeos de uns 10 canais que você não segue, um de cada canal por vez, recentes e antigos misturados.
2. Nenhum canal da lista é tradução ou segundo canal de um canal que você segue (ex.: "Veritasium em Português").
3. Puxar para baixo (OK longo na aba, na TV): a lista troca por outros canais. Repetir algumas vezes: os canais não se repetem logo.
4. Rolar até o fim: chegam mais canais.
5. Fechar e abrir o app: a Explore abre mais rápido que na primeira vez (os destaques ficam guardados).
6. O avatar e o nome do canal no cartão abrem a página do canal.

## Como testar a 0.5.1

1. Legenda (CC): centralizada, branca com contorno preto e sem fundo, também na automática.
2. Celular ou tablet: um toque mostra e esconde os controles, sem piscar num toque duplo. Dois toques na esquerda ou na direita voltam ou avançam 10 s, e mais toques no mesmo lado somam ("+30 s"). Dois toques no meio pausam.
3. Num vídeo com merchandising: os traços claros na barra marcam onde ele começa; "Skip ahead" logo antes de um traço pula para onde o vídeo volta. No meio de uma parte normal, ele avisa "Nothing to skip here."
4. Um vídeo largo abre deitado e um vertical em pé, sozinhos; o botão de girar troca. **No tablet** (Android 16 ignora a orientação em tela grande, então é o principal a conferir).
5. Celular em pé (ou o botão de girar): a ilha fica em duas linhas, com todos os botões.
6. Celular e tablet começam em 1080p (a etiqueta mostra "1080p"); a TV continua em 2160p.
7. Tablet em pé: 3 vídeos por linha.
8. History: linhas como nas playlists, e deslizar para a esquerda ainda tira o vídeo.
9. *(Até a 0.5.1; a 0.5.2 troca os temas pelos canais destacados.)* Explore: na primeira vez, "Looking for topics in your history…" e depois os temas. Segurar um tema > "Hide topic", e ele não volta ao puxar para atualizar.

## Como testar a 0.5.0

1. A aba Explore aparece entre Recommended e Playlists (no celular, na barra de baixo e no deslizar; na TV, a terceira aba).
2. *(Até a 0.5.0; a 0.5.1 troca as categorias à mão pelos temas do histórico.)* Celular: "New category" > nome, algumas palavras (vírgula ou "Done" viram chips), um filtro em "Opens with" > Save. O cartão ganha o mosaico em alguns segundos.
3. Abrir a categoria: vídeos das várias palavras misturados, com os filtros escolhidos em cima. Mudar um filtro busca de novo, e ele continua lá ao voltar à categoria.
4. Rolar até o fim carrega mais vídeos. Puxar para baixo atualiza.
5. O lápis na página muda o nome e as palavras. Segurar uma categoria no Explore > "Delete category" apaga.
6. Na TV: "New category" com o controle (nome e palavras separadas por vírgula), abrir, mudar os filtros, e o OK longo numa categoria para editar ou apagar.

## Como testar a 0.4.2

1. Listas e busca: nenhum "@canal" aparece (nos cards, no player e nos canais da busca).
2. Celular ou tablet: a lua no player não tem mais "When done". Na TV continua.
3. Celular: Watch later > ícone de setas > arrastar um vídeo pela alça (as três linhas à direita) ou segurar e arrastar. "Save order" e conferir no app do YouTube.
4. Histórico: deslizar um vídeo para a esquerda. Ele some com o aviso "Removed from history"; conferir no app do YouTube. Na TV: segurar OK no vídeo > "Remove from history".
5. Watch later (ou uma playlist sua): deslizar um vídeo para a esquerda tira ele da playlist. Numa playlist de outro canal, deslizar não faz nada. Na TV: OK longo > "Remove from this playlist".
6. Deslizar para o lado nas abas troca de aba; no Histórico, deslizar um vídeo para a direita volta para Playlists.
7. O avatar no canto de cima abre as Configurações, com o Sign out; "voltar" retorna à aba.
8. Player novo: título e canal em cima à esquerda, a pílula com like, dislike, Save e Subscribe à direita, e a ilha embaixo. Tocar no nome do canal abre a página dele. A lua e a etiqueta da resolução abrem os painéis à direita, acima da ilha.
9. Na TV: andar pelos botões com o controle (o foco é um anel âmbar), abrir a lua e a resolução e escolher com OK. Nada pode ficar cortado na borda da tela.
10. Na TV: segurar OK num vídeo. O menu aparece ainda com o botão apertado; ao soltar, o foco vai para a primeira opção, sem escolher nada.

## Como testar a 0.4.1

1. Recomendações: os chips (celular) ou botões (TV) no topo. "Duration: 1 hour or more" deve deixar só vídeos de uma hora ou mais; "Upload date: Before 2022", só vídeos com "5 years ago" ou mais; "Sort by: Most viewed" põe os mais vistos primeiro.
2. Busca: "Upload date: Before 2022" com um termo qualquer (só vídeos antigos) e "Duration: 45 min or more".
3. TV: cada botão de filtro abre a lista dos valores, com o atual em foco; "voltar" fecha sem mudar.
4. Segurar o dedo numa miniatura: o marcador e o aviso aparecem na hora. Segurar de novo logo em seguida tira (confira no app do YouTube depois de alguns segundos: deve ficar como o último toque).
5. Player: abrir vídeos de várias listas e por um link do WhatsApp: o título aparece sempre.
6. Canal: o bloquear é o ícone ⊘ pequeno na barra de cima (na TV, ao lado do nome; o texto aparece com o foco). Tocar mostra um aviso.
7. Desempenho: trocar de aba e voltar mantém a posição da lista; Recommended e History já estão carregadas na primeira visita; as miniaturas aparecem mais rápido ao rolar.
8. TV: um vídeo em 4K deve abrir em 2160p (o botão de resolução e a barra de cima mostram), ou descer sozinho para 1440p/1080p se a TV não der conta; nunca em 360p. Escolher outra resolução no botão troca na hora; "Auto" volta à escolha do app. A barra de cima mostra a resolução e o codec. Se continuar em 360p, os logs do app no adb (`adb logcat -s Doorglow`) dizem qual cliente do YouTube e quais faixas o aparelho aceita.
9. Sequência: abrir um vídeo pelo Watch later e pular para perto do fim. O próximo da lista começa sozinho, com o título certo. Um vídeo aberto pelo feed não continua.
10. Modo dormir (TV): a lua na barra de baixo abre o painel. "Brightness: 10%" deixa o vídeo bem escuro e os controles normais. "Then play: 1" toca só mais um vídeo e o número desce para None.
11. Desligar: escolher "When done: Turn off". Na primeira vez, o app abre Configurações > Acessibilidade: ligar o Doorglow. (Se o botão estiver cinza, pode ser o "acesso restrito" do Android 13 ou mais novo para apps instalados por fora da loja: Informações do app > ⋮ > "Permitir configurações restritas".) Depois, com "Then play: None", ir para o fim do vídeo: aparece o aviso, e em 10 s a TV desliga. Repetir apertando um botão durante o aviso: a TV fica ligada. Fechar e abrir o player: "Turn off" continua escolhido.

## Como testar a etapa 6

1. Atualizar pelo Obtainium para a 0.4.0.
2. Visual: fundo grafite, fonte nova, a fresta âmbar embaixo do ícone da aba ativa. As barras de vidro vão até as bordas da tela (atrás da barra de status e da de gestos), e o conteúdo rola por baixo delas. No Android 12+ o que passa por baixo fica desfocado.
3. Tablet: em pé, 3 colunas e a barra de baixo. Deitado, a coluna de ícones à esquerda (com a lupa) e 4 colunas. O recorte da câmera não deve empurrar nada.
4. Segurar o dedo numa miniatura: aparece o aviso "Saved to Watch later" e o marcador na miniatura. Segurar de novo tira (confira no app do YouTube).
5. Tocar no avatar ou no nome do canal embaixo de um card abre a página do canal.
6. Página de uma playlist de outro canal: inscrito, o botão mostra "Subscribed ✓"; tocar de novo cancela.
7. TV: o foco aumenta o card, com borda creme e brilho. Segurar o OK num vídeo e soltar abre o menu com "Save to Watch later" (ou "Remove from…") e "Go to channel". "Voltar" fecha o menu.
8. Atualizar: no celular, puxar a lista para baixo (aparece o círculo âmbar logo abaixo da barra de cima). Na TV, segurar OK na aba Recommended e soltar: a lista recarrega.
9. Ícone: em Settings > App icon, a sétima opção, "Red".

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
- **Visual (etapa 6):**
  - Os tokens de cor e a fonte ficam em `ui/Look.kt`; a fresta de luz e a porta, em `ui/Glyphs.kt`.
  - **Vidro:** Haze 1.7.2 (`dev.chrisbanes.haze`). As versões 1.7.3 e 2.x já pedem Compose 1.12 (e com ele AGP 9). O desfoque só funciona no Android 12+ (API 31); antes disso o Haze usa a cor de reserva, e o app escolhe uma quase opaca (`Look.GlassFallback`). Na TV o app não usa o Haze.
  - **Ponta a ponta:** `enableEdgeToEdge` com as barras do sistema transparentes, `isNavigationBarContrastEnforced = false` e o recorte da câmera em `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` (Android 11+) ou `SHORT_EDGES` (9 e 10). O app só usa os insets `statusBars` e `navigationBars`, nunca o do recorte. O espaço coberto pelas barras de vidro passa para as listas por um `CompositionLocal` (`LocalBars`), para elas rolarem por baixo.
  - **Fonte:** Figtree em TTF estáticos (regular, medium, semibold, bold) em `res/font/`, com a licença OFL em `licenses/Figtree-OFL.txt`.
  - **Puxar para atualizar:** o `Modifier.pullToRefresh` do Material 3 em volta da lista, dentro do `Loadable`; os ViewModels das listas implementam `Refreshable` (recarregar sem tirar a lista da tela, e sem "carregar mais" ao mesmo tempo).
  - **OK longo na TV:** o `Surface` do tv-material chama o `onLongClick` no primeiro repeat do ACTION_DOWN e engole o ACTION_UP seguinte. Se o menu abrisse ali, o soltar do OK cairia no primeiro item do menu. Por isso o card só marca o toque longo e abre o menu quando o OK é solto.
  - Fotos dos canais nos cards: vêm da lista de inscrições (`ChannelPhotos`), porque os vídeos dos feeds não trazem a foto do canal.
- **Desempenho (0.4.1):**
  - Os tópicos (excluídos e preferidos) eram normalizados de novo para cada vídeo, na thread da tela: com umas 150 recomendações, isso travava a rolagem a cada página. Agora as palavras-chave são normalizadas uma vez (`ContentFilters.excludedNeedles`), e as listas das Recomendações e da busca são montadas fora da thread da tela (`flowOn(Dispatchers.Default)`).
  - Miniaturas: o Coil já tinha cache na memória e no disco. O app agora adianta o download das miniaturas de cada página que chega para o cache de disco (`Thumbnails.prefetch`), decodificando pequeno.
  - Abas: um `SaveableStateHolder` guarda o estado de cada aba (a posição da lista), e os ViewModels de Recommended, Playlists e History são criados quando o feed das Inscrições chega.
- **Sequência e modo dormir (0.4.1):** a sequência é um objeto do app (`PlayQueue`), montado pela página da playlist ao abrir o vídeo e limpo por qualquer outro jeito de abrir. O mesmo player troca de vídeo no fim (`PlaybackController`, no `STATE_ENDED`). Escurecer usa o `overlayFrameLayout` do `PlayerView`, que fica entre o vídeo (com a legenda) e os controles. Desligar usa `AccessibilityService.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)`, que chama o `PowerManager.goToSleep` do sistema; na TV isso é o mesmo que o botão de desligar do controle.
- **Compilar:** `./gradlew :app:assembleAppRelease` gera `app/build/outputs/apk/app/release/app-app-release.apk`, assinado com a chave de debug (uso pessoal). Os submódulos precisam estar baixados: `git submodule update --init`.
