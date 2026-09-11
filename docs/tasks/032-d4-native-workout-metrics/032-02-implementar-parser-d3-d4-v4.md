# 032-02 — Implementar parser D3/D4 v4

- **Status:** completed
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

## Execução

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD3WorkoutIndexProtocol.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutParser.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutD4V4.kt`

### Decisões e desvios

- D3 agora retém respostas por `packetIndex`, ordena candidatos antes de atribuir
  `blockIndex` físico e avança sobre CRC zero; CRC repetido é deduplicado sem
  renumerar o slot físico.
- D4 v4 reconstrói header de 42 bytes e preserva header, samples e checks de
  consistência em modelo semanticamente nomeado.
- BPM fora da faixa vira ausência somente de BPM. Passos, `sportValue`, calorias,
  distância e pausa permanecem preservados.
- T/N é aplicado somente para série completa e contígua cujo `recordCount` fecha;
  séries parciais mantêm grade nominal e estado `PARTIAL`.
- `d4-v4` foi adicionado como contrato interno, sem inclusão no parser efetivo
  usado pelo coordenador; Room, derivação, UI e Health Connect não foram tocados.

### Comandos executados

- `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin app:lintDebug`
- `./gradlew lint assembleDebug assembleRelease`
- `./gradlew :app:testDebugUnitTest --tests '*ChampionD3WorkoutIndexProtocolTest' --tests '*ChampionD4WorkoutParserTest'`
- `git diff --check`

### Resultados e evidências

- Compilação Kotlin debug/release: passou.
- `lint`, `assembleDebug` e `assembleRelease`: passaram.
- `git diff --check`: passou.
- Testes focados: 9 falhas em 24. Testes existentes usam fixtures preferidas
  incompatíveis com parser legacy `d4-v1`; teste D3 ainda espera índices
  comprimidos `[0, 1]`, enquanto contrato corrigido produz slots físicos `[1, 3]`.
  Nenhum teste foi alterado, conforme regra do repositório.
- Revisão independente final: aprovada, sem achados bloqueadores ou altos.

### Riscos residuais

- Fixtures de teste legacy precisam manutenção futura quando alteração de testes
  for autorizada.
- Variações de firmware e validação física permanecem sob responsabilidade de
  `032-08`.
