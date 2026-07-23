# Task 010-10 - Integrar progresso, cancelamento, retry, lifecycle e erros seguros

## Status

Concluída em 2026-07-23.

## Objetivo

Fechar integração de lifecycle, cancelamento, retry, bloqueio de navegação e mensagens seguras entre Exercícios, Home, Saúde e Dispositivo para o novo owner `WATCH_WORKOUT_IMPORT`.

## Contexto

Após coordinator, ViewModel e UI básica, a fase ainda precisa impedir operação invisível, cancelar ao background, confirmar saída da tela e mostrar mensagens seguras quando outra feature estiver usando o gate foreground.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-07-implement-watch-workout-import-coordinator-and-foreground-gate.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-08-create-exercises-import-viewmodel.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-09-create-exercises-import-and-pending-ui.md`

## Dependências

- `010-07`
- `010-08`
- `010-09`

## Entradas

- UI básica pronta;
- coordinator e ViewModel prontos;
- gate foreground já funcional.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/ChampionManualHeartRateCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/ChampionD1HeartRateSyncCoordinator.kt`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-10-integrate-progress-cancel-retry-lifecycle-and-safe-errors.md`

## Arquivos proibidos

- `core/database/**`
- `data/**`
- manifesto, Gradle e DataStore.

## Escopo

- confirmação de saída da tela Exercícios;
- cancelamento ao background;
- retry com nova tentativa e novo permit;
- mensagens seguras de bloqueio em Home, Saúde, Dispositivo e Exercícios;
- liberação de gate em todos os caminhos visuais;
- tratamento de erros seguros finais.

## Fora do escopo

- validação física;
- revisão de atividade;
- Home banner completo da Fase 11.

## Passos

1. Bloquear saída da tela Exercícios durante importação ativa.
2. Cancelar ao background sem persistir bloco inseguro.
3. Integrar retry e dismiss de erro/resultado.
4. Atualizar mensagens de bloqueio seguras para o novo owner em Home, Saúde e Dispositivo.
5. Garantir cleanup visual e `finally` coerente em todos os caminhos.

## Critérios de aceite

- sair da tela exige confirmação quando importação estiver ativa;
- background cancela e libera gate;
- retry cria nova tentativa limpa;
- Home, Saúde e Dispositivo exibem mensagem segura para `WATCH_WORKOUT_IMPORT`;
- cancelamento não apaga importações já persistidas.

## Testes pequenos aplicáveis

- não criar novos testes nesta task sem pedido explícito do usuário;
- se o usuário pedir testes, priorizar background cancelando importação, callback tardio ignorado, mensagem segura do owner e retry com nova tentativa.

## Validação mínima

- `./gradlew assembleDebug`

## Evidências

- diff focado em navegação, ViewModel e mensagens seguras.

## Resultado

- `ChampionNavigation.kt` passou a bloquear saída da tela Exercícios durante importação ativa, com confirmação para troca de destino, back físico e fechamento da activity raiz.
- `ExercisesScreen.kt` agora observa lifecycle e cancela importação em `ON_STOP`, preservando blocos já persistidos e evitando operação invisível no background.
- `ChampionWatchWorkoutImportCoordinator.kt`, `ChampionManualHeartRateCoordinator.kt` e `ChampionD1HeartRateSyncCoordinator.kt` agora exibem mensagens seguras específicas do owner ativo quando o gate foreground rejeita nova operação.
- Retry e dismiss já existentes permaneceram válidos, preservando nova tentativa limpa com novo permit após estados terminais.

## Atualização do overview

- marcar `010-10` como concluída;
- registrar qualquer ajuste em paralelismo ou riscos aceitos para a etapa física.

## Validação final

- confirmar que validação física e fechamento documental permaneceram para tasks posteriores.

## Validação executada

- `./gradlew assembleDebug`
