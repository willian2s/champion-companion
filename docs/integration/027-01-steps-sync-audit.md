# 027-01 — Auditoria de passos D4, Daily Summary e Health Connect

- **Spec:** `027-workout-steps-cadence-1hz-timeline`
- **Ticker:** `027`
- **Status:** concluída documentalmente
- **Base auditada:** branch atual, Room v13, após Spec 023
- **Natureza:** auditoria sem alteração de código, schema ou efeito remoto

## Resultado executivo

A base atual possui sincronização incremental de passos do `DailySummary`, mas
não possui projeção de passos da atividade, `StepsCadenceRecord` ou crédito de
passos ativado após aceite remoto. O checkpoint atual representa o maior total
**enfileirado**, não o maior total aceito pelo Health Connect.

Decisão fechada para a Spec 027:

1. `DailySummary.steps` continua sendo total cumulativo autoritativo.
2. `WorkoutActivity.metrics.steps` é total autoritativo da atividade quando
   válido; `decodedSteps` por intervalo serve para derivação e cadência, não
   substitui o total do pacote.
3. A publicação de passos da atividade será elegível somente com prova local de
   janela segura. Operação diária aceita que sobreponha a atividade bloqueia
   `StepsRecord` da atividade; nenhum readback do Health Connect será usado.
4. Crédito só nasce após `StepsRecord` da atividade ter aceite local observável
   (`SYNCED` + `AFTER_REMOTE_EFFECT`/`SENT`). Operação pendente ou falha não
   reduz o próximo delta diário.
5. Atividade que cruza data local, dados D4 parciais ou janela residual sem
   representação não sobreposta falham fechados.
6. O crédito será persistido em entidade Room própria, não em campos com
   semântica de `DailyStepsSyncPayload` nem em `HealthSyncOperation` como soma
   global. A evolução será aditiva em task posterior; esta auditoria não cria
   migration.
7. Cadência permanece independente da contabilidade de passos.

Nenhuma escrita remota nova foi introduzida. O comportamento atual continua
sem publicar passos/cadência de atividade.

## 1. Daily Summary

### Estrutura e autoridade

O armazenamento local mantém um `DailySummary` cumulativo por
`deviceId + origin + localDate` (`docs/specs/016-health-connect-incremental-daily-steps.md:15-18`).
O merge atual somente eleva `steps`; redução ou igualdade preserva o resumo
existente (`app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt:384-400`).

O total é enfileirado quando a leitura não reduz o resumo. A persistência do
resumo e a criação/reparo da operação ocorrem dentro de `write`, que usa
`database.withWriteTransaction` (`RoomRepositories.kt:402-409,4375-4381`).

### Payload, janela, fuso e ordinal

`DailyStepsSyncPayload` congela:

- `kind`: `INCREMENTAL` ou `LEGACY_BRIDGE`;
- `sequence`;
- `previousTotalSteps`, `currentTotalSteps` e `stepDelta`;
- `startTime`, `endTime` e `timeZoneId`.

O domínio exige total crescente, delta positivo, janela crescente, fuso
resolvível e `sequence > 0` para incremento (`app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt:480-509`).
O primeiro incremento começa no início da data local; os seguintes começam no
`endTime` anterior. `isValidDailyStepsChain` exige mesmo fuso, primeiro total
anterior igual a zero, ordinais contíguos e janelas contíguas
(`RoomRepositories.kt:3964-3983,4747-4761`).

`resolveDailyStepsWindowEnd` rejeita coleta cujo instante esteja em outra data
local (`RoomRepositories.kt:4020-4029`). Portanto, Daily não cria janela que
atravesse meia-noite local.

### Checkpoint e transação

O checkpoint operacional não é uma coluna separada: é derivado do maior
`currentTotalSteps` entre payloads incrementais persistidos. A nova operação
usa `previousTotalSteps` desse último payload e `currentTotalSteps` do resumo
(`RoomRepositories.kt:3954-4008`). A igualdade ou redução não gera operação.

O `StepsRecord` é mapeado exclusivamente do payload congelado, com
`stepDelta`, offsets do fuso no início/fim e identidade persistida
(`app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt:116-145`).
O mapper não relê `DailySummary.steps`.

O ponto importante para 027 é a fronteira: o checkpoint avança ao persistir a
operação local, antes de qualquer efeito remoto. O coordinator só depois marca
`BEFORE_REMOTE_EFFECT`, chama o gateway e, em aceite, grava `SYNCED`,
`SENT`/`OBSOLETE` e `AFTER_REMOTE_EFFECT`
(`RoomRepositories.kt:2511-2568`; `HealthConnectSyncCoordinator.kt:387-445`).
Não existe hoje crédito de atividade associado a essa transição.

### Ponte legada, retry e reconciliação

Uma operação cumulativa sem payload é reconhecida como legado. A reconciliação
cria uma ponte única `LEGACY_BRIDGE`, preserva a identidade legada e não
mistura ponte com incrementos posteriores
(`RoomRepositories.kt:3896-3951,4733-4745`). Mistura, ordinal inválido ou
cadeia temporal não contígua falham fechados.

Retry conserva `clientRecordId`, `clientRecordVersion` e payload. A falha
parcial é por operação; uma operação aceita remotamente cujo estado local não
seja marcado como `SYNCED` permanece retryável com a mesma identidade
(`HealthConnectSyncCoordinator.kt:407-428`). Isso é suficiente para idempotência
do Daily, mas não para ativar crédito de uma atividade sem um ledger próprio.

## 2. WorkoutActivity e D3/D4

### Totais e passos por intervalo

O parser lê o total de passos de `Packet 2` e o congela em
`WorkoutMetrics.steps` (`ChampionD4WorkoutParser.kt:154-170`). Esse total é a
autoridade de quantidade para um futuro `StepsRecord` da atividade, quando a
atividade for elegível.

Para `d4-v2` e `d4-v3`, cada intervalo decodifica
`rawMotionValue ushr 16` em `decodedSteps`; `d4-v1` mantém o valor legado sem
decodificação (`ChampionD4WorkoutParser.kt:34-42,109-121`; `WatchWorkoutDerivedMetrics.kt:290-311`).
O domínio valida que `decodedSteps` coincida com o raw e com `interval.steps`
quando presente (`WatchWorkoutActivity.kt:810-830`). Não há validação atual que
exija que a soma dos intervalos coincida com `metrics.steps`.

Assim:

- `metrics.steps` é total do pacote e autoridade do total da atividade;
- soma de `decodedSteps` é evidência temporal para derivação, distância e
  cadência;
- divergência, ausência de decodificação ou série parcial não autoriza inventar
  passos nem substituir o total por uma soma incompleta.

### Duração e janelas

O runtime efetivo usa parser `d4-v3` e derivação `watch-d3-d4-v2`. O parser
resolve duração autoritativa pelo cabeçalho/layout preferido, exige `endSecond`
válido nesse layout e normaliza todos os intervalos sobre a duração oficial
(`ChampionD4WorkoutParser.kt:48-77,88-121,351-358`).

`WorkoutDurationResolver` exige índices contínuos começando em zero, passos
decodificados em todos os intervalos, duração positiva e janelas contíguas. Ele
distribui a duração oficial de forma determinística, sem criar cauda fora da
sessão (`WatchWorkoutDerivedMetrics.kt:386-420,450-559`). Intervalo ausente,
duplicado, fora de ordem ou sem `decodedSteps` torna a série inelegível; os
fatos persistidos permanecem preservados.

O parser parcial retorna intervalos presentes (`ChampionD4WorkoutParser.kt:172-213`)
e `persistImported` os persiste na transação de importação
(`RoomRepositories.kt:648-725`). Logo, futura publicação de passos da atividade
deve exigir elegibilidade explícita e não pode tratar conjunto parcial como
cobertura temporal completa.

### Reimportação, identidade e meia-noite

O índice Room `(activityId, intervalIndex)` impede duplicata persistida, mas não
preenche lacunas (`ChampionDatabase.kt:286-310`). Reimportação mantém identidade
natural e preserva `recordedAt` quando revisão/workflow são preservados; remove e
recria derivados somente por ação explícita, conforme ADR-016 e ADR-018.

Não há política de crédito para atividade que atravesse meia-noite. O Daily
valida cada janela contra uma única data local, e o detalhe/calendário usa
`startedAt` como data da atividade. Para 027, qualquer atividade cujo intervalo
`[startedAt, endedAt)` atravesse uma mudança de data no fuso do Daily é
inelegível para `WorkoutSteps` e para crédito diário; não dividir nem atribuir
silenciosamente a um dos dias.

### Tipos aplicáveis

O mapper atual trata a série canônica para atividades watch não-CYCLING e usa
distância manual para `CYCLING` (`HealthRecordMapper.kt:550-575`). Não existe
filho nem predicado separado para passos. A política fechada para o futuro é
reutilizar a proveniência `WATCH_D3_D4` e excluir `CYCLING`; nenhum tipo phone,
GPS, rota ou localização entra nesta spec. `StepsCadenceRecord` usa somente
intervalos D4 válidos e não consome crédito.

## 3. Health Connect e fila local

### Chaves, tipos e permissões atuais

`HealthProjectionChildKey` atual contém sessão, distância, velocidade, BPM e
calorias; não contém `WorkoutSteps` nem `StepsCadenceSeries`
(`app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HealthProjectionContracts.kt:9-80`).
`HealthSyncRecordType` contém `STEPS`, mas não `STEPS_CADENCE`
(`core/model/HeartRateAndSync.kt:177-186`).

O par `WORKOUT_ACTIVITY + STEPS` é rejeitado por `HealthSyncOperation.supports`;
atividade aceita sessão, distância, calorias, BPM e velocidade
(`HeartRateAndSync.kt:822-854`). O gateway conhece `StepsRecord`, não
`StepsCadenceRecord`, e as permissões atuais têm `WRITE_STEPS`, sem permissão de
cadência (`core/healthconnect/HealthConnectGateway.kt:7-36,202-211`).

### Payload, identidade e ordenação

Filhos atuais usam `projectionChildKey`, payload congelado, origem, versão
monotônica e checkpoint local. A chave Room única inclui agregado, tipo, filho e
`operationSequence` (`ChampionDatabase.kt:622-670`). O stable ID é derivado de
application ID, agregado, tipo e filho (`RoomRepositories.kt:4367-4371`).

Essa infraestrutura é compatível com novos filhos, mas o payload atual não
possui quantidade/janela de passos da atividade nem estado de crédito. Reutilizar
`dailyStepsPayload` para isso confundiria dois agregados e violaria sua
validação.

O coordinator processa cada operação independentemente. Deleções exatas vêm
antes de inserts; distância/velocidade têm prioridade de ordenação, e os demais
filhos vêm depois (`HealthConnectSyncCoordinator.kt:956-969`). Não há regra
específica de precedência entre futura operação `WorkoutSteps` e Daily.

### Aceite remoto e falha parcial

O gateway é write-only: `insertRecords` retorna `Accepted` ou falha categorizada;
não há readback remoto (`HealthConnectGateway.kt:71-80,106-146`). Portanto,
"aceite" significa apenas retorno aceito pelo gateway e checkpoint local
posterior. Não é prova de conteúdo remoto observado.

Crédito de atividade será elegível somente depois dessa transição local. Se a
escrita falhar, for cancelada ou o checkpoint local não puder ser salvo, o
crédito permanece ausente/inativo. Demais filhos da atividade continuam
independentes; falha de passos não bloqueia cadência, BPM ou sessão elegíveis.

## 4. Política de cobertura e residual

### Prova local de cobertura

Uma atividade é considerada coberta para fins de bloqueio somente por operação
própria do Champion Companion que satisfaça simultaneamente:

- agregado `DailySummary` do mesmo dispositivo/origem e mesmo `localDate`;
- `recordType = STEPS`, ação `INSERT` e payload incremental/ponte válido;
- `state = SYNCED`;
- `reconciliationCheckpoint = AFTER_REMOTE_EFFECT` e estado de reconciliação
  `SENT`;
- fuso válido e janela semiaberta `[startTime, endTime)` válida.

Operação `PENDING`, `FAILED`, `SYNCING` ou apenas enfileirada não cobre a
atividade. Não consultar records do Health Connect nem records de terceiros.

Operação Daily legada aceita sem payload também não prova cobertura: ela deve
ser primeiro reconciliada para `LEGACY_BRIDGE` com janela e total congelados.
Enquanto a ponte não existir, a cobertura é desconhecida e `WorkoutSteps` fica
bloqueado.

Se a janela aceita do Daily se sobrepuser à janela da atividade no mesmo fuso,
`WorkoutSteps` fica `ALREADY_COVERED`/bloqueado. A regra é deliberadamente
conservadora: uma janela diária extensa não prova quais passos intradiários
foram contados, mas bloquear evita duplicidade. Se não houver sobreposição e os
demais critérios forem válidos, a atividade pode ser candidata à publicação em
task posterior.

Atividade que cruza data local, janela inválida, Daily pendente concorrente,
parser parcial ou dados que não permitam provar não sobreposição falha fechada.

### Ordem atividade → Daily

Quando `WorkoutSteps` for aceito primeiro, o ledger futuro cria crédito por
`activityId + localDate + projectionChildId`, congelando quantidade real,
janela, fuso, identidade/versionamento remoto e estado de consumo. O crédito só
fica `ACTIVE` após aceite remoto.

Ao processar Daily, a transação calcula:

```text
deltaBruto = currentTotalSteps - lastEnqueuedTotalSteps
residual   = deltaBruto - créditos ACTIVE do mesmo dia
```

Regras:

- `deltaBruto <= 0` não cria record nem total negativo;
- `residual < 0` bloqueia o efeito e não avança checkpoint;
- `residual > 0` só publica se houver janela residual válida, não sobreposta e
  contida na mesma data local;
- `residual == 0` pode avançar checkpoint sem criar `StepsRecord` zero, mas
  somente na mesma transação que consome créditos já aceitos;
- se nenhuma representação segura existir, Daily não publica o residual, não
  consome crédito e não avança checkpoint; o total local continua fonte
  autoritativa e o estado fica observável para retry/decisão explícita.

A aproximação temporal permitida é atribuir o residual à janela posterior ao
último fim de atividade aceito, desde que `dailyEnd` seja posterior, a janela
permaneça no dia local e não sobreponha nenhuma atividade creditada. Não
distribuir passos por intervalo desconhecido, não criar amostra e não alterar
records remotos históricos.

A serialização local é obrigatória: a transação que confirma o efeito aceito e
ativa o crédito deve terminar antes de uma transação Daily calcular o residual.
O caminho inverso também adquire a mesma exclusão lógica antes de preparar
`WorkoutSteps`. O efeito remoto fica fora da transação Room, mas nenhum crédito
é ativado antes de `markSynced`/`AFTER_REMOTE_EFFECT` persistido.

### Ordem Daily → atividade

Daily aceito que se sobreponha à atividade bloqueia somente o filho
`WorkoutSteps`. Cadência D4 e demais filhos elegíveis continuam independentes.
Não apagar, repartir ou reescrever automaticamente o Daily já aceito.

### Persistência escolhida

O crédito será entidade Room própria, com índice único lógico por atividade/dia e
campos para atividade, data local, quantidade, janela, fuso, identidade do
`WorkoutSteps`, versão, estado (`PENDING`, `ACTIVE`, `CONSUMED`, `BLOCKED`) e
ordinal Daily consumido. A operação Health Connect continuará sendo a fonte do
aceite remoto; o ledger não será um contador global.

Essa evolução exige migration aditiva a partir da versão 13 e schema exportado,
mas pertence a `027-02`. Nenhuma coluna atual será reinterpretada e nenhuma
migration é necessária para concluir `027-01`.

## 5. Cadência e limites

Cadência não participa de total, crédito, checkpoint ou cobertura. Para o primeiro
Gate A, a fonte é cada intervalo D4 elegível:

```text
cadenceSpm = decodedSteps / effectiveDurationSeconds * 60
```

A janela e o instante representativo usam a timeline normalizada; midpoint é a
convenção já usada para derivados temporais (`ChampionD4WorkoutParser.kt:123-134,400-425`).
Intervalo inválido, passo ausente, lacuna ou fora da sessão não cria amostra.

Timeline 1 Hz, suavização, reamostragem e eventual promoção de cadência 1 Hz
ficam fora desta subtarefa. Dados D4 raw/measured e snapshots históricos não são
reescritos.

## 6. Estado do schema e lacunas

Room está na versão 13. O schema atual comporta payload incremental diário,
filhos genéricos e checkpoints, mas não representa com segurança:

- `WorkoutSteps`/`StepsCadenceSeries`;
- `StepsCadenceRecord` e permissão correspondente;
- cobertura/credito por atividade e data;
- consumo transacional do crédito;
- distinção entre total autoritativo e soma parcial para publicação.

Não foram encontrados testes automatizados específicos para atividade antes/depois
do Daily, múltiplas atividades, meia-noite, crédito pós-aceite, cadência Health
Connect ou falha parcial de novos filhos. A política não depende de criar ou
executar testes nesta auditoria; a Spec 027 proíbe isso sem pedido explícito.

## Aceite da subtarefa

| Critério | Resultado |
| --- | --- |
| Premissas da Spec 027 confirmadas contra código real | PASS, com lacunas registradas |
| Política de cobertura/residual fechada | PASS, fail-closed e sem readback |
| Total diário e checkpoint documentados | PASS |
| D4 total, intervalos, duração e parcialidade documentados | PASS |
| Identidade, payload, mapper, gateway e falha parcial documentados | PASS |
| Persistência escolhida sem migration prematura | PASS |
| Nenhuma escrita remota nova | PASS |
| Dados de origem não reescritos | PASS |

## Riscos residuais

1. O gateway não oferece readback; aceite continua evidência de retorno do
   provider, não observação do record remoto.
2. Janela diária extensa pode bloquear uma atividade realmente não contada; essa
   subcontagem potencial é preferida à duplicidade e deve ser observada no Gate A.
3. Um Daily residual inseguro pode deixar checkpoint local parado até uma
   representação segura ou ação explícita.
4. A compatibilidade real de `StepsCadenceRecord` com Health Sync/Strava ainda
   não foi validada fisicamente.
5. Divergência entre total do pacote e soma de intervalos D4 continua lacuna
   factual; nenhuma camada deve reconciliá-los por heurística.
