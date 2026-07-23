# Task 010-04 - Implementar collector D4 por bloco e remontagem

## Status

Pendente.

## Objetivo

Implementar builder D4, collector por bloco, ordenação, duplicados, índices ausentes e remontagem sem ainda interpretar métricas do treino.

## Contexto

O app atual não possui protocolo D4. A coleta precisa ser sequencial, por bloco, com timeout de 15 segundos, limpeza de observer e cálculo explícito de ausências.

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

- contrato D4 congelado;
- header little-endian;
- regra de conclusão por `packetIndex >= totalPackets`.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleTimeouts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutBlockProtocol.kt`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-04-implement-d4-block-collector-and-remounting.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutBlockProtocolTest.kt`

## Arquivos proibidos

- `ChampionD3*`
- `core/database/**`
- `data/**`
- `feature/**`

## Escopo

- builder `D4 <blockIndex> 00 00`;
- parser do header D4;
- collector com ordenação, duplicados e ausentes;
- timeout por bloco;
- remontagem pronta para o parser D4.

## Fora do escopo

- interpretar pacote 1/2/3/4+;
- persistir;
- UI.

## Passos

1. Criar protocolo D4 por bloco com timeout próprio.
2. Implementar leitura do header little-endian.
3. Aceitar fora de ordem, rastrear duplicados e calcular ausentes.
4. Garantir limpeza de observer em todos os caminhos.
5. Cobrir comportamento por testes pequenos.

## Critérios de aceite

- builder D4 exato;
- conclusão por `totalPackets > 0 && packetIndex >= totalPackets`;
- fora de ordem aceito;
- duplicados e ausentes relatados;
- timeout por bloco respeitado sem retry interno.

## Testes pequenos aplicáveis

- builder D4;
- header little-endian;
- fora de ordem;
- duplicados;
- ausentes;
- timeout por bloco.

## Validação mínima

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest --tests "*ChampionD4WorkoutBlockProtocolTest"`

## Evidências

- resultados dos testes pequenos;
- diff focado no collector D4.

## Atualização do overview

- marcar `010-04` como concluída;
- registrar conflitos ou decisões necessárias para `010-05`.

## Validação final

- confirmar que parser do treino, Room e UI continuam fora desta task.
