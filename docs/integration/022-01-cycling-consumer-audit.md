# Auditoria de consumers de bicicleta — 022-01

- **Spec:** `022-cycling-workouts`
- **Subtarefa:** `022-01`
- **Status do relatório:** concluído
- **Data da auditoria:** 2026-08-11
- **Escopo:** leitura do código atual; nenhum código funcional alterado

## Resultado executivo

O domínio atual não possui `WorkoutType.CYCLING`, `manualDistanceMeters` ou
`HealthProjectionChildKey.ManualDistance`. A série canônica atual é derivada de
passos e é consumida por detalhe, intervalos, splits, calendário e projeções
Health Connect sem filtro por tipo confirmado. Esses são os principais pontos de
risco para `022-02..04`.

O relógio continua sendo a única origem elegível para o novo tipo. A seleção de
`CYCLING` não deve ser adicionada aos fluxos de treino phone, que permanecem
descontinuados. A política congelada nesta auditoria é **limpar distância manual
ao salvar revisão com tipo diferente de `CYCLING`**. Assim, walking, running,
treadmill e free workout não podem consumir valor digitado para bicicleta.

## Contratos atuais localizados

| Contrato | Arquivo e símbolos | Comportamento atual | Decisão para ciclismo |
| --- | --- | --- | --- |
| Tipo | `core/model/WatchWorkoutActivity.kt:22-28`, `WorkoutType` | Enum possui `WALKING`, `RUNNING`, `FREE_WORKOUT`, `TREADMILL`, `UNKNOWN` | Adicionar somente `CYCLING`; não inferir por passos |
| Distâncias | `WatchWorkoutActivity.kt:54-58`, `WorkoutDistanceMetrics` | Possui somente original, estimada e calibrada | Adicionar campo separado nullable para distância manual |
| Métricas | `WatchWorkoutActivity.kt:60-76`, `WorkoutMetrics` | Pace e velocidade importados ficam disponíveis para qualquer atividade | Para `CYCLING`, usar somente distância manual/duração para velocidade média local; pace ausente |
| Revisão | `WatchWorkoutActivity.kt:286-353`, `submitReview`/`confirmReview` | Aceita qualquer tipo diferente de `UNKNOWN`; não limpa dado específico de tipo | Limpar distância manual quando tipo salvo não for `CYCLING` |
| Calibração | `TreadmillCalibration.kt:43-75`; `WatchWorkoutActivity.kt:355-379` | Calibração importada exige `confirmedType == TREADMILL` | Não reutilizar para bicicleta |
| Estado pós-revisão | `WatchWorkoutDerivedMetrics.kt:1175-1188`, `WatchWorkoutWorkflowPolicy` | Somente treadmill sem série canônica elegível vai para `PENDING_CALIBRATION`; demais vão para `READY_TO_SYNC` | `CYCLING`, com ou sem distância, vai para `READY_TO_SYNC` |
| Sugestão | `WatchWorkoutActivity.kt:607-608`, `suggestType` | Passos sugerem `FREE_WORKOUT` ou `RUNNING` | Preservar; nunca sugerir bicicleta automaticamente |

## Inventário de switches e mapeamentos de `WorkoutType`

### Domínio e persistência

- `core/model/WatchWorkoutActivity.kt:686-701`: `phoneOrigin` e validação de
  proveniência. `CYCLING` não pode ser mapeado para origem phone.
- `core/model/AppWorkoutSession.kt:88-89`: somente walking/running registram
  rota; sessão phone não deve ganhar ciclismo.
- `core/model/WorkoutRoute.kt:184-196`: rota aceita somente walking/running.
- `data/RoomRepositories.kt:496-524`: histórico de BPM reconstrói
  `suggestedType`/`confirmedType` por nome; não calcula distância, mas precisa
  continuar aceitando `CYCLING` como label da origem do sample.
- `data/RoomRepositories.kt:2739-2755`: `hasConsistentPhoneActivityOwnership`
  possui `when` fechado para origens phone e deve continuar rejeitando ciclismo
  phone.
- `data/RoomRepositories.kt:3798-3807`: elegibilidade base de atividade para
  Health Connect exige parser, workflow, tipo confirmado e janela temporal; não
  deve transformar cycling em exceção de validade.
- `core/model/TreadmillCalibration.kt:50-52`: guard exclusivo de treadmill.
- `data/DatabaseMappers.kt:430-467` e `920-998`: mapeiam atividade entre
  Room/domínio; enum é convertido pelo nome. Novo campo manual exigirá alteração
  nos dois sentidos.
- `data/DatabaseMappers.kt:324-380`: mapper de `CalendarWorkoutSummary`; hoje
  watch usa distância canônica e phone usa calibrated/original/estimated.
- `core/database/ChampionDatabase.kt:159-193`: `WorkoutActivityEntity` ainda não
  tem distância manual.
- `core/database/ChampionDatabase.kt:676-708`: banco está na versão 11.
- `core/database/ChampionMigrations.kt:437-490` e
  `core/database/DatabaseModule.kt:21-28`: última migration é `10→11`; `11→12`
  ainda não existe nem está registrada.
- `core/database/ChampionDaos.kt:233-272` e `342-360`: consultas e record de
  resumo de calendário não projetam distância manual; o transporte deve incluir
  o novo campo no read model.

### UI e apresentação

- `feature/exercises/ExercisesScreen.kt:1843-1860`: formulário enumera todas as
  entradas exceto `UNKNOWN`; `CYCLING` aparecerá após inclusão do enum, mas ainda
  não há campo de distância.
- `feature/exercises/ExercisesScreen.kt:2150-2155`: ação especial de calibração
  só existe para treadmill phone; não deve ser aplicada a ciclismo.
- `feature/exercises/ExercisesScreen.kt:2420-2474` e `2587-2592`: título,
  label e ícone têm `when` fechado; exigem label `Bicicleta` e ícone coerente.
- `feature/exercises/ExercisesScreen.kt:2162-2193`: detalhe watch sempre exibe
  distância, passos, ritmo e velocidade da série canônica; este é consumer
  funcional crítico.
- `feature/exercises/ExercisesScreen.kt:1048-1052`, `1204-1313`: detalhe
  sempre mostra métricas e gráfico da série canônica.
- `feature/exercises/ExercisesScreen.kt:1511-1669`: intervalos watch exibem
  distância, pace, velocidade e cadência derivadas.
- `feature/exercises/ExercisesScreen.kt:1671-1758`: splits exibem pace,
  velocidade, cadência de passos e BPM por quilômetro.
- `feature/exercises/ExercisesScreen.kt:1333-1359`: resumo e card de calibração;
  calibração histórica deve continuar sendo diagnóstico de treadmill, não fonte
  de distância manual.
- `feature/exercises/ExercisesScreen.kt:2433-2468`: estados, títulos e ícones;
  os novos ramos precisam preservar guards de estados existentes.
- `feature/exercises/WorkoutShells.kt:90-95`, `1469-1479`: opções e labels do
  fluxo phone. Não adicionar `CYCLING`; esse fluxo permanece fora da direção.
- `feature/exercises/ExercisesFeatureContract.kt:52-69` e
  `ExercisesViewModel.kt:1436-1437`: GPS é exigido apenas para walking/running;
  isso continua válido porque ciclismo não terá entrada phone.
- `feature/exercises/ExercisesViewModel.kt:496-531`: entrada de preparação phone
  encerra quando `PhoneWorkoutPolicy.isDiscontinued()` está ativa; não adicionar
  ciclismo a esse caminho nem iniciar localização para ele.
- `feature/calendar/CalendarDayScreen.kt:325-380` e `506-552`: card diário
  mostra distância canônica de watch e labels fechados por tipo. Deve mostrar
  distância manual ou ausência para ciclismo e não exibir provenance de passos.
- `feature/health/HealthMetricDetailScreens.kt:1343-1353`: label do tipo em
  histórico de BPM; incluir `Bicicleta` sem alterar agregação de BPM.
- `feature/home/HomeViewModel.kt:407-417`: banner de sessões phone pendentes;
  não adicionar ciclismo, pois não existe sessão phone de bicicleta.
- `core/notification/WorkoutNotificationBuilder.kt:110-116`: label de
  notificação para sessão phone; manter sem ramo funcional de ciclismo phone.

## Derivação, série canônica e splits

- `core/model/WatchWorkoutDerivedMetrics.kt:1128-1163`: `deriveCanonicalSeries`
  calcula cada intervalo como `decodedSteps × stepLengthMeters` e produz série
  canônica persistível.
- `core/model/WatchWorkoutDerivedMetrics.kt:1207-1264`:
  `WorkoutSplitCalculator` calcula splits de 1 km, pace, velocidade e cadência
  de passos usando a série.
- `data/RoomRepositories.kt:2334-2501`: materializa snapshot, série, intervalos
  e splits; em `2425-2431`, splits são materializados quando a série é elegível.
- `data/RoomRepositories.kt:2504-2522`:
  `promoteCanonicalTreadmillIfNeeded` promove somente treadmill watch elegível de
  `PENDING_CALIBRATION` para `READY_TO_SYNC`.
- `data/DatabaseMappers.kt:952-965` reconstrói distâncias/pace/velocidade
  importados, enquanto `975-985` preserva fatos raw e passos D4.

Conclusão: snapshots, intervalos raw/measured, série derivada e splits devem
continuar persistidos para proveniência/diagnóstico. Consumers funcionais devem
falhar fechados por `confirmedType == CYCLING`, sem apagar ou recalcular esses
dados ao trocar tipo.

## Matriz comportamental congelada

| Métrica/efeito | Walking | Running | Treadmill | Cycling |
| --- | --- | --- | --- | --- |
| Distância por passos × comprimento | Vigente em watch | Vigente em watch | Vigente quando série canônica elegível; caso contrário, calibração própria | Nunca |
| Distância manual | Não existe hoje | Não existe hoje | Não substitui calibração | Opcional; campo próprio em metros |
| Pace | Série canônica ou cálculo vigente | Série canônica ou cálculo vigente | Série canônica/calibração conforme guards | Ausente |
| Cadência de passos | Vigente em série/splits | Vigente em série/splits | Vigente quando série/splits elegíveis | Ausente como métrica de ciclismo |
| Splits por km | Vigentes quando série elegível | Vigentes quando série elegível | Vigentes quando série elegível | Nunca derivados de passos |
| Velocidade média | Derivação vigente | Derivação vigente | Derivação vigente/calibrada | Somente `manualDistanceMeters / duração oficial` |
| DistanceInterval HC | Elegível quando série/proveniência atual permitem | Igual | Igual | Nunca |
| SpeedSeries HC | Elegível quando série possui samples | Igual | Igual | Nunca |
| ManualDistance HC | Não | Não | Não via calibração | Opcional, exatamente um total |
| BPM e calorias HC | Elegíveis conforme samples/calorias atuais | Igual | Igual | Mantidos elegíveis quando válidos |
| Rota/GPS | Somente guards phone históricos walking | Somente guards phone históricos running | Sem rota | Nunca |

Exceções preservadas: treino phone continua descontinuado; treadmill mantém
calibração própria; watch sem perfil ou série elegível permanece sem fallback para
distância legada; snapshots canônicos implausíveis continuam bloqueados pelos
guards atuais.

## Calendário, históricos e agregados de distância

- `core/database/ChampionDaos.kt:233-272` consulta summaries de atividades e
  série canônica.
- `data/DatabaseMappers.kt:341-362` define a distância funcional do calendário:
  watch usa somente canonical quando o status permite; phone usa a precedência
  calibrada → original → estimada.
- `core/model/CalendarHistory.kt:239-262` documenta que distância de atividade
  não participa dos totais diários.
- `feature/calendar/CalendarDayScreen.kt:349-370` é consumer visual da distância
  de atividade e da provenance da derivação.
- `data/LocalCalendarRepository.kt:194-221` e
  `feature/health/DistanceHistoryChart.kt:68-87` usam distância de resumo diário
  (`DailySummary.estimatedDistanceMeters`), não distância de treino. Não devem
  receber distância manual de ciclismo nesta spec.

Decisão: o detalhe/calendário de uma atividade `CYCLING` deve ler o campo manual
quando válido e exibir ausência quando nulo; histórico geral de distância diária
permanece agregado de `DailySummary`, sem promover distância de treino para total
diário.

## Health Connect: manifesto, filhos e retry

### Chaves e manifesto

- `core/model/HealthProjectionContracts.kt:9-74`: chaves atuais são sessão,
  `DistanceInterval`, `SpeedSeries`, `HeartRateSeries` e calorias. Não existe
  `ManualDistance`; `fromStableValue` falha fechado para chaves desconhecidas.
- `core/model/HealthProjectionContracts.kt:111-181`:
  `HealthSyncFrozenProjectionPayload` v1 já possui `distanceMeters` e
  `projectionChildKey`; não há necessidade prevista de aumentar schema somente
  para distinguir provenance manual.
- `core/model/HealthProjectionContracts.kt:195-207` valida manifesto com filhos
  distintos e record types coerentes.
- `core/model/HeartRateAndSync.kt:236-253` define identidade por agregado,
  record type e chave filha.

### Elegibilidade atual

- `core/healthconnect/HealthRecordMapper.kt:494-534` sempre inclui sessão; para
  série elegível inclui todos `DistanceInterval` positivos e `SpeedSeries`; BPM
  e calorias entram quando presentes.
- `HealthRecordMapper.kt:536-554` converte children em record types.
- `HealthRecordMapper.kt:852-858` mapeia tipos para Health Connect; hoje não há
  ciclismo. O novo mapping deve ser `EXERCISE_TYPE_BIKING`, não stationary.
- `core/model/HeartRateAndSync.kt:822-853`: `supports` valida agregado, record
  type e combinação de child key. O novo `ManualDistance` precisa de ramo
  explícito com `DISTANCE`; sem isso a operação falha fechada antes do mapper.
- `HealthRecordMapper.kt:836-840` escolhe distância watch por
  calibrated → original → estimated. Essa função não pode ser usada para
  `CYCLING`.

### Payload, mapper e provenance

- `HealthRecordMapper.kt:557-658` cria payload congelado por filho.
- `HealthRecordMapper.kt:676-685` decodifica somente schema v1.
- `HealthRecordMapper.kt:285-454` reconstrói filhos; metadata atualmente é
  `Metadata.autoRecorded(watchDevice())` para todos os filhos, inclusive
  distância.
- `HealthRecordMapper.kt:112-141` usa `StepsRecord` somente para resumo diário;
  não há `StepsRecord` específico de atividade watch. `ExerciseRoute` também não
  aparece no mapper de atividade watch; rota permanece contrato phone separado
  em `core/model/WorkoutRoute.kt`.
- `HealthRecordMapper.kt:734-761` cria distância total phone com metadata do
  relógio; esse caminho não serve para distância manual watch.

Para `ManualDistance`, `022-04` precisa adicionar chave estável
`manual-distance`, `recordTypeName = DISTANCE`, ramo no manifesto/elegibilidade,
freeze usando `distanceMeters`, e mapper com `Metadata.manualEntry(phoneDevice())`.
O valor deve cobrir a janela oficial completa e não gerar `SpeedRecord`.

### Manifesto, reconciliação e operações

- `core/healthconnect/HealthConnectSyncCoordinator.kt:583-781` cria manifesto,
  identifica filhos obsoletos, calcula versão e congela cada payload.
- `HealthConnectSyncCoordinator.kt:642-685` trata filhos de distância e legado;
  hoje exige série substituta em certos cenários de remoção de agregado legado.
  `022-04` deve revisar esse guard para que cycling sem distância não seja
  bloqueado artificialmente e mantenha remoção exata somente quando a identidade
  legada for conhecida.
- `HealthConnectSyncCoordinator.kt:812-879` valida filhos esperados e atualiza
  workflow; ausência de filho esperado causa falha fechada.
- `HealthConnectSyncCoordinator.kt:906-943` filtra operações por children
  elegíveis; a nova chave precisa ser aceita nesse caminho.
- `data/RoomRepositories.kt:1643-1687`, `1713-1762` e `2681+` validam chaves,
  operações antigas e reconciliação. `fromStableValue` deve continuar legível
  para operações antigas e novas.
- `data/DatabaseMappers.kt:1307-1387` serializa/deserializa chave, payload,
  versão, origem e reconciliação.

### Identidade e versionamento

- `data/RoomRepositories.kt:3376-3409` resolve metadata da atividade; versão
  atual é `max(recordedAt, reviewedAt)`.
- `RoomRepositories.kt:3441-3470` recupera fuso por chave filha e preserva
  fallback legado.
- `RoomRepositories.kt:3533-3565` cria operação pendente e gera ID estável:
  `applicationId:aggregateType:aggregateId:recordType[:stableChildValue]`.
- Distinção relevante: `ids.next()` em `RoomRepositories.kt:3537` cria UUID local
  da operação; o formato acima é `clientRecordId` determinístico, calculado em
  `3562-3565`, não o ID primário da operação.
- `core/model/HeartRateAndSync.kt:627-649` exige versão não decrescente para
  payload congelado; mudança de payload/ação/origem requer versão maior.
- `HealthConnectSyncCoordinator.kt:613-616` usa atividade como `originId` e
  versão de derivação como `originVersion`.

Decisão para próximas subtarefas: reutilizar payload congelado v1, manter
identidade local da operação separada de `clientRecordId` determinístico, manter
versão monotônica, manter operações antigas legíveis e não **reescrever**
operações `SYNCED` automaticamente.

## Riscos e decisões para `022-02..04`

### `022-02` — tipo, distância e persistência

1. Adicionar `CYCLING` quebra `when` fechados de labels, ícones, notificações,
   Health Connect e Home; separar ramos phone dos ramos de revisão watch.
2. Adicionar `manualDistanceMeters REAL NULL` na entidade, nos mappers de
   atividade e no transporte DAO/read model de calendário
   (`ChampionDaos.kt:233-272`, `DatabaseMappers.kt:319-380`).
3. Criar `MIGRATION_11_12` aditiva, registrar no módulo e exportar schema v12;
   linhas existentes devem permanecer `NULL`.
4. Campo de revisão deve aceitar valor positivo e finito em quilômetros,
   converter para metros e permitir limpeza.
5. Ao salvar tipo diferente de `CYCLING`, limpar valor manual no mesmo write.
6. `PENDING_CALIBRATION` deve continuar exclusivo de treadmill conforme política
   vigente.

### `022-03` — métricas e UI locais

1. Filtrar por `confirmedType` antes de ler `canonicalSeries` em detalhe, gráfico,
   intervalos, splits e qualquer helper de distância.
2. Para cycling, mostrar duração/BPM/calorias; mostrar distância e velocidade
   apenas quando manual válida; não mostrar pace, cadência, splits ou passos como
   performance.
3. Adaptar calendário sem alterar `DailySummary` nem históricos gerais de
   distância.
4. Preservar walking/running/treadmill, inclusive série canônica, calibração e
   mensagens de ausência atuais.

### `022-04` — projeções Health Connect

1. Criar chave `ManualDistance` e adicionar suporte em `when`, parser de stable
   value, manifesto, reconciliação e validação de operation.
2. Para cycling, remover elegibilidade de `DistanceInterval` e `SpeedSeries`
   mesmo com snapshot canônico preservado.
3. Sem distância manual, manter sessão/BPM/calorias elegíveis sem child DISTANCE.
4. Com distância manual, criar exatamente um child DISTANCE total com metadata
   manual/local.
5. Preservar retry por payload congelado v1 e identidade/versionamento atuais.
6. Revisar tratamento do agregado legado de distância para evitar bloqueio
   indevido e não apagar operação antiga sem identidade exata.
7. Não criar `StepsRecord` específico de atividade, `ExerciseRoute`,
   `DistanceInterval` ou `SpeedSeries` para cycling.

## Critérios cobertos nesta auditoria

- switches e mappings relevantes de `WorkoutType`: localizados;
- consumers de distância, velocidade, pace, cadência e splits: localizados;
- política de revisão e `PENDING_CALIBRATION`: documentada;
- criação, manifesto, retry, payload e reconciliação de filhos Health Connect:
  documentados;
- política de limpeza de distância ao trocar tipo: congelada;
- código funcional: não alterado.

A revisão independente permanece necessária para confirmar cobertura antes de
marcar a subtarefa como concluída.
