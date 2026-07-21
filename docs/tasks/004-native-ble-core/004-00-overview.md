# Task 004-00 — Overview Núcleo BLE Nativo

## Status

Pendente.

## Objetivo

Controlar ordem, dependências, checklist e riscos de conflito da Fase 04.

## Documentos Autoritativos

- `AGENTS.md`
- `docs/core/current-project-baseline.md`
- `docs/specs/_inputs/phase-04-native-ble-core-direction.md`
- `docs/specs/004-native-ble-core.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`
- `docs/protocol/history-d1.md`
- `docs/protocol/workouts-d3-d4.md`
- `docs/protocol/diagnostics-d8-a8.md`
- `docs/baseline/gaps-and-open-questions.md`
- `docs/baseline/source-map.md`
- `docs/baseline/feature-parity-checklist.md`

## Status Inicial

- App Android já possui Hilt, Room 3, DataStore, Compose e testes base.
- Não existe `core/bluetooth`.
- Não existe uso de tipos Android GATT no código atual.
- `feature/device` ainda é placeholder.

## Estado Inicial Esperado

- worktree pode estar suja; preservar mudanças alheias;
- roadmap, ADRs, fases concluídas e input da fase intactos;
- sem código BLE Kotlin prévio em `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth`.

## Limites Da Fase

- Planejar e implementar núcleo BLE reutilizável e independente da UI.
- Não validar fisicamente Champion C001 nesta fase.
- Não implementar handshake A1 funcional, D0, D1, D3, D4, Health Connect, GPS, background BLE ou auto-connect.

## Lista Completa Das Tasks

1. `004-00-overview.md`
2. `004-01-audit-ble-platform-and-protocol-boundaries.md`
3. `004-02-define-ble-contracts-states-and-errors.md`
4. `004-03-prepare-permissions-and-bluetooth-prerequisites.md`
5. `004-04-create-champion-uuid-catalog-and-device-filter.md`
6. `004-05-implement-native-ble-scanner.md`
7. `004-06-implement-gatt-operation-model-and-serial-queue.md`
8. `004-07-implement-gatt-callback-bridge-and-client.md`
9. `004-08-implement-service-discovery-and-capability-registry.md`
10. `004-09-implement-notification-and-indication-multiplexing.md`
11. `004-10-implement-command-packet-and-response-routing.md`
12. `004-11-implement-connection-manager-and-cleanup.md`
13. `004-12-implement-safe-protocol-logging.md`
14. `004-13-wire-hilt-dispatchers-and-test-fakes.md`
15. `004-14-add-ble-core-tests.md`
16. `004-15-validate-native-ble-core.md`

## Ordem Recomendada

1. `004-01`
2. `004-02`
3. `004-03` e `004-04`
4. `004-06`
5. `004-07`
6. `004-05`
7. `004-08`
8. `004-09`
9. `004-10`
10. `004-11`
11. `004-12`
12. `004-13`
13. `004-14`
14. `004-15`

## Dependências

- `004-00` deve ser lida antes de qualquer outra task.
- `004-01` antes de decisões de pacote/nome/escopo.
- `004-02` antes de scanner, fila, client ou manager.
- `004-03` e `004-04` antes de scanner e connection manager.
- `004-06` antes de qualquer operação concreta de client.
- `004-07` antes de discovery, subscriptions e roteamento.
- `004-08` antes de `004-09` e `004-11`.
- `004-10` antes de `004-11` e `004-12`.
- `004-13` após contratos e componentes principais estabilizarem.
- `004-15` é última.

## Tasks Paralelizáveis

- `004-03` e `004-04`.
- `004-12` pode correr em paralelo com `004-11` após `004-10`.
- `004-14` pode dividir testes JVM e instrumentados em arquivos diferentes após `004-13`.

## Arquivos Compartilhados Com Risco De Conflito

- `docs/specs/004-native-ble-core.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionGattQueue.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleClient.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleModule.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`

## Checklist

- [x] `004-01` fronteiras BLE e lacunas físicas auditadas
- [x] `004-02` contratos, estados e erros definidos
- [x] `004-03` permissões e pré-requisitos tipados implementados
- [x] `004-04` catálogo UUID e filtro Champion centralizados
- [x] `004-05` scanner nativo com timeout/cancelamento criado
- [x] `004-06` fila GATT serial criada
- [x] `004-07` callback bridge e client concreto criados
- [x] `004-08` discovery e capability registry criados
- [x] `004-09` notifications, indications e CCCD multiplexados
- [x] `004-10` command/packet envelope e response routing criados
- [ ] `004-11` connection manager, state machine e cleanup criados
- [ ] `004-12` logging seguro implementado
- [ ] `004-13` Hilt, dispatcher BLE e fakes ligados
- [ ] `004-14` testes BLE adicionados
- [ ] `004-15` validação final concluída

## Artefatos Esperados

- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-00-overview.md`
- `docs/tasks/004-native-ble-core/004-01-*.md` a `004-15-*.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/androidTest/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`, se realmente necessário

## Comandos De Validação

```bash
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
./gradlew app:testDebugUnitTest --tests "*Ble*"
./gradlew app:connectedDebugAndroidTest
```

## Critérios Gerais De Conclusão

- Fronteira Android BLE única criada.
- Nenhuma feature ou domínio recebe `BluetoothGatt`.
- Fila serial por sessão validada por testes.
- Scan e disconnect têm timeout/cancelamento/cleanup previsíveis.
- `TransportReady` separado de handshake.
- Logs não contêm bytes brutos, hex, MAC ou dados de saúde.
- Fase 05 pode iniciar PoC física sem retrabalho estrutural da Fase 04.

## Instruções Para Atualização Do Checklist

- Cada task altera somente próprio `Status`.
- Ao concluir qualquer task da fase, atualizar no mesmo trabalho o item correspondente neste overview.
- `004-15` consolida este checklist e registra evidências finais.
- Ao concluir task, atualizar item correspondente para `[x]` e registrar bloqueios reproduzíveis se houver.
