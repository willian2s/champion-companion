# Task 004-07 — Implementar Bridge De Callback GATT e Client Concreto

## Status

Pendente.

## Objetivo

Criar client BLE concreto e bridge que converte callbacks Android em eventos internos de sessão seguros e serializáveis.

## Contexto

Somente implementação BLE concreta pode tocar `BluetoothGatt` e `BluetoothGattCallback`. Todo callback deve virar evento interno com `sessionId`.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-06-implement-gatt-operation-model-and-serial-queue.md`
- `docs/protocol/ble-transport.md`

## Dependências

- `004-06` concluída.

## Entradas

- modelo de operação e sessão;
- catálogo UUID.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleClient.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionGattCallbackBridge.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidChampionBleClient.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidGattFacade.kt`
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `feature/**`
- `data/**`
- `core/model/**`

## Escopo

- abertura/encerramento de sessão GATT;
- bridge de callbacks para eventos internos;
- wrappers estreitos para operações Android difíceis de simular;
- descarte de callbacks de sessão antiga.

## Fora Do Escopo

- connection manager final;
- discovery e capabilities completos;
- subscriptions multiplexadas finais.

## Regras De Segurança

- nenhum tipo Android BLE sai da implementação concreta;
- callback antigo não conclui operação nova;
- `close()` controlado, nunca livremente exposto.

## Passos De Implementação

1. Criar interface pura do client BLE.
2. Implementar fachada Android mínima para `BluetoothGatt` e callbacks.
3. Converter callbacks em eventos com `sessionId`.
4. Integrar com fila e ignorar stale callbacks.
5. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- tipos Android GATT confinados à implementação concreta;
- callbacks viram eventos internos puros;
- sessão antiga é ignorada;
- client suporta disconnect e close controlados.

## Testes

- JVM para stale callback, callback duplicado e conclusão única com doubles;
- instrumentado só se wrapper Android exigir.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*CallbackBridge*"
```

## Validação Final

- confirmar ausência de import Android BLE nos contratos públicos.

## Atualização Documental Esperada

- `Status` deste arquivo concluído.
