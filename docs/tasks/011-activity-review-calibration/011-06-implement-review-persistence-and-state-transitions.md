# Task 011-06 - Implementar persistência da revisão e transições de estado

## Status

Planejada em 2026-07-24.

## Objetivo

Implementar submit atômico de revisão, descarte lógico e preservação das novas decisões na reimportação.

## Contexto

Hoje existe apenas `review(activityId, type, reviewedAt)` e `discard(activityId)` minimalistas. Falta persistir `title`, `notes`, tipo final completo, bloqueios de `PARTIAL` e reuso do novo contrato de repository.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-03-define-review-calibration-contracts-and-ui-state.md`
- `docs/tasks/011-activity-review-calibration/011-04-add-activity-review-fields-and-room-migration.md`

## Dependências

- `011-03`
- `011-04`

## Entradas

- migration v4 e mappers novos;
- contrato final de submit review.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-06-implement-review-persistence-and-state-transitions.md`

## Arquivos proibidos

- `feature/exercises/**`
- `feature/home/**`
- `core/navigation/**`
- `app/src/test/**`
- `app/src/androidTest/**`

## Escopo

- implementar `submitReview(input)` equivalente;
- validar `PENDING_REVIEW` e `parserValidity == VALID`;
- persistir `confirmedType`, `title`, `notes` e `reviewedAt`;
- fazer transições para `READY_TO_SYNC` ou `PENDING_CALIBRATION`;
- manter descarte lógico;
- preservar novas decisões na reimportação.

## Fora do escopo

- implementar calibração;
- implementar ViewModel ou UI;
- implementar Home.

## Passos

1. Evoluir domínio puro de confirmação da revisão.
2. Implementar `submitReview(input)` no repository.
3. Ajustar descarte para nova superfície da fase.
4. Garantir preservação de `title` e `notes` na reimportação.
5. Atualizar spec e overview.

## Critérios de aceite

- review é atômico;
- `PARTIAL` não confirma;
- não esteira vai para `READY_TO_SYNC`;
- esteira vai para `PENDING_CALIBRATION`;
- descarte continua lógico e permitido nos estados planejados;
- reimportação não apaga `title`, `notes` nem decisões existentes.

## Validação manual

- revisar transições e guardas de estado;
- conferir preservação de dados no plano de reimportação.

## Comandos de build aplicáveis

- `./gradlew lint assembleDebug assembleRelease`

## Evidências

- diff do domínio e do repository;
- diff do plano de reimportação.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `011-06` como concluída;
- registrar qualquer risco residual de concorrência ou estado inesperado.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
