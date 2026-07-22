# Task 007-04 — Implementar Consolidação Atômica Do Resumo Diário

## Status

Concluida em 2026-07-22.

## Objetivo

Criar operação repository atômica para consolidar FEA1 acumulado em `DailySummary` sem permitir redução de passos.

## Contexto

`DailySummaryRepository.save()` atual faz upsert cego. FEA1 é acumulado e leitura menor não pode reduzir total persistido.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/specs/003-domain-and-persistence-foundation.md`

## Dependências

- `007-02`
- `007-03`

## Entradas

- `DailySummaryRepository`
- `DeviceRepository`
- `DailySummaryDao`
- `DeviceDao`
- `RoomRepositories`
- `DailySummary`
- `DeviceImportMetadata`

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/tasks/007-daily-summary-home/007-04-implement-atomic-daily-summary-consolidation.md`

## Arquivos Proibidos

- `HomeScreen.kt`
- `HomeViewModel.kt`
- `DeviceManagementViewModel.kt`
- `ChampionConnectionManager.kt`
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Adicionar `mergeWatchDailySummary` ou nome equivalente.
- Usar transação Room.
- Para mesma chave: `nextSteps = max(existingSteps, incomingSteps)`.
- Atualizar `DeviceImportMetadata.lastImportedAt` somente após persistir resumo.
- Testar leitura maior, igual, menor e nova data.

## Fora Do Escopo

- UI.
- Coordinator BLE.
- Health Connect.
- Mudança de schema salvo necessidade comprovada.

## Passos

1. Definir resultado do merge quando criação/atualização preserva valor.
2. Implementar DAO/update necessário.
3. Implementar repository em transação.
4. Preservar `lastSuccessfulConnectionAt` separado de `lastImportedAt`.
5. Adicionar testes pequenos.
6. Atualizar overview.

## Critérios De Aceite

- Leitura maior substitui.
- Leitura igual é sucesso sem soma.
- Leitura menor não reduz.
- Merge é atômico.
- `lastImportedAt` atualiza só em sucesso de persistência.

## Validação Mínima

```bash
./gradlew assembleDebug
```

## Evidência Manual

- `DailySummaryRepository.mergeWatchDailySummary(input)` foi implementado em `RoomRepositories.kt` com `withWriteTransaction` já centralizado no helper `write { ... }`.
- Para mesma chave natural (`deviceId`, `WATCH_DAILY_SUMMARY`, `localDate`), regra ficou:
  - sem resumo: cria novo `DailySummary`;
  - leitura maior: atualiza passos, estimativas, `collectedAt` e `updatedAt`;
  - leitura igual ou menor: preserva resumo existente, não soma e não reduz.
- `DeviceRepository.updateImportMetadata(...)` foi implementado para atualizar `lastImportedAt` e `importParserVersion` sem tocar `lastSuccessfulConnectionAt`.
- `mergeWatchDailySummary(input)` atualiza `DeviceImportMetadata.lastImportedAt` e `importParserVersion` na mesma transação somente após resumo válido estar persistido/preservado com sucesso.
- Nenhum teste novo foi adicionado ou executado nesta task por orientação explícita do usuário para não investir em testes agora.

## Atualização Do Overview

- Marcar `007-04` como concluída.

## Validação Final

- Confirmar que regra de merge não ficou no ViewModel.
