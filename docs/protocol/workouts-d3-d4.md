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
- Fase 014: `in_progress` após conclusão documental de `014-01`.
- Treinos phone, GPS, rota, mapa, `ExerciseRoute` e BLE live não fazem parte da
  direção da Fase 014.

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
| duração | diferença positiva entre início/fim; fallback do pacote 3 | interpretação do código |
| `endedAt` no domínio | parser não passa explicitamente o fim resolvido; o domínio usa `startedAt + duration` | perda atual de proveniência explícita |

## Pacote 2 — totais

| Campo | Layout preferido atual | Layout legado atual | Classificação |
| --- | --- | --- | --- |
| passos totais | u16 LE `7..8` | u24 LE `5..7` | comportamento do código; coerente na atividade de referência |
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
| movimento | u24 LE `7..9` | salvo diretamente em `steps` pelo `d4-v1` | raw empacotado; não é contagem direta de passos |
| distância temporal | u24 LE `10..12` | parser atual converte e persiste como distância | valor raw; unidade e semântica não comprovadas |
| calorias temporais | u16 LE `13..14` | parser atual converte e persiste como kcal | valor raw; unidade e semântica não comprovadas |

O comportamento atual de conversão de distância/calorias é conhecido, mas não é
contrato semântico do relógio e não deve alimentar a série canônica da Fase 014.

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

O runtime de produção atual usa `parserVersion = d4-v1`.

Cadeia de persistência auditada:

```text
bytes[7..9]
  → readUInt24LittleEndian(7)
  → SampleData.stepsDelta
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

O parser corrigido será versionado inicialmente como `d4-v2` e deverá produzir
separadamente:

```kotlin
val rawMotionValue = bytes.readUInt24LittleEndian(7)
val decodedSteps = rawMotionValue ushr 16
```

Versão desconhecida falha fechada para reprocessamento automático.

# Índices, parcialidade e duração

## Estado atual

- pacotes `4+` presentes são ordenados por `minuteIndex`;
- duplicatas usam a primeira ocorrência e tornam o parser parcial;
- lacunas permanecem como lacunas;
- todos os intervalos persistidos recebem 60 segundos;
- a duração total é resolvida separadamente;
- não existe duração nominal versus efetiva;
- não existe plausibilidade tipada.

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
| início e duração | interpretados dos pacotes 1/3 |
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
```

A chave única `(activityId, intervalIndex)` evita duas linhas do mesmo índice,
mas não exige continuidade.

`WatchWorkoutImportEntity` preserva a `parserVersion` autoritativa da importação.

Durante reimportação, `RoomRepositories.persistImported` substitui todos os
intervalos e amostras BPM da atividade. Por isso, a migration de compatibilidade
deve preservar o raw `d4-v1` antes de uma eventual reimportação corrigida.

# Consumidores atuais auditados

| Consumidor | Comportamento atual |
| --- | --- |
| parser | calcula ritmo e velocidade pela distância original total |
| lista/detalhe | prioridade calibrada → original → estimada |
| calendário | prioridade calibrada → original → estimada |
| workflow de esteira | qualquer `TREADMILL` entra em `PENDING_CALIBRATION` |
| Health Connect | um `DistanceRecord` agregado pela prioridade calibrada → original → estimada |
| fila Health Connect | uma operação de atividade por `recordType`; sem filho temporal |

Nenhum consumidor atual usa uma distância principal igual à soma dos intervalos
canônicos.

# Health Connect planejado

A projeção futura de `WATCH_D3_D4` deverá conter:

```text
EXERCISE_SESSION / singleton
DISTANCE / interval-000..N
SPEED / series
HEART_RATE / series
TOTAL_CALORIES_BURNED / singleton
```

Cada filho terá operação própria, identidade estável, payload congelado e retry
independente. `SpeedRecord` usará amostra no ponto médio do intervalo.

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
