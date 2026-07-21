# Task 004-15 — Validar Núcleo BLE Nativo

## Status

Concluída em 2026-07-21. `lint`, `testDebugUnitTest`, `assembleDebug`, `assembleRelease` e `connectedDebugAndroidTest` passaram após correção dos wrappers Android BLE em `core/bluetooth/android`.

## Objetivo

Consolidar evidências da Fase 04, validar critérios da spec e encerrar `004-00`.

## Contexto

Validação final deve confirmar que núcleo BLE está pronto para Fase 05 sem já implementar handshake ou PoC física.

## Documentos Obrigatórios

- `docs/tasks/004-native-ble-core/004-00-overview.md`
- `docs/specs/004-native-ble-core.md`
- todas tasks `004-01` a `004-14`

## Dependências

- `004-01` a `004-14` concluídas.

## Entradas

- código BLE criado;
- testes e evidências das tasks anteriores.

## Arquivos Exatos Permitidos

- `docs/tasks/004-native-ble-core/004-15-validate-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-00-overview.md`, somente para checklist/status final

## Arquivos Que Não Podem Ser Modificados

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`
- `docs/specs/_inputs/phase-04-native-ble-core-direction.md`

## Escopo

- revisar arquitetura, limites e rastreabilidade;
- executar validação de build e testes;
- confirmar ausência de A1/D0/D1/D3/D4 funcionais;
- consolidar lacunas físicas remanescentes para Fase 05.

## Fora Do Escopo

- commit;
- push;
- scan ou conexão reais com relógio;
- alterar roadmap ou ADRs.

## Regras De Segurança

- registrar bloqueios reproduzíveis em vez de mascará-los;
- não tratar `TransportReady` como handshake concluído;
- não validar fisicamente Champion C001 nesta fase.

## Passos De Implementação

1. Revisar componentes BLE contra a spec e checklist.
2. Confirmar fronteira Android BLE única e ausência de vazamento de `BluetoothGatt`.
3. Executar `./gradlew lint testDebugUnitTest assembleDebug assembleRelease`.
4. Executar instrumentados mínimos se houver e registrar resultado ou bloqueio reproduzível.
5. Confirmar que A1, D0, D1, D3 e D4 seguem fora do escopo funcional.
6. Atualizar `004-00` com checklist final e este arquivo com evidências.

## Critérios De Aceite

- build e testes relevantes passam ou possuem bloqueio explícito;
- núcleo BLE cumpre limites da spec;
- Fase 05 pode iniciar PoC física sem refatoração estrutural do core.

## Testes

- `lint`
- `testDebugUnitTest`
- `assembleDebug`
- `assembleRelease`
- `connectedDebugAndroidTest`, se aplicável

## Comandos De Validação

```bash
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
./gradlew app:connectedDebugAndroidTest
git status --short
```

## Validação Final

- listar arquivos criados/alterados;
- resumir arquitetura, state machine, fila, timeout/cancelamento/retry;
- listar canais obrigatórios e opcionais;
- listar decisões que pedem ADR;
- confirmar ausência de implementação funcional A1/D0/D1/D3/D4;
- confirmar ausência de alteração em roadmap, ADRs e fases anteriores;
- mostrar `git status --short`;
- confirmar sem commit e sem push.

## Atualização Documental Esperada

- `Status` deste arquivo concluído;
- checklist de `004-00` consolidado.

## Evidências

### Revisão arquitetural

- Fronteira Android BLE segue concentrada em `core/bluetooth/android`, com tipos Android GATT localizados em `AndroidChampionBleScanner.kt`, `AndroidGattFacade.kt`, `AndroidGattDiscoveryMapper.kt` e `AndroidBleEnvironment.kt`.
- Não encontrei `BluetoothGatt`, `BluetoothDevice`, `BluetoothManager` ou `BluetoothLeScanner` em `feature/**`, `data/**` ou `core/model/**`.
- `ChampionConnectionManager` orquestra estados `Scanning -> Connecting -> DiscoveringServices -> ResolvingChannels -> ConfiguringSubscriptions -> TransportReady -> Disconnecting/Disconnected`, sem tratar `TransportReady` como handshake concluído.
- `ChampionGattQueue` usa actor por sessão, FIFO, timeout por operação, retry explícito e fechamento definitivo em disconnect.
- `ChampionUuidCatalog` centraliza canais obrigatórios `F008_NOTIFY` e `F008_WRITE`; mantém opcionais `F002_NOTIFY`, `F002_WRITE`, `FEA1_NOTIFY`, `FEA2_INDICATE` e `FEC8_INDICATE`.
- `ChampionProtocolLogger` registra apenas metadados seguros (`session_id`, canal, direção, `command_code`, `byte_count`, resultado e `error_code`) e envia bytes copiados apenas para sink diagnóstico privado.

### Escopo funcional confirmado

- Busca textual em `core/bluetooth/**` não encontrou implementação funcional de handshake `A1`, `D0`, `D1`, `D3` ou `D4`.
- Ocorrências restantes ligadas a canais catalogados (`FEA1`) ou documentação/nomenclatura, não a parser funcional ou fluxo de negócio.
- Não houve indício de dependência do núcleo BLE com Room, DataStore de produto, Health Connect, GPS ou UI funcional.

### Comandos executados

```text
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
./gradlew app:connectedDebugAndroidTest
git status --short
```

### Resultados

- `./gradlew lint testDebugUnitTest assembleDebug assembleRelease`: `BUILD SUCCESSFUL` em 41s após correção de permissões explícitas e compatibilidade API 33+/legado nos wrappers Android BLE.
- `./gradlew app:connectedDebugAndroidTest`: `BUILD SUCCESSFUL` em 54s; 16 testes concluídos em `SM-S901E - 16`, sem falhas.
- `git status --short`: apenas arquivos alterados desta correção/validação:
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidChampionBleScanner.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidGattFacade.kt`
  - `docs/tasks/004-native-ble-core/004-00-overview.md`
  - `docs/tasks/004-native-ble-core/004-15-validate-native-ble-core.md`

### Correção de lint consolidada

- `AndroidChampionBleScanner.kt`: acesso ao nome do dispositivo passou a usar leitura segura condicionada a `BLUETOOTH_CONNECT`.
- `AndroidGattFacade.kt`: operações `discoverServices`, `readCharacteristic`, `writeCharacteristic`, `writeDescriptor`, `setCharacteristicNotification`, `requestMtu`, `disconnect` e `close` agora validam permissão de conexão antes da chamada Android.
- `AndroidGattFacade.kt`: `writeCharacteristic` e `writeDescriptor` agora separam caminho API 33+ (`BluetoothStatusCodes.SUCCESS`) do caminho legado com APIs deprecated.

### Arquivos criados ou alterados da fase 04 revisados nesta validação

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/internal/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `docs/tasks/004-native-ble-core/004-00-overview.md`
- `docs/tasks/004-native-ble-core/004-15-validate-native-ble-core.md`

### ADRs que seguem pedindo decisão explícita

- serialização da fila GATT;
- ownership da sessão GATT;
- separação entre transporte e protocolo;
- multiplexação de notifications;
- política geral de timeout e retry;
- política de logging e captura diagnóstica privada.

### Conclusão

- Núcleo BLE está estruturalmente pronto para Fase 05 quanto a fronteiras, fila, state machine, capabilities, subscriptions e testes mínimos em dispositivo.
- Fase 04 fecha verde: checks automatizados pedidos nesta task passaram, sem bloqueio aberto remanescente.
- Não houve alteração em roadmap, ADRs ou docs de fases anteriores durante esta task.
- Nenhum commit ou push foi realizado.
