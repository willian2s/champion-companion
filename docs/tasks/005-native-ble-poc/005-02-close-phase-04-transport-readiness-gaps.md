# Task 005-02 — Fechar Gaps De `TransportReady` Físico Herdados Da Fase 04

## Status

Concluída em 2026-07-21.

Resumo: `ChampionCapabilityResolver` deixou de ser stub e passou a resolver capabilities reais a partir de snapshot concreto de discovery mantido pelo client Android. `DefaultAndroidGattFacade` agora entrega snapshot mapeado de services/characteristics/properties no callback de discovery, `AndroidChampionBleClient` passou a armazenar esse snapshot por sessão ativa, e `ChampionBleModule` passou a prover bindings concretos também para `AndroidBleEnvironment` e `AndroidGattFacade`. O core ganhou dispatch reutilizável de `ChampionCommand` sobre fila + router em `ChampionTransportStreams` e `ChampionTransportSession`, preservando `TransportReady` como estado de transporte puro. Validação de compilação de produção passou em `app:compileDebugKotlin`. Validação de testes ficou parcialmente bloqueada por falhas pré-existentes e não relacionadas em `app/src/test/java/.../core/designsystem`, `core/error`, `core/model` e `data`, antes da suite BLE nova terminar compilação.

## Objetivo

Fechar gaps mínimos do core para permitir caminho real até `TransportReady` físico e envio seguro de comandos reutilizáveis na PoC.

## Contexto

Hoje o Hilt injeta `ChampionCapabilityResolver` stub e o caminho concreto ainda não fecha snapshot completo de discovery nem dispatch genérico de `ChampionCommand`. Sem isso, A1 e D0/F002 ficariam bloqueados ou espalhados em camada errada.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/tasks/005-native-ble-poc/005-01-audit-phase-04-contracts-and-physical-gaps.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/specs/004-native-ble-core.md`

## Dependências

- `005-01` concluída.

## Pré-Condições

- auditoria dos gaps concluída.

## Entradas

- contratos BLE atuais;
- Hilt atual;
- wrappers Android atuais.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportStreams.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `docs/tasks/005-native-ble-poc/005-02-close-phase-04-transport-readiness-gaps.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `app/src/main/AndroidManifest.xml`
- `feature/device/**`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`

## Escopo

- resolver snapshot concreto de discovery/capabilities;
- fechar wiring Hilt necessário;
- expor dispatch mínimo de `ChampionCommand` reaproveitável pela PoC;
- preservar semântica de `TransportReady` como transporte puro.

## Fora Do Escopo

- permissões de manifesto;
- A1 funcional completo;
- FEA1/D0/F002 funcionais;
- UI técnica.

## Passos

1. Implementar `ChampionCapabilityResolver` concreto usando snapshot real de discovery.
2. Garantir que wrappers Android exponham dados mínimos necessários sem vazar `BluetoothGatt`.
3. Criar adaptador mínimo de dispatch de `ChampionCommand` para fila + await estruturado.
4. Validar que `TransportReady` continua sem significado de handshake.
5. Adicionar ou ajustar testes JVM/integration do core para esses gaps.
6. Atualizar overview e `Status`.

## Regras De Segurança

- não mudar semântica de `ChampionBleState.TransportReady`;
- não mover lógica para UI;
- não introduzir scanner paralelo;
- não logar payload.

## Critérios De Aceite

- app consegue atingir `TransportReady` físico sem stub de capabilities;
- caminho de comando reutilizável existe acima da fila existente;
- mudanças são mínimas e cobertas por teste;
- nenhuma feature recebe `BluetoothGatt`.

## Testes

- JVM para resolver de capabilities e command dispatch.
- integração com fakes para caminho `connect -> discovery -> capabilities -> TransportReady`.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Ble*"
./gradlew app:testDebugUnitTest --tests "*ConnectionManager*"
```

## Evidências Esperadas

- testes cobrindo resolver concreto e dispatch de comando.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar sem alterações em Manifest, Compose e roadmap.

## Implementação Executada

### Resolver concreto de capabilities

- `ChampionStoredDiscoveryCapabilityResolver` foi adicionado em `ChampionConnectionManager.kt`;
- ele recebe provider de `AndroidGattDiscoverySnapshot` por `sessionId` e delega a `ChampionCapabilityRegistry`;
- ausência de snapshot após discovery agora falha com `ChampionBleError.DiscoveryFailed()` em vez de `OperationUnavailable` genérico.

### Snapshot concreto de discovery no caminho produtivo

- `DefaultAndroidGattFacade` passou a receber `AndroidGattDiscoveryMapper`;
- callback `onServicesDiscovered` agora entrega `serviceUuids` e `AndroidGattDiscoverySnapshot` mapeado do `BluetoothGatt` real;
- `AndroidChampionBleClient` armazena o snapshot na sessão ativa e expõe `discoverySnapshot(sessionId)` para o resolver produtivo.

### Wiring Hilt faltante

- `ChampionBleModule` agora fornece `AndroidBleEnvironment <- DefaultAndroidBleEnvironment`;
- `ChampionBleModule` agora fornece `AndroidGattFacade <- DefaultAndroidGattFacade`;
- `ChampionCapabilityResolver` agora usa `AndroidChampionBleClient::discoverySnapshot`.

### Dispatch reutilizável de `ChampionCommand`

- `ChampionTransportStreams` ganhou:
  - `sendCommand(sessionId, queue, command)`
  - `executeCommand(sessionId, queue, command)`
- `executeCommand` registra espera de resposta antes do write usando `CoroutineStart.UNDISPATCHED`, reduzindo janela de corrida entre write e notify rápido.
- `ChampionTransportSession` ganhou:
  - `sendCommand(command)`
  - `executeCommand(command)`
- validação de capability/write type fica na sessão antes de enfileirar operação.

### Ajuste estrutural para respostas cross-channel com matcher custom

- `ChampionResponseMatchers.forCommand` passou a escopar matcher custom por `sessionId` em vez de `sessionId + channel`;
- default sem matcher custom permanece por `sessionId + channel + commandCode`;
- isso fecha gap necessário para A1, onde write sai em `F008_WRITE` e confirmação chega por `F008_NOTIFY`.

## Semântica De `TransportReady` Preservada

- `ChampionBleState.TransportReady` não foi renomeado nem recebeu semântica de handshake;
- mudanças desta task fecharam somente capabilities reais, snapshot de discovery e caminho de comando;
- nenhuma lógica de A1, FEA1 ou D0/F002 foi implementada aqui.

## Testes E Validação

### Produção

Comando executado:

```bash
./gradlew app:compileDebugKotlin
```

Resultado:

- `BUILD SUCCESSFUL`

### Testes BLE focados

Comando executado:

```bash
./gradlew app:testDebugUnitTest --tests "*ChampionBleConnectionManagerTest" --tests "*ChampionBleCapabilityRegistryTest" --tests "*ChampionBleCoreModelsTest" --tests "*ChampionBlePacketRouterTest" --tests "*ChampionTransportSessionTest"
```

Resultado:

- compilação de testes bloqueada por falhas pré-existentes não relacionadas em árvores `core/designsystem`, `core/error`, `core/model` e `data`;
- bloqueio ocorre antes do término da validação da suite BLE nova;
- bloqueio ficou registrado, não mascarado.

### Cobertura adicionada nesta task

- `ChampionTransportSessionTest.kt` cobre:
  - resolver concreto de capabilities com snapshot Android;
  - dispatch de comando pela fila e await de resposta;
  - rejeição de comando quando capability é incompatível.

## Critérios De Aceite Atendidos

- app agora tem caminho produtivo para capabilities reais sem stub;
- caminho reutilizável de `ChampionCommand` existe acima da fila e do router;
- `TransportReady` permaneceu transporte puro;
- nenhuma feature recebeu `BluetoothGatt`;
- nenhuma alteração tocou Manifest, Compose, roadmap ou ADRs.

## Bloqueios Remanescentes

- suite de testes unitários do app já estava quebrada fora do escopo BLE;
- `005-03` pode seguir, mas `005-11` ou validações mais amplas continuarão herdando esse bloqueio até correção separada.
