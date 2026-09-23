# 036-02 — Implementar agregação histórica de zonas

- **Ticker:** `036`
- **Número:** `02`
- **Status:** `completed`

## Objetivo e resultado esperado

Estender `CardiovascularLoadHistoryPolicy` para agregar as seis zonas da análise
034 em current7, current28 e buckets de current28, preservando integralmente a
carga, estados, comparações e semântica temporal da 035.

## Requisitos cobertos

- R1, R3, R4, R5, R6 e R7 da Spec 036.
- Fechamento de duração, percentuais, cobertura e parcialidade.
- Carga zero e análise individual indisponível parcial.

## Escopo incluído

- Extensão aditiva de `CardiovascularLoadPeriodSummary` e
  `CardiovascularLoadDailyBucket` com coleção de `HeartRateZoneSummary` ou
  contrato equivalente.
- Uma execução de `WorkoutHeartRateAnalysisPolicy` por workout elegível.
- Soma de duração por zona somente de resultados `AVAILABLE`.
- Percentual por zona com denominador da duração analisada, nunca duração total.
- Seis entradas sempre presentes quando houver análise disponível, inclusive
  `BELOW_ZONE_1` e zeros.
- Buckets diários fechando current28 por zona.
- Estados e `partial` conforme 035/036, sem threshold de cobertura.

## Escopo excluído

- Repository, DAO, `CardiovascularLoadWorkoutInput` ou fonte bounded.
- Compose, gráfico, seleção de período ou navegação.
- Persistência, migration, Room, Health Connect e testes automatizados novos.
- Comparação de percentual de zonas com período anterior.

## Dependências

- `036-01` concluída.
- `WorkoutHeartRateAnalysisPolicy` e `HeartRateZoneSummary` da 034.
- Contratos existentes de `CardiovascularLoadHistoryPolicy` da 035.

## Arquivos e símbolos prováveis

- `core/model/WorkoutHeartRateLoadHistory.kt`:
  `CardiovascularLoadPeriodSummary`, `CardiovascularLoadDailyBucket`,
  `CardiovascularLoadHistoryPolicy`, `AnalyzedWorkout`, `HistoryWindows`.
- `core/model/WatchWorkoutHeartRateAnalysis.kt` somente para consumo de tipos;
  não duplicar lógica.

## Passos de implementação

1. Preservar `HistoryWindows.from(asOf)` como única origem dos períodos.
2. Reusar o resultado já calculado por workout em todas as projeções.
3. Criar helper puro de agregação por zona a partir de análises `AVAILABLE`.
4. Produzir todas as seis zonas com `Duration.ZERO` quando necessário.
5. Calcular percentuais sobre soma de todas as zonas disponíveis.
6. Manter `coverageRatio` com duração coberta sobre duração total do workout.
7. Manter `partial` quando houver resultado não `AVAILABLE` ou cobertura parcial.
8. Tratar carga `0.0` como `AVAILABLE` válido.
9. Atribuir bucket por `startedAt.atZone(zoneId).toLocalDate()` sem dividir sessão.
10. Validar fechamentos com cálculo independente antes de entregar ao read model.

## Testes e comandos de validação

- Inspecionar ranges semiabertos e soma por zona.
- Calcular independentemente percentuais, cobertura e buckets.
- Conferir que baseline/comparações e totalLoad da 035 não mudaram.
- `./gradlew lint assembleDebug assembleRelease` somente na integração futura,
  não criar testes automatizados novos.

## Definição de pronto

- current7/current28 possuem distribuição tipada quando há `AVAILABLE`.
- Todas as zonas, inclusive `BELOW_ZONE_1`, aparecem.
- Percentuais fecham sobre duração analisada.
- Buckets fecham current28 por zona.
- Estados, parcialidade, carga e janelas 035 permanecem iguais.
- Nenhum acesso a infraestrutura ou escrita foi introduzido.

## Riscos e cuidados

- Não somar percentuais individuais; somar durações e dividir uma vez.
- Não usar `totalWorkoutDuration` como denominador de zona.
- Não excluir análise com carga zero.
- Não converter `NoHeartRateSamples`/`Unavailable` em zona zero.
- Não alterar a semântica de `summarize` da 035 incidentalmente.

## Execução e evidências

### Implementação realizada

- `CardiovascularLoadPeriodSummary` e `CardiovascularLoadDailyBucket` receberam
  `zones: List<HeartRateZoneSummary>?` de forma aditiva.
- `CardiovascularLoadHistoryPolicy` continua sendo a única policy histórica e
  continua usando `HistoryWindows.from(asOf)` para todas as janelas.
- O helper puro `aggregateZones(records)` seleciona somente resultados
  `WorkoutHeartRateAnalysisResult.Available`, soma `Duration` por zona e produz
  exatamente `HeartRateZone.entries` na ordem `BELOW_ZONE_1`, `Z1`, `Z2`, `Z3`,
  `Z4`, `Z5`.
- O denominador de cada percentual é a soma das seis durações agregadas do
  próprio período ou bucket. A conversão para `Double` ocorre somente depois da
  soma de `Duration`; `totalWorkoutDuration` não participa desse cálculo.
- `coverageRatio` continua sendo calculado como duração coberta sobre duração
  total do workout, sem mistura com percentual de zona. `totalLoad`, contadores,
  estados, `partial`, comparações, tipos e janelas não foram alterados.
- `zones == null` ocorre quando não há resultado `AVAILABLE`; não é usada lista
  vazia. `NoHeartRateSamples`, `Unavailable` e `ConfigurationRequired` não são
  convertidos em seis zonas zero. Com `AVAILABLE`, as seis entradas permanecem
  presentes mesmo quando alguma duração é `Duration.ZERO`.
- Carga cardiovascular `0.0` continua pertencendo a resultado `AVAILABLE` e
  participa da agregação; `BELOW_ZONE_1` pode concentrar toda a duração.
- Buckets chamam o mesmo `summarize` sobre os `AnalyzedWorkout` do dia e mantêm
  a atribuição integral por `startedAt.atZone(zoneId).toLocalDate()`. Assim,
  cada bucket usa seu próprio denominador e a soma por zona fecha `current28`.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WorkoutHeartRateLoadHistory.kt`
- `docs/tasks/036-heart-rate-zone-history/036-02-implementar-agregacao-historica-de-zonas.md`
- `docs/tasks/036-heart-rate-zone-history/036-00-overview.md`

Nenhum arquivo de repository, DAO, mapper, Room, Health Connect, UI ou
`WatchWorkoutHeartRateAnalysis.kt` foi alterado.

### Validações lógicas

1. Três resultados `AVAILABLE` produzem seis entradas, somam durações antes do
   cálculo e fecham percentuais aproximadamente em `1.0` quando há duração.
2. Dois `AVAILABLE` e `NoHeartRateSamples` mantêm `AVAILABLE`, contagem analisada
   igual a dois, população igual a três, `partial = true` e zonas dos dois
   resultados disponíveis.
3. Dois `AVAILABLE` e `Unavailable` mantêm `AVAILABLE`, `partial = true` e
   zonas presentes.
4. Sem `AVAILABLE`, com máxima configurada, o status permanece
   `NO_ANALYZABLE_WORKOUTS` e `zones == null`.
5. Workouts com máxima nula permanecem `CONFIGURATION_REQUIRED` e não criam
   distribuição.
6. População vazia permanece `NO_WORKOUTS` com `zones == null`.
7. Resultado `AVAILABLE` com carga `0.0` continua analisado e mantém zonas,
   inclusive `BELOW_ZONE_1`.
8. Cobertura parcial usa duração analisada para percentuais de zona e duração
   total somente para `coverageRatio`.
9. Zonas sem duração continuam presentes com `Duration.ZERO` e `0.0`.
10. A agregação diária é executada sobre os registros do próprio dia; por isso,
    a soma independente das seis zonas dos buckets fecha `current28` zona a zona.

### Comandos executados e resultados

- `git diff --check` — passou.
- `./gradlew app:compileDebugKotlin` — passou (`BUILD SUCCESSFUL`).
- Inspeção estática de construtores, chamadas da policy, ranges, `Duration`,
  status, `partial`, buckets e arquivos proibidos — sem divergências.
- Revisão independente do agente `review` — `No issues`; nenhuma correção
  objetiva pendente.
- Não foram criados nem executados testes automatizados novos, conforme escopo
  congelado da subtarefa.

### Decisões, desvios e riscos residuais

- Nenhum desvio ou decisão nova: implementação segue contrato congelado da
  `036-01` e não extraiu `HistoryWindows`.
- A policy 034 continua fonte única de classificação, sample-and-hold,
  cobertura, carga e zonas; há uma única chamada por workout elegível, e o
  `AnalyzedWorkout` é reutilizado em períodos, buckets, tipos e distribuição.
- Room permanece v20, sem migration, tabela, coluna, cache ou backfill. Health
  Connect permanece sem alterações.
- Validação física e cenários naturais de dados permanecem para subtarefas
  posteriores; nenhum workout ou sample foi fabricado. Não há bloqueio técnico
  identificado para `036-03`.
