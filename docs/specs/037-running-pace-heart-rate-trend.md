# Spec 037 — Tendência de corrida: relação entre ritmo e frequência cardíaca

- **Ticker:** `037`
- **Status:** `planned`
- **Branch documental:** `feat/037-running-pace-heart-rate-trend`
- **Base auditada:** `main` em `201a7b8c80d176c2f726176fa9107a7326a62f22`
- **Base declarada pelo pedido:** `201a7b8` — merge da Spec 036
- **Data da auditoria:** 2026-09-23
- **ADR:** [ADR-037](../decisions/adr-037-running-pace-heart-rate-trend.md)

## Contexto e objetivo

A Spec 037 deve responder de forma factual:

> Nas minhas corridas, como meu ritmo e minha frequência cardíaca estão
> variando ao longo do tempo?

O resultado será um histórico local, bounded e efêmero de corridas iniciadas no
relógio. Cada corrida elegível terá um ponto temporal com ritmo canônico,
proveniência da distância, média de frequência cardíaca ponderada pela duração
coberta e cobertura de FC quando houver amostras válidas.

A feature descreve relação observada entre fatos persistidos. Não infere
causalidade, adaptação fisiológica, eficiência, condicionamento ou melhora de
desempenho. Percurso, inclinação, vento, temperatura, superfície, fadiga, sono,
estrutura do treino e diferença entre intervalado e contínuo não são
normalizados.

O ticker `037` foi confirmado livre: não existe spec `037-*`, ADR-037 ou
diretório `docs/tasks/037-*` antes desta auditoria. A branch
`feat/037-running-pace-heart-rate-trend` foi criada a partir da `main` em
`201a7b8c80d176c2f726176fa9107a7326a62f22`.

## Limites de produto

### Incluído

- histórico dos últimos 56 dias;
- uma linha por corrida, sem agregação diária;
- população estrita de corridas confirmadas;
- ritmo calculado dos escalares canônicos do snapshot ativo;
- frequência cardíaca média ponderada pela duração da timeline sample-and-hold
  da Spec 034;
- cobertura de FC exposta separadamente;
- proveniência da distância traduzida em labels compreensíveis;
- entrada contextual e tela própria na subtree de Exercícios;
- dois gráficos temporais independentes, um para ritmo e outro para FC média;
- read model local, bounded, sem persistência analítica.

### Explicitamente excluído

- score de eficiência;
- índice pace/BPM;
- VO2max estimado;
- fitness score;
- classificação bom/ruim;
- recomendação de treino;
- recuperação, fadiga ou risco;
- regressão, slope, forecast ou interpretação positiva/negativa;
- scatter pace × BPM na primeira versão;
- comparação percentual current28/previous28;
- reprocessamento, recalibração ou edição de snapshots;
- nova tabela, coluna, cache, backfill ou migration;
- qualquer mudança em Room, schema, Health Connect, parser, BLE ou
  sincronização.

## Base e auditoria realizada

### Estado do repositório

- `HEAD` inicial era `201a7b8 (HEAD -> main, origin/main, origin/HEAD)`;
- `git show -s --format=%H 201a7b8` confirmou
  `201a7b8c80d176c2f726176fa9107a7326a62f22`;
- working tree inicial estava limpo;
- branch nova foi criada sem recriar branch existente;
- `docs/specs/036-heart-rate-zone-history.md` e
  `docs/decisions/adr-036-heart-rate-zone-history.md` registram Spec 036
  concluída em Room v20.

### Documentos e evidências auditados

- `AGENTS.md`;
- `docs/baseline/current-baseline.md`;
- `docs/roadmap/post-phase-15-direction.md`;
- `docs/specs/032-d4-native-workout-metrics.md`;
- `docs/decisions/adr-029-d4-native-metrics-and-distance-calibration.md`;
- `docs/specs/034-heart-rate-zones-training-load.md`;
- `docs/decisions/adr-034-heart-rate-zones-training-load.md`;
- `docs/specs/035-training-load-history-trends.md`;
- `docs/decisions/adr-035-training-load-history-trends.md`;
- `docs/tasks/035-training-load-history-trends/035-08-corrigir-leitura-historica-release.md`;
- `docs/specs/036-heart-rate-zone-history.md`;
- `docs/decisions/adr-036-heart-rate-zone-history.md`;
- `docs/integration/032-08-physical-validation.md`;
- `docs/integration/034-06-physical-validation.md`;
- `docs/integration/035-06-physical-validation.md`;
- `docs/integration/036-06-physical-validation.md`;
- `docs/tasks/035-training-load-history-trends/035-00-overview.md`;
- `docs/tasks/036-heart-rate-zone-history/036-00-overview.md`.

As evidências físicas 032, 034, 035 e 036 são sanitizadas. Há evidência de
distância nativa, calibração, fallback, timeline D4, sample-and-hold e Room
v20, mas não há banco físico no working tree nem evidência sanitizada específica
da composição de corridas `confirmedType == RUNNING` com mistura completa de
proveniências para a 037. A validação futura deve confirmar essa composição em
banco real somente para inspeção. Nenhuma distribuição foi inventada nesta
auditoria.

### Código auditado

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutHeartRateAnalysis.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WorkoutHeartRateLoadHistory.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/CalendarHistory.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/CardiovascularLoadHistoryScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/AppDestination.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/chart/ChampionChartContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/chart/ChampionCharts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/chart/ChampionChartTheme.kt`.

### Precisões da auditoria 037-01

- O símbolo de domínio vigente é `WorkoutActivity`; `WatchWorkoutActivity.kt` é o
  arquivo que contém esse contrato. `activityOrigin` é o campo autoritativo e
  `origin` é uma propriedade de compatibilidade. `confirmedType` permanece
  nullable e separado de `suggestedType` (`WatchWorkoutActivity.kt:245-264`).
- As consultas atuais `observeWatchActivitiesBetween()` e
  `findWatchActivitiesBetween()` filtram origem, descarte e janela, mas ainda não
  filtram `confirmedType == RUNNING` (`ChampionDaos.kt:306-330`). Portanto, a
  população estrita da 037 exige query dedicada; não pode reutilizar essas
  consultas sem adaptação.
- `WorkoutDerivationSnapshotEntity` persiste os nomes escalares
  `canonicalDistanceMeters`, `canonicalCoveredDurationNanos`,
  `temporalEligibility` e `temporalEligibilityReasons`
  (`ChampionDatabase.kt:547-572`). No domínio, a leitura equivalente aparece
  como `canonicalDistanceMeters`, `canonicalSeries?.coveredDuration` e
  `temporalResolution.eligibility/reasons` (`WatchWorkoutDerivedMetrics.kt:1194-1304`).
  A futura projection deve distinguir nomes persistidos de nomes de domínio e
  não reidratar o snapshot completo.
- O código declara `WATCH_WORKOUT_NATIVE_DERIVATION_VERSION =
  "watch-d3-d4-v4"` (`WatchWorkoutDerivedMetrics.kt:11-18`). Essa é a versão
  nativa suportada pelo contrato; versões divergentes continuam fatos históricos
  não comparáveis quando os escalares permitirem listagem factual.
- `WorkoutHeartRateAnalysisPolicy` filtra samples na janela canônica, valida
  BPM `30..220` e timestamps estritamente crescentes, usa
  `sampleAndHoldWindows()` e calcula `coveredDuration`/`coverageRatio`
  (`WatchWorkoutHeartRateAnalysis.kt:83-160`). A média ponderada da 037 deve
  compartilhar essa timeline e validar os mesmos fatos, sem exigir FC máxima.
- A fonte cardiovascular atual permanece deliberadamente mínima:
  `CardiovascularLoadWorkoutInput` não contém snapshot ou distância;
  `RoomRepositories` carrega atividades e samples em lote
  (`RepositoryContracts.kt:399-415`; `RoomRepositories.kt:1085-1120`). A 037
  cria contrato próprio e não amplia esse input.
- O read model de tendência ainda não existe. Exercícios possui somente estado
  de histórico cardiovascular (`ExercisesFeatureContract.kt:26-56`;
  `ExercisesViewModel.kt:399-420`), e navegação possui somente o destino
  contextual de carga (`AppDestination.kt:56-64`; `ChampionNavigation.kt:435-450`).
- `ChampionChartSeries` exige eixo X estritamente crescente e
  `ChampionChartPoint.y == null` representa ausência (`ChampionChartContracts.kt:5-36`).
  Empates de `startedAt` exigem posição ordinal na entrada da série, sem epsilon
  temporal ou substituição de ausência por zero.
- Não há banco físico local sanitizado no checkout (`champion-release-db/` e
  arquivos `*.db` ausentes). As evidências 032/034/035/036 continuam referências
  documentais; composição física específica de corridas `RUNNING` e mistura de
  proveniências fica `N/A` até 037-06.

## Comportamento atual encontrado

### População e tipo

`WorkoutActivity` diferencia `suggestedType` de `confirmedType`, e o DAO da
035/036 filtra `WATCH_D3_D4` e `workflowState != DISCARDED`. A 035 resolve tipo
com `confirmedType ?: suggestedType ?: UNKNOWN`, o que é correto para carga
cardiovascular, mas não para esta análise longitudinal. A 037 congela
`confirmedType == RUNNING` como filtro obrigatório. `suggestedType == RUNNING`
sozinho não participa.

`TREADMILL`, `WALKING`, `FREE_WORKOUT`, `CYCLING`, `UNKNOWN`, origens phone e
atividades descartadas não participam da população da 037. Estados operacionais
não descartados permanecem elegíveis, inclusive `PENDING_REVIEW`,
`PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCING`, `SYNCED` e `SYNC_FAILED`.

### Distância e ritmo

O snapshot ativo persiste `canonicalDistanceMeters`,
`canonicalCoveredDurationNanos`, `derivationMethod`, `derivationVersion`,
`distanceCalibrationFactor`, `temporalEligibility`, razões temporais e
proveniência de plausibilidade. `CanonicalWorkoutSeries` calcula pace acumulado
pela duração coberta dividida pela distância canônica, mas a 037 não precisa
reconstruir essa série.

`WorkoutActivity.metrics.averagePaceSecondsPerKilometer` é agregado importado e
não representa sozinho a fonte histórica comparável. A 037 não o usa.

### Proveniência atual

`WorkoutDerivationMethod` contém:

- `WATCH_NATIVE`;
- `WATCH_NATIVE_CONTEXT_CALIBRATED`;
- `STEP_LENGTH_FALLBACK`;
- `FIXED_PERSONAL_STEP_LENGTH`.

As duas primeiras preservam lineage nativa D4. As duas últimas estimam distância
por comprimento de passo e não devem ser misturadas silenciosamente ao trend
principal.

### Frequência cardíaca atual

`WorkoutHeartRateSample` é persistido com timestamp, BPM e índice de origem.
`WorkoutHeartRateAnalysisPolicy` filtra samples na janela
`[startedAt, startedAt + duration)`, valida BPM e timestamps, aplica
sample-and-hold, calcula `coveredDuration` e `coverageRatio`, sem interpolação,
timeout ou threshold de cobertura. A policy atual usa essa timeline para zonas e
carga, enquanto `WorkoutActivity.metrics.averageBpm` pode ser uma média
aritmética do parser D4.

`WorkoutSplitCalculator.weightedBpm` já calcula média ponderada por duração em
trechos, mas a implementação futura deve extrair uma primitiva compartilhada
sem alterar a semântica existente dos splits ou das zonas.

### Read models atuais

`CardiovascularLoadWorkoutInput` contém apenas origem, workflow, início,
duração, tipo e samples. A correção 035-08 removeu derivação, distância,
intervalos, splits e snapshots desse caminho para impedir que snapshots
históricos incompatíveis derrubem a leitura bounded. A 036 reutiliza exatamente
esse contrato.

`CalendarWorkoutSummary` já usa `LEFT JOIN` no snapshot ativo e expõe alguns
escalares canônicos, mas sua população, finalidade e campos não fecham o
contrato de comparabilidade da 037. Não será reutilizado como fonte final.

### DAO, repository e Room

O caminho bounded da 035 executa uma leitura de atividades e uma leitura batch
de samples, agrupando samples por `activityId` dentro de transação. Não há
`Flow` por workout nem consulta DAO no ViewModel. O índice temporal composto da
tabela `workout_activities` continua ausente; o horizonte de 56 dias limita o
risco, mas não justifica migration ou índice especulativo.

`ChampionDatabase` está em Room v20. O schema atual já contém os escalares
necessários no snapshot e o índice de samples por atividade. Não há tabela ou
coluna analítica de tendência.

### Exercícios e gráficos

Exercícios usa navegação contextual com back stack manual. Histórico de carga
cardiovascular possui destino próprio contextual e não é aba principal. A 037
deve seguir esse padrão, mas manter pergunta de pace/FC separada da carga.

`ChampionCartesianChartData` tem um eixo Y; `ChampionChartSeries` exige X
estritamente crescente. `ChampionLineChart` não possui scatter nem eixo Y
secundário. `ChampionTimeSeriesChart` e dois `ChampionLineChart` separados são
suficientes. Valores ausentes são `y = null`, nunca zero.

## Contrato congelado

### 1. População exata

```text
origin == WATCH_D3_D4
confirmedType == RUNNING
workflowState != DISCARDED
startedAt ∈ [asOf - 56 dias, asOf)
```

`suggestedType` não substitui `confirmedType`. Corridas confirmadas continuam
na população independentemente do workflow não descartado; o estado operacional
não é classificação de comparabilidade.

### 2. Range temporal

Usar uma única janela rolling semiaberta de 56 dias:

```text
[asOf - 56 dias, asOf)
```

`asOf` e `ZoneId` entram explicitamente no contrato. O domínio não chama
`Instant.now()`. `startedAt` define pertencimento e ordenação; uma atividade
que atravessa mudança de dia não é dividida. Não haverá `current28`,
`previous28`, delta ou percentual de melhora nesta versão.

### 3. Fonte e fórmula do pace

A fonte autoritativa é o snapshot ativo unido por
`activeActivityId = activity.id AND isActive = 1`, usando somente:

- `canonicalDistanceMeters`;
- `canonicalCoveredDurationNanos`.

O pace interno é segundos por quilômetro:

```text
paceSecondsPerKilometer =
    (canonicalCoveredDurationNanos / 1_000_000_000.0)
    /
    (canonicalDistanceMeters / 1_000.0)
```

Não usar `averagePaceSecondsPerKilometer`, `averageSpeedMetersPerSecond`,
`WorkoutActivity` completo, `CanonicalWorkoutSeries`,
`DerivedWorkoutInterval`, `WorkoutSplit`, header D4 ou fórmula baseada em
samples para reconstruir o pace. Não arredondar antes dos cálculos.

### 4. Elegibilidade objetiva do pace

Uma linha fornece pace comparável somente quando:

1. existe snapshot ativo para a atividade;
2. `canonicalDistanceMeters` é finito e maior que zero;
3. `canonicalCoveredDurationNanos` existe, é positivo e pode ser convertido
   com segurança em `Duration`;
4. duração coberta não excede a duração positiva da atividade;
5. `temporalEligibility == ELIGIBLE` e razões temporais não contradizem esse
   estado;
6. `derivationMethod` é `WATCH_NATIVE` ou
   `WATCH_NATIVE_CONTEXT_CALIBRATED`;
7. `derivationVersion` é a versão nativa suportada
   `watch-d3-d4-v4`;
8. para `WATCH_NATIVE`, fator de calibração é ausente ou nulo conforme o
   contrato do snapshot;
9. para `WATCH_NATIVE_CONTEXT_CALIBRATED`,
   `distanceCalibrationFactor` é finito e maior que zero;
10. não há `WorkoutPlausibilityIndicator` bloqueador.

Falta ou inconsistência não vira pace zero. A linha permanece observável com
estado tipado de inelegibilidade. Um snapshot estruturalmente inválido não é
silenciosamente ignorado como se a corrida não existisse.

### 5. Política de proveniência e comparabilidade

`WATCH_NATIVE` e `WATCH_NATIVE_CONTEXT_CALIBRATED` participam do mesmo trend
principal factual porque ambos são lineage D4 nativa e usam os mesmos escalares
canônicos. A calibração contextual altera a distância canônica preservada no
snapshot, mas não cria uma medição externa independente. Proveniência permanece
campo obrigatório do ponto e label visível na lista.

`STEP_LENGTH_FALLBACK` e `FIXED_PERSONAL_STEP_LENGTH` podem exibir pace
estimado na lista quando seus escalares canônicos forem válidos, mas recebem
estado `AVAILABLE_FALLBACK_NOT_COMPARABLE` e não entram nos gráficos nem na
série comparável. Não reprocessar histórico para uniformizar fontes.

Versão nativa diferente de `watch-d3-d4-v4` pode permanecer listável com estado
`AVAILABLE_UNSUPPORTED_VERSION_NOT_COMPARABLE` se escalares fecharem. A lista
exibe pace factual e label `Distância do relógio — versão não comparável`; o
ponto fica fora dos gráficos e da série principal. Proveniência desconhecida ou
escalares inválidos não produzem valor.

Plausibilidade local usa `indicators.isEmpty()` como requisito. A policy
`NO_GLOBAL_THRESHOLD_BLOCKS_PROJECTION` não bloqueia este read model local
quando não há indicador conhecido, pois seu bloqueio é da projeção funcional/
remota e a 037 não escreve dados. A 037 não cria threshold nem reutiliza ação de
aceite para promover atividade remotamente. Qualquer indicador
`IMPLAUSIBLE_CADENCE` ou `IMPLAUSIBLE_SPEED` bloqueia comparabilidade.

Labels de usuário:

| Método interno | Label factual |
| --- | --- |
| `WATCH_NATIVE` | `Distância do relógio` |
| `WATCH_NATIVE_CONTEXT_CALIBRATED` | `Distância do relógio calibrada` |
| `STEP_LENGTH_FALLBACK` | `Distância estimada por comprimento de passo` |
| `FIXED_PERSONAL_STEP_LENGTH` | `Distância estimada por comprimento de passo` |

Nomes internos de enumeração não aparecem na UI.

### 6. Fonte e fórmula da FC média

A fonte é `WorkoutHeartRateSample` persistido. A implementação deve extrair uma
primitiva compartilhada, conceitualmente `WorkoutHeartRateTimelinePolicy`, que
preserve a validação, ordenação, filtro temporal e sample-and-hold da Spec 034.
`WorkoutHeartRateAnalysisPolicy` continuará sendo a autoridade de zonas e carga;
037 usará a mesma timeline para média.

Para samples válidos na janela:

```text
averageHeartRateBpm =
    Σ(bpm × duração da janela sample-and-hold)
    ------------------------------------------
    coveredHeartRateDuration
```

O denominador é a duração coberta, não a quantidade de samples. Não usar
`WorkoutActivity.metrics.averageBpm`, não duplicar filtro/ordenação/validação,
não usar FC máxima e não alterar thresholds ou zonas da 034.

### 7. Cobertura de FC e parcialidade

- timeline com samples presentes na janela canônica expõe média e
  `coverageRatio` somente quando todos esses samples são válidos; cobertura
  temporal pode ser parcial porque o trecho anterior ao primeiro sample continua
  descoberto;
- cobertura parcial permanece válida e factual, sem threshold de 80%, 90% ou
  outro valor inventado;
- cobertura completa é `coverageRatio == 1.0` após a regra do domínio;
- sem samples na janela, média e cobertura ficam ausentes com status
  `NO_HEART_RATE_SAMPLES`;
- ausência de FC não remove ponto que possui pace comparável;
- ausência de FC não é `averageHeartRateBpm = 0`;
- status de ponto `partial` indica FC ausente ou cobertura parcial, não uma
  classificação de qualidade ou desempenho.

### 8. Resultado tipado e estados

Nomes congelados para o domínio:

- `RunningPerformanceHistory`;
- `RunningPerformancePoint`;
- `RunningPerformanceHistoryStatus`:
  `AVAILABLE`, `NO_RUNS`, `NO_COMPARABLE_RUNS`, `UNAVAILABLE`;
- `RunningPerformancePaceStatus`:
  `COMPARABLE_NATIVE`, `AVAILABLE_FALLBACK_NOT_COMPARABLE`,
  `AVAILABLE_UNSUPPORTED_VERSION_NOT_COMPARABLE`, `NO_ACTIVE_SNAPSHOT`,
  `TEMPORAL_INELIGIBLE`, `PLAUSIBILITY_BLOCKED`, `INVALID_CANONICAL_FACTS`;
- `RunningPerformanceHeartRateStatus`:
  `AVAILABLE`, `NO_HEART_RATE_SAMPLES`, `INVALID_SAMPLES`,
  `SOURCE_UNAVAILABLE`;
- `RunningPerformanceDistanceProvenance`;
- `RunningPerformanceHistoryPolicy`.

`AVAILABLE` exige ao menos um ponto com pace `COMPARABLE_NATIVE`. `NO_RUNS`
significa que não existe corrida confirmada na janela. `NO_COMPARABLE_RUNS`
significa que existem corridas, mas nenhuma fornece pace comparável. Falha
estrutural da fonte ou composição produz `UNAVAILABLE`.

Falha estrutural de uma linha produz estado naquela linha e não derruba outras
corridas. Falha de leitura da fonte de atividades/snapshot produz
`UNAVAILABLE`. Uma falha batch de samples deve preservar o pace já composto,
mas marcar a fonte de FC indisponível sem fabricar média. A matriz congelada é:

| Situação da linha/fonte | Status de FC | Pace da linha | Status do histórico |
| --- | --- | --- | --- |
| nenhum sample na janela canônica | `NO_HEART_RATE_SAMPLES` | preservado | não torna indisponível |
| qualquer sample na janela com BPM inválido ou timestamp não crescente | `INVALID_SAMPLES` | preservado | não torna indisponível |
| consulta batch de samples falha | `SOURCE_UNAVAILABLE` | preservado quando já composto | `UNAVAILABLE` |
| todos os samples na janela válidos, cobertura parcial ou completa | `AVAILABLE` | preservado | não torna indisponível |

Falha de fonte significa erro estrutural de leitura/composição, não ausência
natural de amostras. Samples mistos válidos/inválidos seguem `INVALID_SAMPLES`,
em alinhamento fail-closed com a 034. Samples fora da janela não participam da
decisão. `SOURCE_UNAVAILABLE` é estado tipado de linha; também torna o status
top-level `UNAVAILABLE`, sem renderizar histórico parcial como disponível. A UI
não transforma qualquer estado em zero e o Compose não decide precedência.

### 9. Ponto, ordem e múltiplas corridas

`RunningPerformancePoint` carrega, no mínimo:

- `startedAt`;
- `canonicalDistanceMeters` quando disponível;
- `canonicalCoveredDuration` quando disponível;
- `paceSecondsPerKilometer` quando disponível;
- `averageHeartRateBpm` quando disponível;
- `heartRateCoverage` quando houver análise válida;
- `paceStatus`;
- `heartRateStatus`;
- `derivationVersion` quando disponível;
- `distanceProvenance` quando conhecida;
- `partial`.

`activityId` permanece somente no record interno da consulta para agrupar
samples e desempatar. Não chega ao contrato visual na primeira versão e não é
exposto em semantics, documentação ou evidência.

A policy retorna pontos em ordem ascendente por `startedAt`, com desempate
determinístico interno por ID. Corridas no mesmo dia permanecem distintas. Se
duas tiverem exatamente o mesmo instante, o gráfico usa posição ordinal
estritamente crescente e conserva o instante real em label; não usa epsilon
falso nem agrega por dia.

`asOf` e `ZoneId` são parâmetros explícitos e estáveis. `ZoneId` serve para
labels civis de data/hora; não cria buckets nem altera pertencimento temporal.

### 10. Read model, query e anti-N+1

Criar read model dedicado, conceitualmente `RunningPerformanceWorkoutInput`, e
método `observeRunningPerformanceWorkoutsBetween(range)` no
`WorkoutActivityRepository`.

A query de atividade deve filtrar no SQL origem, tipo confirmado, descarte e
janela, e fazer `LEFT JOIN` do snapshot ativo. Deve selecionar somente os fatos
mínimos:

- origem, workflow, início e duração;
- `confirmedType`;
- `canonicalDistanceMeters`;
- `canonicalCoveredDurationNanos`;
- `derivationMethod` e `derivationVersion`;
- `distanceCalibrationFactor`;
- elegibilidade/razões temporais necessárias;
- policy/indicadores de plausibilidade;
- ID interno da atividade para agrupamento.

Samples devem ser carregados em uma segunda consulta bounded batch e agrupados
em memória dentro de uma transação de leitura. Não criar `Flow` por atividade,
N+1, consulta DAO no ViewModel, leitura global, `CalendarWorkoutSummary` como
fonte final ou hidratação de `WorkoutActivity` completo.

O read model não carrega splits, derived intervals, header D4, payload BLE,
imports completos, Health Connect, rota ou GPS.

### 11. UI e gráficos

Adicionar CTA contextual **Tendência de corrida** em Exercícios e abrir tela
própria na subtree contextual. Não criar aba principal nem entrada em Home,
Saúde ou página de carga cardiovascular.

A tela deve mostrar:

- título `Tendência de corrida`;
- explicação factual `Ritmo e frequência cardíaca`;
- período `Últimos 56 dias`;
- lista/cards por corrida, suficiente sem depender do gráfico;
- ritmo em `m:ss /km`;
- distância em km com precisão coerente com Exercícios;
- FC média em `bpm`;
- coverage separado como percentual;
- label de proveniência sem enumeração interna;
- estados de ausência e falha sem linguagem avaliativa.

Usar dois gráficos temporais alinhados, cada um com eixo Y próprio:

1. ritmo, unidade `s/km`, formatter de apresentação `m:ss /km`;
2. FC média, unidade `bpm`, com `y = null` para corrida sem FC.

Ambos usam a mesma ordem ordinal dos pontos e labels de data/hora derivados de
`startedAt`/`ZoneId`. O gráfico não deve usar pace e BPM na mesma escala, dual
axis falso ou linha com eixo X = BPM. Scatter é evolução posterior, pois o
design system atual não possui contrato para pontos independentes, e BPM
repetido não satisfaz a semântica de `ChampionChartSeries`.

Acessibilidade deve anunciar período, quantidade de corridas, métrica, unidade,
lacunas de FC, parcialidade, valor e provenance. Cor nunca é único canal.

### 12. Persistência, Room e Health Connect

Room permanece v20 e não haverá migration, schema novo, índice novo, entidade,
coluna, tabela, cache, snapshot de performance ou backfill.

Health Connect fica completamente fora do escopo: sem permissões, records,
payloads, IDs, versões, fila, retry, reconciliação, readback ou efeitos de
abertura/reabertura da tela.

A 037 observa snapshots e calibrações existentes. Não reprocessa atividades,
recalibra corridas, edita snapshots, materializa snapshot novo, altera perfil,
step length, distância canônica, workflow ou sincronização.

## Critérios de aceite

1. A população usa somente `WATCH_D3_D4`, `confirmedType == RUNNING`,
   `workflowState != DISCARDED` e `startedAt` na janela de 56 dias.
2. `suggestedType == RUNNING` sem confirmação não participa.
3. `TREADMILL`, `WALKING`, `FREE_WORKOUT`, `CYCLING`, `UNKNOWN` e origens phone
   ficam fora.
4. O resultado usa uma única janela `[asOf - 56 dias, asOf)` sem delta,
   slope, score ou comparação de melhora.
5. Pace fecha independentemente com
   `canonicalCoveredDuration / canonicalDistance` em unidades SI, sem usar o
   agregado persistido de pace.
6. Distância ausente, não positiva, duração coberta inválida, temporalidade
   inelegível, método desconhecido ou versão não suportada não produzem zero e
   recebem estado tipado.
7. `WATCH_NATIVE` e `WATCH_NATIVE_CONTEXT_CALIBRATED` válidos entram no trend
   principal com provenance visível.
8. Fallback por comprimento de passo pode aparecer na lista, mas não nos
   gráficos comparáveis e é marcado como estimado/não comparável.
9. Indicadores de plausibilidade bloqueiam comparabilidade; ausência de
   indicador não exige aceite remoto para este read model local.
10. FC média usa sample-and-hold compartilhado com a 034 e média ponderada por
    duração coberta; nenhum filtro, threshold, zona ou validação é duplicado.
11. FC parcial mostra média e coverage; ausência de FC não apaga pace nem vira
    zero.
12. Estados distinguem nenhuma corrida, corrida sem pace comparável, pace com FC
    indisponível e falha estrutural da fonte.
13. Uma linha estruturalmente inválida não derruba corridas válidas; falha de
    fonte inteira é `UNAVAILABLE`.
14. O read model usa query dedicada, snapshot ativo, duas leituras bounded
    batch e nenhum N+1/Flow por atividade.
15. Nenhum snapshot completo, import, header, intervalo, split, payload, rota,
    GPS ou Health Connect é carregado.
16. Pontos são ordenados por `startedAt`; corridas do mesmo dia e do mesmo
    instante permanecem distintas.
17. Exercícios oferece entrada contextual e tela própria, separada da carga
    cardiovascular e sem nova aba principal.
18. Ritmo e FC usam gráficos separados com eixo Y próprio; não há dual-axis,
    normalização artificial ou scatter simulado.
19. UI usa `m:ss /km`, `bpm`, percentual de coverage e labels de provenance
    factuais, sem linguagem de melhora, piora, eficiência ou condicionamento.
20. Room continua v20, sem migration, schema, persistência analítica ou
    backfill.
21. Health Connect, calibração, derivação, workflow e snapshots não sofrem
    efeitos causados pela feature.
22. Validação futura usa banco real existente, cálculo independente, registra
    `N/A` sem fabricar dados e captura screenshot sanitizada.

## Abordagem escolhida

1. Congelar contrato e comparabilidade antes da implementação em `037-01`.
2. Extrair uma primitiva pura de timeline de FC que preserve a 034; adicionar a
   média ponderada sem ampliar a semântica de zonas ou carga em `037-02`.
3. Criar input/read model dedicado de performance de corrida e policy pura, sem
   modificar `CardiovascularLoadWorkoutInput`.
4. Implementar query bounded dedicada com projeção mínima de atividade +
   snapshot ativo e leitura batch de samples em `037-03`.
5. Integrar estado, CTA e destino contextual no `ExercisesViewModel` e
   navegação em `037-04`, sem cálculo em Compose.
6. Renderizar cards e dois gráficos com unidades independentes, provenance,
   coverage e semantics em `037-05`.
7. Validar banco físico real, cálculos, preservação e ausência de efeitos em
   `037-06`.

## Alternativas descartadas

- **Reutilizar `CardiovascularLoadWorkoutInput`:** ele não contém distância,
  snapshot ou provenance e foi deliberadamente reduzido pela 035-08.
- **Reutilizar `CalendarWorkoutSummary`:** população e semântica são de
  calendário; faltam duração coberta e campos completos de comparabilidade.
- **Reidratar `WorkoutActivity`/snapshot completo:** reabre regressão Release
  da 035-08 e valida splits/derivação histórica sem necessidade.
- **Usar `metrics.averagePaceSecondsPerKilometer`:** mistura agregado importado
  e distância de origem com a série canônica.
- **Misturar fallback ao trend:** combina distância nativa com estimativa por
  passo e falsifica comparabilidade.
- **Exigir `maxHeartRateBpm`:** média de BPM e coverage não dependem de FC
  máxima, zonas ou carga.
- **Usar média aritmética de samples:** ignora duração e diverge da timeline
  da 034.
- **Criar sample-and-hold paralelo:** permite divergência temporal; a primitiva
  compartilhada será única.
- **Agir somente após aceite de plausibilidade:** aceite atual protege
  projeção/Health Connect; a 037 é análise local e só bloqueia indicadores
  conhecidos.
- **current28/previous28:** adiciona comparação e percentual sem requisito,
  além de exigir agregação semântica para pace.
- **Agregação diária:** perde corridas múltiplas no mesmo dia.
- **Timestamp com epsilon:** fabrica espaçamento temporal; a solução ordinal é
  determinística e conserva `startedAt` nos labels.
- **Um gráfico dual-axis ou pace/BPM scatter simulado:** o design system não
  possui contrato honesto para isso.
- **Nova tabela/cache:** não há necessidade de persistir análise recalculável.

## Arquivos, módulos e contratos afetados

### Implementação futura provável

- `core/model/WatchWorkoutHeartRateAnalysis.kt` — extração behavior-preserving
  da timeline compartilhada;
- `core/model/RunningPerformanceHistory.kt` ou nome equivalente no mesmo
  pacote — input, estados, ponto, provenance e policy;
- `core/model/RepositoryContracts.kt` — método bounded dedicado;
- `core/database/ChampionDaos.kt` — projection/query do snapshot ativo e
  atividade confirmada;
- `data/DatabaseMappers.kt` — mapper mínimo da projection e samples;
- `data/RoomRepositories.kt` — composição batched sem reidratação;
- `feature/exercises/ExercisesFeatureContract.kt` — estado separado da carga;
- `feature/exercises/ExercisesViewModel.kt` — composição e exposição do
  resultado;
- `feature/exercises/ExercisesScreen.kt` — CTA contextual;
- novo `feature/exercises/RunningPerformanceHistoryScreen.kt` — lista,
  estados, gráficos e acessibilidade;
- `core/navigation/AppDestination.kt` e `ChampionNavigation.kt` — destino
  contextual serializável;
- formatter/helper de pace no domínio ou presentation, sem arredondamento
  prévio ao cálculo.

### Explicitamente não afetados

- `ChampionDatabase.kt` como versão/schema;
- `ChampionMigrations.kt` e schemas exportados;
- entidades `WorkoutActivityEntity`, `WorkoutDerivationSnapshotEntity` e
  `WorkoutHeartRateSampleEntity` como estrutura persistida;
- `WorkoutActivity`, snapshots, intervalos, derived intervals e splits;
- parser D3/D4, BLE, calibração e workflow;
- permissões, records, writer, fila e contratos Health Connect;
- `CardiovascularLoadWorkoutInput`, `CardiovascularLoadHistoryPolicy` e UI da
  carga cardiovascular, salvo adaptação de navegação comprovadamente isolada;
- design system de gráficos compartilhado.

## Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| Poucas corridas comparáveis por snapshots antigos/fallback | Mostrar lista factual e status de cada linha; não ampliar população por conveniência. |
| Snapshot ativo ausente ou inválido | `LEFT JOIN`, estado por linha e falha fechada; nunca converter para zero. |
| Mistura acidental de proveniências | Filtrar trend por lineage nativa, carregar provenance e label visível. |
| Divergência entre média FC e 034 | Extrair uma única primitiva temporal e preservar saída de zonas/carga. |
| Regressão 035-08 | Não tocar `CardiovascularLoadWorkoutInput` nem reidratação completa. |
| N+1 ou range ilimitado | Query dedicada bounded de 56 dias, duas leituras batch e agrupamento transacional. |
| Mesmo instante quebrar gráfico | Usar índice ordinal X após ordenação por `startedAt`, sem epsilon. |
| Ausência virar zero | Nulidade e estados tipados; `y = null` para FC ausente. |
| Usuário interpretar provenance como julgamento | Labels descritivos, sem bom/ruim, eficiente ou melhora. |
| Dois gráficos aumentarem altura | Manter tela própria e lista legível; não sacrificar unidade correta por compactação. |
| Query temporal sem índice composto | Medir plano/custo na validação; qualquer migration/index fica em decisão separada. |
| Ruído de inicialização gerar falsa atribuição | Capturar snapshots físicos e call path; registrar causalidade não isolável como N/A/risco. |
| Dados sensíveis em documentação | Sanitizar screenshot e evidência; não registrar IDs, MAC, payload, BPM sample a sample ou localização. |

## Estratégia de testes e validação

Esta execução cria somente documentação e não cria nem executa testes
automatizados. A regra de `AGENTS.md` continua vigente para implementação.

### Prova documental desta execução

- confirmar base, branch e ticker;
- conferir existência, links, números e checklist dos artefatos;
- executar `git diff --check`;
- não executar build completo por alteração exclusivamente documental.

### Prova técnica futura

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- inspeção de Room v20 e ausência de migration/schema/tabela analítica;
- revisão de query dedicada, `LEFT JOIN`, projeção mínima e duas leituras batch;
- revisão de que o ViewModel não acessa DAO e Compose não calcula métricas;
- comparação de saída 034 antes/depois da extração temporal;
- revisão de que nenhum arquivo Health Connect, parser, BLE ou calibração foi
  alterado.

### Prova física futura

Usar banco real existente e instalação já usada nas Specs 034/035/036, sem
criar corrida, sample ou fixture:

1. confirmar Room `user_version = 20`;
2. confirmar população somente `RUNNING` confirmado, origem correta e descarte
   excluído;
3. registrar composição de métodos/versões, distância canônica, duração
   coberta, cobertura e mistura de provenance sem dados pessoais;
4. fechar pace independentemente para cada ponto comparável;
5. confirmar provenance do snapshot ativo e labels da UI;
6. fechar média de FC e coverage independentemente usando sample-and-hold;
7. distinguir corrida sem FC, FC parcial e FC completa;
8. distinguir corrida sem pace elegível e não derrubar pontos válidos;
9. verificar múltiplas corridas no mesmo dia, ordem cronológica e empate de
   instante;
10. comparar assinaturas/contagens de activities, snapshots, derivados,
    splits, calibrações e fila Health Connect antes/depois;
11. reabrir a tela e confirmar ausência de analytics persistido;
12. capturar screenshot sanitizada.

O fechamento físico exige validação no artefato Release e instalação incremental
sem wipe, desinstalação, reimportação ou rederivação, reproduzindo o risco
histórico corrigido pela `035-08`. Debug pode complementar a evidência, mas não
substitui o smoke Release da tela e a comparação sanitizada de fatos.

Se cenário não existir naturalmente, registrar `N/A` com motivo. Não fabricar
dados para preencher lacunas.

## Ordem das subtarefas

1. `037-01-auditar-comparabilidade-e-congelar-contrato.md` — auditoria final,
   contratos e decisões; nenhuma alteração funcional.
2. `037-02-implementar-metricas-de-performance-de-corrida.md` — domínio puro,
   pace e primitiva de FC compartilhada.
3. `037-03-implementar-read-model-bounded-de-corridas.md` — projection/query,
   snapshot ativo e batch de samples.
4. `037-04-integrar-tendencia-de-corrida-no-viewmodel-e-navegacao.md` — estado,
   ViewModel, CTA e destino contextual.
5. `037-05-renderizar-historico-de-ritmo-e-fc.md` — tela, cards, gráficos,
   provenance, coverage e acessibilidade.
6. `037-06-validar-fisicamente-e-fechar.md` — banco real, cálculos,
   preservação, screenshot e fechamento documental.

## Premissas explícitas

- `confirmedType` já representa confirmação suficiente para o primeiro recorte;
  ausência de confirmação não será inferida pelo tipo sugerido.
- `watch-d3-d4-v4` é a versão nativa suportada para a série comparável; versões
  divergentes continuam fatos históricos, mas não entram silenciosamente na
  série principal.
- Indicadores de plausibilidade são suficientes para bloquear localmente; a
  ausência de aceite explícito bloqueia projeção remota, não este read model
  efêmero local.
- A média de FC requer samples válidos na janela; sem samples, a corrida
  continua existente e o pace continua independente.
- Não há banco físico seguro disponível no working tree nesta auditoria; a
  composição física final será confirmada ou marcada `N/A` na 037-06.
- Qualquer necessidade de índice temporal, mudança de schema, alteração de
  Health Connect ou nova política de calibração exige spec/decisão separada.
