# Task 010-09 - Criar UI de importação e pendências na tela Exercícios

## Status

Concluída em 2026-07-23.

## Objetivo

Substituir placeholder da tela Exercícios por UI de importação D3/D4, última importação, progresso, resultado e lista simples read-only de pendências.

## Contexto

`ExercisesScreen.kt` ainda mostra `PlaceholderScreen("Exercícios")`. A ViewModel da fase já deve existir, então a task fecha a superfície visual sem ainda resolver navegação bloqueada nem mensagens cross-feature.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-08-create-exercises-import-viewmodel.md`

## Dependências

- `010-08`.

## Entradas

- `ExercisesViewModel` pronto;
- contrato de estado da feature estabilizado.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-09-create-exercises-import-and-pending-ui.md`

## Arquivos proibidos

- `core/database/**`
- `data/**`
- `core/bluetooth/**`
- `feature/home/**`
- `feature/device/**`

## Escopo

- remover placeholder;
- CTA explícita de importação;
- progresso e resultado;
- última importação;
- estados vazio e erro;
- lista read-only de pendências;
- acessibilidade textual.

## Fora do escopo

- bloqueio de navegação;
- background cancel;
- mensagens de bloqueio em outras features;
- revisão da atividade.

## Passos

1. Conectar `ExercisesScreen` à `ExercisesViewModel`.
2. Criar estados ocioso, importando, concluído, parcial, vazio e erro.
3. Exibir CTA, progresso, resultado e última importação.
4. Exibir lista simples read-only de pendências `PENDING_REVIEW`.
5. Garantir acessibilidade e copy em pt-BR.

## Critérios de aceite

- placeholder removido;
- CTA explícita visível;
- progresso, resultado e pendências renderizados;
- lista permanece somente leitura;
- nenhuma ação de revisão ou sincronização aparece.

## Testes pequenos aplicáveis

- não criar novos testes nesta task sem pedido explícito do usuário.

## Validação mínima

- `./gradlew assembleDebug`
- `./gradlew installDebug`

## Evidências

- screenshot da tela para futuro PR;
- diff restrito a Exercícios.

## Resultado

- `ExercisesScreen.kt` passou a consumir `ExercisesViewModel` e renderizar CTA explícita, progresso, resultado, metadata da última importação e lista read-only de pendências.
- Placeholder foi removido sem introduzir revisão, Health Connect ou navegação bloqueada.
- Screenshot ficou pendente para PR futuro, conforme execução local desta task.

## Atualização do overview

- marcar `010-09` como concluída;
- registrar screenshot pendente ou anexada, conforme execução real.

## Validação final

- confirmar que revisão, detalhes completos e Health Connect não entraram nesta task.
