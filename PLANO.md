# ytslim — cliente do YouTube enxuto (celular, tablet e TV)

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
- **Fora do app:** envio de vídeos (upload), Shorts.

## Telas

| Tela | Notas |
|---|---|
| **Início = Inscrições** | Feed das inscrições como tela principal. |
| Recomendações | Secundária, com exclusão e preferência de tópicos (ver Filtragem). |
| Busca | "Melhorada": filtros de duração, data, tipo e ordenação. Lives só aparecem aqui. |
| Playlists | Inclui **Assistir mais tarde** (o real, que o motor acessa). Reordenar vídeos, inclusive pela TV. |
| Histórico | Lista do que foi assistido. |
| Canal | Página básica do canal (vídeos, playlists). |
| Configurações | Mínimas: filtros, tópicos, canais bloqueados, conta. |

## Filtragem

- **Shorts:** nunca aparecem, em lugar nenhum.
- **Lives:** só nos resultados de busca, nunca nas recomendações. Inscrições: *a definir* (padrão proposto: esconder, com opção para mostrar).
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

## Etapas

0. **Base do projeto:** submódulos, build Gradle e um APK que abre no celular e na TV.
1. **Protótipo do motor:** login por código, listar as playlists e tocar um vídeo em qualidade máxima com áudio original. *Valida o MediaServiceCore + Media3 antes de construir telas.*
2. **Inscrições (Início) + player completo:** controles mínimos, legenda, regra de qualidade, pular partes.
3. **Playlists:** salvar com um toque, reordenar (inclusive na TV).
4. **Busca com filtros, Histórico e Canal.**
5. **Recomendações** com exclusão e preferência de tópicos.
6. **Polimento visual** e rotina de atualização do motor.

Extensão do Firefox: deixa de ser necessária para o "Assistir mais tarde", porque o motor acessa o real. Fica como ideia opcional para limpar o site.

## Perguntas em aberto

- Lives no feed de Inscrições: esconder ou mostrar?
- Comentários, chat ao vivo, notificações, like/dislike, inscrever/cancelar inscrição: quais ficam?
- Nome e ícone do app.

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
