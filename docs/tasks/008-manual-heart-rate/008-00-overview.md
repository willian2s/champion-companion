# Task 008-00 - Overview Frequencia Cardiaca Manual

## Status

Em andamento. `008-01`, `008-02`, `008-03`, `008-04`, `008-05`, `008-06`, `008-07`, `008-08`, `008-08A` e `008-08B` concluidas em 2026-07-22; `008-09` e `008-10` seguem pendentes. Após a conclusão de `008-08`, foi aprovada extensão arquitetural da área de Saúde. A extensão segue por `008-08A` e `008-08B`, sem reabrir a task original.

## Objetivo

Controlar escopo, ordem, dependencias, paralelismo, checklist, validacao e divida tecnica da Fase 08, que agora inclui medicao manual de produto, integracao do ultimo BPM na Home e extensao da area de Saude para hub de metricas com telas especificas de frequencia cardiaca, peso, passos e distancia.

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

## Achados De `008-06`

- `HealthViewModel` agora é `@HiltViewModel` e owner do fluxo de produto da tela Saúde, sem mover detalhes técnicos para Compose.
- ViewModel observa dispositivo associado, última medição válida e recentes manuais direto dos repositories e combina tudo em `HealthUiState` único.
- `StartMeasurement` continua explícito; `Load` não inicia medição e morte de processo restaura `Idle` com histórico persistido vindo do repository.
- Resultado `Completed` do coordinator agora passa por persistência no ViewModel antes de fechar sessão como concluída; falha de storage vira erro seguro `STORAGE_FAILED`.
- `CancelMeasurement`, `ConfirmExitMeasurement` e novo intent `AppMovedToBackground` cancelam sessão sem persistir parcial.
- Testes pequenos cobrem ausência de auto-start em `Load`, persistência do resultado válido, cancelamento em background sem persistência e conflito de gate virando erro seguro.

## Achados De `008-07`

- `HealthScreen` deixou de ser placeholder e agora coleta `HealthViewModel` diretamente, enviando `Load` sem auto-start e `AppMovedToBackground` ao sair para background.
- UI passou a renderizar estados `Idle`, `Preparing`, `Connecting`, `Starting`, `Measuring`, `Stopping`, `Completed`, `Cancelled` e `Failed` a partir de `HealthSurfaceState`.
- Sessão ativa mostra BPM grande, indicador discreto de valor estimado, qualidade do sinal, tempo restante, progresso e CTA de cancelamento.
- `Completed`, `Cancelled` e `Failed` agora mostram feedback de produto com copy segura em pt-BR e ações compatíveis com retry, dismiss e nova medição.
- Última medição e lista simples de recentes ficaram visíveis na própria tela Saúde, sem acesso direto a BLE, DAO ou repository pela Compose.
- `BackHandler` e diálogo de confirmação passaram a proteger saída durante sessão ativa sem introduzir start automático.

## Achados De `008-08`

- `HomeViewModel` agora observa `HeartRateRepository.observeLatestValidMeasurement()` e expõe último BPM persistido na Home sem iniciar BLE nem consultar estado técnico da PoC.
- `HomeScreen` ganhou card próprio para última frequência cardíaca, visível mesmo sem resumo diário, com valor e horário ou estado `Sem medições`.
- `HomeViewModel` agora usa `ForegroundBleOperationGate` antes do refresh diário e converte conflito com medição manual em erro seguro `BLE_OPERATION_IN_PROGRESS`.
- `HealthScreen` agora mantém tela acordada durante sessão ativa por `view.keepScreenOn` e continua cancelando ao background.
- `ChampionNavigation` passou a interceptar saída da aba Saúde durante sessão ativa, exibindo confirmação antes de navegar para outro destino.
- `DeviceManagementViewModel` agora evita auto-connect e connect manual enquanto `MANUAL_HEART_RATE` estiver ativo no gate compartilhado, reduzindo concorrência de foreground BLE.

## Extensao Pos `008-08`

- `008-08` permanece concluida e preservada. Ela continua representando a entrega original de Home + lifecycle + integracao do ultimo BPM persistido.
- Auditoria de codigo atual confirma que `AppDestination` ainda possui apenas destinos top-level sem parametros, com unica rota `Health` em `core/navigation/AppDestination.kt`.
- `ChampionNavigation.kt` ainda renderiza `HealthScreen()` diretamente para rota `Health`; nao existe sub-rota atual para detalhe de frequencia cardiaca, peso, passos ou distancia.
- `HealthScreen.kt` ainda combina na mesma rota raiz: cabecalho da Saude, sessao de medicao manual, ultima medicao e historico recente manual.
- `HealthViewModel.kt` hoje observa apenas dispositivo associado e dados de frequencia cardiaca; nao agrega peso, passos ou distancia para Saude.
- `HomeViewModel.kt` ja observa `DailySummaryRepository.observeSummaryForDate(...)`, `HeartRateRepository.observeLatestValidMeasurement()` e `PersonalMetricsRepository`, confirmando que passos, distancia estimada e ultimo BPM persistido ja possuem fontes seguras existentes para reuso.
- `PersonalMetricsRepository.observeSnapshot()` em `RoomRepositories.kt` ja deriva ultimo peso persistido por `UserProfileDao.observeLatestWeight(...)`, o que viabiliza shell inicial de peso sem nova persistencia.
- `DailySummary` e `HomeViewModel` confirmam que distancia atual disponivel nesta fase continua sendo estimativa local derivada do resumo diario; quando faltam dados pessoais, valor permanece ausente sem inventar numero.
- Extensao aprovada exigiu formalizacao arquitetural antes da implementacao visual, porque altera arquitetura duradoura de navegacao e ownership visual da area Saude.
- `008-08A` define rotas finais planejadas para implementacao: `AppDestination.Health`, `AppDestination.HealthHeartRate`, `AppDestination.HealthWeight`, `AppDestination.HealthSteps` e `AppDestination.HealthDistance`.
- `008-08A` tambem formaliza ADR-009 e atualizacao do roadmap nas fases 8, 9, 12, 15 e 16.

## Achados De `008-08B`

- `HealthScreen.kt` deixou de ser superfície direta da medição manual e passou a atuar como hub de métricas com cards de frequência cardíaca, peso, passos e distância.
- `HealthMetricDetailScreens.kt` concentra telas específicas de frequência cardíaca, peso, passos e distância, com estrutura comum de resumo atual, gráfico e histórico.
- UI atual da medição manual foi composta dentro da tela específica de frequência cardíaca; não houve nova implementação de coordinator, D0 ou F002.
- `ChampionNavigation.kt` agora navega para `HealthHeartRate`, `HealthWeight`, `HealthSteps` e `HealthDistance`, mantendo `Saúde` como item selecionado em toda subtree.
- `HealthViewModel.kt` passou a observar `DailySummaryRepository`, `PersonalMetricsRepository` e `AppPreferencesRepository`, agregando peso, passos, distância e meta diária sem criar novas sincronizações.
- `PersonalMetricsSnapshot` e `RoomRepositories.observeSnapshot()` passaram a expor timestamp e origem do último peso persistido, permitindo shell inicial de peso sem expansão de schema.
- Gráfico real, histórico completo, D1 e Health Connect continuam fora da implementação entregue.
- Validação mínima executada em `008-08B`: `assembleDebug`, `assembleRelease` e `installDebug`, com instalação confirmada em `SM-S901E`.

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
- `008-08A` pode alterar roadmap e criar ADR, mas continua sem reabrir `008-08`.

## Lista De Tasks

1. `008-01-audit-poc-heart-rate-domain-and-health-feature.md`
2. `008-02-define-manual-heart-rate-contracts-state-and-operation-gate.md`
3. `008-03-promote-d0-f002-protocol-and-estimator.md`
4. `008-04-implement-manual-heart-rate-coordinator.md`
5. `008-05-implement-manual-heart-rate-persistence-and-queries.md`
6. `008-06-create-health-viewmodel.md`
7. `008-07-create-manual-heart-rate-health-ui.md`
8. `008-08-integrate-latest-heart-rate-into-home-and-lifecycle.md`
9. `008-08A-formalize-health-metrics-navigation-and-roadmap.md`
10. `008-08B-implement-health-hub-and-metric-detail-shells.md`
11. `008-09-run-manual-heart-rate-physical-validation.md`
12. `008-10-close-manual-heart-rate-phase.md`

## Ordem

1. `008-01`
2. `008-02`
3. `008-03`
4. `008-04`
5. `008-05`
6. `008-06`
7. `008-07`
8. `008-08`
9. `008-08A`
10. `008-08B`
11. `008-09`
12. `008-10`

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
| `008-08A` | `008-08` |
| `008-08B` | `008-08A` |
| `008-09` | `008-08B` |
| `008-10` | `008-09` |

## Paralelismo

- `008-03` e `008-05` podem paralelizar apos `008-02` se evitarem editar `RepositoryContracts.kt` ao mesmo tempo.
- parte visual de `008-07` pode preparar layout apos contrato e ViewModel de `008-06`, mas `HealthScreen.kt` precisa owner unico.
- `008-08` foi sequencial porque integrou Home, Saude, lifecycle, gate e navegacao.
- `008-08A` deve ser sequencial antes de `008-08B`, porque precisa formalizar ADR futura, roadmap futuro e contratos de rotas.
- `008-08B` passa a ser nova integracao sequencial entre Saude, navegacao, Home e repositories existentes.
- `008-09` e `008-10` sao sequenciais.

## Arquivos Com Risco De Conflito

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/ChampionDailyStepsSyncCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocHeartRateSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocF002BpmEstimator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/AppDestination.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionPacketRouter.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DomainTime.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`

## Riscos Da Extensao

- Navegacao: `AppDestination` e `ChampionNavigation` ainda nao possuem estrutura para rotas de metricas; risco de conflito alto se outra mudanca tocar back stack manual.
- `HealthScreen`: hoje concentra sessao manual, ultima medicao e historico recente; extensao vai decompor essa responsabilidade sem perder comportamento entregue por `008-07`/`008-08`.
- Tela de frequencia cardiaca: fluxo manual atual mora visualmente na rota raiz de Saude; mover/compor sem duplicar coordinator exige cuidado.
- `HealthViewModel`: hoje nao agrega peso, passos e distancia; extensao pode exigir combinacao maior de estado ou novos ViewModels coordenados.
- `HomeScreen` e `HomeViewModel`: passos, distancia estimada e ultimo BPM ja aparecem na Home; risco de duplicar formatacao/regra ou criar divergencia visual entre Home e hub de Saude.
- Repositories de frequencia cardiaca: continuam fonte da ultima medicao e recentes manuais; nao podem ser bypassados nem expandido com historico rico indevido nesta fase.
- Repositories/resumo diario: passos e distancia dependem de `DailySummary`; extensao nao pode criar nova sincronizacao FEA1 nem inventar dados ausentes.
- Metricas pessoais: peso e distancia dependem de dados pessoais atuais; quando faltarem dados suficientes, UI deve mostrar indisponivel com semantica correta.
- Componentes compartilhados do design system: abstrair cedo demais pode criar framework generico desnecessario; reutilizacao deve ser minima e comprovada.

## Checklist

- [x] `008-01` auditoria de estado real, MotionLog e regra da media concluida
- [x] `008-02` contratos, state machine e gate BLE definidos
- [x] `008-03` D0/F002 promovidos para componentes reutilizaveis unicos
- [x] `008-04` coordinator manual implementado
- [x] `008-05` persistencia final, deduplicacao e queries implementadas
- [x] `008-06` `HealthViewModel` criado
- [x] `008-07` UI da Saude criada
- [x] `008-08` Home, lifecycle, confirmacao de saida e integracao final implementados
- [x] `008-08A` formalizacao arquitetural, ADR-009 e roadmap alinhados
- [x] `008-08B` hub de Saude e shells das telas de metricas implementados
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
2. visualizar os quatro cards;
3. confirmar valores disponiveis;
4. abrir frequencia cardiaca;
5. executar medicao manual;
6. concluir e salvar;
7. voltar para Saude;
8. confirmar atualizacao do card;
9. abrir Peso;
10. confirmar valor atual ou estado vazio;
11. abrir Passos;
12. confirmar passos atuais;
13. abrir Distancia;
14. confirmar valor ou estado indisponivel;
15. confirmar navegacao de retorno;
16. confirmar estados vazios de grafico e historico;
17. confirmar ausencia de dados simulados;
18. confirmar que nenhuma tela inicia BLE ao abrir;
19. confirmar que D1 nao foi implementado;
20. confirmar que Health Connect nao foi implementado.

## Divida Adiada

- D1, acao futura de buscar dados do relogio, grafico de BPM e historico completo de frequencia cardiaca para Fase 09.
- Health Connect para Fase 12.
- foreground service para Fase 14.
- historico/graficos de passos e distancia para Fase 15 atual do roadmap.
- edicao e gerenciamento de peso para Fase 16 atual do roadmap.
- testes Compose amplos para Fase 21.
- harness BLE deterministico ampliado para Fase 21.

## Criterios De Conclusao

- Saude deixa de ser placeholder.
- Medicao inicia so por acao explicita.
- `F002_NOTIFY` antecede `D0 01`.
- Resultado final persiste uma vez por sessao.
- Sessao cancelada nao persiste.
- Home mostra ultimo BPM persistido.
- Saude raiz funciona como hub de metricas.
- Quatro cards principais navegam para telas especificas.
- Grafico real continua fora da Fase 08.
- Historico completo continua fora da Fase 08.
- Gate impede concorrencia com refresh diario.
- Background cancela medicao sem parcial persistido.
- Gate obrigatorio e smoke fisico registrados.

## Instrucoes De Atualizacao

- Cada task atualiza proprio `Status` e item correspondente neste overview no mesmo trabalho.
- Se task bloquear, registrar bloqueio no proprio arquivo e manter checklist pendente.
- `008-10` consolida checklist, resultados dos comandos, riscos aceitos, validacao manual e baseline factual.
