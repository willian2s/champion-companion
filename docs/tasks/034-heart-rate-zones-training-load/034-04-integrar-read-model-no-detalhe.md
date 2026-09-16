# 034-04 — Integrar read model no detalhe

- **Ticker:** `034`
- **Número:** `04`
- **Status:** `completed`

## Objetivo

Combinar a atividade selecionada, a série temporal persistida e o snapshot atual
do perfil para expor `WorkoutHeartRateAnalysis` no estado do detalhe, recalculando
quando FC máxima mudar sem alterar fatos ou workflow.

## Resultado esperado

`ExercisesViewModel`/contrato de detalhe entrega read model derivado com estados
tipados de disponível, configuração necessária, sem samples e indisponível, sem
colocar cálculo de domínio dentro de `ExercisesScreen`.

## Requisitos cobertos

- R4, R5, R6 e R7 da Spec 034;
- critérios de aceite 9 a 17;
- read model baseado em atividade + perfil atual e não em Health Connect.

## Escopo incluído

- composição da policy com `selectedActivity` e `PersonalMetricsSnapshot`;
- estado do detalhe para análise e cobertura;
- atualização reativa ao mudar atividade ou FC máxima;
- escopo para workouts watch com samples locais persistidos;
- preservação de atividades sem samples, sem config e com cobertura parcial;
- integração sem alterar `ActivityWorkflowState`, snapshots ou sync.

## Escopo excluído

- editor de Perfil e migration;
- alteração dos fatos `WorkoutActivity` ou tabelas de samples;
- alteração dos gráficos atuais de FC, ritmo ou cadência;
- Health Connect, parser, BLE, reprocessamento e classificação de esforço;
- persistência do read model.

## Dependências

- `034-02` e `034-03` concluídas;
- `ExercisesViewModel` já observa `selectedActivity` e snapshot pessoal;
- contrato de detalhe atual e `ActivityWorkflowState` preservados.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesFeatureContract.kt` — `ExercisesUiState` e
  `ExerciseDetailState`;
- `feature/exercises/ExercisesViewModel.kt` — flows de atividade, perfil e
  composição do estado;
- `core/model/WatchWorkoutHeartRateAnalysis.kt` — policy/result;
- `data/RoomRepositories.kt` somente se a leitura de samples precisar ser
  adaptada; não criar persistência analítica;
- `feature/exercises/ExercisesScreen.kt` — receber estado, sem regra de cálculo.

## Passos de implementação

1. Definir ponto único de composição do read model.
2. Filtrar atividades fora do escopo watch-only sem reabrir fluxo phone.
3. Passar sessão e samples persistidos à policy.
4. Passar somente `maxHeartRateBpm` do snapshot atual.
5. Expor status e análise no detalhe, preservando loading/not-found/error
   existentes.
6. Recalcular quando `selectedActivity` ou snapshot pessoal emitirem mudança.
7. Garantir que atualização não chama repository de derivação, workflow ou
   coordinator de Health Connect.
8. Adicionar contrato de acessibilidade/estado necessário para a UI, sem valores
   sintéticos.

## Testes e comandos de validação

- inspeção de recomposição e dependências do ViewModel;
- validação manual de três estados: sem config, sem samples e parcial;
- `git diff --check`;
- não criar ou executar testes automatizados nesta task.

## Definição de pronto

- read model não é persistido;
- mudança de FC máxima muda apenas estado derivado do detalhe;
- sem samples não há carga/zona inventada;
- partial coverage permanece disponível;
- `SYNCED`, workflow e operações Health Connect permanecem intocados;
- tela não contém fórmula ou classificação temporal duplicada.

## Riscos e cuidados

- Não adicionar análise como campo factual de `WorkoutActivity`.
- Não usar snapshots históricos para congelar zonas/carga.
- Não recalcular a atividade nem promover workflow ao atualizar Perfil.
- Não buscar dados remotos para completar cobertura.
- Não deixar estado antigo de uma atividade aparecer após trocar
  `selectedActivity`.

## Registro da execução

- **Status:** `completed` — read model efêmero integrado após `034-02` e `034-03`.
- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutHeartRateAnalysis.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`.
- **Decisões e desvios:** `ExerciseDetailState.Ready` agora expõe
  `WorkoutHeartRateAnalysisResult`. O ViewModel compõe a análise somente para
  atividades `WATCH_D3_D4`, usando janela `startedAt + duration`, samples da
  atividade e `maxHeartRateBpm` do snapshot atual. Atividades fora do escopo
  retornam `UNAVAILABLE/OUT_OF_SCOPE`; nenhuma análise foi persistida e nenhum
  workflow, snapshot, derivação ou operação Health Connect foi acionado. A troca
  de atividade invalida imediatamente seleção, calibração e detalhe; guardas por
  `activityId` impedem estado antigo. Nenhum desvio de requisito.
- **Comandos executados:** inspeção de spec, ADR-034, baseline, overview,
  subtarefas, contratos, repository, ViewModel e detalhe; `./gradlew
  app:compileDebugKotlin` (primeira execução identificou nullable incorreto,
  corrigido); `./gradlew app:compileDebugKotlin`; `./gradlew lint assembleDebug
  assembleRelease`; `git diff --check`. Nenhum teste automatizado foi criado ou
  executado.
- **Resultados e evidências:** compilação Kotlin, lint e builds debug/release
  passaram. Mudança de snapshot recompõe somente `heartRateAnalysis`; estados
  `AVAILABLE`, `CONFIGURATION_REQUIRED`, `NO_HEART_RATE_SAMPLES` e
  `UNAVAILABLE` permanecem tipados pela policy. Cobertura parcial continua
  disponível, ausência de samples não vira zero e o diff não toca parser, BLE,
  workflow, snapshots de derivação ou Health Connect. Revisão independente final
  não encontrou bloqueadores, altos, médios ou baixos.
- **Riscos residuais:** validação física, cálculo independente e confirmação de
  ausência de alterações em `health_sync_operations` permanecem em `034-06`.
  Renderização visual e acessibilidade permanecem em `034-05`. O repository
  observa a atividade composta; eventual atualização isolada da tabela de
  samples continua fora desta subtarefa e segue risco já documentado.
