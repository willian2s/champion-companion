# 014-01 — Auditoria D4, consumidores, legado e protocolo

- **Fase:** 014
- **Task:** 014-01
- **Data:** 2026-08-05
- **Branch analisada:** `feat/014-watch-workout-derived-metrics`
- **Base funcional do código:** `main` no commit `6ec14990e20a31374857ca106ab9793e144868ee`
- **Natureza:** auditoria documental, sem alteração de código, schema ou comportamento

## Resultado executivo

A auditoria confirma que o runtime atual preserva informação suficiente para uma
ponte controlada de atividades `d4-v1`, mas mistura no mesmo modelo valores raw,
interpretações não comprovadas e métricas calculadas.

Decisões factuais resultantes:

1. O parser `d4-v1` lê o u24 little-endian de `bytes[7..9]` e o persiste no campo
   denominado `steps` sem preservar uma cópia raw separada.
2. Para atividade `WATCH_D3_D4` cujo `WatchWorkoutImport.parserVersion` seja
   exatamente `d4-v1`, `WorkoutIntervalEntity.steps` é recuperável como
   `rawMotionValue`. A cópia em migration é uma ponte raw, não uma decodificação.
3. O byte superior do u24 reproduz a sequência observada de passos; os 16 bits
   inferiores continuam sem semântica comprovada.
4. Índices ausentes ou duplicados são detectáveis, mas o parser atual ainda
   produz intervalos com os pacotes presentes. A série canônica V1 deve falhar
   fechada nesses casos.
5. Todos os intervalos atuais são persistidos com 60 segundos; duração residual,
   elegibilidade temporal e plausibilidade ainda não existem no domínio.
6. UI, calendário e Health Connect priorizam distância calibrada/original antes
   da estimada. Não existe consumidor canônico baseado na soma dos intervalos.
7. `TREADMILL` sempre entra em `PENDING_CALIBRATION`, inclusive quando a origem é
   `WATCH_D3_D4`.
8. A fila Health Connect possui uma operação de atividade por `recordType`; a
   chave de domínio não possui filho. `operationSequence` existe no banco, mas
   não representa filhos temporais de treino.
9. O mapper de atividade produz um único `DistanceRecord` agregado e o
   coordinator insere exatamente um record por operação.

Esses fatos validam as fronteiras planejadas para `014-02` e não autorizam
interpretação adicional do protocolo.

## 1. Transporte e remontagem D4

### Código atual

| Etapa | Símbolo | Comportamento encontrado |
| --- | --- | --- |
| Comando | `ChampionD4WorkoutBlockProtocol.createBlockCommand` | Envia `D4 <blockIndex> 00 00`, com índice limitado a um byte e sem retry interno. |
| Cabeçalho | `ChampionD4WorkoutBlockProtocol.parsePayload` | Lê `packetIndex` em `bytes[1..2]` e `totalPackets` em `bytes[3..4]`, ambos u16 little-endian. |
| Coleta | `BlockCollector` | Mantém todos os pacotes recebidos, registra índices duplicados, calcula ausentes em `1..highestExpectedPacketCount` e ordena por índice no fechamento. |
| Encerramento | `BlockCollector.onPacket` | Finaliza quando `packetIndex >= totalPackets` e `totalPackets > 0`; timeout preserva o conjunto recebido. |
| Pacotes obrigatórios | `ChampionD4WorkoutParser.parse` | Pacotes 1, 2 e 3 ausentes invalidam o bloco. |
| Duplicatas no parser | `ChampionD4WorkoutParser.parse` | Agrupa por índice e usa a primeira ocorrência; duplicata permanece registrada no summary e torna o resultado parcial. |
| Amostras | `ChampionD4WorkoutParser.parse` | Usa pacotes `4+`, mantém a primeira ocorrência por `packetIndex`, define `minuteIndex = packetIndex - 4` e ordena por minuto. |

### Consequência para a série V1

O runtime atual consegue detectar:

- pacote obrigatório ausente;
- índice ausente dentro da faixa declarada;
- índice duplicado;
- timeout;
- quantidade de amostras divergente de `recordCount`;
- CRC do pacote 3 divergente do D3.

Entretanto, `ParserValidity.PARTIAL` ainda permite persistir os intervalos
presentes. A Fase 014 precisa manter a atividade e os fatos para revisão, mas
tratar lacuna, duplicata, primeiro índice diferente de zero ou ordem inconsistente
como impedimento da série canônica V1.

## 2. Layout atual dos pacotes

### Pacote 1 — início e fim

| Item | Código atual | Confirmação |
| --- | --- | --- |
| Layout preferido | início em `7..13`; fim em `14..19`; anos u16 LE | Heurística confirmada no código; layout do dispositivo não está universalmente comprovado. |
| Layout legado | offsets alternativos em `5..19` | Fallback confirmado no código; sem prova de cobertura de firmware. |
| Duração | prefere diferença positiva entre início/fim; depois fallback do pacote 3 | Comportamento do código. |
| Fim persistido | `WorkoutMetrics.endedAt` cai no default `startedAt + duration` porque o parser não passa `resolvedEndedAt` ao construtor | Perda de intenção explícita no mapper atual; normalmente coincide com a duração resolvida. |

### Pacote 2 — totais

| Campo | Layout preferido atual | Layout legado atual | Classificação |
| --- | --- | --- | --- |
| passos totais | u16 LE em `7..8` | u24 LE em `5..7` | Interpretação do código; total observado é coerente no caso de referência. |
| distância total | u16 LE em `11..12`, multiplicado por 100 antes da conversão | u32 LE em `8..11` | Unidade/layout do relógio não comprovados de forma geral. |
| calorias totais | u24 LE em `15..17` | u32 LE em `12..15` | Unidade/layout do relógio não comprovados de forma geral. |

A escolha entre os layouts usa somente faixas de plausibilidade. Quando ambos são
plausíveis, o código escolhe o legado.

### Pacote 3 — contagem, CRC e fallback

| Campo | Código atual | Classificação |
| --- | --- | --- |
| `recordCount` preferido | byte 9 | Heurística do código. |
| CRC preferido | u16 LE em `14..15` | Heurística do código. |
| fallback legado | duração u32 LE `5..8`, contagem u16 LE `9..10`, CRC u16 LE `11..12` | Heurística do código. |

### Pacotes 4+ — amostras temporais

| Campo | Leitura atual | Uso atual | Estado após auditoria |
| --- | --- | --- | --- |
| BPM | byte 6 | aceita apenas 30..220 e cria amostra em `startedAt + minuteIndex × 60 s` | Medido quando válido; alinhamento temporal continua dependente do bucket D4. |
| movimento | u24 LE `7..9` | salvo diretamente como `stepsDelta` | Raw empacotado; não é contagem direta de passos. |
| distância temporal | u24 LE `10..12` | convertida e persistida como metros | Valor raw com unidade/semântica não comprovadas; uso semântico atual é dívida. |
| calorias temporais | u16 LE `13..14` | convertida e persistida como kcal | Valor raw com unidade/semântica não comprovadas; uso semântico atual é dívida. |

## 3. Campo empacotado de movimento

Exemplo sanitizado da evidência disponível:

```text
rawMotionValue = 6.553.643
hex            = 0x0064002B
decodedSteps   = rawMotionValue >> 16 = 100
lower16        = 0x002B = 43, significado desconhecido
```

Sequência externa observada após o deslocamento de 16 bits:

```text
100, 117, 114, 117, 103, 109, 133, 131, 130, 175, 175, 152,
123, 116, 176, 169, 121, 155, 184, 178, 136, 115, 120, 110,
127, 122, 125, 104
```

Soma externa, sem persistência ou alteração de dados:

```text
3.737 passos
```

A soma coincide com o total da atividade de referência. Isso confirma a regra de
decodificação para o caso observado, mas não atribui significado aos 16 bits
inferiores nem prova todos os firmwares.

## 4. Parser version e recuperabilidade do legado

### Versionamento encontrado

- `ChampionWatchWorkoutImportCoordinator` usa por padrão
  `ParserVersion.create("d4-v1")`.
- A versão é propagada para `WatchWorkoutParserInput`.
- `WatchWorkoutImportEntity.parserVersion` é a versão autoritativa persistida por
  importação.
- `WorkoutActivityEntity.parserVersion` mantém snapshot legado para
  compatibilidade, mas o mapper de domínio usa `watch_workout_imports` quando a
  relação existe.
- A busca textual do código de produção encontrou somente `d4-v1` como versão do
  parser de treino atualmente ativa.

### Caminho atual de persistência

```text
bytes[7..9]
  → readUInt24LittleEndian(7)
  → SampleData.stepsDelta
  → WorkoutInterval.steps
  → WorkoutIntervalEntity.steps
```

Não há transformação intermediária, truncamento ou conversão de unidade nesse
caminho. O campo Room é `Long`, portanto comporta integralmente o u24.

### Conclusão da ponte

A ponte abaixo é factual e segura quando todas as condições forem atendidas:

```text
activity.origin == WATCH_D3_D4
watch_workout_imports.parserVersion == "d4-v1"
interval.steps está dentro do intervalo u24
```

Então:

```text
rawMotionValue = legacy interval.steps
decodedSteps   = ausente durante a migration
```

A migration não deve aplicar `>> 16`, criar distância ou alterar workflow. A
decodificação pertence ao reprocessamento explícito. Versão desconhecida, origem
phone, valor negativo ou acima de `0xFFFFFF` falham fechados.

### Limitação de reimportação

`RoomRepositories.persistImported` remove todos os intervalos e amostras BPM da
atividade antes de inserir o conjunto recebido. Portanto, uma reimportação após a
correção pode substituir o legado, enquanto a migration precisa preservar o que
existe antes dessa substituição. O payload diagnóstico não pode ser requisito de
recuperação porque possui política de retenção própria.

## 5. Modelo e persistência atuais

### Domínio

`WorkoutInterval` contém somente:

```text
index
duration
steps
distanceMeters
caloriesKilocalories
```

Não contém:

- valor de movimento raw;
- componente inferior desconhecido;
- duração nominal versus efetiva;
- timestamps explícitos;
- elegibilidade temporal;
- plausibilidade;
- distância canônica;
- acumulado ou métricas derivadas versionadas.

### Room v8

`WorkoutIntervalEntity` contém:

```text
id
activityId
intervalIndex
durationNanos
steps
distanceMeters
caloriesKilocalories
```

O índice único `(activityId, intervalIndex)` impede duas linhas persistidas para o
mesmo intervalo, mas não impede lacuna nem exige início em zero.

`WorkoutActivityEntity` contém totais e métricas importadas/calibradas no mesmo
registro. Não existe snapshot de derivação ou indicador de fonte canônica.

## 6. Duração, índices e BPM

### Duração

O parser atual cria todos os intervalos com:

```text
Duration.ofSeconds(60)
```

A duração total da atividade é resolvida separadamente. Não existe reconciliação
entre `T`, quantidade de intervalos e 60 segundos.

Para a atividade de referência:

```text
T = 1.637 s
28 intervalos × 60 s = 1.680 s
```

A política planejada de 27 intervalos completos e último de 17 segundos não está
implementada.

### Índices

- O collector calcula ausentes usando a faixa de pacotes `1..totalPackets`.
- O parser transforma pacote `4` em minuto `0`.
- Pacotes presentes são ordenados por `minuteIndex`.
- Duplicatas usam a primeira ocorrência e tornam o parser parcial.
- Lacuna não gera bucket sintético; os índices originais permanecem.
- Nenhuma camada atual exige continuidade `0..N-1` antes de exibir ou enviar a
  atividade.

### BPM

- Uma amostra válida é posicionada no início nominal do bucket:
  `startedAt + minuteIndex × 60 s`.
- O detalhe calcula BPM do intervalo por inclusão do timestamp em
  `[start, end)`, acumulando as durações atuais dos intervalos.
- O mapper Health Connect envia as amostras reais existentes; não existe BPM por
  split.
- Sample-and-hold e ponderação temporal ainda não existem.

## 7. Consumidores de distância, ritmo e velocidade

| Consumidor | Prioridade atual | Consequência |
| --- | --- | --- |
| Parser D4 | calcula ritmo e velocidade com distância original total | Métricas importadas nascem vinculadas à distância do relógio. |
| `WorkoutDistanceMetrics.toDistanceLabel` | calibrada → original → estimada | Lista e detalhe mostram original/calibrada como distância final. |
| `toImportedDistanceLabel` | original → estimada | Detalhe explicita a distância original como importada. |
| `displayAveragePaceLabel` | métrica calibrada → ritmo importado | Ritmo continua baseado na calibração ou distância original. |
| `displayAverageSpeedLabel` | métrica calibrada → velocidade importada | Velocidade continua baseada na calibração ou distância original. |
| `CalendarWorkoutSummaryRecord.toCalendarSummary` | calibrada → original → estimada | Calendário usa a mesma prioridade legada. |
| `HealthRecordMapper.finalDistanceMeters` | calibrada → original → estimada | Health Connect envia distância não canônica para `WATCH_D3_D4`. |
| `HealthRecordMapper.mapWorkoutDistance` | um total sobre toda a sessão | Não preserva série temporal ou splits. |

Não foi encontrado consumidor funcional que calcule a distância principal como
soma de passos decodificados por intervalo.

## 8. Workflow atual de esteira

| Origem | Tipo confirmado | Estado atual | Calibração exigida |
| --- | --- | --- | --- |
| `WATCH_D3_D4` | `TREADMILL` | `PENDING_CALIBRATION` | Sim, pelo contrato atual. |
| `WATCH_D3_D4` | outro tipo válido | `READY_TO_SYNC` | Não. |
| phone histórico | `TREADMILL` | contrato histórico de calibração | Preservado, fora da Fase 014. |

`WorkoutActivity.confirmReview` decide apenas pelo tipo e não considera origem ou
existência de série canônica. `markCalibrated`/`applyCalibration` exigem distância
calibrada para chegar a `READY_TO_SYNC`.

A `014-02` precisa introduzir uma regra específica para `WATCH_D3_D4`: esteira com
série canônica elegível avança sem depender da calibração histórica. A mudança não
pode alterar atividades phone legadas.

## 9. Fila e mapper Health Connect

### Identidade atual

`HealthSyncOperationKey` possui apenas:

```text
aggregate
recordType
```

Para atividade, os tipos suportados são:

```text
EXERCISE_SESSION
DISTANCE
TOTAL_CALORIES_BURNED
HEART_RATE
```

`SPEED` não existe.

### Persistência atual

`health_sync_operations` possui índice único:

```text
aggregateId + recordType + operationSequence
```

`operationSequence` foi introduzido para incrementos de passos diários. A chave de
domínio e `prepareActivityOperations` não fornecem um discriminador de filho de
atividade, portanto a cardinalidade efetiva atual continua uma operação por tipo
de record da atividade.

### Execução atual

- `prepareActivityOperations` percorre um conjunto de `recordType`.
- `ensureOperation` recebe uma chave sem filho.
- `loadAndMap` produz um `HealthConnectMappedRecord`.
- O coordinator executa `gateway.insert(listOf(mapped.value.record))`.
- Sucesso ou falha é registrado na operação inteira.

O modelo atual não consegue representar retry independente de vários
`DistanceRecord` dentro do mesmo tipo sem introduzir `projectionChildKey` ou uma
fila interna adicional. A decisão da ADR-018 por uma operação por filho é
compatível com o fluxo existente e exige evolução posterior de domínio/schema.

## 10. Inventário de testes existentes

A auditoria encontrou suites relevantes, sem executá-las:

- `core/bluetooth/ChampionD4WorkoutParserTest.kt`;
- `core/model/WatchWorkoutActivityTest.kt`;
- `data/DatabaseMappersTest.kt`;
- `data/RoomRepositoriesWatchWorkoutImportTest.kt`;
- `feature/exercises/ChampionWatchWorkoutImportCoordinatorTest.kt`;
- `core/model/HeartRateAndSyncTest.kt`;
- testes do mapper/coordinator Health Connect existentes no diretório
  `core/healthconnect`, quando aplicáveis.

Nenhum teste ou fixture foi criado, alterado ou executado nesta task.

## 11. Matriz de classificação

| Afirmação | Código atual | Dispositivo confirmado | Desconhecido | Derivado planejado |
| --- | --- | --- | --- | --- |
| D4 usa F008 e cabeçalho com índices u16 LE | Sim | Validado no fluxo físico histórico | Variações de firmware | Não |
| Pacotes 1–3 são obrigatórios no parser | Sim | Não é afirmação do relógio | Se blocos reais podem omitir algum pacote | Não |
| u24 `bytes[7..9]` é armazenado como passos | Sim | Não | Significado integral do u24 | Não |
| byte superior representa passos no caso observado | Não no parser atual | Evidência da atividade de referência | Generalização para todo firmware | `decodedSteps` |
| 16 bits inferiores | Preservados apenas dentro do inteiro legado | Não | Significado | Nenhum até prova |
| distância/calorias temporais têm unidade atual | Código assume unidade | Não | Unidade e layout semântico | Permanecem raw |
| intervalo dura sempre 60 s | Sim | Não | Política real do último bucket | Duração efetiva residual |
| índices presentes podem ter lacunas | Sim | N/A | Semântica do período ausente | Série V1 inelegível |
| distância principal prioriza calibrada/original | Sim | N/A | N/A | Soma canônica dos intervalos |
| BPM por split | Não existe | N/A | N/A | Sample-and-hold ponderado |
| distância granular no Health Connect | Não existe | N/A | N/A | Um filho por intervalo positivo |

## 12. Fronteiras obrigatórias para 014-02

A próxima task deve fechar contratos que garantam:

1. separação entre raw, measured, derived e projection;
2. ponte `d4-v1` condicionada por origem, parser version e faixa u24;
3. `d4-v2` como versão inicial do parser corrigido;
4. índices contínuos `0..N-1` como pré-condição da série V1;
5. duração efetiva independente de plausibilidade;
6. motivo tipado para lacuna, duplicata, duração incompatível e métrica
   implausível;
7. distância canônica total exatamente igual à soma dos intervalos;
8. regra de `TREADMILL` específica para `WATCH_D3_D4`;
9. BPM por split com fronteiras semiabertas e sample-and-hold;
10. `SpeedRecord` no ponto médio do intervalo;
11. `projectionChildKey` e operação própria por filho Health Connect;
12. ausência segura quando fatos necessários não estiverem disponíveis.

## 13. Perguntas residuais não bloqueadoras

- significado dos 16 bits inferiores de `rawMotionValue`;
- unidades e significado de distância/calorias temporais;
- relação de `sportCountRaw` e `pauseRaw` com pausas ou esporte;
- comportamento em outros firmwares/layouts;
- razão física da cadência implausível no último bucket residual;
- limiar final de plausibilidade e política de aceite remoto;
- cobertura temporal da última amostra BPM dentro da sessão.

Essas lacunas não autorizam adivinhação, redistribuição ou fallback para distância
original.

## 14. Aceite da auditoria

| Critério da task | Resultado |
| --- | --- |
| Caminhos e símbolos exatos inventariados | PASS |
| Raw, passos e componente inferior separados | PASS |
| Sequência externa fecha em 3.737 | PASS |
| Ponte `d4-v1 steps → rawMotionValue` confirmada | PASS, sob guardas explícitas |
| Lacunas/duplicatas identificadas como bloqueadores V1 | PASS |
| Workflow atual de esteira identificado | PASS |
| Cardinalidade atual da fila registrada | PASS |
| Distância/calorias temporais classificadas como raw | PASS |
| Código/schema/testes inalterados | PASS |

A task está documentalmente apta a ser marcada como concluída após alinhamento do
protocolo, source map e overview.
