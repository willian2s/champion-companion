# Task 011-11 - Integrar banner da Home, descarte, validações e erros seguros

## Status

Concluída em 2026-07-24.

## Objetivo

Conectar resumo de workflows à Home, finalizar fluxos de descarte e confirmação, e fechar validações e mensagens seguras da fase.

## Contexto

Depois do detalhe existir, ainda faltam banner da Home, prioridade visual de calibração, confirmações explícitas, polish de erro seguro e integração final entre superfícies.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-08-extend-exercises-viewmodel-for-filters-review-and-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-09-implement-exercises-filters-list-and-detail-navigation.md`
- `docs/tasks/011-activity-review-calibration/011-10-implement-activity-detail-review-metrics-chart-and-intervals.md`

## Dependências

- `011-08`
- `011-09`
- `011-10`

## Entradas

- resumo por workflow já exposto por repository;
- detalhe e formulários já funcionando.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-11-integrate-home-banner-discard-validation-and-safe-errors.md`

## Arquivos proibidos

- `core/database/**`
- `data/**`
- `app/src/test/**`
- `app/src/androidTest/**`

## Escopo

- integrar banner da Home;
- priorizar calibração pendente;
- navegar `Home -> Exercícios`;
- finalizar confirmação de descarte e descarte de rascunho;
- fechar mensagens seguras e estados de erro.

## Fora do escopo

- validação manual final em dispositivo;
- fechamento documental da fase.

## Passos

1. Evoluir `HomeViewModel` e `HomeFeatureContract` para usar resumo local de workflows.
2. Evoluir `HomeScreen` com banner de pendências.
3. Ajustar `ChampionNavigation.kt` para navegação e confirmações finais necessárias.
4. Ajustar `ExercisesScreen.kt` e `ExercisesViewModel.kt` para descarte, erros seguros e confirmações restantes.

## Critérios de aceite

- Home mostra banner quando houver revisão ou calibração pendente;
- calibração recebe prioridade visual;
- toque no banner abre Exercícios;
- mensagens seguras cobrem not found, conflito de estado, `PARTIAL`, validação e erro interno;
- descarte exige confirmação explícita.

## Validação manual

- revisar copy segura e prioridades visuais;
- revisar fluxo de confirmação de descarte e de saída com alteração não salva.

## Comandos de build aplicáveis

- `./gradlew lint assembleDebug assembleRelease`

## Evidências

- diff da Home, da navegação e da feature Exercícios;
- screenshots da Home e do detalhe no PR.

## Screenshot quando visual

- obrigatória.

## Atualização do overview

- marcar `011-11` como concluída;
- registrar riscos residuais aceitos antes da validação manual final.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
