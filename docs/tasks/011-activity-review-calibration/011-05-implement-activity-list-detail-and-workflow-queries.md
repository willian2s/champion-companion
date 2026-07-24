# Task 011-05 - Implementar queries de lista, detalhe e resumo de workflow

## Status

Concluída em 2026-07-24.

## Objetivo

Adicionar queries e projeções necessárias para lista filtrada, detalhe por atividade, resumo por workflow e observação da calibração ativa, sem expor DAOs à feature.

## Contexto

O código atual só observa `PENDING_REVIEW` e reconstrói domínio por caminho específico de pendências. A Fase 11 precisa lista ampla, detalhe completo e resumo de Home sem filtrar tudo apenas na UI.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-03-define-review-calibration-contracts-and-ui-state.md`
- `docs/tasks/011-activity-review-calibration/011-04-add-activity-review-fields-and-room-migration.md`

## Dependências

- `011-04`.

Observação de execução real:

- `011-05` foi executada antes de `011-03`.
- por isso, `011-03` continua pendente para congelar documentalmente contratos, filtros e estado de UI ainda não formalizados na feature.

## Entradas

- contratos finais dos repositórios;
- schema v4 já estabilizado.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-05-implement-activity-list-detail-and-workflow-queries.md`

## Arquivos proibidos

- `feature/exercises/**`
- `feature/home/**`
- `core/navigation/**`
- `ChampionDatabase.kt`
- `app/src/test/**`
- `app/src/androidTest/**`

## Escopo

- adicionar query de lista por filtro;
- adicionar query de detalhe por ID;
- adicionar resumo por workflow;
- expor calibração ativa/mais recente via repositório;
- generalizar mapper de atividade além de `toPendingReviewDomain(...)`.

## Fora do escopo

- implementar review;
- implementar calibração;
- implementar ViewModel ou UI.

## Passos

1. Adicionar queries novas em `ChampionDaos.kt`.
2. Criar projections necessárias para lista, resumo e detalhe.
3. Generalizar mappers para qualquer workflow observável.
4. Expor `observeActivities`, `observeActivity`, `observeWorkflowSummary` e `observeLatestCalibration` nos repositórios.

## Critérios de aceite

- lista, detalhe e resumo por workflow observáveis via repository;
- filtros suportados por query clara, sem depender de filtrar tudo em Compose;
- calibração ativa observável;
- feature continua sem acesso direto a DAO.

## Validação manual

- revisar SQL, ordenação e mapeamentos;
- confirmar que `SYNCED` e `SYNC_FAILED` são observáveis sem serem produzidos nesta fase.

## Comandos de build aplicáveis

- `./gradlew lint assembleDebug assembleRelease`

## Evidências

- diff dos DAOs;
- diff dos mappers e repositories;
- lista dos filtros e queries correspondentes.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `011-05` como concluída;
- registrar qualquer novo hotspot em `ChampionDaos.kt`, `DatabaseMappers.kt` ou `RoomRepositories.kt`.

## Execução

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-05-implement-activity-list-detail-and-workflow-queries.md`

### Resultado nos contracts

- `WorkoutActivityRepository` passou a expor:
  - `observeActivities(filter)`
  - `observeActivity(activityId)`
  - `observeWorkflowSummary()`
- `observePendingReview()` foi mantido por compatibilidade, delegando ao filtro correspondente.
- `WorkoutActivityFilter` e `WorkoutWorkflowSummary` foram adicionados ao contrato.
- `TreadmillCalibrationRepository` passou a expor `observeLatestCalibration(activityId)`.
- `TreadmillCalibrationSnapshot` foi adicionado como snapshot de contrato para observação da calibração ativa, sem expor DAO à feature.

### Resultado nos DAOs

- `WorkoutActivityDao` passou a expor:
  - `observeAll()`
  - `observeByWorkflowStates(...)`
  - `observeById(...)`
  - `observeWorkflowSummary()`
- `TreadmillCalibrationDao` passou a expor `observeLatestForActivity(...)`.
- `WorkoutWorkflowSummaryRecord` foi adicionado como projection agregada de Room.

### Resultado nos mappers e repositories

- `DatabaseMappers.kt` passou a generalizar reconstrução de `WorkoutActivity` além do caminho específico de `PENDING_REVIEW`.
- `RoomRepositories.kt` passou a implementar:
  - `observeActivities(filter)`
  - `observeActivity(activityId)`
  - `observeWorkflowSummary()`
  - `observeLatestCalibration(activityId)`
- Filtros agora são suportados por query clara no Room, sem depender de filtrar tudo na UI.

### Gate executado

- `./gradlew lint assembleDebug assembleRelease`
- resultado: sucesso.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
- Confirmado: nenhuma UI, review ou calibração funcional foi implementada nesta task.
