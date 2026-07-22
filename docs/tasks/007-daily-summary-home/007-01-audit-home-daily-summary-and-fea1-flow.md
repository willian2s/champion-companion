# Task 007-01 — Auditar Home, Resumo Diário E Fluxo FEA1

## Status

Concluida em 2026-07-22.

## Objetivo

Auditar estado real da Home, BLE, parser FEA1, repositories, DAOs, preferências e bindings Hilt antes de qualquer implementação funcional da Fase 07.

## Contexto

A Fase 06 fechou gerenciamento do dispositivo, mas Home ainda não consome resumo diário. Esta task reduz risco de implementar sync sobre premissas erradas.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/specs/_inputs/phase-07-daily-summary-home-direction.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`

## Dependências

- Nenhuma task anterior da Fase 07.

## Entradas

- Código atual em `feature/home`, `feature/device`, `core/bluetooth`, `core/model`, `core/database`, `core/preferences`, `data`.
- Spec 007.

## Arquivos Exatos Permitidos

- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/tasks/007-daily-summary-home/007-01-audit-home-daily-summary-and-fea1-flow.md`

## Arquivos Proibidos

- Qualquer `.kt`
- `app/build.gradle.kts`
- `settings.gradle.kts`
- `AndroidManifest.xml`
- `app/schemas/**`

## Escopo

- Confirmar gaps e riscos.
- Atualizar overview com achados se necessário.
- Não implementar código.

## Fora Do Escopo

- Parser novo.
- ViewModel.
- UI.
- Persistência.
- Testes.

## Passos

1. Ler docs obrigatórios.
2. Inspecionar arquivos citados na spec.
3. Confirmar que auto-connect não equivale a sync.
4. Confirmar estado de `DailySummaryRepository.save()` e riscos de redução.
5. Confirmar parser FEA1 atual.
6. Confirmar lacunas em prefs, profile metrics e Hilt.
7. Atualizar status da task e checklist do overview.

## Critérios De Aceite

- Achados registrados.
- Riscos de conflito confirmados.
- Nenhum código alterado.
- Overview atualizado.

## Validação Mínima

- Revisão documental.
- `git diff -- docs/specs/007-daily-summary-home.md docs/tasks/007-daily-summary-home/`.

## Evidência Manual

- Lista de arquivos lidos e achados no corpo da task ou overview.

## Arquivos Lidos

- `docs/baseline/current-baseline.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/specs/_inputs/phase-07-daily-summary-home-direction.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocFea1Steps.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/AppPreferences.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DomainMetadata.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/preferences/AppPreferencesStore.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/preferences/PreferencesModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`

## Achados

1. Home continua sem fluxo proprio de resumo diario.
   - `HomeScreen.kt` ainda instancia `DeviceManagementViewModel` via `ViewModelProvider(activity)` e mostra `Painel em preparação`.
   - `HomeFeatureContract.kt` ainda expõe apenas `data object HomeUiState`.

2. Auto-connect continua diferente de sync.
   - `DeviceManagementViewModel` chama `attemptAutoConnectForCurrentEntry()` na entrada da feature e usa `DeviceConnectionBootstrap` para conectar dispositivo associado.
   - Fluxo automatico atual persiste associação e `lastSuccessfulConnectionAt`, mas não toca `DailySummaryRepository`, não aguarda FEA1 fresco e não atualiza `DeviceImportMetadata.lastImportedAt`.

3. Reuso de sessão existe, mas não está ligado ao bootstrap atual.
   - `ChampionConnectionManager.currentSession()` expõe sessão ativa reutilizável.
   - `DeviceConnectionBootstrap.connectAssociatedDevice()` sempre tenta `connectAndHandshake()` e fallback por scan direcionado; não reaproveita `currentSession()` sozinho.
   - `ChampionNotificationHub` já suporta multiplexar subscriptions temporárias e fecha collectors no disconnect.

4. Parser FEA1 atual está correto, mas permanece técnico e isolado na feature de device.
   - `DevicePocFea1Steps` valida payload com 4 bytes, header `0x01`, parse 24-bit little-endian e usa `bytesCopy()`.
   - Parser segue `internal object` em `feature/device` e ainda não foi promovido para componente único reutilizável.

5. `DailySummaryRepository.save()` confirma risco de redução por upsert simples.
   - `RoomRepositories.save()` busca por chave natural e faz `update(entity.copy(id = existing.id))` sem regra de `max`.
   - Leitura menor sobrescreve leitura maior do mesmo dia/dispositivo/origem.

6. DAO e schema já suportam fase 07 sem migration imediata.
   - `DailySummaryDao` já possui `findByNaturalKey()` e `observeBetween()`.
   - `ChampionDatabase` v2 já contém `daily_summaries`, índice único `(deviceId, origin, localDateIso)` e colunas `lastImportedAtEpochMillis`/`importParserVersion` em `devices`.

7. Repositório de dispositivo ainda não oferece atualização explícita de importação.
   - `DeviceRepository` não declara operação equivalente a `updateImportMetadata()`.
   - `RoomRepositories` só preserva `lastImportedAt` existente durante upserts de associação; não há caminho dedicado para atualizar metadados após import real.

8. Preferências e métricas pessoais ainda têm lacunas para Home funcional.
   - `AppPreferences` não contém `dailyStepsGoal`.
   - `AppPreferencesStore` implementa `AppPreferencesRepository`, mas `PreferencesModule` só fornece `DataStore` e `RepositoryModule` não faz binding do contrato `AppPreferencesRepository`.
   - `UserProfileDao` observa perfil atual e lista de pesos, porém `RepositoryContracts.kt` não expõe contrato mínimo para altura/peso/passada e não há binding Hilt correspondente.

## Riscos Confirmados

- `HomeScreen.kt`, `HomeFeatureContract.kt`, `DeviceManagementViewModel.kt`, `DeviceConnectionBootstrap.kt`, `RepositoryContracts.kt`, `RoomRepositories.kt`, `AppPreferences.kt`, `AppPreferencesStore.kt`, `PreferencesModule.kt` e `RepositoryModule.kt` permanecem pontos de conflito prováveis nas próximas tasks.
- Reuso de sessão precisará ficar acima do `DeviceConnectionBootstrap`; se task futura empurrar essa regra para dentro do bootstrap, risco de misturar responsabilidades da Fase 06 com coordinator da Fase 07.
- Sem contrato mínimo de métricas pessoais, distância/calorias tendem a puxar DAO direto ou lógica provisória para UI/ViewModel, o que conflita com arquitetura esperada da spec 007.
- Sem operação transacional dedicada de merge + import metadata, qualquer implementação apressada tende a reutilizar `save()` atual e reduzir passos acidentalmente.

## Resultado Da Auditoria

- Auto-connect não equivale a sync: confirmado.
- `DailySummaryRepository.save()` não protege contra redução: confirmado.
- Parser FEA1 atual existe e está correto, mas ainda não é reutilizável: confirmado.
- Lacunas em prefs, métricas pessoais e bindings Hilt: confirmadas.
- Nenhum código foi alterado nesta task.

## Atualização Do Overview

- Marcar `007-01` como concluída no checklist.
- Adicionar riscos descobertos, se houver.

## Validação Final

- `git status --short` para confirmar que só docs planejados mudaram.
