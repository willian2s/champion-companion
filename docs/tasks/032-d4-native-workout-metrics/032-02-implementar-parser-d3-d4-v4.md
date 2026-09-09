# 032-02 — Implementar parser D3/D4 v4

- **Status:** pending
- **Depende de:** 032-01
- **Pode rodar em paralelo com:** 032-03

## Objetivo

Implementar parsing fiel ao layout fixo Sport Model sem publicar ainda `d4-v4`
como runtime efetivo.

## Escopo

- corrigir D3 para preservar slot físico com CRC zero/repetido;
- reconstruir header D4 completo dos três primeiros pacotes;
- criar modelo interno v4 para header e samples;
- parsear `sportValue`, `steps`, `caloriesRaw`, `distanceRaw`, `pauseState`;
- manter BPM inválido como ausência apenas de BPM;
- preservar coleta/ordenação/timeout do bloco;
- produzir adapters temporários somente quando necessários para compilar.

## Arquivos prioritários

- `core/bluetooth/ChampionD3WorkoutIndexProtocol.kt`
- `core/bluetooth/ChampionD4WorkoutParser.kt`
- contratos de parser em `core/model`
- `docs/protocol/workouts-d3-d4.md`

## Fora do escopo

- Room migration;
- mudar distância canônica;
- calibração;
- UI;
- publicar `d4-v4` como default antes de 032-07.

## Validação

`./gradlew lint assembleDebug assembleRelease` e `git diff --check`.
