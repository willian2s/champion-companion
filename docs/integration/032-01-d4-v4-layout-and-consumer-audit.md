# 032-01 — Auditoria do layout D4 v4 e consumidores

- **Task:** `032-01`
- **Spec:** `docs/specs/032-d4-native-workout-metrics.md`
- **Branch auditada:** `feat/032-d4-native-workout-metrics`
- **Resultado:** contrato suficiente para liberar `032-02` e `032-03`
- **Mudança de runtime nesta task:** nenhuma

## Objetivo

Congelar o contrato factual necessário para substituir a interpretação histórica
`d4-v1/v2/v3` por um parser `d4-v4` fiel ao layout fixo do Sport Model observado
no Champion C001, sem misturar:

```text
SOURCE     bytes e fatos recebidos do relógio
MEASURED   unidades que o APK e a evidência física permitem promover
DERIVED    timeline, distância corrigida, pace, velocidade e cadência
PROJECTION payload congelado para consumidores/Health Connect
```

A task não implementa parser, migration, derivação ou UI.

## Fontes usadas

### APK Champion Smartstyle decompilado

Arquivos auditados localmente, sem versionar o arquivo decompilado:

- `com/veepoo/hband/ble/readmanager/SportModelHander.java`;
- `com/veepoo/hband/modle/SportModelOriginBean.java`;
- `com/veepoo/hband/modle/SportModelHeadBean.java`.

Métodos relevantes:

- `getCrcReadCmd()`;
- `handlerSportModelCrc(...)`;
- `readSportModelDataNextDay()`;
- `readSportModelDataByDay(...)`;
- `handlerOneBlockModelData()`;
- `handlerSportModelOriginBean(...)`;
- `handlerSportHeader(...)`;
- `SportModelOriginBean.getSpeed()`;
- `SportModelHeadBean.getDistanceKmStr()`;
- `SportModelHeadBean.getKcalStr()/getKCal()`;
- `SportModelHeadBean.setSportTime()`.

### Banco local fornecido para investigação

A cópia local foi usada somente para inspeção. Nenhum identificador, payload
integral ou registro individual foi copiado para este documento.

Foram encontrados **34 blocos D4 completos** com payload diagnóstico local
suficiente para reconstruir header e samples do layout fixo. Para esses 34
blocos, a auditoria comparou os valores do header com a soma da série temporal.

### Código atual do Champion Companion

Principais arquivos auditados:

- `core/bluetooth/ChampionD3WorkoutIndexProtocol.kt`;
- `core/bluetooth/ChampionD4WorkoutParser.kt`;
- `core/model/WatchWorkoutActivity.kt`;
- `core/model/WatchWorkoutDerivedMetrics.kt`;
- `core/database/ChampionDatabase.kt`;
- `core/database/ChampionMigrations.kt`;
- `data/DatabaseMappers.kt`;
- `data/RoomRepositories.kt`;
- `feature/exercises/ExercisesScreen.kt`;
- contratos e documentação das Specs 014, 021, 023 e 027.

## 1. D3 — índice físico de slots

### APK

O comando de índice é um buffer iniciado por:

```text
D3 01 00 00
```

Na resposta aceita pelo APK, os CRCs são lidos como pares `u16 little-endian` a
partir do offset 3. Para cada posição `i`, o APK preserva a posição física junto
do CRC e somente depois decide se deve ou não solicitar o D4 daquele slot.

Comportamento equivalente:

```text
slot 0 -> crc0
slot 1 -> crc1
slot 2 -> crc2
...

crc == 0
  -> não solicita D4
  -> posição continua existindo
```

O comando D4 usa a posição física original do slot.

### Divergência no código atual

`ChampionD3WorkoutIndexProtocol.IndexCollector` incrementa
`nextOriginalBlockIndex` somente depois de descartar `crc == 0`. Isso comprime
slots vazios e pode deslocar todos os blocos posteriores.

Contrato congelado para `032-02`:

1. o índice físico avança para todo candidato D3, inclusive CRC zero;
2. CRC zero não gera entrada de treino;
3. CRC repetido pode ser deduplicado como identidade lógica, mas nunca renumera a
   posição física do slot;
4. `D4 <blockIndex>` usa a posição física congelada.

## 2. D4 — framing externo

No layout fixo observado:

```text
byte 0      D4
bytes 1..2  packetIndex u16 LE
bytes 3..4  totalPackets u16 LE
byte 5      framing/valor não consumido pelo parser fixo auditado
bytes 6..19 payload do pacote
```

Pacotes 1, 2 e 3 formam o header. Pacotes 4+ formam a série temporal.

A ordem física de chegada não altera o índice semântico; remontagem continua por
`packetIndex`.

## 3. Header fixo — 42 bytes

O APK concatena exatamente 14 bytes de cada um dos três primeiros pacotes:

```text
header[0..13]   = packet 1 bytes[6..19]
header[14..27]  = packet 2 bytes[6..19]
header[28..41]  = packet 3 bytes[6..19]
```

Layout congelado:

| Header | Campo | Tipo |
| --- | --- | --- |
| `0` | marker/reservado | `u8` |
| `1..2` | ano inicial | `u16 LE` |
| `3` | mês inicial | `u8` |
| `4` | dia inicial | `u8` |
| `5` | hora inicial | `u8` |
| `6` | minuto inicial | `u8` |
| `7` | segundo inicial | `u8` |
| `8..9` | ano final | `u16 LE` |
| `10` | mês final | `u8` |
| `11` | dia final | `u8` |
| `12` | hora final | `u8` |
| `13` | minuto final | `u8` |
| `14` | segundo final | `u8` |
| `15..18` | total de passos | `u32 LE` |
| `19..22` | total de distância | `u32 LE` |
| `23..26` | total de calorias raw | `u32 LE` |
| `27..30` | total de `sportCount` | `u32 LE` |
| `31..32` | `recordCount` | `u16 LE` |
| `33` | `pauseCount` | `u8` |
| `34..35` | `pauseTime` | `u16 LE` |
| `36..37` | CRC | `u16 LE` |
| `38` | tipo de esporte raw | `u8` |
| `39..41` | reservado/desconhecido | bytes preserváveis somente se necessário |

### Mapeamento para pacotes crus

A fronteira entre pacote 1 e pacote 2 explica a leitura histórica do
`endSecond`:

```text
packet1[7..13]  início completo
packet1[14..19] fim até minuto
packet2[6]      segundo final
```

O segundo final não é uma heurística independente: é `header[14]`.

O mesmo vale para os demais campos que atravessam pacotes. O parser v4 deve
reconstruir o header primeiro e interpretar campos completos depois, evitando
heurísticas de largura por pacote.

## 4. Sample fixo D4 — layout v4

O método do APK que constrói `SportModelOriginBean` fixa o seguinte contrato:

| Offset raw | Campo | Tipo/semântica |
| --- | --- | --- |
| `6` | BPM raw | `u8` |
| `7..8` | `sportValue` / `sportCount` | `u16 LE` |
| `9..10` | passos | `u16 LE` |
| `11..12` | calorias raw | `u16 LE` |
| `13..14` | distância | `u16 LE` |
| `15` | estado de pausa raw | `u8` |
| `16..19` | não consumidos pelo parser fixo auditado | desconhecido |

Índice ordinal:

```text
APK minute     = packetIndex - 3   // 1-based
app index v4   = packetIndex - 4   // 0-based
allMinute APK  = totalPackets - 3
```

### Consequência para `rawMotionValue`

A leitura histórica:

```text
u24(bytes[7..9]) >>> 16
```

não representa um packing definido pelo protocolo. Ela apenas devolve
`bytes[9]`, isto é, o byte baixo do verdadeiro `steps u16`, enquanto
`steps < 256`.

Logo:

```text
bytes[7..8] = sportValue
bytes[9..10] = steps
```

O caso histórico funcionava por coincidência enquanto o byte alto dos passos era
zero. `d4-v4` não deve construir `rawMotionValue` a partir desses três bytes.

## 5. Evidência física agregada — 34 blocos completos

A auditoria local reconstruiu 34 headers e suas séries sem usar a interpretação
`d4-v3` para os campos v4.

Resultado:

| Invariante | Resultado |
| --- | ---: |
| `recordCount == quantidade de samples` | `34/34` |
| `header.totalSteps == Σ sample.steps` | `34/34` |
| `header.totalDistance == Σ sample.distance` | `34/34` |
| `header.totalCaloriesRaw == Σ sample.caloriesRaw` | `34/34` |
| `header.totalSportCount == Σ sample.sportValue` | `34/34` |
| `header.crc == CRC associado ao bloco importado` | `34/34` |

Nos 34 headers observados, `pauseCount` e `pauseTime` foram zero; por isso a
semântica completa de pausa não é considerada fisicamente validada por esta
amostra, mesmo que o APK nomeie os campos.

A evidência 34/34 permite tratar os agregados do header como checksums semânticos
fortes da série para o firmware observado. Não autoriza substituir uma série
inconsistente pelo agregado.

## 6. Unidades congeladas

### 6.1 Distância — confirmada como metros no layout fixo observado

O `SportModelHeadBean` do APK exibe a distância dividindo o valor armazenado por
`1000` para produzir quilômetros. O `SportModelOriginBean.getSpeed()` trata a
distância do sample como distância de um intervalo nominal e também divide por
`1000` ao produzir km/h.

Além disso:

```text
Σ sample.distance == header.totalDistance
```

em 34/34 blocos completos.

Decisão:

```text
sample.distance u16 = metros
header.totalDistance u32 = metros
```

`032-02` pode promover o valor do sample diretamente para
`watchDistanceMeters`/equivalente sem aguardar `032-05`.

`032-05` decide somente como essa distância medida pelo relógio participa da
distância canônica e da calibração, não sua unidade.

### 6.2 Calorias — escala confirmada como 1/1000 kcal

O `SportModelHeadBean` do APK converte:

```text
kcal = raw / 1000.0
```

E em 34/34 blocos:

```text
Σ sample.caloriesRaw == header.totalCaloriesRaw
```

Como sample e header fecham no mesmo raw, o sample usa a mesma escala do header.

Decisão:

```text
sample.caloriesRaw / 1000.0 = kcal do intervalo
header.totalCaloriesRaw / 1000.0 = kcal do treino
```

Persistir o raw continua recomendado para provenance; a representação semântica
em kcal pode ser derivada exatamente por escala fixa.

### 6.3 `sportValue`

Confirmado estruturalmente:

```text
Σ sample.sportValue == header.totalSportCount
```

em 34/34 blocos.

Unidade/significado fisiológico: **desconhecido**.

Decisão: preservar como inteiro raw semanticamente nomeado, sem converter para
intensidade, MET, energia, movimento ou outra unidade inventada.

### 6.4 Pausa

APK possui `pauseCount`, `pauseTime` no header e um campo por sample que o modelo
chama de pausa. A amostra física auditada não exercitou pausa real.

Decisão: persistir `pauseStateRaw` e os agregados de pausa; não congelar ainda
um enum de estados nem usar o byte do sample para reescrever a timeline.

## 7. BPM não controla validade dos demais fatos

O APK calcula média de BPM somente quando o valor está em `30..200`, mas salva o
`SportModelOriginBean` independentemente de o BPM entrar na média.

Contrato v4:

```text
sample estrutural válido
  -> preserva steps, distance, caloriesRaw, sportValue, pauseStateRaw

BPM fora do range aceito
  -> bpm = ausente/inelegível para FC
  -> não descarta o sample
```

A faixa final do Champion Companion pode continuar como política própria
versionada, mas não deve decidir a existência dos outros fatos.

## 8. Duração, `recordCount` e timeline

O APK possui simultaneamente:

- `startTime`;
- `stopTime`;
- `recordCount`;
- `pauseTime`.

`SportModelHeadBean.setSportTime()` calcula tempo pelo intervalo início/fim,
subtrai pausa e usa `recordCount × 60` apenas como fallback quando o tempo
calculado não é positivo.

A evidência física do banco também mostra que quantidade de samples não define a
duração exata. Nos 34 blocos completos auditados, a razão
`duration / sampleCount` variou aproximadamente de `48,43 s` a `62,80 s`.

Decisão: manter integralmente a regra da Spec 021:

```text
T = duração oficial válida da atividade
N = quantidade de samples estruturais elegíveis

effectiveDuration[i] = distribuição determinística de T por N
```

Com as invariantes existentes:

```text
Σ effectiveDuration = T
start[0] = workout.startedAt
end[N-1] = workout.endedAt
end[i] = start[i+1]
```

`60 s` continua nominal/proveniência. Não criar buckets sintéticos de primeiro ou
último minuto e não forçar `N × 60`.

## 9. Divergência header × série

O firmware observado fecha todos os quatro agregados em 34/34 blocos completos.
Por isso, divergência em `d4-v4` deve ser tratada como inconsistência real, não
como ruído esperado.

Contrato para implementação:

1. nunca substituir a série pelo total do header;
2. nunca ajustar um sample para fechar o agregado;
3. nunca distribuir diferença proporcionalmente;
4. preservar os dois lados para diagnóstico;
5. marcar a importação pelo menos `PARTIAL` quando um bloco aparentemente
   completo divergir em `recordCount`, passos, distância, calorias, sport count
   ou CRC;
6. a elegibilidade de distância nativa deve falhar fechada se a soma da série
   não fechar no total de distância do header;
7. decisão de fallback canônico pertence a `032-05`, sem apagar a divergência;
8. tamanho/offset impossível ou ausência de header obrigatório continua podendo
   ser `INVALID` por falha estrutural.

## 10. Persistência mínima congelada para v4

### Por sample/intervalo

Campos semânticos mínimos:

```text
sourceSampleIndex / intervalIndex
decodedSteps              // passos u16 reais do D4 v4
sportValueRaw              // u16
watchDistanceMeters        // u16 promovido para metros
watchCaloriesRaw           // u16, escala 1/1000 kcal
pauseStateRaw              // u8
BPM/sourceSampleIndex      // contrato existente de FC
```

`WorkoutInterval.steps` pode continuar espelhando os passos semânticos para
compatibilidade, mas `d4-v4` não deve depender de `rawMotionValue` para obtê-los.

Os campos legacy:

```text
rawMotionValue
rawDistanceValue
rawCaloriesValue
```

continuam pertencendo ao significado histórico de `d4-v1/v2/v3`. Não devem ser
sobrescritos com semântica v4 apenas para evitar migration.

### Por import/header

Persistir como fatos de origem, preferencialmente em `watch_workout_imports` ou
estrutura equivalente ligada à importação:

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

Alguns valores já existem também em `WorkoutActivity` como métricas semânticas.
A persistência de origem não deve depender desses campos derivados/consumíveis
para reconstruir a validação header × série.

### Room

A `main` auditada está em Room v15. `032-03` deve avaliar migration aditiva
`15 -> 16`; nenhuma coluna legacy recebe significado novo por backfill.

## 11. Mapa de consumidores atuais

### Parsing/importação

| Consumidor | Dependência atual | Mudança necessária |
| --- | --- | --- |
| `ChampionD3WorkoutIndexProtocol` | comprime CRC zero no contador de índice | preservar slot físico |
| `ChampionD4WorkoutParser` | u24 `7..9`, u24 `10..12`, u16 `13..14` | parser v4 por campos u16 reais |
| `WatchWorkoutActivity` | valida contrato de `rawMotionValue`/`decodedSteps` por versão | aceitar fatos v4 sem packing legacy |

### Domínio e derivação

| Consumidor | Dependência atual | Mudança necessária |
| --- | --- | --- |
| `WatchWorkoutDerivedMetrics` | distância por `decodedSteps × stepLengthMetersUsed` | aceitar distância nativa v4 como fonte primária elegível |
| snapshots de derivação | congelam step length/source como provenance central | ampliar provenance para fonte/fator de distância |
| splits/pace/speed | usam `canonicalIntervalDistanceMeters` | continuar consumindo série canônica, sem conhecer bytes D4 |

### Persistência

| Consumidor | Dependência atual | Mudança necessária |
| --- | --- | --- |
| `WorkoutIntervalEntity` | colunas legacy raw + `decodedSteps` | campos v4 semanticamente claros |
| `WatchWorkoutImportEntity` | parser/version/CRC/índice, sem header completo | preservar agregados/fatos do header |
| `DatabaseMappers` | reconstrói modelo legacy pelos campos atuais | adapters por parserVersion |
| `RoomRepositories` | dedup/reimport compara raw legacy e materializa derivação | integrar v4 tardiamente após contrato/schema |

### Calibração

| Consumidor | Dependência atual | Mudança necessária |
| --- | --- | --- |
| `step_length_calibrations` | referência / decodedSteps | permanece histórica; não converter silenciosamente |
| resolução contextual RUNNING/WALKING | perfil aprendido de comprimento de passo | v4 ganha perfil independente de fator de distância |
| preview/rederivação | distribui referência por passos | v4 corrige proporcionalmente a distância nativa |

### Produto/UI

| Consumidor | Dependência atual | Mudança necessária |
| --- | --- | --- |
| `ExercisesScreen` | exibe comprimento do passo e série canônica | encaixar nova provenance após merge da 031 |
| lista/calendário/saúde | leem métricas canônicas | não precisam conhecer raw v4 se domínio permanecer estável |

### Health Connect e passos

A Spec 027 permanece autoritativa para contabilidade de passos:

```text
interval.steps -> StepsRecord/crédito quando elegível
interval.steps / effectiveDuration -> cadência
DailySummary.steps -> total diário autoritativo
```

A 032 não usa `header.totalSteps` para criar uma segunda fonte de passos.

Distance/Speed continuam consumindo projeções canônicas congeladas; mudar a
origem da distância em nova derivação não autoriza mutar payload remoto antigo.

## 12. Decisões fechadas para as próximas tasks

### Liberado para `032-02`

- corrigir D3 para slot físico;
- implementar reconstrução do header de 42 bytes;
- implementar sample v4 pelos offsets congelados;
- promover sample distance diretamente para metros;
- promover calorias semanticamente por `raw / 1000.0`, preservando raw;
- preservar `sportValueRaw` e `pauseStateRaw`;
- separar validade do BPM da validade do sample;
- comparar agregados header × série e CRC sem corrigir dados.

### Liberado para `032-03`

- projetar migration aditiva a partir de Room v15;
- persistir fatos v4 em colunas semanticamente novas;
- persistir fatos mínimos do header ligados à importação;
- manter campos legacy intactos;
- não backfillar v4 a partir de `rawMotionValue/rawDistanceValue/rawCaloriesValue`.

### Não decidido por `032-01`

Ficam deliberadamente para tasks posteriores:

- política final de escolha entre distância nativa sem fator, fator contextual e
  step-length fallback (`032-05`);
- modelo persistente das evidências de fator de distância e agregação contextual
  (`032-06`);
- copy/UI final e encaixe no redesign 031 (`032-07`);
- semântica enum completa de `pauseStateRaw`;
- uso analítico futuro de `sportValueRaw`;
- variantes D4 GPS-rich para capability 4/6, fora do runtime C001 observado.

## 13. Riscos residuais

- o contrato foi validado no firmware/dispositivo observado; outros firmwares
  podem divergir;
- os 34 blocos disponíveis não exercitaram pausa no header;
- a variante GPS-rich do APK usa framing distinto e não é coberta por este
  layout fixo;
- `sportValue` possui estrutura e agregado confirmados, mas significado funcional
  desconhecido;
- atividade legacy sem payload D4 preservado não pode ser promovida a v4 por
  inferência dos campos interpretados incorretamente.

## Conclusão

A evidência disponível é suficiente para encerrar a ambiguidade principal da
Fase 014:

```text
7..8   sportValue
9..10  steps
11..12 caloriesRaw
13..14 distanceMeters
15     pauseStateRaw
```

Distância e calorias deixam de ser campos temporalmente desconhecidos para o
layout fixo observado. A timeline `T/N` permanece correta e independente dessa
correção. `032-02` e `032-03` podem avançar em paralelo após revisão deste
contrato, sem tocar ainda em UI ou na escolha final da distância canônica.
