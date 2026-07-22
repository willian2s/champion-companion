# Task 007-02 — Definir Contratos De Sync Diário E Estado Da Home

## Status

Concluida em 2026-07-22.

## Objetivo

Definir contratos mínimos de Home, refresh, coordinator, pacote fresco, data/timezone, repository e preferências antes da implementação.

## Contexto

Home precisa de estado único e testável. UI não deve manter estado paralelo de refresh.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/tasks/007-daily-summary-home/007-01-audit-home-daily-summary-and-fea1-flow.md`
- ADR-007

## Dependências

- `007-01`

## Entradas

- `HomeFeatureContract.kt`
- `RepositoryContracts.kt`
- `DomainTime.kt`
- estado real dos repositories e BLE.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DomainTime.kt`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/tasks/007-daily-summary-home/007-02-define-daily-sync-contracts-and-ui-state.md`

## Arquivos Proibidos

- `HomeScreen.kt`
- `RoomRepositories.kt`
- `ChampionDaos.kt`
- `ChampionDatabase.kt`
- `DeviceManagementViewModel.kt`
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Criar `HomeUiState` real.
- Criar `HomeIntent.Refresh`, retry/dismiss se necessário.
- Modelar `HomeRefreshStage`: `Preparing`, `Connecting`, `WaitingForSteps`, `Persisting`.
- Modelar erro seguro da Home.
- Definir contratos repository a implementar depois.
- Definir helper testável `Instant + ZoneId -> LocalDate`.

## Fora Do Escopo

- Implementar coordinator.
- Implementar persistência.
- Implementar UI pull-to-refresh.
- Rodar BLE.

## Passos

1. Definir estado composto da Home sem booleanos contraditórios.
2. Definir estado de refresh equivalente a `isRefreshing`, `refreshStage`, `refreshError`, `lastSuccessfulRefreshAt`.
3. Definir contrato de pacote fresco com `sessionId`, `refreshAttemptId`, `startedAt`.
4. Definir operações previstas de repository.
5. Atualizar docs/checklist.

## Critérios De Aceite

- Contratos compiláveis e mínimos.
- State machine coerente com spec.
- Nenhum Composable ganha lógica de sync.
- Nenhum DAO/Room alterado nesta task.

## Validação Mínima

```bash
./gradlew assembleDebug
```

## Evidência Manual

- Resumo do contrato criado e decisões de nomes.

## Resumo Do Contrato Criado

- `HomeFeatureContract.kt`
  - `HomeUiState` virou estado composto real com `surfaceState`, `observedDate`, `summary`, `watch`, `pendingActivities` e `refresh`.
  - `HomeIntent` ganhou `Load`, `Refresh`, `Retry`, `DismissError` e `OpenDevice`.
  - `HomeRefreshState` concentra equivalentes de `isRefreshing`, `refreshStage`, `refreshError` e `lastSuccessfulRefreshAt`.
  - `HomeRefreshStage` ficou em `Preparing`, `Connecting`, `WaitingForSteps`, `Persisting`.
  - `HomeRefreshAttempt` modela `refreshAttemptId`, `sessionId` e `startedAt`.
  - `HomeRefreshError` e `HomeRefreshErrorCategory` definem erro seguro da Home.

- `RepositoryContracts.kt`
  - `DailySummaryRepository` ganhou `observeSummaryForDate()` e `mergeWatchDailySummary(input)`.
  - `DeviceRepository` ganhou `updateImportMetadata(deviceId, importedAt, parserVersion)`.
  - `PersonalMetricsRepository` foi definido como contrato mínimo para altura/peso/passada manual.
  - `DailyStepsSyncCoordinator` foi definido com `refresh(request)`.
  - `DailySyncAttempt`, `FreshPacketCandidate` e `DailyStepsRefreshRequest` modelam contrato mínimo de pacote fresco e tentativa atual.
  - `AppPreferencesRepository` ganhou contrato provisório de `observeDailyStepsGoal()` e `updateDailyStepsGoal()` sem ainda alterar `AppPreferences` nesta task.

- `DomainTime.kt`
  - `LocalDateResolver.resolve(instant, zoneId)` e `Instant.toLocalDateIn(zoneId)` definem helper testável para `Instant + ZoneId -> LocalDate`.

## Decisões De Nome

- Usei `surfaceState` para separar estado principal da tela de `refresh`.
- Usei `HomeRefreshOperation` para evitar booleano paralelo e derivar `isRefreshing` de presença da operação.
- Usei `DailySyncAttempt` no domínio/repository e `HomeRefreshAttempt` na camada de Home para manter contratos mínimos independentes.

## Atualização Do Overview

- Marcar `007-02` como concluída.
- Ajustar arquivos de risco se contrato final mudar.

## Validação Final

- Confirmar que não existe UI de sync permanente introduzida.
