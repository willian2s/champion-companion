# Task 011-04 - Adicionar campos de revisão e migration Room

## Status

Concluída em 2026-07-24.

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

## Execução

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/4.json`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-04-add-activity-review-fields-and-room-migration.md`

### Resultado no domínio e na entity

- `WorkoutActivity` passou a ter `title: String?` e `notes: String?`.
- `WorkoutActivityEntity` passou a ter `title` e `notes` nullable.
- `createFromImport(...)` continua criando atividades novas sem valores artificiais para esses campos.
- `reimport(...)` agora preserva `title` e `notes` já existentes na atividade.

### Resultado da migration

- `ChampionDatabase` subiu para `v4`.
- `AutoMigration(from = 3, to = 4)` foi suficiente para adicionar as colunas opcionais sem mudança estrutural adicional.
- Nenhuma outra tabela precisou ser alterada.

### Resultado da reimportação

- `WorkoutActivity.toEntity()` passou a mapear `title` e `notes`.
- `planWatchWorkoutPersistence(...)` agora preserva `title` e `notes` na reimportação, junto de `confirmedType`, `workflowState`, `reviewedAt`, `calibrationId` e métricas calibradas.

### Resultado do schema exportado

- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/4.json` foi gerado.
- `workout_activities` agora contém `title TEXT` e `notes TEXT` como colunas opcionais.

### Gate executado

- `./gradlew lint assembleDebug assembleRelease`
- resultado: sucesso.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
- Confirmado: nenhuma query nova, review, calibração ou UI foi implementada nesta task.
