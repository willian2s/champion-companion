# 032-01 — Auditar e congelar layout D4 v4

- **Status:** completed
- **Depende de:** nenhuma
- **Libera:** 032-02 e 032-03
- **Auditoria:** `docs/integration/032-01-d4-v4-layout-and-consumer-audit.md`

## Objetivo

Consolidar em documento autoritativo o layout D3/D4 observado no APK, os dados
físicos validados e as fronteiras entre fato, derivação e diagnóstico antes de
mudar código.

## Entregáveis

- [x] atualizar `docs/protocol/workouts-d3-d4.md` com header de 42 bytes e sample v4;
- [x] registrar sem payload integral a atividade física/evidência agregada de referência e invariantes;
- [x] congelar unidades confirmadas, candidatas e desconhecidas;
- [x] congelar regra de índice físico D3;
- [x] congelar que `T/N` permanece autoritativo para timeline efetiva;
- [x] mapear consumidores de `rawMotionValue`, `rawDistanceValue`,
  `rawCaloriesValue`, `decodedSteps`, distância canônica e calibração;
- [x] alinhar a Spec 032 às decisões fechadas nesta auditoria.

## Evidência auditada

A inspeção local do APK confirmou o layout fixo do Sport Model por
`SportModelHander`, `SportModelOriginBean` e `SportModelHeadBean`.

A cópia local do banco fornecida para investigação contém 34 blocos D4 completos
com payload diagnóstico suficiente para remontar header e samples. Sem versionar
payloads ou identificadores, a auditoria verificou:

```text
recordCount == quantidade de samples            34/34
header.totalSteps == Σ sample.steps              34/34
header.totalDistance == Σ sample.distance        34/34
header.totalCaloriesRaw == Σ sample.caloriesRaw  34/34
header.totalSportCount == Σ sample.sportValue    34/34
header.crc == CRC associado ao bloco             34/34
```

## Decisões fechadas

### Sample fixo v4

```text
byte  6      BPM raw
bytes 7..8   sportValue u16 LE
bytes 9..10  steps u16 LE
bytes 11..12 caloriesRaw u16 LE
bytes 13..14 distanceMeters u16 LE
byte  15     pauseStateRaw
```

`rawMotionValue = u24(bytes[7..9])` não representa um packing definido pelo
protocolo. O `decodedSteps = rawMotionValue >>> 16` histórico só reproduz o byte
baixo de `steps` quando o byte alto é zero.

### Header

Os três primeiros pacotes fornecem 42 bytes por concatenação de
`packet[6..19]`. O layout completo e os offsets estão congelados no documento de
auditoria e em `docs/protocol/workouts-d3-d4.md`, incluindo timestamps,
agregados u32, `recordCount`, pausas, CRC e tipo raw.

### Distância

**Confirmada para o layout fixo observado:** sample e header usam metros.

Justificativas combinadas:

- `SportModelHeadBean` divide distância por `1000` para exibir quilômetros;
- `SportModelOriginBean.getSpeed()` usa a distância do sample como metros;
- soma dos samples fecha no header em 34/34 blocos.

`032-02` pode promover diretamente o sample para `watchDistanceMeters` ou nome
equivalente. `032-05` decide o papel dessa distância na derivação canônica.

### Calorias

**Escala confirmada para o layout fixo observado:** `raw / 1000.0 = kcal`.

O header usa essa conversão no APK e a soma dos samples fecha no raw do header em
34/34 blocos. O raw deve continuar persistido para provenance.

### `sportValue`

Estrutura e soma contra header confirmadas. Unidade/significado fisiológico
permanecem desconhecidos. Persistir como raw sem inferir intensidade, MET ou
energia.

### Pausa

Header e sample possuem campos nomeados de pausa no APK, mas os 34 headers
observados têm `pauseCount = 0` e `pauseTime = 0`. Persistir raw; não congelar enum
nem alterar timeline com base no byte do sample nesta task.

### Índice D3

Slot físico avança inclusive sobre CRC zero. Ignorar CRC zero não pode comprimir
índices posteriores. CRC repetido pode ser deduplicado logicamente sem renumerar
o slot usado no comando D4.

### Timeline

A Spec 021 permanece autoritativa. `T` e `N` são independentes; nos 34 blocos a
razão `duration/sampleCount` variou aproximadamente de `48,43 s` a `62,80 s`.
Logo, `60 s` continua apenas nominal/proveniência e `T/N` permanece a aproximação
efetiva consistente.

### Divergência header × samples

- nunca corrigir sample para fechar header;
- nunca substituir série por total;
- preservar os dois lados;
- bloco completo divergente deve ser pelo menos `PARTIAL`;
- distância nativa falha fechada para derivação se sua soma não fechar no header;
- falha estrutural de layout pode permanecer `INVALID`.

### Persistência mínima

Por intervalo v4:

```text
intervalIndex / sourceSampleIndex
decodedSteps
sportValueRaw
watchDistanceMeters
watchCaloriesRaw
pauseStateRaw
BPM/sourceSampleIndex existente
```

Por import/header:

```text
reportedTotalSteps
reportedTotalDistanceMeters
reportedTotalCaloriesRaw
reportedTotalSportValue
reportedRecordCount
reportedPauseCount
reportedPauseTime
reportedSportTypeRaw
reportedCrc
```

`rawMotionValue`, `rawDistanceValue` e `rawCaloriesValue` mantêm o significado
legacy de `d4-v1/v2/v3`; não recebem novo significado por conveniência.

## Fechamento

O contrato foi sincronizado em:

- `docs/integration/032-01-d4-v4-layout-and-consumer-audit.md`;
- `docs/protocol/workouts-d3-d4.md`;
- `docs/specs/032-d4-native-workout-metrics.md`;
- `docs/tasks/032-d4-native-workout-metrics/032-00-overview.md`.

`032-02` e `032-03` estão liberadas para execução paralela depois que a base local
for atualizada para a branch 032.

## Validação

A task alterou somente documentação e não exige build. A sessão atual acessa o
repositório pelo conector GitHub e não possui checkout local montado, portanto o
comando literal `git diff --check` não pôde ser executado aqui. A comparação
`main..feat/032-d4-native-workout-metrics` foi inspecionada e não mostrou arquivos
de runtime modificados. Executar `git diff --check` no checkout local como
preflight antes de iniciar as mudanças de código de `032-02/032-03`.
