# Task 010-02 - Definir contratos de importação, estado e política diagnóstica

## Status

Pendente.

## Objetivo

Congelar contratos de domínio, estado, resultado tipado, metadata e política diagnóstica da Fase 10 antes de tocar protocolo, parser, persistência ou UI final.

## Contexto

`ExercisesFeatureContract.kt` ainda é vazio e o app não possui contratos tipados para tentativa D3/D4, metadata `WATCH_WORKOUT_D3_D4`, estado de progresso, política `PARTIAL` nem resultado final de importação.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-01-audit-d3-d4-baseline-domain-and-workflow-boundaries.md`
- `docs/protocol/workouts-d3-d4.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`

## Dependências

- `010-01`.

## Entradas

- achados auditados sobre D3/D4;
- padrões de contratos da Fase 09;
- retenção e fluxo diagnóstico já existentes.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DiagnosticPayload.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutImportSync.kt`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-02-define-watch-workout-import-contracts-state-and-diagnostic-policy.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesImportContractsTest.kt`

## Arquivos proibidos

- `core/database/**`
- `data/**`
- `core/bluetooth/**`
- `feature/exercises/ExercisesScreen.kt`
- `feature/home/**`
- `feature/device/**`

## Escopo

- definir owner `WATCH_WORKOUT_IMPORT` no gate;
- definir `WatchWorkoutImportSurfaceState` e `WatchWorkoutImportSessionState`;
- definir resultado tipado `Completed/Partial/Empty/Cancelled/Failed`;
- definir progresso, metadata e erro seguro;
- definir política `VALID/PARTIAL/INVALID` no contrato da fase;
- definir política de captura diagnóstica privada e derived outputs.

## Fora do escopo

- implementar protocolo D3/D4;
- implementar Room;
- implementar coordinator BLE;
- implementar UI final.

## Passos

1. Expandir contrato de Exercícios para estado, intents e resultado D3/D4.
2. Reservar owner `WATCH_WORKOUT_IMPORT` e contratos de repositório necessários.
3. Criar modelos duradouros para metadata, progresso, resultado, resumo e erro.
4. Formalizar política de payload privado, retenção, captura em release e derived outputs.
5. Atualizar spec e overview com contratos finais.

## Critérios de aceite

- contratos D3/D4 compiláveis e pequenos;
- owner `WATCH_WORKOUT_IMPORT` reservado;
- `PARTIAL` e `INVALID` definidos sem heurística oculta;
- metadata `WATCH_WORKOUT_D3_D4` modelada;
- nenhuma lógica BLE, Room ou UI final implementada.

## Testes pequenos aplicáveis

- modelos puros de estado e transição;
- validação inicial de resultado tipado e metadados.

## Validação mínima

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest` se houver teste pequeno novo de contrato.

## Evidências

- diff focado em contratos;
- lista dos modelos novos e onde cada task seguinte os consumirá.

## Atualização do overview

- marcar `010-02` como concluída;
- registrar paralelismo liberado para `010-03`, `010-04` e `010-06`.

## Validação final

- confirmar que protocolo, Room e UI final não foram implementados aqui.
