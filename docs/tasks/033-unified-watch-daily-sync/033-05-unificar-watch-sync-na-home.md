# 033-05 — Unificar watch sync na Home

- **Status:** completed
- **Depende de:** 033-02 e 033-03
- **Evidência física:** `docs/integration/033-05-unified-watch-sync-physical-validation.md`

## Objetivo

Fazer do pull-to-refresh da Home o único entrypoint de leitura de FEA1, D1 e
D3/D4.

## Escopo

- `WatchSyncCoordinator`;
- owner foreground único;
- reuso de conexão/sessão/handshake;
- sequência FEA1 -> D1 -> D3/D4;
- persistência por componente;
- partial success sem rollback global;
- metadata/timestamps por componente;
- estados de Home claros para progresso, partial, retry e erro;
- dedup/skip D3/D4 existentes preservados.

## Guard rails

- não sincronizar Health Connect automaticamente;
- não iniciar watch sync ao abrir app/tela;
- não tornar D1/D4 dependentes de Compose;
- não reintroduzir background polling.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ChampionWatchWorkoutImportCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WatchWorkoutImportCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/ChampionD1HeartRateSyncCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/ChampionManualHeartRateCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/D1HeartRateSyncCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/ChampionDailyStepsSyncCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/WatchSyncCoordinator.kt`
- `docs/tasks/033-unified-watch-daily-sync/033-00-overview.md`
- `docs/tasks/033-unified-watch-daily-sync/033-05-unificar-watch-sync-na-home.md`

## Decisões e desvios

- `ChampionWatchSyncCoordinator` tornou-se owner único do gate `WATCH_SYNC` e
  coordena FEA1, D1 e D3/D4 sequencialmente sobre a mesma sessão e handshake.
- FEA1 foi movido para o coordinator composto, incluindo persistência do resumo;
  D1 e D3/D4 receberam operações suspendíveis internas que não adquirem gate nem
  repetem handshake quando chamadas pelo fluxo composto.
- Falha ou cancelamento de componente mantém persistências anteriores e permite
  continuação dos componentes seguintes; resultado e timestamps permanecem
  observáveis por componente na Home.
- Deduplicação/skip existente de D3/D4 foi preservada. A persistência D1 mantém
  fatos válidos quando houver pacote estrutural inválido, classificando o
  componente como parcial quando aplicável.
- O auto-sync do Health Connect após refresh da Home foi removido. Settings e
  demais ações explícitas de Health Connect não foram alteradas.
- O owner adicional exigiu branches de mensagem nos consumidores do gate; não
  houve alteração funcional nos fluxos de conexão manual, medição manual ou
  sessão de treino.

## Comandos executados

```text
./gradlew app:compileDebugKotlin
./gradlew lint assembleDebug assembleRelease
git diff --check
```

## Resultados e evidências técnicas

- `./gradlew app:compileDebugKotlin`: sucesso após integração do coordinator,
  bindings Hilt e tratamento de partial/cancelamento.
- `./gradlew lint assembleDebug assembleRelease`: sucesso; lint, compilação e
  empacotamento debug/release concluídos.
- `git diff --check`: sucesso, sem erro de whitespace.
- Não foram criados nem executados testes automatizados, conforme `AGENTS.md` e
  a regra da task.
- Revisão independente final: aprovado, sem achados blocker/high/medium/low.
- Revisão independente identificou e foi incorporada correção para cancelamento
  terminal da Home, `COMPLETED_WITH_GAPS` como parcial e preservação de amostras
  D1 válidas quando houver conversão parcial.

## Validação física pós-task

Refreshes sucessivos em Champion C001 real confirmaram:

- um único pull da Home executa FEA1 -> D1 -> D3/D4;
- D1 permaneceu idempotente em tentativa sem mudança (`0` inserts, `0` updates e
  samples existentes classificados como duplicatas);
- D3/D4 permaneceu idempotente, sem recriar workouts já válidos;
- um workout novo foi descoberto e persistido pelo fluxo unificado sem usar os
  entrypoints de Saúde/Exercícios;
- a fila Health Connect permaneceu sem tentativa remota automática;
- FEA1 pode avançar alguns minutos à frente da soma D1 mesmo com D1
  estruturalmente completo.

O último item gerou a correção de contrato absorvida pela 033-06: cauda recente
`FEA1 > ΣD1` não é automaticamente divergência e não pode virar residual temporal
sintético.

Detalhes sanitizados em:
`docs/integration/033-05-unified-watch-sync-physical-validation.md`.

## Riscos residuais

- Partial success físico deliberadamente forçado por falha de componente ainda
  pertence à validação 033-09.
- Reuso de sessão/handshake foi exercitado no fluxo real, mas falhas intermediárias
  e recovery continuam gates finais da 033-09.
- Semântica de campos raw D1 e cobertura física de layouts continuam riscos já
  registrados nas subtarefas anteriores.
- O fluxo D1 legado ainda possui auto-sync Health Connect em `HealthViewModel`; a
  remoção desse entrypoint pertence à 033-06 e não é usada pelo watch sync da Home.
- Testes automatizados históricos não foram executados e podem conter
  expectativas legadas de D1 v1.
