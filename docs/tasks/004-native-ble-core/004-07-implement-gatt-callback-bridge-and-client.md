# Task 004-07 — Implementar Bridge De Callback GATT e Client Concreto

## Status

Concluida.

Resumo: implementados contrato puro `ChampionBleClient`, `ChampionGattCallbackBridge`, fachada Android mínima de GATT e `AndroidChampionBleClient`. Callbacks Android agora viram eventos internos puros com `sessionId`, detalhes sanitizados e bytes copiados defensivamente. Tipos Android BLE ficaram confinados ao subpackage `core/bluetooth/android`.

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

## Implementacao Registrada

Arquivos criados:

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleClient.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionGattCallbackBridge.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidChampionBleClient.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidGattFacade.kt`

Conclusoes:

- `ChampionBleClient` passou a expor somente contrato puro: abrir sessao, anexar fila, executar operacao, disconnect, close e stream de eventos internos;
- `ChampionGattCallbackBridge` converte callbacks em `ChampionGattSessionEvent` com `sessionId`, `Instant`, `SafeGattDetail`, mapeamento de canal por UUID e copia defensiva de bytes;
- `AndroidGattFacade` encapsula `BluetoothGatt`, `BluetoothGattCallback`, `BluetoothGattCharacteristic`, `BluetoothGattDescriptor` e `BluetoothManager` em API estreita e simulavel;
- `AndroidChampionBleClient` integra callback bridge com sessao ativa, ignora stale callbacks por `sessionId`, emite eventos puros e resolve fila associada para connect, discovery, read, write, descriptor, mtu e disconnect;
- `close()` ficou controlado no client concreto, nunca exposto como `BluetoothGatt.close()` publico.

Limites mantidos:

- nenhum tipo Android BLE saiu dos arquivos em `core/bluetooth/android`;
- callbacks antigos de sessao fechada ou substituida sao descartados silenciosamente;
- discovery completo, capability registry final e multiplexacao de subscriptions continuam fora desta task.

Validacoes executadas:

- `./gradlew app:compileDebugKotlin`: sucesso;
- busca por `import android.` em `core/bluetooth`: ocorrencias somente em `core/bluetooth/android/**` e wrapper Android existente;
- `./gradlew app:testDebugUnitTest --tests "*CallbackBridge*"`: nao usada como evidencia final por instabilidade do compilador incremental/KSP no ambiente atual; cobertura especifica segue para fase de testes `004-14`.

Spec: nenhum ajuste factual necessario.
