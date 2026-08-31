# 030-01 — Auditar e comparar superfícies com Google Health

- **Ticker:** `030`
- **Número:** `01`
- **Status:** `completed`

## Objetivo e resultado esperado

Inventariar todas as superfícies produtivas e registrar benchmark sanitizado do
Google Health/Fitbit, incluindo shell, navegação visual, hierarquia, cards,
listas, controles, estados, gráficos, densidade e interação observáveis. A
subtarefa só poderá ser marcada como concluída após matriz integral por tela ser
conferida e aceita.

## Requisitos e critérios cobertos

- R1, R2, R4, R6 e critérios 1–4 e 8 da Spec 030.
- Confirmar que períodos públicos/instalados são referência visual, não contrato
  para criar dados ausentes.

## Escopo incluído

- `core/designsystem`, `core/navigation` e `core/designsystem/chart`;
- Home, Saúde, Calendário, Exercícios, Evolução, Dispositivo, Perfil e
  Configurações;
- referências públicas e app `com.fitbit.FitbitMobile` quando disponível;
- matrizes de gaps por superfície e por gráfico.

## Escopo excluído

- código de produção;
- alteração de períodos, domínio, Room, fluxos ou componentes produtivos;
- screenshots com dados pessoais;
- testes automatizados.

## Regra de referência e implementação

A interface do Google Health/Fitbit pode ser replicada fielmente nesta
subtarefa, incluindo layout, geometria, espaçamento, navegação visual, controles
de período aplicáveis à superfície, gráficos, cores e comportamento de
apresentação. Não copiar código-fonte ou APK, dados pessoais ou credenciais,
nem reutilizar arquivos ou ativos extraídos do app de referência; a
implementação deve usar código próprio no Champion Companion.

## Dependências

- Nenhuma.

## Arquivos e símbolos prováveis

- `docs/specs/030-refinamento-graficos-google-health.md`;
- `docs/tasks/030-refinamento-graficos-google-health/030-00-overview.md`;
- `core/designsystem/chart/*`;
- `feature/health/*Chart.kt`;
- `feature/exercises/ExercisesScreen.kt`;
- `feature/evolution/EvolutionScreen.kt`.

## Observação real do app de referência

Em 2026-08-31, o app instalado `com.fitbit.FitbitMobile` foi aberto no Galaxy
S22 autorizado, sem login, troca de conta, escrita, sincronização, medição ou
alteração de dados. A inspeção estrutural temporária observou:

- resumo dominante no overview e cards compactos de métricas;
- detalhe de Peso com seletor `D`/`S`/`M`/`3M`/`A`, navegação temporal, valor
  destacado, marker vertical, eixo Y discreto à direita e resumos agrupados
  abaixo da série.

Essa observação é referência de padrão, não evidência de que cada métrica do
Champion ou cada período do app de referência tenha sido aberto. Nenhum
screenshot, dump, valor ou identificador pessoal foi versionado.

## Matriz objetiva por consumidor

A matriz cruza código auditado no Champion com a observação real acima. O período
alvo é uma convenção visual; somente consultas e granularidades já suportadas
podem ser usadas. `Ocultar/desabilitar` significa não apresentar a opção ou,
se o componente comum exigir o conjunto completo, mantê-la sem ação e sem
query nova.

| Consumidor e código auditado | Período atual | Período-alvo e controle visual | Granularidade | Gaps e ausência | Interação | Fonte e limitação |
|---|---|---|---|---|---|---|
| **Passos** — `StepsHistoryShell.kt`, `StepsHistoryChart.kt`, `StepsHistoryFeatureContract.kt` | `CalendarHistoryPeriod.LAST_7_DAYS` (padrão), `LAST_30_DAYS`, `LAST_90_DAYS`; UI atual `7D/30D/90D`. | `S` → 7 dias; `M` → 30; `3M` → 90. `D` e `A` ocultos/desabilitados, sem callback e sem query nova. | Uma barra por dia em `CalendarStepsHistorySnapshot.entries`; valor diário de passos. | `steps == null` permanece lacuna; barra é omitida, lista mantém `Sem dados` e nenhum zero é criado. Falha de fonte mantém estado seguro. | `SelectPeriod` existente; marker mais próximo no renderer; scroll horizontal somente acima de 7 entradas; tela e lista continuam com rolagem vertical/paginação. | Resumos diários locais (`StepsHistoryViewModel`/`CalendarHistoryPeriod`). Não há contrato intradia ou anual. A observação real de referência sustenta apenas resumo, controle e densidade, não paridade de dados. |
| **Distância** — `DistanceHistoryShell.kt`, `DistanceHistoryChart.kt`, `DistanceHistoryFeatureContract.kt` | `CalendarHistoryPeriod.LAST_7_DAYS` (padrão), `LAST_30_DAYS`, `LAST_90_DAYS`; UI atual `7D/30D/90D`. | `S` → 7 dias; `M` → 30; `3M` → 90. `D` e `A` ocultos/desabilitados, sem callback e sem query nova. | Uma barra por dia em `CalendarDistanceHistorySnapshot.entries`; distância estimada diária em km. | `estimatedDistanceMeters == null` permanece sem estimativa; barra é omitida, lista e agregados preservam ausência e não usam zero. | `SelectPeriod` existente; marker mais próximo; scroll horizontal acima de 7 entradas; rolagem vertical/paginação da superfície. | Estimativas de resumos diários locais. Não há fonte intradia ou anual; a referência real observada não autoriza criar esses dados. |
| **Frequência cardíaca histórica** — `HealthMetricDetailScreens.kt`, `HeartRateHistoryChart.kt`, `HealthFeatureContract.kt` | `HeartRateHistoryPeriod.TODAY` (padrão), `LAST_7_DAYS`, `LAST_30_DAYS`; UI atual `Hoje/7 dias/30 dias`. | `D` → `TODAY` intradia; `S` → 7 dias; `M` → 30. `3M` e `A` ocultos/desabilitados, sem callback e sem query nova; não converter HR 7/30 em 90. | `TODAY`: amostras reais intradia. 7/30: buckets diários com média, mínimo, máximo e contagem de amostras; mapper preserva posições sem dado como nulas. | Dias sem amostra permanecem ausentes/nulos; snapshot vazio permanece vazio; nunca zero BPM ou interpolação. | `SelectHistoryPeriod` existente; marker mais próximo; scroll horizontal acima de 12 pontos; rolagem vertical da tela e lista paginada. | `HeartRateHistoryQuery` usa fontes locais manual, D1 e atividade de treino elegível; `RoomRepositories` confirma intradia em TODAY e buckets diários em 7/30. Não existe contrato HR de 90 dias ou ano. |
| **BPM no detalhe de treino** — `ExercisesScreen.kt: ActivityHeartRateChartSection` e `WorkoutHeartRateChart` | Janela fixa da atividade, definida por `WorkoutHeartRateSample.sampledAt`; não há período de pesquisa nem seletor. | Sem seletor `D/S/M/3M/A`; replicar fielmente a composição de resumo, unidade e série, com janela real do treino e sem query nova. | Uma amostra real de BPM por timestamp; séries separadas quando timestamps não podem compartilhar lane. | Lista vazia exibe estado indisponível; amostras ausentes não viram pontos sintéticos, linha contínua ou zero. | Marker mais próximo; scroll horizontal acima de 6 pontos; rolagem vertical do detalhe. Não iniciar fluxo phone. | Amostras da atividade importada do relógio (`WorkoutHeartRateSample`). Limites 30–220 BPM e ausência dependem do snapshot; GPS/localização estão fora. |
| **Distância canônica no detalhe de treino** — `ExercisesScreen.kt: ActivityCanonicalChartSection` e `WorkoutCanonicalDistanceChart` | Janela fixa entre início/fim efetivos de `CanonicalWorkoutSeries`; não há período de pesquisa nem seletor. | Sem seletor `D/S/M/3M/A`; mostrar janela efetiva da atividade, sem rederivar distância e sem query nova. | Um ponto por término de intervalo D4; distância canônica acumulada em km. | Série nula ou sem intervalos exibe indisponibilidade; intervalos precisam ser contíguos no contrato; sem interpolação ou suavização. | Marker mais próximo; scroll horizontal acima de 6 intervalos; rolagem vertical do detalhe. | `CanonicalWorkoutSeries` derivada e congelada no snapshot da atividade. Não usar distância/velocidade por passos de bicicleta, GPS ou sessão phone. |
| **Cadência D4 no detalhe de treino** — `ExercisesScreen.kt: ActivityCadenceChartSection` e `WorkoutCadenceChart` | Janela fixa da atividade; somente intervalos D4 elegíveis, sem período de pesquisa nem seletor. | Sem seletor `D/S/M/3M/A`; conservar janela e contexto do treino, sem query nova. | Um ponto no ponto médio de cada intervalo D4, em passos/min; somente série canônica elegível. | Série inelegível, ausente ou sem intervalos exibe indisponibilidade; sem interpolação, suavização, reamostragem ou pontos sintéticos. | Marker mais próximo; scroll horizontal acima de 6 intervalos; rolagem vertical do detalhe. | `DerivedWorkoutInterval`/`CanonicalWorkoutSeries`; bicicleta não recebe cadência por passos. Valores D4 e estado de plausibilidade permanecem explícitos. |
| **Tendências de Evolução** — `EvolutionScreen.kt`, `EvolutionFeatureContract.kt` | `EvolutionPeriod.LAST_7_DAYS`, `LAST_30_DAYS` (padrão), `LAST_90_DAYS`; UI atual expõe `7/30/90 dias` e filtro por modalidade. | `S` → 7 dias; `M` → 30; `3M` → 90. `D` e `A` ocultos/desabilitados, sem callback e sem query nova; manter filtro de modalidade e comparação existente. | Séries diárias com exatamente a janela de `EvolutionVolumeSnapshot`; métricas de tendência mapeadas sem mudar unidade. | `GAP`, `ABSENT` e `NOT_APPLICABLE` permanecem estados distintos; pontos nulos não viram zero; `failed`, vazio e `Não aplicável` continuam explícitos. | `SelectPeriod` e filtro de modalidade existentes; marker mais próximo; scroll horizontal acima de 6 pontos; rolagem vertical, comparação com janela anterior de mesmo tamanho. | `EvolutionReadModel` local, somente atividades elegíveis e filtro atual. Não há fonte anual nem autorização para ampliar read model ou criar dados. |

### Leitura da matriz

- Os históricos de passos e distância compartilham 7/30/90; isso não altera a
  política distinta de frequência cardíaca, que permanece TODAY/7/30.
- Evolução também usa 7/30/90, enquanto o detalhe de treino não pesquisa
  períodos: sua unidade de leitura é a atividade importada e sua janela
  efetiva.
- O padrão real observado (`D/S/M/3M/A`, resumo dominante, marker, eixo discreto
  e resumos sob a série) pode ser reproduzido fielmente na composição. Dados e
  contratos do Champion continuam limitadores semânticos, sem fabricar dados.

## Matriz objetiva de inventário por destino e subsuperfície

Esta matriz complementa a matriz gráfica preservada acima. A conferência foi
feita contra `AppDestination` e o `when` exaustivo de `ChampionNavDisplay` em
`ChampionNavigation.kt`, depois contra composição, callbacks, contratos de UI e
estados de cada superfície. “Gap” registra diferença ou limitação observada;
não é requisito funcional e não autoriza criar dado, período, destino ou fluxo.

| Destino/subsuperfície concreta | Composição, componentes e arquivos | Controles e ações | Estados e fluxos | Limites e semântica | Acessibilidade e adaptação | Fonte e limitação |
|---|---|---|---|---|---|---|
| **Shell — `AppDestination`/`ChampionNavigation`** | `AppDestination.kt` declara `Home`, `Health`, `HealthHeartRate`, `HealthWeight`, `HealthSteps`, `HealthDistance`, `Calendar`, `CalendarDay`, `Exercises`, `Evolution`, `ExerciseDetail`, `WorkoutTypeSelection`, `WorkoutPreparation`, `ActiveWorkout`, `WorkoutRecovery`, `Device`, `Profile` e `Settings`. `ChampionNavigation.kt` compõe `Scaffold`, `NavigationBar`, `DropdownMenu`, `AnimatedContent`, `AlertDialog` e encaminha cada destino para a tela correspondente. | Barra inferior: `Início`, `Saúde`, `Calendário`, `Exercícios`; `Mais`: `Dispositivo`, `Perfil`, `Configurações`; retorno por back stack/manual e botões contextuais; confirmação de saída para medição/importação e alterações não salvas. | Top-level preserva back stack manual; subárvores de Saúde, Calendário e Exercícios são removidas/repostas conforme destino. Transição slide/fade de 260 ms. Preparação/atividade phone são redirecionadas ou exibem shell descontinuado por `PhoneWorkoutPolicy`; recuperação permanece segura. | Não há `NavHost`/`NavController`; rotas são identificadores e `CalendarDay` aceita data inválida como estado seguro. Destinos phone históricos não reativam treino phone. Nenhuma PoC GPS/mapa é inventariada. | Ícones têm `contentDescription`; itens de navegação e menu usam rótulos; diálogos têm ações nomeadas. **N/A**: foco real, fonte ampliada, portrait/landscape e claro/escuro não foram exercitados fisicamente. | `AppDestination.kt`, `ChampionNavigation.kt`, `ChampionTheme.kt`, componentes em `core/designsystem/*`. Referência Google Health/Fitbit informa hierarquia visual, não contrato de navegação do Champion. |
| **Shell — tokens, cards, estados e gráficos compartilhados** | `ChampionTokens.kt` (espaçamento/formas/tipografia), `ChampionDisplayComponents.kt` (`HealthCard`, `MetricCard`, `HealthChart` legado), `ChampionActionAndStatusComponents.kt` (ações/status), `ChampionFeedbackComponents.kt` (vazio/erro/loading/banners), `ChampionCharts.kt`, `ChampionChartTheme.kt` e `ChampionChartContracts.kt`. | Slots de status, ações primárias, retry/dismiss, seleção de ponto mais próximo e scroll/zoom opt-in no contrato gráfico. | `LoadingState`, `EmptyState`, `ErrorState`, banners de permissão/revisão/calibração e badges distinguem estados visuais sem mudar domínio. Renderer omite `y == null`. | Boundary produtiva de séries é `core/designsystem/chart`; features não devem importar Vico nem criar Canvas de série. `HealthChart` é legado de preview/testes, sem consumidor produtivo confirmado. | Cards e gráficos expõem descrições semânticas em vários pontos; cobertura não é uniforme entre componentes. **N/A**: comportamento com fonte ampliada, orientação e contraste físico. | Código próprio Material 3/Vico e padrões observados; nenhum ativo, APK, dado ou credencial de referência foi copiado. |
| **Home — overview diário** | `HomeScreen.kt` (`HomeTopBar`, `HomeSummaryContent`, `HomeLatestHeartRateCard`, `HomeMetricCard`, `HomeRefreshStatus`, `HomeRefreshHint`, pendências) e `HomeFeatureContract.kt`; `HomeViewModel.kt` fornece `HomeUiState`. | Pull-to-refresh, ação de acessibilidade “Atualizar dados do relógio”, `Atualizar agora`, abrir Dispositivo, Perfil, Saúde/Frequência cardíaca e Exercícios. | `Loading`, `Empty`, `Ready`, `RefreshFailed`, `DeviceRequired`, `PermissionsRequired`, `BluetoothDisabled`; refresh tem `Preparing`, `Connecting`, `WaitingForSteps`, `Persisting`; feedback Health Connect é secundário. | Resumo local diário: passos/meta, distância e calorias podem estar indisponíveis; ausência não vira zero. Pendências priorizam calibração e navegam para Exercícios. | Resumo dominante e cards têm `contentDescription`; refresh usa live region/custom action; rolagem vertical. **N/A**: fonte ampliada, portrait/landscape e temas exercitados fisicamente. | Código atual de `feature/home`; padrão de overview/card compacto observado no Fitbit. Não há paridade de dados ou consulta nova. |
| **Saúde — hub** | `HealthScreen.kt` (`HealthHubHeader`, `HealthMetricSummaryCard`) e `HealthFeatureContract.kt`; quatro cards para frequência cardíaca, peso, passos e distância. | Toque em cada card para `HealthHeartRate`, `HealthWeight`, `HealthSteps` ou `HealthDistance`; carregamento inicial via `HealthIntent.Load`. | Valores disponíveis, sem dados, carregando e indisponível são apresentados por métrica; origem, unidade e atualização acompanham o valor. | Hub mostra apenas dados locais atuais; distância é estimativa do resumo diário e depende de dados pessoais. Não há ação de medição no hub. | Cards são botões com descrição de título, valor, estado, apoio e destino. **N/A**: orientação, fonte ampliada, claro/escuro e estados físicos não exercitados. | `HealthScreen.kt`, `HealthFeatureContract.kt`; apenas overview/Peso do app instalado foram observados diretamente; demais padrões são comparação estrutural limitada. |
| **Saúde — detalhe de frequência cardíaca** | `HealthMetricDetailScreens.kt` (`HeartRateDetailScreen`, `HeartRateDetailHeader`, `HeartRateOverviewCard`, `HeartRateSessionSurface`, `HeartRateD1ImportCard`, `HeartRateHistoryChartSection`, lista paginada) e `HealthFeatureContract.kt`; `HeartRateHistoryChart.kt`. | Medir, cancelar, retry/dismiss, pull-to-refresh/importação D1, seletor de histórico e paginação; diálogo confirma saída durante operação. | Medição: `Idle`, `Preparing`, `Connecting`, `Starting`, `Measuring`, `Stopping`, `Completed`, `Cancelled`, `Failed`. D1: `Idle`, `Preparing`, `Connecting`, `Reading`, `Persisting`, `Completed`, `CompletedWithGaps`, `Partial`, `Failed`, `Cancelled`. | Períodos efetivos `TODAY`, 7 e 30 dias; intradia usa amostras reais, 7/30 usam buckets diários; lacunas permanecem nulas. Medição manual dura 60 s; sem período de pesquisa no detalhe de treino. | Cabeçalho, progresso, live regions, resumo acessível e custom action de refresh. Paginação e rolagem vertical existentes. **N/A**: fonte ampliada/orientação/temas exercitados fisicamente. | `HealthMetricDetailScreens.kt`, `HeartRateHistoryChart.kt`, `HealthFeatureContract.kt`; referência direta do Fitbit observou seletor, marker e eixo discreto em Peso, não este fluxo inteiro. |
| **Saúde — detalhe de peso** | `HealthMetricDetailScreens.kt` (`WeightDetailScreen`, `WeightDetailHeader`, `WeightEditor`, `WeightMessageCard`, `WeightHistoryList`) e `HealthFeatureContract.kt`. | `Registrar peso`, editar campo em kg, `Salvar medição`, `Cancelar`, fechar aviso e voltar. | Loading, valor válido, histórico indisponível, sem dados, editor aberto, salvo, valor inválido e falha ao salvar. | Entrada manual local em quilogramas; data/hora atual automática; histórico preserva origem e medições. Não há seletor de período ou gráfico produtivo confirmado. | Campo tem label/suffix e teclado decimal; mensagem usa live region; lista é rolável. **N/A**: teste físico de fonte ampliada, landscape, portrait, claro/escuro e conteúdo longo. | `HealthMetricDetailScreens.kt`, `HealthFeatureContract.kt`; detalhe de Peso foi observado no `com.fitbit.FitbitMobile`, mas valores/dados não foram preservados. |
| **Saúde — detalhe de passos** | `StepsHistoryShell.kt` (`StepsDetailScreen`, resumo diário, `StepsHistoryShell`, resumo/lista) e `StepsHistoryChart.kt`; contratos `StepsHistoryFeatureContract.kt` e `HealthFeatureContract.kt`. | Seleção de período, retry, paginação da lista e retorno; progresso visual da meta diária. | Resumo presente/ausente; histórico loading, erro, parcial e disponível; lista vazia preserva ausência. | `LAST_7_DAYS`, `LAST_30_DAYS`, `LAST_90_DAYS`; uma entrada por dia; `steps == null` é lacuna e não zero. | Progresso tem `ProgressBarRangeInfo`; períodos têm seleção/descrição; gráfico possui resumo e lista acessível. **N/A**: fonte ampliada/orientação/tema não exercitados fisicamente. | `StepsHistoryShell.kt`, `StepsHistoryChart.kt`, `StepsHistoryFeatureContract.kt`; sem contrato intradia/anual e sem autorização para criá-los. |
| **Saúde — detalhe de distância** | `DistanceHistoryShell.kt` (`DistanceDetailScreen`, resumo, `DistanceHistoryShell`, lista) e `DistanceHistoryChart.kt`; `DistanceHistoryFeatureContract.kt`. | Seleção de período, retry, paginação e retorno. | Resumo sem dados/estimativa, histórico loading, indisponível, parcial, estimativas disponíveis e lista vazia. | `LAST_7_DAYS`, `LAST_30_DAYS`, `LAST_90_DAYS`; distância é estimativa diária do resumo; `estimatedDistanceMeters == null` permanece sem estimativa e não vira zero. | Períodos informam janela, lista informa unidade/origem/atualização e gráfico tem resumo acessível. **N/A**: fonte ampliada, orientação, temas e rolagem física não exercitados. | `DistanceHistoryShell.kt`, `DistanceHistoryChart.kt`, `DistanceHistoryFeatureContract.kt`; não há fonte intradia/anual nem dado equivalente observado diretamente na referência. |
| **Calendário — mês** | `CalendarScreen.kt` (`CalendarHeader`, `MonthHeader`, `MonthCalendar`, `CalendarDayCell`, marcadores e legenda) e `CalendarFeatureContract.kt`; `CalendarViewModel.kt`. | Mês anterior/próximo, `Hoje`, seleção de dia, `Abrir dia`, retry e rolagem da tela. | Loading, erro do mês, dados parciais, mês sem registros; célula distingue hoje/selecionada e presença de passos, BPM, atividade, pendência e falha. | Grade fixa de até 42 células, ordem semanal conforme locale, read model mensal local; indicadores usam forma e cor, com legenda. | Células expõem data, presença, seleção e estado; botões de mês têm descrições. **N/A**: fonte ampliada, portrait/landscape, temas e leitura física de indicadores não exercitados. | `CalendarScreen.kt`, `CalendarFeatureContract.kt`; preserva read model e refinamento da Spec 017. Google Health/Fitbit não define contrato de calendário do Champion. |
| **Calendário — detalhe diário** | `CalendarDayScreen.kt` (`DayHeader`, `DayOverviewCard`, cards de Passos, Distância, Frequência cardíaca, Atividades, Peso, fila local e problemas de fontes). | Voltar ao calendário, retry de detalhe/fontes e links para quatro detalhes de Saúde e `ExerciseDetail` por atividade. | Data inválida, loading, erro, vazio, panorama completo/parcial, problemas de fontes e fila sem operações/pendente/sincronizando/sincronizada/falha/parcial. | Data ISO inválida é estado seguro; distância não soma atividades; fila é informativa e não inicia sync; estados de ausência, zero e falha permanecem distintos. | Cabeçalhos e cards têm descrições/heading; status usa `stateDescription` e live region; rolagem vertical. **N/A**: fonte ampliada, orientação, conteúdo longo e temas físicos. | `CalendarDayScreen.kt`, `CalendarFeatureContract.kt`, `AppDestination.kt`; dados locais e links existentes limitam paridade funcional. |
| **Exercícios — overview e importação watch-first** | `ExercisesScreen.kt` (`ExercisesHeader`, `ExercisesEvolutionCard`, `ExercisesImportCard`, `ImportResultBanner`, `ImportErrorBanner`, `MetadataSection`) e `ExercisesFeatureContract.kt`; `ExercisesViewModel.kt`. CTA debug, quando fornecido, é excluído desta auditoria produtiva. | `Buscar treinos no relógio`, cancelar, retry, importar de novo, fechar/dispensar, abrir Evolução e abrir atividade. | Importação: `Idle`, `Preparing`, `Connecting`, `ReadingIndex`, `ReadingBlocks`, `Parsing`, `Persisting`, `Completed`, `Partial`, `Empty`, `Failed`, `Cancelled`; progresso, metadata, duplicados, inválidos, timeout e blocos preservados. | Importação somente por ação explícita, relógio associado obrigatório; watch-first, local-first, sem criar consulta, GPS ou treino phone. | Progresso e resultado têm live region/content description; ações nomeadas e rolagem. **N/A**: estados físicos completos, fonte ampliada, orientação e temas. | `ExercisesScreen.kt`, `ExercisesFeatureContract.kt`; referência só sustenta composição de overview/importação, não dados ou fluxo equivalente. |
| **Exercícios — lista, filtros e paginação** | `ExercisesScreen.kt` (`WorkoutFiltersSection`, diálogo `Selecionar filtro`, `ExercisesListSection`, `WorkoutActivityCard`, `ExerciseSyncFeedbackCard`). | Trocar filtro, selecionar `Todas`, revisão, calibração, prontas, sincronizadas, erro ou descartadas; abrir card; `Anterior`/`Próxima`. | Lista vazia, filtros com contagens, paginação de 5 atividades, estados de workflow, indicação de reprocessamento e feedback Health Connect. | Filtros são locais: `ALL`, `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCED`, `SYNC_FAILED`, `DISCARDED`; card não altera atividade ao ser aberto. | Cards têm ícone com descrição e estado textual; título da seção é levado à visualização após trocar página. **N/A**: fonte ampliada, orientação, conteúdo longo e temas físicos. | `ExercisesScreen.kt`, `ExercisesFeatureContract.kt`; densidade e hierarquia são auditadas contra padrão visual, sem exigir recurso inexistente. |
| **Exercícios — detalhe, resumo, intervalos e três gráficos** | `ExercisesScreen.kt` (`ExerciseDetailShell`, `ActivityHeaderCard`, `ActivityStatusOriginSection`, `ActivityMetricsSection`, `ActivityCanonicalChartSection`, `ActivityCadenceChartSection`, `ActivityHeartRateChartSection`, `ActivityIntervalsSection`, `ActivitySplitsSection`) e `ChampionCharts.kt`/contratos gráficos. | Voltar, ver intervalos/splits, abrir ações de revisão/calibração/reprocessamento/aceite/sync/descarte/exclusão conforme estado; marker e scroll do gráfico. | Loading, not found, failed, ready; estados canônicos `NOT_APPLICABLE`, `MISSING_STEP_LENGTH`, `NOT_MATERIALIZED`, `SERIES_INELIGIBLE`, `PLAUSIBILITY_BLOCKED`, `SPLITS_NOT_MATERIALIZED`, `AVAILABLE`. | BPM usa amostras reais; distância canônica usa término de intervalos D4; cadência usa ponto médio de intervalos elegíveis; bike usa distância manual opcional e não passos; gaps não são interpolados. | Cabeçalho/sections usam heading; gráficos têm unidades, descrições, resumo acessível, marker e lista/intervalos textuais. **N/A**: drag/tooltip, fonte ampliada, orientação, rolagem física e claro/escuro não exercitados. | `ExercisesScreen.kt`, `ChampionCharts.kt`, `ChampionChartContracts.kt`; renderer único Vico encapsulado, sem inferir paridade de dados da referência. |
| **Exercícios — revisão local** | `ExercisesScreen.kt` (`ReviewFormSection`, `WorkoutType` por radio, campos de distância/título/observações, checkbox de plausibilidade) e `ExercisesFeatureContract.kt`. | Escolher tipo, informar distância manual de bicicleta ou referência, editar título/notas, aceitar plausibilidade e concluir revisão. | `PENDING_REVIEW`, saving, erro de tipo/título/notas/distância, aceite de plausibilidade requerido/bloqueado; revisão pode encaminhar para calibração de esteira. | Alterações locais preservam identidade, fatos e snapshots; referência externa não é GPS; bicicleta não recebe métrica por passos. | Radio, checkbox e campos têm labels; erro fica textual; botão de ação usa loading/estado. **N/A**: teclado, fonte ampliada, landscape, conteúdo longo e temas fisicamente. | `ExercisesScreen.kt`, `ExercisesFeatureContract.kt`; contratos da Fase 14/16 limitam a semântica; gaps não viram novos campos/regras. |
| **Exercícios — calibração** | `ExercisesScreen.kt` (`CalibrationFormSection`, `ReferenceDistanceCalibrationSection`, prévias e `CyclingCalibrationBlockedSection`), `ExercisesFeatureContract.kt`; `ProfileScreen.kt` referencia histórico/calibração. | Informar distância real/externa, ver prévias, aplicar calibração, abrir/corrigir/remover histórico no Perfil. | `PENDING_CALIBRATION`; saving, validação/falha, bicicleta bloqueada; calibração de referência elegível apenas para watch RUNNING/WALKING com série válida e passos elegíveis. | Distância de referência é entrada manual externa; atualiza atividade/perfil futuro localmente; não inicia sync automática, não reescreve snapshots históricos e não coleta GPS. | Campos têm unidade e mensagens; prévias e métricas são textuais. **N/A**: validação física da adaptação a fonte/orientação e conteúdo longo. | `ExercisesScreen.kt`, `ExercisesFeatureContract.kt`, `ProfileScreen.kt`; Spec 023/ADR-022 definem limites semânticos, referência visual não autoriza nova calibração. |
| **Exercícios — sync e workflow** | `ExercisesScreen.kt` (`ReadOnlyWorkflowSection`, `ExerciseSyncFeedbackCard`, ações locais e diálogos) e `ExercisesFeatureContract.kt`; integração visual acionada pelo detalhe. | Confirmar plausibilidade, sincronizar, tentar novamente, descartar, excluir permanentemente, cancelar/confirmar ações. | `READY_TO_SYNC`, `SYNCING`, `SYNCED`, `SYNC_FAILED`, `DISCARDED`; estados de sync local e feedback Health Connect permanecem distintos. | Health Connect write-only, local-first e explícito; `SYNCED` significa escrita aceita localmente, não readback. Exclusão é irreversível e bloqueada quando há sync/sessão conforme contrato. | Diálogos explicam efeitos e ações destrutivas; loading desabilita ação. **N/A**: foco de diálogo, fonte ampliada, orientação e temas não exercitados fisicamente. | `ExercisesScreen.kt`, `ExercisesFeatureContract.kt`, ADRs de Health Connect; nenhuma confirmação remota é fabricada. |
| **Exercícios — estados de workflow e fluxos de saída** | `ChampionNavigation.kt` concentra `hasBlockingExerciseOperation`, confirmação global e encaminhamento; `ExercisesScreen.kt` trata `detailState`, `importState`, `operationError`, `hasUnsavedDetailChanges`; `WorkoutShells.kt` trata shells históricos. | Back do sistema, botão de retorno, sair durante importação, descartar alterações, retry/dismiss e retorno após exclusão. | Operação de importação em andamento bloqueia saída sem confirmação; detalhe com alterações não salvas pede confirmação; `activityDeleted` retorna à lista; timeout/falha parcial preservam blocos seguros. | Fluxos não podem criar sessão phone, rota ou localização; `WorkoutTypeSelection`/preparação/atividade phone permanecem descontinuados ou redirecionados por política. PoC GPS fica fora. | Confirmações têm título, texto e ações distintas; estados são textuais. **N/A**: back físico, fonte ampliada, orientação e temas não exercitados. | `ChampionNavigation.kt`, `ExercisesScreen.kt`, `WorkoutShells.kt`, `PhoneWorkoutPolicy`; inventário cobre workflow produtivo sem reabrir direção phone. |
| **Evolução** | `EvolutionScreen.kt` (`EvolutionHeader`, seletores de período/modalidade, resumo, tendências, comparação, recordes) e `EvolutionFeatureContract.kt`; gráficos usam `ChampionCharts.kt`. | Voltar para Exercícios, período, modalidade consolidada/por tipo, retry e abrir atividade por recorde. | Loading, empty, failed, ready; comparação loading/empty/failed; séries distinguem `PRESENT`, `GAP`, `ABSENT`, `NOT_APPLICABLE`; recordes têm loading/empty/failed/ready. | Períodos atuais 7/30/90 dias; comparação usa janela anterior igual; métricas seguem unidade e filtro existentes; sem ano/read model novo. `CYCLING` não usa passos. | Seletores têm selected/stateDescription; cards e gráficos têm descrições e resumo textual; rolagem vertical. **N/A**: fonte ampliada, orientação, temas e interação física não exercitados. | `EvolutionScreen.kt`, `EvolutionFeatureContract.kt`, `ChampionCharts.kt`; padrão de tendências da referência é visual, não fonte de dados. |
| **Dispositivo** | `DeviceScreen.kt` (`DeviceManagementScreen`, `DeviceHeader`, estado atual, associado, busca, conexão, manutenção, diagnóstico e diálogos) e `DeviceManagementContract.kt`/`DeviceManagementUiState.kt`. | Buscar, cancelar busca, selecionar candidato, conectar/trocar, conectar associado, desconectar, esquecer, permissões, Bluetooth, diagnóstico e exportação sanitizada. | Loading, sem associado, permissões, Bluetooth desligado, pronto, scanning, resultados, conectando/preparando, conectado, desconectando, não encontrado, falha, esquecendo e erro. | BLE e permissões permanecem contrato; identificador exibido é seguro; exportação sanitizada não expõe payload. CTA da PoC debug não integra inventário produtivo desta subtarefa. | Candidatos têm role/descrição; feedback, loading e diálogos são textuais; mensagens longas rolam. **N/A**: teste físico de fonte/orientação/temas e leitor de tela. | `DeviceScreen.kt`, `DeviceManagementContract.kt`, `DeviceManagementUiState.kt`; fonte local BLE, sem alterar permissões/payloads e sem promover GPS/mapa. |
| **Perfil** | `ProfileScreen.kt` (`ProfileHeader`, `PersonalMetricsEditorCard`, `StepLengthContextCard`, sugestão, perfis aprendidos, histórico/calibração, mensagens) e `ProfileFeatureContract.kt`; textos em `app/src/main/res/values/strings.xml`. | Editar altura/meta/comprimento do passo, salvar, aceitar sugestão com confirmação, abrir perfis aprendidos/histórico, corrigir/remover calibração, abrir atividade e fechar mensagens. | Loading, valores ausentes, sugestão disponível/aceita, saving, inválido/falha; perfis `Loading`, `Available`, `Unavailable`; histórico vazio e correção aberta. | Meta permanece DataStore; perfis RUNNING/WALKING independentes; calibração e snapshots históricos preservados; unidades m/cm/kg/passos. | Campos e ações têm labels/strings; diálogos têm confirmação; seções usam heading e conteúdo rola. **N/A**: fonte ampliada, orientação, teclado, conteúdo longo e temas não exercitados fisicamente. | `ProfileScreen.kt`, `ProfileFeatureContract.kt`, `strings.xml`, Spec 023/ADR-022; referência não fornece contrato de perfil do Champion. |
| **Configurações** | `SettingsScreen.kt` (`SettingsHeaderCard`, `HealthConnectStatusCard`, `PermissionGroupsCard`, `SyncQueueCard`) e `SettingsFeatureContract.kt`. | Toggle, conectar/atualizar provider/conceder permissões, `Sincronizar agora`, `Gerenciar acesso`, fechar status; refresh no resume. | Disponível, atualização necessária, indisponível; sync ativa/pausada/em execução; permissões concedidas/pendentes; fila pendente/falha/sincronizando/sincronizada. | Health Connect write-only/local-first/foreground; `sincronizado` é escrita aceita sem readback; nenhuma leitura remota ou sync automático novo. | Switch tem descrição de estado; headings e status são textuais; ações ocupam largura e tela rola. **N/A**: fonte ampliada, orientação, temas e provider físico não exercitados. | `SettingsScreen.kt`, `SettingsFeatureContract.kt`; contratos Health Connect/ADR-018/019/021 limitam semântica, referência visual não autoriza nova capacidade. |

### Conferência de completude e limitação da referência

- Todos os destinos produtivos de R4 aparecem nas linhas acima: shell; Home;
  Saúde hub e quatro detalhes; Calendário e detalhe diário; Exercícios por
  importação, lista, detalhe, revisão, calibração, sync e estados; Evolução;
  Dispositivo; Perfil; Configurações.
- A enumeração de `AppDestination` foi cruzada com o `when` de
  `ChampionNavDisplay`: nenhum destino produtivo ficou sem consumidor. Os
  destinos phone históricos foram registrados somente para confirmar a política
  de descontinuação; não foram promovidos a requisito. A PoC GPS/mapa não foi
  incluída na matriz, conforme R4.
- Cada linha registra composição/arquivos, controles/ações, estados/fluxos,
  limites/semântica, acessibilidade/adaptação e fonte/limitação. Validações
  físicas não exercitadas estão marcadas como `N/A`; isso não converte gap em
  requisito funcional.
- Divergências concretas contra o padrão de referência foram confirmadas no
  código atual: cabeçalhos usam `LightColorScheme` também em tema escuro;
  períodos usam vocabulário local (`7D/30D/90D`, `Hoje/7 dias/30 dias` e
  `7/30/90 dias`); renderer posiciona eixo Y à esquerda (`VerticalAxis.rememberStart`)
  enquanto a referência observada usa eixo Y discreto à direita; alturas não
  uniformes são `190dp` (passos/distância), `192dp` mínimo do frame compartilhado
  e `210dp` (BPM, Evolução e gráficos de treino); acessibilidade é desigual
  entre superfícies; adaptação a fonte ampliada e orientação não foi exercitada.
- A inspeção direta de 2026-08-31 observou somente overview e detalhe de Peso do
  pacote `com.fitbit.FitbitMobile`; páginas públicas e padrão autorizado foram
  usados para as demais comparações. Nenhum screenshot, dump, valor, dado
  pessoal, identificador, credencial, APK ou ativo foi versionado.

## Passos de implementação

1. Inventariar superfícies, componentes, destinos e políticas de período atuais.
2. Consultar fontes públicas e, se disponível, observar app instalado somente de
   forma estrutural e sanitizada.
3. Registrar padrões observados e limitações da referência.
4. Produzir matrizes por superfície e por consumidor gráfico, com composição,
   controles, estados, interação, limites e fonte/limitação.
5. Conferir matrizes contra código, contratos e observação real antes de
   solicitar aceite da subtarefa.

## Testes e comandos de validação

Task documental; não executar testes automatizados. Validar links, ticker,
consumidores e privacidade da evidência.

## Definição de pronto

- todas as superfícies produtivas estão listadas;
- cada superfície possui composição, controles, estados, fluxos, limites e
  fonte/limitação preenchidos na matriz;
- cada consumidor gráfico mantém período atual, período-alvo, granularidade,
  gaps, interação e fonte/limitação na matriz gráfica;
- períodos de referência e períodos existentes estão comparados;
- benchmark não contém dados pessoais, credenciais ou arquivos/ativos extraídos
  do app de referência;
- resultado, limitações e evidências estão registrados neste arquivo e no
  overview; o status só muda para `completed` após conferência da matriz.

## Riscos e cuidados

- páginas públicas podem não expor matriz completa de períodos;
- declarar limites e não transformar ausência de observação em requisito;
- não registrar labels livres, métricas ou identificadores pessoais.

## Registro de execução

### Status

`completed`; matriz gráfica preservada e matriz integral conferida contra o
código atual, contratos de apresentação e observação sanitizada da referência.

### Arquivos alterados

- `docs/tasks/030-refinamento-graficos-google-health/030-01-auditar-e-benchmark-visual-integral-google-health.md`;
- `docs/tasks/030-refinamento-graficos-google-health/030-00-overview.md`.

Nenhum arquivo de código de produção, contrato, spec ou teste foi alterado.

### Decisões e desvios

- A auditoria anterior foi escalada porque cobria somente consumidores gráficos;
  não havia inventário comprovável por destino e subsuperfície de R4. Causa foi
  confirmada no diff/registro anterior e corrigida somente documentalmente.
- Consumidores gráficos produtivos permanecem passos, distância e frequência
  cardíaca em Saúde; BPM, distância canônica e cadência D4 em Exercícios; e
  tendências em Evolução. Todos passam por `core/designsystem/chart`.
- A matriz gráfica anterior foi preservada integralmente como submatriz. A nova
  matriz cruza `AppDestination`, `ChampionNavigation` e cada subsuperfície R4,
  sem transformar gaps em requisitos funcionais.
- `HealthChart` em `ChampionDisplayComponents.kt` continua legado de
  preview/testes, sem consumidor produtivo confirmado; remoção fica para
  `030-06` somente após comprovação.
- Benchmark visual permanece sanitizado: overview e detalhe de Peso do
  `com.fitbit.FitbitMobile` foram observados no Galaxy S22 autorizado; páginas
  públicas sustentaram os padrões gerais das demais linhas. Nenhuma captura
  contendo dados pessoais foi versionada.
- Seletor de referência permanece `D`/`S`/`M`/`3M`/`A`, com descrição acessível
  completa. Períodos não suportados ficam ocultos/desabilitados e não geram
  query; `A` não é criado sem fonte real.
- Divergências são achados para subtarefas posteriores, não aceite de nova
  funcionalidade: cabeçalhos usam `LightColorScheme` no tema escuro;
  vocabulário local de períodos; eixo Y esquerdo versus referência direita;
  alturas `190/192/210dp`; acessibilidade desigual; fonte ampliada e orientação
  não exercitadas.

### Comandos e consultas executados

| Comando/verificação | Resultado |
|---|---|
| `git status --short` | Passou antes da edição; repositório sem alterações prévias. Após a edição, somente os dois documentos desta subtarefa ficaram alterados. |
| inspeção de `AppDestination.kt` + `ChampionNavigation.kt` | Passou; destinos e consumidores foram cruzados, incluindo top-level, subárvores e fluxo de retorno. |
| inspeção dos contratos e telas de `feature/home`, `health`, `calendar`, `exercises`, `evolution`, `device`, `profile` e `settings` | Passou; matriz contém composição, ações, estados, limites, semântica, acessibilidade/adaptação e fonte/limitação por linha. |
| inspeção de `core/designsystem/*` e `core/designsystem/chart/*` | Passou; tokens, estados, boundary de renderer, alturas e eixo foram registrados. |
| `https://healthapp.google/` | Consultado; hierarquia e proposta de visão holística confirmadas. |
| Google Play `com.fitbit.FitbitMobile` | Consultado; screenshots públicas e descrição de padrões consultadas. |
| `adb devices -l` | Passou; Galaxy S22 autorizado e operacional. |
| `adb shell pm path com.fitbit.FitbitMobile` | Passou; pacote e splits instalados. |
| `adb shell monkey -p com.fitbit.FitbitMobile 1` | Passou; Google Health aberto sem ação de escrita. |
| `adb exec-out screencap -p` temporário | Passou; inspeção visual local, sem versionamento. |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; app do projeto restaurado em primeiro plano. |
| `git diff --check` | Passou após a edição; nenhuma falha de whitespace. |

Não foram criados, alterados ou executados testes automatizados.

### Resultado e evidências

- Matriz integral conferida: 21 linhas objetivas cobrem shell compartilhado,
  todos os destinos produtivos de R4, Saúde hub + quatro detalhes, Calendário +
  detalhe diário e as sete subsuperfícies exigidas de Exercícios.
- Composição e arquivos, controles/ações, estados/fluxos, limites/semântica,
  acessibilidade/adaptação e fonte/limitação estão preenchidos em todas as
  linhas. A matriz gráfica mantém períodos, granularidade, gaps, interação e
  fonte/limitação de cada consumidor.
- Divergências concretas foram registradas contra código atual: `LightColorScheme`
  nos cabeçalhos em tema escuro, períodos locais, eixo Y esquerdo, alturas
  `190/192/210dp`, acessibilidade desigual e adaptação de fonte/orientação não
  exercitada.
- Nenhum código de produção foi editado. Nenhum teste foi criado ou executado.
- Nenhum screenshot, dump, valor, identificador pessoal, credencial, APK ou
  ativo do app de referência foi versionado.

### Reexecução e aceite

- Matriz gráfica conferida contra contratos e consumidores atuais.
- Matriz integral conferida contra `AppDestination`, `ChampionNavDisplay`, telas,
  contratos e design system atuais.
- Critérios atendidos: superfícies R4 listadas; composição visual integral
  descrita; períodos existentes comparados; gaps, estados e limites semânticos
  preservados; benchmark sanitizado; validações físicas não exercitadas marcadas
  como `N/A`; nenhum teste automatizado executado.
- Aceite documental concluído. Próxima subtarefa elegível é `030-02`; esta
  execução não avançou para ela.

### Riscos residuais

- Matriz completa do Google Health/Fitbit não foi inferida de páginas públicas;
  benchmark permanece limitado aos padrões observáveis e às superfícies reais
  do Champion.
- Acessibilidade física, fonte ampliada, portrait, landscape e temas não foram
  exercitados nesta subtarefa documental e permanecem `N/A`; validação física
  fica bloqueada até subtarefas de código e seus aparelhos disponíveis.
- Divergências visuais permanecem para `030-02` em diante. Elas não bloqueiam o
  aceite de `030-01`, mas bloqueiam alegação de refinamento visual concluído da
  Spec 030 antes das subtarefas posteriores.
- PoC GPS/mapa continua fora da auditoria produtiva e não pode ser promovida por
  esta documentação.
