# 036-02 — Implementar agregação histórica de zonas

- **Ticker:** `036`
- **Número:** `02`
- **Status inicial:** `pending`

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
