# 013 — Exercícios iniciados pelo Champion Companion com GPS e rota

- **Status geral:** pending
- **Spec:** [013-app-workouts-gps-route.md](../../specs/013-app-workouts-gps-route.md)
- **Progresso:** 7/17 subtarefas concluídas

## Objetivo

Entregar caminhada, corrida, treino livre e esteira iniciados pelo celular, com preparação, countdown, sessão persistida, foreground service, GPS/rota controlados, relógio opcional, revisão, correlação D3/D4 e `ExerciseRoute` idempotente.

## Checklist

- [x] [013-01-audit-post-phase-15-app-workout-location-and-correlation-boundaries.md](013-01-audit-post-phase-15-app-workout-location-and-correlation-boundaries.md)
- [x] [013-02-update-roadmap-privacy-adr-and-operational-rules.md](013-02-update-roadmap-privacy-adr-and-operational-rules.md)
- [x] [013-03-define-generalized-workout-session-route-gps-and-correlation-contracts.md](013-03-define-generalized-workout-session-route-gps-and-correlation-contracts.md)
- [x] [013-04-add-location-permissions-dependencies-preferences-and-navigation.md](013-04-add-location-permissions-dependencies-preferences-and-navigation.md)
- [x] [013-05-migrate-room-for-phone-activities-routes-and-correlation.md](013-05-migrate-room-for-phone-activities-routes-and-correlation.md)
- [x] [013-06-implement-gps-readiness-gnss-quality-and-point-filtering.md](013-06-implement-gps-readiness-gnss-quality-and-point-filtering.md)
- [x] [013-07-implement-workout-preparation-countdown-and-permission-flow.md](013-07-implement-workout-preparation-countdown-and-permission-flow.md)
- [ ] [013-08-implement-active-workout-foreground-service-and-recovery.md](013-08-implement-active-workout-foreground-service-and-recovery.md)
- [ ] [013-09-implement-walking-running-route-capture-and-live-metrics.md](013-09-implement-walking-running-route-capture-and-live-metrics.md)
- [ ] [013-10-implement-free-workout-and-treadmill-phone-sessions.md](013-10-implement-free-workout-and-treadmill-phone-sessions.md)
- [ ] [013-11-integrate-watch-live-heart-rate-and-steps-with-phone-session.md](013-11-integrate-watch-live-heart-rate-and-steps-with-phone-session.md)
- [ ] [013-12-finalize-phone-session-into-reviewable-activity.md](013-12-finalize-phone-session-into-reviewable-activity.md)
- [ ] [013-13-correlate-watch-d3-d4-imports-and-suppress-duplicates.md](013-13-correlate-watch-d3-d4-imports-and-suppress-duplicates.md)
- [ ] [013-14-sync-phone-session-route-and-metrics-with-health-connect.md](013-14-sync-phone-session-route-and-metrics-with-health-connect.md)
- [ ] [013-15-integrate-exercises-calendar-details-states-and-accessibility.md](013-15-integrate-exercises-calendar-details-states-and-accessibility.md)
- [ ] [013-16-run-manual-physical-gps-watch-health-connect-validation.md](013-16-run-manual-physical-gps-watch-health-connect-validation.md)
- [ ] [013-17-close-app-workouts-gps-route-phase.md](013-17-close-app-workouts-gps-route-phase.md)

## Observações

- Dependência principal segue grafo da spec. Após `013-08`, `013-09`, `013-10` e `013-11` podem avançar em paralelo sem editar simultaneamente contratos/repositories. Após `013-12`, `013-13` e `013-14` podem avançar em paralelo.
- Hotspots: `AppWorkoutSession.kt`, `WatchWorkoutActivity.kt`, `HeartRateAndSync.kt`, `RepositoryContracts.kt`, `ChampionDatabase.kt`, `ChampionDaos.kt`, `DatabaseMappers.kt`, `RoomRepositories.kt`, `HealthRecordMapper.kt`, `HealthConnectSyncCoordinator.kt`, `ChampionNavigation.kt`, `ExercisesViewModel.kt` e `ExercisesScreen.kt`.
- Migration prevista: manual `v5 → v6`, sem destructive migration, com schema `6.json`.
- ADR prevista: ADR-014, sucedendo parcialmente política de rota da ADR-004 e alinhando ADR-012.
- `Phase003PrivacyGuardTest` é incompatível com nova decisão. Dívida conhecida; não alterar nem executar testes nesta fase.
- F13 não usa `WorkManager` para sessão ativa; F14 mantém WorkManager para
  tarefas adiáveis. Sem mapa externo, exportação ou servidor.
- Cada subtarefa deve atualizar este overview no trabalho de execução, marcando somente seu item após conclusão real.
- `013-01` concluída com auditoria documental reproduzível e revisão independente aprovada. Evidências confirmam Room v5, migration planejada v5 → v6, mapper de sessão incompleto, acoplamento D3/D4, ausência de rota/FGS e protocolo live não confirmado.
- `013-02` concluída com ADR-014 aceita, ADR-004 parcialmente sucedida,
  roadmap/baselines/AGENTS alinhados e fronteira F12/F13/F14 formalizada. A
  implementação funcional permanece pendente; `Phase003PrivacyGuardTest` não
  foi alterado nem executado. Revisão independente aprovada com ressalvas, sem
  bloqueadores.
- `013-03` concluída com contratos Android-free de atividade, sessão, rota, GPS,
  correlação e projeção Health Connect. Mappers/Room/transações persistentes
  permanecem para subtarefas seguintes; revisão independente final aprovada sem
  bloqueadores.
- `013-04` concluída com permissões mínimas de localização/FGS/notificação/rota,
  countdown persistido e normalizado, destinos serializáveis com saver e shells
  passivos. SDK Health Connect `1.1.0` foi mantido; service operacional e
  solicitação runtime permanecem nas subtarefas seguintes.
- Gate físico de `013-05` foi concluído sem divergência: uma calibração, duas
  sessões `DISCARDED`, snapshots conjuntos `.db`/`-wal`/`-shm`, hashes,
  normalização temporal, foreign keys, Exercícios, detalhe, calibração,
  Calendário e reabertura foram validados. A revisão independente encontrou
  bloqueador nas invariantes de escrita de rota, posteriormente corrigido e
  aprovado em revisão independente. Conforme limitação explícita da task,
  ausência de rota real não bloqueia migration enquanto captura funcional não
  existir. Item marcado e progresso atualizado para `5/17`.
- Tentativa inicial de continuação em 2026-07-30 foi bloqueada por ausência
  temporária de dispositivo ADB; execução posterior foi concluída sem
  divergências. Checkpoint remoto não estava disponível porque `sqlite3` não
  existe no sandbox `run-as`; snapshots usaram `.db`/`-wal`/`-shm` juntos.
- `WorkoutRouteRepository.loadRecoverySnapshot` com rota real permanece
  follow-up separado, pois captura funcional de rota ainda não produz pontos
  reais. Detalhes sanitizados em `docs/integration/013-05-room-v6-follow-up.md`.
- Bloqueador identificado em `RoomRepositories`: impedir append aceito com
  primeiro ponto anterior à rota ou segmento novo anterior ao término do
  segmento anterior; correção aplicada e aprovada em revisão independente.
- Correção estática aplicada em `RoomRepositories`: criação e append agora
  alinham início de sessão/rota e fronteiras temporais de pontos/segmentos.
  A validação física de rota real foi mantida como follow-up separado.
- Correção residual valida ordem estrita na representação epoch-millis antes do
  CAS/insert, impedindo colisão de pontos ordenados apenas por nanos. Cursor e
  inícios de segmento também são validados na precisão persistida. Diff check e
  build completo passaram; revisão lossless foi aprovada com ressalvas.
- Rota real, process death com pontos e `loadRecoverySnapshot` ficam como
  follow-up das tasks `013-08`/`013-09`; não bloqueiam conclusão de `013-05`.
- Revisão independente final aprovou a correção lossless com ressalvas. Nenhum
  bloqueador estático restante; rota real, process death com pontos e
  `loadRecoverySnapshot` físico continuam follow-up separado. Conforme limitação
  explícita da task, `013-05` foi concluída e o progresso atualizado para
  `5/17`.
- `013-06` concluída com fonte Android nativa isolada por runtime, cleanup
  idempotente, descarte de callbacks antigos, readiness primária por fix,
  complemento GNSS opcional, filtro e acumulador puros e logging categórico
  sanitizado. Build completo passou; validação manual das variações reais de GPS
  no Galaxy S22 permanece para os gates físicos das subtarefas de integração.
- Revisão independente de `013-06` encontrou e aprovou correções para idade de
  fix monotônica e rejeição de saltos após lacunas. `lint`, builds debug/release
  e `installDebug` passaram; nenhum bloqueio permanece nesta subtarefa.
- `013-07` concluída com CTA, seleção dos quatro tipos, preparação contextual,
  warming GPS descartável, permissão precisa somente para caminhada/corrida,
  countdown persistido `0/3/5/10`, cancelamento, invalidação por perda de
  readiness, cleanup de lifecycle e handoff idempotente em zero para `013-08`.
  `lint`, builds debug/release e `installDebug` passaram; validação física
  parcial confirmou seleção acessível e treino livre sem prompt GPS. Roteiro
  completo de GPS, countdown, permissões, rotação, fonte aumentada e landscape
   permanece pendente para gate manual.
- `013-08` reaberta após validação independente de 2026-07-31. Permanecem
  pendentes validação física e evidências visuais. Correções estáticas cobrem
  criação/start transacional, compensação para recuperação, resultado das ações,
  confirmações seguras, duração monotônica, permissão runtime de notificações,
  preservação explícita de `CREATED`, remoção da captura GPS prematura,
  compensação CAS limitada e bloqueio global/do canal de notificações.
  `lint`, builds debug/release e `installDebug` passaram sem execução de testes.
  Item segue desmarcado e progresso permanece `7/17` até revisão e gate manual.
- Correção parcial de `013-14` removeu offsets UTC fixos dos records derivados
  de `WorkoutActivity` e passou a congelar o timezone na fila v6 existente,
  sem migration ou reenvio automático. O item permanece desmarcado e bloqueado
  por `013-12`, pela implementação de `ExerciseRoute`/workflow e pela validação
  física no provider.
- A validação desta execução passou em `git diff --check` e em
  `./gradlew lint assembleDebug assembleRelease`. Nenhum teste foi criado,
  alterado ou executado, conforme restrição explícita.
- Correção residual de `013-08` formalizou leitura pontual da superfície aberta
  de recuperação e passou a tratar todos os resultados de `interrupt` com duas
  tentativas CAS/reload no máximo. Falha persistente preserva owner quando o FGS
  já está válido; cleanup só ocorre após estado recuperável, superfície aberta
  ou ausência persistida confirmados. `onTaskRemoved` usa a mesma reconciliação.
- `013-08` permanece `in_progress`, item `[ ]` e progresso `7/17`. Gate manual,
  screenshots, dumps sanitizados e nova revisão independente seguem bloqueando
  conclusão.
- Correção concorrente de `013-08` adicionou coordenador process-scoped para
  serializar attach/promoção/reconciliação e revalidar owner da mesma sessão
  antes do CAS. `isAlive` foi removido da decisão. Finalização agora aguarda
  confirmação persistida de `FINISHING`/`FINISHED` e reabilita ações após
  timeout seguro. Item permanece `[ ]`, progresso `7/17`; gate manual e
  evidências continuam bloqueando conclusão.
- Correção residual de `013-08` substituiu o `Mutex` por exclusão process-scoped
  com `Semaphore` justo e não vinculado à thread. `onDestroy` agora marca
  `TEARING_DOWN` dentro da mesma seção crítica de attach, promoção, consulta de
  owner e reconciliação; `release` comum não remove essa fase, bloqueando novo
  attach até o fechamento process-scoped. Retry/CAS limitado e
  indisponibilidade explícita foram preservados.
- `git diff --check`, `./gradlew lint assembleDebug assembleRelease` e
  `./gradlew installDebug` passaram sem execução de testes. `013-08` segue
  `in_progress`, item `[ ]` e progresso `7/17`; gate manual e evidências
  sanitizadas continuam bloqueando conclusão.
- Correção final de exclusão de `013-08` tornou aquisição do `Semaphore` segura
  contra cancelamento e removeu espera bloqueante de `onDestroy`. Teardown usa
  `tryAcquire`, CAS para `TEARING_DOWN`, tombstone contra corrida com `release`
  e reconciliação process-scoped, bloqueando novo attach sem prender lifecycle.
- Abertura e falha transitória com sessão observada recebem no máximo duas
  tentativas com intervalo de `200 ms`. `Unavailable` permanece distinto de
  ausência; sem sessão conhecida não há ID/destino inventado. Reconciliação
  duplicada sem consumidor no ViewModel foi removida.
- `013-08` continua `in_progress`, item `[ ]` e progresso `7/17`. Gate manual de
  cancelamento/teardown/Room, screenshots e dumps sanitizados ainda bloqueiam
  conclusão. `git diff --check`, build completo e `installDebug` passaram;
  nenhum teste automatizado foi criado, alterado ou executado.
- Bloqueador final de `013-08` corrigido: `ATTACHING` agora é reservado antes da
  primeira leitura suspensa, e `onStartCommand` registra o ID pendente da sessão
  para teardown anterior à execução da coroutine. Coordenador instala tombstone sem
  sobrescrever owner concorrente, preserva owner foreground válido da mesma
  sessão e mantém reconciliação process-scoped com retry/CAS limitado.
- `Unavailable` em abertura agora produz aviso explícito para tentar novamente,
  sem ID fictício. `git diff --check`, build completo e `installDebug` passaram
  sem testes automatizados. `013-08` permanece `in_progress`, item `[ ]` e
  progresso `7/17`; gate manual, screenshots e dumps sanitizados ainda bloqueiam
  conclusão.
