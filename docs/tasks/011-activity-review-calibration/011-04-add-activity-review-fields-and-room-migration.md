# Task 011-04 - Adicionar campos de revisão e migration Room

## Status

Planejada em 2026-07-24.

## Objetivo

Adicionar `title` e `notes` à atividade importada com migration segura v3 -> v4, atualização de entity, domínio, mappers, plano de reimportação e schema exportado.

## Contexto

`WorkoutActivityEntity` ainda não possui `title` nem `notes`. A fase precisa persistir essas decisões locais sem criar migration desnecessária além do que foi demonstrado.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-03-define-review-calibration-contracts-and-ui-state.md`

## Dependências

- `011-03`.

## Entradas

- contratos estáveis de review/calibração;
- schema Room v3 atual;
- regra de reimportação preservando decisões.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/4.json`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-04-add-activity-review-fields-and-room-migration.md`

## Arquivos proibidos

- `feature/exercises/**`
- `feature/home/**`
- `core/navigation/**`
- `app/src/test/**`
- `app/src/androidTest/**`

## Escopo

- adicionar `title` e `notes` ao domínio e à entity;
- subir banco para `v4`;
- criar migration segura;
- atualizar mappers;
- atualizar plano de reimportação para preservar `title` e `notes`;
- exportar schema novo.

## Fora do escopo

- implementar queries novas;
- implementar review/calibração;
- implementar UI.

## Passos

1. Evoluir `WorkoutActivity` e `WorkoutActivityEntity`.
2. Atualizar `ChampionDatabase.kt` para v4 e migration correspondente.
3. Atualizar mappers e plano de persistência/reimportação.
4. Exportar schema Room novo.
5. Atualizar spec e overview.

## Critérios de aceite

- banco sobe para v4 apenas por `title` e `notes`;
- migration não cria valores artificiais;
- migration não altera workflow, tipo, calibração ou métricas existentes;
- reimportação passa a preservar `title` e `notes`;
- schema exportado atualizado.

## Validação manual

- conferir schema v4 exportado;
- revisar diff de migration e mappers.

## Comandos de build aplicáveis

- `./gradlew lint assembleDebug assembleRelease`

## Evidências

- diff da migration;
- diff do schema exportado;
- diff do plano de reimportação preservando `title` e `notes`.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `011-04` como concluída;
- liberar `011-05`, `011-06` e `011-07`.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
