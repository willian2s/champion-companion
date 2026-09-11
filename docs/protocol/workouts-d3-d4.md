# Treinos D3/D4 — Champion C001

## Objetivo

Documentar o contrato observado para importação de treinos gravados no Champion
C001 via D3/D4, separando claramente:

```text
SOURCE      bytes e campos recebidos do relógio
MEASURED    unidades promovidas com evidência suficiente
DERIVED     timeline, distância corrigida, pace, velocidade e cadência
PROJECTION  payloads congelados para consumidores e Health Connect
```

O runtime atual de novas importações, após `032-07`, usa `parserVersion = d4-v4`.
`d4-v1/v2/v3` permanecem versões históricas identificáveis para leitura e
reprocessamento explícito. O contrato v4 só é publicado quando parser,
persistência e derivação estão coerentes no mesmo fluxo.

Auditoria autoritativa da Spec 032:

`docs/integration/032-01-d4-v4-layout-and-consumer-audit.md`.

## Fontes

### APK Champion Smartstyle decompilado

Arquivos auditados localmente, sem versionar o APK decompilado:

- `com/veepoo/hband/ble/readmanager/SportModelHander.java`;
- `com/veepoo/hband/modle/SportModelOriginBean.java`;
- `com/veepoo/hband/modle/SportModelHeadBean.java`.

### Champion Companion

- `ChampionD3WorkoutIndexProtocol`;
- `ChampionD4WorkoutBlockProtocol`;
- `ChampionD4WorkoutParser`;
- `ChampionWatchWorkoutImportCoordinator`;
- `WatchWorkoutActivity.kt`;
- `WatchWorkoutDerivedMetrics.kt`;
- `ChampionDatabase.kt`;
- `DatabaseMappers.kt`;
- `RoomRepositories.kt`.

### Evidência física local

A auditoria `032-01` inspecionou uma cópia local do banco fornecida pelo usuário.
Foram reconstruídos 34 blocos D4 completos com header e samples suficientes para
validar invariantes agregados, sem versionar payload integral, identificadores ou
dados pessoais.

## Classificação normativa

| Classe | Significado |
| --- | --- |
| **Comportamento do runtime atual** | O app existente executa essa leitura hoje. Não prova semântica do relógio. |
| **Confirmado pelo APK** | O APK oficial auditado usa esse campo/layout/conversão. |
| **Confirmado no dispositivo** | Evidência física do C001 observado sustenta a afirmação. |
| **Derivado planejado** | Política do Champion Companion; não é campo recebido do relógio. |
| **Desconhecido** | Evidência insuficiente para atribuir unidade ou significado funcional. |

# Fluxo de produto

Treino importado por D3/D4 permanece local-first e exige ação explícita:

```text
Champion C001 inicia e encerra
        ↓
importação D3/D4 explícita
        ↓
normalização e derivação local
        ↓
revisão do usuário
        ↓
sincronização Health Connect explícita
```

Importar, reprocessar, calibrar ou migrar schema não autoriza sync automático.

# Transporte compartilhado

| Item | Contrato |
| --- | --- |
| Escrita | F008 write com resposta |
| Resposta | F008 notify |
| Preparo | conexão, notifications e handshake A1 |
| Execução | foreground, um D3 seguido de D4 sequenciais |
| Timeout atual | D3: 10 s; D4: 15 s por bloco |
| Retry interno | nenhum |
| Limpeza | listener específico removido no encerramento |

# D3 — índice de blocos

## Requisição

```text
D3 01 00 00
```

O APK monta um buffer de 20 bytes iniciado por esse prefixo.

## Pares CRC

No layout observado, os candidatos são lidos em pares `u16 little-endian` a
partir do offset `3`.

Cada posição física do par representa o slot usado depois no comando D4.

Exemplo:

```text
slot 0 = 0000
slot 1 = B945
slot 2 = 91FC
```

A leitura correta é:

```text
ignorar slot 0 como treino ausente
requisitar D4 slot 1 para B945
requisitar D4 slot 2 para 91FC
```

Não é permitido compactar para `slot 0` e `slot 1`.

### Regra v4

```text
posição física do CRC no D3 = blockIndex usado no D4
```

- CRC `0x0000` pode ser ignorado como entrada vazia;
- ignorar CRC zero não desloca slots posteriores;
- CRC repetido pode ser deduplicado logicamente, mas a entrada preservada mantém
  a posição física original;
- o runtime atual ainda possui bug ao avançar o índice apenas após CRC não zero;
  a correção pertence à `032-02`.

A relação CRC D3 ↔ CRC do header D4 fechou para todos os 34 blocos completos
inspecionados na auditoria, sem promover isso a garantia universal para firmware
não observado.

# D4 — framing e coleta

## Requisição

```text
D4 <blockIndex> 00 00
```

## Framing

```text
byte 0      D4
bytes 1..2  packetIndex u16 LE
bytes 3..4  totalPackets u16 LE
byte 5      framing/valor não consumido pelo parser fixo auditado
bytes 6..19 payload
```

Pacotes `1..3` formam o header. Pacotes `4+` formam a série temporal.

A coleta pode receber fora de ordem. A remontagem semântica usa `packetIndex`.
Duplicata e lacuna não são corrigidas por síntese.

# Header fixo D4 — 42 bytes

O APK concatena 14 bytes de cada um dos três primeiros pacotes:

```text
header[0..13]   = packet 1 bytes[6..19]
header[14..27]  = packet 2 bytes[6..19]
header[28..41]  = packet 3 bytes[6..19]
```

## Layout congelado para `d4-v4`

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
| `39..41` | reservados/desconhecidos | bytes |

### Mapeamento relevante para pacotes crus

```text
packet1[7..13]  início completo
packet1[14..19] fim até minuto
packet2[6]      segundo final

packet2[7..10]  totalSteps u32 LE
packet2[11..14] totalDistance u32 LE
packet2[15..18] totalCaloriesRaw u32 LE
packet2[19]     low byte de totalSportCount

packet3[6..8]   restante de totalSportCount
packet3[9..10]  recordCount u16 LE
packet3[11]     pauseCount
packet3[12..13] pauseTime u16 LE
packet3[14..15] CRC u16 LE
packet3[16]     sportTypeRaw
```

O `endSecond` historicamente descoberto em `packet2[6]` é simplesmente
`header[14]`; no v4 não deve existir uma heurística separada para esse byte.

# Samples D4 fixos — layout `d4-v4`

Para cada `packetIndex >= 4`:

```text
app intervalIndex = packetIndex - 4
APK minute        = packetIndex - 3  // ordinal 1-based
APK allMinute     = totalPackets - 3
```

## Layout

| Offset raw | Campo | Tipo/semântica |
| --- | --- | --- |
| `6` | BPM raw | `u8` |
| `7..8` | `sportValue` / `sportCount` | `u16 LE` |
| `9..10` | passos | `u16 LE` |
| `11..12` | calorias raw | `u16 LE` |
| `13..14` | distância | `u16 LE` |
| `15` | estado de pausa raw | `u8` |
| `16..19` | não consumidos pelo parser fixo auditado | desconhecido |

BPM inválido não torna os demais campos inválidos. A política de faixa de BPM é
independente do aceite estrutural do sample.

# Correção da interpretação histórica de `rawMotionValue`

O runtime `d4-v2/v3` lê atualmente:

```kotlin
rawMotionValue = u24LE(bytes[7..9])
decodedSteps = rawMotionValue ushr 16
```

Isso não corresponde a um packing definido pelo protocolo fixo auditado.

Como `u24LE(bytes[7..9]) >>> 16` devolve apenas `bytes[9]`, o resultado coincide
com passos somente quando o byte alto real `bytes[10]` é zero.

Portanto:

```text
bytes[7..8]  = sportValue
bytes[9..10] = steps u16 LE
```

O caso histórico de `3.737` passos e a atividade posterior de `6.240` passos
funcionaram porque nenhum bucket ultrapassou `255` passos. `d4-v4` deve ler o
`u16` real e não reconstruir `rawMotionValue`.

As colunas legacy `rawMotionValue`, `rawDistanceValue` e `rawCaloriesValue`
continuam preservadas para `d4-v1/v2/v3`; não recebem novo significado por
migration ou conveniência.

# Unidades confirmadas para o layout fixo observado

## Distância

**Confirmado pelo APK + confirmado no dispositivo:**

```text
sample.distance       = metros
header.totalDistance  = metros
```

Evidência:

- `SportModelHeadBean` divide o total por `1000` para exibir quilômetros;
- `SportModelOriginBean.getSpeed()` usa a distância do sample como metros em um
  intervalo nominal;
- `Σ sample.distance == header.totalDistance` em 34/34 blocos completos.

A `032-02` pode promover o valor para `watchDistanceMeters`/equivalente.
A escolha de distância nativa como fonte canônica pertence à `032-05`.

## Calorias

**Confirmado pelo APK + confirmado estruturalmente no dispositivo:**

```text
kcal = caloriesRaw / 1000.0
```

- o header usa essa escala no APK;
- `Σ sample.caloriesRaw == header.totalCaloriesRaw` em 34/34 blocos completos.

O raw deve ser preservado para provenance mesmo quando a projeção em kcal for
materializada.

## `sportValue`

Estrutura confirmada no APK e fechamento confirmado no dispositivo:

```text
Σ sample.sportValue == header.totalSportCount
```

em 34/34 blocos completos.

A unidade ou significado fisiológico permanece **desconhecido**. Não chamar de
MET, intensidade, energia, carga ou equivalente sem nova evidência.

## Pausa

O APK nomeia:

```text
sample.pauseStateRaw
header.pauseCount
header.pauseTime
```

Nos 34 blocos completos auditados, `pauseCount == 0` e `pauseTime == 0`.
Logo, offsets/nome estrutural estão documentados, mas semântica operacional de
valores não zero ainda não está fisicamente validada. `d4-v4` deve preservar raw
sem inventar enum ou alterar timeline apenas por suposição.

# Invariantes físicos da auditoria `032-01`

Para 34 blocos completos reconstruídos:

```text
recordCount == quantidade de samples            34/34
header.totalSteps == Σ sample.steps              34/34
header.totalDistance == Σ sample.distance        34/34
header.totalCaloriesRaw == Σ sample.caloriesRaw  34/34
header.totalSportCount == Σ sample.sportValue    34/34
header.crc == CRC associado ao bloco             34/34
```

Esses totais são checks semânticos fortes da série para o firmware observado.
Eles **não** autorizam sobrescrever samples divergentes com valores do header.

## Política para divergência header × série

Quando um bloco completo divergir:

- preservar header e samples recebidos;
- não alterar sample para forçar fechamento;
- não substituir série pelo total;
- marcar pelo menos `PARTIAL` quando a estrutura ainda for reconhecível;
- distância nativa deve falhar fechada para derivação quando sua soma não fechar
  no total do header;
- falha de layout/estrutura pode continuar `INVALID`.

# Passos — autoridade por contexto

## Atividade D4

Para `d4-v4`:

```text
sample.steps = u16 LE bytes[9..10]
```

A série por intervalo é a fonte do treino para:

- passos da atividade;
- cadência;
- créditos/guards de passos da Spec 027;
- análises temporais locais.

O `header.totalSteps` é check de consistência, não substituto da série.

## Total diário

`DailySummary.steps` continua sendo a autoridade do acumulado diário do relógio.
Nada na Spec 032 transforma o total D4 do header em substituto do resumo diário.

# Timeline — duração oficial independente da quantidade de samples

A decisão da Spec 021 permanece autoritativa:

```text
startedAt + endedAt
  = janela oficial da atividade

N samples D4
  = sequência granular ordenada

60 s
  = granularidade nominal/proveniência
  ≠ duração física obrigatória de cada sample
```

Na atividade física de referência:

```text
startedAt   07:27:14
endedAt     08:09:36
duração     42:22
samples     44
pauseTime   0
```

Nos 34 blocos auditados, `duration / sampleCount` variou aproximadamente de
`48,43 s` a `62,80 s`, portanto não existe base para impor `N × 60 s`.

## Normalização efetiva preservada

Para série estruturalmente elegível, com duração oficial `T` e `N > 0`:

```text
base = T / N
resto = T % N

effectiveDuration[i] = base + 1 ns para os primeiros `resto` intervalos
```

Invariantes:

```text
Σ effectiveDuration[i] = T
start[0] = workout.startedAt
end[N-1] = workout.endedAt
end[i] = start[i+1]
```

Não criar primeiro/último bucket sintético, não inferir duração por
`recordCount` e não voltar a usar `sampleIndex × 60 s` como duração autoritativa.

# Persistência alvo para `d4-v4`

A `032-03` deve decidir o schema final, mas a auditoria congelou a necessidade de
não reutilizar semanticamente as colunas legacy.

## Por intervalo

Campos mínimos:

```text
intervalIndex/sourceSampleIndex
steps u16 promovidos
sportValueRaw
watchDistanceMeters
watchCaloriesRaw
pauseStateRaw
BPM + sourceSampleIndex existente
```

## Por header/importação

Campos mínimos de provenance/consistência:

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

A `main` auditada no início da Spec 032 está em Room v15; uma eventual migration
deve revalidar a base e, se necessária, ser aditiva a partir da versão corrente.

Migration nunca deve reinterpretar `d4-v1/v2/v3` como v4 sem payload suficiente,
ativar snapshot, criar calibração, mudar workflow ou gerar efeito Health Connect.

# Derivação atual e direção da Spec 032

## Runtime antes da 032

O runtime vigente `d4-v3` ainda deriva:

```text
canonicalIntervalDistanceMeters = decodedSteps × stepLengthMetersUsed
canonicalDistanceMeters = Σ canonicalIntervalDistanceMeters
```

A normalização temporal já usa `T/N`.

## Direção `d4-v4`

Para tipos elegíveis e série nativa válida, a Spec 032 planeja:

```text
baseIntervalDistanceMeters = watchDistanceMeters
baseWorkoutDistanceMeters  = Σ watchDistanceMeters
```

Calibração externa opcional deverá corrigir a série proporcionalmente, em vez de
substituí-la por uma passada fixa.

Comprimento de passo permanece fallback explícito quando a distância nativa não
for elegível. Calibrações legacy de comprimento de passo não são convertidas
silenciosamente em fator de distância.

`CYCLING` mantém política própria da Spec 022.

# Health Connect

O contrato de produto permanece:

- write-only;
- local-first;
- sync explícito;
- operações filhas com identidade/payload congelados;
- retry idempotente;
- passos da atividade respeitam crédito/cobertura da Spec 027;
- mudança de derivação local não altera records remotos aceitos silenciosamente.

A projeção vigente pode conter:

```text
EXERCISE_SESSION
DISTANCE
SPEED
HEART_RATE
STEPS
STEPS_CADENCE
TOTAL_CALORIES_BURNED
```

conforme elegibilidade e contratos das specs anteriores.

## Runtime atual após 032-07

Novas importações usam o layout `d4-v4`, com fatos nativos persistidos e
derivação `watch-d3-d4-v4` quando a série é elegível. A distância nativa é fonte
primária para RUNNING/WALKING; calibração externa corrige a série
proporcionalmente. Quando a distância nativa não é elegível, fallback por
comprimento de passo permanece explícito. `CYCLING` mantém sua política própria.

Review-before-sync continua obrigatório. Projeções Health Connect usam payloads
congelados, provenance, IDs estáveis, versões monotônicas e retry explícito;
importação, review, calibração e reprocessamento não executam efeito remoto.

# Versionamento

## `d4-v1`

Legado que persistiu leitura empacotada antiga. Continua identificável e não
recebe interpretação v4 automática.

## `d4-v2`

Legado que passou a preservar `rawMotionValue` e derivar passos por
`rawMotionValue ushr 16`.

## `d4-v3`

Runtime vigente antes da Spec 032. Corrige duração oficial e usa timeline
normalizada `T/N`, mas ainda mantém a interpretação histórica dos offsets dos
samples.

## `d4-v4`

Contrato congelado na `032-01`:

- D3 preserva slot físico;
- header reconstruído em 42 bytes;
- passos `u16` em `9..10`;
- `sportValue u16` em `7..8`;
- calorias raw `u16` em `11..12`;
- distância em metros `u16` em `13..14`;
- pausa raw em `15`;
- agregados completos do header;
- timeline efetiva permanece `T/N`.

`parserVersion = d4-v4` é publicado para novas importações após `032-07`, quando
parser, schema e derivação estão coerentes no mesmo runtime. Histórico legacy não
é reinterpretado automaticamente.

# Lacunas ainda abertas

- semântica operacional de `pauseStateRaw` com valores não zero;
- significado/unidade fisiológica de `sportValue`;
- significado de `header[0]`, `header[39..41]` e sample `bytes[16..19]`;
- variações de firmware e eventual seleção entre layout fixo e GPS-rich;
- política exata para reprocessamento de históricos com/sem payload preservado;
- tolerâncias finais de validade/plausibilidade em divergências físicas futuras;
- cobertura física de treino com sample de passos acima de 255;
- retenção final de payload diagnóstico.

Lacunas já **não** abertas para o layout fixo observado:

- offsets de passos por sample;
- unidade de distância por sample/header;
- escala de calorias;
- largura de `recordCount`;
- largura dos totais do header;
- localização do CRC e `sportTypeRaw` no header;
- regra de índice físico D3;
- independência entre duração oficial e quantidade de samples.

## Validação física final da Spec 032 — 2026-09-11

A validação manual do Champion C001 foi concluída com conferência em dumps
sanitizados do banco. Foram considerados válidos os cenários de slot físico D3,
variação de intensidade, fechamento diagnóstico de header e série, timeline
`T/N`, distância nativa, calibração manual, perfis RUNNING/WALKING, fallback,
review-before-sync, Health Connect, Daily Summary, cadência e `CYCLING`.

Payload BLE integral, identificadores pessoais e dump completo não foram
versionados. Permanecem abertas a semântica operacional de `pauseStateRaw` não
zero, a unidade funcional de `sportValue`, campos reservados ou desconhecidos,
política de reprocessamento de históricos, tolerâncias para divergências futuras,
cobertura de samples acima de 255 passos, retenção de payload diagnóstico e
variações de firmware/layout não observadas. A referência externa manual
permanece sujeita a erro e serve somente como referência de calibração.

Evidência detalhada: `docs/integration/032-08-physical-validation.md`.

## Referências

- `docs/integration/032-01-d4-v4-layout-and-consumer-audit.md`
- `docs/specs/032-d4-native-workout-metrics.md`
- `docs/decisions/adr-029-d4-native-metrics-and-distance-calibration.md`
- `docs/decisions/adr-019-authoritative-workout-duration-and-normalized-d4-timeline.md`
- `docs/decisions/adr-018-canonical-workout-time-series-and-health-projection.md`
- `docs/specs/021-d4-timeline-normalization-and-calendar-integrity.md`
- `docs/specs/023-personal-metrics-calibration.md`
- `docs/specs/027-workout-steps-cadence-1hz-timeline.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`
