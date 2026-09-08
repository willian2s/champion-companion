# 031-06 — Redesenhar Saúde e métricas

- **Ticker:** `031`
- **Número:** `06`
- **Status:** in_progress

## Objetivo

Reorganizar hub Saúde, detalhes de frequência cardíaca, peso, passos e distância,
incluindo gráficos, filtros e listas conforme referência e dados locais.

## Requisitos e aceite

- O hub usa lista colorida com card Atividades no topo, seguido por Frequência
  cardíaca, Peso, Passos e Distância; todos os cards mantêm mesma estrutura,
  acessibilidade e ação de abertura.
- Medição manual e importação D1 de BPM continuam explícitas.
- Gráficos preservam lacunas, unidades, períodos, origem e acessibilidade.
- Distância continua estimada quando essa for sua origem. Na tela de distância,
  `Semana` mostra domingo–sábado no gráfico com `D S T Q Q S S` e lista diária;
  `M` mostra dias no gráfico com labels `1, 5, 10, 15, 20, 25` e último dia,
  e lista semanal dentro do mês; `3M` e `A` mostram o mês atual parcial e os
  meses civis anteriores no gráfico e na lista. Gráfico mantém colunas vazias;
  lista mostra somente valores válidos.
- Health Connect não vira fonte visual nem é duplicado sem necessidade.
- Registrar nomes dos componentes compartilhados usados também em `031-05`;
  promoção exige uso real nas duas superfícies, nunca apenas preview.

## Escopo

Inclui composição visual, filtros, headers, listas e uso da infraestrutura de
gráficos. Exclui novas métricas, leitura Health Connect, fórmulas e BLE.

## Dependências

- `031-03`, `031-04`, `031-05`.
- Specs 009, 015, 023, 028 e ADR-009.

## Arquivos e símbolos prováveis

- `feature/health/HealthScreen.kt`.
- `HealthMetricDetailScreens.kt`, `StepsHistoryShell.kt`,
  `DistanceHistoryShell.kt`.
- `HeartRateHistoryChart.kt`, `StepsHistoryChart.kt`,
  `DistanceHistoryChart.kt` e contratos/ViewModels.

## Passos

1. Criar matriz de métricas disponíveis e blocos equivalentes.
2. Reordenar hub/detalhes e adaptar filtros sem mudar intents.
3. Ajustar gráficos via `core/designsystem/chart`, sem reamostrar dados.
4. Validar estados vazios, períodos, retorno e ações explícitas.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado confirma hub, header, cards coloridos, card Atividades no
topo, quatro períodos da distância, buckets equivalentes entre gráfico/lista e
ausência de rolagem horizontal nos gráficos de Saúde. Não criar ou executar
testes automatizados.

## Definição de pronto

Hub e quatro detalhes aprovados visualmente; dados reais, filtros, gaps, estados
e callbacks permanecem equivalentes; evidência sanitizada, estado
inicial/restaurado do aparelho e revisão independente aprovados.

## Riscos

Visual denso pode incentivar valores sintéticos ou ocultar lacunas. Rejeitar
qualquer composição que não exponha unidade, período e ausência corretamente.

## Execução atual

  - **Arquivos alterados:** `HealthScreen.kt`, `HealthFeatureContract.kt`,
  `HealthViewModel.kt`, `HeartRateHistoryChart.kt`, `CalendarHistory.kt`,
   `StepsHistoryChart.kt`, `DistanceHistoryChart.kt`, `DistanceHistoryViewModel.kt`,
   `HealthMetricDetailScreens.kt`, `StepsHistoryShell.kt`, `DistanceHistoryShell.kt`,
   `ChampionCharts.kt` e
  `ChampionNavigation.kt`.
- **Decisões:** após solicitação explícita, o hub voltou ao estilo lista da
  implementação anterior, mantendo `ChampionRedesignSurfaceHeader`, ações de
  Dispositivo/Perfil, card de Atividades no topo seguido pelas quatro métricas
  locais, com card igual aos
  demais cards, com a paleta colorida restaurada; o card fica no topo da lista;
  o calendário não é exposto neste hub e permanece dentro de Exercícios; FC intraday usa início do dia como origem temporal, bandas e
  pontos estilo referência; detalhes preservam seus gráficos, filtros, lacunas
  e rolagem horizontal desativada. Distância usa buckets derivados dos mesmos
   dados para gráfico e lista: semana civil atual com sete entradas diárias,
   entradas diárias e semanas civis recortadas no mês fechado em `M`, e meses
   fechados em `3M` e `A`. Intervalos semanais respeitam domingo–sábado e os
   limites do mês, como `30/08 - 31/08`; o gráfico anual usa somente a inicial
   maiúscula de cada mês.
- **Desvios:** nenhum contrato de meta semanal foi criado. Unidade de Passos não
  é repetida no valor do card. O período anual foi adicionado ao contrato comum
  de histórico e permanece oculto no seletor de Passos.
- **Comandos executados:** `git diff --check`; `./gradlew app:compileDebugKotlin
  --no-daemon`; `./gradlew app:lintDebug --no-daemon`; `./gradlew lint assembleDebug
  assembleRelease --no-daemon`; `./gradlew installDebug --no-daemon`.
- **Comandos adicionais executados:** `git diff --check`; `./gradlew
  app:compileDebugKotlin --no-daemon`; `./gradlew lint assembleDebug
  assembleRelease --no-daemon`; `./gradlew installDebug --no-daemon`.
  - **Intercorrência:** uma compilação intermediária falhou por referência
  ausente de `onOpenExercises` durante a inclusão do card; assinatura foi
  corrigida e compilação subsequente passou.
 - **Resultados/evidências:** compilação, lint e builds Debug/Release passaram
   após implementação dos períodos. Instalação Debug passou. A implementação
   foi inspecionada no código: Semana civil atual, M diário no gráfico e semanal
   no recorte domingo–sábado, 3M/A mensais, labels de M nos dias requeridos e
    colunas vazias preservadas. A tentativa anterior de smoke foi interrompida
    por atividade externa do aparelho; a execução posterior foi concluída após a
    correção do crash. Dumps e screenshots temporários foram descartados e não
    serão versionados. Não foram criados nem executados testes automatizados,
    conforme regra do projeto.
  - **Riscos residuais:** revisão independente pendente. Imagem de referência não
  deve entrar no versionamento.

  - **Correção pós-smoke:** o fechamento da tela foi diagnosticado via
   `dumpsys dropbox` como `IllegalStateException` do Vico por formatter de eixo
   retornando string vazia. O renderer agora aceita `labelValues` e usa
    `ItemPlacer` explícito; Semana e 3M passaram a fixar labels nos mesmos X das
    colunas, o chip semanal foi reduzido para `S` e os labels dos filtros foram
    centralizados.
 - **Atualização de intervalo civil:** `Semana` consulta a semana civil atual,
   de domingo a sábado; `M` consulta o mês calendário fechado anterior; `3M`
   consulta os três meses calendário fechados anteriores; `A` consulta doze
   meses calendário fechados anteriores. O gráfico mensal permanece diário com
    labels espaçados; a lista mensal usa semanas civis recortadas no mês; 3M usa
    meses abreviados em maiúsculas e A usa iniciais mensais.
  - **Validação adicional:** `./gradlew app:compileDebugKotlin --no-daemon`,
    `./gradlew lint assembleDebug assembleRelease --no-daemon` e `git diff --check`
    passaram após o ajuste de intervalo civil e correção do renderer. `./gradlew
    installDebug --no-daemon` passou. Smoke ADB confirmou abertura de Distância
    sem crash, labels `D S T Q Q S S`, chip `S`, e alinhamento visual entre
     colunas/eixos em Semana e 3M, iniciais mensais no gráfico A sem quebra de
     texto e labels dos filtros centralizados. Nenhum teste automatizado foi
     criado ou executado.

## Atualização da execução — 2026-09-04

- **Status:** `in_progress`; revisão independente e decisão de fechamento
  permanecem pendentes.
- **Escopo aplicado:** a tela de frequência cardíaca compõe somente cabeçalho,
  gráfico e histórico; foram removidos da composição os cards `Resumo atual`,
  `Importar histórico D1` e `Medição manual`, junto dos composables privados que
  existiam exclusivamente para esses blocos.
- **Preservação confirmada:** `PullToRefreshBox`, gesto de pull-to-refresh,
  `triggerD1Refresh`, ação de acessibilidade `Atualizar histórico do relógio`,
  estado/coordenação D1 e manual, lifecycle, contratos, persistência, histórico,
  gráfico, filtros e proteção de saída continuam no fluxo.
- **Arquivos alterados nesta execução:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`.
- **Validação:** `git diff --check`,
  `./gradlew app:compileDebugKotlin --no-daemon`,
  `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram. Nenhum teste automatizado foi
  criado ou executado.
- **Bloqueios/riscos:** revisão independente e smoke visual final continuam
  pendentes; nenhum backend, domínio ou navegação foi alterado.

- **Esclarecimento de escopo:** por solicitação explícita do usuário, os cards
  `Resumo atual`, `Importar histórico D1` e `Medição manual` foram removidos
  somente da superfície visual de frequência cardíaca. O pull-to-refresh e sua
  ação de acessibilidade continuam sendo a entrada de importação D1; contratos,
  ViewModel, coordenação e persistência permanecem disponíveis para tratamento
  posterior em spec própria.
- **Smoke visual sanitizado:** em aparelho autorizado, a navegação Saúde →
  Frequência cardíaca exibiu `Tendência` e `Histórico completo`, sem os três
  rótulos removidos. Pull-to-refresh foi preservado estaticamente e não foi
  disparado para gerar evidência. Dumps temporários foram apagados e a ausência
  foi confirmada no aparelho.

## Refinamento de gráficos e listas — 2026-09-04

- **Status:** `in_progress`; revisão independente e decisão de fechamento
  permanecem pendentes.
- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`,
  `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`,
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HeartRateHistoryChart.kt`
  e `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`.
- **Decisões:** `Hoje` usa 144 buckets de 10 minutos, gráfico compacto com
  bandas visuais e eixo `0, 4, 8, 12, 16, 20, 23`; a lista foi substituída por
  tempo por faixa e faixas de frequência. Como não há idade no modelo e a
  Spec 023 exclui esse dado, as quatro faixas são display-only (`30–79`,
  `80–129`, `130–169`, `170–220`), sem interpretação clínica ou cálculo etário.
  Cada bucket preenchido contribui com 10 minutos; lacunas não são interpoladas.
- **Decisões:** demais períodos usam barras e preservam buckets vazios no
  gráfico. `Semana` usa domingo–sábado e médias diárias; `M` usa mês civil
  fechado, gráfico diário e lista de semanas civis recortadas; `3M` e `A` usam
  meses civis fechados e listas mensais. Médias e gráfico visual filtram
  `WATCH_WORKOUT_ACTIVITY`; persistência e entradas originais permanecem
  intactas.
- **Riscos/desvios:** o filtro de atividades também foi aplicado às listas para
  manter médias coerentes com os gráficos, embora o pedido tenha explicitado o
  filtro nos gráficos. Faixas por idade dependem de futura spec que forneça
  idade confiável; não foram criados contratos persistentes, migration ou
  fórmula clínica nesta execução.
- **Validação:** `git diff --check`,
  `./gradlew app:compileDebugKotlin --no-daemon` e
  `./gradlew lint assembleDebug assembleRelease --no-daemon` passaram;
  `./gradlew installDebug --no-daemon` passou. Smoke ADB sanitizado confirmou
  filtros `Hoje`, `Semana`, `M`, `3M`, `A`, bandas/faixas de Hoje, ausência dos
  três cards removidos e seções de tendência; dumps temporários foram apagados.
  Uma execução inicial de build excedeu timeout e uma execução inicial de lint
  falhou por indentação suspeita; ambos foram corrigidos e reexecutados com
  sucesso.
- **Intercorrência:** agente delegado criou temporariamente um teste de bucket
  mensal e executou o teste focado; o arquivo de teste e a alteração foram
  revertidos antes da validação final. Nenhum teste novo permanece no diff.
- **Riscos residuais:** revisão independente e smoke de interação completa dos
  cinco períodos permanecem pendentes; warnings legados de override deprecated
  em `RoomRepositories.kt` continuam sem relação com esta mudança.

## Correção de compilação — 2026-09-04

- **Sintoma observado:** `app:compileDebugKotlin` falhou por `when` não
  exaustivo para `LAST_90_DAYS`, `LAST_365_DAYS` e `MONTHLY`, além de chamada
  insegura em `HeartRateChartPoint.bpm`, agora nullable para representar lacunas.
- **Causa raiz:** a implementação parcial ampliou os enums e o contrato de ponto,
  mas `HealthMetricDetailScreens.kt` e `HeartRateHistoryChart.kt` mantiveram
  consumidores do contrato anterior.
- **Correção:** adicionados branches para os dois períodos na tela e no gráfico;
  adicionado renderer mensal com índices de bucket e labels; pontos nulos agora
  viram lacunas explícitas sem conversão para zero; bounds mensais usam quantidade
  real de buckets. Nenhuma UI de zonas ou listas novas foi introduzida.
- **Regressão:** `RoomRepositoriesHeartRateHistoryQueriesTest` confirma buckets
  mensais e mês vazio preservado como `bpm = null`/`sampleCount = 0`.
- **Evidência:** `./gradlew app:compileDebugKotlin --no-daemon` passou;
  `./gradlew app:testDebugUnitTest --tests
  br.com.mwconsulting.championcompanion.data.RoomRepositoriesHeartRateHistoryQueriesTest
  --no-daemon` passou; `git diff --check` passou.
 - **Risco residual:** revisão independente e validação visual final continuam
   pendentes; nenhum backend, domínio ou navegação foi alterado.

## Correções solicitadas — 2026-09-04

- **Status:** `in_progress`; revisão independente e validação visual final
  continuam pendentes.
- **Correções aplicadas:** `M` de frequência cardíaca e distância agora consulta
  o mês civil atual, do primeiro dia até hoje inclusive; `3M` e `A` continuam
  limitados aos meses civis fechados anteriores. Gráficos diários usam o último
  dia disponível sem cortar o eixo; lacunas permanecem explícitas. Listas de
  `M` recortam semanas civis no mês atual e não criam semanas futuras vazias.
- **Labels e acessibilidade:** descrições de `M` deixaram de declarar mês
  fechado anterior; labels diários mantêm os marcos `1`, `5`, `10`, `15`, `20`,
  `25` e o último dia disponível quando já transcorrido.
- **Hub Saúde (tentativa anterior):** frequência cardíaca usou
  `latestMeasurement`, stream fixo já existente e independente do período
  selecionado no detalhe. Essa escolha foi substituída pela correção delimitada
  abaixo para preservar a semântica vigente de mínimo/máximo diário. Distância
  continua usando `todaySummary`.
- **Arquivos alterados nesta correção:** `RoomRepositories.kt`,
  `DistanceHistoryViewModel.kt`, `DistanceHistoryChart.kt`,
  `DistanceHistoryShell.kt`, `HealthMetricDetailScreens.kt` e
  `HealthScreen.kt`.
- **Validação:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon` e `./gradlew app:lintDebug --no-daemon` passaram. Nenhum teste
  automatizado foi criado ou executado.
- **Bloqueios/riscos:** revisão independente, smoke visual e decisão de
  fechamento continuam pendentes.

## Correção delimitada — 2026-09-04

- **Status:** `in_progress`; revisão independente e smoke visual final continuam
  pendentes.
- **Causa corrigida:** o hub derivava Frequência cardíaca de `state.history`, que
  muda ao selecionar períodos no detalhe. A tentativa anterior com
  `latestMeasurement` também alterava a semântica vigente de mínimo/máximo diário.
- **Decisão:** `HealthViewModel` observa em paralelo um snapshot in-memory fixo de
  `HeartRateHistoryPeriod.TODAY`; `HealthUiState` o expõe como
  `todayHeartRateHistory`. `HealthScreen` calcula mínimo/máximo diário desse
  snapshot, mantendo `state.history` exclusivo do período selecionado.
- **Preservação:** nenhum Room, schema, persistência, gráfico, lista ou período do
  detalhe foi alterado.
- **Arquivos alterados:** `HealthFeatureContract.kt`, `HealthViewModel.kt` e
  `HealthScreen.kt`.
- **Validação:** `git diff --check` e `./gradlew app:compileDebugKotlin
  --no-daemon` passaram. Nenhum teste automatizado foi criado ou executado.
- **Bloqueios/riscos:** revisão independente e smoke visual permanecem pendentes;
  nenhuma pendência funcional conhecida nesta correção.

## Validação final disponível — 2026-09-04

- **Status:** `in_progress`; revisão independente permanece pendente.
- **Validação de build:** `git diff --check`, `./gradlew lint assembleDebug
  assembleRelease --no-daemon` e `./gradlew installDebug --no-daemon` passaram
  após reconexão do aparelho. Permanecem somente warnings legados de override
  deprecated em `RoomRepositories.kt`.
- **Smoke ADB sanitizado:** filtros `S`, `M`, `3M`, `A` e `D` foram alternados
  na frequência cardíaca sem `FATAL EXCEPTION`/`AndroidRuntime`; Activity
  permaneceu no pacote esperado. Nenhum teste automatizado foi criado ou
  executado.
- **Riscos residuais:** revisão independente e validação visual de dados do mês
  atual/hub fixo ainda dependem de inspeção manual; nenhum bloqueio funcional
  conhecido.

## Inclusão de FC de exercícios — 2026-09-04

- **Status:** `in_progress`; revisão independente permanece pendente.
- **Decisão:** amostras válidas de `WATCH_WORKOUT_ACTIVITY`, já disponíveis no
  read model local, passam a compor gráficos, médias, mínimos/máximos e listas
  da tela de frequência cardíaca. Nenhum dado novo, contrato, persistência ou
  fonte Health Connect foi criado.
- **Arquivos alterados:** `RoomRepositories.kt`,
  `HeartRateHistoryChart.kt` e `HealthMetricDetailScreens.kt`.
- **Desvio corrigido:** removido filtro visual que mantinha FC de exercícios
  somente no snapshot, mas a ocultava do gráfico e da lista. Origem passa a ser
  anunciada como `atividade do relógio` quando presente.
- **Riscos residuais:** amostras inválidas continuam descartadas pelo limite
  canônico de 30–220 BPM; revisão independente e smoke visual desta inclusão
  permanecem pendentes.

## Validação da inclusão de FC de exercícios — 2026-09-04

- **Resultados:** `git diff --check`, `./gradlew lint assembleDebug
  assembleRelease --no-daemon` e `./gradlew installDebug --no-daemon` passaram.
  Smoke ADB alternou `D`, `S`, `M`, `3M` e `A` sem linhas de crash
  (`FATAL EXCEPTION`/`AndroidRuntime` = 0); Activity permaneceu no pacote
  esperado.
- **Limitação da evidência:** aparelho não possui fixture versionada nem foi
  criado dado pessoal para provar amostra positiva de exercício na UI. A
  inclusão fica evidenciada estaticamente pelo read model já existente e pelo
  caminho de composição agora sem exclusão de `WATCH_WORKOUT_ACTIVITY`.
- **Riscos residuais:** revisão independente e confirmação visual com atividade
  real permanecem pendentes; nenhum teste automatizado foi criado ou executado.

## Ajuste do eixo diário — 2026-09-04

- **Decisão:** gráficos diários de FC usam baseline `0 BPM` no eixo Y; barras
  passam a encostar no eixo XY, sem alterar valores, agregação ou lacunas.
  Gráfico intradiário continua usando bandas visuais `30–220 BPM`.
- **Arquivo alterado:** `HeartRateHistoryChart.kt`.
- **Riscos residuais:** revisão independente e confirmação visual final ainda
  pendentes.

## Validação do baseline diário — 2026-09-04

- **Resultados:** `git diff --check`, `./gradlew app:compileDebugKotlin
  app:lintDebug --no-daemon`, `./gradlew lint assembleDebug assembleRelease
  --no-daemon` e `./gradlew installDebug --no-daemon` passaram. Smoke ADB abriu
  Saúde/Frequência cardíaca e alternou para `M` sem crash; `FATAL EXCEPTION` e
  `AndroidRuntime` ficaram em zero.
- **Evidência de código:** `ChampionChartAxisSpec.yAxis.minimum` do renderer
  diário foi fixado em `0 BPM`; intradiário permanece fora desse renderer e
  conserva bandas `30–220 BPM`.
- **Riscos residuais:** inspeção visual independente permanece pendente.

## Ajuste de tooltip e legendas — 2026-09-04

- **Decisão:** tooltip compartilhado passa a exibir somente valor formatado com
  unidade, sem prefixo de série, data longa ou legenda. Isso remove texto
  redundante de todos os gráficos e deixa BPM, passos e distância legíveis.
- **Arquivo alterado:** `core/designsystem/chart/ChampionCharts.kt`.
- **Riscos residuais:** gráfico intradiário de FC continua sendo Canvas sem
  interação de tooltip; revisão visual independente permanece pendente.

## Ajuste intradiário e legenda — 2026-09-04

- **Decisão:** FC de `Hoje` deixou de ser reamostrada em buckets de 10 minutos;
  cada amostra válida armazenada mantém seu timestamp original no gráfico.
  Indicador por faixa passou a contar amostras, sem inventar duração temporal.
- **Layout:** label `0` do eixo horário usa `SpaceBetween` e fica alinhado à
  borda inicial do gráfico.
- **Tooltip/legenda:** renderer compartilhado exibe somente valor formatado e
  unidade no tooltip, removendo prefixo de série e legenda textual.
- **Arquivos alterados:** `HeartRateAndSync.kt`, `RoomRepositories.kt`,
  `HeartRateHistoryChart.kt`, `HealthMetricDetailScreens.kt` e
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/chart/ChampionCharts.kt`.
- **Riscos residuais:** confirmação visual de tooltip com dados reais e revisão
  independente permanecem pendentes.

## Validação do ajuste intradiário — 2026-09-04

- **Resultados:** `git diff --check`, compilação Kotlin Debug, lint Debug,
  `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram. Smoke ADB abriu Saúde → FC e
  alternou período `D` sem crash; `FATAL EXCEPTION`/`AndroidRuntime` ficaram em
  zero e Activity permaneceu no pacote esperado.
- **Limitação:** aparelho não possui fixture versionada de leituras para
  confirmar tooltip por interação sem fabricar dado pessoal; comportamento foi
  validado por compilação e inspeção do renderer compartilhado.
- **Riscos residuais:** revisão independente e inspeção visual com dados reais
  continuam pendentes.

## Remoção efetiva das legendas visuais — 2026-09-04

- **Decisão:** removidos rótulos visuais de linhas de referência no renderer
  compartilhado, legenda manual de Passos e lista visual de faixas da FC. Linhas
  de referência e informações equivalentes para acessibilidade permanecem.
- **Arquivos alterados:** `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/chart/ChampionCharts.kt`,
  `StepsHistoryChart.kt` e `HealthMetricDetailScreens.kt`.
 - **Riscos residuais:** revisão visual independente permanece pendente.

## Restauração dos dados por zona — 2026-09-04

- **Correção:** restaurada seção de dados `Leituras por faixa` da FC diária,
  mantendo contagem real de amostras armazenadas por zona. Removido somente o
  `Resumo acessível` textual abaixo do gráfico.
- **Validação ADB:** tela `D` exibiu dados de zona (`Zona 1`/`Zona 2` visíveis),
  não exibiu `Resumo acessível` e não registrou `FATAL EXCEPTION`.
- **Validação de build:** `git diff --check`, compile Kotlin Debug, lint Debug e
  `./gradlew installDebug --no-daemon` passaram.
- **Riscos residuais:** revisão visual independente permanece pendente.

## Remoção do resumo acessível visível — 2026-09-04

- **Correção final:** removidos `Text` de resumo acessível abaixo dos gráficos de
  Saúde e Exercícios. Sem remoção de semântica: gráficos continuam expondo
  `accessibilitySummary`/`contentDescription` para TalkBack.
- **Validação:** `git diff --check`, compile Kotlin Debug, lint Debug,
  `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram.
- **Riscos residuais:** revisão visual independente permanece pendente.

## Remoção dos resumos acessíveis visíveis — 2026-09-04

- **Correção:** removidos apenas os composables `Text` que exibiam o prefixo
  `Resumo acessível:` abaixo dos gráficos de Saúde e Exercícios. Descrições de
  acessibilidade continuam anexadas aos gráficos via `accessibilitySummary` e
  `contentDescription`.
- **Preservação:** dados, tooltips, rótulos internos e legendas inferiores já
  removidas não foram alterados.
- **Riscos residuais:** revisão visual independente permanece pendente.

## Correção de escopo da remoção de legenda — 2026-09-04

- **Ajuste:** removida somente legenda abaixo dos gráficos. Rótulos internos de
  linhas de referência, marcações `Z1`–`Z4` dentro do Canvas e tooltips não foram
  removidos; tooltip continua mostrando apenas valor/unidade.
- **Evidência:** dump ADB sanitizado da tela de FC não contém `Barras:`,
  `Leituras por faixa` ou `Zona 1`–`Zona 4`; `FATAL EXCEPTION` ficou em zero e a
  Activity permaneceu ativa.
- **Validação:** `git diff --check`, compilação Kotlin Debug, lint Debug,
  `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram.
- **Riscos residuais:** revisão visual independente permanece pendente.

## Remoção final das legendas — 2026-09-04

- **Correção:** removidas somente legendas textuais posicionadas abaixo dos
  gráficos: legenda manual de Passos e seção visual de faixas da FC. Rótulos de
  linhas de referência e marcações `Z1`–`Z4` dentro do gráfico foram preservados.
  Informações continuam disponíveis em acessibilidade.
- **Tooltip:** permanece somente valor formatado com unidade, sem título de série
  ou texto extenso.
- **Validação:** `git diff --check`, compilação Kotlin Debug, lint Debug,
  `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram. Smoke ADB abriu Saúde → FC e
  alternou `D` sem crash (`FATAL EXCEPTION`/`AndroidRuntime` = 0).
- **Riscos residuais:** revisão visual independente permanece pendente.

## Correção delimitada de janela mensal — 2026-09-08

- **Status:** `in_progress`; revisão independente e smoke visual final continuam
  pendentes.
- **Causa:** `3M` e `A` ainda excluíam o mês civil atual, consultando somente
  meses fechados anteriores.
- **Correção:** `3M` agora cobre o mês atual parcial e os dois meses civis
  anteriores; `A` cobre o mês atual parcial e os onze meses anteriores. Ambos
  terminam em `today.plusDays(1)` (limite exclusivo), preservando o dia atual e
  lacunas mensais.
- **Textos atualizados:** acessibilidade, descrição dos gráficos e headline de
  frequência cardíaca deixaram de declarar somente meses fechados anteriores.
- **Arquivos alterados nesta correção:** `RoomRepositories.kt`,
  `DistanceHistoryViewModel.kt`, `DistanceHistoryShell.kt`,
  `DistanceHistoryChart.kt` e `HealthMetricDetailScreens.kt`.
- **Validação:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon`, `./gradlew app:lintDebug --no-daemon` e `./gradlew lint
  assembleDebug assembleRelease --no-daemon` passaram. Nenhum teste
  automatizado foi criado ou executado.
- **Bloqueios/riscos:** revisão independente e smoke visual permanecem
  pendentes; mudanças não alteram contratos, persistência ou fórmulas.

## Validação adicional — 2026-09-08

- **Resultados:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon`, `./gradlew app:lintDebug --no-daemon`, `./gradlew lint
  assembleDebug assembleRelease --no-daemon` e `./gradlew installDebug
  --no-daemon` passaram.
- **Smoke sanitizado:** `adb devices -l` encontrou o aparelho autorizado;
  `MainActivity` iniciou no pacote Debug e o processo permaneceu ativo. Não
  foram usados dumps ou screenshots não sanitizados.
- **Documentação:** requisito de `031-06` e observações do overview foram
  alinhados à semântica atual de `3M`/`A`; checklist permanece pendente até
  revisão independente.
- **Riscos residuais:** interação visual completa dos filtros e revisão
  independente continuam pendentes; nenhum teste automatizado foi criado ou
  executado.

## Revisão independente — 2026-09-08

- **Resultado:** revisão independente reprovou o fechamento da subtarefa por
  divergências preexistentes do escopo amplo: medição manual não está explícita
  na composição atual da tela e Saúde não possui entrada direta para Calendário.
- **Decisão:** não alterar essas superfícies nesta correção delimitada de
  janela mensal; preservar mudanças já registradas e manter `in_progress`.
- **Correção derivada da revisão:** a descrição acessível do gráfico de
  frequência cardíaca passou a declarar explicitamente mês atual até hoje mais
  os meses anteriores em `3M` e `A`.
- **Arquivos adicionais alterados:** `HeartRateHistoryChart.kt`.
- **Validação posterior:** `git diff --check`, compilação Kotlin Debug, lint,
  builds Debug/Release e `installDebug` passaram novamente. Nenhum teste
  automatizado foi criado ou executado.
- **Bloqueios:** revisão independente continua sem aprovação; os dois desvios
  preexistentes exigem decisão/escopo próprio antes de marcar o checklist.
- **Reavaliação incremental:** a correção mensal e a descrição acessível de
   `3M`/`A` foram aprovadas isoladamente; a subtarefa geral permanece
   `in_progress` pelos bloqueios preexistentes.

## Correção delimitada de duração por faixa — 2026-09-08

- **Solicitação aplicada:** a seção `Leituras por faixa` da frequência cardíaca
  diária agora exibe tempo observado por faixa, substituindo a contagem de
  amostras.
- **Semântica:** pontos com BPM são ordenados por timestamp; cada intervalo
  positivo entre leituras consecutivas é atribuído à faixa do ponto inicial.
  Pontos nulos, intervalos nulos/negativos e o último ponto sem sucessor não
  contribuem; não há reamostragem nem duração sintética.
- **Apresentação e acessibilidade:** duração agregada usa `0 min`, `N min` ou
  `N h MM min`; a descrição acessível anuncia as mesmas durações. Texto
  explicativo explicita tempo entre leituras e ausência de atribuição ao último
  ponto.
- **Arquivo alterado:** `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`.
- **Validação:** `git diff --check`; `./gradlew lint assembleDebug assembleRelease --no-daemon` passaram. Nenhum teste automatizado foi criado ou executado.
- **Bloqueios preservados:** revisão independente e smoke visual da subtarefa
  ampla continuam pendentes; medição manual explícita e entrada direta Saúde →
  Calendário permanecem fora desta correção delimitada.
- **Revisão incremental:** blocker sobre lacunas atravessadas foi corrigido;
  revisão independente aprovou isoladamente o cálculo por timestamps e a
  acessibilidade. `031-06` permanece `in_progress` pelos bloqueios amplos já
  registrados.
- **Validação final desta alteração:** `git diff --check`, `./gradlew
  app:compileDebugKotlin --no-daemon`, `./gradlew app:lintDebug --no-daemon`,
  `./gradlew lint assembleDebug assembleRelease --no-daemon` e `./gradlew
  installDebug --no-daemon` passaram. Nenhum teste automatizado foi criado ou
  executado.

## Correção do blocker da revisão — 2026-09-08

- **Status:** `in_progress`; bloqueios amplos da revisão permanecem inalterados.
- **Causa:** `heartRateZoneDurations` removia pontos nulos ou sem amostras antes
  de formar pares, atravessando lacunas e atribuindo duração entre pontos não
  consecutivos originais.
- **Correção:** pontos originais agora são ordenados e processados em pares
  consecutivos; qualquer intervalo com BPM nulo ou `sampleCount <= 0` em uma das
  extremidades é ignorado. Delta positivo, faixa do ponto inicial, último ponto
  sem sucessor e ausência de reamostragem permanecem preservados.
- **Arquivo de produção alterado:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`.
- **Validação:** `git diff --check` passou. Nenhum teste foi criado ou executado.
- **Bloqueios:** revisão independente ampla e smoke visual continuam pendentes;
  medição manual explícita e entrada direta Saúde → Calendário não foram
  alteradas.

## Verificação da janela mensal — 2026-09-08

- **Resultado:** a decisão de usar o mês atual em `3M` e `A` está preservada,
  não revertida.
- **Evidência:** Distância e Frequência cardíaca consultam o primeiro dia do
  mês atual até amanhã como limite exclusivo; `3M` retrocede dois meses e `A`
  retrocede onze meses. Os buckets mensais vazios permanecem no gráfico e
  valores ausentes não viram zero.
- **Revisão independente:** passou para o escopo mensal isolado. A subtarefa
  geral continua `in_progress` pelos bloqueios preexistentes de medição manual
  explícita e entrada Saúde → Calendário.
- **Validação:** `git diff --check` e `./gradlew app:compileDebugKotlin
  app:lintDebug --no-daemon` passaram. Nenhum teste automatizado foi criado ou
  executado.
- **Riscos residuais:** smoke visual completo e revisão da subtarefa ampla
  continuam pendentes.

## Execução delimitada de Passos — 2026-09-08

- **Status:** `in_progress`; alteração limitada aos gráficos e listas de Passos.
- **Implementação:** `StepsHistoryViewModel` consulta `dateRangeOverride` para
  semana civil atual domingo–sábado, mês atual até hoje, três meses com mês
  atual parcial e dois meses anteriores, e doze meses com mês atual parcial e
  onze meses anteriores. `StepsHistoryShell` agrega apresentação por semana/mês,
  soma somente valores não nulos, preserva zero e oculta buckets sem valor na
  lista. `StepsHistoryChart` usa sete labels `D S T Q Q S S`, marcos diários do
  mês, abreviações mensais e iniciais anuais, mantendo lacunas no gráfico.
- **Preservação:** fonte continua `DailySummary`; meta atual, origem, unidade,
  acessibilidade, estados e callbacks existentes foram mantidos. FC, Distância,
  `HealthMetricDetailScreens` e contratos globais não foram alterados nesta
  execução.
- **Arquivos de produção alterados:** `StepsHistoryViewModel.kt`,
  `StepsHistoryShell.kt` e `StepsHistoryChart.kt`.
- **Validação:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon` e `./gradlew app:lintDebug --no-daemon` passaram. Nenhum teste
  automatizado foi criado ou executado.
- **Validação adicional:** `./gradlew lint assembleDebug assembleRelease
  --no-daemon` e `./gradlew installDebug --no-daemon` passaram. Smoke ADB
  sanitizado iniciou `MainActivity` no aparelho autorizado e confirmou a
  Activity retomada no pacote Debug esperado. Não foram usados dumps ou dados
  pessoais para validar valores.
- **Bloqueios/riscos:** revisão independente e smoke visual permanecem
  pendentes; checklist geral não foi marcado nesta execução delimitada.

- **Revisão independente:** reprovou inicialmente por textos de acessibilidade
  que ainda diziam meses fechados em `3M`/`A`; correção aplicada para anunciar
  mês atual até hoje mais dois/onze meses civis anteriores, conforme janela
  efetiva. Nenhum outro blocker foi encontrado.
- **Validação posterior:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon` e `./gradlew app:lintDebug --no-daemon` passaram. Revisão
  independente precisa ser repetida após esta correção; checklist permanece
  pendente.

## Remoção dos resumos de Passos — 2026-09-08

- **Solicitação aplicada:** removidos somente os cards visuais `Resumo atual` e
  `Resumo do período` da tela de Passos. Histórico, gráfico, lista, meta atual
  usada no contexto dos dados, filtros, callbacks e estados foram preservados.
- **Arquivo alterado:** `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/StepsHistoryShell.kt`.
- **Validação:** `git diff --check`, `./gradlew app:compileDebugKotlin
  app:lintDebug --no-daemon`, `./gradlew lint assembleDebug assembleRelease
  --no-daemon` e `./gradlew installDebug --no-daemon` passaram. Smoke ADB
  sanitizado confirmou `MainActivity` retomada no pacote Debug esperado.
- **Riscos:** revisão independente e inspeção visual final da tela de Passos
  continuam pendentes; nenhum teste automatizado foi criado ou executado.

## Revisão independente de Passos — 2026-09-08

- **Resultado:** aprovado com ressalvas para o escopo delimitado de Passos.
  Semana, Mês, `3M`, `A`, agregações, lacunas, labels, acessibilidade e remoção
  dos dois cards foram considerados coerentes.
- **Ressalva:** smoke visual com dados representativos não confirmou valores
  reais, zero/lacuna e labels no aparelho; não há fixture versionada e nenhum
  dado pessoal foi criado.
- **Status:** `031-06` permanece `in_progress`; checklist não foi marcado porque
  FC/Distância e demais critérios amplos da subtarefa continuam fora desta
  revisão delimitada.
