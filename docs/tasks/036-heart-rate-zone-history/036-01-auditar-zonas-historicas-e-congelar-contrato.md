# 036-01 — Auditar zonas históricas e congelar contrato

- **Ticker:** `036`
- **Número:** `01`
- **Status:** `completed`

## Objetivo e resultado esperado

Confirmar no código atual que a análise individual da Spec 034, a fonte bounded
da Spec 035, as janelas, a página cardiovascular, as cores e o renderer gráfico
suportam a 036. Congelar contrato de agregação, estados, denominadores,
parcialidade e fallback visual antes de implementação.

## Requisitos cobertos

- R1 a R10 da Spec 036, especialmente fonte única, população, janelas,
  invariantes de fechamento e decisão stacked/fallback.
- ADR-034 e ADR-035.
- Preservação da correção 035-08.

## Escopo incluído

- Revisar `WorkoutHeartRateAnalysisPolicy` e todos os estados individuais.
- Revisar `CardiovascularLoadWorkoutInput` e
  `observeCardiovascularLoadWorkoutsBetween()`.
- Revisar `HistoryWindows.from(asOf)` e determinar se extração compartilhada é
  necessária; preferir não extrair se a extensão ficar na mesma policy.
- Revisar `ExercisesViewModel`, `ExercisesScreen` e
  `CardiovascularLoadHistoryScreen`.
- Revisar `ChampionBarChart`, `ChampionCartesianChartData`, Vico e tema de
  gráfico.
- Revisar ordem, labels, cores e semantics já usados nas zonas da Spec 034.
- Congelar `zones`/coleção equivalente, nulidade, denominador e semântica de
  bucket sem alterar código.

## Escopo excluído

- Alteração de código funcional, Room, repository, DAO, UI ou renderer.
- Nova query, nova policy, migration, schema, teste automatizado ou fixture.
- Criação de workout/sample.

## Dependências

- `main` em `91bbaca` com 034/035 e 035-08.
- Spec/ADR 034 e 035 e todas as tasks 035 concluídas.
- Código de `core/model`, `data`, `core/database` e `feature/exercises`.

## Arquivos e símbolos prováveis

- `core/model/WatchWorkoutHeartRateAnalysis.kt`:
  `HeartRateZone`, `HeartRateZoneSummary`, `WorkoutHeartRateAnalysisPolicy`.
- `core/model/WorkoutHeartRateLoadHistory.kt`:
  `CardiovascularLoadHistoryPolicy`, `HistoryWindows`, resumos e buckets.
- `core/model/RepositoryContracts.kt`:
  `observeCardiovascularLoadWorkoutsBetween`.
- `data/RoomRepositories.kt`, `data/DatabaseMappers.kt` e
  `core/database/ChampionDaos.kt`.
- `feature/exercises/ExercisesViewModel.kt`, `ExercisesScreen.kt` e
  `CardiovascularLoadHistoryScreen.kt`.
- `core/designsystem/chart/ChampionChartContracts.kt`, `ChampionCharts.kt` e
  `ChampionChartTheme.kt`.

## Passos de implementação

1. Registrar branch/base e confirmar branch de trabalho
   `feat/036-heart-rate-zone-history` criada a partir da `main` auditada.
2. Confirmar que a fonte bounded contém todos os fatos da 036 e não depende de
   derivação, snapshots, splits, headers ou distância.
3. Confirmar uma única chamada da policy 034 por workout na composição atual.
4. Comparar a lógica privada de janelas com a semântica documentada da 035.
5. Confirmar que `ChampionBarChart` é grouped e não stacked.
6. Congelar extensão aditiva do read model, incluindo seis zonas, zonas zero,
   nulidade semântica e percentuais sobre duração analisada.
7. Registrar riscos e cenários físicos que poderão ser N/A sem fabricar dados.

## Testes e comandos de validação

- `git diff --check` para documentação.
- Inspeção de símbolos e referências, sem build obrigatório nesta subtarefa.
- Não criar nem executar testes automatizados.

## Definição de pronto

- Contrato escrito e coerente com 034/035.
- Fonte bounded aprovada sem alteração.
- Janelas compartilhadas sem segunda implementação.
- Decisão de UI e gráfico congelada.
- Riscos, nulidade, denominadores e estados documentados.

## Riscos e cuidados

- Não relaxar o read model mínimo que corrigiu a regressão Release.
- Não tratar seis séries grouped como stacked.
- Não criar paleta nova nem permitir classificação visual de esforço.
- Não expor BPM, samples, IDs ou payloads na documentação.

## Execução e evidências

### Base e consistência documental

- Branch confirmada: `feat/036-heart-rate-zone-history`.
- HEAD `c66ea1ce` contém `91bbaca` como ancestral; base auditada da Spec 036
  permanece coerente com o histórico declarado.
- Spec, ADR, overview e todas as subtarefas usam ticker `036`.
- `036-00-overview.md` possui uma única seção `## Checklist`, com seis itens,
  um por arquivo de subtarefa. Nenhuma subtarefa anterior estava concluída.
- Room efetivo permanece v20; nenhuma alteração funcional foi feita nesta
  auditoria.

### Fonte bounded e composição atual

- `WorkoutActivityDao.observeWatchActivitiesBetween()` e sua variante suspensa
  `findWatchActivitiesBetween()` filtram `origin = WATCH_D3_D4`,
  `workflowState != DISCARDED` e `startedAtEpochMillis` no intervalo
  semiaberto.
- A leitura bounded de samples usa os mesmos filtros da atividade, agrupa por
  activity em uma transação de leitura e mapeia somente
  `CardiovascularLoadWorkoutInput` com origem, workflow, início, duração, tipo e
  samples. Não lê imports, headers, snapshots, derivação, splits ou distância.
- O limite de 56 dias é exigido no repository. Portanto a fonte contém todos os
  fatos necessários à 036 e preserva a correção `035-08`; não há necessidade
  factual de query, mapper ou repository novo.
- Mapeamento inválido de origem, workflow, duração, tipo ou sample faz o mapper
  retornar `null`; o repository lança `CardiovascularLoadWorkoutMappingException`
  e o ViewModel expõe `Unavailable`. Esse é o caminho de falha estrutural
  `UNAVAILABLE`, distinto de workout individual não analisável que deve manter
  contadores e `partial` quando a composição bounded conseguir prosseguir.
- `ExercisesViewModel` combina uma única leitura bounded com
  `observeSnapshot()`. `asOf` e `ZoneId` são capturados uma vez na composição;
  mudança de `maxHeartRateBpm` recompõe somente o read model em memória.
- `CardiovascularLoadHistoryPolicy.analyze()` cria um único
  `AnalyzedWorkout` por entrada e chama `WorkoutHeartRateAnalysisPolicy` uma
  vez. O mesmo resultado é reutilizado nos quatro períodos, buckets e grupos
  atuais.

### Contrato congelado para implementação

- `WorkoutHeartRateAnalysisPolicy` continua única autoridade para classificação,
  sample-and-hold, cobertura, carga e as seis zonas
  `BELOW_ZONE_1`, `Z1`, `Z2`, `Z3`, `Z4`, `Z5`.
- `CardiovascularLoadPeriodSummary` e `CardiovascularLoadDailyBucket` receberão
  extensão aditiva `zones: List<HeartRateZoneSummary>?` ou nome equivalente,
  sem tipo paralelo. Coleção não nula terá exatamente seis entradas, inclusive
  durações zero. Ordem de domínio será `HeartRateZone.entries`; apresentação
  pode manter a ordem visual já usada no detalhe, `Z5` até `Abaixo de Z1`.
- `zones == null` significa ausência semântica: nenhum resultado `AVAILABLE` no
  período/dia. Não usar lista vazia, duração zero ou carga zero como substituto.
  Dia sem workout mantém `workoutCount == 0`; workout sem análise mantém seus
  contadores e a coleção ausente.
- Agregação soma `duration` por zona somente de análises `AVAILABLE`. O
  denominador de `percentage` é a soma das seis durações agregadas, isto é,
  `coveredHeartRateDuration` das análises `AVAILABLE`; nunca é
  `totalWorkoutDuration`. Percentuais fecham aproximadamente 100% quando o
  denominador é positivo.
- O denominador é calculado independentemente em cada resumo de período e em
  cada bucket diário: duração da zona dividida pela soma das seis zonas
  disponíveis daquele mesmo período/dia. Um bucket não usa o denominador de
  `current28` nem de outro bucket.
- A soma das seis durações fecha a duração coberta analisada. `coverageRatio`
  permanece independente: `coveredHeartRateDuration / totalWorkoutDuration`.
  Carga `0.0` não elimina uma análise disponível, inclusive quando todo tempo
  está em `BELOW_ZONE_1`.
- Estados e precedência permanecem os da 035: `UNAVAILABLE`, `NO_WORKOUTS`,
  `CONFIGURATION_REQUIRED`, `NO_ANALYZABLE_WORKOUTS`, `AVAILABLE`. Mistura de
  análises disponíveis com workouts sem análise permanece `AVAILABLE` e marca
  `partial`; qualquer workout elegível não disponível ou análise com cobertura
  menor que 1 torna `partial = true`.
- `HistoryWindows.from(asOf)` continuará única origem de
  `current7`, `previous7`, `current28` e `previous28`. Não extrair segunda
  implementação temporal. Buckets usam `startedAt` convertido pelo `ZoneId`,
  atribuem o workout inteiro ao dia de início e podem tocar 29 datas em bordas
  civis/DST, conforme contrato da 035.

### UI e renderer

- `CardiovascularLoadHistoryScreen` já é destino contextual existente; não criar
  nova aba, destino ou entrada em Home/Saúde.
- A seção atual preserva carga, comparação, contadores, cobertura, parcialidade,
  estados e gráfico diário da 035. A futura extensão adicionará rows das seis
  zonas, duração, percentual, cobertura e semantics factuais.
- O detalhe 034 já fixa ordem visual `Z5`, `Z4`, `Z3`, `Z2`, `Z1`,
  `Abaixo de Z1`, labels e associação de cores: `error`, `tertiary`, `primary`,
  `secondary`, `onSurfaceVariant`, `outline`, respectivamente. Essa associação
  será reutilizada; não criar paleta nova.
- `ChampionBarChart` usa `ColumnCartesianLayer.MergeMode.Grouped`; não há
  stacking em `ChampionCartesianChartData`, Vico ou tema. Distribuição temporal
  será uma única série por vez, selecionável por zona, com label e semantics
  identificando a zona. A série usará token não semântico já existente
  (`primarySeries`, sem criar paleta ou token novo); não passar seis séries para
  simular stacked.
- `ChampionChartFrame` preserva pontos nulos como ausência e não os converte em
  zero; dia sem workout pode ser zero real, enquanto workout sem análise deve
  continuar gap/ausência.

### Desvios e riscos residuais

- Nenhum desvio de requisito. Escopo exclui alteração funcional, portanto os
  arquivos de código permaneceram inalterados.
- Riscos aceitos: ausência de índice temporal composto na tabela de atividades,
  semântica sample-and-hold até o fim da sessão, recomposição histórica após
  mudança da FC máxima e possibilidade de 29 datas civis em bordas/DST.
- Validação física, fechamentos numéricos e cenários naturais de carga zero ou
  workout sem análise pertencem às subtarefas posteriores; não foram fabricados
  dados.

## Arquivos alterados

- `docs/tasks/036-heart-rate-zone-history/036-01-auditar-zonas-historicas-e-congelar-contrato.md`
- `docs/tasks/036-heart-rate-zone-history/036-00-overview.md`

## Comandos executados

- `git status --short --branch`
- `git log -1 --oneline`
- `git branch --show-current`
- `git rev-parse HEAD`
- `git rev-parse main`
- `git merge-base --is-ancestor 91bbaca HEAD`
- Inspeção de símbolos e referências em domínio, DAO, repository, ViewModel,
  Compose, navigation e design system de gráficos.
- `git diff --check`

## Resultado e riscos residuais

- Auditoria concluída sem alteração de código funcional, persistência, Room,
  UI, renderer, testes, fixtures ou Health Connect.
- Contrato de agregação, nulidade, seis zonas, denominadores, parcialidade,
  janelas e fallback gráfico está congelado para `036-02` em diante.
- Sem bloqueio para próxima subtarefa. Riscos residuais permanecem registrados
  acima e na Spec/ADR 036.

## Revisão independente

- Agente `review` aprovou estrutura SDD, ticker, checklist, escopo documental,
  evidências da fonte bounded, contrato de denominadores e fallback gráfico.
- Findings documentais sobre `findWatchActivitiesBetween()`, falha estrutural
  `UNAVAILABLE`, denominador diário, terminologia de zonas e token
  `primarySeries` foram corrigidos antes da aprovação final.
- Ressalvas mantidas: build, testes automatizados e validação física pertencem
  às subtarefas posteriores, especialmente `036-06`.
