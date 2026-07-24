# Task 011-03 - Definir contratos de revisão, calibração e estado de UI

## Status

Planejada em 2026-07-24.

## Objetivo

Congelar contratos de domínio, filtros, state machine, inputs equivalentes de review/calibração e estado de UI da feature antes de tocar migration, queries ou telas.

## Contexto

Os contratos reais ainda cobrem apenas importação D3/D4 e observação de pendências `PENDING_REVIEW`. A fase precisa formalizar revisão, descarte, detalhe, resumo por workflow, calibração e formulários sem acoplar a feature aos DAOs.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-01-audit-review-domain-persistence-and-phase-boundaries.md`
- `docs/tasks/011-activity-review-calibration/011-02-formalize-ready-to-sync-adr-and-roadmap.md`

## Dependências

- `011-02`.

## Entradas

- estado real auditado dos repositórios e da feature;
- fronteira documental já alinhada.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/TreadmillCalibration.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-03-define-review-calibration-contracts-and-ui-state.md`

## Arquivos proibidos

- `core/database/**`
- `data/**`
- `feature/home/**`
- `core/navigation/**`
- `app/src/test/**`
- `app/src/androidTest/**`

## Escopo

- definir inputs equivalentes de review e calibração;
- definir filtros e resumo por workflow;
- definir estado de detalhe;
- definir estado dos formulários;
- definir bloqueios de `PARTIAL` e limites de edição;
- preparar contratos para migration e queries seguintes.

## Fora do escopo

- implementar persistência;
- implementar migration;
- implementar UI final;
- implementar Home ou navegação.

## Passos

1. Expandir `RepositoryContracts.kt` com contratos equivalentes da fase.
2. Evoluir domínio puro para suportar `title`, `notes` e fallback de calibração sem base positiva.
3. Expandir `ExercisesFeatureContract.kt` com filtros, detalhe e formulários.
4. Atualizar spec e overview com contratos finais.

## Critérios de aceite

- contratos compiláveis e pequenos;
- `SubmitWorkoutReviewInput` e `CalibrateImportedActivityInput` equivalentes definidos;
- estado de UI separado em `importState`, `selectedFilter`, `activityList`, `workflowSummary`, `selectedActivity`, `detailState`, `reviewFormState`, `calibrationFormState` e `operationError`;
- nenhuma query, migration ou UI final implementada.

## Validação manual

- revisão do diff de contratos;
- conferência da coerência entre domínio, feature contract e spec.

## Comandos de build aplicáveis

- `./gradlew assembleDebug`

## Evidências

- diff focado em contratos;
- lista dos modelos novos e tasks consumidoras.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `011-03` como concluída;
- liberar `011-04`, `011-05`, `011-06` e `011-07` com contratos estáveis.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
