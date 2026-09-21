# 035-04 — Integrar read model em Exercícios

- **Ticker:** `035`
- **Número:** `04`
- **Status:** `completed`

## Objetivo e resultado esperado

Compor o histórico cardiovascular no `ExercisesViewModel` a partir da leitura
bounded, do `PersonalMetricsSnapshot.maxHeartRateBpm`, de `asOf` e de
`ZoneId`, deixando o resultado pronto para apresentação sem cálculo na tela.

## Requisitos cobertos

- R2, R8, R9 e R10 da Spec 035.
- Recomputação completa ao alterar FC máxima.
- Separação entre população analítica e filtro/navegação da lista.
- Ausência de escrita ou efeitos Health Connect.

## Escopo incluído

- Campo de histórico cardiovascular em `ExercisesUiState` ou contrato equivalente.
- Composição de flows bounded + snapshot pessoal + contexto temporal estável.
- Reação a alteração de `maxHeartRateBpm`.
- Tratamento de loading, estados tipados, erro e dados disponíveis.
- Preservação do detalhe 034 e da lista de atividades atual.

## Escopo excluído

- Renderização final de gráfico/cards, pertencente à 035-05.
- Alteração da policy pura, pertencente à 035-02.
- Acesso DAO pelo ViewModel.
- Timer/polling sem requisito.
- Alteração de workflow, snapshots, atividades ou Health Connect.

## Dependências

- `035-02` domínio concluído.
- `035-03` leitura bounded concluída.
- `PersonalMetricsRepository.observeSnapshot()` existente.
- Contexto explícito de `asOf` e `ZoneId`.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesFeatureContract.kt`;
- `feature/exercises/ExercisesViewModel.kt`;
- `ExercisesUiState`, `ExerciseDetailState` e `WorkoutActivityRepository`;
- `PersonalMetricsSnapshot`;
- `ExercisesScreen` somente para fornecer/consumir contexto, sem domínio.

## Passos de implementação

1. Adicionar estado read model separado da `activityList` e do detalhe.
2. Definir origem estável de `asOf` e `ZoneId`; capturar uma vez por composição,
   sem chamar `Instant.now()` dentro da policy.
3. Combinar leitura bounded, snapshot pessoal e contexto temporal.
4. Invocar `CardiovascularLoadHistoryPolicy` em dispatcher apropriado, sem
   bloquear Compose.
5. Reemitir current7, previous7, current28, previous28, buckets e tipos quando
   snapshot de FC máxima mudar.
6. Manter filtro Semana/Mês apenas para histórico/lista existente.
7. Preservar estados de loading, erro e ausência sem zeros sintéticos.
8. Confirmar que recomposição/reopen não tem caminho de escrita.

## Testes e comandos de validação

- Confirmar mudança de FC máxima recomputa os quatro períodos e derivações.
- Confirmar mudança de filtro da lista não muda a população analítica.
- Confirmar `ZoneId` e `asOf` permanecem estáveis na emissão.
- Confirmar ViewModel não referencia DAO.
- Confirmar nenhuma operação Health Connect é disparada.
- Executar futuramente `./gradlew lint assembleDebug assembleRelease`.
- Não criar testes automatizados novos.

## Definição de pronto

- Estado expõe resultado histórico tipado e completo.
- FC máxima altera somente read model visual.
- Lista Semana/Mês e detalhe individual continuam funcionais.
- Sem timer, polling ou leitura indefinida.
- Erros e ausência chegam à UI sem conversão enganosa.

## Riscos e cuidados

- Não reutilizar `activityList` global para evitar carga sem bounded range.
- Não aplicar `selectedFilter` à população histórica.
- Não usar a seleção de mês como substituto de current28.
- Evitar recomputação duplicada para cada componente visual.
- Não logar conteúdo sensível do resultado.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt` — estado tipado do read model cardiovascular.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt` — composição bounded, snapshot pessoal, contexto temporal estável e policy em dispatcher de cálculo.
- `docs/tasks/035-training-load-history-trends/035-04-integrar-read-model-em-exercicios.md` — status, decisões, evidências e riscos.
- `docs/tasks/035-training-load-history-trends/035-00-overview.md` — checklist e progresso da Spec 035.

## Decisões e desvios

- Decisão: capturar `asOf` e `ZoneId.systemDefault()` uma vez por instância do ViewModel e derivar o range bounded de 56 dias a partir desse contexto.
- Decisão: combinar `observeWatchActivitiesBetween` com `observeSnapshot()` em fluxo separado da lista e invocar `CardiovascularLoadHistoryPolicy` em `Dispatchers.Default`.
- Decisão: expor `Loading`, `Ready(result)` e `Unavailable` no estado de Exercícios; estados analíticos da policy permanecem dentro de `CardiovascularLoadHistoryResult`.
- Decisão: capturar falha da fonte/composição como `Unavailable`, sem detalhes técnicos, carga sintética ou escrita.
- Desvio: nenhum.

## Comandos executados

- `./gradlew app:compileDebugKotlin`
- `./gradlew lint assembleDebug assembleRelease`
- `git diff --check`

## Resultados e evidências

- Primeira compilação após a implementação falhou por inferência do tipo de retorno no `map` do Flow; o tipo foi explicitado e a correção foi validada.
- `./gradlew app:compileDebugKotlin`: `BUILD SUCCESSFUL`.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL` na segunda execução; a primeira execução excedeu o timeout de 120 s durante tarefas finais, sem erro de compilação ou lint reportado.
- `git diff --check`: passou.
- Revisão independente: aprovada, sem blocker, major ou minor.
- Inspeção confirma fluxo histórico sem `selectedFilter`/`activityList`, sem acesso DAO, sem leitura/escrita Health Connect e sem persistência analítica.
- Não foram criados nem executados testes automatizados novos, conforme escopo da Spec 035 e `AGENTS.md`.

## Riscos residuais

- Recomposição numérica com banco/workouts reais, mudança e restauração de FC máxima, efeitos persistentes/remotos e screenshot permanecem validação física da 035-06.
- Renderização final do resultado, buckets, comparações e estados pertence à 035-05.
- Origem usa `ZoneId.systemDefault()` capturada no ViewModel; mudança de timezone do sistema durante a vida da instância só será refletida após reabertura/recriação do ViewModel.
