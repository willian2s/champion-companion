# 013 — Exercícios iniciados pelo Champion Companion com GPS e rota

- **Status geral:** closed
- **Outcome:** `partially_delivered`
- **Motivo do encerramento:** `product_reorientation`
- **Spec:** [013-app-workouts-gps-route.md](../../specs/013-app-workouts-gps-route.md)
- **Progresso:** 9 `completed` / 8 `cancelled` / 0 `in_progress` (17 subtarefas)

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
- [x] [013-09-implement-walking-running-route-capture-and-live-metrics.md](013-09-implement-walking-running-route-capture-and-live-metrics.md)
- [ ] [013-10-implement-free-workout-and-treadmill-phone-sessions.md](013-10-implement-free-workout-and-treadmill-phone-sessions.md)
- [ ] [013-11-integrate-watch-live-heart-rate-and-steps-with-phone-session.md](013-11-integrate-watch-live-heart-rate-and-steps-with-phone-session.md)
- [ ] [013-12-finalize-phone-session-into-reviewable-activity.md](013-12-finalize-phone-session-into-reviewable-activity.md)
- [ ] [013-13-correlate-watch-d3-d4-imports-and-suppress-duplicates.md](013-13-correlate-watch-d3-d4-imports-and-suppress-duplicates.md)
- [ ] [013-14-sync-phone-session-route-and-metrics-with-health-connect.md](013-14-sync-phone-session-route-and-metrics-with-health-connect.md)
- [ ] [013-15-integrate-exercises-calendar-details-states-and-accessibility.md](013-15-integrate-exercises-calendar-details-states-and-accessibility.md)
- [ ] [013-16-run-manual-physical-gps-watch-health-connect-validation.md](013-16-run-manual-physical-gps-watch-health-connect-validation.md)
- [x] [013-17-close-app-workouts-gps-route-phase.md](013-17-close-app-workouts-gps-route-phase.md)

## Matriz de encerramento

| Subtarefa | Status | Resultado entregue | Motivo | Destino/reaproveitamento |
|---|---|---|---|---|
| 013-01 | `completed` | Auditoria factual pós-Fase 15 e limites de sessão, localização, BLE e Health Connect | Entrega documental preservada | Fonte histórica para decisões futuras; sem novo escopo phone |
| 013-02 | `completed` | Política de privacidade, ADR-014, roadmap e fronteira F13/F14 | Decisão documental histórica preservada | ADR-017 reorienta novos treinos; guard e privacidade permanecem |
| 013-03 | `completed` | Contratos de atividade, sessão, rota, GPS, correlação e projeção Health Connect | Contratos entregues antes da reorientação | Preservar como infraestrutura; não expandir rota phone |
| 013-04 | `completed` | Permissões, countdown, destinos Navigation3 e shells passivos | Preparação/configuração entregue | Legado preservado; entradas phone protegidas por `013-17` |
| 013-05 | `completed` | Migration v5→v6, schema e invariantes Room com preservação histórica | Gate documental/físico aceito conforme registro | Preservar migrations, schemas, dados e evidências |
| 013-06 | `completed` | Readiness GPS, GNSS complementar, filtro e acumulador determinísticos | Infraestrutura entregue; gates posteriores não reabertos | Preservar sem nova expansão GPS/rota |
| 013-07 | `completed` | Seleção, preparação, countdown, permissões contextuais e handoff | Fluxo histórico preservado | Entrada phone protegida por `013-17`; não é direção ativa |
| 013-08 | `cancelled` | Base FGS, sessão, ownership, recuperação e evidências sanitizadas parcialmente entregues | Reorientação; bloqueios físicos não convertidos em sucesso | Preservar infraestrutura e evidências; não corrigir phone |
| 013-09 | `completed` | Captura GPS, rota, segmentação e métricas com validação manual e waiver explícito | Entrega preservada, sem ampliar direção de produto | Preservar dados/infraestrutura; não criar novos gates phone |
| 013-10 | `cancelled` | Superfície sem GPS e orientação de esteira parcialmente entregues; handoff ficou bloqueado | Reorientação antes do aceite integral | Preservar política sem GPS; futura redefinição para relógio |
| 013-11 | `cancelled` | Ownership BLE e participação explícita parcialmente entregues; gates físicos permaneceram falhos ou bloqueados | Reorientação; ausência de métricas live não é sucesso | Preservar evidências; priorizar importação de treinos do relógio |
| 013-12 | `cancelled` | Não executada; sem finalização phone ou atividade revisável nova | Reorientação de produto | Redefinição conceitual futura na Fase 014 |
| 013-13 | `cancelled` | Não executada; sem correlação phone/watch nova | Reorientação de produto | Redefinição conceitual futura na Fase 014 |
| 013-14 | `cancelled` | Não executada; sem `ExerciseRoute` ou sync de rota phone | Reorientação de produto | Health Connect granular de treinos do relógio na Fase 014, se redefinido |
| 013-15 | `cancelled` | Não executada; sem integração de superfícies phone específica | Reorientação de produto | Redefinição de revisão/calendário do relógio na Fase 014 |
| 013-16 | `cancelled` | Não executada; sem gate físico integral phone/GPS/Health Connect | Reorientação de produto | Encerrar novos gates físicos phone; futura validação explícita do relógio |
| 013-17 | `completed` | Guard central, bloqueios de entrada/runtime phone e recovery histórico seguro implementados e revisados | Reorientação de produto encerrada com preservação histórica | Preservar sessões/dados/legado; Fase 013 fechada como `partially_delivered` |

## Observações

- A matriz acima é o estado atual autoritativo: `9 completed`, `8 cancelled` e
  `0 in_progress`. Registros abaixo preservam histórico de execuções anteriores
  e podem mencionar progressos/status que eram verdadeiros naquele momento.
- A Fase 013 está `closed`, com resultado `partially_delivered` por
  `product_reorientation`. `013-17` foi concluída após validação e revisão;
  itens cancelados permanecem desmarcados.
- O Bloco B foi implementado nesta execução: entradas, criação/retomada,
  foreground service, GPS, rota e BLE phone estão bloqueados por política
  central. O aceite final foi confirmado por revisão independente. O Bloco C
  preserva migrations, schemas, sessões, dados e evidências. O Bloco D registrará
  `partially_delivered`; esse encerramento foi concluído.
- Gate D foi encerrado como `PASS/N/A`, pois não havia sessão phone histórica
  elegível e o cenário não pode ser fabricado com segurança. Gate E passou com
  importação real do relógio (`inseridos 1`, `parciais 1`) e atividade pendente
  de revisão.
- A direção futura prioriza importação, derivação, revisão e sincronização com
  Health Connect de treinos iniciados pelo relógio. A Fase 014 será redefinida
  para esse refinamento; WorkManager permanece somente detalhe técnico de
  tarefas finitas e adiáveis.

- Dependência principal segue grafo da spec. A base operacional de `013-08` já
  permite iniciar `013-09`, `013-10` e `013-11` em paralelo, sem editar
  simultaneamente contratos/repositories. O aceite integrado de `013-08` aguarda
  handoffs GPS/BLE e evidências dessas frentes. Após `013-12`, `013-13` e `013-14`
  podem avançar em paralelo.
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

- `013-09` recebeu integração estática de rota GPS e métricas live no serviço,
  com source por geração, captura somente `ACTIVE` de caminhada/corrida, lotes
  confirmados após sucesso, segmentação de pausa/gap/loss/recovery, retry seguro
  de falha de persistência e UI sem coordenadas. `lint`, builds debug/release,
  `git diff --check` e `installDebug` passaram; nenhum teste foi criado,
  alterado ou executado.
- `013-09` permanece `in_progress`, item `[ ]` e progresso `7/17`: gate manual
  de caminhada/corrida, pausa/retomada, perda/recuperação GPS, gaps, process
  death com lotes, falha de persistência e screenshots obrigatórios não foram
  concluídos. `013-08` continua `in_progress`; `013-12` ainda finaliza rota e
  atividade.
- Revisão independente de `013-09` aprovou implementação estática após limite
  de um ponto por confirmação, reconciliação de append ambíguo e separação de
  métricas confirmadas/pending. `git diff --check`, lint, builds debug/release e
  `installDebug` passaram. Gate físico e screenshots seguem bloqueando conclusão;
  item permanece `[ ]`, status `in_progress` e progresso `7/17`.
- Validação manual parcial de `013-09` usou atividade `WALKING` existente no
  Galaxy S22: snapshot sanitizado preservou 41 pontos, 2 segmentos, 155,9 m e
  sequência `0..40` após `am crash` e reabertura. UI não exibiu coordenadas e
  nenhum FGS permaneceu ativo. A atividade estava `FINISHING`; tela ativa,
  pausa/retomada, perda/recuperação GPS, falha de escrita e screenshots ainda
  não foram validados. Item permanece `[ ]`, `in_progress`, `7/17`.
- Gate manual adicional de `013-09` validou sessão GPS nova: pausa em 26 m,
  retomada com segmentos `1..5`, perda por localização desativada sem aumento de
  distância, recuperação para `GPS pronto`, background/tela apagada com FGS
  presente e `am crash` seguido de `Retomar treino`. Sessão livre não mostrou
  card GPS. Screenshots sanitizados foram capturados temporariamente. Falha real
  de persistência não foi reproduzida; item permanece `[ ]`, `in_progress`,
  `7/17`.
- Tentativa controlada de falha em `013-09` tornou SQLite somente leitura, mas
  conexões Room já abertas continuaram escrevendo; após force-stop, app não
  apresentou UI com arquivos somente leitura. Permissões foram restauradas para
  `600`, recuperação confirmou funcionamento e sessão de teste foi descartada.
  Não há evidência válida de degradação em write failure; item permanece `[ ]`,
  `in_progress`, `7/17`.
- `013-09` está bloqueada no aceite: não há mecanismo seguro e reproduzível para
  falhar `appendAcceptedPoints` no dispositivo não-root. Permissões não bastam,
  armazenamento cheio e alteração de banco são inadequados, e fault injection
  exige decisão explícita para build debug. Item permanece `[ ]`, progresso
  `7/17`; desbloqueio depende de ambiente controlado, seam debug aprovada ou
  waiver formal.
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
- Gate manual parcial executou countdowns `0/5/10`, FGS `health`, pausa/continuação,
  background, rotação, fonte aumentada, force-stop, recuperação, confirmações,
  descarte, swipe da task e permissão de notificações. Item permanece `[ ]` e
  progresso `7/17`.
- Registro intermediário anterior marcou reboot e ações de notificação como
  pendentes enquanto ADB estava `offline`; retificação posterior confirmou
  reboot sem restart automático, ações diretas `Pausar`/`Finalizar` e gates de
  notificações. CAS, banco indisponível/lento, relógio civil e countdown `3`
  intermediário seguem pendentes em `013-08`.
- Correções visuais pós-gate concluídas: `Finalizar` tornou-se icon-only com
  descrição acessível, e a Activity aberta pela notificação passou a usar tema
  dedicado sem action bar, header do nome do app ou fundo teal da splash.
  Build/install e validação manual passaram; `013-08` permanece `[ ]`, `7/17`,
  aguardando CAS/banco/relógio e revisão independente.
- Fundo da janela de sessão agora é determinístico via `Surface` Compose de tela
  inteira, inclusive confirmação; captura visual confirmou ausência de teal
  residual. `013-08` continua `[ ]`, `7/17`.
- Pendências restantes de `013-08` foram consolidadas no [follow-up de validação
  adversarial](../../integration/013-08-foreground-service-recovery-follow-up.md):
  CAS, Room indisponível/lento, relógio civil, countdown `3`, safe insets e
  evidências duráveis.
- Reboot foi repetido após reconexão: nenhum FGS/notificação iniciou antes da
  abertura do app, e recuperação apareceu somente depois. Bloqueios global e
  isolado do canal foram validados com retry/retomada após reativação. O usuário
  confirmou manualmente `Pausar` e `Finalizar` diretamente na notificação;
  permanecem somente CAS, banco indisponível/lento, relógio civil e countdown
  `3` intermediário.
- Gate adversarial complementar de `013-08` confirmou, com dump imediato, ausência
  de serviço durante countdown `3` e FGS somente após a transição final. Force-stop
  seguido de abertura exibiu recuperação e retomada promoveu FGS novamente.
  Capturas e dump sanitizado foram preservados em
  `docs/integration/013-08-evidence/`. Alteração de relógio civil foi bloqueada
  por permissão do dispositivo e restaurada; CAS persistente e Room lento/
  indisponível continuam sem reprodução segura. Item permanece `[ ]`, progresso
  `7/17`.
- Revisão independente do gate complementar bloqueou conclusão por CAS, Room,
  relógio civil, safe insets completos, ação `Continuar` inconclusiva nesta
  repetição e proveniência landscape. Serial ADB foi removido da documentação;
  captura de fonte aumentada foi substituída após `installDebug`. Item permanece
  `[ ]`, progresso `7/17`.
- Permanecem também sem encerramento explícito tela apagada com múltiplos
  checkpoints, morte real de processo e tipos FGS dinâmicos `location`/
  `connectedDevice`; evidência atual cobre somente `health`. Imagens e dump
  sanitizado estão no working tree e ainda precisam integrar o conjunto do PR.
- Análise formal de CAS/Room rejeitou hook, fake, atraso artificial e manipulação
  de banco; código mantém fail-closed e retry limitado. `./gradlew lint
  assembleDebug assembleRelease` passou novamente; item permanece `[ ]`,
  progresso `7/17`.
- Primeira pendência adversarial de `013-08` foi analisada sem avançar para as
  demais. Não há forma segura de injetar CAS persistente ou Room lento/
  indisponível pelo produto atual sem alterar produção, criar teste/fake ou
  manipular banco privado. Inspeção confirmou owner único, duas tentativas
  máximas, distinção entre ausência e indisponibilidade e preservação de owner
  válido ou superfície conhecida. Room permanentemente suspenso ainda pode
  reter permit/tombstone até retorno ou morte do processo; timeout exige contrato
  e cenário reproduzível. Item segue `[ ]`, progresso `7/17`.
- Nova tentativa física do relógio civil foi encerrada sem alteração temporal:
  pela interface do Galaxy S22, `Data e hora automáticas` foi restaurada; ao tocar
  em `Definir data` após desativá-la, o sistema reativou a sincronização antes do
  seletor. Sem comando `date`, alteração direta de `settings`, hook ou manipulação
  de banco. A evidência de duração monotônica, ordenação de auditoria e round-trip
  Room sob mudança civil permanece bloqueada; `013-08` continua `[ ]`, `7/17`.
- Revisão independente do bloqueio do relógio civil aprovou estrutura SDD e
  registro factual, mas manteve `013-08` bloqueada. Capturas anteriores no
  diretório de evidências exibiam horários civis de notificações; versões
  sanitizadas foram geradas e os originais não devem ser versionados. Item
  permanece `[ ]`, progresso `7/17`.
- Revisão final aprovou documentação e sanitização das notificações. Os originais
  foram removidos do workspace; `013-08` continua bloqueada por relógio civil,
  CAS/Room e demais gates físicos, com item `[ ]` e progresso `7/17`.
- Gate adicional de tela apagada foi aprovado: sessão permaneceu em `Dozing` por
  75 segundos, FGS `health` e notificação persistiram, retorno pela notificação
  abriu sessão correta, pausa exibiu `PAUSED`/`Continuar`, e finalização/descarte
  confirmados removeram serviço. Morte real de processo, relógio civil, CAS/Room,
  tipos FGS dinâmicos e insets permanecem pendentes; item `[ ]`, `7/17`.
- Tentativa adicional de morte real do processo foi bloqueada pelo dispositivo:
  `am kill` não encerrou o FGS e `kill -9` retornou `Operation not permitted`.
  Sem `force-stop`, root, hook ou banco. Sessão foi limpa pela UI; gate permanece
  pendente e `013-08` segue `[ ]`, `7/17`.
- Revisão independente aprovou o registro documental com ressalvas e confirmou o
  bloqueio. Nova tentativa pode avaliar `am crash` se aceita como mecanismo de
  morte real, mantendo sem `force-stop`, root, hook ou banco; item permanece `[ ]`,
  progresso `7/17`.
- Gate repetido com `adb shell am crash PACKAGE` foi aceito como morte induzida real
  do processo: processo e FGS desapareceram; após reabrir o app, a recuperação
  exibiu `Retomar treino`, a retomada restaurou `Treino ativo` e o FGS, e a sessão
  foi finalizada/descartada com confirmação. Evidência cobre crash induzido pelo
  Android, não morte espontânea por OEM. `013-08` segue `[ ]`, `in_progress` e
  `7/17` por CAS, Room, relógio civil, safe insets, tipos FGS dinâmicos, evidências
  duráveis e revisão independente.
- Revisão independente do gate `am crash` aprovou o gate, mas reprovou conclusão de
  `013-08`: permanecem bloqueadores de liveness sob Room suspenso, validação de
  tipos FGS dinâmicos `location`/`connectedDevice` e trilha durável específica dos
  estados antes/depois do crash, reabertura, retomada e cleanup. Item permanece
  `[ ]`, progresso `7/17`.
- Análise isolada dos tipos FGS confirmou declaração completa no manifesto e
  promoção atual `health` + `location` somente em caminhada/corrida `ACTIVE`.
  Tentativa física de caminhada permaneceu em `GPS fraco`; preparação impediu
  countdown e FGS, sem relaxar readiness ou injetar localização. O vínculo com
  produtor GPS contínuo permanece dependência explícita de `013-09`.
- `connectedDevice` continua deliberadamente ausente da promoção runtime: conexão
  geral do app não comprova participação BLE contínua da sessão. Owner, lease,
  protocolo live e handoff pertencem a `013-11`. Nenhuma flag, intent, API ou
  evidência foi fabricada; tipos `health`/`location` atuais e ownership foram
  preservados. `013-08` segue `in_progress`, `[ ]`, `7/17`.
- Dependência circular de aceite foi identificada e resolvida no planejamento:
  `013-09`, `013-10` e `013-11` podem iniciar com a base operacional de `013-08`;
  o aceite integrado de `013-08` aguarda seus handoffs e evidências. Nenhum item
  adicional foi marcado.
- Revisão independente do bloqueio FGS aprovou somente o registro documental, com
  ressalvas. Confirmou dependência circular entre `013-08` e handoffs de
  `013-09`/`013-11`; nenhuma próxima subtarefa é elegível sem decisão explícita de
  planejamento. Status permanece `in_progress`, item `[ ]` e progresso `7/17`.
- Planejamento normalizado: `013-09`, `013-10` e `013-11` podem iniciar usando a
  base operacional de `013-08`; nenhum item foi marcado como concluído. O aceite
   integrado de `013-08` continua aguardando handoffs e evidências, e `013-12`
   permanece dependente da conclusão dessas três frentes.
- Decisão de produto registrada para `013-13`: correlação phone/watch não faz
  merge de métricas nem cria atividade duplicada. A atividade phone mantém
  sessão, tempo, distância, calorias, intervalos, ritmo, velocidade e rota; o
  import `WatchWorkoutImport` fica retido para CRC/proveniência/idempotência e
  somente BPM real elegível pode ser associado.
- Escopo de `013-14` ampliado para distância temporal: `DistanceRecord` deve
  representar intervalos da fonte correta, sem agregado concorrente. Sessão
  phone usa sua série phone, watch-only usa intervalos D3/D4 elegíveis e uma
  atividade correlacionada ignora distância do relógio. Correção de record
  agregado legado, retry e validação com app terceiro permanecem bloqueados por
  `013-12` e pela implementação da subtarefa.
- `013-09` concluída com waiver explícito do usuário para único critério sem
  evidência física: falha de `appendAcceptedPoints` não é reproduzível com
  segurança no dispositivo não-root disponível. Todos os demais gates manuais,
  GPS real, pausa/retomada, perda/recuperação, background, process death,
  métricas, tipos sem GPS e privacidade foram validados; builds passaram.
  Item marcado `[x]`; progresso atualizado para `8/17`. Risco de write failure
  permanece documentado como residual, sem alegação de teste executado.
- `013-10` recebeu card acessível na sessão ativa para treino livre e esteira,
  com ausência explícita de GPS, distância de esteira indisponível antes da
  calibração e métricas opcionais exibidas somente quando agregadas. A
  preparação passou a orientar a calibração da esteira. `git diff --check`,
  lint, builds debug/release e `installDebug` passaram. Validação física
  confirmou treino livre sem GPS, card ativo, duração, finalização/descarte e
  preparação de esteira sem localização. O handoff de início da esteira ficou
  em `START_REQUESTED`, bloqueando card ativo, pause/resume e screenshots
  correspondentes; diagnóstico confirmou falha no handoff compartilhado de
  `013-08` antes de `startForeground()`, com `startForegroundCount=0`. A
  correção exige serviço/coordenador compartilhados e confirmação real de
  foreground, portanto não foi absorvida por `013-10`. Item segue `[ ]`, status
  `in_progress` e progresso `8/17`.
- `013-10` foi marcada `blocked` após diagnóstico do handoff compartilhado de
  `013-08`. A execução pode prosseguir somente em outra subtarefa independente;
  `013-11` permanece elegível conforme grafo aprovado, sem marcar itens como
  concluídos.
- `013-11` foi selecionada explicitamente e marcada `in_progress`. Sua execução
  permanece isolada de `013-10`, com contratos e repositories congelados.
- `013-11` adicionou owner `APP_WORKOUT_SESSION`, participação BLE somente por
  ação explícita, status acessíveis e retenção do treino em desconexão. Builds,
  instalação, sessão sem participação, associação explícita e estado conectado
  foram validados no Galaxy S22. Protocolos D0/F002/FEA1 continuam sem contexto
  esportivo live seguro; BPM/passos permanecem ausentes. Desconexão, reconexão,
  conflito e screenshots duráveis permanecem pendentes. Item segue `[ ]`, status
  `in_progress` e progresso `8/17`.
- Revisão independente encontrou e a execução corrigiu dois riscos de
  concorrência: `connectedDevice` só é promovido após transporte confirmado e
  conexões da tela Dispositivo agora usam owner transitório no gate. Callbacks
  antigos de transporte também são ignorados por sessão. Builds e instalação
  foram repetidos com sucesso; os gates físicos de desconexão, reconexão,
  conflito e screenshots duráveis continuam pendentes. Item segue `[ ]`, status
  `in_progress` e progresso `8/17`.
- Gate físico sem dispositivo associado foi executado no Galaxy S22: sessão de
  treino livre iniciou, mostrou `Sem relógio associado`, manteve ausência de
  BPM/passos e não bloqueou o treino. Desconexão, reconexão, conflito e
  screenshots duráveis dos demais estados continuam pendentes. Item segue `[ ]`,
  status `in_progress` e progresso `8/17`.
- Nova revisão independente aprovou as correções estáticas, mas bloqueou a
  conclusão por ausência de gates físicos pós-correção: sem dispositivo,
  desconexão, reconexão e conflito. Screenshots duráveis também permanecem
  pendentes. Item segue `[ ]`, status `in_progress` e progresso `8/17`.
- Continuação física de `013-11` em 2026-08-04 reconectou o Champion C001 e
  confirmou preparação de treino livre sem GPS, ausência segura de BPM/passos e
  handoff explícito. Após o countdown, a UI permaneceu em `Solicitação de início
  emitida`; `dumpsys` confirmou FGS ativo com `startForegroundCount=1`, mas sem
  sessão ativa exposta. A reabertura após limpeza controlada mostrou `Treino
  interrompido` e recuperação disponível. O bloqueio pertence ao handoff
  compartilhado de `013-08`; não houve alteração de `013-08`, nenhum protocolo
  BLE não comprovado foi enviado e `013-11` continua `[ ]`, `in_progress`,
  progresso `8/17`. Desconexão, reconexão, conflito e screenshots duráveis seguem
  pendentes.
- Ainda em 2026-08-04, o Champion C001 foi esquecido e uma sessão de treino livre
  iniciou sem associação. A tela ativa confirmou `Sessão sem GPS`, ausência de
  métricas opcionais, `Status: Sem relógio associado` e continuidade do treino.
  O gate sem dispositivo pós-correção está aprovado; handoff com dispositivo,
  desconexão, reconexão, conflito e screenshots duráveis permanecem pendentes.
- Correções mínimas estáticas de `013-11` protegeram o POC debug com o gate
  compartilhado `DEVICE_CONNECTION`, incluindo cleanup idempotente, e liberaram
  `APP_WORKOUT_SESSION` após `AWAITING_RECOVERY` persistido antes do cleanup do
  serviço. Nenhum documento da subtarefa `013-08` foi editado; não houve comando
  BLE novo nem alteração de contratos, Room, GPS ou rota. `git diff --check`,
  `app:lintDebug`, lint, builds debug/release e `installDebug` passaram sem
  testes. Item permanece `[ ]`, status `in_progress` e progresso `8/17`; revisão
  independente estática deve ser repetida e gates físicos de desconexão,
   reconexão, conflito, handoff e screenshots continuam bloqueados.
- Revisão independente encontrou leak residual em falhas de protocolo do POC e
  construtor com gate local. Correção final removeu o gate local, centralizou
  cleanup terminal serializado e liberou `DEVICE_CONNECTION` por identidade em
  falhas de handshake/subscription/D0/FEA1. `git diff --check`, lint, builds
  debug/release e `installDebug` passaram; nenhum teste foi criado ou executado.
  O item permanece `[ ]`, status `in_progress`, progresso `8/17`; handoff
  associado, desconexão, reconexão, conflito e screenshots duráveis continuam
   bloqueados.
- Blindagem final de lifecycle de `013-11` adicionou lease por ciclo/sessão,
  cleanup `NonCancellable`, validação de eventos `Disconnected`/`Failed`, cleanup
  em cancelamento de BPM e compensação de conexão sem lease. `git diff --check` e
  `./gradlew lint assembleDebug assembleRelease` passaram; `installDebug` não
  pôde ser repetido porque `adb devices` estava sem dispositivo. Nenhum teste foi
  criado ou executado. Item permanece `[ ]`, status `in_progress`, progresso
  `8/17`; gates físicos e screenshots seguem pendentes.
- Correção P1 de `013-11` tratou o retorno `Success` de `connect()` quando o bind
  perde lease ou ciclo: compensação `NonCancellable` agora usa owner transitório
  `DEVICE_CONNECTION`, revalida ciclo/estado/lease e identidade da sessão sob as
  sincronizações existentes, e desconecta somente o transporte recém-criado ainda
  ativo. Owner concorrente, inclusive `APP_WORKOUT_SESSION`, bloqueia a
  compensação; release permanece identitário. `connectAndBootstrap`, cleanup
  existente, contratos e protocolos foram preservados. `git diff --check`, lint,
  builds debug/release e `installDebug` passaram; nenhum teste foi criado ou
 executado. `013-11` permanece `in_progress`, item `[ ]`, progresso `8/17`;
  corrida física de perda de lease/ciclo, desconexão, reconexão, conflito e
  screenshots duráveis continuam bloqueadas.
- Correção P1 final de `013-11` fechou a janela de rollover com
  `activeSessionId` nulo: `disconnectExpectedTransport` agora exige ciclo
  esperado, sessão esperada e estado PoC pré-bootstrap permitido
  (`Connecting`/`TransportReady`), além de manter referência do transporte,
  lease local, owner `DEVICE_CONNECTION` e rejeição de `APP_WORKOUT_SESSION`.
  `git diff --check` passou e `./gradlew lint assembleDebug assembleRelease`
  terminou com `BUILD SUCCESSFUL`; `adb devices` não mostrou dispositivo, então
  `installDebug` ficou bloqueado nesta rodada. Nenhum teste foi criado, alterado
  ou executado. `013-11` permanece `in_progress`, item `[ ]`, progresso `8/17`;
  rollover/perda de lease físico, desconexão, reconexão, conflito e screenshots
  duráveis continuam bloqueados.
- Revisão P1 adicional de `013-11` confirmou dois caminhos ainda fora do gate:
  `DeviceManagementViewModel.startScan()` e cancelamento de
  `WatchWorkoutParticipationCoordinator.enable()`. O scan agora usa owner
  transitório `DEVICE_CONNECTION`, recusa conflito antes do manager e libera o
  permit em `finally` `NonCancellable`; `enable()` usa cleanup identitário em
  `NonCancellable`, restaura `DISCONNECTED`, libera somente permit adquirido
  pela tentativa e desconecta somente transporte esperado. Nenhum owner de outra
  sessão é tocado. `git diff --check`, lint, builds debug/release e
  `installDebug` passaram; nenhum teste foi criado, alterado ou executado.
  `013-11` permanece `in_progress`, item `[ ]`, progresso `8/17`. Corridas
  físicas de cancelamento/perda de lease, conflito, desconexão, reconexão,
  handoff, métricas live e screenshots duráveis continuam bloqueadas.
- Revisão adicional confirmou P1 em falha não cancelável de
  `connectAssociatedDevice()`: transporte inválido podia permanecer ativo e
  ser reutilizado por `currentSession()` sem novo handshake. `enable()` agora
  executa cleanup identitário em `NonCancellable`, validando sessão, permit,
  `transportKey`, referência e `ChampionSessionId`; desconecta somente o
  transporte esperado, marca identidade inválida para impedir reuso e libera
  somente owner da tentativa sem tocar `APP_WORKOUT_SESSION` de outra sessão.
  `ChampionBleState.Disconnected` restaura `DISCONNECTED`, limpa transporte
  rastreado e mantém owner da sessão para reconexão controlada.
- `git diff --check` passou e `./gradlew lint assembleDebug assembleRelease`
  terminou com `BUILD SUCCESSFUL` (103 tarefas acionáveis; 29 executadas, 74
  atualizadas). `./gradlew installDebug` também terminou com `BUILD SUCCESSFUL`
  e instalou o APK no Galaxy S22; nenhum teste foi criado, alterado ou
  executado. `013-11`
 permanece `in_progress`, item `[ ]`, progresso `8/17`; falha não cancelável,
  reuso pós-falha, desconexão/reconexão, conflito, handoff e screenshots
  duráveis continuam bloqueados.
- Validação física adicional de `013-11` confirmou sessão livre com Champion C001
  associado, participação explícita e `Status: Conectado`, sem BPM/passos. Após
  `Desconectar` na tela Dispositivo, a UI mostrou `Dispositivo desconectado` e o
  `WorkoutRecordingService` permaneceu foreground (`startForegroundCount=1`),
  aprovando gate de desconexão. Reconexão não expôs candidato nem reabriu ação de
  associação; conflito, rollover, screenshots, handoff e BPM live seguem
  bloqueados. `013-11` permanece `[ ]`, `in_progress`, progresso `8/17`.
- Validação pós-correção de reconexão instalou APK no Galaxy S22 e confirmou que
  `Mais → Dispositivo → desconectar → Exercícios` restaura `ActiveWorkout` da
  mesma sessão, com FGS preservado. A tela Dispositivo chegou a mostrar
  desconectado/`Conectar`, mas o retorno ao treino mostrou `Status: Conectado`
  sem retry explícito observável; reconexão controlada permanece inconclusiva.
  Sessão foi limpa após validação. Item permanece `[ ]`, status `in_progress`,
  progresso `8/17`; conflito, rollover, handoff, screenshots e BPM live seguem
  bloqueados.
- Revisão independente final aprovou a implementação estática de `013-11` com
  ressalvas e bloqueou o fechamento somente por falta de gates físicos: falha não
  cancelável/reuso, desconexão/reconexão, conflito, rollover/perda de lease,
  handoff de `013-08`, screenshots e BPM live. Scan, lifecycle do POC, conflito
  estático e protocolos proibidos não têm bloqueador. `013-11` permanece `[ ]`,
  status `in_progress`, progresso `8/17`.
- Correção de reconexão de `013-11` implementada sem alterar owner, serviço,
  coordinator, protocolos BLE ou handoff de `013-08`. Ao sair de
  `ActiveWorkout` para `Device`, a navegação preserva somente o `sessionId` cuja
  sessão persistida ainda está `ACTIVE` ou `PAUSED`; ao retornar a Exercícios,
  valida novamente esse ID e reabre `ActiveWorkout`, nunca
  `FINISHING`/`AWAITING_RECOVERY`/terminal. O retry continua no caminho
  `ExercisesViewModel.retryWatchParticipation()` → `WorkoutRecordingService` →
  `WatchWorkoutParticipationCoordinator`, mantendo `APP_WORKOUT_SESSION` em
  desconexão. Arquivos alterados: `ChampionNavigation.kt`,
  `ExercisesViewModel.kt`, este overview e a task `013-11`.
- A validação desta correção passou em `git diff --check`,
  `./gradlew lint assembleDebug assembleRelease` e `./gradlew installDebug`;
  `adb devices` confirmou Galaxy S22 disponível. Nenhum teste automatizado foi
  criado, alterado ou executado. O gate físico pós-correção de desconexão →
  retorno a Exercícios → retry → reconexão ainda não foi executado nesta rodada;
 screenshots, conflito, rollover/perda de lease, handoff de `013-08` e BPM/
  passos live continuam bloqueados. Item permanece `[ ]`, status `in_progress`,
  progresso `8/17`.
- Correção adicional de `013-11` reconciliou estado stale do coordinator contra
  transporte/lease atual durante bind e retry. `ACTION_RETRY_WATCH` agora percorre
  API `retry(sessionId)` distinta de `enable`, preserva `APP_WORKOUT_SESSION`, não
  adquire `DEVICE_CONNECTION` e publica `CONNECTING`/`CONNECTED`/`DISCONNECTED`.
  `git diff --check` e lint/builds passaram; `installDebug` não pôde concluir
  porque `adb devices` estava sem dispositivo. Gate físico de retry/reconexão,
  screenshots, conflito, rollover, handoff `013-08` e BPM/passos live continuam
  pendentes. `013-11` permanece `[ ]`, `in_progress`, progresso `8/17`.
- Cleanup final de retry passou a preservar token de sessão/permit/transportKey/
  transportSessionId, desconectar somente transporte da tentativa mesmo após
  liberação local e rejeitar retry/rebind/enable em sessão terminal. Lint e
  builds debug/release passaram; `installDebug` aguardou dispositivo ADB. Gate
  físico `DISCONNECTED → Tentar novamente → CONNECTING → CONNECTED`, conflito,
  rollover, handoff, screenshots e BPM/passos live seguem pendentes. Item
  permanece `[ ]`, `in_progress`, progresso `8/17`.
- Bloqueadores estáticos residuais de `013-11` foram corrigidos no coordinator:
  `TransportReady` agora captura referência, chave, sessão e owner da tentativa;
  identidade incompleta invalida o transporte e impede adoção stale. Cleanup só
  desconecta a referência esperada e conserva invalidação fail-closed. Bind,
  enable, retry e bootstrap revalidam dentro de `operationMutex` os estados
  `FINISHING`, `AWAITING_RECOVERY`, `FINISHED` e `DISCARDED`; a API retry e os
  owners `APP_WORKOUT_SESSION`/`DEVICE_CONNECTION` foram preservados. Nenhum
  arquivo de `013-08`, teste, contrato ou protocolo proibido foi alterado.
  `git diff --check` e `./gradlew lint assembleDebug assembleRelease` passaram;
  `adb devices` não encontrou dispositivo, então `installDebug` não foi
  executado. Item permanece `[ ]`, `in_progress`, progresso `8/17`; gates
  físicos, handoff, screenshots e BPM/passos live continuam bloqueados.
- Correção P1 final confirmou corrida em `reconcileBoundStateLocked`: durante
  `connectionAttemptToken != null`, `TransportReady` pendente agora mantém
  `CONNECTING`, `pendingTransportSessionId` e token, sem promoção ou limpeza
  antecipada enquanto `connectAssociatedDevice()` ainda executa A1. Promoção só
  ocorre após captura identitária validada e `Success` do bootstrap para a mesma
  referência, chave, `ChampionSessionId`, permit e owner.
- Cleanup de falha/cancelamento captura estado, sessão e referência enquanto token
  existe, invalida ID, desconecta somente tentativa própria, limpa token/estado e
  libera owner esperado. `APP_WORKOUT_SESSION`, `DEVICE_CONNECTION`, retry
  separado, `canBind` terminal e protocolos proibidos foram preservados; nenhum
  arquivo de `013-08`, teste ou contrato foi alterado.
- Nesta rodada, `git diff --check` e `./gradlew lint assembleDebug
 assembleRelease` passaram. `adb devices` não encontrou dispositivo, então
  `installDebug` não foi executado; nenhum teste foi criado, alterado ou
  executado. Item permanece `[ ]`, status `in_progress`, progresso `8/17`;
  gates físicos de falha/cancelamento, rollover, desconexão, reconexão, conflito,
  handoff de `013-08`, screenshots e BPM/passos live continuam bloqueados.
- Bloqueador final de `013-11` corrigido: após `Success` do bootstrap e validação
  completa da identidade do transporte, `attemptConnection()` agora revalida
  `canBindSessionLocked(sessionId)` imediatamente antes da promoção final para
  `CONNECTED`, dentro de `operationMutex`. Sessão em `FINISHING`,
  `AWAITING_RECOVERY`, `FINISHED` ou `DISCARDED` não é promovida nem tem token
  limpo prematuramente; o cleanup identitário desconecta somente o transporte da
  tentativa e aplica a política de owner existente. `APP_WORKOUT_SESSION`,
  retry separado, `DEVICE_CONNECTION` e protocolos foram preservados. Nenhum
  arquivo de `013-08`, contrato ou teste foi alterado.
- A validação passou em `git diff --check` e
  `./gradlew lint assembleDebug assembleRelease` (`BUILD SUCCESSFUL`; 103
  tarefas acionáveis, 5 executadas e 98 atualizadas). `adb devices` confirmou
  Galaxy S22 `SM-S901E` conectado; `./gradlew installDebug` terminou com
  `BUILD SUCCESSFUL` e instalou o APK em 1 dispositivo. Nenhum teste foi criado,
  alterado ou executado. Corrida física durante bootstrap, desconexão,
  reconexão, conflito, rollover/perda de lease, handoff de `013-08`, screenshots
  e BPM/passos live continuam bloqueados; item permanece `[ ]`, status
  `in_progress` e progresso `8/17`.
- Gate físico de reconexão confirmou causa adicional no manager: `disconnect()`
  publicava `Disconnected`, mas o cleanup não carregava a referência do
  `ChampionTransportSession` encerrado; `currentSession()` podia continuar
  expondo transporte stale ou sofrer limpeza indevida durante tentativa
  concorrente. `disconnect()` e `handleClientEvent` agora passam a referência
  esperada, e `cleanupSession()` só limpa `activeTransport` quando referência e
  `sessionId` correspondem. Owners `APP_WORKOUT_SESSION`/`DEVICE_CONNECTION`,
  lifecycle, protocolos e retry/adoção fail-closed permanecem preservados.
- Arquivo de produção alterado: `core/bluetooth/ChampionConnectionManager.kt`.
  Nenhum arquivo de `013-08`, teste, contrato ou protocolo foi alterado. A
  validação desta correção passou em `git diff --check`,
  `./gradlew lint assembleDebug assembleRelease` e
  `./gradlew installDebug` no Galaxy S22. Nenhum teste foi criado, alterado ou
  executado. Gate físico `DISCONNECTED → Tentar novamente → CONNECTING →
  CONNECTED`, conflito, rollover, handoff de `013-08`, screenshots e
  BPM/passos live continuam bloqueados; item permanece `[ ]`, status
  `in_progress` e progresso `8/17`.
- Dois P1 residuais de `013-11` foram corrigidos no manager: scan e cancelamento
  usam mutex dedicado, impedindo disconnect stale de cancelar scan de geração
  nova; cancelamento externo de `connect()` publica `Disconnected` somente após
  cleanup identitário, com mesma geração e ausência confirmada de transporte e
  pending. Cleanup de pending continua por referência e estado novo não é
  sobrescrito. `git diff --check` e build completo passaram sem testes. Item
  permanece `[ ]`, `in_progress`, progresso `8/17`; gates físicos, handoff,
  screenshots e BPM/passos live continuam bloqueados.
- Correções adicionais de `013-11` fecharam quatro riscos de concorrência:
  callbacks/cleanup usam geração e referência de transporte; tentativas usam
  tombstone e `cancelAndJoin` antes da liberação terminal; retry sem owner
  delega para `enable()` fora do lock; e `TransportReady` não é mais adotado sem
  bootstrap A1 bem-sucedido. Retry com owner retido continua limitado e sem
  `DEVICE_CONNECTION`; nenhum protocolo BLE novo foi adicionado. O build e a
  instalação no Galaxy S22 passaram sem testes automatizados. Gates físicos de
  retry/reconexão, conflito, rollover, handoff `013-08`, screenshots e BPM/
  passos live permanecem pendentes; item segue `[ ]`, status `in_progress`,
  progresso `8/17`.
- Validação física continuada de `013-11` em 2026-08-05 confirmou conexão
  inicial explícita, FGS ativo, ausência segura de BPM/passos e cleanup de
  serviço/notificação após término e descarte. A desconexão exibiu estado correto
  em `Dispositivo`, mas o retorno a `ActiveWorkout` mostrou `Conectado` sem
  `Tentar novamente`; gate de reconexão permanece `FAIL`. Repetição ficou
  bloqueada pelo handoff de `013-08`, que terminou em `Solicitação de início
  emitida` sem FGS; o sintoma é compatível com bloqueio conhecido do handoff,
  mas a causa não foi isolada nesta execução. Conflito de owner, cancelamento
  durante handshake, rollover, recuperação e reconexão ponta a ponta permanecem
  sem aceite. Evidências
  sanitizadas estão em `docs/integration/013-11-evidence/`; nenhum arquivo de
  `013-08`, teste, contrato, banco ou protocolo BLE foi alterado. Item continua
  `[ ]`, status `in_progress`, progresso `8/17`.
- Reteste físico focado da correção `fc43550384f745f0b5bb42537e12ffbf786da210`
  confirmou novamente Gate A e reconhecimento `Conectado` na tela Dispositivo.
  Porém, `Desconectar` foi aceito durante participação do treino, contrariando
  bloqueio esperado por `APP_WORKOUT_SESSION`; ao retornar, ActiveWorkout mostrou
  `Status: Conectado` enquanto Dispositivo havia mostrado `Desconectado`, sem
  `Tentar novamente`. FGS permaneceu ativo e BPM/passos continuaram ausentes.
  A tentativa de repetição terminou em `Solicitação de início emitida`, sem FGS
  ou sessão ativa, portanto não reproduziu o mesmo cenário. Primeira divergência
  interna não foi isolada: manager, currentSession, owner e coordinator não são
  observáveis por UI/dumpsys/Logcat sanitizado. Evidências sanitizadas estão em
  `docs/integration/013-11-evidence/retest-fc43550/`. Não houve correção de
  produção, alteração de `013-08`, protocolo BLE, banco, GPS, rota, Health
  Connect, persistência ou testes. Item permanece `[ ]`, status `in_progress`,
  progresso `8/17`.
