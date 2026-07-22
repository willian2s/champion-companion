# Task 008-01 - Auditar PoC de frequencia cardiaca, dominio e feature Saude

## Status

Concluida em 2026-07-22.

## Objetivo

Auditar estado real da PoC D0/F002, dominio de frequencia cardiaca, MotionLog, feature Saude, Home e ownership BLE antes de qualquer implementacao funcional da Fase 08.

## Achados

- `feature/health/HealthScreen.kt` ainda e placeholder via `PlaceholderScreen(title = "Saude")` e `HealthFeatureContract.kt` ainda expoe apenas `data object HealthUiState`. Nao existe `HealthViewModel`, intent, historico, estado de sessao ou integracao BLE/repository.
- `feature/home/HomeViewModel.kt` ja controla refresh explicito com `refreshMutex`, observa resumo diario, dispositivo associado e estado BLE, e nao observa BPM persistido. `feature/home/HomeScreen.kt` continua focada em resumo diario e status do relogio.
- `feature/home/ChampionDailyStepsSyncCoordinator.kt` ja tenta reusar `ChampionConnectionManager.currentSession()` quando `transportKey` coincide com dispositivo associado. Se nao houver sessao compativel, chama `DeviceConnectionBootstrap.connectAssociatedDevice(...)`.
- `feature/device/DeviceConnectionBootstrap.kt` nao reutiliza sessao ativa por conta propria. Ele sempre entra em `connectAndHandshake(...)`, que chama `ChampionConnectionManager.connect(device)`.
- `core/bluetooth/ChampionConnectionManager.kt` continua owner da unica sessao BLE ativa do app. `connect(device)` chama `disconnect(CLEANUP)` no inicio, entao qualquer nova conexao foreground derruba sessao atual antes de abrir outra.
- `feature/device/DeviceManagementViewModel.kt` ainda pode iniciar auto-connect controlado ao entrar na tela de Dispositivo. Isso compartilha mesmo owner BLE do app e hoje nao existe gate global de produto entre Dispositivo, Home e futura medicao manual.
- `feature/device/DevicePocCoordinator.kt` ja tem guards por `activeSessionId` e `activeHeartRateSessionId`, subscribe F002 antes do start, cleanup de jobs/buffer e selecao final recalculada a partir de `lastD0Bpm` e estimativa F002 da sessao atual.
- `feature/device/DevicePocHeartRateSession.kt` confirma contrato tecnico atual: `D0 01`, `D0 00`, parser D0 somente em `F008_NOTIFY`, filtro `35..220`, parser F002 little-endian por pares e buffer limitado em memoria.
- `feature/device/DevicePocF002BpmEstimator.kt` preserva heuristica da Fase 05: minimo 250 amostras positivas, descarte inicial `min(25%, 6s)`, baseline/suavizacao, threshold `mean + std * 0.35`, minimo 3 intervalos validos e politica final `D0_CURRENT -> F002_ESTIMATE -> UNAVAILABLE`.
- MotionLog auditado em `motionlog-app/src/app/(tabs)/health/index.tsx` e `motionlog-app/src/lib/ble/champion-heart-session.ts`: fluxo manual da Saude fecha com `finalBpm = d0Bpm ?? estimatedBpm ?? null`. Sessao F002 apenas calcula estimativa; nao ha media nova fechando resultado manual de 60 segundos.
- `core/model/HeartRateAndSync.kt` ja suporta medicao manual final sem mudar schema: `HeartRateSource.MANUAL_MEASUREMENT` existe, `HeartRateDeduplicationKey.MeasurementSession` existe, `HeartRateMeasurement.create(...)` aceita `deviceId`/`parserVersion` opcionais para fontes manuais e exige apenas BPM valido e `measuredAt` ou `observedAt`.
- `core/model/RepositoryContracts.kt` ainda limita `HeartRateRepository` a `import()` e `findPendingSync()`. Nao existe API atual para salvar medicao manual final, observar ultima medicao valida nem listar recentes manuais.
- `core/database/ChampionDatabase.kt` v2 e `heart_rate_measurements` ja possuem `source`, `bpm`, `measuredAtEpochMillis`, `deviceId`, `parserVersion`, `appWorkoutSessionId`, `workoutActivityId` e `deduplicationKey` unico. `ChampionDaos.kt` ja expoe `findByDeduplicationKey(...)`, mas ainda nao existem queries de ultima medicao valida ou recentes manuais.
- `data/DatabaseMappers.kt` ja serializa `HeartRateDeduplicationKey.MeasurementSession` como `MEASUREMENT|<uuid>` e reconstrui dominio correspondente. `data/RoomRepositories.kt` ja deduplica `import()` por `deduplicationKey`, mas caminho atual foi desenhado para import geral e `HealthSyncOperation` de `WATCH_D1_HISTORY`, nao para persistencia de medicao manual de produto.

## Riscos Confirmados

- Falta gate global de operacao BLE foreground. Hoje Home usa `refreshMutex` local, Dispositivo usa `activeOperation` local e PoC usa guards locais, mas nenhum deles impede concorrencia cross-feature.
- `ChampionConnectionManager.currentSession()` e reuso atual da Home nao sao aquisicao atomica de ownership. Entre checagem e uso, outra feature pode chamar `connect()` e derrubar sessao ativa.
- Sem gate, refresh da Home pode colidir com medicao manual futura ou com auto-connect de Dispositivo. Como `connect()` faz cleanup da sessao ativa, conflito tende a cancelar uma operacao em vez de coexistir com seguranca.
- `DeviceConnectionBootstrap` ainda nao e gate-aware nem session-aware. Reuso de sessao compativel hoje fica na Home; medicao manual precisara repetir essa regra ou extrair componente compartilhado acima do bootstrap.

## Migration

- Auditoria atual nao encontrou necessidade de migration Room para Fase 08.
- Mudanca esperada fica restrita a novos metodos de DAO/query, contracts, mappers e repository usando schema `heart_rate_measurements` v2 existente.

## Contexto

Fase 08 depende de componentes tecnicos existentes da PoC, de contratos de persistencia ja presentes e de restricoes fortes: medicao so por acao explicita, persistencia somente do resultado final e ausencia de Health Connect.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/specs/_inputs/phase-08-manual-heart-rate-direction.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/protocol/heart-rate-d0-f002.md`

## Dependencias

- Nenhuma task anterior da Fase 08.

## Entradas

- Codigo atual em `feature/health`, `feature/home`, `feature/device`, `core/bluetooth`, `core/model`, `core/database`, `data`.
- MotionLog apenas como referencia funcional.

## Arquivos Exatos Permitidos

- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-01-audit-poc-heart-rate-domain-and-health-feature.md`

## Arquivos Proibidos

- Qualquer `.kt`
- `AndroidManifest.xml`
- `app/build.gradle.kts`
- `settings.gradle.kts`
- `app/schemas/**`

## Escopo

- Confirmar pontos de extensao e riscos.
- Auditar MotionLog para regra real do resultado final e da suposta media.
- Confirmar se schema atual atende sem migration.
- Atualizar spec e overview com achados factuais.

## Fora Do Escopo

- Implementar codigo.
- Criar ADR.
- Criar testes.

## Passos

1. Ler docs obrigatorios.
2. Inspecionar `HealthScreen`, `HomeScreen`, `HomeViewModel`, `DeviceConnectionBootstrap`, `DeviceManagementViewModel`, `DevicePocCoordinator`, `DevicePocHeartRateSession`, `DevicePocF002BpmEstimator`, `ChampionConnectionManager`, `ChampionTransportSession`, `ChampionNotificationHub`, `ChampionPacketRouter`, `HeartRateAndSync.kt`, `RepositoryContracts.kt`, `ChampionDatabase.kt`, `ChampionDaos.kt`, `DatabaseMappers.kt`, `RoomRepositories.kt` e bindings Hilt relacionados.
3. Confirmar se MotionLog usa media ou somente `D0 -> F002` no fluxo manual da Saude.
4. Confirmar se `HeartRateMeasurement` atual suporta medicao manual final sem migration.
5. Registrar riscos de ownership BLE e conflito com Home refresh.
6. Atualizar status da task e checklist do overview.

## Criterios De Aceite

- Achados registrados com origem.
- Regra da media auditada explicitamente.
- Necessidade ou ausencia de migration registrada.
- Nenhum codigo alterado.

## Testes Pequenos Aplicaveis

- Nenhum nesta task documental.

## Validacao Minima

- Revisao documental.
- `git diff -- docs/specs/008-manual-heart-rate.md docs/tasks/008-manual-heart-rate/`.

## Evidencia Manual

- Lista de arquivos lidos e achados registrada no proprio arquivo e no overview.
- Arquivos lidos nesta auditoria:
  - `docs/baseline/current-baseline.md`
  - `docs/specs/008-manual-heart-rate.md`
  - `docs/specs/_inputs/phase-08-manual-heart-rate-direction.md`
  - `docs/tasks/008-manual-heart-rate/008-00-overview.md`
  - `docs/specs/007-daily-summary-home.md`
  - `docs/tasks/007-daily-summary-home/007-00-overview.md`
  - `docs/specs/006-device-management.md`
  - `docs/tasks/006-device-management/006-00-overview.md`
  - `docs/specs/005-native-ble-poc.md`
  - `docs/protocol/heart-rate-d0-f002.md`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthScreen.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/ChampionDailyStepsSyncCoordinator.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocCoordinator.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocHeartRateSession.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocF002BpmEstimator.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionPacketRouter.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
  - `../motionlog-app/src/app/(tabs)/health/index.tsx`
  - `../motionlog-app/src/lib/ble/champion-heart-session.ts`
  - `../motionlog-app/src/lib/ble/champion-bpm-estimator.ts`

## Atualizacao Do Overview

- Marcar `008-01` como concluida.
- Atualizar riscos de conflito e achados de auditoria.

## Validacao Final

- `git status --short` para confirmar alteracoes apenas documentais.
