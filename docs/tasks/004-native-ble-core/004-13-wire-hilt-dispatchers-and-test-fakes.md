# Task 004-13 — Ligar Hilt, Dispatcher BLE e Fakes De Teste

## Status

Pendente.

## Objetivo

Registrar bindings Hilt do núcleo BLE, dispatcher dedicado e doubles/fakes necessários para testes JVM e instrumentados mínimos.

## Contexto

Sem DI e doubles, núcleo BLE vira difícil de testar, fácil de acoplar e propenso a singletons globais de GATT.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-05-implement-native-ble-scanner.md`
- `docs/tasks/004-native-ble-core/004-11-implement-connection-manager-and-cleanup.md`

## Dependências

- `004-05` a `004-12` concluídas.

## Entradas

- módulos Hilt atuais de `core/common` e `core/logging`.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/common/CoroutineDispatchersModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleModule.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/fakes/FakeChampionBleScanner.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/fakes/FakeChampionBleClient.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/fakes/FakeMonotonicClock.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/fakes/RecordedChampionPacketSource.kt`
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `feature/**`
- `data/**`
- `AndroidManifest.xml`

## Escopo

- qualifier para dispatcher BLE;
- bindings Hilt do scanner/client/queue/manager/logger;
- fakes estreitos para testes.

## Fora Do Escopo

- novos módulos Gradle;
- singletons globais de `BluetoothGatt`;
- testes físicos.

## Regras De Segurança

- Hilt não pode expor GATT cru;
- dispatcher BLE dedicado não substitui IO global sem necessidade;
- fakes não devem reproduzir toda API Android Bluetooth.

## Passos De Implementação

1. Adicionar qualifier do dispatcher BLE.
2. Criar módulo Hilt do núcleo BLE.
3. Criar fakes estreitos para scanner, client, clock e packets.
4. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- componentes principais são injetáveis;
- dispatcher BLE é separado;
- testes conseguem isolar fila, manager e scanner com fakes.

## Testes

- JVM para bindings simples e uso dos fakes em cenários de fila/manager.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Fake*"
```

## Validação Final

- confirmar ausência de singleton global livre de `BluetoothGatt`.

## Atualização Documental Esperada

- `Status` deste arquivo concluído.
