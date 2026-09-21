# ADR-035 — Histórico de carga cardiovascular e tendência

- **Status:** aceita para planejamento
- **Data:** 2026-09-21
- **Relacionada:** [Spec 035](../specs/035-training-load-history-trends.md)
- **Base:** Spec 034 concluída, Room v20

## Contexto

A Spec 034 calcula carga cardiovascular individual como read model efêmero a partir de `WorkoutActivity`, samples persistidos e `UserProfile.maxHeartRateBpm`. Não existe carga persistida por workout. A evolução necessária é agregar essa mesma saída em janelas históricas, sem criar uma segunda interpretação da série de frequência cardíaca.

O acesso atual por `CalendarWorkoutSummary` é bounded, mas não carrega samples. O acesso global `observeActivities(ALL)` é completo, porém indefinido e inclui atividades descartadas. A tela de Exercícios já possui histórico Semana/Mês, mas seu `activityList` e filtros não podem determinar a população analítica.

## Decisão

### 1. Uma única fonte de carga

`CardiovascularLoadHistoryPolicy` reutilizará diretamente `WorkoutHeartRateAnalysisPolicy` para cada workout elegível. A 035 não repetirá classificação de zonas, sample-and-hold, cobertura ou Edwards TRIMP.

O domínio receberá `workouts`, `maxHeartRateBpm`, `asOf` e `ZoneId`. `asOf` é obrigatório e fornecido pelo chamador; o domínio puro não chama `Instant.now()`. O `ZoneId` somente define conversão para `LocalDate` e permanece explícito.

### 2. Janelas e população

As janelas serão semiabertas e calculadas por `startedAt`: current7 `[asOf - 7 dias, asOf)`, previous7 `[asOf - 14 dias, asOf - 7 dias)`, current28 `[asOf - 28 dias, asOf)` e previous28 `[asOf - 56 dias, asOf - 28 dias)`. A população é `origin = WATCH_D3_D4` e `workflowState != DISCARDED`. Todos os demais estados operacionais são elegíveis. Workout não é dividido entre períodos.

### 3. Resultado não persistido

Resumo de período, comparação, bucket diário e grupo por tipo são derivados em memória. Não haverá tabela, coluna, snapshot, migration, backfill, operação Health Connect ou alteração de atividade. Alterar `maxHeartRateBpm` recompõe o histórico inteiro visualmente.

### 4. Ausência e comparação

O read model usará estados tipados `AVAILABLE`, `CONFIGURATION_REQUIRED`, `NO_WORKOUTS`, `NO_ANALYZABLE_WORKOUTS` e `UNAVAILABLE`, com precedência: falha de fonte, população vazia, configuração ausente, nenhum resultado analisável, ou ao menos um resultado disponível. Ausência nunca será simulada por zero. Comparações só calculam percentual quando `previousLoad > 0`; baseline zero produz `NO_PREVIOUS_BASELINE` e “Sem base anterior”.

### 5. Leitura bounded

Adicionar contrato específico ao `WorkoutActivityRepository` para uma única fonte reativa de atividades completas watch no máximo usado pela 035. Filtros de `origin`, `workflowState` e `startedAt` ficam no DAO/repository; retorno contém samples e fatos necessários à policy 034; hidratação deve ser agrupada/batched; ViewModel não acessa DAO; não criar `Flow` por activity. `CalendarWorkoutSummary` permanece contrato leve de calendário.

### 6. Sem migration nesta entrega

Room permanece v20. A ausência de índice temporal composto em `workout_activities` é risco de performance, não motivo suficiente para migration especulativa. Se medição demonstrar custo incompatível com 56 dias, a necessidade será documentada em trabalho separado. Nenhuma tabela de carga será criada.

### 7. Superfície de UI

Renderizar a seção na tela histórica de Exercícios, sem nova aba, tela global ou Home. Semana/Mês continua controlando a navegação existente; a seção usa suas janelas de 7/28 dias e sua população analítica.

## Alternativas rejeitadas

- Duplicar cálculo da Spec 034: criaria divergência em zonas, sample-and-hold e cobertura.
- `CalendarWorkoutSummary` + `observeActivity` por ID: não tem samples e repete N+1 reativo.
- `observeActivities(ALL)`: não é bounded, inclui descartados e carrega histórico completo.
- Nova tabela, snapshot ou carga persistida: contradiz recalculabilidade com FC máxima atual e cria migration.
- Índice temporal ou migration v21 imediata: adiada até evidência de plano/performance.
- Nova aba ou tela global: a superfície histórica de Exercícios já existe.

## Consequências

### Positivas

- detalhe 034 e histórico usam a mesma policy;
- alteração de FC máxima recompõe passado sem reescrever fatos;
- análise permanece local e sem Health Connect;
- janela máxima limita custo;
- buckets, tipos e períodos permitem validação independente;
- ausência de workout, análise e configuração permanece distinguível.

### Negativas e riscos aceitos

- carga histórica muda quando FC máxima muda;
- hidratação completa pode carregar dados auxiliares;
- falta de índice temporal pode exigir investigação;
- sample-and-hold mantém riscos aceitos da ADR-034;
- baseline documental histórico ainda menciona Room v19;
- prova numérica depende de validação física, sem testes automatizados novos.

## Contratos relacionados

- `WorkoutHeartRateAnalysisPolicy` e `WorkoutHeartRateAnalysis` da Spec 034;
- `PersonalMetricsSnapshot.maxHeartRateBpm`;
- `WorkoutActivityRepository` e novo contrato bounded;
- `CalendarInstantRange`/`CalendarDateRange`;
- `ExercisesUiState` e `ExercisesViewModel`.

## Limites

Esta decisão não altera parser, BLE, D1, D3, D4, workflow, snapshots de derivação, Health Connect, treino phone, GPS, rota ou roadmap macro. Não define meta de carga, esforço ideal, fadiga, recuperação, risco clínico ou recomendação comportamental.
