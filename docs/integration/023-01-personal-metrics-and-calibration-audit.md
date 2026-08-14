# 023-01 — Auditoria de métricas pessoais e calibração

- **Spec:** `023-personal-metrics-calibration`
- **Fase:** 16
- **Status:** contratos congelados para `023-02`
- **Base auditada:** branch atual após Spec 022, Room v12

## Resultado

A base já possui altura persistida, perfil de comprimento de passo `BASE`,
sugestão por altura, entidade/histórico parcial de peso, meta de passos em
DataStore, derivação D3/D4 v2 e projeções filhas Health Connect. Ainda não
possui mutation de altura, cadastro de peso, calibração por referência ou
resolução contextual de comprimento de passo.

Nenhuma alteração de código, migration ou algoritmo foi feita nesta auditoria.

## Mapa de arquivos e contratos atuais

### Perfil e métricas pessoais

| Área | Arquivos e pontos relevantes | Estado congelado |
| --- | --- | --- |
| Perfil de domínio | `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt:89-112` | `UserProfile` já valida altura opcional, positiva e finita; `stepLengthProfile` é perfil `BASE`. |
| Perfil Room | `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt:80-88`; `data/DatabaseMappers.kt:392-416` | `UserProfileEntity` já persiste altura, valor/origem/data do passo base e atualização. |
| DAO | `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt:68-110` | Observa perfil atual, último peso e histórico; não há mutation específica de altura. |
| Repository/snapshot | `core/model/RepositoryContracts.kt:123-148`; `data/RoomRepositories.kt:184-207,1078-1129` | `PersonalMetricsSnapshot` observa altura e último peso; não expõe calibrações ou perfis aprendidos. `updateStepLength` preserva altura. |
| Perfil UI | `feature/profile/ProfileScreen.kt:40-95,127-279`; `ProfileViewModel.kt:25-172` | Edita passo base manual, aceita sugestão explicitamente; altura, meta e evidências ainda não são editáveis/visíveis. |
| Peso | `core/database/ChampionDatabase.kt:90-107`; `ChampionDaos.kt:84-110`; `data/DatabaseMappers.kt:297-308` | `WeightMeasurementEntity` e consultas existem; falta API de cadastro manual e superfície funcional Saúde/Peso. |
| Meta de passos | `core/preferences/AppPreferencesStore.kt:30-93`; `core/model/RepositoryContracts.kt:750-758` | `dailyStepsGoal` permanece DataStore, positivo e sanitizado. Home, Saúde e Calendário observam fonte existente; Exercícios também consome valor. |
| Sugestão/passo base | `core/model/WatchWorkoutDerivedMetrics.kt:11-73` | `MANUAL`, `HEIGHT_SUGGESTION_ACCEPTED` e `REFERENCE_DISTANCE_CALIBRATION` existem no enum; última origem ainda não possui fluxo. Fórmula de altura: `altura_cm / 100 * 0.415`. |

### Derivação e workflow

| Área | Arquivos e pontos relevantes | Estado congelado |
| --- | --- | --- |
| Versões/parser | `core/model/WatchWorkoutDerivedMetrics.kt:11-15`; `core/model/WatchWorkoutActivity.kt:12-20` | Derivação atual é `watch-d3-d4-v2`; parser efetivo é `d4-v3`. O parser não será alterado. |
| Snapshot | `core/database/ChampionDatabase.kt:289-328`; `data/DatabaseMappers.kt:552-867` | Snapshot congela passo usado, origem, método, versão, série, distância, pace, velocidade e splits. Snapshots v2 permanecem legíveis e intactos. |
| Seleção atual | `data/RoomRepositories.kt:2337-2426` | `deriveWatchWorkoutInternal` sempre seleciona perfil base atual antes de `WorkoutDerivationSnapshot.derive`. |
| Importação/review | `data/RoomRepositories.kt:701-717,867-993`; `core/model/WatchWorkoutActivity.kt:289-383` | Importação deriva sem tipo confirmado; `review` apenas persiste `confirmedType` e metadados. Review não dispara rederivação. |
| Reprocessamento | `data/RoomRepositories.kt:2113-2227` | Bloqueia `SYNCING` e `SYNCED`; aceita `SYNC_FAILED` sem verificar efeitos remotos por filho. Essa abertura é insuficiente para calibração e será fechada. |
| Ciclo de atividade | `core/model/WatchWorkoutActivity.kt:39-47,413-435` | Estados existentes: `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCING`, `SYNCED`, `SYNC_FAILED`, `DISCARDED`. |

O denominador de calibração será a soma de `WorkoutIntervalEntity.decodedSteps`
elegíveis da série D4, a mesma série usada para distribuir a distância
canônica. `steps` agregado e distância original do relógio não substituem esse
denominador.

### Health Connect

| Área | Arquivos e pontos relevantes | Estado congelado |
| --- | --- | --- |
| Altura/peso | `core/healthconnect/HealthRecordMapper.kt:185-260`; `HealthConnectSyncCoordinator.kt:788-804` | `WeightRecord` e `HeightRecord` são write-only, com `Metadata.manualEntry` para entradas manuais. |
| Enqueue/reconciliação | `data/RoomRepositories.kt:1501-1503,3225-3355` | `ensureOperation`/`ensureReconciledOperation` e `reconcileBacklogInternal` já cobrem peso e altura. Não criar fila nova. |
| Projeções filhas | `core/model/HealthProjectionContracts.kt:9-213`; `data/RoomRepositories.kt:1505-1565` | Filhos têm chave estável, payload congelado, origem e versão monotônica; preparação não executa efeito remoto. |
| Mapper de treino | `core/healthconnect/HealthRecordMapper.kt:263-409,503-728` | Distância calibrada hoje seria indistinguível no payload de distância derivada; `ManualDistance` é reservado à bicicleta da Spec 022. |
| Guardas remotos | `data/RoomRepositories.kt:3814-3847,3905-3909`; `core/model/HeartRateAndSync.kt:878-883` | `SYNC_FAILED` pode conter filhos aceitos. Estado da atividade isolado não prova ausência de efeito remoto parcial. |

### Consumers de distância

- Exercícios: `feature/exercises/ExercisesScreen.kt:1145-1157,1652-1667,2231-2232`
  lê série/snapshot para distância, pace, velocidade e splits. Deve consumir o
  snapshot congelado, inclusive após calibração.
- Calendário: `feature/calendar/CalendarDayScreen.kt:249-270` e
  `data/LocalCalendarRepository.kt:749-756` usam resumo diário e summaries de
  atividade. O detalhe de atividade pode exibir snapshot; resumo diário não
  soma atividades nem usa perfil aprendido.
- Saúde: `feature/health/HealthViewModel.kt:555-568`,
  `DistanceHistoryViewModel.kt` e `DistanceHistoryShell.kt:70-80,288` usam
  históricos de resumo diário. Permanecem na semântica `BASE` vigente: o
  `DailySummaryPolicy` atualmente prioriza `manualStrideLengthMeters` quando
  presente e depois sugere por altura (`DeviceProfileDailySummary.kt:147-172`),
  sem consultar perfis aprendidos. A Fase 16 não altera esse comportamento nem
  reescreve resumos persistidos.
- Home: `feature/home/HomeViewModel.kt:240-279` e `HomeScreen.kt:334-335` usam
  `DailySummaryPolicy`; permanece BASE.

## Contratos congelados para implementação

### 1. Ligação da calibração ao perfil e à atividade

Criar `step_length_calibrations` ligada logicamente a `profileId` e `activityId`.
Usar FK para `user_profiles` alinhada à retenção atual de dados pessoais. Não
usar FK rígida para `workout_activities`: atividades hoje são preservadas e
descartadas logicamente, mas uma retenção futura legítima não deve apagar uma
evidência sem decisão explícita. O repository validará a atividade no aceite e
manterá índice por `(profileId, workoutType)`.

Campos mínimos permanecem os previstos na Spec 023:
`id`, `profileId`, `activityId`, `workoutType`, `referenceDistanceMeters`,
`decodedSteps`, `acceptedAtEpochMillis` e `updatedAtEpochMillis`.

### 2. Uma evidência ativa por atividade

`UNIQUE(activityId)` garante uma linha ativa por atividade. Correção atualiza a
mesma linha; não cria nova amostra. Remoção exclui a evidência ativa. Não haverá
soft-delete ou linha histórica duplicada nesta fase.

### 3. Perfil aprendido sem estado duplicado

Não persistir agregado aprendido autoritativo. Repository calcula por contexto:

```text
Σ referenceDistanceMeters / Σ decodedSteps
```

Somente evidências válidas e ativas entram no cálculo. A leitura expõe tipo,
comprimento, `sampleCount`, distância total, passos totais e `updatedAt`. Sem
evidência, retorna ausência e o resolver usa `BASE`.

### 4. Momento de seleção contextual

- Importação inicial pode derivar com `BASE`.
- Revisão persiste `confirmedType`, sem rederivação implícita.
- Ação explícita de rederivação após confirmação seleciona `RUNNING` ou
  `WALKING` aprendido, com fallback `BASE`.
- Calibração aplicada à própria atividade usa diretamente
  `referenceDistanceMeters / Σ decodedSteps` e grava
  `REFERENCE_DISTANCE_CALIBRATION`.
- `TREADMILL` e `DailySummary` continuam em `BASE`.
- `CYCLING` nunca consulta perfil de passos.
- Nenhuma rederivação ocorre em background ou ao apenas alterar perfil.

O tipo usado no resolver é sempre o `confirmedType` lido na mesma transação da
derivação. Troca de tipo invalida seleção semântica anterior e exige nova ação
explícita.

### 5. Estados permitidos para rederivar a própria atividade

Permitidos, com tipo confirmado e demais critérios D4 válidos:

- `PENDING_CALIBRATION`;
- `READY_TO_SYNC`;
- `SYNC_FAILED` somente quando nenhuma operação filha possui estado `SYNCED`,
  estado `SYNCING`, checkpoint `AFTER_REMOTE_EFFECT` ou efeito remoto aceito;
  a checagem percorre todas as operações filhas do agregado.

Bloqueados sempre:

- `PENDING_REVIEW`, inclusive quando o tipo ainda não foi confirmado;
- `SYNCING`;
- `SYNCED`;
- `DISCARDED`;
- qualquer atividade com efeito remoto parcial não reconciliável por filho.

Persistência da evidência e snapshot calibrado ocorrerá na mesma transação local.
Falha em qualquer preflight desfaz ambos. Calibração nunca cria operação ou
efeito remoto.

### 6. Atividade já sincronizada

`SYNCED`, `SYNCING` e atividades com efeito remoto parcial não são elegíveis
para corrigir a própria atividade nem para entrar como evidência nesta fase.
Não alterar snapshot, filhos Health Connect ou `clientRecordVersion`. Reconciliação
pós-sync genérica fica fora do escopo.

### 7. Proveniência mínima para filhos Health Connect

Manter `HealthProjectionOrigin` com `originId` igual ao `activity.id` vigente em
`HealthConnectSyncCoordinator.kt:615-618` e `originVersion` igual à versão do
snapshot (ou `WATCH_ACTIVITY_PROJECTION_VERSION` quando não houver snapshot).
Não trocar identidade para o ID da linha de snapshot: isso quebraria a
compatibilidade das operações e retries já persistidos.

Evoluir payload congelado de forma retrocompatível, adicionando
`metricProvenance: String?` e subindo schema para versão 2. Valores serializados
permitidos:

- `WATCH_DERIVED`: distância/velocidade calculadas por passos e perfil base ou
  aprendido;
- `EXTERNAL_REFERENCE_CALIBRATION`: distância/velocidade da atividade
  diretamente calibrada por referência digitada.

Payload v1 ausente desse campo decodifica como `WATCH_DERIVED`; valor desconhecido
falha fechado. Sessão, BPM e calorias permanecem com proveniência atual.
`ManualDistance` continua exclusivo de `CYCLING`; distância calibrada não será
rotulada como GPS coletado pelo app.
O sync permanece explícito, com IDs e versões atuais.

### 8. Enqueue de altura e peso

Após mutation local bem-sucedida, a mesma transação pode chamar
`ensureReconciledOperation` para:

- `HealthSyncAggregate.UserProfileHeight(profileId)` + `HEIGHT`;
- `HealthSyncAggregate.WeightMeasurement(measurementId)` + `WEIGHT`.

O ponto de fallback é `reconcileBacklogInternal`, já usado em foreground. A fila
continua local; configuração/permissão e execução remota permanecem no
`HealthConnectSyncCoordinator`, sem readback e sem desfazer persistência local.

### 9. Consumers que mudam e que permanecem

Devem mudar em subtarefas posteriores:

- `PersonalMetricsRepository`/DAO para evidências e resolver;
- derivação em `RoomRepositories` e versão `watch-d3-d4-v3`;
- Perfil para altura, meta e perfis aprendidos;
- Saúde/Peso para cadastro e histórico;
- detalhe de Exercícios para preview/aceite/correção de calibração;
- payload/proveniência de projeções Health Connect.

Devem permanecer intactos:

- parser `d4-v3` e snapshots `watch-d3-d4-v2`;
- `DailySummaryPolicy`, Home, histórico diário de Saúde e meta em DataStore,
  exceto wiring da edição da meta;
- `TREADMILL` em BASE;
- `CYCLING` com `manualDistanceMeters` e sem distância por passos;
- BPM, calorias, timestamps D4 e dados raw;
- sync Health Connect write-only, foreground, explícito e local-first.

## Riscos residuais

1. A fila atual não tem mutation pública de altura/peso; `023-03` e `023-04`
   devem adicionar APIs sem duplicar infraestrutura.
2. Todos os entrypoints de rederivação (`reprocessWatchWorkoutInternal`,
   `deriveWatchWorkoutInternal` e a futura operação de calibração) devem aplicar
   a mesma whitelist; hoje o código também permite derivação substitutiva para
   `PENDING_REVIEW`, portanto a implementação precisa bloquear esse estado.
3. O estado `SYNC_FAILED` exige inspeção por operação filha antes de liberar
   rederivação; não basta checar o estado agregado da atividade.
4. Payload de provenance v2 exige decoder retrocompatível e validação de schema
   em `023-06`; payloads antigos não podem ser regravados.
5. A remoção física de uma atividade não existe hoje. Se for introduzida no
   futuro, deverá definir retenção de evidência antes de alterar FK/política.
6. Não há testes automatizados novos nesta task, conforme guard rail da Spec 023.

Com esses contratos, `023-02` pode implementar schema/domínio sem decisão
arquitetural aberta dentro do escopo desta auditoria.
