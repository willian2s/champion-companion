# 034-04 — Integrar read model no detalhe

- **Ticker:** `034`
- **Número:** `04`
- **Status:** `pending`

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

- **Status:** `pending` — dependente de domínio e persistência implementados.
- **Arquivos alterados:** nenhum arquivo de produto nesta fase de planejamento.
- **Decisões e desvios:** read model será efêmero e reativo; não será adicionado
  como fato em `WorkoutActivity` nem persistido.
- **Comandos executados:** inspeção de flows de `ExercisesViewModel`, contrato
  de detalhe, repository e `ExercisesScreen`.
- **Resultados e evidências:** ViewModel já observa atividade selecionada e
  `PersonalMetricsSnapshot`; ponto de inserção de estado foi identificado.
- **Riscos residuais:** composição concorrente pode exibir estado anterior ao
  trocar atividade; invalidar análise por `activityId` durante integração.
