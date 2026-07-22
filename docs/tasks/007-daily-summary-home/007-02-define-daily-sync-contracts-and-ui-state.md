# Task 007-02 — Definir Contratos De Sync Diário E Estado Da Home

## Status

Pendente.

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

## Atualização Do Overview

- Marcar `007-02` como concluída.
- Ajustar arquivos de risco se contrato final mudar.

## Validação Final

- Confirmar que não existe UI de sync permanente introduzida.
