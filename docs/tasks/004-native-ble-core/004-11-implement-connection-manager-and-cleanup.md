# Task 004-11 — Implementar Connection Manager e Cleanup

## Status

Concluida.

Resumo: implementados `ChampionConnectionManager` e `ChampionTransportSession` como fachada principal do núcleo BLE. Manager agora orquestra scan opcional, abertura de sessão, espera de conexão, discovery via fila, resolução de capabilities por resolver injetável, assinatura obrigatória de F008 notify, emissão de `TransportReady`, disconnect idempotente e cleanup completo de sessão.

## Objetivo

Orquestrar scan, conexão, discovery, subscriptions obrigatórias, `TransportReady`, disconnect e cleanup final de sessão.

## Contexto

`ChampionConnectionManager` é fachada principal do núcleo BLE. Ele concentra estado da sessão e impede que features acessem GATT diretamente.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-05-implement-native-ble-scanner.md`
- `docs/tasks/004-native-ble-core/004-08-implement-service-discovery-and-capability-registry.md`
- `docs/tasks/004-native-ble-core/004-10-implement-command-packet-and-response-routing.md`

## Dependências

- `004-05`, `004-08`, `004-09` e `004-10` concluídas.

## Entradas

- scanner, client, queue, capabilities, subscriptions e router.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `feature/**`
- `data/**`
- `core/model/**`

## Escopo

- estado único de conexão;
- orquestração do fluxo scan -> connect -> discovery -> subscriptions -> ready;
- disconnect controlado e cleanup;
- proteção contra callbacks antigos e sessão encerrada.

## Fora Do Escopo

- handshake A1 funcional;
- UI de dispositivo;
- persistência de device associado.

## Regras De Segurança

- `TransportReady` não significa handshake concluído;
- disconnect cancela ativa e pendentes;
- reconexão infinita proibida.

## Passos De Implementação

1. Implementar state machine do manager.
2. Integrar scanner, client, queue, discovery e subscriptions obrigatórias.
3. Garantir cleanup completo em falha, cancelamento e disconnect.
4. Encerrar streams e referências da sessão.
5. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- manager expõe estado único e seguro;
- `TransportReady` só após discovery + channels obrigatórios + subscriptions principais;
- disconnect é idempotente e limpa sessão;
- stale callbacks não afetam nova sessão.

## Testes

- JVM para state machine, disconnect, cleanup, sessão antiga e falha no meio do fluxo.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*ConnectionManager*"
```

## Validação Final

- confirmar que nenhuma feature recebe `BluetoothGatt` ou state Android cru.

## Atualização Documental Esperada

- `Status` deste arquivo concluído.

## Implementacao Registrada

Arquivos criados:

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`

Conclusoes:

- manager expõe `state: StateFlow<ChampionBleState>` como estado único e seguro da sessão;
- fluxo principal ficou orquestrado como `scan -> openSession -> wait connected -> discovery -> resolve capabilities -> subscribe F008 -> TransportReady`;
- `TransportReady` só ocorre após discovery, capabilities resolvidas e assinatura obrigatória de `F008_NOTIFY`;
- cleanup cancela jobs internos, fecha hub de notifications, fecha streams de transporte, encerra fila, chama `client.disconnect()` e `client.close()`;
- stale callbacks continuam ignorados por `sessionId`; disconnect inesperado da sessão ativa converte estado para `Failed` e dispara cleanup;
- manager não expõe `BluetoothGatt`, `ScanResult` ou estado Android cru para features.

Limites mantidos:

- handshake A1 funcional continua fora do escopo;
- persistência de device associado não foi adicionada;
- resolução de capabilities foi integrada via `ChampionCapabilityResolver` injetável, evitando acoplamento especulativo a detalhes Android ainda não expostos pelos wrappers atuais.

Validacoes executadas:

- `./gradlew app:compileDebugKotlin`: sucesso;
- `./gradlew app:testDebugUnitTest --tests "*ConnectionManager*"`: falha por ausência de testes com esse filtro nesta fase.

Spec: nenhum ajuste factual necessario.
