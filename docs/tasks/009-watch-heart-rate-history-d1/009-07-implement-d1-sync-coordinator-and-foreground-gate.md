# Task 009-07 - Implementar coordinator D1 e gate foreground compartilhado

## Status

Pendente.

## Objetivo

Implementar coordinator de importacao D1, arbitragem foreground BLE e reuso seguro de sessao/app handshake para consulta explicita do historico D1.

## Contexto

Core BLE atual ja oferece sessao unica, fila serial, `ChampionNotificationHub`, `ChampionPacketRouter`, `TransportReady` e `DeviceConnectionBootstrap`. Tambem ja existe gate foreground para `DAILY_STEPS_REFRESH` e `MANUAL_HEART_RATE`. Fase 09 precisa adicionar owner `WATCH_D1_HISTORY` e um coordinator de produto equivalente ao da medicao manual, mas sem duplicar bootstrap, conexao ou protocolo A1.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-04-implement-d1-protocol-parser-and-multipart-collector.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-05-implement-d1-persistence-deduplication-and-pending-queue.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/handshake-a1.md`

## Dependencias

- `009-04`
- `009-05`

## Entradas

- protocolo D1 pronto;
- persistencia D1 tipada pronta;
- gate atual com owners existentes;
- necessidade de conflito explicito com medicao manual e refresh diario.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/D1HeartRateSyncCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/ChampionD1HeartRateSyncCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/InMemoryForegroundBleOperationGate.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-07-implement-d1-sync-coordinator-and-foreground-gate.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/feature/health/ChampionD1HeartRateSyncCoordinatorTest.kt`

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- `feature/health/HealthMetricDetailScreens.kt`
- `feature/health/HealthViewModel.kt`
- `docs/roadmap/**`

## Escopo

- interface e implementacao do coordinator D1;
- owner `WATCH_D1_HISTORY` no gate foreground;
- reuso de sessao atual compativel ou conexao controlada ao associado;
- handshake A1 antes de D1;
- timeout, cancelamento, cleanup e `finally`;
- mensagens de bloqueio seguras em Home e Dispositivo.

## Fora Do Escopo

- UI final do botao e do grafico;
- migration Room;
- Health Connect real.

## Passos

1. Criar contrato do coordinator D1 na feature de Saude.
2. Implementar `ChampionD1HeartRateSyncCoordinator` usando `ForegroundBleOperationGate`, `DeviceConnectionBootstrap`, `ChampionConnectionManager`, `ChampionD1HistoryProtocol` e `HeartRateRepository`.
3. Adicionar owner `WATCH_D1_HISTORY` ao gate e garantir liberacao em `finally`.
4. Generalizar mensagens de bloqueio em `HomeViewModel` e `DeviceManagementViewModel` para contemplar D1 alem da medicao manual.
5. Adicionar testes pequenos de concorrencia, timeout, cancelamento, reuso de sessao e callback tardio ignorado.
6. Atualizar spec e overview com nomes reais do coordinator.

## Criterios De Aceite

- owner `WATCH_D1_HISTORY` adicionado ao gate;
- D1 conflita com `DAILY_STEPS_REFRESH` e `MANUAL_HEART_RATE`;
- gate libera em cancelamento, timeout, erro e `finally`;
- coordinator reusa sessao compativel quando possivel;
- handshake A1 ocorre antes do envio D1;
- callback tardio nao reabre tentativa nem readquire gate.

## Testes Pequenos Aplicaveis

- gate bloqueia concorrencia com manual e refresh diario;
- cancelamento libera gate;
- timeout libera gate;
- callback tardio ignorado;
- sessao compativel e reutilizada;
- sem retry automatico interno do D1.

## Validacao Minima

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest` focado no coordinator D1.

## Evidencias

- resultados de testes pequenos;
- diff de gate e coordinator mostrando cleanup em `finally`.

## Atualizacao Do Overview

- marcar `009-07` como concluida;
- registrar owner novo no gate e pontos de conflito resolvidos;
- confirmar dependencia de `009-08` em `009-07`.

## Validacao Final

- confirmar que grafico, historico visual e provider Health Connect continuaram fora desta task.
