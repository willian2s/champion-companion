# Task 004-06 — Implementar Modelo De Operação GATT e Fila Serial

## Status

Pendente.

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
