# 035-04 — Integrar read model em Exercícios

- **Ticker:** `035`
- **Número:** `04`
- **Status:** `pending`

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
