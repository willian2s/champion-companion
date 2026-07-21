# Task 004-08 — Implementar Discovery De Services e Registro De Capabilities

## Status

Concluida.

Resumo: implementados modelos puros de discovery/capabilities, mapper Android de `BluetoothGattService` para snapshot estreito e `ChampionCapabilityRegistry` para validar canais obrigatórios/opcionais e produzir snapshot imutável por sessão. FEA2, FEC8 e F002 write seguem opcionais e não bloqueiam `TransportReady`.

## Objetivo

Descobrir services/characteristics, validar obrigatórios/opcionais e produzir snapshot imutável de capabilities por sessão.

## Contexto

Discovery não pode ficar espalhado por `getService()` e `getCharacteristic()` em múltiplos componentes. Capabilities precisam ser centralizadas para `TransportReady` e subscriptions.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-04-create-champion-uuid-catalog-and-device-filter.md`
- `docs/tasks/004-native-ble-core/004-07-implement-gatt-callback-bridge-and-client.md`
- `docs/protocol/champion-c001.md`

## Dependências

- `004-04`, `004-06` e `004-07` concluídas.

## Entradas

- catálogo UUID;
- fila e client BLE.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionCapabilities.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionCapabilityRegistry.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidGattDiscoveryMapper.kt`
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `feature/**`
- `data/**`
- `AndroidManifest.xml`

## Escopo

- operation de discovery;
- validação de services/channels obrigatórios;
- separação obrigatório vs opcional;
- snapshot imutável de capabilities.

## Fora Do Escopo

- multiplexação completa de notifications;
- response routing;
- handshake.

## Regras De Segurança

- FEA2/FEC8/F002 write não bloqueiam `TransportReady`;
- propriedades incompatíveis devem falhar de forma tipada;
- nenhum `BluetoothGattService` sai da camada Android concreta.

## Passos De Implementação

1. Criar modelos puros de capability e channel.
2. Implementar mapeamento da discovery Android para snapshot puro.
3. Validar presença/propriedades de canais obrigatórios e opcionais.
4. Integrar resultado com fila e sessão.
5. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- capabilities da sessão são imutáveis e tipadas;
- canais obrigatórios falham de forma clara quando ausentes;
- opcionais degradam capability sem bloquear transporte.

## Testes

- JVM para capabilities obrigatórias/opcionais e propriedades incompatíveis.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Capability*"
```

## Validação Final

- confirmar que discovery não está espalhada em múltiplos componentes.

## Atualização Documental Esperada

- `Status` deste arquivo concluído.

## Implementacao Registrada

Arquivos criados:

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionCapabilities.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionCapabilityRegistry.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidGattDiscoveryMapper.kt`

Conclusoes:

- discovery passou a ter snapshot puro via `ChampionDiscoverySnapshot`, `ChampionDiscoveredService` e `ChampionDiscoveredCharacteristic`;
- `AndroidGattDiscoveryMapper` concentra leitura de `BluetoothGattService` e conversão de bitmask de properties para `ChampionChannelProperty`;
- `ChampionCapabilityRegistry` resolve capabilities por `sessionId` a partir do snapshot Android estreito, sem espalhar `getService()`/`getCharacteristic()` por múltiplos componentes;
- canais obrigatórios ausentes falham com `MissingRequiredService` ou `MissingRequiredChannel`;
- canal presente com properties incompatíveis falha com `IncompatibleChannelProperties`;
- canais opcionais ausentes apenas degradam capability, sem bloquear transporte;
- snapshot final mantém `ChampionCapabilitySnapshot` imutável, serviços descobertos, opcionais disponíveis e subscriptions padrão possíveis na sessão.

Limites mantidos:

- nenhum `BluetoothGattService` ou tipo Android de discovery sai de `core/bluetooth/android/**`;
- FEA2, FEC8 e F002 write não entram em `requiredTransportChannels`;
- multiplexação de notifications, handshake e response routing continuam fora desta task.

Integracao preparada:

- registry já aceita `sessionId` e snapshot vindo do mapper Android;
- resultado pode alimentar sessão/fila após discovery concreta sem retrabalho estrutural do modelo.

Validacoes executadas:

- `./gradlew app:compileDebugKotlin`: sucesso;
- `./gradlew app:testDebugUnitTest --tests "*Capability*"`: falha por ausência de testes com esse filtro nesta fase;
- discovery tipada ficou centralizada nos novos arquivos, sem espalhamento adicional em outros componentes.

Spec: nenhum ajuste factual necessario.
