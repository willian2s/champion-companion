# 036-03 — Integrar read model cardiovascular

- **Ticker:** `036`
- **Número:** `03`
- **Status inicial:** `pending`

## Objetivo e resultado esperado

Entregar a extensão de zonas através da composição cardiovascular existente,
garantindo que o ViewModel reutilize a fonte bounded, o snapshot pessoal, `asOf`
e `ZoneId` sem criar fluxo, query ou cálculo paralelo.

## Requisitos cobertos

- R2, R3, R5, R6 e R10 da Spec 036.
- Reutilização de `CardiovascularLoadWorkoutInput` e
  `observeCardiovascularLoadWorkoutsBetween()`.
- Recomposição pela FC máxima e ausência de efeitos persistentes/remotos.

## Escopo incluído

- Adaptar tipos de estado somente se a extensão do resultado exigir.
- Preservar `CardiovascularLoadHistoryUiState` e estados tipados existentes.
- Confirmar que um resultado histórico contém zonas de períodos e buckets.
- Garantir que alteração de `PersonalMetricsSnapshot.maxHeartRateBpm` recompõe
  toda a distribuição.
- Garantir que falha estrutural continua `UNAVAILABLE` e falha individual não
  derruba período com análises disponíveis.
- Manter filtro da lista de Exercícios separado da população histórica.

## Escopo excluído

- Nova leitura bounded, DAO, mapper, entidade, migration ou alteração de Room.
- Cálculo em Compose ou nova policy.
- Alteração de `WorkoutActivity`, workflow, snapshots, derivação ou Health
  Connect.
- Nova aba/destino.

## Dependências

- `036-01` e `036-02` concluídas.
- `ExercisesViewModel` e `ExercisesFeatureContract` da Spec 035.
- `PersonalMetricsRepository.observeSnapshot()` existente.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesViewModel.kt`:
  `cardiovascularLoadHistoryState`, `cardiovascularHistoryAsOf`,
  `cardiovascularHistoryRange` e composição de flows.
- `feature/exercises/ExercisesFeatureContract.kt`:
  `CardiovascularLoadHistoryUiState`.
- `core/model/WorkoutHeartRateLoadHistory.kt` e
  `core/model/RepositoryContracts.kt` para contratos existentes.

## Passos de implementação

1. Confirmar que o fluxo atual já recebe exatamente a fonte bounded de 56 dias.
2. Propagar o resultado estendido sem duplicar fluxo ou policy.
3. Manter `asOf`/`ZoneId` estáveis durante a vida da composição.
4. Reagir a mudanças de FC máxima somente por recomposição em memória.
5. Preservar loading, indisponibilidade estrutural, população vazia,
   configuração ausente e disponibilidade parcial.
6. Revisar que ViewModel não acessa DAO e não usa `activityList`/filtros para o
   histórico.
7. Comparar saída de carga 035 antes/depois da extensão.

## Testes e comandos de validação

- Inspeção de fluxo bounded e ausência de N+1.
- Verificar recomposição com mudança/restauração de FC máxima.
- Verificar ausência de chamadas Health Connect.
- `./gradlew lint assembleDebug assembleRelease` após integração de código.
- Não criar testes automatizados novos.

## Definição de pronto

- Estado da UI recebe distribuição completa sem nova fonte.
- Mudança de FC máxima recalcula zonas históricas.
- População, janelas, carga e estados 035 permanecem preservados.
- Nenhum DAO é acessado pelo ViewModel.
- Nenhum efeito persistente ou remoto ocorre.

## Riscos e cuidados

- Não reintroduzir hidratação completa que depende de snapshots/splits.
- Não deixar `current7`/`current28` depender da seleção Semana/Mês da lista.
- Não ocultar falha individual quando houver `partial`.
- Não transformar `history == null` em lista de zonas vazia.
