# Task 011-09 - Implementar filtros, lista e navegação para detalhe

## Status

Planejada em 2026-07-24.

## Objetivo

Evoluir a superfície de Exercícios com resumo, filtros, cards clicáveis e navegação manual para detalhe, preservando a área de importação D3/D4.

## Contexto

`ExercisesScreen.kt` ainda mostra apenas CTA de importação, metadata e lista read-only. A navegação ainda não possui destino de detalhe de atividade importada.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-08-extend-exercises-viewmodel-for-filters-review-and-calibration.md`

## Dependências

- `011-08`.

## Entradas

- estado completo da feature já exposto pela ViewModel.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/AppDestination.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-09-implement-exercises-filters-list-and-detail-navigation.md`

## Arquivos proibidos

- `feature/home/**`
- `core/database/**`
- `data/**`
- `app/src/test/**`
- `app/src/androidTest/**`

## Escopo

- preservar área atual de importação;
- adicionar resumo e filtros;
- tornar cards clicáveis;
- adicionar destino de detalhe por `activityId`;
- manter navegação com back stack manual e sem `NavController`.

## Fora do escopo

- conteúdo completo da tela de detalhe;
- Home;
- review/calibração visual final.

## Passos

1. Adicionar novo destino em `AppDestination` para detalhe da atividade.
2. Evoluir `ChampionNavigation.kt` para `Home -> Exercícios -> Detalhe`.
3. Evoluir `ExercisesScreen.kt` com resumo, filtros e cards clicáveis.
4. Atualizar spec e overview.

## Critérios de aceite

- área de importação existente preservada;
- lista mostra filtros e resumo de workflow;
- cards navegam para detalhe por `activityId`;
- navegação continua manual e sem objeto completo na rota.

## Validação manual

- revisar back stack manual e novos destinos;
- confirmar ausência de `NavHost` e `NavController`.

## Comandos de build aplicáveis

- `./gradlew lint assembleDebug assembleRelease`

## Evidências

- diff da tela e da navegação;
- screenshot da lista com filtros durante implementação/PR.

## Screenshot quando visual

- obrigatória.

## Atualização do overview

- marcar `011-09` como concluída;
- registrar qualquer novo hotspot em `ExercisesScreen.kt` ou `ChampionNavigation.kt`.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
