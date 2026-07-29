# 013 — Exercícios iniciados pelo Champion Companion com GPS e rota

- **Status geral:** pending
- **Spec:** [013-app-workouts-gps-route.md](../../specs/013-app-workouts-gps-route.md)
- **Progresso:** 0/17 subtarefas concluídas

## Objetivo

Entregar caminhada, corrida, treino livre e esteira iniciados pelo celular, com preparação, countdown, sessão persistida, foreground service, GPS/rota controlados, relógio opcional, revisão, correlação D3/D4 e `ExerciseRoute` idempotente.

## Checklist

- [ ] [013-01-audit-post-phase-15-app-workout-location-and-correlation-boundaries.md](013-01-audit-post-phase-15-app-workout-location-and-correlation-boundaries.md)
- [ ] [013-02-update-roadmap-privacy-adr-and-operational-rules.md](013-02-update-roadmap-privacy-adr-and-operational-rules.md)
- [ ] [013-03-define-generalized-workout-session-route-gps-and-correlation-contracts.md](013-03-define-generalized-workout-session-route-gps-and-correlation-contracts.md)
- [ ] [013-04-add-location-permissions-dependencies-preferences-and-navigation.md](013-04-add-location-permissions-dependencies-preferences-and-navigation.md)
- [ ] [013-05-migrate-room-for-phone-activities-routes-and-correlation.md](013-05-migrate-room-for-phone-activities-routes-and-correlation.md)
- [ ] [013-06-implement-gps-readiness-gnss-quality-and-point-filtering.md](013-06-implement-gps-readiness-gnss-quality-and-point-filtering.md)
- [ ] [013-07-implement-workout-preparation-countdown-and-permission-flow.md](013-07-implement-workout-preparation-countdown-and-permission-flow.md)
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
- Nenhum `WorkManager`, mapa externo, exportação ou servidor.
- Cada subtarefa deve atualizar este overview no trabalho de execução, marcando somente seu item após conclusão real.
