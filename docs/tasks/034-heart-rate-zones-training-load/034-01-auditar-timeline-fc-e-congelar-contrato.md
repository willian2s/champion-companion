# 034-01 — Auditar timeline de FC e congelar contrato

- **Ticker:** `034`
- **Número:** `01`
- **Status:** `completed`

## Objetivo

Consolidar evidência do contrato atual de série temporal, perfil pessoal, Room,
Profile e detalhe de Exercícios antes de qualquer implementação, transformando
as decisões de produto em limites verificáveis para as subtarefas seguintes.

## Resultado esperado

Documento/evidência de implementação atualizado ou anexado à revisão da task,
com janela canônica, pontos de extensão, estados de ausência, versão Room,
contratos remotos preservados e dependências de `034-02` a `034-05` congelados.

## Requisitos cobertos

- R1, R4, R5 e R7 da Spec 034;
- ADR-018 como autoridade única para sample-and-hold;
- confirmação de `WorkoutHeartRateSample`, `UserProfile`,
  `PersonalMetricsSnapshot`, `PersonalMetricsRepository`, Profile,
  `ExercisesScreen`/`ExercisesViewModel` e Room v19;
- ausência de alterações BLE, parser, workflow e Health Connect.

## Escopo incluído

- auditar `WorkoutHeartRateSample`, DAO, entidade, mapper e consumers;
- confirmar `[startedAt, startedAt + duration)` como janela canônica;
- mapear fronteiras e riscos de `endedAt`, samples fora da sessão e lacunas;
- mapear `UserProfile` e todos os reconstrutores que podem perder o novo campo;
- mapear editor de Perfil e ponto de composição do detalhe;
- confirmar migration/schema atuais e cadeia v19;
- registrar contratos que não podem ser reabertos.

## Escopo excluído

- qualquer alteração em código de produção;
- criação de testes automatizados;
- ajuste de parser, BLE, Health Connect ou snapshots;
- decisão de baseline para classificar carga.

## Dependências

- [ADR-018](../../decisions/adr-018-canonical-workout-time-series-and-health-projection.md);
- baseline atual e estado documental da Spec 033;
- código vigente em `core/model`, `core/database`, `data`, `feature/profile` e
  `feature/exercises`.

## Arquivos e símbolos prováveis

- `core/model/WatchWorkoutActivity.kt` — `WorkoutHeartRateSample`;
- `core/model/WatchWorkoutDerivedMetrics.kt` — `weightedBpm`;
- `core/database/ChampionDatabase.kt` — entidades e versão;
- `core/database/ChampionDaos.kt` — queries da série e perfil;
- `core/database/ChampionMigrations.kt` — última migration;
- `data/DatabaseMappers.kt` e `data/RoomRepositories.kt`;
- `core/model/DeviceProfileDailySummary.kt` — `UserProfile`;
- `core/model/RepositoryContracts.kt` — repository e snapshot;
- `feature/profile/ProfileFeatureContract.kt`, `ProfileViewModel.kt`,
  `ProfileScreen.kt`;
- `feature/exercises/ExercisesFeatureContract.kt`, `ExercisesViewModel.kt`,
  `ExercisesScreen.kt`;
- `app/schemas/.../19.json`.

## Passos de implementação

1. Revalidar paths e linhas dos contratos já auditados.
2. Escrever matriz de fonte, janela, estado e consumer.
3. Fixar `maxHeartRateBpm` como `Int?` manual em `30..220`.
4. Fixar buckets por razão e pesos, incluindo `BELOW_ZONE_1`.
5. Fixar cobertura por duração de sample-and-hold, sem threshold.
6. Fixar ausência segura para configuração, samples e falha de leitura.
7. Fixar ordem de dependência: domínio e persistência em paralelo após esta
   task; read model depois; UI por último; validação final.
8. Registrar qualquer divergência como risco, não como comportamento implícito.

## Testes e comandos de validação

- leitura e inspeção dos arquivos auditados;
- conferência de Room v19 e schema 19;
- `git diff --check` quando houver documentação produzida;
- não criar ou executar testes automatizados.

## Definição de pronto

- janela temporal e fórmula não dependem de interpretação informal;
- pontos de extensão e preservação de dados estão listados;
- escopo watch-only e fronteira Health Connect estão explícitos;
- `034-02` e `034-03` podem ser implementadas sem decisão adicional material.

## Riscos e cuidados

- Não transformar a lacuna visual de 120 s do gráfico em nova política.
- Não usar `metrics.averageBpm` ou `maximumBpm` como fonte da análise.
- Não confundir `endedAt` com fim canônico quando duração e pausas divergem.
- Não assumir que `SYNCED` pode ser reprocessado.
- Preservar dados de saúde; não incluir BPM individual em logs ou evidências.

## Auditoria factual — 2026-09-16

### Matriz de fonte, janela, estado e consumer

| Fonte/contrato | Evidência atual | Janela/estado vigente | Consumer e limite congelado |
| --- | --- | --- | --- |
| `WorkoutHeartRateSample` | `core/model/WatchWorkoutActivity.kt:106-111,238-255` contém `sampledAt`, `bpm` e `sourceSampleIndex?`; `WorkoutActivity` mantém a lista junto dos fatos da atividade | BPM já validado em `30..220` por `WatchWorkoutActivity.kt:900-904`; amostras precisam estar em ordem estritamente crescente (`:906-912`) | Fonte exclusiva da análise de zonas. Não usar `metrics.averageBpm` ou `maximumBpm` como série |
| Persistência da série | `core/database/ChampionDatabase.kt:646-664` define `workout_heart_rate_samples`, FK para atividade e índice por atividade/timestamp; `core/database/ChampionDaos.kt:685-714` lê ordenado e insere/atualiza fatos | Precisão preservada por millis, segundo e nanos | `data/DatabaseMappers.kt:1390-1417` faz round-trip do sample e do índice de origem. Não criar tabela, coluna ou alteração de parser para a análise |
| Importação D4 e BLE | `feature/exercises/ChampionWatchWorkoutImportCoordinator.kt:344-363` aguarda blocos e chama `ChampionD4WorkoutParser`; `core/bluetooth/ChampionD4WorkoutParser.kt:133-145` cria samples a partir da grade/midpoint normalizada e preserva `sourceSampleIndex`; `:164-180` compõe `WorkoutMetrics` | Timestamp é fato derivado do parser vigente, não nova medição | Fluxo BLE/importação e parser D1/D4 permanecem intocados; nenhum sample novo será fabricado |
| Sessão da atividade | `WorkoutMetrics` separa `startedAt`, `duration` e `endedAt` em `core/model/WatchWorkoutActivity.kt:66-82`; `endedAt` pode exceder a duração ativa (`:80-81`) | Para 034, sessão canônica fica fixada em `[startedAt, startedAt + duration)`; `endedAt` divergente não abre segunda janela | A policy de 034 deve receber/construir janela canônica diretamente. Samples em `t < startedAt` ou `t >= startedAt + duration` não iniciam janela analítica |
| Sample-and-hold existente | `WorkoutSplitCalculator` em `core/model/WatchWorkoutDerivedMetrics.kt:1673-1715,1779-1802` ordena samples, usa próximo timestamp ou fim da sessão e calcula BPM por sobreposição ponderada | Janelas semiabertas `[sampledAt_i, sampledAt_{i+1})`; último sample vai até o fim da janela recebida; trecho antes do primeiro sample permanece descoberto | ADR-018 é autoridade única. `weightedBpm` é privado e hoje recebe janela derivada da série (`:1679-1682`); extração/reuso deve preservar splits e limitar samples à sessão canônica |
| Fronteiras incidentais | Importação correlacionada filtra com `isAfter(metrics.endedAt)` em `core/model/WatchWorkoutActivity.kt:547-555`; validação phone aceita sample em `endedAt` em `:623-637`; Health Connect usa filtro inclusivo em `core/healthconnect/HealthRecordMapper.kt:1149-1154` | Essas fronteiras não substituem `[S,E)` da análise de 034 | Divergência registrada como risco, não corrigida nesta task. Não alterar consumidores remotos, parser ou workflow |
| Perfil pessoal | `UserProfile` tem altura, atualização e `StepLengthProfile?`, sem FC máxima, em `core/model/DeviceProfileDailySummary.kt:89-111` | Nova configuração será opcional, manual e `Int?`, válida em `30..220`; ausência significa não configurado | `UserProfile.create` é ponto de validação. `data/DatabaseMappers.kt:542-566` é round-trip e precisa preservar o campo futuro |
| Snapshot/repository pessoal | `PersonalMetricsSnapshot` em `core/model/RepositoryContracts.kt:244-253` não expõe FC máxima; `PersonalMetricsRepository` em `:124-142` só tem observação, altura e passada | Snapshot atual será fonte da FC máxima para read model; não usar DataStore | `RoomRepositories.observeSnapshot()` compõe snapshot e peso em `data/RoomRepositories.kt:251-276`. Novo update deve ser local e separado do fluxo de altura |
| Reconstrutores do perfil | `RoomRepositories.updateStepLength()` reconstrói perfil em `data/RoomRepositories.kt:1967-1995`; `updateHeight()` faz o mesmo em `:2002-2047`; mapper reconstrói entidade em `DatabaseMappers.kt:542-566` | Todo caminho de reconstrução precisa copiar `maxHeartRateBpm` | Altura hoje chama reconciliação/sync em `RoomRepositories.kt:2007-2045` e `ProfileViewModel.kt:119-146,193-203`; FC máxima não pode reutilizar esse gatilho |
| Editor de Perfil | `ProfileUiState`, `ProfileMessage` e `ProfileIntent` só têm altura/meta em `feature/profile/ProfileFeatureContract.kt:6-33`; coleta do snapshot ocorre em `ProfileViewModel.kt:39-59`; card atual está em `ProfileScreen.kt:75-81,126-195` | Estados de valor, limpeza, validação e falha deverão ser próprios da FC máxima | Supporting text deve explicar uso em zonas/carga. Meta de passos continua em DataStore; não adicionar FC máxima a preferências |
| Detalhe de Exercícios | `ExercisesViewModel.kt:320-352` observa `selectedActivity` e `PersonalMetricsSnapshot` separadamente; `ExerciseDetailState.Ready` só expõe id/calibração em `ExercisesFeatureContract.kt:109-117` | Read model efêmero deve combinar atividade selecionada, samples locais e snapshot atual | `ExercisesScreen.kt:1883-1895` renderiza métricas, gráfico de FC e depois Cadência; regra nova fica em `core.model`/ViewModel, inserida visualmente entre ambos |
| Gráfico de FC | `ExercisesScreen.kt:2215-2243` apresenta gráfico e agregados `averageBpm`/`maximumBpm`; `:2245-2282` usa `endedAt` para duração visual e insere lacuna visual de `120 s` | A lacuna é somente visual; não cria stale timeout nem política de cobertura | Preservar gráfico e agregados atuais. A análise não pode usar a lacuna visual nem os agregados como fonte |
| Room e schema | `ChampionDatabase.kt:922-962` declara `version = 19`; `ChampionMigrations.kt:772-794` termina em `MIGRATION_18_19`; `DatabaseModule.kt:21-36` registra a cadeia; `app/schemas/.../19.json:267-301` descreve perfil atual e `:2660-2719` descreve série | Próxima evolução, somente em 034-03, é migration aditiva `19 -> 20`, coluna nullable sem backfill | Não migrar nesta auditoria. Não alterar dados de atividades, samples, snapshots ou fila |
| Health Connect/workflow | `health_sync_operations` é entidade existente em `core/database/ChampionDatabase.kt:842-874`; projeção de BPM usa samples reais em `core/healthconnect/HealthRecordMapper.kt:988-990` | Zonas, cobertura e carga são locais e não persistidos; `SYNCED` e snapshots continuam fatos congelados | Nenhuma permissão, record, payload, ID, versão, operação, retry, reconciliação ou write novo; salvar FC máxima não dispara sync |

### Contrato congelado para `034-02` a `034-05`

1. **Entrada e escopo:** somente `WorkoutActivity` watch com série local
   persistida, seus `WorkoutHeartRateSample` reconstruíveis e
   `UserProfile.maxHeartRateBpm` atual. Workouts phone, Health Connect, parser,
   BLE, `WorkoutDerivationSnapshot` e `ActivityWorkflowState` ficam fora da
   análise.
2. **Janela:** `S = startedAt` e `E = startedAt + duration`; análise usa `[S,E)`.
   Samples são ordenados por `sampledAt`; cada janela é `[sampledAt_i,
   sampledAt_{i+1})`, limitada a `[S,E)`, e a última termina em `E`. Não há
   cobertura antes do primeiro sample válido. Não há interpolação, síntese,
   redistribuição, timeout ou threshold de sample stale.
3. **Configuração:** `maxHeartRateBpm` é inteiro manual `30..220`, inclusive,
   nullable, sem default ou estimativa. Valor observado da atividade continua
   distinto da configuração pessoal.
4. **Buckets:** calcular `ratio = bpm.toDouble() /
   configuredMaximumBpm.toDouble()` e classificar pelas fronteiras exatas:
   `BELOW_ZONE_1 < 0.50` peso 0; `Z1 0.50..<0.60` peso 1; `Z2 0.60..<0.70`
   peso 2; `Z3 0.70..<0.80` peso 3; `Z4 0.80..<0.90` peso 4; `Z5 >= 0.90`
   peso 5. BPM acima da máxima permanece válido, fica em `Z5` e ativa
   `observedAboveConfiguredMaximum`.
5. **Cobertura e carga:** `coveredDuration` é soma das sobreposições positivas;
   `coverageRatio = coveredDuration / sessionDuration`, limitado a `0.0..1.0`.
   `BELOW_ZONE_1` fecha cobertura com peso zero. Carga é `Σ(durationInMinutes ×
   weight)` em `Double`; arredondamento somente na UI. Percentuais usam
   `coveredDuration`, não duração total, e a soma das durações dos buckets fecha
   a cobertura.
6. **Estados seguros:** `AVAILABLE` exige configuração e samples; com samples
   sem configuração, retornar `CONFIGURATION_REQUIRED` com observação/cobertura,
   mas sem zonas ou carga sintéticas; sem sample utilizável na janela, retornar
   `NO_HEART_RATE_SAMPLES`; falha ou inconsistência de leitura/composição,
   `UNAVAILABLE`. Esses estados não são workflow.
7. **Composição e ordem:** `034-02` entrega policy pura em `core.model`;
   `034-03` trata perfil/Room; ambos podem seguir em paralelo após esta task;
   `034-04` compõe read model reativo no detalhe; `034-05` apenas apresenta-o
   entre Frequência cardíaca e Cadência. Alteração de FC máxima recalcula só
   read model visual, inclusive histórico, sem reprocessar fatos.

### Contratos preservados e riscos registrados

- Parser D1/D4, fluxo BLE/importação (`feature/exercises/ChampionWatchWorkoutImportCoordinator.kt:344-363`), série persistida, derivação congelada,
  `workflowState`, snapshots e Health Connect não recebem alteração nesta
  subtarefa nem nas subsequentes de análise.
- `endedAt` inclusivo em consumidores existentes e a janela derivada usada por
  `weightedBpm` são fronteiras distintas já observadas. A policy nova deve
  aplicar `[startedAt, startedAt + duration)` sem corrigir esses consumidores;
  qualquer divergência adicional deve ser registrada, não inferida.
- `weightedBpm` é privado. Extração compartilhada é ponto de atenção: comparar
  resultados de splits e não permitir que sample externo estenda a janela.
- D4 não entrega timestamp real de BPM; o timestamp normalizado/nominal do
  parser é fato de entrada e não deve ser reinterpretado.
- Reconstrutores de perfil são risco de perda silenciosa: mapper, `updateHeight`
  e `updateStepLength` precisam preservar a nova coluna quando `034-03` for
  executada.
- Ausência nunca será representada por zero, lista vazia semântica ou BPM
  fabricado. Samples individuais e FC máxima não devem aparecer em logs ou
  evidências.

## Registro da execução

- **Status:** `completed` — auditoria documental concluída; nenhuma alteração de
  produção foi feita.
- **Arquivos alterados:**
  `docs/tasks/034-heart-rate-zones-training-load/034-01-auditar-timeline-fc-e-congelar-contrato.md`
  e `docs/tasks/034-heart-rate-zones-training-load/034-00-overview.md`.
- **Decisões e desvios:** ADR-018 permanece autoridade temporal única; a janela
  canônica foi fixada em `[startedAt, startedAt + duration)`. Divergências
  inclusivas de `endedAt` em consumidores existentes foram registradas como
  risco e não corrigidas. FC máxima, buckets, pesos, cobertura, ausência segura,
  read model efêmero e ordem de dependências foram congelados conforme Spec 034
  e ADR-034.
- **Comandos executados:** leitura de `AGENTS.md`, baseline atual, Spec 034,
  ADR-018, ADR-034, overview e subtarefas `034-02` a `034-06`; inspeção dos
  contratos de domínio, DAO, entidade, mapper, repository, Profile, Exercícios,
  parser, Health Connect e schema Room 19; `git diff --check`.
- **Resultados e evidências:** ticker `034` confirmado em spec, pasta, overview
  e seis subtarefas; overview possui uma única seção `Checklist` com seis itens;
  Room v19, `MIGRATION_18_19`, schema `19.json`, série persistida, helper
  `weightedBpm`, perfil e pontos de composição foram confirmados. `034-02` e
  `034-03` estão liberadas em paralelo; `034-04` depende de ambas, `034-05`
  depende de `034-04` e `034-06` depende de `034-02` a `034-05`.
- **Testes e validações:** conforme escopo documental, nenhum teste automatizado
  foi criado ou executado. `git diff --check` passou após a documentação.
- **Riscos residuais:** extração/reuso do helper pode afetar splits; fronteiras
  inclusivas de `endedAt` permanecem em consumidores fora do escopo; fixture
  física, fronteiras numéricas e migration v20 dependem das subtarefas futuras.
