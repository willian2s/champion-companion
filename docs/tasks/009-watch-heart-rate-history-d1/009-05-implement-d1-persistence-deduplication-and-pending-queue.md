# Task 009-05 - Implementar persistencia D1, deduplicacao e fila pendente

## Status

Concluida em 2026-07-23.

## Objetivo

Implementar persistencia local de D1 com deduplicacao persistente, contagens explicitas, repair de operacao pendente ausente e metadata especifica de importacao.

## Contexto

`RoomRepositories.import(...)` hoje insere D1 novo e cria `PENDING` apenas para registro novo. Nao ha query por periodo, metadata especifica de D1 nem retorno tipado com contagens. `DeviceImportMetadata.lastImportedAt` tambem nao pode ser reutilizado como se fosse ultima importacao D1.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-03-define-d1-contracts-import-state-and-import-metadata-models.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-04-implement-d1-protocol-parser-and-multipart-collector.md`
- `docs/baseline/current-baseline.md`

## Dependencias

- `009-03`

## Entradas

- contratos de importacao D1 aprovados;
- politica de conflito `preserve-first`;
- necessidade de migration Room para metadata especifica.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/3.json`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-05-implement-d1-persistence-deduplication-and-pending-queue.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/data/RoomRepositoriesD1ImportTest.kt`
- `app/src/androidTest/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabaseSchemaTest.kt`

## Arquivos Proibidos

- `feature/health/**`
- `core/navigation/**`
- `feature/home/**`
- `feature/device/**`
- `core/bluetooth/**`

## Escopo

- migration Room `v2 -> v3` para metadata D1 especifica;
- contrato de persistencia D1 com `inserted`, `duplicates`, `conflicts`, `pendingCreated`, `pendingExisting`;
- deduplicacao persistente por `WATCH_D1_HISTORY + deviceId + measuredAt`;
- repair de `HealthSyncOperation.PENDING` ausente em registro D1 ja existente;
- metadata D1 especifica por `deviceId + importType`.

## Fora Do Escopo

- coordinator BLE;
- UI Compose;
- grafico e historico visual;
- Health Connect real.

## Passos

1. Evoluir schema Room e exportar schema novo.
2. Adicionar DAO e mapeadores para metadata D1 especifica.
3. Evoluir `HeartRateRepository` e `RoomRepositories` para `importD1Measurements(...)` tipado e idempotente.
4. Implementar politica `preserve-first` para conflito de mesmo timestamp com BPM divergente.
5. Garantir criacao ou repair idempotente de `HealthSyncOperation.PENDING`.
6. Adicionar testes pequenos de importacao idempotente, conflito e repair de pendente ausente.
7. Atualizar spec e overview com nomes reais e numero da migration.

## Criterios De Aceite

- migration Room criada e schema exportado;
- importacao D1 retorna contagens explicitas;
- deduplicacao persistente funciona sem usar BPM como segunda chave;
- conflito preserva primeiro registro;
- operacao `PENDING` ausente e reparada quando detectada;
- nenhuma transicao para `SYNCING` ou `SYNCED` ocorre nesta task.

## Testes Pequenos Aplicaveis

- importacao idempotente do mesmo lote;
- conflito de mesmo timestamp com BPM divergente;
- `PENDING` criada uma vez;
- repair de `PENDING` ausente em registro existente;
- metadata D1 persistida e observavel;
- schema `v3` valido.

## Validacao Minima

- `./gradlew assembleDebug`
- `./gradlew assembleRelease`
- `./gradlew testDebugUnitTest` para testes pequenos da task.

## Evidencias

- schema exportado novo;
- resultados de testes pequenos;
- diff mostrando contagens e repair de pendente ausente.

## Implementacao Executada

- `ChampionDatabase.kt` foi elevado para `version = 3` com `AutoMigration(from = 2, to = 3)` e nova entidade `DeviceImportMetadataEntity`.
- `ChampionDaos.kt` ganhou `DeviceImportMetadataDao` para leitura e upsert por `deviceId + importType`.
- `DatabaseMappers.kt` agora converte metadata D1 dedicada entre Room e dominio.
- `RoomRepositories.kt` agora implementa `importD1Measurements(...)`, `observeD1ImportMetadata(...)` e planner interno idempotente com repair de `PENDING` ausente.
- `RoomRepositoriesD1ImportTest.kt` cobre idempotencia, conflito `preserve-first` e repair de pendente ausente; `ChampionDatabaseSchemaTest.kt` cobre tabela nova no schema v3.

## Atualizacao Do Overview

- marcar `009-05` como concluida;
- registrar migration prevista/executada e schema exportado;
- liberar `009-06` e parte de `009-07`.

## Validacao Final

- confirmar que nenhuma permissao ou escrita Health Connect foi implementada.
