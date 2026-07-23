# Task 010-03 - Implementar protocolo D3, parser de índice e collector

## Status

Concluída em 2026-07-23.

## Objetivo

Implementar builder, parser, collector e timeout do índice D3, sem ainda tocar D4, parser do treino, Room ou UI final.

## Contexto

Fase 10 depende de um índice de CRCs confiável antes de qualquer D4. O app atual não possui protocolo D3. A task deve seguir estritamente a baseline auditada e o contrato da spec.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-02-define-watch-workout-import-contracts-state-and-diagnostic-policy.md`
- `docs/protocol/workouts-d3-d4.md`
- `docs/protocol/ble-transport.md`

## Dependências

- `010-02`.

## Entradas

- contrato D3 congelado;
- timeout de 10 segundos;
- regra de header plausível, CRC zero, ordem e deduplicação.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleTimeouts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD3WorkoutIndexProtocol.kt`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-03-implement-d3-index-protocol-and-collector.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD3WorkoutIndexProtocolTest.kt`

## Arquivos proibidos

- `data/**`
- `core/database/**`
- `feature/**`
- `ChampionD4*`

## Escopo

- builder `D3 01 00 00`;
- parser D3 com little-endian;
- header multipart plausível;
- suporte a resposta única sem header plausível;
- deduplicação de CRC repetido;
- timeout parcial com CRCs preservados.

## Fora do escopo

- D4;
- Room;
- UI;
- coordinator de produto.

## Passos

1. Criar componente único de protocolo D3.
2. Implementar parser e collector de índice com cópia defensiva.
3. Expor progresso indeterminado e resultado do índice.
4. Cobrir little-endian, zero, duplicado, single response, multipart e timeout parcial.
5. Atualizar spec e overview com nomes reais do protocolo.

## Critérios de aceite

- builder D3 exato;
- parser ignora CRC zero e deduplica repetidos;
- `blockIndex` sequencial não usa CRC como índice;
- multipart só vale com header plausível;
- timeout retorna CRCs coletados.

## Testes pequenos aplicáveis

- builder D3;
- little-endian;
- CRC zero ignorado;
- CRC duplicado;
- resposta sem header;
- multipart;
- timeout parcial.

## Validação mínima

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest --tests "*ChampionD3WorkoutIndexProtocolTest"`

## Evidências

- resultados dos testes pequenos;
- diff restrito a D3 e timeouts relacionados.

## Execução

- `ChampionBleTimeouts.kt` recebeu `d3WorkoutIndex = 10 segundos`.
- `ChampionD3WorkoutIndexProtocol.kt` foi criado com:
  - `createIndexCommand(...)` para `D3 01 00 00`;
  - parser `parsePayload(...)` e `parsePacket(...)`;
  - `D3IndexContext`, `D3IndexProgress`, `D3WorkoutBlockEntry`;
  - `IndexCollector` com deduplicação determinística, suporte a resposta única sem header plausível e timeout parcial.
- `ChampionD3WorkoutIndexProtocolTest.kt` cobre:
  - builder D3;
  - little-endian;
  - CRC zero ignorado;
  - CRC duplicado;
  - resposta sem header;
  - multipart;
  - timeout parcial.

## Atualização do overview

- marcar `010-03` como concluída;
- registrar impacto de conflito com `ChampionBleTimeouts.kt`.

Registrado: `ChampionBleTimeouts.kt` passou a concentrar timeout dedicado de D3 e permanece arquivo compartilhado com `010-04` e futuro coordinator.

## Validação final

- confirmar que D4, parser do treino e persistência não foram implementados nesta task.

Confirmado: esta task não implementou D4, parser do treino nem persistência.
