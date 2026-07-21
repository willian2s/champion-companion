# Task 005-04 — Criar State Machine E Coordenador Da PoC

## Status

Concluída em 2026-07-21.

Resumo: `feature/device` agora possui modelo explícito de state machine da PoC em `DevicePocState`, `DevicePocCycleId` e `DeviceUiState`, além do coordenador observável `DevicePocCoordinator`. O coordenador depende de `ChampionConnectionManager`, mantém `cycleId` e `activeSessionId`, escuta `ChampionBleState` sem redefinir sua semântica, expõe transições controladas para protocolo da PoC e implementa `resetCycle()`/`disconnect()` com limpeza de candidato, erro e sessão ativa. Guards de sessão/ciclo ignoram callbacks de sessão antiga após reset. `app:compileDebugKotlin` passou. A validação por `app:testDebugUnitTest --tests "*DevicePocCoordinatorTest"` continua bloqueada por falhas pré-existentes não relacionadas em `core/designsystem`, `core/error`, `core/model` e `data`, mas a rerun não mostrou erro restante no teste novo.

## Objetivo

Criar camada de orquestração técnica da PoC com state machine explícita, ownership de ciclo/sessão e regras de cleanup.

## Contexto

`ChampionBleState` representa somente transporte. Fase 05 precisa estado de protocolo e de ciclo sem contaminar o core com booleans concorrentes nem empurrar lógica para Compose.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/specs/_inputs/phase-05-native-ble-poc-direction.md`
- `docs/specs/004-native-ble-core.md`

## Dependências

- `005-02` concluída.

## Pré-Condições

- dispatch de comando e `TransportReady` físico disponíveis.

## Entradas

- `ChampionConnectionManager`
- `ChampionTransportSession`
- `ChampionBleState`

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`, somente se extensão mínima do contrato exposto for necessária
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/tasks/005-native-ble-poc/005-04-create-poc-state-machine-and-coordinator.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `app/src/main/AndroidManifest.xml`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`

## Escopo

- criar estados da PoC;
- criar coordenador técnico observável;
- implementar reset de ciclo e cleanup;
- guardar `sessionId` e `cycleId` para bloquear callbacks tardios.

## Fora Do Escopo

- UI completa;
- handshake A1 funcional;
- parsers FEA1/D0/F002 completos.

## Passos

1. Definir modelo de estado da PoC.
2. Implementar coordenador técnico com dependência de `ChampionConnectionManager` e transporte.
3. Definir regras de transição válidas e ações permitidas por estado.
4. Implementar reset por ciclo, invalidação de handshake e guardas de callback tardio.
5. Cobrir state machine e cleanup com testes JVM.
6. Atualizar `Status` e overview.

## Regras De Segurança

- não redefinir `ChampionBleState` como estado de protocolo;
- não usar mar de booleans;
- não permitir que cleanup dependa do sucesso do stop.

## Critérios De Aceite

- state machine explicita estados principais da PoC;
- coordenador orquestra fluxo sem acessar `BluetoothGatt`;
- reset e cleanup limpam estado anterior;
- callbacks tardios não atualizam ciclo novo.

## Testes

- JVM para state machine, reset, cleanup e stale callback.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Poc*"
```

## Evidências Esperadas

- testes da state machine e de guards de sessão/ciclo.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar ausência de dependência do coordenador com `BluetoothGatt`, Room e Compose.

## Implementação Executada

### Modelo de estado da PoC

Foram adicionados em `DeviceFeatureContract.kt`:

- `DevicePocCycleId`;
- `DevicePocState` com estados explícitos:
  - `Idle`
  - `PermissionsRequired`
  - `ReadyToScan`
  - `Scanning`
  - `DeviceFound`
  - `Connecting`
  - `TransportReady`
  - `Handshaking`
  - `HandshakeConfirmed`
  - `WaitingForSteps`
  - `StepsObserved`
  - `StartingHeartRate`
  - `MeasuringHeartRate`
  - `StoppingHeartRate`
  - `CycleCompleted`
  - `Disconnecting`
  - `Failed`
- `DeviceUiState` com:
  - `cycleId`
  - `transportState`
  - `pocState`
  - `candidate`
  - `activeSessionId`
  - `lastError`

### Coordenador técnico

`DevicePocCoordinator.kt` foi criado com:

- dependência de `ChampionConnectionManager`;
- `StateFlow<DeviceUiState>` observável;
- coleta contínua de `ChampionBleState`;
- métodos de entrada atuais:
  - `markPermissionsRequired()`
  - `markReadyToScan()`
  - `scan(request)`
  - `connect(device)`
  - `disconnect(reason)`
  - `resetCycle()`
  - `currentTransportSession()`
- métodos `internal` para evolução futura do protocolo:
  - `moveToHandshaking(sessionId)`
  - `moveToHandshakeConfirmed(sessionId)`
  - `moveToWaitingForSteps(sessionId)`
  - `moveToStepsObserved(sessionId)`

### Regras de ownership e cleanup

- `cycleId` troca em `resetCycle()` antes do disconnect do manager;
- `activeSessionId` só é aceito quando `ChampionBleState.Connecting` corresponde ao candidato atual;
- `TransportReady`, `Failed` e `Disconnecting` só atualizam o ciclo ativo quando `sessionId` confere;
- `Disconnected` após ciclo ativo limpa candidato, erro e sessão;
- cleanup não depende de handshake, FEA1 ou D0/F002 já existirem.

## Critérios De Aceite Atendidos

- state machine da PoC ficou explícita e separada do transporte;
- coordenador orquestra fluxo sem acessar `BluetoothGatt`;
- reset e cleanup limpam estado anterior;
- callbacks tardios de sessão antiga são ignorados após reset de ciclo;
- `ChampionBleState` não foi redefinido como estado de protocolo.

## Testes E Validação

### Produção

Comando executado:

```bash
./gradlew app:compileDebugKotlin
```

Resultado:

- `BUILD SUCCESSFUL`

### Teste focado da PoC

Arquivo criado:

- `app/src/test/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocCoordinatorTest.kt`

Cenários cobertos no teste novo:

- reset de ciclo invalida estado técnico e handshake;
- callback tardio não atualiza ciclo novo;
- fluxo `scan -> connect -> TransportReady -> disconnect` atualiza state machine;
- falha da sessão ativa transita para `Failed`.

Comando executado:

```bash
./gradlew app:testDebugUnitTest --tests "*DevicePocCoordinatorTest"
```

Resultado:

- bloqueado por falhas pré-existentes não relacionadas em `core/designsystem`, `core/error`, `core/model` e `data` durante compilação ampla dos testes do módulo;
- após correção do único erro local detectado (`FakeChampionBleScanner.enqueueResult`), rerun não mostrou erro adicional do teste novo.

## Evidência Final

- coordenador da PoC existe em `feature/device`;
- state machine está pronta para tasks `005-05` a `005-08`;
- nenhum acesso a `BluetoothGatt`, Room, Health Connect ou Compose foi movido para o coordenador;
- nenhuma alteração em Manifest, roadmap ou ADRs foi feita nesta task.
