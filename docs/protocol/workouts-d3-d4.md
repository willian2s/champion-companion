# Treinos D3/D4 — MotionLog para Champion Companion

## Objetivo

Documentar a importação de treinos gravados no Champion C001: índice D3,
download D4, remontagem, parser, persistência, revisão e bloqueio de envio
automático.

O documento preserva a baseline MotionLog e registra separadamente o estado
factual do Champion Companion auditado em `014-01`.

## Fontes

### Baseline MotionLog

- `../motionlog-app/src/lib/ble/champion-commands.ts`
- `../motionlog-app/src/lib/ble/watch-workout-sync.ts`
- `../motionlog-app/src/lib/ble/watch-workout-parser.ts`
- `../motionlog-app/src/lib/ble/watch-workout-import-adapter.ts`
- `../motionlog-app/src/lib/repositories/watch-workout-repository.ts`
- `../motionlog-app/src/lib/repositories/database/migrations.ts`
- `../motionlog-app/src/lib/health-connect/watch-workout-sync.ts`
- `../motionlog-app/src/models/watch-workout.ts`

### Champion Companion

- `ChampionD4WorkoutBlockProtocol`
- `ChampionD4WorkoutParser`
- `ChampionWatchWorkoutImportCoordinator`
- `WatchWorkoutActivity.kt`
- `ChampionDatabase.kt`
- `DatabaseMappers.kt`
- `RoomRepositories.kt`
- `ExercisesScreen.kt`
- `HealthRecordMapper.kt`
- `HealthConnectSyncCoordinator.kt`
- `docs/integration/014-01-d4-layout-and-consumer-audit.md`

## Classificação normativa

Toda afirmação relevante deve usar uma destas classes:

| Classe | Significado |
| --- | --- |
| **Comportamento do código atual** | O software existente executa essa leitura ou transformação. Não prova que a semântica atribuída corresponde ao relógio. |
| **Confirmado no dispositivo** | Evidência física disponível sustenta a afirmação para o cenário observado. Não implica todos os firmwares. |
| **Desconhecido** | Não há evidência suficiente para nome, unidade ou significado funcional. |
| **Derivado planejado** | Métrica que será calculada pelo aplicativo por política versionada; não é campo recebido do relógio. |

“Confirmado pelo código” nunca deve ser interpretado como “confirmado pelo
dispositivo”.

## Estado das fases

- Fase 013: `closed`, resultado `partially_delivered`, motivo
  `product_reorientation`.
- Fase 014: `completed`, com progresso `17/17` após conclusão de `014-16`.
- Treinos phone, GPS, rota, mapa, `ExerciseRoute` e BLE live não fazem parte da
  direção da Fase 014.

## Estado factual final da Fase 014

- Novas importações usam `parserVersion = d4-v2`; `d4-v1` permanece identificável
  como legado. `MIGRATION_8_9` preserva o u24 legado como raw e não cria
  `decodedSteps` automaticamente.
- O parser novo preserva `rawMotionValue`, deriva `decodedSteps` com deslocamento
  de 16 bits e mantém os 16 bits inferiores, distância temporal e calorias
  temporais como desconhecidos/raw. A sequência sanitizada de referência soma
  `3.737` passos; isso é evidência do caso observado, não garantia para todos os
  firmwares.
- A série entregue aplica índices contínuos e únicos, duração `T/N/60 s`,
  residual positivo e plausibilidade separada. `T = 1.637 s` produz `27 × 60 s +
  17 s`; não há redistribuição nem síntese de cauda.
- A distância canônica usa a soma dos intervalos `decodedSteps ×
  stepLengthMetersUsed`. O snapshot congela valor individual, origem, método e
  versão da derivação. Distâncias original e calibrada histórica permanecem
  diagnósticas.
- O fluxo `TREADMILL` de `WATCH_D3_D4` usa série canônica elegível sem calibração
  histórica. Projeções filhas Health Connect usam IDs, versões, payloads,
  resultados e retry próprios; a integração continua write-only.
- A validação física passou apenas nos cenários exercitados. Gates B, C, D, E e G
  permanecem `N/A`; midpoint, delete-before-insert, round-trip completo e
  variações de firmware continuam desconhecidos como evidência física.

## Regra obrigatória de produto

Treino importado por D3/D4 nasce pendente de revisão e nunca é enviado
automaticamente ao Health Connect.

Fluxo vigente:

```text
Champion C001 inicia e encerra
        ↓
importação D3/D4 explícita
        ↓
normalização e derivação local
        ↓
revisão do usuário
        ↓
sincronização explícita
```

## Transporte compartilhado

| Item | Comportamento do código atual |
| --- | --- |
| Escrita | F008 write com resposta |
| Resposta | F008 notify; listener filtra primeiro byte `D3` ou `D4` |
| Preparo | garantir conexão, notifications e handshake A1 |
| Execução | foreground, um D3 seguido de D4 sequenciais |
| Retry interno | nenhum retry interno para D3/D4 |
| Timeout | D3: 10 s; D4: 15 s por bloco |
| Limpeza | listener específico removido no encerramento |

Detalhes BLE gerais: `ble-transport.md`. Comandos gerais:
`ble-command-mapping.md`.

# D3 — Índice de CRCs

## Requisição

```text
D3 01 00 00
```

Finalidade observada: listar referências de blocos de treino disponíveis no
relógio.

## Leitura atual

- aceita apenas notificações cujo primeiro byte seja `D3`;
- lê pares u16 little-endian a partir do offset 3;
- ignora `0x0000`;
- cria `blockIndex` sequencial pela ordem de coleta;
- encerra coleta multipart quando o cabeçalho é plausível e
  `packetIndex >= totalPackets`;
- sem cabeçalho plausível, a primeira notificação encerra a coleta;
- timeout preserva CRCs já coletados;
- não há retry interno.

A relação entre CRC D3 e CRC do pacote 3 D4 ainda precisa de validação mais ampla
entre firmwares.

# D4 — Download e remontagem

## Requisição

```text
D4 <blockIndex> 00 00
```

O `blockIndex` ocupa um byte e é derivado da ordem dos CRCs D3 válidos.

## Cabeçalho e coleta no Champion Companion

| Campo | Leitura atual |
| --- | --- |
| comando | byte 0, esperado `D4` |
| `packetIndex` | u16 LE em `bytes[1..2]` |
| `totalPackets` | u16 LE em `bytes[3..4]` |

`ChampionD4WorkoutBlockProtocol.BlockCollector`:

- preserva cada pacote recebido;
- registra índices duplicados;
- calcula ausentes na faixa `1..totalPackets` conhecida;
- ordena por `packetIndex` ao finalizar;
- encerra quando `totalPackets > 0` e `packetIndex >= totalPackets`;
- preserva coleta parcial em timeout.

Duplicata ou ausência não é corrigida por síntese.

# Parser D4 atual

## Pacotes obrigatórios

O parser atual exige pacotes 1, 2 e 3. Ausência ou falha estrutural de qualquer um
impede criar uma atividade.

Essa é uma regra do parser, não uma garantia universal do protocolo do relógio.

## Pacote 1 — início e fim

| Item | Comportamento do código atual | Estado semântico |
| --- | --- | --- |
| layout preferido | início em `7..13`; fim em `14..19`; anos u16 LE | heurística do código |
| layout legado | offsets alternativos em `5..19` | fallback do código |
| segundo final preferido | `packet2[6]`, somente quando o candidato preferido do pacote 2 é selecionado; exige `0..59` | evidência confirmada no dispositivo para três atividades; limite de firmware |
| segundo final legado | `packet1[14]` | comportamento do código atual; sem alteração de offset |
| duração | diferença positiva entre `startedAt` e `endedAt` corrigidos; fallback do pacote 3 somente no layout legado | duração oficial separada de `recordCount` e samples |
| `endedAt` no domínio | layout preferido combina data/hora/minuto de `packet1[14..19]` com `packet2[6]`; valor preferido fora de `0..59` falha fechado | proveniência temporal explícita no domínio |

## Pacote 2 — totais

| Campo | Layout preferido atual | Layout legado atual | Classificação |
| --- | --- | --- | --- |
| passos totais | u16 LE `7..8` | u24 LE `5..7` | comportamento do código; coerente na atividade de referência |
| segundo final | byte `6`, `0..59` | não usado; segundo vem de `packet1[14]` | confirmado no dispositivo somente no layout preferido observado |
| distância total raw | u16 LE `11..12`, escalado pelo parser | u32 LE `8..11` | unidade/layout do dispositivo não comprovados de forma geral |
| calorias totais raw | u24 LE `15..17` | u32 LE `12..15` | unidade/layout do dispositivo não comprovados de forma geral |

A seleção entre layouts usa plausibilidade de valores. Plausibilidade não prova o
layout físico.

## Pacote 3 — contagem e CRC

| Campo | Leitura preferida | Fallback legado | Classificação |
| --- | --- | --- | --- |
| `recordCount` | byte 9 | u16 LE `9..10` | heurística do código |
| CRC | u16 LE `14..15` | u16 LE `11..12` | heurística do código |
| duração fallback | não usada no preferido | u32 LE `5..8` | comportamento do código |

## Pacotes 4+ — amostras temporais

Para cada pacote presente:

```text
minuteIndex = packetIndex - 4
```

| Campo | Offset/leitura atual | Tratamento atual | Classificação após 014-01 |
| --- | --- | --- | --- |
| BPM | byte 6 | 30..220 gera amostra; demais valores viram ausência | medido quando válido no caso observado |
| movimento | u24 LE `7..9` | `d4-v1` salva o u24 em `steps`; `d4-v2` separa raw e passos deslocados | raw empacotado; somente byte superior é passo confirmado no caso observado |
| distância temporal | u24 LE `10..12` | `d4-v1` e `d4-v2` transportam somente `rawDistanceValue`; valores legados já persistidos permanecem preservados | valor raw; unidade e semântica não comprovadas |
| calorias temporais | u16 LE `13..14` | `d4-v1` e `d4-v2` transportam somente `rawCaloriesValue`; valores legados já persistidos permanecem preservados | valor raw; unidade e semântica não comprovadas |

Conversões históricas de distância/calorias podem existir em linhas `d4-v1` já
persistidas, mas não são reproduzidas por nenhum caminho atual do parser nem
alimentam a série canônica da Fase 014.

## Implementação `d4-v2`

Para cada pacote temporal, o parser lê o movimento uma única vez e transporta os
fatos separadamente:

```kotlin
val rawMotionValue = bytes.readUInt24LittleEndian(7)
val decodedSteps = rawMotionValue ushr 16
```

`WorkoutInterval.steps` recebe `decodedSteps` somente em `d4-v2`; o total de
passos do pacote 2 permanece em `WorkoutMetrics.steps` e não é substituído pela
soma temporal. `rawMotionValue`, `rawDistanceValue`, `rawCaloriesValue` e
`decodedSteps` seguem para as colunas raw/measured da migration local `v9`.

Distância e calorias temporais de `d4-v1` e `d4-v2` não são convertidas para
métricas semânticas. Os 16 bits inferiores de `rawMotionValue` permanecem
desconhecidos e não participam de qualquer derivação.

# Movimento empacotado e passos

## Evidência confirmada no caso de referência

```text
rawMotionValue = 6.553.643
hex            = 0x0064002B
decodedSteps   = rawMotionValue ushr 16 = 100
lower16        = 0x002B = 43
```

O valor `43` permanece sem nome ou uso funcional.

Sequência observada após a extração do byte superior:

```text
100, 117, 114, 117, 103, 109, 133, 131, 130, 175, 175, 152,
123, 116, 176, 169, 121, 155, 184, 178, 136, 115, 120, 110,
127, 122, 125, 104
```

Soma:

```text
3.737 passos
```

A soma coincide com o total da atividade analisada. Essa evidência sustenta o
`decodedSteps` para o caso observado, mas não identifica os 16 bits inferiores
nem garante todos os firmwares.

# Versionamento e compatibilidade

## `d4-v1`

`d4-v1` identifica importações legadas que persistiram o u24 empacotado no campo
`steps`. Essas atividades continuam identificáveis e não recebem
`decodedSteps` por backfill automático.

Cadeia de persistência auditada:

```text
bytes[7..9]
  → readUInt24LittleEndian(7)
  → SampleData.rawMotionValue
  → WorkoutInterval.steps
  → WorkoutIntervalEntity.steps
```

Não há truncamento intermediário. Portanto, para:

```text
origin == WATCH_D3_D4
parserVersion == d4-v1
0 <= legacy steps <= 0xFFFFFF
```

o valor legado pode ser copiado em migration para `rawMotionValue`.

A migration não deve:

- deslocar bits;
- preencher `decodedSteps`;
- derivar distância;
- alterar workflow;
- depender do payload diagnóstico.

## `d4-v2`

O runtime de novas importações usa `parserVersion = d4-v2` e produz
separadamente:

```kotlin
val rawMotionValue = bytes.readUInt24LittleEndian(7)
val decodedSteps = rawMotionValue ushr 16
```

Versão desconhecida falha fechada na criação da atividade; não há reclassificação
silenciosa de dados legados.

## Fronteira da Spec 021 — subtarefa `021-01`

O parser passou a resolver a duração oficial a partir de `startedAt` e `endedAt`
corrigidos. No layout preferido, `packet2[6]` é `endSecond` e qualquer valor fora
de `0..59` falha fechado; não há inferência por `recordCount`, quantidade de
samples ou horário inventado. No layout legado, o segundo continua vindo de
`packet1[14]`.

Esta correção de fonte temporal não ativa uma semântica parcial `d4-v3`: o
coordenador e o runtime efetivo continuam em `d4-v2`. `d4-v3` fica preparado no
domínio para o contrato completo, cuja timeline normalizada pertence à
subtarefa `021-02`. A correspondência de `packet2[6]` foi observada em três
atividades; não constitui garantia universal para firmware não observado.

# Índices, parcialidade e duração

## Estado atual

- pacotes `4+` presentes são ordenados por `minuteIndex`;
- duplicatas usam a primeira ocorrência e tornam o parser parcial;
- lacunas permanecem como lacunas;
- os intervalos brutos mantêm 60 segundos nominais;
- o resolvedor temporal valida índices antes de aplicar a política `T/N/60 s`;
- lacuna, duplicata, primeiro índice não zero ou ordem inconsistente preservam os
  fatos sem receber janelas efetivas sintetizadas;
- séries elegíveis distinguem duração exata, residual, insuficiente e cauda não
  coberta; plausibilidade permanece política separada e tipada;
- a materialização do snapshot e da duração efetiva persistida pertence à
  derivação posterior.

## Contrato planejado V1

A série canônica exige índices:

```text
0, 1, 2, ..., N-1
```

Primeiro índice diferente de zero, lacuna, duplicata ou ordem inconsistente torna
a série inelegível, preservando os fatos raw/measured.

Com duração total `T`, `N` intervalos elegíveis e quantum `Q = 60 s`:

- `T == NQ`: todos usam 60 s;
- `(N−1)Q < T < NQ`: último usa o restante positivo;
- `T <= (N−1)Q`: série inelegível;
- `T > NQ`: cauda permanece sem intervalo sintético.

Exemplo disponível:

```text
T = 1.637 s
27 intervalos completos
último intervalo = 17 s
```

Plausibilidade é avaliada separadamente e nunca redistribui passos.

# Campos e métricas

## Produzidos pelo código atual

| Campo | Estado atual |
| --- | --- |
| início e duração | início e `endedAt` vêm dos cabeçalhos; no preferido, `endedAt` usa `packet1[14..19]` + `packet2[6]` validado |
| passos totais | interpretados do pacote 2 |
| `steps` temporal | contém o u24 empacotado no `d4-v1` |
| distância total | conversão atual de raw, diagnóstica para a Fase 014 |
| distância temporal | conversão atual não comprovada, deve migrar como raw |
| calorias totais | conversão atual; elegibilidade permanece dependente de auditoria própria |
| calorias temporais | conversão atual não comprovada, deve migrar como raw |
| BPM | amostras válidas de 30..220 |
| tipo sugerido | `RUNNING` quando passos totais >= 1000; caso contrário `FREE_WORKOUT` |

## Derivados planejados

```text
canonicalIntervalDistanceMeters = decodedSteps × stepLengthMetersUsed
canonicalDistanceMeters = soma exata dos intervalos canônicos
cadência = passos ÷ duração efetiva em minutos
velocidade = distância ÷ duração efetiva
ritmo = duração efetiva ÷ distância em quilômetros
```

Distância original, calibrada histórica, distância temporal raw e estimativa por
passos totais não são fallback canônico para `WATCH_D3_D4`.

# Persistência atual no Champion Companion

`WorkoutIntervalEntity` armazena:

```text
id
activityId
intervalIndex
durationNanos
steps
distanceMeters
caloriesKilocalories
rawMotionValue
rawDistanceValue
rawCaloriesValue
decodedSteps
```

A chave única `(activityId, intervalIndex)` evita duas linhas do mesmo índice,
mas não exige continuidade.

`WatchWorkoutImportEntity` preserva a `parserVersion` autoritativa da importação.

Durante reimportação, `RoomRepositories.persistImported` substitui todos os
intervalos e amostras BPM da atividade. Por isso, o mapper grava os fatos raw de
`d4-v2` na mesma transação e a migration de compatibilidade preserva o raw
`d4-v1` antes de uma eventual reimportação corrigida.

# Snapshot histórico dos consumidores na auditoria inicial (`014-01`)

| Consumidor | Comportamento atual |
| --- | --- |
| parser | calcula ritmo e velocidade pela distância original total |
| lista/detalhe | prioridade calibrada → original → estimada |
| calendário | prioridade calibrada → original → estimada |
| workflow de esteira | qualquer `TREADMILL` entra em `PENDING_CALIBRATION` |
| Health Connect | um `DistanceRecord` agregado pela prioridade calibrada → original → estimada |
| fila Health Connect | uma operação de atividade por `recordType`; sem filho temporal |

Na auditoria inicial, nenhum consumidor usava uma distância principal igual à
soma dos intervalos canônicos. Esse snapshot não descreve o comportamento
entregue após `014-07` e `014-10`.

## Consumidores entregues na Fase 014

| Consumidor | Comportamento efetivo |
| --- | --- |
| parser/derivação | preserva raw e usa série canônica derivada por intervalo |
| lista, detalhe e calendário | usam distância, métricas e acumulado canônicos; ausência permanece ausente |
| workflow `WATCH_D3_D4` | `TREADMILL` elegível pode avançar sem calibração histórica |
| Health Connect | usa projeções filhas granulares, sem distância original ou calibrada como fallback |
| fila Health Connect | mantém uma operação independente por filho, com retry e reconciliação |

# Health Connect entregue na Fase 014

A projeção efetiva de `WATCH_D3_D4` contém:

```text
EXERCISE_SESSION / singleton
DISTANCE / interval-000..N
SPEED / series
HEART_RATE / series
TOTAL_CALORIES_BURNED / singleton
```

Cada filho possui operação própria, identidade estável, payload congelado e retry
independente. `SpeedRecord` usa amostra no ponto médio do intervalo.

Agregado legado e filhos de distância não podem coexistir após reconciliação.

# D8 e A8 relacionados

| Comando | Evidência atual | Relação com D3/D4 |
| --- | --- | --- |
| D8 | não localizado em fluxo funcional auditado | desconhecida |
| A8 | constante relacionada a esporte atual em referências históricas | não substitui nem complementa D3/D4 sem evidência adicional |

# Lacunas abertas

- significado dos 16 bits inferiores do movimento;
- unidades/semântica de distância e calorias temporais;
- significado de `sportCountRaw` e `pauseRaw`;
- variações de firmware e layouts;
- relação CRC D3/D4 em todos os casos;
- política final de plausibilidade do último bucket;
- cobertura temporal da última amostra BPM;
- retenção final de payload diagnóstico.

Nenhuma dessas lacunas autoriza adivinhação, redistribuição ou fallback para a
distância original.

## Referências

- `docs/integration/014-01-d4-layout-and-consumer-audit.md`
- `docs/decisions/adr-018-canonical-workout-time-series-and-health-projection.md`
- `docs/specs/014-watch-workout-derived-metrics.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`
