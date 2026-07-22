# Task 008-00 - Overview Frequencia Cardiaca Manual

## Status

Em andamento. `008-01`, `008-02`, `008-03`, `008-04` e `008-05` concluidas em 2026-07-22; demais tasks ainda pendentes.

## Objetivo

Controlar escopo, ordem, dependencias, paralelismo, checklist, validacao e divida tecnica da Fase 08, que transforma fluxo tecnico D0/F002 em medicao manual de produto com persistencia local e exibicao na Saude e na Home.

## Documentos Autoritativos

- `AGENTS.md`
- `docs/specs/_inputs/phase-08-manual-heart-rate-direction.md`
- `docs/specs/008-manual-heart-rate.md`
- ADRs vigentes em `docs/decisions/`
- `docs/decisions/adr-007-controlled-associated-device-auto-connect.md`
- `docs/decisions/adr-008-app-open-ble-session-ownership.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- todas as tasks concluidas em `docs/tasks/007-daily-summary-home/`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/004-native-ble-core.md`
- `docs/specs/003-domain-and-persistence-foundation.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`
- `docs/protocol/history-d1.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/baseline/current-baseline.md`

## Status Inicial

- `HealthScreen.kt` ainda e placeholder.
- `HealthFeatureContract.kt` ainda so expone `data object HealthUiState`.
- `HomeScreen` e `HomeViewModel` nao observam BPM persistido.
- `DevicePocCoordinator` ja possui fluxo tecnico com D0/F002, guards de sessao e cleanup.
- `DevicePocHeartRateSession` e `DevicePocF002BpmEstimator` ja contem builder/parser/buffer/estimador reutilizaveis, mas ainda vivem na feature debug.
- `HeartRateSource.MANUAL_MEASUREMENT` e `HeartRateDeduplicationKey.MeasurementSession` ja existem no dominio.
- `HeartRateRepository` ainda nao expone save/observe para medicao manual.
- schema `heart_rate_measurements` atual ja parece suficiente para um unico resultado final por sessao.

## Achados De `008-01`

- `HealthScreen.kt` segue placeholder e `HealthFeatureContract.kt` ainda nao possui estado/intents reais de produto.
- MotionLog manual da Saude confirmou politica final `ultimo D0 valido -> estimativa F002 -> null`; auditoria nao encontrou media nova no fechamento da sessao manual de 60s.
- `DevicePocCoordinator`, `DevicePocHeartRateSession` e `DevicePocF002BpmEstimator` ja concentram builder/parsers/buffer/estimador e cleanup tecnico reutilizavel, ainda presos ao escopo PoC debug.
- `HeartRateSource.MANUAL_MEASUREMENT`, `HeartRateDeduplicationKey.MeasurementSession` e schema `heart_rate_measurements` v2 ja suportam resultado manual final sem migration.
- `HeartRateRepository`, `HeartRateDao` e `RoomRepositories` ainda nao expoem save manual final, ultima medicao valida ou recentes manuais.
- `ChampionConnectionManager` segue owner da unica sessao BLE ativa e `connect()` continua derrubando sessao atual antes de nova conexao.
- `ChampionDailyStepsSyncCoordinator` reusa sessao ativa compativel, mas esse reuso nao e ownership atomico nem gate global; falta coordenacao cross-feature entre Home, Dispositivo e futura medicao manual.

## Achados De `008-02`

- `HealthFeatureContract.kt` agora formaliza state machine de produto sem booleans contraditorios, usando `HealthSurfaceState` e `ManualHeartRateSessionState`.
- Contrato de medicao manual passou a exigir sessao explicita com `manualMeasurementSessionId`, `bleSessionId`, `startedAt`, `monotonicStartedAt`, `progress`, `remaining`, leitura atual, qualidade, resultado final e erro seguro.
- `RepositoryContracts.kt` agora reserva gate foreground compartilhado por `ForegroundBleOperationGate`, com owners `DAILY_STEPS_REFRESH` e `MANUAL_HEART_RATE`, aquisicao por `tryAcquire(...)` e liberacao por `release(...)`.
- `HeartRateRepository` agora tem contratos-alvo para save manual final, ultima medicao valida e recentes manuais, sem tocar DAO/Room nesta task.
- `DomainTime.kt` agora expoe `MonotonicClock` testavel e helpers puros de progresso/restante para medicao de 60s.
- `HomeFeatureContract.kt` ficou alinhado ao gate ao reservar erro `BLE_OPERATION_IN_PROGRESS` para conflito seguro com refresh diario.

## Achados De `008-03`

- Protocolo tecnico manual D0/F002 foi promovido para `core/bluetooth/ChampionManualHeartRateProtocol.kt`.
- Estimador, qualidade e selecao final foram promovidos para `core/bluetooth/ChampionF002BpmEstimator.kt`.
- PoC debug permaneceu funcional por delegacao: `DevicePocHeartRateSession` e `DevicePocF002BpmEstimator` agora sao adapters finos de compatibilidade.
- Nao restou duplicacao de builder `D0 01`, builder `D0 00`, parser D0, parser F002, buffer limitado ou heuristica do estimador.
- Suite pequena de regressao tecnica foi mantida na PoC e ganhou cobertura explicita para pacote D0 sem BPM valido.

## Achados De `008-04`

- `ChampionManualHeartRateCoordinator` agora controla fluxo de produto da medicao manual sem copiar `DevicePocCoordinator` inteiro.
- `InMemoryForegroundBleOperationGate` foi implementado e ligado por Hilt como gate foreground compartilhado.
- Coordinator reusa sessao ativa compativel, executa handshake, garante `F002_NOTIFY`, envia `D0 01`, acompanha progresso monotonic e tenta `D0 00` quando seguro.
- Resultado final continua efemero no coordinator; persistencia segue para `008-05`/`008-06`.
- Testes pequenos agora cobrem gate, callback tardio ignorado, stop falho sem bloquear cleanup e timeout sem reaproveitar valor antigo.
- Nenhuma ADR nova foi necessaria neste ponto; gate atual permanece compatível com ADR-007 e ADR-008.

## Achados De `008-05`

- `HeartRateMeasurement.createManualMeasurement(...)` agora centraliza criacao do registro final manual valido com `MANUAL_MEASUREMENT` e dedup por `MeasurementSession(sessionId)`.
- `HeartRateDao` agora expoe query observavel generica da ultima medicao valida e query observavel de recentes manuais validos sem alterar schema ou versao do banco.
- `RoomRepositories.saveManualMeasurement(...)` agora e idempotente por `sessionId`, retorna registro existente em retries e nao cria `HealthSyncOperation` para medicao manual.
- Importacao D1 e `findPendingSync()` permaneceram inalterados e continuam restritos a `WATCH_D1_HISTORY` valido com sync pendente.
- Testes pequenos cobrem factory manual, round-trip de mapper e queries criticas de ultima medicao valida e recentes manuais.
- Schema atual v2 bastou; nenhuma migration foi necessaria.

## Estado Herdado

- ADR-007 permite auto-connect controlado do dispositivo associado, mas isso nao autoriza medicao automatica.
- ADR-008 mantem sessao BLE saudavel enquanto app estiver aberto, o que permite reuso entre telas.
- `ChampionConnectionManager` e owner da unica sessao BLE ativa e `connect()` derruba sessao anterior antes de abrir nova.
- `ChampionDailyStepsSyncCoordinator` ja usa sessao ativa do associado quando compativel; manual heart rate deve seguir regra similar, mas com gate de ownership mais forte.

## Limites

- Medicao so por acao explicita.
- Persistir somente resultado final.
- Sem Health Connect.
- Sem task exclusiva de testes.
- Sem stream D0 ou sinal F002 persistidos.
- Sem D1.
- Sem foreground service.
- Sem alterar roadmap, ADRs vigentes ou docs concluidos de fases anteriores.

## Lista De Tasks

1. `008-01-audit-poc-heart-rate-domain-and-health-feature.md`
2. `008-02-define-manual-heart-rate-contracts-state-and-operation-gate.md`
3. `008-03-promote-d0-f002-protocol-and-estimator.md`
4. `008-04-implement-manual-heart-rate-coordinator.md`
5. `008-05-implement-manual-heart-rate-persistence-and-queries.md`
6. `008-06-create-health-viewmodel.md`
7. `008-07-create-manual-heart-rate-health-ui.md`
8. `008-08-integrate-latest-heart-rate-into-home-and-lifecycle.md`
9. `008-09-run-manual-heart-rate-physical-validation.md`
10. `008-10-close-manual-heart-rate-phase.md`

## Ordem

1. `008-01`
2. `008-02`
3. `008-03`
4. `008-04`
5. `008-05`
6. `008-06`
7. `008-07`
8. `008-08`
9. `008-09`
10. `008-10`

## Dependencias

| Task | Depende De |
| --- | --- |
| `008-01` | leitura inicial completa |
| `008-02` | `008-01` |
| `008-03` | `008-02` |
| `008-04` | `008-02`, `008-03` |
| `008-05` | `008-02` |
| `008-06` | `008-02`, `008-04`, `008-05` |
| `008-07` | `008-06` |
| `008-08` | `008-04`, `008-05`, `008-06`, `008-07` |
| `008-09` | `008-08` |
| `008-10` | `008-09` |

## Paralelismo

- `008-03` e `008-05` podem paralelizar apos `008-02` se evitarem editar `RepositoryContracts.kt` ao mesmo tempo.
- parte visual de `008-07` pode preparar layout apos contrato e ViewModel de `008-06`, mas `HealthScreen.kt` precisa owner unico.
- `008-08` deve ser sequencial, porque integra Home, Saude, lifecycle, gate e navegacao.
- `008-09` e `008-10` sao sequenciais.

## Arquivos Com Risco De Conflito

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/ChampionDailyStepsSyncCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocHeartRateSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocF002BpmEstimator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionPacketRouter.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DomainTime.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`

## Checklist

- [x] `008-01` auditoria de estado real, MotionLog e regra da media concluida
- [x] `008-02` contratos, state machine e gate BLE definidos
- [x] `008-03` D0/F002 promovidos para componentes reutilizaveis unicos
- [x] `008-04` coordinator manual implementado
- [x] `008-05` persistencia final, deduplicacao e queries implementadas
- [ ] `008-06` `HealthViewModel` criado
- [ ] `008-07` UI da Saude criada
- [ ] `008-08` Home, lifecycle, confirmacao de saida e integracao final implementados
- [ ] `008-09` gate reduzido e smoke fisico executados
- [ ] `008-10` fechamento, baseline e divida consolidados

## Comandos Minimos

Gate obrigatorio:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
```

Informativos:

```bash
./gradlew lint
./gradlew testDebugUnitTest
```

Falhas antigas e nao relacionadas nao bloqueiam. Regressao introduzida pela Fase 08 deve ser corrigida.

## Roteiro Manual

Galaxy S22 com Champion C001:

1. abrir Saude;
2. confirmar estado sem medicao quando aplicavel;
3. tocar `Medir frequencia cardiaca`;
4. conectar ao relogio quando necessario;
5. confirmar inicio da medicao;
6. confirmar progresso de 60 segundos;
7. confirmar BPM em tempo real;
8. confirmar atualizacao do tempo restante;
9. aguardar conclusao;
10. confirmar resultado final;
11. confirmar persistencia;
12. fechar e reabrir o app;
13. confirmar ultima medicao;
14. confirmar BPM na Home;
15. iniciar segunda medicao;
16. confirmar novo registro;
17. cancelar uma medicao;
18. confirmar ausencia de registro da sessao cancelada;
19. tentar iniciar duas medicoes;
20. confirmar bloqueio;
21. iniciar refresh de passos e tentar medir simultaneamente;
22. confirmar bloqueio seguro de uma das operacoes;
23. desconectar durante a medicao;
24. confirmar cleanup e ausencia de persistencia;
25. enviar app para background durante a medicao;
26. confirmar cancelamento;
27. desligar Bluetooth;
28. confirmar erro seguro;
29. ligar Bluetooth e tentar novamente;
30. confirmar ausencia de Health Connect;
31. confirmar ausencia de BPM e F002 no Logcat.

## Divida Adiada

- D1 e historico automatico para Fase 09.
- Health Connect para Fase 12.
- foreground service para Fase 14.
- historico rico e graficos para Fase 09/15.
- testes Compose amplos para Fase 21.
- harness BLE deterministico ampliado para Fase 21.

## Criterios De Conclusao

- Saude deixa de ser placeholder.
- Medicao inicia so por acao explicita.
- `F002_NOTIFY` antecede `D0 01`.
- Resultado final persiste uma vez por sessao.
- Sessao cancelada nao persiste.
- Home mostra ultimo BPM persistido.
- Gate impede concorrencia com refresh diario.
- Background cancela medicao sem parcial persistido.
- Gate obrigatorio e smoke fisico registrados.

## Instrucoes De Atualizacao

- Cada task atualiza proprio `Status` e item correspondente neste overview no mesmo trabalho.
- Se task bloquear, registrar bloqueio no proprio arquivo e manter checklist pendente.
- `008-10` consolida checklist, resultados dos comandos, riscos aceitos, validacao manual e baseline factual.
