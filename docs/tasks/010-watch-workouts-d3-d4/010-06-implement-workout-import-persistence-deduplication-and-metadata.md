# Task 010-06 - Implementar persistência, deduplicação e metadata D3/D4

## Status

Concluída em 2026-07-23.

## Objetivo

Adaptar persistência atual para importação D3/D4 por bloco, metadata dedicada e retomada por CRC, preferindo reuso do schema existente.

## Contexto

`persistImported(...)` já cobre reimportação e preservação de revisão/calibração, mas o app ainda não expõe metadata `WATCH_WORKOUT_D3_D4`, consulta local por CRC para retomada nem política diagnóstica persistida para D3/D4.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-02-define-watch-workout-import-contracts-state-and-diagnostic-policy.md`
- `docs/specs/003-domain-and-persistence-foundation.md`
- `docs/baseline/database-reference.md`

## Dependências

- `010-02`.

## Entradas

- schema Room atual;
- reimportação e deduplicação existentes;
- metadata genérica `device_import_metadata` já presente.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DiagnosticPayload.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/**`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-06-implement-workout-import-persistence-deduplication-and-metadata.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/data/RoomRepositoriesWatchWorkoutImportTest.kt`

## Arquivos proibidos

- `feature/exercises/**`
- `core/bluetooth/**`
- manifesto, Gradle e DataStore.

## Escopo

- persistência transacional por bloco;
- deduplicação por `(deviceId, origin, crc)`;
- reimportação preservando revisão e calibração;
- metadata `WATCH_WORKOUT_D3_D4`;
- consulta local por CRC para retomada;
- payload diagnóstico privado e derived outputs;
- migration somente se necessária e justificada.

## Fora do escopo

- coordinator BLE;
- UI;
- Health Connect.

## Passos

1. Reusar schema atual sempre que possível.
2. Expor query local por chave de importação para skip/reimportação.
3. Adicionar metadata `WATCH_WORKOUT_D3_D4` sem reutilizar metadata D1.
4. Ajustar persistência de `PARTIAL`, `INVALID` e diagnóstico privado conforme contrato.
5. Se migration for inevitável, justificar, versionar e exportar schema novo.

## Critérios de aceite

- deduplicação por CRC funcional;
- reimportação preserva revisão, workflow e calibração;
- metadata D3/D4 observável existe;
- retomada por CRC consegue distinguir `VALID`, parcial, ausente e falho;
- nenhuma migration é criada sem necessidade demonstrada.

## Testes pequenos aplicáveis

- deduplicação por CRC;
- reimportação preserva revisão;
- `PARTIAL` seguro persiste;
- `INVALID` não cria atividade;
- metadata D3/D4 atualiza tentativa e sucesso;
- skip local de `VALID`.

## Validação mínima

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest --tests "*WatchWorkoutImport*"`

## Evidências

- testes pequenos de Room/repositório;
- decisão explícita sobre migration ou ausência dela;
- schema exportado, se houver mudança de banco.

## Execução

### Decisão de schema

- nenhuma migration foi criada;
- `ChampionDatabase` permaneceu em `version = 3`;
- schema exportado permaneceu inalterado;
- justificativa: `watch_workout_imports`, `device_import_metadata` e `diagnostic_derived_outputs` já cobriam deduplicação, metadata e vínculo diagnóstico exigidos pela fase.

### Contratos e persistência adicionados

- `WorkoutActivityRepository.findWatchWorkoutImport(deviceId, crc)`
- `WatchWorkoutImportLookup`
- metadata D3/D4 observável por `WATCH_WORKOUT_D3_D4`
- upsert de metadata D3/D4 separado da metadata D1
- persistência de derived outputs diagnósticos junto do `DiagnosticPayload`
- guarda defensiva: `persistImported(...)` rejeita parser `INVALID`

### Reuso e retomada por CRC

- lookup local por `(deviceId, origin = WATCH_D3_D4, crc)` agora retorna parser validity, workflow e timestamps suficientes para:
  - pular `VALID` em retomada;
  - reprocessar `PARTIAL`;
  - distinguir ausência local de falha/pendência.

### Testes adicionados

- deduplicação por CRC via planner puro de persistência;
- reimportação preserva revisão, workflow e calibração;
- `INVALID` não gera plano de persistência;
- metadata D3/D4 usa import type dedicado e preserva status;
- lookup local diferencia `VALID` de `PARTIAL` para retomada;
- derived outputs diagnósticos viram entidades persistíveis.

## Atualização do overview

- marcar `010-06` como concluída;
- registrar se houve migration, conflito de schema ou novo hotspot.

Registrado: não houve migration; hotspots principais ficaram em `RoomRepositories.kt` e `DatabaseMappers.kt`.

## Validação final

- confirmar que UI, coordinator BLE e Health Connect não foram tocados aqui.

Confirmado: UI, coordinator BLE e Health Connect não foram tocados nesta task.
