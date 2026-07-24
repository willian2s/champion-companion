# Task 012-04 - Expandir domínio de sync, record types, identidades e schema Room

## Status

Planejada.

## Objetivo

Expandir domínio, contratos básicos e schema Room para suportar fila Health Connect granular com múltiplos tipos de record, IDs estáveis, versões e metadados de sincronização.

## Contexto

O schema atual `v4` e o domínio de sync ainda estão limitados ao backlog D1 de `HEART_RATE`. A Fase 12 precisa ampliar essa base antes de qualquer gateway ou coordinator real.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-02-formalize-health-connect-sync-policy-adr-and-roadmap.md`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/4.json`

## Dependências

- `012-02`.

## Entradas

- política de IDs e versões da ADR da fase;
- estado real de `HeartRateAndSync.kt`, `RepositoryContracts.kt`, `ChampionDatabase.kt`, `ChampionDaos.kt` e `DatabaseMappers.kt`.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-04-expand-sync-domain-record-types-identities-and-room-schema.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/*.json`

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- `AndroidManifest.xml`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `feature/**`

## Escopo

- expandir `HealthSyncRecordType`;
- expandir `HealthSyncAggregate`;
- adicionar `clientRecordId`, `clientRecordVersion`, `syncedAt` e demais metadados necessários;
- planejar timezone de `DailySummary`;
- criar migration a partir do schema `v4`;
- preservar operações D1 existentes;
- exportar schema novo.

## Fora do escopo

- implementar gateway do Health Connect;
- integrar UI;
- processar backlog real.

## Passos

1. Expandir modelos de domínio e contratos base.
2. Evoluir entidade e DAO de `health_sync_operations`.
3. Implementar migration `v4 -> próxima versão`.
4. Exportar schema atualizado.
5. Atualizar spec e overview com a modelagem definitiva.

## Critérios de aceite

- domínio de sync comporta passos, BPM, atividade, peso e altura;
- schema novo preserva backlog D1 atual;
- migration parte do `v4`;
- campos novos suportam repair idempotente quando necessário.

## Validação manual

- revisão de schema exportado e migration;
- leitura cruzada entre domínio e banco.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- diff de domínio;
- diff de banco e DAOs;
- novo schema exportado.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `012-04` como concluída;
- registrar versão nova do banco e hotspots de schema.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
