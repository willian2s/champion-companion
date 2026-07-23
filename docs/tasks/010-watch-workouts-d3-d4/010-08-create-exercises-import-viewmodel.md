# Task 010-08 - Criar ExercisesViewModel para importação D3/D4

## Status

Concluída em 2026-07-23.

## Objetivo

Criar `ExercisesViewModel` como owner do fluxo de produto da tela Exercícios, mantendo estado separado para pendências persistidas, tentativa D3/D4, metadata, erro, progresso e resultado.

## Contexto

`ExercisesFeatureContract.kt` ainda não modela intents nem estado. O coordinator D3/D4 já estará pronto nesta etapa, então a ViewModel deve apenas orquestrar UI e repositórios, sem auto-start BLE.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-02-define-watch-workout-import-contracts-state-and-diagnostic-policy.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-07-implement-watch-workout-import-coordinator-and-foreground-gate.md`

## Dependências

- `010-07`.

## Entradas

- contratos de estado e coordinator prontos;
- `observePendingReview()` e metadata D3/D4 observáveis.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-08-create-exercises-import-viewmodel.md`

## Arquivos proibidos

- `core/database/**`
- `data/**`
- `core/bluetooth/**`
- `feature/exercises/ExercisesScreen.kt`

## Escopo

- criar ViewModel Hilt de Exercícios;
- observar pendências persistidas;
- observar metadata D3/D4;
- integrar coordinator D3/D4;
- expor intents de start, cancel, retry, dismiss e load;
- manter `Load` sem auto-start BLE.

## Fora do escopo

- layout final da UI;
- bloqueio de navegação;
- mensagens cross-feature fora de Exercícios.

## Passos

1. Expandir contrato da feature com intents e estado necessários.
2. Criar `ExercisesViewModel` e ligar observers locais.
3. Integrar start, cancel, retry e dismiss sem acoplar BLE ao Composable.
4. Garantir que `Load` observe dados, mas não inicie importação.

## Critérios de aceite

- `ExercisesViewModel` é owner do fluxo de produto;
- `Load` não inicia BLE;
- estado de pendências, metadata, erro, progresso e resultado fica separado;
- retry cria nova tentativa e novo estado terminal.

## Testes pequenos aplicáveis

- não criar novos testes nesta task sem pedido explícito do usuário;
- se o usuário pedir testes, priorizar ausência de auto-start em `Load`, start explícito, retry com nova tentativa e cancel sem apagar persistido.

## Validação mínima

- `./gradlew assembleDebug`

## Evidências

- diff focado em contrato e ViewModel.

## Resultado

- `ExercisesViewModel.kt` criado com `@HiltViewModel`, observando dispositivo associado, pendências persistidas, metadata D3/D4, goal diário e estado do coordinator.
- intents `StartImport`, `CancelImport`, `RetryImport`, `DismissImport`, `ConfirmExitImport`, `AppMovedToBackground` e `Load` agora passam pela ViewModel.
- `Load` permaneceu sem auto-start BLE; erro por ausência de relógio associado agora gera estado terminal seguro na própria feature.

## Atualização do overview

- marcar `010-08` como concluída;
- registrar campos reais do estado da feature.

## Validação final

- confirmar que a UI final ainda não foi implementada nesta task.
