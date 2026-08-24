# Evidência 027-15 — Remoção do Gate B

- **Data:** 2026-08-24
- **Escopo:** remoção sanitizada da timeline local 1 Hz, sem alteração do Gate A

## Arquivos e símbolos removidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/Workout1HzTimeline.kt`;
- `Workout1HzTimeline`, `Workout1HzTimelineUiState` e estado associado;
- `deriveWorkout1HzTimeline`, `deriveBpm1Hz` e `deriveCadence1Hz`;
- job, geração/invalidação e wiring da timeline em
  `ExercisesViewModel.kt`;
- estado, seção, gráfico, legenda e desenho da timeline em
  `ExercisesFeatureContract.kt` e `ExercisesScreen.kt`.

## Auditoria sanitizada

Busca na árvore ativa `app/src/main` não encontrou uso de
`Workout1HzTimeline`, `deriveBpm1Hz` ou `deriveCadence1Hz`. Referências
remanescentes em specs, ADRs, tasks, evidências e screenshot são históricas e
não representam uso ativo. O detalhe de atividade continua com gráfico BPM raw
e métricas históricas.

Superfícies Gate A preservadas:

- BPM raw e projeção `HeartRateRecord`;
- cadência derivada de intervalos D4 e projeção `StepsCadenceRecord`;
- identidades, payloads congelados, retry, `DailySummary`, crédito e
  independência da cadência;
- sync explícito e Health Connect write-only.

Não houve nova sincronização física, leitura/readback do Health Connect,
Health Sync ou Strava durante esta remoção. `SYNCED` nas evidências anteriores
continua significando aceite local da escrita, não confirmação por readback.
