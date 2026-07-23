# Task 010-00 - Overview Importação de treinos D3/D4

## Status

Planejada em 2026-07-23. `010-01` e `010-02` concluídas; `010-03` a `010-13` pendentes.

## Objetivo

Controlar escopo, ordem, dependências, paralelismo, checklist, validação física e dívida técnica da Fase 10, dedicada à importação local-first de treinos D3/D4 até `PENDING_REVIEW`.

## Documentos autoritativos

- `AGENTS.md`
- `docs/specs/_inputs/phase-10-watch-workouts-d3-d4-direction.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- ADRs vigentes em `docs/decisions/`
- `docs/decisions/adr-010-d1-local-first-and-health-connect-phase-boundary.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- todas as tasks concluídas da Fase 09
- specs e overviews das Fases 03 a 08
- `docs/protocol/workouts-d3-d4.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/history-d1.md`
- `docs/protocol/diagnostics-d8-a8.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

## Status inicial

- `ExercisesScreen.kt` continua placeholder e `ExercisesFeatureContract.kt` continua vazio.
- `WatchWorkoutActivity.kt`, `DiagnosticPayload.kt` e Room já oferecem base de domínio e persistência para D3/D4.
- `WorkoutActivityRepository.persistImported(...)` já preserva revisão/calibração e substitui intervalos e amostras em reimportação.
- `observePendingReview()` já existe, mas não há `ExercisesViewModel`, coordinator de importação D3/D4, metadata D3/D4 nem UI final.
- `ForegroundBleOperationGate` ainda não possui owner `WATCH_WORKOUT_IMPORT`.
- `ChampionNavigation.kt` só bloqueia saída da tela de frequência cardíaca durante medição manual ou D1.
- `HomeViewModel.kt` e `DeviceManagementViewModel.kt` só conhecem bloqueios de `DAILY_STEPS_REFRESH`, `MANUAL_HEART_RATE` e `WATCH_D1_HISTORY`.
- `device_import_metadata` já existe e favorece reuse para `WATCH_WORKOUT_D3_D4`, sem migration obrigatória aparente.

## Achados de `010-01`

- `AGENTS.md` foi alinhado para `docs/baseline/current-baseline.md`.
- Baseline MotionLog confirma D3 com CRCs `u16 LE` a partir do offset `3`, mas também usa `bytes[3..4]` para testar header multipart plausível; esse acoplamento deve ser tratado como fato herdado, não como header limpo já confirmado.
- Baseline MotionLog confirma D4 com `packetIndex = u16 LE bytes[1..2]`, `totalPackets = u16 LE bytes[3..4]`, `minuteIndex = packetIndex - 4` e `sampledAt = startedAt + minuteIndex * 60 segundos`.
- Baseline MotionLog confirma bucket fixo de 60 segundos, inclusive no último intervalo; sobra de duração total permanece diagnóstica.
- Baseline MotionLog confirma `recordCount` apenas como verificação de completude; divergência gera `partial`, sem buckets sintéticos.
- Baseline MotionLog não deduplica CRC repetido dentro da mesma leitura D3 antes do loop D4; isso virou risco funcional explícito para `010-02` e `010-03`.
- App nativo já possui fronteira correta entre importação e revisão: `createFromImport(...)` nasce em `PENDING_REVIEW` e `confirmReview(...)` bloqueia avanço quando `parserValidity != VALID`.
- App nativo já possui deduplicação/reimportação transacional por `(deviceId, origin, crc)` em `persistImported(...)`, com preservação de revisão, workflow e calibração existentes.

## Achados de `010-02`

- `WatchWorkoutImportSync.kt` passou a concentrar contratos de tentativa, progresso, resumo, resultado tipado, metadata, erro seguro e política explícita de persistência para `VALID/PARTIAL/INVALID`.
- `ExercisesFeatureContract.kt` deixou de ser vazio e agora já separa estado de superfície da importação D3/D4 do restante da tela Exercícios.
- `RepositoryContracts.kt` já reserva owner `WATCH_WORKOUT_IMPORT` e contratos de metadata, liberando trabalho paralelo de `010-03`, `010-04` e `010-06`.
- `DiagnosticPayload.kt` já formaliza política diagnóstica privada da fase sem introduzir logging de payload nem retenção nova.

## Estado herdado da Fase 09

- D1 local-first está funcional e Health Connect real segue fora do escopo até a Fase 12.
- Sessão BLE saudável continua no escopo do app, não da tela Dispositivo.
- Gate foreground já é compartilhado entre Home, Saúde e Dispositivo.
- D1 já consolidou padrão de coordinator foreground com `attemptId`, `bleSessionId`, observer temporário, timeout, cancelamento e `finally`.

## Fronteiras das Fases 11 e 12

- Fase 10 termina em `PENDING_REVIEW`.
- Fase 11 assume revisão, edição de tipo, título, observações, calibração e avanço para `READY_TO_SYNC`.
- Fase 12 assume Health Connect real, backlog local e estados `SYNCING/SYNCED`.

## Limites

- não implementar revisão, calibração ou Health Connect;
- não executar D3/D4 automaticamente;
- não criar task exclusiva de testes;
- não depender de foreground service ou WorkManager;
- não tratar evidência fake como substituta de validação física.

## Lista completa de tasks

1. `010-01-audit-d3-d4-baseline-domain-and-workflow-boundaries.md`
2. `010-02-define-watch-workout-import-contracts-state-and-diagnostic-policy.md`
3. `010-03-implement-d3-index-protocol-and-collector.md`
4. `010-04-implement-d4-block-collector-and-remounting.md`
5. `010-05-implement-d4-workout-parser-and-domain-mapping.md`
6. `010-06-implement-workout-import-persistence-deduplication-and-metadata.md`
7. `010-07-implement-watch-workout-import-coordinator-and-foreground-gate.md`
8. `010-08-create-exercises-import-viewmodel.md`
9. `010-09-create-exercises-import-and-pending-ui.md`
10. `010-10-integrate-progress-cancel-retry-lifecycle-and-safe-errors.md`
11. `010-11-run-initial-d3-d4-physical-validation.md`
12. `010-12-resolve-physical-findings-and-run-gate-b.md`
13. `010-13-close-watch-workout-import-phase.md`

## Ordem

1. `010-01`
2. `010-02`
3. `010-03`
4. `010-04`
5. `010-05`
6. `010-06`
7. `010-07`
8. `010-08`
9. `010-09`
10. `010-10`
11. `010-11`
12. `010-12`
13. `010-13`

## Dependências

| Task | Depende de |
| --- | --- |
| `010-01` | leitura inicial completa |
| `010-02` | `010-01` |
| `010-03` | `010-02` |
| `010-04` | `010-02` |
| `010-05` | `010-03`, `010-04` |
| `010-06` | `010-02` |
| `010-07` | `010-03`, `010-04`, `010-05`, `010-06` |
| `010-08` | `010-07` |
| `010-09` | `010-08` |
| `010-10` | `010-07`, `010-08`, `010-09` |
| `010-11` | `010-10` |
| `010-12` | `010-11` |
| `010-13` | `010-12` |

## Paralelismo

- `010-03`, `010-04` e `010-06` podem avançar após `010-02` se coordenarem conflitos em `RepositoryContracts.kt` e `WatchWorkoutImportSync.kt`.
- `010-05` depende de `010-03` e `010-04` porque usa contratos D4 já estabilizados.
- `010-07` só inicia quando D3, D4, parser e persistência estiverem estáveis.
- `010-08`, `010-09` e `010-10` devem permanecer sequenciais por alto conflito em `feature/exercises/**` e `ChampionNavigation.kt`.
- `010-11`, `010-12` e `010-13` são sequenciais.

## Arquivos com risco de conflito

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DiagnosticPayload.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/InMemoryForegroundBleOperationGate.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionPacketRouter.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`

## Checklist

- [x] `010-01` auditar MotionLog, baseline factual, domínio, Room, offsets e corrigir caminho futuro em `AGENTS.md`
- [x] `010-02` definir contratos, state machine, metadata e política diagnóstica D3/D4
- [ ] `010-03` implementar protocolo e collector D3
- [ ] `010-04` implementar collector D4 e remontagem
- [ ] `010-05` implementar parser D4, timestamps, intervalos e agregados
- [ ] `010-06` implementar persistência, deduplicação, metadata e payload privado
- [ ] `010-07` implementar coordinator D3/D4 e owner `WATCH_WORKOUT_IMPORT`
- [ ] `010-08` criar `ExercisesViewModel`
- [ ] `010-09` substituir placeholder de Exercícios por UI de importação e pendências
- [ ] `010-10` integrar progresso, cancelamento, retry, lifecycle, navegação e mensagens seguras
- [ ] `010-11` executar primeira validação física D3/D4
- [ ] `010-12` resolver achados físicos e validar Gate B
- [ ] `010-13` fechar spec, overview, baseline factual, checklist e dívida

## Comandos

Gate obrigatório:

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Comandos pequenos opcionais por task:

```bash
./gradlew app:testDebugUnitTest --tests "*Workout*"
./gradlew app:testDebugUnitTest --tests "*D3*"
./gradlew app:testDebugUnitTest --tests "*D4*"
```

Falhas antigas e não relacionadas devem ser documentadas. Regressão introduzida pela Fase 10 deve ser corrigida.

## Roteiro físico

Galaxy S22 com Champion C001:

1. abrir Exercícios;
2. confirmar ausência de importação automática;
3. tocar `Buscar treinos no relógio`;
4. observar D3;
5. observar D4 sequencial;
6. importar treino curto;
7. confirmar atividade pendente;
8. repetir e confirmar deduplicação;
9. cancelar durante D4;
10. tentar novamente e confirmar retomada;
11. validar ausência de Health Connect;
12. validar ausência de payload/CRC no Logcat.

## Gate B

- treino curto real;
- treino longo ou multipart real;
- D3 com índice coerente;
- D4 sequencial e remontagem correta;
- persistência local;
- intervalos e BPM coerentes;
- deduplicação;
- cancelamento;
- retomada por CRC;
- `PENDING_REVIEW` sem Health Connect.

## Dívida técnica

- revisão e calibração da atividade ficam para Fase 11;
- Health Connect real fica para Fase 12;
- foreground service para importações longas fica para Fase 14;
- confirmação física final dos bytes desconhecidos permanece em aberto;
- ADR de política `PARTIAL`, payload release ou retomada por CRC só se a implementação exigir decisão duradoura nova.

## Critérios gerais

- importação apenas por ação explícita;
- `PENDING_REVIEW` como fronteira da fase;
- sem revisão, calibração ou Health Connect;
- sem migration de Room por padrão;
- build gate obrigatório registrado;
- validação física obrigatória, não substituída por fakes.

## Instruções de atualização

- cada task concluída deve atualizar este overview no mesmo trabalho;
- dependência nova ou risco novo deve ser registrado aqui imediatamente;
- bloqueio físico deve manter task pendente e documentar motivo;
- `010-13` consolida checklist, comandos, Gate B, baseline factual e dívida final.

Risco novo aberto por `010-01`: deduplicação intra-tentativa de CRC repetido não existe no MotionLog auditado; implementação D3 do app nativo deve resolver isso sem perder ordem determinística dos blocos.
