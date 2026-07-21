# Task 004-02 — Definir Contratos BLE, Estados e Erros

## Status

Concluida.

Resumo: package `core/bluetooth` criado com contratos puros para sessao efemera, dispositivo descoberto, aliases de service/canal, capabilities, estados explicitos, erros seguros, envelopes imutaveis de comando/pacote e interface publica unica de transporte. Nenhum contrato publico importa Android BLE. `TransportReady` segue separado de handshake. Validacao: `app:compileDebugKotlin` OK; comando `app:testDebugUnitTest --tests "*Ble*"` ainda falha por nao existirem testes BLE nesta fase, cobertura fica preparada para `004-14`.

## Objetivo

Criar contratos puros do núcleo BLE, incluindo estados de conexão, erros de transporte, envelopes e identidade de sessão.

## Contexto

Sem contratos estáveis, scanner, fila, client e connection manager tendem a vazar tipos Android ou misturar transporte com protocolo.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/specs/_inputs/phase-04-native-ble-core-direction.md`
- `docs/tasks/004-native-ble-core/004-01-audit-ble-platform-and-protocol-boundaries.md`
- `docs/protocol/champion-c001.md`

## Dependências

- `004-01` concluída.

## Entradas

- contratos existentes em `core/model`;
- `core/common/UiStateStore.kt`;
- logging seguro atual.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleState.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleError.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionPacket.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionCommand.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionSession.kt`
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `core/model/**`
- `feature/**`
- `data/**`
- `AndroidManifest.xml`

## Escopo

- definir sessão efêmera;
- definir estado explícito de conexão;
- definir modelo de erro seguro;
- definir envelopes imutáveis de comando e pacote;
- definir aliases de canal e contratos públicos do núcleo BLE.

## Fora Do Escopo

- scanner concreto;
- fila concreta;
- acesso Android BLE;
- parser funcional A1/D0/D1/D3/D4.

## Regras De Segurança

- não importar `android.bluetooth.*`;
- `TransportReady` não representa handshake concluído;
- payloads devem ser copiados defensivamente nas bordas do contrato.

## Passos De Implementação

1. Criar package `core/bluetooth` se ausente.
2. Definir tipos puros para sessão, canal, estado, erro, operação base e envelopes.
3. Garantir que nenhum contrato exponha `BluetoothGatt` ou classes Android BLE.
4. Preparar tipos para timeout, cancelamento e retry explícito.
5. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- contratos compilam sem dependência Android BLE;
- estados cobrem scan, conexão, discovery, subscriptions, ready, disconnect e falha;
- erros distinguem pré-requisito, timeout, callback de falha, stale session e cleanup;
- `ChampionCommand` e `ChampionPacket` são imutáveis.

## Testes

- adicionar ou preparar JVM tests de estado, igualdade, cópia defensiva e mapeamento de erros.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Ble*"
```

## Validação Final

- confirmar ausência de imports Android BLE em `core/bluetooth` público.

## Atualização Documental Esperada

- `Status` deste arquivo concluído;
- eventual ajuste factual mínimo na spec, se necessário.

## Implementacao Registrada

Arquivos criados:

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleState.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleError.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionPacket.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionCommand.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionSession.kt`

Conclusoes:

- sessao BLE agora possui identidade efemera tipada por `ChampionSessionId` e `ChampionSession`;
- estado de conexao cobre `Disconnected`, `Scanning`, `Connecting`, `DiscoveringServices`, `ResolvingChannels`, `ConfiguringSubscriptions`, `TransportReady`, `Disconnecting` e `Failed`;
- erros distinguem pre-requisito, timeout, callback de falha, stale session, subscription, cleanup e encerramento de fila;
- `ChampionCommand` e `ChampionPacket` copiam `ByteArray` defensivamente e nao expõem referencia mutavel compartilhada;
- contrato publico `ChampionBleTransport` expoe somente tipos puros, `Flow`/`StateFlow` e resultados tipados;
- aliases F008, F002, FEA1, FEA2 e FEC8 ficaram centralizados sem introduzir parser funcional ou acesso Android.

Validacoes executadas:

- `grep "android\.bluetooth" core/bluetooth`: sem imports encontrados;
- `./gradlew app:compileDebugKotlin`: sucesso;
- `./gradlew app:testDebugUnitTest --tests "*Ble*"`: falha esperada por ausencia de testes BLE nesta task e restricao de arquivos permitidos.

Spec: nenhum ajuste factual necessario.
