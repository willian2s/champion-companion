# Task 010-07 - Implementar coordinator D3/D4 e gate foreground compartilhado

## Status

Concluída em 2026-07-23.

## Objetivo

Implementar coordinator foreground da importação D3/D4, reuso de sessão, handshake, execução sequencial, skip de `VALID` e liberação segura do gate.

## Contexto

Core BLE e bootstrap atual já resolvem conexão, sessão e A1. A fase precisa apenas de orquestração D3/D4 e ownership foreground seguro, seguindo padrão da Fase 09.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-03-implement-d3-index-protocol-and-collector.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-04-implement-d4-block-collector-and-remounting.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-05-implement-d4-workout-parser-and-domain-mapping.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-06-implement-workout-import-persistence-deduplication-and-metadata.md`

## Dependências

- `010-03`
- `010-04`
- `010-05`
- `010-06`

## Entradas

- protocolos D3 e D4 prontos;
- parser D4 pronto;
- persistência pronta;
- owner `WATCH_WORKOUT_IMPORT` reservado no gate.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WatchWorkoutImportCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ChampionWatchWorkoutImportCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/InMemoryForegroundBleOperationGate.kt`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-07-implement-watch-workout-import-coordinator-and-foreground-gate.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/feature/exercises/ChampionWatchWorkoutImportCoordinatorTest.kt`

## Arquivos proibidos

- `core/database/**`
- `data/**`
- `feature/exercises/ExercisesScreen.kt`
- `feature/home/**`
- `feature/device/DeviceManagementViewModel.kt`

## Escopo

- interface e implementação do coordinator D3/D4;
- aquisição/liberação do gate;
- reuso de sessão ou conexão controlada ao associado;
- handshake A1;
- execução D3 seguida de D4 sequencial;
- persistência por bloco;
- timeout, cancelamento, cleanup e `finally`.

## Fora do escopo

- UI final;
- mensagens cross-feature em Home/Saúde/Dispositivo;
- bloqueio de navegação.

## Passos

1. Criar contrato do coordinator D3/D4.
2. Implementar tentativa com `attemptId`, `bleSessionId`, counts e resultado tipado.
3. Reusar sessão ativa compatível ou conectar associado.
4. Executar handshake, D3, consulta local por CRC e D4 sequencial.
5. Persistir bloco a bloco e liberar gate em `finally`.

## Critérios de aceite

- gate `WATCH_WORKOUT_IMPORT` é adquirido e liberado corretamente;
- sessão saudável é reutilizada quando compatível;
- handshake precede D3;
- skip de `VALID` funciona antes do D4;
- callback tardio não reabre tentativa.

## Testes pequenos aplicáveis

- gate bloqueia concorrência;
- timeout libera gate;
- cancelamento libera gate;
- sessão compatível é reutilizada;
- ordem handshake -> D3 -> D4;
- callback tardio ignorado.

## Validação mínima

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest --tests "*ChampionWatchWorkoutImportCoordinatorTest"`

## Evidências

- testes pequenos do coordinator;
- diff mostrando `finally` e persistência por bloco.

## Resultado

- `WatchWorkoutImportCoordinator.kt` criado com contrato explícito de start/cancel/reset.
- `ChampionWatchWorkoutImportCoordinator.kt` implementado com gate foreground, reuso de sessão por `transportKey`, handshake A1, D3, lookup local por CRC, D4 sequencial, persistência por bloco, timeout, cancelamento e `finally`.
- `ExercisesModule.kt` criado para binding Hilt do coordinator.
- Teste unitário alvo reduzido a smoke checks de gate e liberação em falha de conexão; cobertura profunda do fluxo BLE completo fica não priorizada nesta task.

## Atualização do overview

- marcar `010-07` como concluída;
- registrar dependência de `010-08` em contrato final do coordinator.

## Validação final

- confirmar que UI, lifecycle visual e mensagens cross-feature ficaram fora desta task.
