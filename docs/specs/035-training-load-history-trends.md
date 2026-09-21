# 035 — Histórico de carga cardiovascular e tendência

- **Ticker:** `035`
- **Status:** `completed`
- **Tipo:** agregação local de read model e tendência cardiovascular
- **Base auditada:** `main` em `d0ad6a8`, com Spec 034 mergeada e Room v20
- **Branch de trabalho:** `feat/035-training-load-history-trends`
- **ADR:** [ADR-035](../decisions/adr-035-training-load-history-trends.md)

## Contexto

A Spec 034 foi mergeada em `main` e está concluída. Ela adicionou
`WorkoutHeartRateAnalysisPolicy` e `WorkoutHeartRateAnalysis` para calcular,
sob demanda, zonas, cobertura e carga cardiovascular individual a partir de
samples de frequência cardíaca persistidos e da FC máxima pessoal atual.

A auditoria desta Spec 035 confirmou:

- a branch atual está limpa, aponta para o merge da Spec 034 e não exige
  merge local adicional;
- `ChampionDatabase` está na versão `20`, com `MIGRATION_19_20` limitada à
  coluna nullable `user_profiles.maxHeartRateBpm`;
- `WorkoutHeartRateAnalysisPolicy` é a única fonte autorizada para classificação
  de zonas, sample-and-hold, cobertura e Edwards TRIMP;
- `UserProfile.maxHeartRateBpm` e `PersonalMetricsSnapshot.maxHeartRateBpm`
  já existem, são opcionais e aceitam `30..220` BPM;
- a tela `ExercisesScreen` já possui histórico local com seleção `Semana`/`Mês`,
  navegação temporal, filtros e lista de atividades; a seleção permanece estado
  local da tela;
- `WorkoutActivityRepository.observeActivitiesBetween` e
  `observeActivitiesIntersecting` retornam `CalendarWorkoutSummary`, sem
  samples; `observeActivities(ALL)` carrega o histórico inteiro e inclui
  descartados;
- `CalendarWorkoutSummary` não é suficiente para a policy 034, pois não contém
  `WorkoutActivity.heartRateSamples`;
- samples são persistidos em `workout_heart_rate_samples`, com índice por
  `(activityId, sampledAtEpochMillis)`, e a leitura atual completa uma atividade
  por vez;
- as consultas de calendário filtram `workflowState != 'DISCARDED'`, mas não
  filtram `origin = WATCH_D3_D4`;
- não há entidade, coluna, snapshot, repository ou payload de carga histórica.

O baseline documental ainda contém menções históricas a Room v19. O código,
schema `20.json`, migration e evidência física da Spec 034 confirmam v20; esta
discrepância fica registrada como risco documental e não será corrigida como
parte da implementação da 035.

## Atualização de apresentação — 2026-09-21

`035-05` e `035-06` entregaram e validaram inicialmente a carga cardiovascular
como seção inline na página de Exercícios. Após revisão de apresentação, o
usuário escolheu a opção 1: a seção inline será substituída por um CTA acessível
em Exercícios, que abrirá uma página própria de histórico de carga
cardiovascular.

O destino será contextual à subtree de Exercícios e seguirá o back stack manual
e os padrões de destinos existentes. Não será uma nova aba principal, não haverá
entrada em Home ou Saúde e não haverá mudança em `CardiovascularLoadHistory`,
`CardiovascularLoadHistoryPolicy`, `WorkoutHeartRateAnalysisPolicy`, repository,
Room ou qualquer contrato de dados. A subtarefa `035-07` executa somente essa
revisão de apresentação e navegação.

## Objetivo

Evoluir a carga cardiovascular local da Spec 034 para uma visão histórica,
com comparação de janelas, distribuição diária de 28 dias, cobertura de FC,
quantidade de workouts analisados e contribuição por tipo de treino.

A entrega deve somente agregar resultados derivados em memória. Não deve criar
uma segunda fórmula, persistir analytics ou alterar fatos de workout,
workflow, derivação ou Health Connect.

## Escopo

### Incluído

- política histórica pura, determinística e explícita quanto a `asOf` e
  `ZoneId`;
- janelas `current7`, `previous7`, `current28` e `previous28` semiabertas;
- chamada de `WorkoutHeartRateAnalysisPolicy` para cada workout elegível;
- resumos de período, comparações, buckets diários de 28 dias e grupos por tipo;
- estados tipados para configuração ausente, ausência de workouts, ausência de
  análises e falha de leitura;
- contrato bounded no repository para carregar atividades completas watch no
  intervalo máximo usado pela 035;
- composição reativa no `ExercisesViewModel` com workouts bounded, snapshot
  pessoal, `asOf` e `ZoneId`;
- CTA em Exercícios e página própria contextual para o histórico, preservando
  a seleção atual `Semana`/`Mês` da lista;
- validação física sobre workouts e banco já existentes na Spec 034.

### Excluído

- nova fórmula de carga, carga por FC média, nova classificação de zonas,
  novo sample-and-hold ou nova definição de cobertura;
- persistência de carga por workout, carga diária, histórico, snapshot ou
  tabela analítica;
- migration, alteração de schema ou índice novo nesta spec, salvo descoberta
  factual que impeça a leitura bounded e que seja documentada antes de qualquer
  implementação;
- alteração de `WorkoutActivity`, `workflowState`,
  `WorkoutDerivationSnapshot`, parser D1/D3/D4, BLE, D1, D4 ou workflow;
- qualquer permission, record, payload, operação, retry, reconciliação, leitura
  ou escrita nova em Health Connect;
- nova aba principal, tela global, seção na Home ou Saúde, ou mudança
  substantiva no detalhe individual da Spec 034; a página própria contextual
  acessível por CTA em Exercícios é tratada na atualização de apresentação;
- timer, polling contínuo, leitura completa indefinida ou acesso do ViewModel
  direto ao DAO;
- classificação de carga como boa/ruim, ideal, excessiva, fadiga,
  recuperação, risco, overtraining ou recomendação de treino/descanso;
- testes automatizados novos nesta execução, conforme `AGENTS.md`.

## Fonte autoritativa e invariantes

A análise histórica deve reutilizar diretamente:

```text
WorkoutHeartRateAnalysisPolicy
+ WorkoutHeartRateAnalysis
```

Para cada workout, a policy 035 constrói a janela canônica
`[startedAt, startedAt + duration)` e chama a policy 034 com os samples da
atividade e o `maxHeartRateBpm` atual. A carga individual continua sendo:

```text
Z1_minutes * 1 +
Z2_minutes * 2 +
Z3_minutes * 3 +
Z4_minutes * 4 +
Z5_minutes * 5
```

Não serão duplicadas classificação de zonas, sample-and-hold, cobertura,
limites BPM, bucket `BELOW_ZONE_1` ou Edwards TRIMP. Um workout sem samples
continua sem análise numérica e nunca recebe carga sintética.

`maxHeartRateBpm` é a configuração pessoal atual. Alterá-la recompõe todo o
read model histórico, sem alterar `WorkoutActivity`, `workflowState`,
`WorkoutDerivationSnapshot`, `health_sync_operations` ou qualquer fato remoto.

## Requisitos funcionais

### R1 — População analítica

1. Incluir somente atividades com `origin = WATCH_D3_D4`.
2. Excluir somente atividades com `workflowState = DISCARDED`.
3. `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCED` e
   `SYNC_FAILED` permanecem elegíveis, sem o estado controlar o cálculo.
4. A participação é definida por `startedAt`; não dividir workout entre janelas.
5. Health Connect não é pré-requisito nem fonte da análise.
6. O tipo deve ser resolvido por `confirmedType ?: suggestedType ?: UNKNOWN`.
   Os grupos previstos são `RUNNING`, `WALKING`, `TREADMILL`, `FREE_WORKOUT`,
   `CYCLING` e `UNKNOWN`.

### R2 — Janelas e tempo

A policy histórica recebe explicitamente `asOf: Instant`. Não chama
`Instant.now()` no domínio puro.

```text
current7:  [asOf - 7 dias,  asOf)
previous7: [asOf - 14 dias, asOf - 7 dias)
current28: [asOf - 28 dias, asOf)
previous28:[asOf - 56 dias, asOf - 28 dias)
```

Os intervalos são semiabertos. `startedAt` determina a única janela de cada
workout. `ZoneId` é recebido explicitamente e serve apenas para converter
`startedAt` em `LocalDate` nos buckets e para manter os contratos civis já
usados pelo calendário. Nenhum timezone analítico novo é persistido.

O read model deve manter `asOf` estável durante sua composição. Para a
apresentação diária, o chamador pode alinhar `asOf` ao início de um dia civil;
quando isso não ocorrer, a policy deve preservar as datas locais tocadas pela
janela e ainda fechar as somas por pertencimento de `startedAt`, sem deslocar
workouts entre períodos.

### R3 — Agregação de período

Para cada workout elegível:

1. construir a janela canônica da sessão;
2. chamar `WorkoutHeartRateAnalysisPolicy` diretamente;
3. consumir o resultado tipado, sem converter ausência em zero semântico.

O resumo deve expor pelo menos:

- `totalLoad`;
- `totalWorkoutCount`;
- `analyzedWorkoutCount`;
- `totalWorkoutDuration`;
- `coveredHeartRateDuration`;
- `coverageRatio`;
- `partial`.

`totalLoad` é a soma de `cardiovascularLoad` somente dos resultados
`AVAILABLE`. `analyzedWorkoutCount` conta somente workouts com resultado
`AVAILABLE`. `totalWorkoutDuration` inclui todos os workouts elegíveis.
`coveredHeartRateDuration` soma `coveredDuration` das análises que possuem
análise temporal, inclusive `CONFIGURATION_REQUIRED`; para ausência de
samples, a contribuição é zero.

```text
coverageRatio =
    soma(coveredDuration de análises disponíveis)
    -----------------------------------------------
    soma(sessionDuration de todos os workouts elegíveis)
```

O resultado deve ser limitado ao intervalo `0.0..1.0`. Não há threshold mínimo
de cobertura. `partial` é verdadeiro quando algum workout elegível não está
analisado ou quando alguma análise disponível possui cobertura parcial.

### R4 — Estados

Planejar contratos equivalentes a:

- `CardiovascularLoadHistoryStatus.AVAILABLE`;
- `CardiovascularLoadHistoryStatus.CONFIGURATION_REQUIRED`;
- `CardiovascularLoadHistoryStatus.NO_WORKOUTS`;
- `CardiovascularLoadHistoryStatus.NO_ANALYZABLE_WORKOUTS`;
- `CardiovascularLoadHistoryStatus.UNAVAILABLE`.

Precedência congelada para o read model:

1. falha de fonte/composição ou inconsistência irrecuperável: `UNAVAILABLE`;
2. população vazia: `NO_WORKOUTS`;
3. população existente sem FC máxima atual: `CONFIGURATION_REQUIRED`;
4. população existente, com FC máxima, mas nenhum resultado `AVAILABLE`:
   `NO_ANALYZABLE_WORKOUTS`;
5. ao menos um resultado `AVAILABLE`: `AVAILABLE`, com `partial` quando
   aplicável.

Essa precedência mantém “Nenhum treino neste período” como estado vazio real,
mesmo quando FC máxima também estiver ausente. Com workouts e sem FC máxima,
a UI mostra somente a solicitação de configuração, sem carga zero sintética.

### R5 — Comparação

Comparar `current7` com `previous7` e `current28` com `previous28`.

```text
absoluteDelta = currentLoad - previousLoad
percentDelta = (currentLoad / previousLoad - 1.0) * 100
```

`percentDelta` só existe quando `previousLoad > 0`. Quando `previousLoad ==
0`, inclusive com `currentLoad > 0`, usar estado equivalente a
`NO_PREVIOUS_BASELINE` e texto “Sem base anterior”; nunca gerar infinito,
`NaN` ou percentual fabricado. Quando há baseline positivo, a UI pode dizer
“12% maior que os 7 dias anteriores” ou “8% menor que os 28 dias anteriores”,
sem classificar o resultado como bom ou ruim.

### R6 — Buckets diários

`current28` deve fornecer buckets por `LocalDate`, usando o `ZoneId` recebido.
Cada bucket deve expor pelo menos:

- `localDate`;
- `totalLoad`;
- `workoutCount`;
- `analyzedWorkoutCount`;
- `coveredHeartRateDuration`;
- `totalWorkoutDuration`;
- `coverageRatio`;
- `partial`.

Devem existir buckets de datas sem workout, com `totalLoad = 0` legítimo.
Workout sem análise aumenta `workoutCount` e `totalWorkoutDuration`, mas não
`analyzedWorkoutCount` nem carga. Assim, dia sem treino não se confunde com dia
com workout sem dados de FC. A soma de `totalLoad` dos buckets deve ser igual a
`current28.totalLoad` dentro da precisão do domínio.

### R7 — Agregação por tipo

Os últimos 28 dias devem ser agrupados pelo tipo resolvido. Cada grupo deve
expor `totalLoad`, `workoutCount`, `analyzedWorkoutCount` e `partial`.
Workouts sem análise continuam visíveis pelo contador e pelo estado parcial.
A soma de `totalLoad` dos grupos deve ser igual a `current28.totalLoad`.
Tipo não altera a fórmula nem a elegibilidade.

### R8 — Acesso a dados e read model

O histórico deve consultar no máximo o intervalo
`[asOf - 56 dias, asOf)`. O contrato recomendado é uma operação específica no
`WorkoutActivityRepository` que retorne um `Flow<List<WorkoutActivity>>`
completo, já bounded e filtrado por origem, descarte e `startedAt`.

Implementação recomendada:

- DAO aplica filtros temporais e de população;
- repository hidrata atividades completas e samples em uma fonte reativa
  agregada, preferencialmente com leituras filhas batched;
- mappers existentes continuam responsáveis por validar e reconstruir
  `WorkoutActivity`;
- não usar `CalendarWorkoutSummary` como fonte final de análise;
- não criar um Flow reativo por activity nem fazer o ViewModel buscar DAO;
- manter `observeActivities(ALL)` e a lista de atividades da tela semântica e
  funcionalmente inalterados.

O índice existente de samples por activity atende a leitura batched por IDs.
`workout_activities` não possui índice temporal composto; não criar migration
ou índice nesta spec sem evidência de plano de consulta que torne a bounded
query inviável. Se performance exigir índice, registrar necessidade e separar
mudança de schema em decisão/task própria.

### R9 — Integração de Exercícios

Exercícios deve expor um CTA acessível para abrir o histórico **Carga
cardiovascular** em página própria contextual. A seção detalhada não deve
permanecer inline no fluxo principal. A seleção `Semana`/`Mês`, navegação,
filtros e lista continuam existindo, sem depender do destino do histórico
cardiovascular. O histórico usa sua própria população e suas janelas de 7/28
dias; não deve herdar o filtro de workflow da lista.

A seção deve poder mostrar:

- resumo de 7 dias e comparação anterior;
- resumo de 28 dias e comparação anterior;
- cobertura de FC e “X de Y treinos analisados”;
- gráfico diário dos últimos 28 dias;
- contribuição por tipo.

Estados visíveis:

- sem FC máxima: “Configure sua FC máxima no Perfil para calcular o histórico
  de carga cardiovascular.”;
- sem workouts: “Nenhum treino neste período.”;
- com workouts, mas sem análise: “Não há dados de frequência cardíaca
  suficientes para calcular a carga deste período.”;
- falha: estado seguro sem detalhes sensíveis.

Não mostrar zero como substituto de ausência e não usar linguagem de carga
ideal, excesso, fadiga, recuperação, risco, overtraining ou recomendação.

### R10 — Efeitos proibidos

Alterar, limpar ou restaurar `maxHeartRateBpm` deve recompor somente o read
model visual. A 035 não pode:

- alterar `WorkoutActivity` ou `ActivityWorkflowState`;
- reprocessar ou criar `WorkoutDerivationSnapshot`;
- criar ou alterar `HealthSyncOperation`;
- ressincronizar Health Connect;
- persistir qualquer analytics histórico;
- mudar parser, BLE, D1, D3, D4 ou workflow.

## Contratos de domínio propostos

Os nomes são orientativos; devem seguir o padrão final encontrado na
implementação.

```text
CardiovascularLoadHistoryStatus
CardiovascularLoadPeriodSummary
CardiovascularLoadDailyBucket
CardiovascularLoadTypeSummary
CardiovascularLoadComparison
CardiovascularLoadHistoryResult
CardiovascularLoadHistory
CardiovascularLoadHistoryPolicy
```

`CardiovascularLoadHistoryPolicy` recebe `workouts`, `maxHeartRateBpm`,
`asOf` e `ZoneId`. `CardiovascularLoadHistoryResult` deve carregar status
tipado e dados opcionais, sem obrigar consumidores a interpretar zero como
ausência. A política deve ser pura: sem Room, Health Connect, ViewModel,
Compose, relógio global, escrita ou efeitos colaterais.

## Comportamento atual encontrado

### Policy 034 e perfil

- `WatchWorkoutHeartRateAnalysis.kt:105-190` filtra samples na janela,
  reutiliza `sampleAndHoldWindows`, calcula cobertura, classifica por razão
  `bpm / configuredMaximumBpm` e soma Edwards TRIMP.
- `WatchWorkoutHeartRateAnalysis.kt:36-75` distingue `AVAILABLE`,
  `CONFIGURATION_REQUIRED`, `NO_HEART_RATE_SAMPLES` e `UNAVAILABLE`.
- `ExercisesViewModel.kt:2391-2414` constrói a janela canônica da atividade e
  chama a policy 034 com samples completos e snapshot pessoal.
- `UserProfile.maxHeartRateBpm` é nullable e validado em `30..220` em
  `DeviceProfileDailySummary.kt:89-115`.
- `RoomRepositories.kt:252-278` expõe o valor no
  `PersonalMetricsSnapshot`; `RoomRepositories.kt:2060-2089` atualiza somente
  o perfil local.

### Room e repository

- `ChampionDatabase.kt:955` confirma `version = 20`.
- `ChampionMigrations.kt:796-801` mostra que v19 -> v20 apenas adiciona
  `maxHeartRateBpm`.
- `ChampionDaos.kt:358-405` possui summaries bounded por `startedAt` e
  interseção, excluindo descartados, mas sem filtro de origem.
- `CalendarHistory.kt:252-277` confirma que `CalendarWorkoutSummary` não
  carrega samples.
- `RepositoryContracts.kt:399-410` oferece os contratos de leitura atuais;
  `observeActivities(ALL)` não é bounded.
- `RoomRepositories.kt:990-1016` implementa a diferença entre leitura global,
  summaries por intervalo e interseção.
- `ChampionDatabase.kt:647-665` confirma a tabela de samples e índice por
  atividade.
- `RoomRepositories.kt:3638-3665` carrega import, intervalos, samples, header
  e derivação para reconstruir uma atividade completa.

### Exercícios

- `ExercisesScreen.kt:186-218` mantém período, mês, semana e filtros no estado
  da tela; `ExercisesScreen.kt:627-818` renderiza o histórico atual.
- Semana e mês têm semânticas diferentes para filtros: mês força `ALL`;
  histórico cardiovascular não deve depender disso.
- `ExercisesUiState` ainda não tem um read model histórico cardiovascular;
  `ExerciseDetailState` já possui análise individual 034.

## Abordagem escolhida

1. Congelar contratos e invariantes da 035 em `core.model`, reutilizando a
   policy 034 sem copiar sua lógica.
2. Criar agregação pura por workout, derivando as quatro janelas do `asOf` e
   fechando resumos, comparações, buckets e tipos em uma única passagem lógica.
3. Adicionar uma leitura bounded específica no `WorkoutActivityRepository`,
   com filtro persistente de origem, descarte e `startedAt`, hidratação completa
   e fluxo agregado; não usar N+1 reativo.
4. Compor no `ExercisesViewModel` o fluxo bounded, o snapshot pessoal, `asOf` e
   `ZoneId`; qualquer alteração de FC máxima recompõe tudo.
5. Expor CTA em Exercícios e renderizar o conteúdo em página própria
   contextual, preservando a seleção `Semana`/`Mês` da lista e sem mover a
   lógica para Compose.
6. Validar com banco e workouts existentes, cálculo independente e comparação
   de invariantes antes/depois, sem fabricar fixtures.

## Alternativas descartadas

- **Recalcular fórmula própria na 035:** duplicaria zonas, sample-and-hold,
  cobertura e Edwards TRIMP, podendo divergir da 034.
- **Usar `CalendarWorkoutSummary`:** summary não contém samples suficientes.
- **Usar `observeActivities(ALL)`:** carrega histórico indefinido e inclui
  descartados antes de filtragem.
- **Usar summary bounded seguido de `observeActivity` por ID:** repete N+1
  reativo já presente no read model diário e cria emissões intermediárias.
- **Consultar DAO no ViewModel:** viola ownership repository/read model.
- **Persistir carga ou snapshot:** impediria o recálculo deliberado com FC
  máxima atual e criaria migração/versionamento sem requisito.
- **Criar uma tabela diária ou índice imediatamente:** não há necessidade
  factual enquanto a query bounded e o índice existente forem suficientes.
- **Usar workflow como filtro analítico:** excluiria estados locais que devem
  contribuir e confundiria processo operacional com fato analítico.
- **Usar Health Connect:** a fonte autoritativa é local e a integração é
  write-only, sem carga cardiovascular.
- **Criar nova aba, Home ou tela global:** não corresponde à superfície
  histórica existente de Exercícios.
- **Manter a seção inline após a revisão de apresentação:** rejeitado em favor
  da opção 1, com CTA em Exercícios e página própria contextual; o conteúdo e
  os contratos analíticos permanecem os mesmos.

## Arquivos, módulos e contratos afetados

### Prováveis arquivos de implementação futura

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/`:
  contrato da história/agregação, possivelmente ao lado de
  `WatchWorkoutHeartRateAnalysis.kt`;
- `core/model/RepositoryContracts.kt`: contrato bounded completo do repository;
- `core/database/ChampionDaos.kt`: queries bounded e leituras batched;
- `data/RoomRepositories.kt`: composição reativa, hidratação e mapeamento;
- `data/DatabaseMappers.kt`: somente se novo record/read model exigir conversão;
- `feature/exercises/ExercisesFeatureContract.kt`: estado do histórico;
- `feature/exercises/ExercisesViewModel.kt`: combinação de fontes e policy;
- `feature/exercises/ExercisesScreen.kt`: CTA, remoção da seção inline e
  acessibilidade;
- `feature/exercises/CardiovascularLoadHistoryScreen.kt`: página própria com a
  seção, gráfico, estados e acessibilidade;
- `core/navigation/AppDestination.kt` e
  `core/navigation/ChampionNavigation.kt`: destino contextual, roteamento e
  retorno.

### Explicitamente não afetados

- `ChampionDatabase.kt` como versão/schema, salvo prova factual posterior;
- `ChampionMigrations.kt` e schemas exportados;
- entidades de `WorkoutActivity`, samples, snapshots e operações Health Connect;
- parsers D1/D3/D4, BLE, `WorkoutDerivationSnapshot` e workflow;
- Home, Saúde, Perfil e detalhe individual da 034.

## Riscos e mitigação

| Risco | Mitigação |
| --- | --- |
| Query temporal sem índice de `startedAt` degradar com histórico maior | Manter range máximo de 56 dias, observar plano/custo e separar eventual índice em nova decisão; não criar migration especulativa. |
| Hidratação completa carregar intervalos/header/derivação desnecessários | Encapsular no repository e preferir leitura batched; medir antes de alterar schema. |
| N+1 reativo por activity | Uma única fonte bounded no repository, sem `flatMapLatest` por ID no ViewModel. |
| Activity inválida ser convertida em zero | Falhar fechado ou marcar indisponibilidade; preservar contadores e não fabricar carga. |
| Ausência virar zero enganoso | Status tipado, UI por estado e contadores de workout/analisado. |
| `maxHeartRateBpm` ser confundida com máxima observada | Reutilizar snapshot pessoal e nomes distintos; nenhuma persistência histórica. |
| Buckets diários não fecharem o período | Atribuir exclusivamente por `startedAt`, usar `ZoneId` explícito e validar soma independente. |
| Comparação com baseline zero gerar infinito | Estado `NO_PREVIOUS_BASELINE`, sem divisão. |
| Filtro de Exercícios contaminar população analítica | Query própria no repository; filtro de UI permanece separado. |
| Destino contextual alterar barra, subtree ou retorno de Exercícios | Reutilizar back stack manual, `isExercisesDestination`, label contextual e saver existentes; não promover destino a área primária. |
| Alteração visual produzir efeitos remotos | Reutilizar `observeSnapshot` e comprovar assinatura de `health_sync_operations` antes/depois. |
| Baseline documental ainda apontar v19 | Registrar discrepância e usar código/schema/evidência v20 como autoridade factual; não misturar correção documental ampla. |
| Dados de saúde aparecerem na documentação/logs | Usar somente contratos, contagens e invariantes; não registrar BPM, samples, FC máxima ou carga em logs diagnósticos. |

## Estratégia de testes e validação

Não criar testes automatizados nesta execução. A implementação futura deve usar
os comandos e a validação física previstos abaixo, sem exigir treino novo e sem
fabricar sample/workout.

### Prova técnica futura

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- inspeção de Room v20, ausência de migration nova e schema sem tabela de carga;
- inspeção de query bounded, filtros e ausência de N+1 reativo;
- revisão de que ViewModel não acessa DAO e policy não acessa infraestrutura;
- revisão de que nenhum arquivo de parser, BLE ou Health Connect mudou.

### Prova física futura

Usar banco e workouts já existentes da Spec 034. Compor um dump sanitizado de
versão, contagens e estados, sem dados pessoais desnecessários, e comparar a
agregação da aplicação com cálculo independente. Comprovar:

1. Room continua em v20 e nenhuma migration nova foi criada.
2. FC máxima existente é reutilizada.
3. Cargas individuais continuam batendo com a Spec 034.
4. `current7`, `previous7`, `current28` e `previous28` fecham por soma
   independente.
5. Deltas absolutos fecham.
6. Percentual fecha quando há baseline positivo.
7. Baseline zero não gera infinito, `NaN` ou percentual falso.
8. Soma dos buckets diários fecha `current28.totalLoad`.
9. Soma por tipo fecha `current28.totalLoad`.
10. `totalWorkoutCount` e `analyzedWorkoutCount` estão corretos.
11. `coveredHeartRateDuration` fecha e `coverageRatio` usa
    `totalWorkoutDuration` como denominador.
12. Workout sem análise não se confunde com ausência de workout.
13. Alterar FC máxima recompõe o histórico e restaurar o valor restaura os
    resultados anteriores.
14. `WorkoutActivity`, `workflowState`, `WorkoutDerivationSnapshot` e
    `health_sync_operations` não mudam.
15. Reabrir/recompor a tela não persiste analytics.
16. Screenshot sanitizada mostra a nova seção e estados relevantes.

Se cenário não existir naturalmente, registrar `N/A`, motivo e risco residual.
Não fabricar treino ou sample para preencher checklist.

## Ordem das subtarefas

1. `035-01` — auditar histórico, Room, contratos e congelar decisões.
2. `035-02` — implementar domínio puro de agregação e comparação.
3. `035-03` — integrar leitura bounded de atividades completas até 56 dias.
4. `035-04` — integrar read model no `ExercisesViewModel`.
5. `035-05` — renderizar histórico, tendência, buckets e estados.
6. `035-06` — validar fisicamente, revisar efeitos colaterais e fechar.
7. `035-07` — substituir seção inline por CTA e página própria contextual.

`035-01` foi executada durante o planejamento documental. As subtarefas de
domínio e acesso podem avançar somente após esse contrato. `035-04` depende das
duas anteriores; `035-05` depende do read model; `035-06` fecha somente após
prova independente e documentação dos cenários N/A; `035-07` depende da
renderização e da validação física anteriores e altera somente apresentação e
navegação.

## Premissas explícitas

- `main` já contém a Spec 034 mergeada; a branch 035 não deve recriar branch ou
  fazer merge local.
- Room v20 e `maxHeartRateBpm` atual são fatos do código e schema, apesar da
  menção histórica a v19 no baseline.
- A FC máxima usada por toda a história é a configuração pessoal atual no
  momento da composição.
- A população analítica é independente de workflow operacional e de Health
  Connect.
- A policy 034 aceita carga parcial sem threshold e não transforma ausência de
  samples em análise.
- A leitura bounded de 56 dias é suficiente para as quatro janelas; qualquer
  necessidade de índice ou migration será bloqueadora e deverá ser replanejada,
  não implementada silenciosamente.
- A seleção `Semana`/`Mês` continuará controlando navegação da lista existente,
  não a população das métricas de carga.
- O histórico cardiovascular será aberto por CTA na página Exercícios e terá
  destino próprio contextual, sem entrada em Home, Saúde ou navegação principal.
- `asOf` e `ZoneId` são entradas estáveis do read model; nenhuma chamada global
  a `Instant.now()` entra na policy.
- O produto continua pessoal e local-first; a spec não reabre direção de treino
  phone, GPS, rota ou roadmap macro encerrado.
