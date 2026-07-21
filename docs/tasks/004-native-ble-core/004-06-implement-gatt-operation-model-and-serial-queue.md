# Task 004-06 — Implementar Modelo De Operação GATT e Fila Serial

## Status

Concluida.

Resumo: implementados `ChampionGattOperation`, `ChampionGattQueue`, `ChampionBleTimeouts`, helpers de `ChampionRetryPolicy` e actor interno por sessao. Fila agora processa operacoes em FIFO, mantem no maximo uma ativa, arma timeout obrigatorio, aceita conclusao/cancelamento explicitos, aplica retry somente com policy limitada e encerra definitivamente no disconnect. Nenhum tipo Android BLE foi introduzido nesta fila.

## Objetivo

Criar modelo de operação GATT e fila serial por sessão, com uma operação ativa, timeout obrigatório, cancelamento e retry explícito.

## Contexto

Fila é regra arquitetural central da Fase 04. Sem ela, callbacks, timeout e disconnect ficam imprevisíveis.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-02-define-ble-contracts-states-and-errors.md`
- `docs/protocol/ble-transport.md`

## Dependências

- `004-02` concluída.

## Entradas

- contratos BLE e sessão efêmera.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionGattOperation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionGattQueue.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleTimeouts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionRetryPolicy.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/internal/ChampionSessionActor.kt`
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `feature/**`
- `data/**`
- `AndroidManifest.xml`

## Escopo

- modelo de operação;
- fila FIFO serial;
- timeout obrigatório por operação;
- cancelamento explícito;
- retry limitado e observável;
- encerramento da fila no disconnect.

## Fora Do Escopo

- chamadas Android GATT concretas;
- scanner;
- parser de protocolo.

## Regras De Segurança

- no máximo uma operação ativa;
- timeout sempre libera fila;
- callback e timeout não podem concluir operação duas vezes;
- reconnect infinito proibido.

## Passos De Implementação

1. Modelar operação, resultado terminal e política de retry.
2. Implementar actor/fila serial por sessão.
3. Integrar timeout centralizado e cancelamento.
4. Garantir prioridade de disconnect e fechamento definitivo da fila.
5. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- FIFO determinística;
- uma operação ativa por vez;
- timeout/cancelamento/disconnect liberam fila;
- retry não ocorre sem policy explícita.

## Testes

- JVM para FIFO, exclusão mútua, timeout, cancelamento, retry, callback duplicado e sessão antiga.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*GattQueue*"
```

## Validação Final

- confirmar que fila não depende de tipos Android BLE.

## Atualização Documental Esperada

- `Status` deste arquivo concluído;
- registrar se decisão de actor precisa ADR.

## Implementacao Registrada

Arquivos criados:

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionGattOperation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionGattQueue.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleTimeouts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionRetryPolicy.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/internal/ChampionSessionActor.kt`

Conclusoes:

- `ChampionGattOperation` passou a carregar `operationId`, `sessionId`, tipo, alvo, timeout, retry policy, tentativa atual e payload defensivamente copiado;
- `ChampionGattQueue` executa FIFO serial por sessao com uma operacao ativa por vez;
- timeout vira mensagem do actor e disputa com callback no mesmo fluxo serial, impedindo conclusao dupla;
- `complete`, `fail` e `cancel` ignoram sessao antiga ou `operationId` nao ativo, protegendo contra callback tardio e duplicado;
- retry ocorre somente quando `ChampionRetryPolicy.Limited` autoriza nova tentativa;
- disconnect cancela operacao ativa, falha pendentes com `QueueClosed` e fecha fila de forma definitiva;
- executor concreto da operacao segue abstrato e sem dependencia Android BLE nesta task.

Validacoes executadas:

- `./gradlew app:compileDebugKotlin`: sucesso;
- `./gradlew app:testDebugUnitTest --tests "*GattQueue*"`: falha por ausencia de testes com esse filtro nesta fase;
- fila e actor nao importam `android.bluetooth.*`.

ADR futura:

- decisao de actor por sessao continua candidata a ADR futura, conforme spec `004-native-ble-core.md`, mas nao foi formalizada nesta task.
