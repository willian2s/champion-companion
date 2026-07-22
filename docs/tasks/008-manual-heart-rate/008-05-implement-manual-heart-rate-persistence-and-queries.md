# Task 008-05 - Implementar persistencia final e queries de frequencia cardiaca manual

## Status

Planejada.

## Objetivo

Evoluir persistencia e repository para salvar somente resultado final valido da medicao manual e expor queries para ultima medicao valida e recentes manuais, sem mudar schema salvo necessidade funcional comprovada.

## Contexto

Dominio atual ja possui `MANUAL_MEASUREMENT` e deduplicacao por `MeasurementSession`, mas repository ainda so importa D1 e busca pendentes de sync. Fase 08 precisa caminho de produto para salvar um unico resultado final por sessao.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/specs/003-domain-and-persistence-foundation.md`

## Dependencias

- `008-02`

## Entradas

- `HeartRateAndSync.kt`
- `RepositoryContracts.kt`
- `ChampionDaos.kt`
- `DatabaseMappers.kt`
- `RoomRepositories.kt`
- `ChampionDatabase.kt`

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `app/src/androidTest/java/br/com/mwconsulting/championcompanion/core/database/**`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-05-implement-manual-heart-rate-persistence-and-queries.md`

## Arquivos Proibidos

- `HealthScreen.kt`
- `HealthFeatureContract.kt`
- `DeviceConnectionBootstrap.kt`
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Adicionar API de save manual idempotente por sessao.
- Adicionar query generica de ultima medicao valida.
- Adicionar query de recentes manuais validos.
- Manter importacao D1 e pendencias de sync compativeis.
- Evitar migration se schema atual atender.

## Fora Do Escopo

- UI.
- Coordinator BLE.
- Health Connect.

## Passos

1. Definir `ManualHeartRateResult` ou entrada equivalente de persistencia.
2. Evoluir `HeartRateRepository` com save e queries observaveis.
3. Implementar DAO para ultima medicao valida e recentes manuais.
4. Implementar idempotencia por `deduplicationKey` em repository.
5. Garantir que manual measurement nao crie `HealthSyncOperation`.
6. Adicionar pequenos testes de dedup e queries criticas quando aplicavel.
7. Atualizar overview.

## Criterios De Aceite

- Uma sessao manual persiste no maximo um registro.
- Cancelamento nao persiste.
- Resultado invalido ou indisponivel nao persiste.
- Query generica de ultimo BPM valido funciona para Home.
- Query de recentes manuais funciona para Saude.
- D1 permanece compativel.

## Testes Pequenos Aplicaveis

- repository deduplica por `sessionId`;
- sessao concluida persiste uma vez;
- ultima medicao considera somente `VALID`;
- recentes manuais filtram `MANUAL_MEASUREMENT`.

## Validacao Minima

```bash
./gradlew assembleDebug
```

## Evidencia Manual

- Registrar se schema atual bastou ou se houve necessidade funcional inevitavel de migration.

## Atualizacao Do Overview

- Marcar `008-05` como concluida.
- Atualizar lista de arquivos de conflito se DAO/query final mudar.

## Validacao Final

- Confirmar que manual measurement nao entrou na fila de sync Health Connect.
