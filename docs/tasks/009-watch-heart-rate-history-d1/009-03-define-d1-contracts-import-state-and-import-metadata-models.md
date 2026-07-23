# Task 009-03 - Definir contratos D1, estado de importacao e modelos de metadata

## Status

Concluida em 2026-07-23.

## Objetivo

Definir contratos de dominio e UI para importacao D1 sem ainda implementar parser, BLE real ou persistencia final.

## Contexto

Estado atual de `HealthFeatureContract.kt` modela apenas medicao manual. `HeartRateRepository` tambem nao expoe historico por periodo, agregacoes ou metadata D1. Antes de tocar parser, collector, coordinator e UI final, Fase 09 precisa fixar contratos pequenos e estaveis para evitar retrabalho e conflito entre tasks paralelas.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-01-audit-d1-protocol-history-and-health-connect-boundary.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-02-formalize-d1-local-first-adr-and-roadmap.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/decisions/adr-009-health-metrics-hub-navigation.md`

## Dependencias

- `009-01`
- `009-02`

## Entradas

- state machine atual da medicao manual;
- contratos atuais de repository e gate;
- requisitos obrigatorios de periodo, resultado parcial, metadata e progresso D1.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-03-define-d1-contracts-import-state-and-import-metadata-models.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModelD1ContractsTest.kt`

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- `core/bluetooth/**`
- `feature/health/HealthMetricDetailScreens.kt`
- `feature/home/**`

## Escopo

- definir resultado tipado `Completed`, `CompletedWithGaps`, `Partial`, `Failed`, `Cancelled`;
- definir estado independente de importacao D1 ao lado da medicao manual;
- definir periodo selecionado, modelos de historico, agregacoes e serie do grafico;
- definir contrato de metadata D1 observavel;
- adicionar owner `WATCH_D1_HISTORY` ao gate foreground;
- reservar contrato de persistencia D1 com contagens explicitas.

## Fora Do Escopo

- implementar parser D1;
- implementar Room;
- implementar coordinator BLE;
- implementar UI final.

## Passos

1. Expandir `HealthFeatureContract.kt` com estado independente para D1, periodo, agregacoes e ultima importacao.
2. Expandir `RepositoryContracts.kt` com contratos D1 de importacao tipada, consultas por periodo e metadata observavel.
3. Ajustar `HeartRateAndSync.kt` com modelos duradouros necessarios para conflito, metadata ou resumo de importacao sem inventar semantica de payload.
4. Garantir que medicao manual continue isolada da state machine D1.
5. Atualizar spec e overview com contratos finais aprovados.

## Criterios De Aceite

- contratos D1 aprovados e compilaveis;
- medicao manual e importacao D1 ficam em estados independentes;
- periodo, agregacoes, historico e metadata ficam modelados sem depender de Compose;
- owner `WATCH_D1_HISTORY` reservado no gate;
- nenhum acesso BLE, DAO ou Room novo implementado nesta task.

## Testes Pequenos Aplicaveis

- validacao de modelos e transicoes puras de state machine;
- cobertura de contratos de periodo e mapeamento de estado inicial.

## Validacao Minima

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest` quando houver teste pequeno novo relacionado a contratos.

## Evidencias

- diff focado em contratos;
- lista de modelos novos e como cada um sera consumido por tasks posteriores.

## Modelos Definidos

- em `HealthFeatureContract.kt`:
  - `D1HeartRateImportSurfaceState`
  - `D1HeartRateImportSessionState`
  - `HealthD1ImportError`
  - `HealthD1ImportErrorCategory`
  - novos campos em `HealthUiState`: `d1ImportState`, `selectedHistoryPeriod`, `history`, `latestD1ImportMetadata`
- em `RepositoryContracts.kt`:
  - `ForegroundBleOperationOwner.WATCH_D1_HISTORY`
  - `SaveD1HeartRateMeasurementsInput`
  - `HeartRateRepository.importD1Measurements(...)`
  - `HeartRateRepository.observeHistory(...)`
  - `HeartRateRepository.observeD1ImportMetadata(...)`
- em `HeartRateAndSync.kt`:
  - `HeartRateHistoryPeriod`
  - `HeartRateHistoryQuery`
  - `HeartRateHistoryEntry`
  - `HeartRateHistoryAggregates`
  - `HeartRateChartPoint`
  - `HeartRateChartSeries`
  - `HeartRateChartResolution`
  - `HeartRateHistorySnapshot`
  - `D1HeartRatePacketProgress`
  - `D1HeartRateImportSummary`
  - `D1HeartRateImportStatus`
  - `D1HeartRateImportResult`
  - `D1HeartRateImportMetadata`
  - `D1HeartRateImportPersistenceResult`

## Consumo Planejado Por Tasks Posteriores

- `009-04` consumira `D1HeartRatePacketProgress`, `D1HeartRateImportResult` e `HealthD1ImportErrorCategory` para parser e collector multipart.
- `009-05` consumira `SaveD1HeartRateMeasurementsInput`, `D1HeartRateImportMetadata` e `D1HeartRateImportPersistenceResult` para Room, deduplicacao e fila `PENDING`.
- `009-06` consumira `HeartRateHistoryQuery`, `HeartRateHistorySnapshot`, `HeartRateHistoryAggregates` e `HeartRateChartSeries` para consultas locais, agregacoes e politica de grafico.
- `009-07` consumira `ForegroundBleOperationOwner.WATCH_D1_HISTORY` e `D1HeartRateImportSurfaceState` como base do coordinator.
- `009-08` consumira novos campos de `HealthUiState` para integrar progresso, periodo, metadata e retry sem misturar com medicao manual.

## Atualizacao Do Overview

- marcar `009-03` como concluida;
- registrar contratos finais aprovados e paralelismo liberado entre `009-04` e `009-05`.

## Validacao Final

- confirmar que parser, Room e UI final nao foram implementados aqui.

Confirmado nesta task:

- parser D1 nao foi implementado;
- Room/persistencia final nao foram implementados;
- UI final de grafico/historico nao foi implementada;
- mudanca ficou restrita a contratos, modelos, um teste pequeno e documentacao associada.
