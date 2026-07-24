# Task 011-01 - Auditar domínio, persistência e fronteiras da revisão

## Status

Concluída em 2026-07-24.

## Objetivo

Auditar estado real do domínio, Room, contracts, queries, reimportação, calibração, filtros, Home, navegação, MotionLog e fronteira entre Fase 11 e Fase 12 antes de qualquer implementação funcional.

## Contexto

O app já importa D3/D4 até `PENDING_REVIEW`, mas ainda não possui `title`, `notes`, filtros, detalhe, banner de Home nem workflow de revisão/calibração pronto. O roadmap também ainda mistura responsabilidades de Health Connect na Fase 11.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/_inputs/phase-11-activity-review-calibration-direction.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/protocol/workouts-d3-d4.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

## Dependências

- nenhuma.

## Entradas

- código real em `feature/exercises`, `feature/home`, `core/navigation`, `core/model`, `core/database` e `data`;
- schemas Room atuais;
- baseline documental MotionLog;
- fronteira desejada `READY_TO_SYNC`.

## Arquivos exatos permitidos

- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-01-audit-review-domain-persistence-and-phase-boundaries.md`

## Arquivos proibidos

- `app/src/main/java/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- `docs/roadmap/**`
- `docs/decisions/**`
- fases anteriores concluídas.

## Escopo

- auditar domínio atual;
- mapear campos ausentes;
- mapear contracts e queries atuais;
- mapear schema Room atual;
- confirmar plano atual de reimportação e preservação de revisão;
- auditar calibração atual;
- auditar filtros ausentes;
- auditar estado atual da Home e da navegação;
- registrar fronteira F11/F12;
- registrar hotspots e riscos.

## Fora do escopo

- implementar código funcional;
- criar ADR;
- alterar roadmap;
- alterar Kotlin, Compose, Room, Gradle ou Manifest.

## Passos

1. Auditar classes e DAOs reais envolvidos na atividade importada.
2. Confirmar campos presentes e ausentes no domínio e no schema Room.
3. Mapear repositórios, queries, reimportação e preservação de decisões.
4. Confirmar estado atual da Home, Exercícios e navegação.
5. Registrar explicitamente a fronteira entre revisão local e sincronização futura.
6. Atualizar spec e overview com achados factuais.

## Critérios de aceite

- auditoria cobre domínio, Room, contracts, queries, Home, navegação, filtros, reimportação, calibração e MotionLog;
- estado real do código fica documentado sem hipótese nova;
- hotspots e riscos de conflito ficam explícitos;
- nenhuma implementação funcional é introduzida.

## Validação manual

- conferência textual das referências citadas;
- revisão do diff documental apenas.

## Comandos de build aplicáveis

- nenhum nesta task documental.

## Evidências

- lista de arquivos auditados;
- lista de lacunas confirmadas;
- diff restrito à documentação da Fase 11.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `011-01` como concluída;
- registrar hotspots, dependências e riscos confirmados.

## Execução

### Arquivos auditados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/TreadmillCalibration.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/AppDestination.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/3.json`
- `docs/protocol/workouts-d3-d4.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

### Achados confirmados do domínio e dos contratos

- `WorkoutActivity` já possui estados `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCING`, `SYNCED`, `SYNC_FAILED` e `DISCARDED`.
- `WorkoutActivity.confirmReview(...)` já bloqueia avanço quando `workflowState != PENDING_REVIEW`, `parserValidity != VALID` ou `reviewedAt < importedAt`.
- `WorkoutActivity.confirmReview(...)` já envia tipo não esteira para `READY_TO_SYNC` e tipo `TREADMILL` para `PENDING_CALIBRATION`.
- `WorkoutActivity` ainda não possui `title` nem `notes`.
- `TreadmillCalibration.forImportedActivity(...)` hoje exige `originalMeters > 0`, `confirmedType == TREADMILL` e `reviewedAt != null`.
- `TreadmillCalibration` ainda não cobre, para atividade importada, fallback por `estimatedMeters > 0` nem caminho com fator ausente quando não existir distância-base positiva.
- `WorkoutActivityRepository` real ainda só expõe `persistImported(...)`, `observePendingReview()`, lookup por CRC, metadata D3/D4, `review(...)` e `discard(...)`.
- `TreadmillCalibrationRepository` real ainda só expõe `register(calibration)`.

### Achados confirmados de Room, queries e reimportação

- `ChampionDatabase` está na versão `3`, com `AutoMigration(1, 2)` e `AutoMigration(2, 3)`.
- `WorkoutActivityEntity` ainda não possui `title` nem `notes`.
- `WorkoutActivityDao` ainda só observa `PENDING_REVIEW` e não possui lista geral por workflow, detalhe observável por ID ou resumo por workflow.
- `WorkoutDetailDao` já entrega intervalos e amostras de BPM por atividade.
- `TreadmillCalibrationDao` já entrega `findLatestForActivity(...)`, mas isso ainda não sobe ao contract público.
- `RoomRepositories.persistImported(...)` já deduplica por `(deviceId, origin, crc)`.
- `planWatchWorkoutPersistence(...)` já preserva `confirmedType`, `workflowState`, `reviewedAt`, `calibrationId`, `calibratedDistanceMeters`, `calibratedAveragePaceSecondsPerKilometer` e `calibratedAverageSpeedMetersPerSecond`.
- O plano atual de reimportação ainda não pode preservar `title` e `notes`, porque os campos ainda não existem.
- `observePendingReview()` reconstrói domínio por mapper específico de pendência; não existe ainda mapper geral adequado para detalhe/lista ampla de múltiplos workflows.

### Achados confirmados de Exercícios, Home e navegação

- `ExercisesFeatureContract.kt` ainda separa apenas `importState`, metadata D3/D4, `pendingActivities`, goal diário e estado do relógio associado.
- `ExercisesViewModel.kt` ainda observa somente `observePendingReview()`, metadata D3/D4, dispositivo associado, goal diário e coordinator de importação.
- `ExercisesScreen.kt` ainda mostra apenas cabeçalho, card de importação e lista read-only de pendências.
- Não existem filtros, rota de detalhe, formulário de revisão, formulário de calibração nem ação de descarte na feature Exercícios.
- `AppDestination.kt` ainda só possui `Exercises` como destino top-level; não existe destino por `activityId`.
- `ChampionNavigation.kt` ainda trata Exercícios apenas como tela top-level com bloqueio de saída durante importação ativa.
- `HomeViewModel.kt` e `HomeScreen.kt` ainda usam `AppWorkoutSessionRepository.observeRecoverable()` para pendências da Home; não usam `WorkoutActivity` nem resumo de review/calibração.

### Filtros, MotionLog e fronteira de fase

- Baseline MotionLog consultada em `docs/baseline/database-reference.md` e `docs/baseline/feature-inventory.md` confirma lista pendente por revisão, revisão manual, calibração de esteira e sync manual posterior, mas também confirma diferenças importantes: descarte lá é exclusão persistida, não estado `DISCARDED`, e fator separado de calibração não foi confirmado.
- O código Kotlin auditado não possui referência funcional direta a MotionLog nesses paths; referência aparece apenas na documentação e em comentário de design system.
- Fronteira real observada no código continua coerente com a direção da fase: importação termina em `PENDING_REVIEW`; não existe caminho funcional de Health Connect para treinos D3/D4; `SYNCING`, `SYNCED` e `SYNC_FAILED` existem no domínio, mas não são produzidos pelo fluxo atual de Exercícios.

### Lacunas confirmadas para as próximas tasks

- faltam `title` e `notes` no domínio persistido e no schema Room;
- faltam contracts equivalentes de `submitReview(input)`, `observeActivities(filter)`, `observeActivity(activityId)`, `observeWorkflowSummary()`, `calibrateImportedActivity(input)` e `observeLatestCalibration(activityId)`;
- falta query de lista filtrada e resumo por workflow;
- falta query de detalhe observável por ID;
- falta fallback de calibração sem base positiva;
- falta banner da Home com `pendingReviewCount` e `pendingCalibrationCount`;
- falta navegação `Home -> Exercícios -> Detalhe` por `activityId`;
- falta separação de estado da feature em filtro, detalhe e formulários.

### Hotspots e riscos confirmados

- hotspots principais: `WatchWorkoutActivity.kt`, `TreadmillCalibration.kt`, `RepositoryContracts.kt`, `ChampionDatabase.kt`, `ChampionDaos.kt`, `DatabaseMappers.kt`, `RoomRepositories.kt`, `ExercisesFeatureContract.kt`, `ExercisesViewModel.kt`, `ExercisesScreen.kt`, `ChampionNavigation.kt`, `HomeViewModel.kt` e `HomeScreen.kt`.
- risco de conflito alto entre `011-05`, `011-06` e `011-07` por compartilharem `RepositoryContracts.kt`, `ChampionDaos.kt`, `DatabaseMappers.kt` e `RoomRepositories.kt`.
- risco funcional real: associação entre intervalos e BPM pode não ser segura em todos os casos; detalhe deve degradar para exibição separada quando necessário.
- risco documental real: roadmap atual ainda mistura Health Connect na Fase 11 e precisa ser corrigido em `011-02`.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
- Confirmado: mudanças ficaram restritas à documentação da Fase 11.
