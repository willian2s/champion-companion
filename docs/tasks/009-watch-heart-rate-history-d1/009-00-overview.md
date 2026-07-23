# Task 009-00 - Overview Historico D1 de frequencia cardiaca do relogio

## Status

Em andamento desde 2026-07-23. `009-01` e `009-02` concluidas; demais tasks pendentes.

## Objetivo

Controlar escopo, ordem, dependencias, paralelismo, checklist, validacao e divida tecnica da Fase 09, dedicada ao fluxo local-first de importacao D1, historico, grafico, agregacoes e fila pendente para futura sincronizacao na Fase 12.

## Documentos Autoritativos

- `AGENTS.md`
- `docs/specs/_inputs/phase-09-watch-heart-rate-history-direction.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- ADRs vigentes em `docs/decisions/`
- `docs/decisions/adr-009-health-metrics-hub-navigation.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- todas as tasks concluidas da Fase 08
- specs e overviews das Fases 03 a 07
- `docs/protocol/history-d1.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/handshake-a1.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

## Status Inicial

- `HealthScreen.kt` ja e hub de metricas e nao inicia BLE ao abrir.
- `HeartRateDetailScreen` ja e owner visual da medicao manual, mas grafico e historico ainda sao shell.
- `HealthViewModel.kt` ainda nao possui estado independente para D1, periodo, agregacoes ou ultima importacao.
- `HeartRateRepository` ja possui scaffolding parcial de D1 no dominio e na persistencia, mas sem queries por periodo nem metadado especifico de importacao.
- `ForegroundBleOperationGate` ainda nao possui owner `WATCH_D1_HISTORY`.
- `Device.importMetadata.lastImportedAt` continua semantica generica de resumo diario e nao pode ser reinterpretado como metadado exclusivo de D1.

## Achados De `009-01`

- `HealthMetricDetailScreens.kt` ainda mantem `HeartRateDetailScreen` como owner visual apenas da medicao manual; grafico segue placeholder, historico segue manual-only e `Buscar dados do relogio` ainda aparece apenas como texto futuro.
- `HealthViewModel.kt` ainda nao possui estado separado para importacao D1, periodo, agregacoes, ultima importacao D1 ou resultado tipado de sync local.
- `ChampionConnectionManager.kt`, `ChampionTransportSession.kt`, `ChampionNotificationHub.kt`, `ChampionPacketRouter.kt` e `DeviceConnectionBootstrap.kt` ja oferecem base de sessao, subscriptions e handshake A1 reutilizavel, mas auditoria nao encontrou parser D1, collector multipart nem coordinator D1 de produto.
- `ForegroundBleOperationGate` ainda expone apenas `DAILY_STEPS_REFRESH` e `MANUAL_HEART_RATE`; `WATCH_D1_HISTORY` continua ausente.
- `HeartRateAndSync.kt` ja contem scaffolding de dominio para `WATCH_D1_HISTORY`, `D1RecordKey` e `HeartRateDeduplicationKey.D1History`.
- `ChampionDatabase.kt` v2, `ChampionDaos.kt` e `RoomRepositories.kt` ja sustentam `deduplicationKey` unico e fila `PENDING` parcial para D1 elegivel novo, mas ainda sem metadata D1 dedicada, query por periodo, lote tipado, historico consolidado ou reparo de operacao pendente ausente.
- `HomeViewModel.kt`, `DeviceManagementViewModel.kt` e telas relacionadas continuam usando `Device.importMetadata.lastImportedAt` como metadado generico; esse campo nao representa ultima importacao D1.
- `docs/protocol/history-d1.md` confirma limite real atual: `D1 01 00 00` consulta historico automatico do dia corrente, com timeout foreground de 15 segundos, conclusao por `totalPackets > 0 && packetIndex >= totalPackets`, sem data/timezone no payload e sem semantica confirmada para offsets `6-13` e `15-18`.
- Fronteira correta confirmada: Fase 09 e local-first; Health Connect real fica para Fase 12, apesar do roadmap ainda divergente.

## Achados De `009-02`

- ADR-010 foi criada para registrar que Fase 09 e owner de D1 local-first e fila `PENDING`, enquanto Fase 12 e owner do provider Health Connect real.
- roadmap foi corrigido sem renumerar fases: Fase 09 deixou de prometer permissao, provider e escrita real; Fase 12 passou a explicitar processamento do backlog D1 pendente.
- `docs/protocol/history-d1.md` foi alinhado para refletir que regra de envio integral continua obrigatoria, mas implementacao real fica para Fase 12.
- spec da Fase 09 ficou alinhada a ADR-010 e consolidou que `DeviceImportMetadata.lastImportedAt` nao pode ser tratado como metadata D1 exclusiva.

## Achados De `009-03`

- `HealthFeatureContract.kt` agora separa medicao manual de importacao D1 por `surfaceState` e `d1ImportState`, mantendo state machines independentes.
- `HealthUiState` agora reserva `selectedHistoryPeriod`, `history` e `latestD1ImportMetadata`, sem depender de Compose nem de implementacao BLE.
- `RepositoryContracts.kt` agora reserva `WATCH_D1_HISTORY` no gate foreground e contratos `importD1Measurements(...)`, `observeHistory(...)` e `observeD1ImportMetadata(...)`.
- `HeartRateAndSync.kt` agora centraliza modelos duradouros de periodo, historico, agregacoes, grafico, progresso D1, resultado tipado, metadata e contagens de persistencia.
- `009-04` e `009-05` passam a poder paralelizar com base em contratos ja fixados, desde que coordenem edicoes concorrentes em `RepositoryContracts.kt` e modelos compartilhados.

## Estado Herdado Da Fase 08

- medicao manual D0/F002 funcional permanece inalterada;
- Home continua observando ultimo BPM persistido por repository;
- Saude raiz segue somente como hub;
- `HeartRateDetailScreen` continua sendo owner visual da funcionalidade de frequencia cardiaca;
- gate foreground ja arbitra `DAILY_STEPS_REFRESH` e `MANUAL_HEART_RATE`.

## Correcao Health Connect

- Fase 09: D1 local, deduplicacao, persistencia, metadata, historico, grafico, agregacoes, garantia de `PENDING`.
- Fase 12: provider real, disponibilidade, permissao, escrita, update, delete, lotes, retry do provider e transicoes `SYNCING/SYNCED`.
- Regra preservada: todos D1 validos devem continuar elegiveis para envio integral futuro.

## Limites

- nao implementar Health Connect real;
- nao executar D1 automatico;
- nao consultar sete ou trinta dias no relogio;
- nao tocar em D3/D4;
- nao criar task exclusiva de testes;
- nao alterar fases anteriores nesta execucao de planejamento.

## Lista Completa De Tasks

1. `009-01-audit-d1-protocol-history-and-health-connect-boundary.md`
2. `009-02-formalize-d1-local-first-adr-and-roadmap.md`
3. `009-03-define-d1-contracts-import-state-and-import-metadata-models.md`
4. `009-04-implement-d1-protocol-parser-and-multipart-collector.md`
5. `009-05-implement-d1-persistence-deduplication-and-pending-queue.md`
6. `009-06-implement-heart-rate-history-queries-aggregations-and-chart-policy.md`
7. `009-07-implement-d1-sync-coordinator-and-foreground-gate.md`
8. `009-08-integrate-health-viewmodel-action-progress-cancel-and-retry.md`
9. `009-09-implement-heart-rate-chart-and-full-history-ui.md`
10. `009-10-run-d1-physical-validation.md`
11. `009-11-close-watch-heart-rate-history-phase.md`

## Ordem

1. `009-01`
2. `009-02`
3. `009-03`
4. `009-04`
5. `009-05`
6. `009-06`
7. `009-07`
8. `009-08`
9. `009-09`
10. `009-10`
11. `009-11`

## Dependencias

| Task | Depende De |
| --- | --- |
| `009-01` | leitura inicial completa |
| `009-02` | `009-01` |
| `009-03` | `009-01`, `009-02` |
| `009-04` | `009-03` |
| `009-05` | `009-03` |
| `009-06` | `009-05` |
| `009-07` | `009-04`, `009-05` |
| `009-08` | `009-06`, `009-07` |
| `009-09` | `009-06`, `009-08` |
| `009-10` | `009-09` |
| `009-11` | `009-10` |

## Paralelismo

- `009-04` e `009-05` podem paralelizar apos `009-03` se evitarem tocar `RepositoryContracts.kt` ao mesmo tempo sem coordenacao.
- `009-06` pode iniciar assim que `009-05` estabilizar contratos de query e metadata.
- `009-07` pode avancar em paralelo parcial com `009-06`, mas depende de contrato estavel de persistencia D1.
- `009-08` e `009-09` devem permanecer sequenciais por conflito alto em `HealthFeatureContract.kt`, `HealthViewModel.kt` e `HealthMetricDetailScreens.kt`.
- `009-10` e `009-11` sao sequenciais.

## Arquivos Com Risco De Conflito

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/InMemoryForegroundBleOperationGate.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionPacketRouter.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`

## Checklist

- [x] `009-01` auditar protocolo D1, lacunas e fronteira Health Connect
- [x] `009-02` formalizar ADR, roadmap e alinhamento Fase 09/Fase 12
- [x] `009-03` definir contratos, metadata e state machine D1
- [ ] `009-04` implementar comando D1, parser e collector multipart
- [ ] `009-05` implementar persistencia, deduplicacao, fila `PENDING` e migration
- [ ] `009-06` implementar consultas, agregacoes e serie do grafico
- [ ] `009-07` implementar coordinator D1, gate e arbitragem BLE
- [ ] `009-08` integrar ViewModel, progresso, cancelamento, retry e lifecycle
- [ ] `009-09` implementar UI final de historico e grafico
- [ ] `009-10` registrar gate de build, smoke fisico e protocolo atualizado
- [ ] `009-11` fechar baseline, checklist, spec e divida tecnica

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

Falhas antigas e nao relacionadas nao bloqueiam. Regressao introduzida pela Fase 09 deve ser corrigida.

## Roteiro De Validacao Fisica

Galaxy S22 com Champion C001:

1. abrir Saude e depois Frequencia cardiaca;
2. confirmar ausencia de D1 automatico;
3. tocar `Buscar dados do relogio`;
4. observar progresso;
5. concluir ou cair em timeout parcial;
6. confirmar ultima importacao;
7. confirmar historico consolidado;
8. repetir importacao e confirmar deduplicacao;
9. alternar `Hoje`, `7 dias` e `30 dias`;
10. confirmar agregacoes;
11. confirmar grafico e alternativa textual;
12. cancelar tentativa;
13. validar cleanup;
14. iniciar medicao manual e tentar D1;
15. iniciar refresh de passos e tentar D1;
16. desligar Bluetooth e validar erro;
17. voltar e fazer retry;
18. mandar app para background durante D1;
19. confirmar operacao `PENDING` sem Health Connect;
20. confirmar ausencia de payload e BPM individual no Logcat.

## Divida Adiada

- escrita real no Health Connect para Fase 12;
- scheduler e retry do provider para Fase 12 ou 14;
- foreground service para importacoes longas para Fase 14;
- historico com BPM de treinos para fase futura apos decisoes de treino;
- cobertura ampla Compose e BLE para Fase 21;
- investigacao dos bytes D1 desconhecidos segue aberta.

## Riscos Atuais

- `HealthFeatureContract.kt`, `HealthViewModel.kt` e `HealthMetricDetailScreens.kt` ainda concentram estado manual e futura integracao D1; conflito alto esperado a partir de `009-03`.
- `Device.importMetadata.lastImportedAt` segue exposto em Home e Dispositivo; risco de reuso indevido como metadado D1 caso `009-03` nao introduza fonte propria.
- `RoomRepositories.import(...)` cria `PENDING` apenas para D1 novo; `009-05` precisa decidir reparo idempotente de operacao ausente para registros existentes.
- `ForegroundBleOperationGate` ainda nao conhece owner D1; `009-07` precisa expandir arbitragem sem quebrar manual heart rate nem refresh diario.
- protocolo D1 continua com bytes desconhecidos e sem data no payload; `009-04` precisa manter parser conservador e contexto de tentativa congelado.
- `009-03` agora depende explicitamente de contratos documentados em ADR-010 para metadata D1, state machine separada e fronteira definitiva com fila `PENDING`.
- `009-04` e `009-05` devem manter compatibilidade com contratos recem-fixados e evitar renomeacoes paralelas de modelos D1 sem coordenacao.

## Criterios Gerais

- Fase 09 continua local-first;
- nenhum acesso real ao Health Connect acontece antes da Fase 12;
- D1 so inicia por acao explicita;
- banco local permanece fonte visual primaria de Saude;
- Home continua consumindo repository, nunca BLE direto;
- qualquer migration de Room deve exportar schema novo;
- mudancas visuais devem gerar screenshots conforme `AGENTS.md`.

## Instrucao Para Atualizacao Do Checklist

- Toda task concluida deve atualizar este overview no mesmo trabalho.
- Marcar item correspondente apenas apos validacao minima da task.
- Se surgir dependencia nova ou risco novo, registrar aqui imediatamente.
- Se task alterar ordem ou paralelismo, este arquivo deve refletir a ordem real antes da proxima execucao.
