# Task 011-08 - Evoluir ExercisesViewModel para filtros, revisão e calibração

## Status

Concluída em 2026-07-24.

## Objetivo

Transformar `ExercisesViewModel` em owner do fluxo completo local de lista, detalhe, revisão, calibração e descarte, mantendo importação D3/D4 separada.

## Contexto

Hoje `ExercisesViewModel` só observa pendências `PENDING_REVIEW`, metadata D3/D4, goal diário e coordinator de importação. Faltam filtros, detalhe, resumo de workflow e formulários.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-05-implement-activity-list-detail-and-workflow-queries.md`
- `docs/tasks/011-activity-review-calibration/011-06-implement-review-persistence-and-state-transitions.md`
- `docs/tasks/011-activity-review-calibration/011-07-implement-treadmill-calibration-workflow.md`

## Dependências

- `011-05`
- `011-06`
- `011-07`

## Entradas

- repositórios completos de lista/detalhe/review/calibração;
- contratos de UI já definidos.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-08-extend-exercises-viewmodel-for-filters-review-and-calibration.md`

## Arquivos proibidos

- `ExercisesScreen.kt`
- `feature/home/**`
- `core/navigation/**`
- `core/database/**`
- `data/**`
- `app/src/test/**`
- `app/src/androidTest/**`

## Escopo

- observar filtros, lista, detalhe e resumo por workflow;
- manter importação D3/D4 separada do fluxo local de review;
- carregar e manter formulários de revisão e calibração;
- enviar ações de review, calibração e descarte;
- impedir duplo submit;
- manter erros seguros no estado.

## Fora do escopo

- renderização Compose final;
- Home;
- navegação real.

## Passos

1. Expandir intents e ui state conforme contratos da fase.
2. Conectar observação de filtro, lista, detalhe e resumo.
3. Conectar ações de review, calibração e descarte.
4. Tratar loading, `NotFound`, `Failed` e confirmação de descarte de rascunho.

## Critérios de aceite

- `ExercisesViewModel` concentra estado do fluxo local de review/calibração;
- importação D3/D4 continua separada;
- formulário não persiste a cada tecla;
- submit duplo fica bloqueado;
- erros seguros ficam no estado e não em exceções cruas de UI.

## Validação manual

- revisar agregação de estado e intents;
- conferir que BLE/coordinator não foram acoplados aos formulários.

## Comandos de build aplicáveis

- `./gradlew lint assembleDebug assembleRelease`

## Evidências

- diff do ViewModel e do FeatureContract;
- tabela resumida de novos estados e intents.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `011-08` como concluída;
- registrar novos hotspots de feature.

## Execução

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-08-extend-exercises-viewmodel-for-filters-review-and-calibration.md`

### Resultado no FeatureContract

- `ExercisesIntent` passou a incluir intents para:
  - filtro;
  - abertura/fechamento de detalhe;
  - edição de tipo, título e observações;
  - submit de review;
  - edição de distância de calibração;
  - submit de calibração;
  - descarte;
  - dismiss de erro.

### Resultado no ViewModel

- `ExercisesViewModel` passou a observar:
  - `observeActivities(filter)`;
  - `observeWorkflowSummary()`;
  - `observeActivity(activityId)`;
  - `observeLatestCalibration(activityId)`.
- `ExercisesViewModel` agora mantém:
  - filtro selecionado;
  - lista;
  - resumo por workflow;
  - atividade selecionada;
  - detalhe carregado;
  - formulário de review;
  - formulário de calibração;
  - erro seguro da operação.
- Importação D3/D4 permaneceu separada e sem acoplamento ao fluxo local de review/calibração.
- Submit duplo foi bloqueado por `isSaving` nos formulários.

### Gate executado

- `./gradlew lint assembleDebug assembleRelease`
- resultado: sucesso.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
- Confirmado: nenhuma renderização Compose final, Home ou navegação real foi implementada nesta task.
