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
- **Fora do app:** envio de vídeos (upload), Shorts, comentários, chat ao vivo e notificações. A única notificação é a do controle de mídia, que o Android exige para o vídeo continuar com a tela desligada (ver "Tela desligada", no Player).
- **Nome e ícone:** **Doorglow**. Ícone: a porta entreaberta (fresta de luz) em fundo azul-noite. `applicationId` = `app.doorglow`. Nome e ID ficam no `gradle.properties`.
  - **Cor do ícone à escolha** (decidido em 2026-11-01): o desenho é sempre a porta original, e nas Configurações ("App icon") dá para escolher uma de sete cores: azul-noite (padrão), preto, verde-azulado, ameixa, mostarda, gradiente vivo e vermelho. São as versões 01 a 07 de `/mnt/project-files/nome-e-icone/versoes/`. O vermelho (luz branca em #E53935, no tom do YouTube) entrou em 2026-11-02, a pedido do Johan.
  - Ícones, banners da TV e cores saem de `tools/make_icons.py` (não editar os XMLs gerados à mão). A tabela de cores fica no script.
  - **Desenho refinado na 0.5.4** (pedido do Johan, 2026-11-03: "mesmo conceito, executado melhor"): a fresta quase reta, como a de uma porta, com um miolo mais claro; o brilho em volta em degradê (no lugar das camadas chapadas); e a luz no chão sumindo aos poucos, dentro da área que os launchers mostram (antes ela passava da borda e aparecia cortada). O **banner da TV virou vetor**, com o nome desenhado pelas curvas da Figtree (a fonte do app): fica nítido em qualquer tamanho (o PNG de 640x360 aparecia borrado). O desenho da porta no app (`DoorGlyph`, nas abas e no login) segue o ícone.
- **Interface em inglês.** (A conversa com o Claude continua em português.)
- **Interações com a conta:** só **like/dislike** e **inscrever/cancelar inscrição**. O botão de inscrição fica na página do vídeo e vale para o canal do vídeo.
- **Inscrição em playlists**, guardada nas playlists salvas da conta (o YouTube não tem isso; ver a seção própria).
- **Links externos** (WhatsApp, navegador, "Compartilhar"): abrem no Doorglow e tocam **sem ir para o histórico** (ver a seção própria).
- **Atualizações pelo Obtainium**, a partir das releases do GitHub (ver a seção própria).
- **Botão YouTube do controle abre o Doorglow** (pedido do Johan, 2026-11-04), pelo serviço de Acessibilidade que o app já tem na TV, e não pelo Button Mapper (escolha dele). Ver a seção própria.
- **Aparência (etapa 6, aprovada pelo Johan em 2026-11-02, a partir do canvas de mockups):**
  - Fundo grafite (#18191B), não preto nem azul; textos creme; fonte **Figtree**. A luz âmbar da porta aparece só no que importa: a aba ativa (uma "fresta" de luz), a barra de progresso, o foco na TV e o marcador de salvo.
  - **Cor de destaque escolhida em Settings** (2026-11-03, pedido do Johan: "da mesma maneira que dá para selecionar a cor do ícone"): as mesmas do ícone e na mesma ordem (pedido do Johan, 2026-11-03: "mais relacionadas com as cores do ícone, incluindo o vermelho YouTube"; onde o ícone é escuro demais, um tom próximo com contraste): Amber (a luz do ícone azul-noite, o padrão), Silver (par do preto), Teal, Plum e Mustard (o fundo do ícone clareado), Vivid (o meio do degradê) e Red (o vermelho do YouTube, #FF0000), no grupo "Highlight color" logo depois do ícone (no celular, bolinhas lado a lado com um anel claro na escolhida e o nome embaixo; na TV, uma linha por cor). Vale para tudo que era âmbar: a aba ativa, o progresso, os filtros mudados, o foco da TV, o marcador de salvo e, no player, a barra, os botões ligados e o foco (`ui/Accent.kt`, `values/accents.xml`). Fica guardada no aparelho; o ícone do app continua com a cor dele.
  - **Sem brilho nos indicadores** (2026-11-03, pedido do Johan): a fresta da aba ativa é só uma barrinha, e o foco dos cards na TV é só o crescer e o contorno claro. O brilho ficou só na porta do canto (`DoorGlyph`).
  - **Barras de vidro de ponta a ponta** no celular e no tablet: a barra de cima e a de baixo passam por trás da barra de status, da barra de gestos e das laterais, e o conteúdo rola por baixo delas. O desfoque só existe no Android 12 ou mais novo; nos mais antigos a barra fica quase opaca.
  - O vidro é um **degradê** (pedido do Johan, 2026-11-03): mais opaco na borda da tela (94%) e menos junto do conteúdo (76%); antes era 62% por igual. Vale também para a pílula e a ilha do player (mais opacas na borda de cima e na de baixo, respectivamente) e para o trilho na tela deitada (mais opaco à esquerda).
  - **O recorte da câmera é ignorado:** o conteúdo passa por baixo dele, e só as barras do sistema afastam os botões.
  - Barra de cima compacta (o nome da aba e a lupa). A barra de baixo tem **só ícones**, sem nomes.
  - **Deitado** (largura maior que a altura e pelo menos 600 dp), a barra de baixo vira uma coluna de ícones à esquerda, que inclui a Busca. O tablet em pé usa o mesmo layout do celular, com 3 colunas (até a 0.5.0 ele mostrava 2). A grade conta pela largura e pela posição: 1 coluna abaixo de 560 dp; **em pé, sempre 3** (0.5.4: o tablet de 14,6" do Johan passa de 960 dp em pé e mostrava 4); deitado, 3 abaixo de 960 dp e, acima disso, quantas couberem com 240 dp, no mínimo 4.
  - O desfoque do vidro é calculado numa cópia menor do que passa por baixo e ampliado (`HazeInputScale.Auto`, 2026-11-03, sugestão da thread de desempenho): com 24 dp de desfoque a diferença não aparece, e rolar fica mais leve.
  - Na TV não tem desfoque: o menu e os avisos usam um cinza quase opaco. O foco aumenta o card em 6%, com borda clara e sem brilho (o brilho âmbar saiu em 2026-11-03, a pedido do Johan; também pesava, porque a sombra borrada é redesenhada a cada troca de foco).
  - **Textos dos cards da TV** (0.6.3, pedido do Johan: as linhas do título longe demais uma da outra, e tão longe quanto do canal; e títulos cortados): o título em 13 sp com linhas de 17 sp (era o `titleSmall`, 14 sp em linhas de 20 sp), e 5 dp entre ele e o canal (eram 2). O canal e a linha de baixo em 12 sp com linhas de 15 sp. Cabe mais do título, e ele fica separado do resto.
  - **Carregando:** enquanto a primeira página de Subscriptions, Recommended, Explore ou da busca chega, a tela mostra **cartões vazios** no lugar onde os vídeos vão entrar (no celular, no lugar do círculo; na TV, no lugar do "Loading…"), parados, sem brilho correndo (2026-11-03).
  - **Filtros da TV** (Recommended e Search) mais leves (2026-11-03): sem contorno, num cinza um pouco mais claro que o fundo; o filtro mudado fica âmbar, e o foco, creme como os outros botões.

## Telas

| Tela | Notas |
|---|---|
| **Início = Inscrições** | Feed das inscrições como tela principal. |
| Recomendações | Secundária, com exclusão e preferência de tópicos (ver Filtragem). Os filtros de data, duração e ordem saíram daqui para a Explore (pedido do Johan, 2026-11-03). |
| Busca | "Melhorada": filtros de duração, data, tipo e ordenação, e sugestões enquanto se digita. Resultados misturam vídeos, canais e playlists. Lives só aparecem aqui. |
| Playlists | Inclui **Assistir mais tarde** (o real, que o motor acessa). Reordenar e tirar vídeos, inclusive pela TV. |
| Histórico | Lista do que foi assistido, **no formato das playlists** (miniatura à esquerda e o título ao lado; 0.5.1, pedido do Johan). Tirar vídeos dela (0.4.2). |
| Explore | **Vídeos dos canais que os canais inscritos destacam**, recentes e antigos, num sorteio diferente a cada atualização (0.5.2; na 0.5.0 eram categorias feitas à mão e na 0.5.1 temas do histórico; ver "Explore"). Aba a mais: **Recommended continua**. |
| Canal | Página básica do canal: vídeos e playlists (duas abas), **Subscribe/Subscribed** (0.6.0, pedido do Johan: o mesmo botão da playlist, em cima da lista no celular e ao lado do nome na TV; aparece quando a lista de canais inscritos da conta diz se está inscrito) e o bloquear, que é só um ícone pequeno (na barra de cima no celular, ao lado do nome na TV). Abre pela busca, pelo nome do canal no player, pelo avatar e nome do canal nos cards (celular e tablet) e pelo menu do OK longo (TV). **Sempre**, também nos vídeos que vêm sem o canal (as Recomendações): ele é procurado na página do vídeo na hora (0.6.3, pedido do Johan: o "Go to channel" não aparecia nas Recomendações da TV). |
| Configurações | Mínimas: **mostrar lives nas Inscrições**, playlist do botão Save, tópicos excluídos e preferidos, canais bloqueados, links externos, playlists inscritas, **cor do ícone**, **cor de destaque** e conta (com o Sign out). No celular e no tablet (2026-11-03, refinamento do visual), cada assunto fica num **cartão arredondado** com o nome em cima e a explicação embaixo, como nos ajustes do Android: interruptores no lugar das caixas de marcar, "+ Add topic" em âmbar, e as sete cores do ícone em círculos maiores, com um anel âmbar na escolhida e o nome dela embaixo. A coluna fica centralizada até 640 dp no tablet. Na TV, os mesmos cartões, com o foco creme em cada linha. |

- **Abas:** no celular e no tablet (0.4.2, pedido do Johan), a barra de baixo tem os ícones de Subscriptions, Recommended, Explore (0.5.0), Playlists e History; a Busca abre pela lupa e as **Configurações pelo avatar da conta**, os dois na barra de cima (como nos apps do Google). "Voltar" na Busca e nas Configurações leva à aba de onde vieram. Deitado, a coluna da esquerda tem as abas e a Busca, e o avatar fica na barra de cima. Na TV, as sete abas ficam na fileira de cima, com Explore em terceiro e Search em quarto; "voltar" no conteúdo leva às abas, e de novo sai do app. **Subir do conteúdo até as abas para na aba aberta** (0.6.0, achado do Johan): antes o foco caía na aba mais perto do card focado, e como a aba muda com o foco, a página trocava (das Inscrições ia para Settings). Vale também para Vídeos e Playlists na página do canal.
- **Deslizar para o lado** troca de aba no celular e no tablet (0.4.2, pedido do Johan), na ordem da barra de baixo. Busca e Configurações ficam fora do deslizar.
- As páginas de playlist e de canal não têm a barra de abas: só a barra de cima, com "voltar".
- **Trocar de aba** não perde a posição da lista, e Recommended, Playlists e History carregam logo depois das Inscrições, ao abrir o app, para já estarem prontas (0.4.1).
- **Atualizar as listas** (pedido do Johan, 2026-11-02): no celular e no tablet, **puxar para baixo** no topo da lista recarrega Subscriptions, Recommended, Playlists, History, a página da playlist e a do canal. A lista continua na tela até a nova chegar. Na TV, **segurar OK na aba** Subscriptions, Recommended, Playlists ou History recarrega a lista (quando o OK é solto). Nada recarrega sozinho: as abas carregam quando o app abre e depois só quando o usuário pede.
  - **Sem cartões voando** (0.5.4, pedido do Johan): numa lista nova (puxar para atualizar, ou as Inscrições guardadas trocadas pelas novas), os cartões só aparecem no lugar. Eles deslizam só quando vídeos saem da lista (tirar do Histórico ou da playlist, bloquear um canal) ou chegam no fim (`ListMotion`).
  - **Vídeos novos aparecem em cima** (0.6.3): com a grade no começo, a lista nova continua no começo, com os vídeos que chegaram (`ListTop`). Até a 0.6.2 a grade segurava o primeiro vídeo de antes no topo e escondia os novos acima dele: ao abrir o app (as Inscrições guardadas trocadas pelas da conta) e no puxar para atualizar. Na TV, o foco que estava na grade vai para o primeiro vídeo. Rolada para baixo, a grade fica no mesmo vídeo.
- **As Inscrições se atualizam sozinhas** (0.6.3, pedido do Johan: na TV só atualizava fechando e abrindo o app de novo): quando o app volta depois de **10 minutos ou mais fora da tela** (TV desligada, Home, outro app; o player aberto por cima não conta), a lista se atualiza e volta ao começo, com o foco no primeiro vídeo na TV. O que estava na tela fica até a lista nova chegar; sem rede (a TV recém-ligada ainda achando o Wi-Fi), tenta de novo depois de 2, 4, 8 e 16 s. Vale na TV, no celular e no tablet, junto com o puxar para atualizar e o OK longo na aba.
- **Inscrições guardadas no aparelho** (0.5.3, pedido do Johan, 2026-11-02): o app abre **na hora** com a primeira página das Inscrições (e os vídeos das playlists inscritas) da última vez, e atualiza por trás, com o indicador de atualizar no topo. O feed guardado vale por até 3 dias; sem rede, ele fica na tela. Sair da conta apaga (`FeedCache`, arquivo `feed.json`).
- **Cards de vídeo:** a miniatura com a duração e o progresso; embaixo, o título, o avatar e o nome do canal (abrem o canal) e o resto da linha (visualizações e data). Canal sem foto (em que o usuário não é inscrito) mostra a inicial num círculo colorido.
  - **Canal e visualizações sem corte duplo** (2026-11-03, refinamento do visual): o canal e o resto ficam na mesma linha só quando cabem inteiros; senão, o resto desce para a linha de baixo, em vez de os dois saírem cortados ("Computerp… · 268K views · 9 …"). Nas grades de várias colunas, nas linhas (Histórico, playlists) e na TV, ficam sempre em duas linhas, para os cartões lado a lado ficarem iguais (`MetaLine`).
- **Sem o @ do canal** (0.4.2, pedido do Johan): o YouTube manda o @ junto com o nome nos cartões da TV ("Veritasium • @veritasium") e na linha dos canais da busca; o app tira o @ das listas, do player e da busca (`Handles`).
- **Menu do OK longo na TV** (0.4.2, pedido do Johan): o menu aparece assim que o toque longo é reconhecido, com o OK ainda apertado. Ele só pega o foco quando o OK é solto, para o "soltar" não escolher a primeira opção sozinho.
  - **Quanto segurar** (pedido do Johan, 2026-11-03): o OK longo é medido pelo app e vale a partir de **0,3 s**, nos cards e nas abas (`OkHold`). Antes era o do Compose for TV, que espera a primeira repetição da tecla do sistema (0,4 s a 0,5 s, ou mais em algumas TVs).
- **Tirar vídeos das listas** (0.4.2, pedido do Johan): no Histórico e nas playlists do próprio usuário (inclui Watch later), **deslizar o vídeo para a esquerda** tira ele da lista, com um aviso. Só para a esquerda, para não brigar com o deslizar entre abas (o Histórico é a última aba). Na TV, é uma opção no menu do OK longo ("Remove from history" ou "Remove from this playlist"). O vídeo some na hora; se a conta recusar, aparece "Couldn't…" e a lista recarrega.
  - **Desfazer** (0.5.5, pedido do Johan): no celular e no tablet, o aviso ganha um botão **"Undo"** e fica 5 segundos na tela. A conta só muda quando o prazo passa (`PendingEdit`), então desfazer devolve o vídeo exatamente ao lugar de antes, sem pedido nenhum ao YouTube (o histórico nem teria como receber o vídeo de volta). Sair da tela não cancela a mudança. Na TV, sem Undo (o aviso não pega o foco); a conta muda depois do mesmo prazo.
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
- **Filtros na Explore** (nas Recomendações desde a 0.4.1, pedido do Johan em 2026-11-02; levados para a Explore a pedido dele em 2026-11-03, porque as Recomendações vêm poucas por vez e a Explore traz vídeos antigos e variados): data, duração e ordem (relevância, mais novos, mais vistos), num chip no topo da lista (celular) ou numa fileira de botões (TV). O YouTube não filtra essas listas, então o app aplica os filtros em cada vídeo (`FeedFilter`): a duração, o tempo relativo ("3 days ago") e as visualizações. O "Before <ano>" só mostra o que com certeza é anterior: "4 years ago" pode ser de dois anos diferentes e fica de fora. Os filtros valem até o app fechar. Como funcionam na Explore: ver "Explore".
- **Bloquear canal**, pelo botão na página do canal, com a lista para desbloquear nas Configurações. Canal bloqueado some das Recomendações, da Busca, das Inscrições e dos vídeos de playlists inscritas. Quando o YouTube não manda o ID do canal (acontece na busca), o app compara pelo nome.

## Player

- **Carregando sempre à vista** (0.6.4, pedido do Johan, 2026-11-13: "às vezes a tela fica só preta, sem nenhuma indicação que o vídeo está realmente carregando"): desde o instante em que o player abre, a tela mostra a **miniatura do vídeo, escurecida, e um círculo girando** na cor de destaque (`PlayerLoading`). A miniatura some no primeiro quadro do vídeo.
  - A causa da tela preta: o círculo era o do Media3, que só aparece quando o player começa a baixar o vídeo. Antes disso o app pede o vídeo ao YouTube (de um a vários segundos, mais no primeiro vídeo), e nesse tempo a tela ficava preta, sem sinal nenhum.
  - No meio do vídeo (a rede atrasou, ou um pulo na barra para um trecho ainda não baixado), só o círculo, sobre um disco escuro para aparecer em cima de imagem clara. Ele espera 0,4 s: um pulo para um trecho já carregado não faz o círculo piscar.
  - O próximo vídeo de uma playlist começa com a miniatura dele. Com erro na tela, nem miniatura nem círculo. Num link de outro app (sem card), a miniatura é a padrão do vídeo. Fica por baixo dos controles e do escurecer do modo dormir.
- **Começar mais rápido** (0.6.4, mesmo pedido: "ainda é mais lento que o aplicativo próprio do YouTube"). O que foi achado lendo o motor, e o que mudou (os detalhes estão em "Desempenho (0.6.4)", nas notas técnicas):
  - **O primeiro vídeo depois de abrir o app** esperava três coisas que o motor só preparava no primeiro play: um token que o YouTube exige (gerado num WebView, com três pedidos de rede), o player do YouTube (2 MB de JavaScript, baixado a cada abertura do app) e o token da conta. Agora isso roda por trás assim que as listas aparecem, e de novo a cada volta ao app (`EngineWarmup`).
  - **Na TV, o vídeo é pedido antes do OK**: com o foco parado 0,6 s num card, o app já pede aquele vídeo ao YouTube. Quem para num card costuma apertar OK um ou dois segundos depois, e a resposta já está lá (ou quase). Um palpite de cada vez; passar pelos cards sem parar não pede nada; os últimos 3 ficam guardados por 10 minutos.
  - **A legenda não segura mais o começo**: em todo vídeo com legenda (mesmo desligada), o player esperava a resposta do pedido da legenda antes de começar a baixar o vídeo. Agora o vídeo começa e a legenda chega junto.
  - Não mudam: resolução, codec, a regra do 4K e o que o app pede ao YouTube para cada vídeo tocado.
  - **Falta medir nos aparelhos.** As TVs e o celular não responderam ao ADB no dia; os tempos acima vêm da leitura do código. O log da tag `Doorglow` agora separa cada etapa (ver "Como testar a 0.6.4").

- **Tela desligada** (pedido do Johan, 2026-11-11: "que continue tocando quando eu desligo"; celular e tablet): desligar a tela no meio do vídeo não pausa mais. O som continua, e a sequência da playlist segue para os próximos vídeos, com os limites do modo dormir. Pausado, continua pausado.
  - Sem a tela, o app **desliga a imagem**: o vídeo não é baixado nem decodificado (poupa bateria e dados), só o áudio. Ao voltar, a imagem volta no mesmo ponto e na mesma regra de qualidade, depois de carregar um instante.
  - Sair para outro app (Home, trocar de app) com o vídeo tocando abre o **mini player flutuante** (abaixo); sem ele, pausa, como antes. Na TV, o player fecha (abaixo).
  - Para isso o Android exige um serviço de mídia em primeiro plano, que mostra o **controle de mídia** na notificação e na tela de bloqueio enquanto um vídeo do celular ou do tablet está aberto: a miniatura, o título, o canal, tocar/pausar e a barra de tempo (os do Android, `PlaybackService`). Tocar na notificação volta para o player. Os botões do fone e do Bluetooth também tocam e pausam. Fechar o player tira a notificação.
- **Mini player flutuante** (pedido do Johan, 2026-11-11: "mini player flutuante quando tiver em outro aplicativo"; celular e tablet, Android 8 em diante): sair do app (Home, trocar de app, os Recentes) com o vídeo tocando deixa o vídeo numa **janelinha por cima dos outros apps**, que o Android deixa arrastar, aumentar e esconder na borda. Pausado, no fim ou com erro, sair pausa, como antes; voltar (fechar o player) ou abrir o canal do vídeo não abre a janelinha.
  - Na janelinha, só o vídeo e a legenda, sem os controles do app. Os botões são os do Android (tocar/pausar, pela mesma sessão de mídia da notificação, voltar ao player e fechar). Tocar nela, ou na notificação, volta para o player inteiro, no mesmo ponto.
  - **Fechar a janelinha** (o X, ou arrastar para baixo) fecha o vídeo, e a notificação some. Desligar a tela com a janelinha aberta segue a regra da tela desligada (o som continua).
  - Abrir outro vídeo (no app ou por um link) fecha a janelinha do anterior: os dois não tocam juntos.
  - Se o Johan desligar o picture-in-picture do Doorglow nos ajustes do Android, sair volta a pausar.
- **Na TV, desligar a TV ou sair do app fecha o player** (0.6.3, pedido do Johan: ao religar a TV, o app voltava numa tela preta tentando carregar o vídeo de antes). Ao voltar, o app está na lista de onde o vídeo foi aberto, e as Inscrições se atualizam. O ponto do vídeo fica guardado (volta de onde parou, abaixo). Só o "When done: Turn off" abrindo os ajustes de Acessibilidade deixa o player aberto (a pessoa volta para ele). No celular e no tablet, a tela desligada e o mini player (acima).
- **Há quanto tempo o vídeo foi publicado** (0.6.3, pedido do Johan: "uma informação pequenininha, discreta"): na linha do canal, embaixo do título, como nos cards ("Veritasium · 3 days ago"), em cinza. Vem do card que abriu o vídeo (ou da sequência); num link de outro app, da data da página do vídeo ("today", "3 days ago", "2 weeks ago"…, `Published`). Nas lives, não aparece. Vale também no celular e no tablet (é a mesma linha).
- **Volta de onde parou** (0.6.0, achado do Johan na TV: recomeçava do início): o vídeo começa onde parou da última vez. O aparelho guarda onde cada vídeo parou (os 300 mais recentes, `WatchPositions`, arquivo `positions.json`, apagado ao sair da conta); sem isso, vale o tempo que o card do YouTube traz (visto em outro aparelho). Menos de 10 s vistos, ou menos de 30 s para o fim, começa do início (`ResumeRules`). Lives e links com tempo (`?t=`) começam como antes. Vale também na sequência de uma playlist.
- **Títulos sempre no idioma original**, sem as traduções automáticas do YouTube.
  - O cliente de TV do YouTube (o primeiro que o motor usa) manda o vídeo **sem título e sem autor**. Até a 0.4.0 o player trocava o título do card por esse vazio. Agora o título vem do stream, se vier; senão do card que abriu o vídeo; e, nos links de outros apps, da página do vídeo (`getMetadata`).
- **Áudio original**: escolher sempre a faixa marcada como original, nunca a dublagem automática.
- **Controles com bem menos elementos.** Manter apenas:
  - **Pular as partes menos interessantes** (sem botão desde 2026-11-03; até então, o botão "Skip ahead"). O Johan queria o "Jump ahead" do YouTube Premium, que pula a parte de merchandising; esse dado não chega a apps de fora (a página "next" sem login só traz o gráfico de "mais repetidos", conferido em 2026-11-02), e ele recusou o SponsorBlock por ser de terceiros. Então o app usa o gráfico do próprio YouTube, pedido à página "next" do cliente WEB, sem login (`HeatmapRepository`), e procura **vales**: um trecho em que a audiência cai abaixo de 30% da mediana do vídeo por 15 s a 5 min (`SkipRules`, 0.5.1).
    - **O avanço é o pulo** (2026-11-03, pedido do Johan, como o app do YouTube faz hoje: "um botão dedicado para isso não é o ideal"): avançar dentro de um vale, ou até 20 s antes dele, vai direto para o ponto em que a audiência volta à mediana, em vez de andar 10 s, e um aviso rápido "Jumped ahead" aparece do lado direito. Vale para os dois toques do lado direito (celular e tablet), para a direita na barra de tempo e para a tecla de avançar do controle (TV). Fora dos vales, o avanço é o normal; o próximo avanço depois do pulo também. Menos de 5 s até o fim do vale não pula (`SkipRules.valleyEnd`, `PlayerActivity.jumpAhead`).
    - Até a 0.5.6 era um botão "Skip ahead", que pulava do começo do vale (até 20 s à frente) para o fim dele, ia para o próximo pico em até 60 s quando não havia vale, ou avisava "Nothing to skip here.". Agora, no meio do vale também pula (o botão só pulava a partir do começo dele).
    - Calibrado com 25 vídeos com patrocínio (o gráfico do YouTube comparado com os trechos que o SponsorBlock marca, só para medir): acerta 33 dos 45 merchandisings, e no meio de conteúdo normal pula em 20% dos toques (a regra da 0.5.0, que ia para o próximo pico, pulava em 91%).
    - **Marcas na barra** (0.5.1, pedido do Johan, como as do app do YouTube): onde começa cada vale, um traço claro na barra de tempo.
    - Sem gráfico (vídeos com poucas visualizações), o avanço é sempre o normal (o botão ia para o próximo capítulo).
  - **Ligar e desligar a legenda** com um toque. O botão liga uma legenda escolhida sozinha (`SubtitleRules`): feita por pessoa no idioma do aparelho, depois feita por pessoa no idioma original, depois qualquer uma feita por pessoa, e por último a automática no idioma original. A escolha (ligada/desligada) vale para os próximos vídeos. Se o download da legenda falhar, o vídeo toca sem ela.
    - **Visual da legenda** (0.5.1, pedido do Johan): sempre centralizada, texto branco com um contorno preto fino e sem fundo, na fonte do app (`CaptionRules`). A posição e o fundo que vêm do YouTube (a automática vem alinhada à esquerda) são ignorados; só a altura da linha é mantida.
    - **Legenda acima da barra** (0.5.8, pedido do Johan): com os controles abertos, a legenda sobe só o que ficaria atrás da barra de baixo (e do nome do capítulo) e desce quando eles somem (`CaptionLift`). Com os controles fechados, ela fica um pouco acima da borda de baixo, só para não encostar: 8 dp no celular e 16 dp na TV (`player_captions_bottom`), e a mesma distância acima da barra quando sobe.
    - **Legenda em blocos** (0.6.2, pedido do Johan: "aparecem duas linhas a cada vez, ou uma se só tiver conteúdo para uma, e troca ela de vez"): a automática do YouTube rola, cada linha nova entra embaixo e empurra a de cima. O app lê a legenda inteira antes e, quando ela rola, mostra **duas linhas de uma vez**, trocadas inteiras pelas duas seguintes (`CaptionBlocks`). A segunda linha aparece junto com a primeira, um pouco antes de ser falada. Fica sozinha a linha cuja seguinte começa mais de 6 s depois, ou depois de uma pausa sem legenda. A legenda feita por pessoa já vem em blocos e passa como veio.
      - Na 0.5.8 a legenda **sumia na TV com os controles fechados** (achado do Johan). O vídeo fica numa camada por baixo da janela, e é no layout que o Android anota que partes da janela por cima dele têm algo desenhado; o resto ele nem mostra. A subida e a descida são só um deslocamento, sem layout, então a anotação ficava a do meio da descida, com a faixa de baixo vazia. Na 0.6.0, a tela passa por um layout quando a legenda para.
  - **Capítulos na barra** (2026-11-03, pedido do Johan): onde cada capítulo começa, uma **fresta** na barra de tempo, como no YouTube (`ChapterTimeBar`), e o **nome do capítulo atual** logo acima da ilha, à esquerda. Os capítulos vêm da mesma página "next" do cliente WEB, sem login, que já traz o gráfico de "mais repetidos" (`HeatmapRepository`); com um capítulo só, ou nenhum, nada aparece. (Antes, os capítulos vinham da página do vídeo pelo motor, só com login, e só serviam para o botão Skip ahead.)
  - **Preview ao arrastar a barra** (2026-11-03, pedido do Johan): com o dedo, ou com esquerda e direita na barra da TV, um cartão de vidro acima da ilha segue o círculo da barra e mostra a **miniatura daquele ponto**, o nome do capítulo e o tempo. As miniaturas são o storyboard do YouTube (folhas de 5 x 5 quadros que o motor lê junto com os formatos do vídeo, `Storyboard`); o app baixa só a folha do ponto e a seguinte, e guarda as 4 últimas. Sem storyboard (lives, ou um cliente do YouTube que não mande), o cartão mostra só o capítulo e o tempo. Não deu para conferir fora do aparelho: o YouTube pede login ao contêiner onde o Claude roda.
  - **Lives: botão LIVE** (0.5.8, pedido do Johan): numa live o tempo e a duração ficavam parados (o trecho que dá para voltar anda junto com o vídeo), então dão lugar a um botão "● LIVE". O ponto fica vermelho (o do YouTube) no ao vivo e cinza quando o vídeo ficou mais de 10 s para trás (pausa ou volta na barra); tocar nele, ou OK na TV, volta para o ao vivo (`seekToDefaultPosition`). No celular deitado ele fica no fim da barra; com duas linhas (celular em pé e TV), logo depois do tocar, onde o foco do controle chega.
  - Além disso: tocar/pausar e a barra de tempo. Em cima ficam like, dislike, Save e "Subscribe" (vale para o canal do vídeo); só aparecem com login.
- **Visual do player: opção A** (0.4.2; o Johan pediu um player "mais elegante, mais compacto, mais moderno" e escolheu a A no canvas, entre três opções):
  - Em cima, à esquerda: o título e, embaixo dele, o nome do canal (um toque, ou OK na TV, abre a página do canal) e o aviso "not saved to history" quando for o caso. À direita, uma **pílula de vidro** com like, dislike, Save (âmbar quando salvo) e "Subscribe" (cheio e claro; inscrito, só o contorno).
  - Embaixo, uma **ilha de vidro** arredondada: tocar/pausar num círculo claro, o tempo, a barra (âmbar, com o carregado mais claro), a duração e, depois de um divisor, legenda, a etiqueta da resolução ("2160p", com contorno), girar a tela (só celular e tablet) e a lua.
    - Numa tela com menos de 600 dp de largura (o celular em pé), a ilha vira **duas linhas** (0.5.1): o tempo e a barra em cima, e embaixo o tocar/pausar e os botões (`PlayerIsland`). Numa linha só, os botões não cabiam. Até a 0.5.6 o limite era 720 dp; sem o Skip ahead, o celular pequeno deitado (640 dp) cabe numa linha só.
    - **Na TV, sempre duas linhas** (0.5.6, achado pelo Johan com o controle remoto): na barra, esquerda e direita avançam o vídeo, então numa linha só não dava para chegar nos botões depois dela sem arrastar o vídeo até o fim (e aí pulava para o próximo). Com a barra em cima, **baixo** leva aos botões (legenda, resolução, lua) e **cima** volta para a barra.
    - **Descer da última linha, ou subir da primeira, esconde os controles na hora** (0.6.2, pedido do Johan), sem esperar os 4 s. Segurar a seta não os acende de novo (a próxima seta acende), e a seta que acendeu os controles, se continuar apertada, não os apaga. Com o painel da resolução ou do modo dormir aberto, a seta fica com ele.
    - **Esquerda e direita na barra andam 10 s** (0.6.0, pedido do Johan), como os dois toques no celular; segurar anda mais rápido. Antes era o padrão do Media3, 1/20 do vídeo por toque (minutos num vídeo longo). Dentro de um vale, o avanço continua sendo o pulo.
    - **Barra de borda a borda** (2026-11-03, pedido do Johan no antes e depois): a ilha deixou de ser um cartão arredondado longe das bordas e virou uma **barra de vidro que preenche toda a parte de baixo**, até as laterais e a borda de baixo, sem cantos (o mesmo degradê, 76% em cima e 94% embaixo). O conteúdo fica a 14 dp das laterais no celular e a 48 dp na TV (`player_bar_side`), com a barra de tempo mais comprida.
    - **Na TV, like, dislike, Save e Subscribe descem para a barra de baixo** (mesmo pedido: "agora que tem um espação"), soltos logo depois do tocar/pausar; legenda, resolução e lua continuam à direita. Em cima fica só o título e o canal. No celular e no tablet a pílula continua em cima.
  - **Controles compactos** (2026-11-03, pedido do Johan: "compactar um pouco mais os controles para liberar mais espaço para o vídeo"): margens de 20 dp (eram 32), o título em 15 sp numa linha (era 17 sp em duas), a pílula de cima e o Subscribe mais baixos, a ilha de 48 dp (era 56) com botões de 36 dp (eram 40) e as sombras de cima e de baixo mais curtas. Na TV ficam um pouco maiores, para o foco aparecer bem: título em 18 sp e até duas linhas, ilha de 52 dp e botões de 40 dp (`values/player.xml` e `values-television/player.xml`).
  - Os painéis da resolução e do modo dormir abrem num cartão **à direita, logo acima da ilha**, com as opções em **trilhos segmentados** (a escolhida fica âmbar e cheia). Os trilhos de um painel ficam da mesma largura.
  - Na TV: o foco é um anel âmbar em todos os botões (o tocar/pausar fica âmbar inteiro), e os controles ficam a 48 dp das bordas, dentro da área segura da tela (`values-television`). O título cresce um pouco.
  - Saiu da tela a linha técnica com o codec e o idioma do áudio ("1080p avc1 · audio: en"): a resolução já aparece na etiqueta, e o resto continua no log (tag Doorglow).
- **Toques no vídeo** (celular e tablet, 0.5.1, pedido do Johan, como no app do YouTube): **um toque** mostra ou esconde os controles; **dois toques** no terço da esquerda ou da direita voltam ou avançam 10 s, e no meio pausam ou continuam. Depois de um pulo, cada toque no mesmo lado (por 0,8 s) pula mais 10 s, e um aviso do lado do toque mostra o total ("+30 s"). O toque único espera um instante para ter certeza de que não vem o segundo, então os controles não piscam num toque duplo (`TouchRules`). Avançar dentro de um trecho que o público pula vai para o fim dele (ver "Pular as partes menos interessantes"); um toque a mais logo depois volta a contar "+10 s" a partir dali.
- **Girar a tela** (celular e tablet, 0.5.1, pedido do Johan, que queria as duas coisas): o player **gira sozinho** conforme o vídeo (deitado para vídeos largos, em pé para os verticais, seguindo o sensor), e o **botão de girar** na ilha troca entre deitado e em pé; a escolha vale até fechar o player.
  - O Android 16 ignora a orientação pedida pelos apps em telas grandes (tablets). O app declara a exceção que o Android 16 ainda aceita (`PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` no manifest), que deixa de valer quando o app mirar o SDK 37. Não foi testado num tablet.
- **Qualidade sempre na máxima.** Uma mudança manual vale só para o vídeo atual; o próximo vídeo volta para a máxima.
  - **No celular e no tablet o topo é 1080p** (0.5.1, pedido do Johan): o vídeo começa em 1080p e pode cair para 720p. O 4K primeiro é só da TV.
  - **Botão de resolução** (0.4.1, pedido do Johan), na ilha de baixo: mostra a resolução tocando (em âmbar quando escolhida à mão) e abre um painel com "Auto" e as resoluções do vídeo. A escolhida à mão **fica** mesmo com a rede oscilando (o Johan não gosta de o app oficial cair do 4K e não voltar); só sai se o decodificador falhar, com o aviso "Couldn't play …p on this device. Back to Auto."
  - **TV em 360p** (relato do Johan na 0.4.0, TV Hisense com Android 11 de 32 bits; corrigido na 0.4.1 sem log da TV, precisa confirmar nela): duas causas possíveis, as duas tratadas. (1) Um cliente do YouTube que só entrega o arquivo único (360p, o itag 18) era aceito na hora; agora o app tenta os outros clientes antes e prefere o HLS ao arquivo único. (2) Algumas TVs declaram limites de decodificador mais baixos do que decodificam, e o Media3 respeita (`isFormatSupported` com os "performance points" do Android 10+).
  - **4K primeiro na TV** (decisão do Johan, 2026-11-02: "tentar sempre 4K antes, só cair para uma resolução inferior se realmente necessário"; era para a TV): o app escolhe a faixa sozinho (`QualityRules`), a maior com decodificador de hardware até 4K, mesmo que o decodificador declare menos. Fica de fora só o que não tem como dar certo: codec sem decodificador de hardware, mais de 60 quadros por segundo e HDR que o decodificador não aceita. Na mesma resolução, vale a faixa que o decodificador aceita.
  - **Descer um degrau** só quando o aparelho falha de verdade: erro do decodificador ou muitos quadros perdidos duas vezes (o teto cai para a resolução de baixo até o app fechar), ou o vídeo parar para carregar três vezes sem o usuário pular (só naquele vídeo, porque é a rede). Depois de descer pela rede, o app tenta o topo de novo após 2 minutos sem parar para carregar.
  - **Demora para começar:** o app prepara o motor ao abrir (`refreshCacheIfNeeded` desde a 0.4.1; desde a 0.6.4, também o token do YouTube, o player dele e a decifragem, ver "Começar mais rápido" acima). Numa TV fraca, isso levava segundos no primeiro vídeo.
- **Modo dormir** (0.4.1, pedido do Johan, que usa playlists para dormir, principalmente na TV): o botão da lua, na ilha de baixo, abre um painel acima dela com três linhas:
  - **Brightness: 100% / 50% / 25% / 10% / 0%** escurece o vídeo e a legenda, sem mexer no brilho da TV (uma camada preta por cima do vídeo e por baixo dos controles). Escurecido, os controles não acendem sozinhos na troca de vídeo nem no fim. **Vale até fechar o app** (0.5.6, pedido do Johan; até a 0.5.5 voltava a 100% a cada player aberto): o brilho escolhido fica para os próximos vídeos, mas só na memória, então o app aberto de novo (ou a TV ligada de novo) começa com o vídeo claro.
  - **Then play: None / 1 / 2 / 3 / 4 / 5 / All**: quantos vídeos da playlist ainda tocam depois do atual (ver "Tocar em sequência"). Só aparece quando o vídeo veio da página de uma playlist. O número desce a cada vídeo. Começa em All a cada vez. Por decisão do Johan, essa escolha fica **só no player**, não na lista.
  - **When done: Stay on / Turn off** (**só na TV**; no celular e no tablet a linha e o serviço de Acessibilidade saem do app, pedido do Johan na 0.4.2): o que fazer quando os vídeos acabam. A escolha **fica guardada** (pedido do Johan) e vale para todo vídeo ou sequência que terminar. Com "Turn off", o app avisa e espera 10 s (qualquer botão ou toque cancela) e põe o aparelho para dormir. Pelo HDMI-CEC, a TV desliga junto.
  - O Android não deixa um app comum pôr o aparelho para dormir. Por isso o app tem um serviço de Acessibilidade (`ScreenOffService`) que faz isso e, desde a 0.6.1, também segura o botão YouTube do controle (ver "Botão YouTube do controle"); ele não lê a tela, e o usuário liga uma vez, em Configurações > Acessibilidade. Ao escolher "Turn off" sem o serviço ligado, o app abre essa tela. Precisa do Android 9 ou mais novo; antes disso, a linha não aparece.
  - Com "Stay on", ou sem o serviço, o player só para de manter a tela acesa quando os vídeos acabam, e quem apaga é o timer do aparelho.
  - Com o painel aberto, os controles não somem sozinhos; "voltar" ou a lua fecham o painel.

## Playlists

- **Tocar em sequência** (0.4.1): um vídeo aberto pela página de uma playlist (Watch later ou qualquer outra) é seguido pelos próximos da lista, carregando mais páginas se precisar (`PlayQueue`). Vídeos abertos de outras listas (feed, busca, histórico, canal) ou por link tocam sozinhos. O limite de quantos seguem fica no painel de dormir do player. Um vídeo da sequência que não toca (apagado, privado) mostra o erro por 3 s e é pulado sem gastar o limite; agendados ficam de fora.

- **Salvar com um toque**: o botão de marcador na barra de cima do player põe o vídeo direto no "Assistir mais tarde" (ou na playlist escolhida em Settings > Save button) e tira de lá no segundo toque. O ícone cheio mostra que o vídeo já está na playlist.
- **Salvar pelas listas** (etapa 6): no celular e no tablet, segurar o dedo numa miniatura põe o vídeo na playlist do botão Save, ou tira se já estiver, com um aviso embaixo ("Saved to…"/"Removed from…"). Na TV, segurar o OK abre um menu com duas opções: "Save to…"/"Remove from…" e "Go to channel".
  - **Resposta na hora** (0.4.1, o Johan achou lento na 0.4.0): o marcador e o aviso mudam no toque, e a conta é atualizada logo depois (`SavedVideos.toggle`). Tocar de novo antes de a conta responder só muda o que o usuário quer; no fim a conta fica como o último toque, sem pedido repetido. Se a conta recusar, o marcador volta e aparece "Couldn't…". O botão Save do player usa o mesmo caminho.
  - Um marcador discreto (âmbar) na miniatura mostra os vídeos que já estão nessa playlist. Ele não aparece dentro da própria playlist.
  - **Dentro da própria playlist do Save, segurar a miniatura não faz nada** (0.5.5, pedido do Johan: confundia, porque lá o toque longo tirava o vídeo da playlist). Para tirar, é só deslizar para a esquerda. Nas outras playlists suas, segurar continua salvando no Watch later.
  - O app guarda a lista dos vídeos da playlist do Save (até 10 páginas), lida ao abrir e ao trocar a playlist em Settings. Na hora de salvar ou tirar, ele confere na conta se o vídeo está lá, então o marcador pode estar desatualizado numa playlist enorme, mas o toque faz sempre a coisa certa.
- **Listas que abrem na hora e rolam sem esperar** (0.5.5, pedido do Johan: o Watch later dele tem mais de 700 vídeos e travava no fim de cada página). As playlists e o Histórico ficam guardados no aparelho (os primeiros 300 vídeos, por até 14 dias; `ListCache`): abrem com a lista da última vez, e a da conta chega por trás, com o indicador de atualizar. No celular e no tablet, a próxima página é pedida quando faltam 50 vídeos para o fim, e cada página que chega pede a seguinte, então a lista carrega à frente enquanto rola. Mover e a seta para o topo esperam a lista da conta (um instante), porque a guardada pode estar velha. Sair da conta apaga as listas guardadas.
- **Reordenar vídeos dentro das playlists do usuário**, principalmente o Assistir mais tarde. As mudanças vão para a conta, então aparecem em todos os aparelhos. Só nas playlists do próprio usuário.
  - **Sem modo de reordenar** (0.5.5, pedido do Johan: o botão carregava a playlist inteira antes, e com 700 vídeos demorava muito).
  - **Sem ícones na linha** (2026-11-03, o Johan achou estranha a fileira de alças e setas na lateral e escolheu "segurar e arrastar"): no celular e no tablet, nas suas playlists, **segurar o vídeo** pega ele (vibra, cresce um pouco e ganha sombra) e **arrastar** muda a ordem; perto da borda a lista rola sozinha e carrega mais. Soltar sem arrastar não abre o vídeo. **Deslizar para a direita** leva o vídeo ao topo (fundo na cor de destaque com a seta; aviso "Moved to the top"), e para a esquerda continua tirando. Por isso, nas suas playlists, segurar não salva mais no Watch later (nas playlists dos outros e nas outras listas, continua salvando). Até então eram uma alça e uma seta para o topo à direita de cada vídeo.
  - **Cada vídeo solto vai para a conta na hora**, com um movimento só ("pôr antes de" quem ficou logo depois dele). Não precisa da playlist inteira: solto no fim do que já carregou, o app carrega a próxima página para saber quem vem depois. Os movimentos vão um por vez, na ordem em que foram feitos. Se a conta recusar, aparece "Couldn't move it" e a lista volta a ser a da conta.
  - Na TV: o botão "Reorder" liga o modo mover (sem carregar nada antes), em que OK pega o vídeo, cima e baixo levam ele pela lista e OK solta, já gravando. "Voltar" solta o vídeo pego, e de novo sai do modo; "Done" também sai.
  - O MediaServiceCore já tem adicionar, remover, criar e renomear, mas **não tem mover**. O app manda o movimento para o endpoint interno `browse/edit_playlist` (ação `ACTION_MOVE_VIDEO_BEFORE`, como o ytmusicapi).
  - Cada movimento usa o `setVideoId` do vídeo (a posição dele na playlist), que o app lê pedindo a playlist ao YouTube (cliente de TV e, se faltar, WEB). Os já lidos ficam guardados enquanto o app está aberto (eles não mudam quando os vídeos mudam de lugar), então uma playlist grande só é lida no primeiro movimento; se um movimento falhar, o app lê de novo e tenta mais uma vez. Uma playlist pública não mostra o `setVideoId` sem ser do dono, então **essa parte só dá para validar no aparelho**, com o login.

## Explore

Pedido do Johan (2026-11-02): os filtros das Recomendações só filtram o que o YouTube já mandou, e muita coisa é difícil de descobrir. Ele quer descobrir canais, sem perder as Recomendações.

- Histórico da ideia: na 0.5.0, categorias feitas à mão (buscas por palavra-chave); na 0.5.1, temas achados nos títulos do histórico, que trouxeram "muita recomendação, nada a ver". Na **0.5.2**, o pedido dele: **vídeos dos canais que os canais que ele segue destacam**, misturando vídeos recentes e antigos, e **um subconjunto bem diferente a cada atualização**, com a lista dos canais guardada para não ficar lento.
- **A lista dos canais inscritos** (0.5.3): o motor entrega essa lista **sem o id dos canais** (o `TabMediaItem` dele não lê o browseId), e por isso a Explore da 0.5.2 vinha vazia no aparelho: sem ids, nenhuma página era lida. O app agora lê a página de inscrições da TV (`FEsubscriptions`, uma aba por canal) direto no Innertube, com o login do motor, e tira o id de cada canal dos params da aba (protobuf em base64 com o "UC…" dentro; `SubscribedChannels`). Conferido com as respostas reais da TV guardadas nos testes do motor (956 canais, todos com id).
- **De onde vêm os canais:** a página de cada canal inscrito traz os canais que o criador destaca ("Featured channels", "Other channels to check out"). O app lê essa página pelo motor (o mesmo pedido da página do canal), um por canal inscrito, e guarda o resultado num arquivo do aparelho (`explore.json`). Cada página é lida de novo depois de 30 dias.
  - Na primeira vez, o app lê 12 canais inscritos antes de mostrar a lista (mais 12, até 3 vezes, se aparecerem menos de 30 canais), e o resto em segundo plano, 2 por vez.
  - Ficam de fora os canais que o usuário já segue, os bloqueados e os **canais-irmãos do mesmo criador** (traduções, segundo canal, podcast: "Veritasium em Português", "2veritasium", "Dinge Erklärt – Kurzgesagt", "Mentour Now!"), pelo nome e pelo @ (`ExploreRules.isSibling`). O irmão sai só para quem o destacou.
- **O sorteio** (`ExploreRules`): 10 canais por vez, com mais chance para os destacados por mais canais inscritos. Os canais dos últimos sorteios (os 40 mais recentes) ficam de fora enquanto houver outros, então cada atualização traz canais diferentes.
- **Os vídeos de cada canal sorteado:** a página dele traz a aba de vídeos (os recentes) e as prateleiras (os populares e os escolhidos pelo criador, quase sempre antigos). O app sorteia 2 entre os 12 mais recentes e 2 das prateleiras; se faltar de um lado, o outro completa. Só vídeos do próprio canal (as prateleiras também mostram participações em outros canais). Sem Shorts, lives, vídeos já assistidos (90% ou mais) e tópicos excluídos.
- A lista junta os canais em rodadas (um vídeo de cada canal, depois o segundo…), para não ter vídeos do mesmo canal seguidos. Rolar até o fim sorteia mais 10 canais. **Puxar para atualizar** (OK longo na aba, na TV) faz um sorteio novo.
- Os pedidos vão com a conta logada (o motor põe o login em todo pedido), como abrir a página de um canal no app. Sem login (ou sem as inscrições), a aba mostra o erro.
- Conferido contra o YouTube em 2026-11-02, com 8 canais de exemplo: 41 canais candidatos, e cada sorteio de 10 canais com 40 vídeos levou 2 a 3 s.
- Descartado: as seções prontas do YouTube (Gaming, News, Sports, Live, Podcasts), que trazem conteúdo popular e pouco controle.
- **Filtros de data, duração e ordem** (pedido do Johan, 2026-11-03, tirados das Recomendações): cada canal sorteado escolhe os seus vídeos **entre os que passam pelos filtros** (dos 12 recentes e das prateleiras), em vez de filtrar depois os 4 já escolhidos; assim "This week" pega os recentes e "Before 2020" pega as prateleiras. Se sobrarem menos de 12 vídeos na tela, o app sorteia mais 10 canais, até 4 vezes; o fim da lista faz o mesmo. Mudar um filtro refaz a lista sem ler nada de novo: o app guarda os vídeos de cada canal sorteado e a semente de cada sorteio (`ExploreRules.screen`). "Most viewed" e "Newest" ordenam a lista inteira (sem as rodadas por canal). Conferido contra o YouTube em 2026-11-03: os 83 vídeos de um canal sorteado trouxeram data, duração e visualizações.
- **O texto que explica a aba** (pedido do Johan, 2026-11-03): não fica mais fixo no topo. Aparece como um aviso só na primeira vez que a aba abre, com "Got it" para fechar (no celular e no tablet, tocar fora também fecha; na TV, o foco já começa no botão). Depois disso não volta; a marca fica nas configurações do aparelho (`explore_intro_seen`).

## Inscrição em playlists

O YouTube só tem inscrição em canais; "salvar playlist" não gera feed. O Doorglow monta esse feed, usando as **playlists salvas da conta** como a lista de inscrições (decisão do Johan, 2026-11-01). Assim a lista é a mesma no celular, no tablet e na TV.

- Botão "Inscrever" na página da playlist: salva a playlist na biblioteca da conta (`MediaItemService.savePlaylistObserve`). "Cancelar inscrição" tira da biblioteca (`removePlaylistObserve`).
  - Inscrito, o botão fica contornado em âmbar, com "Subscribed ✓", e o texto ao lado diz que tocar de novo cancela (etapa 6).
- Contam como inscritas as playlists **de outros canais** salvas na conta, inclusive as que já estavam salvas antes do Doorglow. As playlists do próprio usuário (Assistir mais tarde e as que ele criou) ficam de fora do feed.
- Lista para gerenciar nas Configurações, com "Unsubscribe". Para se inscrever: salvar a playlist no YouTube, ou abrir um link `youtube.com/playlist?list=` no Doorglow (abre a página da playlist com o botão).
- **O que é "novo" numa playlist:** os vídeos de playlist não trazem data. Por isso o app anota, neste aparelho, quando cada vídeo apareceu na playlist (`PlaylistLedger`). O que já estava lá na primeira leitura não entra no feed; o que entrar depois aparece com a data em que foi visto. Por isso, logo depois de se inscrever, a playlist ainda não mostra nada nas Inscrições.
- O feed de Inscrições junta o feed dos canais com os vídeos das playlists inscritas.
- **Sem duplicados:** um vídeo de playlist inscrita só entra no feed se o usuário **não** estiver inscrito no canal do vídeo (se estiver, o vídeo já vem pelo canal).
  - Até a 0.5.2 essa regra não funcionava no aparelho, pela mesma falta de ids da lista de canais inscritos (ver "Explore"); a 0.5.3 corrige. As fotos dos canais inscritos nos cards vêm da mesma lista.
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

## Botão YouTube do controle (TV)

- Pedido do Johan em 2026-11-04: o botão YouTube do controle remoto abrir o Doorglow, nas duas TVs (Hisense Android 11 e TCL Android 14). Ele escolheu fazer pelo próprio Doorglow, e não pelo Button Mapper.
- **Por que não pelos links:** o botão não manda um link. A TV entrega a tecla a um componente fixo do sistema (as "teclas globais" do Android mandam um broadcast explícito; no Google TV, quem recebe é o launcher), e ele abre o app do YouTube direto. Por isso nem desativar o YouTube faz o botão cair no Doorglow.
- **Como fica:** o serviço de Acessibilidade do app (`ScreenOffService`, o mesmo do "When done: Turn off") ganha o filtro de teclas: vê cada tecla do controle antes do sistema, segura só a do botão YouTube (a descida, as repetições e a subida) e traz o Doorglow para a frente como o launcher faz (desde a 0.6.3, sair do app fecha o player da TV, então ele volta na lista). As outras teclas seguem na hora. Só na TV (no celular e no tablet o serviço sai do app). O nome da classe fica, para o serviço não desligar em quem já o ligou.
- **O app aprende o código do botão** (`YouTubeButton`), porque cada marca e cada controle mandam um código diferente, e o Johan só poderia medir pelo ADB dias depois. Quando uma janela do app do YouTube aparece até 5 s depois de uma tecla de app (fora das setas, OK, voltar, início, volume, mídia, canais, números, letras, voz e teclas do sistema), sem outra tecla no meio, aquela tecla era o botão. O código fica guardado no aparelho e, dali em diante, o YouTube nem abre. Na primeira vez, o YouTube abre e o Doorglow vem por cima logo depois. Um controle novo soma outro código.
  - Para isso o serviço fica sabendo quando uma janela do app do YouTube aparece (`packageNames`), e só disso: dos outros apps não chega nada, e a tela continua fora do alcance dele. A descrição do serviço nos ajustes diz que ele vê as teclas e quando o YouTube abre.
  - Um vídeo mandado do celular para a TV abre o YouTube sem tecla nenhuma, então não ensina nada nem é desviado.
  - Teclas de app vão para o log (tag Doorglow, "remote key: code=… scan=…"), e o aprendizado também ("learned the YouTube button").
- **Limites e riscos, a conferir no aparelho:** se a TV tratar o botão por baixo do Android (antes do filtro de teclas), o serviço não vê a tecla e o botão continua abrindo o YouTube. Com a TV desligada, o botão pode ligar a TV direto no YouTube, por fora do Android. E em algumas Hisense, um serviço que filtra teclas faz o volume parar de repetir ao segurar.

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
   - *Versão 0.6.4 (2026-11-13), pedido do Johan do mesmo dia, aguardando o teste no aparelho* (ver "Como testar a 0.6.4"): a miniatura do vídeo e um círculo de carregando desde que o player abre (nunca a tela só preta), e o começo do vídeo mais rápido: o motor preparado antes do primeiro vídeo, o vídeo pedido com o foco parado no card da TV e a legenda sem segurar o começo.
   - *Versão 0.6.3 (2026-11-13), pedidos do Johan de 2026-11-11 e 2026-11-13 juntos, aguardando o teste no aparelho* (ver "Como testar a 0.6.3"): o vídeo continua tocando com a tela do celular ou do tablet desligada, só o som, com o controle de mídia na notificação e na tela de bloqueio; o mini player flutuante ao sair do app com o vídeo tocando; as Inscrições se atualizam sozinhas ao voltar depois de um tempo, com os vídeos novos em cima; na TV, desligar a TV ou sair do app fecha o player; há quanto tempo o vídeo foi publicado, na linha do canal do player; "Go to channel" sempre, também nas Recomendações; e os títulos dos cards da TV menores, com as linhas mais juntas.
   - *Versão 0.6.2 (2026-11-04), pedidos do Johan testando a 0.6.0, aguardando o teste no aparelho* (ver "Como testar a 0.6.2"): a legenda automática aparece em blocos de duas linhas, sem rolar; e na TV, descer da última linha dos controles (ou subir da primeira) os esconde na hora.
   - *Versão 0.6.1 (2026-11-04), pedido do Johan, aguardando o teste no aparelho* (ver "Como testar a 0.6.1"): o botão YouTube do controle abre o Doorglow, que aprende o código do botão sozinho (ver "Botão YouTube do controle").
   - *Versão 0.6.0 (2026-11-04), achados e pedidos do Johan testando a 0.5.9 na TV, aguardando o teste no aparelho* (ver "Como testar a 0.6.0"): Subscribe/Subscribed na página do canal; subir até as abas para na aba aberta; a legenda não some mais com os controles fechados; voltar a um vídeo começado retoma de onde parou; 10 s por toque na barra da TV; o contorno do nome do canal no player não sai cortado; e o 4K da TV começa mais rápido (o primeiro pedaço em até 1080p, da thread de desempenho).
   - *Versão 0.5.9 (2026-11-04), achado do Johan testando a 0.5.8 na TV, aguardando o teste no aparelho* (ver "Como testar a 0.5.9"): na TV, as listas (início, playlist e canal) rolam até a borda de baixo da tela, sem a última linha de miniaturas cortada antes dela.
   - *Versão 0.5.8 (2026-11-03), pedidos do Johan depois de testar a 0.5.7, aguardando o teste no aparelho* (ver "Como testar a 0.5.8"): os filtros de data, duração e ordem saem das Recomendações e vão para a Explore; a legenda sobe acima da barra com os controles abertos e ganha uma margem pequena da borda; o botão LIVE nas lives; e as cores de destaque passam a fazer par com as do ícone, com o vermelho do YouTube.
   - *Versão 0.5.7 (2026-11-03), pedidos do Johan do mesmo dia, juntados antes do build; aguardando o teste no aparelho* (ver "Como testar a 0.5.7"): o texto da Explore vira um aviso que aparece só na primeira vez, o vidro das barras e do player mais opaco, em degradê da borda da tela para o conteúdo, e o OK longo da TV mais curto (0,3 s). Do refinamento do visual (mesmo dia): o canal e as visualizações sem corte duplo nos cartões, as Configurações em cartões, o player mais compacto, com a **barra de baixo de borda a borda** (e, na TV, like, dislike, Save e Subscribe nela) e **sem o botão Skip ahead** (avançar dentro do trecho pula até o fim dele), a **cor de destaque escolhida em Settings**, os indicadores sem brilho, **reordenar sem ícones** (segurar e arrastar; deslizar para a direita leva ao topo), os **capítulos na barra** (frestas e o nome do atual) e o **preview com miniatura** ao arrastar a barra. Na TV, os Ajustes em cartões e os filtros mais leves; e cartões vazios no lugar do carregando.
   - *Na 0.5.7 também, o pacote de desempenho* (pedido do Johan em 2026-11-03: carregar o vídeo e rolar as listas mais rápido, sem perder qualidade de vídeo nem de imagem; ele leu o levantamento e liberou todos os itens): o vídeo pedido já no toque, a qualidade certa na primeira escolha do player, os extras depois do primeiro quadro, o próximo vídeo da playlist resolvido antes, voltar 10 s sem baixar de novo, HTTP/3 no Android 14, miniaturas em HTTP/2 e só à frente da tela, filtros fora da thread da tela, perfil de compilação e o R8. Ver "Desempenho (0.5.7)" nas notas técnicas. O levantamento está em https://claude.ai/artifact/WvrQg4nVqexDtmtHdwZkPd.
   - *Versão 0.5.6 (2026-11-03), achados do Johan na TV, aguardando o teste no aparelho* (ver "Como testar a 0.5.6"): a ilha do player em duas linhas na TV (os botões depois da barra não pegavam o foco) e o brilho do modo dormir mantido até fechar o app.
   - *Versão 0.5.5 (2026-11-03), pedidos do Johan testando a 0.5.4, aguardando o teste no aparelho* (ver "Como testar a 0.5.5"): Undo ao tirar vídeos do Histórico e das playlists, reordenar sem o modo (alça em cada vídeo, cada movimento gravado na hora), a seta para o topo, o toque longo desligado dentro do Watch later, e as playlists e o Histórico guardados no aparelho e carregando à frente.
   - *Versão 0.5.4 (2026-11-03), ajustes depois de o Johan testar a 0.5.3, aguardando o teste no aparelho* (ver "Como testar a 0.5.4"): 3 colunas no tablet de 14,6" em pé, a atualização das listas sem os cartões voando e o ícone e o banner da TV refinados.
   - *Versão 0.5.3 (2026-11-02), correções depois de o Johan testar a 0.5.2; o Johan aprovou a Explore em 2026-11-03* (ver "Como testar a 0.5.3"): a Explore vazia (a lista de canais inscritos vinha sem ids; achado pelos logs do celular, via ADB pelo PC) e as Inscrições guardadas no aparelho para o app abrir na hora.
   - *Versão 0.5.2 (2026-11-02), a Explore pelos canais que os canais inscritos destacam, pedida pelo Johan depois de testar a 0.5.1.* No aparelho, a aba veio vazia (corrigido na 0.5.3). A leitura dos destaques e dos vídeos foi conferida contra o YouTube.
   - *Versão 0.5.1 (2026-11-02), ajustes pedidos pelo Johan depois de testar a 0.5.0; o Johan testou e aprovou tudo menos a Explore* (ver "Como testar a 0.5.1"): legendas centralizadas com contorno, toques no vídeo, Skip ahead pelos vales do gráfico com marcas na barra, girar a tela, 1080p no celular e no tablet, 3 colunas no tablet em pé, Histórico em linhas e a Explore automática.
   - *Versão 0.5.0 (2026-11-02), a aba Explore pedida pelo Johan* (ver "Como testar a 0.5.0"). A busca das categorias foi conferida contra o YouTube, sem login.
   - *Versão 0.4.2 (2026-11-02), pedidos do Johan depois de testar a 0.4.1, aguardando o teste no aparelho* (ver "Como testar a 0.4.2"): sem o @ do canal, "When done" só na TV, reordenar arrastando no celular, tirar vídeos do Histórico e das playlists, deslizar entre abas, as Configurações pelo avatar, o menu do OK longo abrindo antes de soltar (TV) e o **player novo (opção A)**. A aba Explore vem na versão seguinte.
   - *Versão 0.4.1 (2026-11-02), ajustes pedidos pelo Johan depois da 0.4.0, aguardando o teste no aparelho* (ver "Como testar a 0.4.1"): filtros nas Recomendações, duração de 30/45/60 min ou mais, "Before <ano>", salvar com resposta na hora, título no player, bloquear discreto, desempenho, playlists em sequência, o modo dormir no player (escurecer, quantos vídeos seguem e desligar a TV no fim) e a correção dos vídeos em 360p e lentos para começar na TV.
   - *Polimento visual implementado em 2026-11-02 (versão 0.4.0), aguardando o teste no aparelho* (ver "Como testar a etapa 6"). Junto, a pedido do Johan: salvar pelas listas (toque longo, e o menu do OK longo na TV), o marcador de salvo, o canal pelos cards, o estado de inscrito mais claro na playlist, puxar para atualizar (OK longo na aba, na TV) e o ícone vermelho. As Configurações só ganharam o visual novo.
   - Falta a rotina de atualização do motor.

Extensão do Firefox: deixa de ser necessária para o "Assistir mais tarde", porque o motor acessa o real. Fica como ideia opcional para limpar o site.

## Perguntas em aberto

Nenhuma no momento. Falta a rotina de atualização do motor (resto da etapa 6) e os testes no aparelho das etapas 3 a 6.

## Como testar a 0.6.4

1. Abrir um vídeo (TV, celular ou tablet): na hora aparecem a miniatura do vídeo, escurecida, e um círculo girando na cor de destaque. Em nenhum momento a tela fica só preta. Quando o vídeo começa, a miniatura some.
2. Abrir o app, esperar uns 5 s nas listas e abrir o primeiro vídeo: ele deve começar perto do tempo dos vídeos seguintes (antes, o primeiro era bem mais lento).
3. TV: parar o foco num card por um ou dois segundos e apertar OK: o vídeo deve começar mais rápido do que apertando OK logo ao chegar no card.
4. Um vídeo com legenda (ligada ou não): começa sem esperar a legenda; com ela ligada, o texto aparece junto com a fala.
5. No meio do vídeo, pular para bem adiante na barra: o círculo aparece enquanto carrega (sem a miniatura) e some quando volta a tocar. Voltar 10 s (trecho já carregado): o círculo não pisca.
6. Numa playlist, quando um vídeo acaba: a miniatura do próximo e o círculo, até ele começar.
7. Um vídeo que não toca (privado, apagado): a mensagem de erro, sem o círculo.
8. Com o modo dormir escurecendo o vídeo, a miniatura e o círculo ficam escurecidos junto.
9. Para medir (com o ADB ligado): `adb logcat -s Doorglow`. Ao abrir o app, a linha `engine warm-up: client TV, core … ms, account … ms, po token … ms, solver … ms` diz quanto cada preparo levou. A cada vídeo, `video <id>, client …: DashManifest (engine … ms, manifest … ms)` é o pedido ao YouTube (com `(ahead of the OK)` quando foi o foco parado), e `first frame of <id> in … ms (stream ready in …, player prepared in …, tracks in …, buffered in …)` separa o resto. Bom comparar o primeiro vídeo depois de abrir o app com o segundo, e a 0.6.3 com a 0.6.4.

## Como testar a 0.6.3

1. Celular ou tablet: abrir um vídeo e apertar o botão de desligar a tela. O som continua.
2. Na tela de bloqueio (e na notificação), o controle de mídia com a miniatura, o título e o canal: pausar e tocar por ali. O título e o canal podem aparecer um pouco depois do vídeo começar.
3. Desbloquear: o player está lá, no ponto em que o som está, e a imagem volta em um instante.
4. Playlist com a tela desligada: o próximo vídeo começa sozinho quando o atual acaba, e o "Then play" do modo dormir continua valendo.
5. Com o vídeo tocando, apertar Home (ou trocar de app): o vídeo fica numa janelinha por cima, sem os controles do app. Arrastar e aumentar a janelinha; tocar nela mostra os botões do Android (tocar/pausar, voltar ao player, fechar).
6. Voltar ao player pela janelinha: o player inteiro, com os controles, no mesmo ponto. Voltar dali vai para a tela do app em que o vídeo foi aberto.
7. Fechar a janelinha (o X, ou arrastar para baixo): o vídeo para e a notificação some.
8. Com a janelinha aberta, abrir o Doorglow e tocar em outro vídeo (ou num link do WhatsApp): a janelinha do anterior fecha.
9. Pausar o vídeo e apertar Home: não abre a janelinha, e o vídeo fica pausado, como antes.
10. Tocar na notificação com o app por trás (sem a janelinha): abre o player. Também com um vídeo aberto por um link do WhatsApp.
11. Fechar o player (voltar): a notificação some, e não abre a janelinha.
12. TV: desligar a TV com um vídeo tocando: sem notificação e sem janelinha. Religar e abrir o Doorglow: ele está na lista de onde o vídeo foi aberto (não numa tela preta carregando o vídeo). Abrir o vídeo de novo: volta de onde parou.
13. TV: com um vídeo aberto, apertar Home e abrir o Doorglow de novo: o mesmo, a lista. Ligar o "When done: Turn off" pela primeira vez (abre os ajustes de Acessibilidade) e voltar: o player continua lá.
14. TV: deixar a TV desligada (ou o app fora) por mais de 10 minutos e voltar ao Doorglow nas Inscrições: em alguns segundos a lista se atualiza, volta ao começo com os vídeos novos e o foco vai para o primeiro. Se a TV demorar a achar o Wi-Fi, a lista antiga fica até a nova chegar.
15. Celular: sair do app por mais de 10 minutos e voltar: as Inscrições se atualizam sozinhas e voltam ao começo. Por menos tempo, nada muda. Puxar para atualizar no começo da lista: os vídeos novos aparecem em cima (antes ficavam escondidos acima do primeiro).
16. Abrir o app depois de um tempo: as Inscrições guardadas aparecem e, quando as da conta chegam, os vídeos novos entram em cima, à vista.
17. TV, player: embaixo do título, "Canal · 3 days ago" (o mesmo tempo do card), pequeno e em cinza. Numa live, sem o tempo. Num vídeo aberto por link de outro app, o tempo vem da data do vídeo ("2 weeks ago", "today").
18. TV, Recommended: segurar OK num vídeo: o menu tem "Go to channel", e escolher abre o canal (pode levar um instante). No celular, tocar no nome do canal de um vídeo das Recomendações abre o canal.
19. TV, as listas: os títulos um pouco menores, as duas linhas mais juntas, e um respiro maior entre o título e o canal. Títulos mais longos cabem nas duas linhas.

## Como testar a 0.6.2

1. Legenda: ligar o CC num vídeo que só tem a automática (a maioria dos vídeos sem legenda feita por pessoa). As linhas aparecem de duas em duas e trocam inteiras; nenhuma sobe. Numa pausa da fala, pode aparecer uma linha só.
2. Num vídeo com legenda feita por pessoa, ela continua como antes.
3. TV: abrir os controles e descer até a linha de baixo (tocar/pausar e os botões). Mais uma vez para baixo e os controles somem na hora. O mesmo subindo até o nome do canal e apertando para cima.
4. Segurar a seta para baixo com os controles abertos: eles somem e não voltam enquanto a seta estiver apertada. Soltar e apertar de novo acende.
5. Com o painel da lua ou da resolução aberto, as setas continuam andando nele.

## Como testar a 0.6.1

1. TV: a Acessibilidade do Doorglow precisa estar ligada (a mesma do "When done: Turn off"; Configurações > Acessibilidade > Doorglow). A descrição dela agora fala do botão YouTube.
2. Com o Doorglow fechado, apertar o botão YouTube do controle. Na primeira vez, o YouTube abre e, logo depois, o Doorglow vem por cima.
3. Voltar ao início e apertar o botão de novo: o Doorglow abre direto, sem o YouTube aparecer. Com o player aberto, o botão volta para ele.
4. As outras teclas continuam iguais: setas, OK, voltar, voz e o botão Netflix. Segurar o volume ainda sobe ou desce sem parar (em algumas Hisense, o filtro de teclas atrapalha isso).
5. Com a TV desligada, apertar o botão YouTube: ver se ela liga no Doorglow ou no YouTube.
6. Se o botão continuar abrindo o YouTube, a TV trata o botão por baixo do Android. Aí, pelo ADB, `logcat -s Doorglow` mostra se aparece "remote key" ao apertar o botão.

## Como testar a 0.6.0

1. Canal (celular e TV): abrir a página de um canal. Aparece "Subscribe" (cheio) ou "✓ Subscribed" (contornado), como na playlist. Apertar troca, e o canal entra ou sai das Inscrições. Na TV, o foco fica no botão depois de apertar.
2. TV: numa aba (Subscriptions, Search…), ir até um card da direita e segurar cima até chegar nas abas: o foco para na aba aberta, e a página não troca. Na página de um canal, cima a partir dos vídeos para em "Videos".
3. TV: ligar a legenda num vídeo e deixar os controles sumirem. A legenda continua na tela, embaixo; com os controles abertos, ela fica acima da barra.
4. Assistir um pedaço de um vídeo, sair e abrir de novo: ele continua de onde parou. Um vídeo visto até o fim começa do início. Vale no celular, no tablet e na TV.
5. TV: na barra de tempo, cada toque para a esquerda ou a direita anda 10 s.
6. TV: no player, subir até o nome do canal: o contorno aparece inteiro, sem corte na esquerda.
7. TV: um vídeo em 4K começa mais rápido: os primeiros segundos em 1080p e depois 4K, sem parar. O mesmo depois de pular para um trecho ainda não carregado.

## Como testar a 0.5.9

1. TV: em Subscriptions (e nas outras abas, numa playlist e num canal), a última linha de miniaturas vai até a borda de baixo da tela, sem um corte reto antes dela. Descendo até o fim da lista, o último vídeo fica inteiro, com um respiro embaixo.

## Como testar a 0.5.8

1. Recommended não tem mais os filtros no topo (celular e TV).
2. Explore tem os filtros no topo (no celular, rolam com a lista; na TV, uma fileira de botões).
3. Explore com "This week": aparecem só vídeos da semana. Se forem poucos, o app sorteia mais canais sozinho, com o círculo no fim da lista.
4. Explore com "Before <ano>" (por exemplo Before 2020): só vídeos antigos, das prateleiras dos canais.
5. Duração (por exemplo 20+ min) e ordem ("Most viewed", "Newest") reordenam a lista inteira.
6. Tirar os filtros volta à lista de antes, sem carregar de novo. Puxar para atualizar (OK longo na aba, na TV) sorteia outros canais e mantém os filtros.
7. Legenda: ligar o CC num vídeo e abrir os controles. A legenda sobe para cima da barra (e do nome do capítulo) e desce quando os controles somem. Com os controles fechados, ela fica um pouquinho acima da borda de baixo. No celular em pé, com o vídeo no meio, ela não se mexe.
8. Live: o tempo e a duração dão lugar a "● LIVE", com o ponto vermelho. Pausar uns 15 s (ou voltar na barra): o ponto fica cinza. Tocar em LIVE (na TV, baixo até ele e OK) volta para o ao vivo e o ponto fica vermelho de novo.
9. Settings > Highlight color: as sete cores seguem as do ícone, na mesma ordem (Amber, Silver, Teal, Plum, Mustard, Vivid e Red). Quem tinha escolhido Pink, Purple, Blue ou Green volta para Amber. O Red é o vermelho do YouTube, na barra do player inclusive.

## Como testar a 0.5.7

Tem o R8 ligado no release pela primeira vez: vale um teste completo (login, feeds, Explore, busca, player, playlists e mover na playlist), no celular e na TV. Se algo quebrar só nesta versão, o log (tag Doorglow) mostra.

1. Explore: na primeira vez aparece um aviso explicando a aba, com "Got it". Depois de fechar, não volta, nem reabrindo o app.
2. As barras de cima e de baixo, e o trilho do tablet deitado, ficam mais opacos na borda da tela e mais transparentes junto do conteúdo.
3. TV: segurar OK num vídeo por uns 0,3 s já abre o menu, e um OK rápido continua abrindo o vídeo. Segurar OK numa aba recarrega a lista.
4. Cartões: o nome do canal numa linha e "visualizações · data" embaixo, sem cortar os dois. Settings em cartões agrupados (celular e TV).
5. Carregando: em Subscriptions, Recommended, Explore e na busca, cartões vazios no lugar do círculo (celular) e do "Loading…" (TV).
6. Player: a barra de baixo vai de borda a borda, sem cantos, com os controles mais compactos. Na TV, like, dislike, Save e Subscribe estão nela, logo depois do tocar; com o controle, baixo leva da barra de tempo a eles, e direita anda até legenda, resolução e lua.
7. Não tem mais o botão Skip ahead. Num vídeo com patrocínio, avançar (dois toques à direita, ou a barra e a tecla de avançar na TV) no começo dele, ou logo antes, vai para o fim do trecho e mostra "Jumped ahead".
8. Num vídeo com capítulos: frestas na barra onde cada um começa e o nome do atual acima dela. Arrastar a barra (ou andar nela com o controle) mostra um preview com a miniatura daquele ponto, o capítulo e o tempo. A miniatura só deu para conferir no aparelho.
9. Settings > Highlight color: escolher outra cor (por exemplo Teal). A aba ativa, o progresso, os filtros mudados, o foco da TV e, ao abrir um vídeo, a barra do player mudam para ela. Fechar e abrir o app: continua a mesma.
10. A fresta da aba ativa e o foco dos cards na TV não têm mais brilho em volta; a porta do canto continua com o brilho.
11. Watch later: sem ícones à direita. Segurar um vídeo (vibra) e arrastar muda a ordem; soltar sem arrastar não abre o vídeo. Deslizar para a direita leva ao topo ("Moved to the top"); para a esquerda tira, com Undo. Conferir no app do YouTube.
12. Desempenho: o vídeo começa mais cedo (no log, "first frame of … in … ms"), e rolar o Watch later grande fica liso, sem perder a nitidez das miniaturas.

## Como testar a 0.5.6

1. TV: abrir um vídeo e mostrar os controles. A barra de tempo fica numa linha só dela, em cima dos botões.
2. Com o controle: na barra, esquerda e direita avançam e voltam; **baixo** vai para os botões (Skip ahead, legenda, resolução, lua), e esquerda e direita andam entre eles sem mexer no vídeo. **Cima** volta para a barra.
3. Na lua, escolher Brightness 25%. Abrir outro vídeo (ou o próximo da playlist): continua em 25%. Fechar o app de vez (ou desligar a TV) e abrir de novo: o vídeo volta claro.

## Como testar a 0.5.5

1. Histórico: deslizar um vídeo para a esquerda. O aviso "Removed from history" aparece com "Undo"; tocar em Undo devolve o vídeo ao mesmo lugar, e no app do YouTube ele continua no histórico. Sem tocar, depois de uns 5 segundos ele sai da conta.
2. Watch later (ou uma playlist sua): o mesmo com "Removed from…" e Undo.
3. Watch later: abrir e arrastar um vídeo pela alça (à direita) para outro lugar, sem tocar em botão nenhum antes. Conferir no app do YouTube em alguns segundos. Arrastar vários seguidos também.
4. Watch later: tocar na seta de um vídeo lá embaixo. Ele vai para o primeiro lugar, com o aviso "Moved to the top"; conferir no app do YouTube.
5. Watch later: segurar a miniatura não faz nada (nem aviso). Em outra playlist sua, segurar ainda salva no Watch later.
6. Watch later: rolar rápido até bem embaixo. Os vídeos já estão lá antes de chegar no fim, sem parar para carregar a cada página.
7. Fechar o app e abrir o Watch later (ou o Histórico) de novo: a lista aparece na hora, com o indicador de atualizar por um instante no topo. A alça e a seta aparecem quando ele some.
8. TV: na playlist, "Reorder" abre o modo mover na hora (sem "Loading the whole playlist"); OK pega, cima e baixo movem, OK solta e já grava. "Done" sai.

## Como testar a 0.5.4

1. Tablet de 14,6" em pé: 3 vídeos por linha em Subscriptions, Recommended, Explore e na busca. Deitado, continua com mais colunas.
2. Recommended (e as outras abas): puxar para atualizar. A lista nova aparece no lugar, sem os cartões voando. Tirar um vídeo do Histórico ainda faz os de baixo subirem deslizando.
3. TV: o banner na lista de apps, com o nome nítido e a luz no chão sumindo aos poucos, sem corte. No celular, o ícone novo (a mesma porta) na tela inicial e em Settings > App icon, nas sete cores.

## Como testar a 0.5.3

1. Explore: na primeira vez, alguns segundos com o indicador de carregar, e então vídeos de uns 10 canais que você não segue. Os itens 1 a 6 de "Como testar a 0.5.2" valem a partir desta versão.
2. Fechar o app (tirar dos recentes) e abrir de novo: as Inscrições aparecem na hora, com o indicador de atualizar no topo, e trocam pela lista nova quando ela chega.
3. Os cards das Inscrições mostram a foto dos canais inscritos (antes, muitos mostravam só a inicial).
4. Uma playlist inscrita com um vídeo novo de um canal que você segue: o vídeo aparece uma vez só, pelo canal.

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
3. *(Até a 0.5.4; a 0.5.5 tirou o modo de reordenar.)* Celular: Watch later > ícone de setas > arrastar um vídeo pela alça (as três linhas à direita) ou segurar e arrastar. "Save order" e conferir no app do YouTube.
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
7. TV: o foco aumenta o card, com borda creme. Segurar o OK num vídeo e soltar abre o menu com "Save to Watch later" (ou "Remove from…") e "Go to channel". "Voltar" fecha o menu.
8. Atualizar: no celular, puxar a lista para baixo (aparece o círculo âmbar logo abaixo da barra de cima). Na TV, segurar OK na aba Recommended e soltar: a lista recarrega.
9. Ícone: em Settings > App icon, a sétima opção, "Red".

## Como testar as etapas 3 a 5

1. Atualizar pelo Obtainium. **O app deve abrir já com o login** (antes pedia o código de novo depois de atualizar). Se ainda pedir, avise: aí a causa é outra.
2. Recomendações: aba "Recommended". Não deve ter Shorts, lives, esportes nem política, e vídeos de tecnologia, engenharia, computação e IA vêm primeiro.
3. Busca: lupa no celular, aba "Search" na TV. Testar as sugestões, e os filtros juntos (por exemplo "Over 20 minutes" + "Videos" + "Most viewed"). Abrir um canal e uma playlist dos resultados.
4. Canal: na página do canal, as abas Videos e Playlists. Tocar "Block channel" e ver o canal sumir da busca e das Recomendações; desbloquear em Settings > Blocked channels.
5. Histórico: aba "History".
6. Salvar: no player, o marcador ao lado de "Subscribe" põe o vídeo no Watch later (confira no app do YouTube). Em Settings > Save button dá para trocar a playlist.
7. *(Até a 0.5.4; a 0.5.5 tirou o modo de reordenar.)* Reordenar: abrir o Watch later (ou uma playlist sua), tocar no ícone de setas (celular) ou "Reorder" (TV), mudar a ordem e tocar "Save order". Conferir no app do YouTube. Na TV: OK pega, cima e baixo movem, OK solta.
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
- **Imagens:** Coil 3 sobre o OkHttp 3.12 do motor, por um adaptador próprio (`ui/ImageHttp.kt`, um `NetworkClient` do `coil-network-core`), em HTTP/2. O `coil-network-okhttp` exigiria OkHttp 4+, que conflita com o motor. Até a 0.5.6 era o Ktor (HttpURLConnection, HTTP/1.1). As que chegam da rede ou do disco entram com esmaecer de 120 ms; as que vêm da memória aparecem direto.
- **Player (Media3):** o motor gera um MPD (`createMpdStream`) com os formatos DASH; o app faz o parse com o `DashManifestParser` e toca. Ordem de escolha igual à do SmartTube: lives por HLS ou DASH do YouTube; vídeos pelo MPD; depois arquivo único.
  - **SABR:** o Media3 não toca (o SmartTube usa um fork próprio do ExoPlayer). Se um cliente do YouTube só entrega SABR, ou falha, o app troca de cliente (até 6 tentativas). Erros de rede no player (ex.: 403) também trocam de cliente e retomam do mesmo ponto (até 3 vezes).
  - **Áudio original:** o motor rotula cada faixa como "idioma (tipo)", com tipo original, dubbed, dubbed-auto, descriptive. O Media3 preferiria o idioma do sistema (a dublagem em português), então o app define a faixa original como preferida e, se ainda assim vier outra, força a original.
  - **Qualidade máxima:** `forceHighestSupportedBitrate`, sem limite de tamanho de tela. Os parâmetros voltam ao padrão a cada vídeo. A regra do app (`QualityRules`) entra já na escolha do Media3, por um `DefaultTrackSelector` próprio (`QualitySelector`); o `pickQuality` continua tratando a escolha à mão e as descidas. No Auto acima de 1080p (o 4K da TV), o primeiro pedaço do vídeo vem numa faixa menor (ver "Começo do 4K").
- **V8 só no aparelho:** o motor decifra as URLs com o j2v8, que só tem binários para Android. Nos testes no JVM (Robolectric) dá para validar busca e playlists, mas não os formatos de vídeo.
  - Teste online: `./gradlew :app:testAppDebugUnitTest -Pytslim.online=true --tests '*EngineOnlineTest*'`.
- **Busca:** todos os itens vêm com tipo indefinido (-1). Canal: `channelId` "UC…" e sem `videoId`. Playlist: ou `channelId` = `VL<id>`, ou o `playlistId` junto com o `videoId` do primeiro vídeo (e duração 0, com "16 lessons" no badge); por isso o app trata como playlist todo item com `playlistId`. Vídeos da busca vêm sem `channelId`, e o `author` traz "Canal • views • data".
  - O primeiro grupo da resposta é o dos resultados (o único com continuação); os outros são prateleiras ("People also watched", Shorts…), que ficam de fora.
  - **Filtros:** o motor só conhece uma tabela pequena de combinações (`SearchFilterHelper`); duração + tipo, por exemplo, virava "sem filtro". O app monta o parâmetro de filtros ele mesmo (`SearchParams`, um protobuf em base64, o mesmo `sp` do site) e faz a busca pela API do motor por um atalho em Java (`EngineSearch.java`), reaproveitando a leitura da resposta e a paginação do motor. Esse atalho usa classes "internal" do motor: se uma atualização do motor quebrar a compilação ali, é o primeiro lugar a olhar.
- **Canal:** o motor devolve a página em grupos: "Videos" (com paginação), "Shorts", "For You" e seções que costumam ser playlists (todos os vídeos com o mesmo `playlistId`, ex.: "Physics"). O app usa o primeiro grupo com vídeos que não são Shorts como "Videos" e as seções-playlist como "Playlists".
- **Chamadas que o motor não tem** (mover vídeo, ler `setVideoId`): `Innertube.post` usa o cliente HTTP do próprio motor (`EngineHttp.java`), que já põe o login nos cabeçalhos, e os corpos de pedido do motor (`PostDataHelper.createQueryTV`/`createQueryWeb`).
- **Login perdido nas atualizações (corrigido na 0.3.0):** o serviço de login do motor só é criado quando alguém pede, e aí carrega a conta salva numa thread própria. Na primeira abertura depois de uma atualização, o app perguntava "tem login?" antes de a conta carregar, ouvia "não" e mostrava o código. Agora o `Engine.init` cria o serviço logo no começo, e o `AuthRepository.isSigned()`, se há conta salva, espera o motor terminar de carregar (até 2 s).
- **APK:** ~52 MB em debug e ~28 MB em release (com R8; era ~43 MB sem), só com arm64-v8a e armeabi-v7a (o j2v8 tem ~30 MB por ABI, comprimido no APK). Para emulador, acrescentar x86_64 em `app.abis`.
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
- **Desempenho (0.5.7, 2026-11-03):** o levantamento completo está em https://claude.ai/artifact/WvrQg4nVqexDtmtHdwZkPd. Nada aqui muda resolução, bitrate, codec ou a nitidez das miniaturas.
  - **Vídeo pedido no toque:** o card chama `PlaybackRepository.prefetch` antes de abrir o player, e o `load` do mesmo vídeo espera esse pedido (valia por 60 s; desde a 0.6.4, por 10 minutos, pelo `EarlyRequests`; uma vez só). Abrir a tela do player leva de 0,2 a 0,5 s, que agora correm junto com o pedido ao motor.
  - **Qualidade certa de primeira:** antes, o Media3 escolhia a maior faixa que o aparelho declara, o vídeo começava a baixar por ela e o app trocava em seguida (no celular, do 4K para o 1080p), baixando o começo duas vezes. Agora o `QualitySelector` aplica a regra na primeira escolha.
  - **Extras depois do primeiro quadro:** o gráfico do Skip ahead, like/inscrito e o Save esperam o primeiro quadro (até 4 s), para não disputar rede e processador com o começo do vídeo.
  - **Próximo da sequência:** 40 s antes do fim, o próximo vídeo da playlist (`PlayQueue.peek`) é pedido ao motor.
  - **Buffer (`LoadRules`):** no celular e no tablet, os últimos 15 s vistos ficam guardados (voltar 10 s não baixa de novo; uns 8 MB em 1080p). Na TV, teto de 80 MB à frente: o padrão do Media3 guarda 50 s, que em 4K chegam a ~130 MB.
  - **HTTP do vídeo (`MediaHttp`):** com o HttpEngine do sistema (Android 14, ou 12 e 13 com a extensão S 7), HTTP/2 e QUIC, com o que ele aprende dos servidores guardado em `cache/httpengine`. Sem ele (a TV Hisense, Android 11), o `DefaultHttpDataSource`, como antes.
  - **Miniaturas:** o adiantamento cobre só os 24 vídeos logo depois da tela, conforme rola (`PrefetchThumbnails` com o estado da lista). Antes, cada página adiantava a lista inteira, e no Watch later eram centenas de uma vez. Pedidos iguais ao mesmo tempo baixam uma vez só (`DeDupeConcurrentRequestStrategy`).
  - Os filtros das Inscrições e da Explore rodam fora da thread da tela (`flowOn(Dispatchers.Default)`), como já era nas Recomendações e na busca.
  - **Compilação:** a cada atualização, o Android descarta o código compilado e volta a compilar quando o aparelho fica parado. O `src/main/baseline-prof.txt` marca o app e o motor inteiros para compilar junto com o que as bibliotecas já trazem, e o app escreve no log (tag Doorglow, 10 s depois de abrir) se já está compilado. Para medir desempenho logo depois de instalar: `adb shell cmd package compile -m speed-profile -f app.doorglow`.
  - **R8 ligado no release** (`proguard-rules.pro`): otimiza Compose, Media3, Kotlin e o app, e tira o que não é usado (o `material-icons-extended` sozinho tinha 11.400 classes das 43.851 do APK; o app usa 16 ícones). O motor e as bibliotecas dele (Gson, JsonPath, Retrofit, OkHttp, RxJava, V8, Cronet) ficam inteiros, porque leem as respostas do YouTube por reflexão. Sem ofuscar nomes (`-dontobfuscate`), para os logs continuarem legíveis. Se algo do motor quebrar só no release, a primeira suspeita é uma regra faltando ali.
  - Ficou com a thread do visual (mesmos arquivos): calcular o desfoque do vidro em 1/3 da resolução (`HazeInputScale.Auto`) e um brilho de foco da TV mais barato que o glow do tv-material.
- **Começo do 4K (2026-11-04, pedido do Johan):** na TV o vídeo demorava a começar por ir direto para o 4K, e ele topou começar menor e trocar logo depois. O `QualitySelector` entrega ao Media3 duas faixas do mesmo grupo e codec: o alvo da regra e a de partida (`QualityRules.start`: a maior até 1080p com o mesmo HDR e, se houver, os mesmos quadros por segundo). A `StartLowSelection` pede nela só o primeiro pedaço do vídeo (uns 5 s, que pesam umas 5 vezes menos que em 4K) e todos os outros no alvo. A troca é sem parar: os pedaços seguintes já chegam em 4K, e o decodificador é configurado desde o início para o tamanho maior. Vale também depois de pular para um trecho que não estava carregado (de novo uns 5 s menores ali). Só no Auto acima de 1080p: no celular (1080p) e com a resolução escolhida à mão, nada muda. O botão de qualidade mostra o alvo. No log (tag Doorglow): `first frame of … (… 1080p going to 2160p …)` e `video now 3840x2160 at N ms (same decoder)`; se aparecer `decoder recreated`, o decodificador da TV não troca de tamanho sozinho e a troca dá uma piscada.
- **Sequência e modo dormir (0.4.1):** a sequência é um objeto do app (`PlayQueue`), montado pela página da playlist ao abrir o vídeo e limpo por qualquer outro jeito de abrir. O mesmo player troca de vídeo no fim (`PlaybackController`, no `STATE_ENDED`). Escurecer usa o `overlayFrameLayout` do `PlayerView`, que fica entre o vídeo (com a legenda) e os controles. Desligar usa `AccessibilityService.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)`, que chama o `PowerManager.goToSleep` do sistema; na TV isso é o mesmo que o botão de desligar do controle.
- **Tela desligada (2026-11-11):** a tela do player continua dona do `ExoPlayer` (`PlaybackController`); o `PlaybackService` (um `MediaSessionService` do Media3 1.11, com `foregroundServiceType="mediaPlayback"`) só põe uma `MediaSession` sobre ele. O serviço começa no `onStart` do player, com a tela na frente (o Android 12+ não deixa começar com o app por trás), e o Media3 o põe em primeiro plano quando o vídeo toca; sai no `onDestroy`, antes do `release` do player. Na TV ele não é criado.
  - **Quem decide** é o `onStop` do player: com `PowerManager.isInteractive` falso (a tela apagou) e o vídeo tocando, não pausa (`ScreenOffRules`). Home deixa a tela acesa: abre o mini player (e então não há `onStop`) ou, sem ele, pausa.
  - **Sem imagem:** `setTrackTypeDisabled(TRACK_TYPE_VIDEO)` no `onStop` e de volta no `onStart` (`PlaybackController.setBackground`). Os parâmetros de cada vídeo novo (`TrackRules.defaults`) mantêm o vídeo desligado, então a sequência continua só com o som. Com a imagem desligada, as paradas para carregar não contam para descer a resolução.
  - **Acordado:** `setWakeMode(WAKE_MODE_NETWORK)` (processador e Wi-Fi enquanto toca) e um `WakeLock` próprio de até 60 s entre um vídeo e o próximo, porque no fim do vídeo o player solta o dele e o próximo ainda é pedido ao YouTube.
  - **Título e canal na notificação:** o stream do cliente de TV vem sem eles, que chegam depois (card, sequência, página do vídeo). Trocar o `MediaItem` do player (`replaceMediaItem`) quebraria: o `DashMediaSource` montado a partir do MPD não tem URI e o `canUpdateMediaItem` dele falha. Então a sessão vê o player por um `ForwardingSimpleBasePlayer` (`SessionPlayer`) que acrescenta título, canal e a miniatura `mqdefault` (16:9, sem faixas pretas).
  - **Tocar na notificação:** volta para o player com `SINGLE_TOP`. Se o player estiver em outra tarefa (um link aberto pelo WhatsApp entra na tarefa do WhatsApp), abre uma tela vazia do player que só chama `moveTaskToFront` para a tarefa dele e fecha (permissão `REORDER_TASKS`).
  - O Android 13+ não pede a permissão de notificações para a notificação de uma sessão de mídia. O ícone pequeno (`ic_notification`) sai do `tools/make_icons.py`.
- **Mini player (2026-11-11):** o picture-in-picture do Android na própria tela do player (`supportsPictureInPicture`), sem outra tela nem outro player. A forma é a do vídeo, limitada de 1:2,39 a 2,39:1 (`MiniPlayerRules.shape`), e a janelinha sai de onde o vídeo está (`sourceRectHint`, o `exo_content_frame`). Do Android 12 em diante ela abre sozinha (`setAutoEnterEnabled`, atualizado a cada tocar/pausar/fim); até o 11, pelo `onUserLeaveHint`. Voltar e o botão do canal fecham o player antes (`finish`), o que desliga a abertura sozinha.
  - Os botões da janelinha vêm da sessão de mídia do `PlaybackService` (o Android os tira dela quando o app não manda os seus).
  - Ao entrar na janelinha, o Android põe o player numa tarefa só dele (se havia outras telas na tarefa) e o devolve à tarefa de antes ao voltar (Android 12+). Fechada a janelinha, o Android só esconde a tela atrás do início: o player percebe (o aviso de saída chega com a tela já parada) e fecha, com `finishAndRemoveTask` quando está sozinho na tarefa.
  - O toque na notificação com a janelinha aberta: o `backIntent` (`NEW_TASK` + `SINGLE_TOP`) acha a tarefa da janelinha, cuja raiz é o player, e o Android a abre de volta.
- **Vídeos novos em cima (0.6.3):** o `LazyVerticalGrid` acompanha o primeiro vídeo visível pela chave: com vídeos novos inseridos antes dele, ele continua no topo e os novos ficam acima, fora da tela (o `ListTopTest` mostra isso). O `ListTop` lê, na composição e antes da grade medir a lista nova, se ela estava no começo, e então chama `requestScrollToItem(0)`, que esquece a chave. A lista precisa chegar como parâmetro (como em todas as telas): lida direto dentro do conteúdo da grade, ela é medida antes da recomposição, e o pedido chega tarde.
- **Volta ao app (0.6.3):** o `AppVisibility` conta as telas do app visíveis (`ActivityLifecycleCallbacks`, `SystemClock.elapsedRealtime`, que conta o tempo da TV dormindo); quando a primeira volta depois de nenhuma, diz quanto tempo o app ficou fora. Girar a tela não conta. O `FeedViewModel` recarrega com 10 min ou mais (`FeedRules.refreshesAfterAway`) e marca a lista para voltar ao começo (`VideoListContent.jumpToTop`, dentro do mesmo estado da lista para chegar junto com ela).
- **Canal pela página do vídeo (0.6.3):** os cards das Recomendações vêm sem o `channelId`. O "Go to channel" (e o nome do canal no celular) chama `VideoActions.channel`, que lê a página do vídeo (`getMetadata`, a mesma do like) e guarda os últimos 200 canais achados. O SmartTube faz o mesmo.
- **Compilar:** `./gradlew :app:assembleAppRelease` gera `app/build/outputs/apk/app/release/app-app-release.apk`, com o R8 (desde a versão do pacote de desempenho), assinado com a chave das releases se `-Pdoorglow.keystore=` for passado, senão com a de debug da máquina. Os submódulos precisam estar baixados: `git submodule update --init`.
- **Desempenho (0.6.4, 2026-11-13), o começo do vídeo.** Achado lendo o app e o motor, sem medição nos aparelhos (as TVs e o celular não responderam ao ADB no dia). Nada foi mudado nos submódulos.
  - **Onde o tempo vai, antes do primeiro quadro:** (1) o pedido ao motor (`getFormatInfo`), que para o cliente de TV do YouTube (o primeiro da lista do motor) precisa do **poToken** (`PoTokenGate`, gerado pelo `TvPoTokenProvider` num WebView com três pedidos ao YouTube e guardado por horas; o motor o gera dentro do pedido do primeiro vídeo), do `/player` e de decifrar o "n" dos endereços (`TclChallengeProvider`: um V8 novo a cada vídeo, e o player do YouTube, uns 2 MB, baixado na primeira vez de cada abertura do app, mesmo com o decifrador já guardado em disco); (2) o Media3 abrir a fonte, escolher as faixas e baixar o primeiro segundo. O `Engine.service.refreshCacheIfNeeded()` da 0.4.1 só lia a página do YouTube e criava o decifrador: não gerava o poToken, não baixava o player nem carregava o V8.
  - **`EngineWarmup` / `EngineWarm.java`:** roda, por trás, as peças na ordem em que o pedido de um vídeo as usa: `AppService.refreshCacheIfNeeded(client)`, `YouTubeSignInService.checkAuth()`, `PoTokenGate.getPoToken(client)` (só se venceu) e um `bulkSigExtract` com o mesmo valor de exemplo que o motor usa para conferir o decifrador (só uma vez por player do YouTube; um valor inventado poderia fazer o motor jogar fora o decifrador guardado). Começa 1,5 s depois que as listas aparecem (o WebView é criado na thread da tela) e a cada volta ao app (`AppVisibility.returns`). O Java é porque o `PoTokenGate` e o `AppClient` são `internal` do Kotlin do motor. Um vídeo pedido no meio disso espera só a peça em andamento (o motor já tem as travas).
  - **`EarlyRequests`:** os pedidos feitos antes de o player precisar: no toque (`asked`, o `prefetch` de antes) e, na TV, com o foco parado 600 ms num card (`focused`/`unfocused`, no `VideoCard` do `TvUi`; lives e estreias ficam de fora). O motor atende um pedido por vez (`FormatInfoWrapper` tem uma trava), então há **um palpite de cada vez**, e um palpite faz uma tentativa só, sem trocar de cliente do YouTube. Um palpite só vale se trouxe DASH ou HLS; senão o OK pede de novo. Os últimos 3 ficam por 10 minutos, cada um usado uma vez. O pior caso: parar num card, ir para outro e apertar OK na hora faz o vídeo escolhido esperar o palpite do anterior terminar (o pedido ao motor não dá para cancelar no meio).
  - **Legenda (`LenientDataSource`):** a legenda é uma fonte junto com o vídeo (`MergingMediaSource`), e o Media3 só escolhe as faixas depois que todas as partes abrem. A da legenda abria esperando a resposta do `timedtext` (conexão nova a outro servidor). Agora o `open` volta na hora e o pedido sai na primeira leitura; com a legenda desligada, o Media3 cancela a leitura.
  - **Tela de carregando (`PlayerLoading`, `LoadingRules`):** uma camada no `overlayFrameLayout` do PlayerView, por baixo do escurecer do modo dormir. O `show_buffering` do Media3 ficou em `never`. A miniatura vem do card (extra `thumbnail` do `PlayerActivity.intent`, e `Video.thumbnailUrl` na sequência) ou, sem card, é a `mqdefault` do vídeo.
  - **O que ficou de fora:** trocar o cliente do YouTube que o motor usa primeiro (os outros têm menos qualidades ou não tocam com a conta); pedir o vídeo no toque do dedo no celular antes de soltar (rolar a lista dispararia pedidos); e guardar um V8 pronto entre os vídeos (está dentro do motor).
