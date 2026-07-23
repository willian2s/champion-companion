# Task 009-01 - Auditar protocolo D1, historico e fronteira Health Connect

## Status

Concluida em 2026-07-23.

## Objetivo

Auditar estado real do protocolo D1, lacunas conhecidas, estado real do codigo apos a Fase 08 e divergencia atual entre roadmap e fronteira correta com Health Connect antes de qualquer implementacao funcional da Fase 09.

## Contexto

O app ja possui hub de Saude, tela especifica de frequencia cardiaca, medicao manual funcional, scaffolding parcial de `WATCH_D1_HISTORY` no dominio e fila `PENDING` parcial na persistencia. Ainda assim, Fase 09 nao tem parser D1, collector multipart, metadata especifica, queries por periodo nem UI funcional. Roadmap ainda descreve Health Connect real dentro da Fase 09 e precisa ser corrigido com base no documento de direcao autoritativo.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/_inputs/phase-09-watch-heart-rate-history-direction.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-009-health-metrics-hub-navigation.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/protocol/history-d1.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/handshake-a1.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/baseline/current-baseline.md`

## Dependencias

- nenhuma task anterior da Fase 09.

## Entradas

- codigo real em `feature/health`, `feature/home`, `feature/device`, `core/bluetooth`, `core/model`, `core/database`, `core/navigation`, `data`;
- documentos de protocolo e baseline ja existentes;
- divergencia explicitada entre input da Fase 09 e roadmap atual.

## Arquivos Exatos Permitidos

- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-01-audit-d1-protocol-history-and-health-connect-boundary.md`

## Arquivos Proibidos

- qualquer `.kt`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-*.md`
- `app/schemas/**`

## Escopo

- confirmar contrato D1 real e limite do comando atual;
- confirmar offsets, bytes desconhecidos e criterio de conclusao;
- confirmar estado real herdado da Fase 08 no codigo;
- registrar lacunas de metadata, deduplicacao, gate e historico local;
- registrar que Fase 09 e local-first e que Health Connect real fica para Fase 12.

## Fora Do Escopo

- implementar codigo;
- alterar roadmap;
- criar ADR;
- criar migration;
- tocar em tasks concluidas de fases anteriores.

## Passos

1. Ler documentos obrigatorios.
2. Auditar `HeartRateDetailScreen`, `HealthMetricDetailScreens.kt`, `HealthViewModel.kt`, `ChampionConnectionManager.kt`, `ChampionTransportSession.kt`, `ChampionNotificationHub.kt`, `ChampionPacketRouter.kt`, `DeviceConnectionBootstrap.kt`, `ForegroundBleOperationGate`, `HeartRateAndSync.kt`, `RepositoryContracts.kt`, `ChampionDatabase.kt`, `ChampionDaos.kt`, `DatabaseMappers.kt`, `RoomRepositories.kt` e bindings Hilt relevantes.
3. Confirmar no protocolo D1 os campos autorizados, timeout, criterio de conclusao e lacunas conhecidas.
4. Registrar explicitamente fronteira correta entre Fase 09 e Fase 12.
5. Atualizar spec e overview com achados factuais, sem reescrever escopo aprovado.

## Criterios De Aceite

- estado real do codigo pos-Fase 08 registrado com nomes reais de arquivos e contratos;
- contrato D1 real e lacunas conhecidas registrados sem inventar bytes;
- fronteira correta Health Connect registrada;
- nenhum codigo alterado.

## Testes Pequenos Aplicaveis

- nenhum nesta task documental.

## Validacao Minima

- revisao de diff documental;
- `git diff -- docs/specs/009-watch-heart-rate-history-d1.md docs/tasks/009-watch-heart-rate-history-d1/`.

## Evidencias

- lista de arquivos lidos e achados registrada no proprio arquivo;
- referencias cruzadas para protocolo D1, baseline e estado real do codigo.

## Arquivos Lidos

- `docs/specs/_inputs/phase-09-watch-heart-rate-history-direction.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-009-health-metrics-hub-navigation.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/protocol/history-d1.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/handshake-a1.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/baseline/current-baseline.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/ChampionManualHeartRateCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionPacketRouter.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/InMemoryForegroundBleOperationGate.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`

## Achados

### Protocolo D1 confirmado

- comando real confirmado continua `D1 01 00 00` em `F008_WRITE` com resposta via `F008_NOTIFY`;
- pacote minimo confirmado continua 20 bytes;
- offsets confirmados continuam: `0` comando `0xD1`, `1-2` `packetIndex` little-endian, `3-4` `totalPackets` little-endian, `5` hora, `14` BPM, `19` minuto;
- offsets `6-13` e `15-18` continuam sem semantica confirmada; auditoria nao encontrou evidencia para data, timezone, segundos, checksum ou bytes de controle adicionais;
- criterio de conclusao confirmado continua `totalPackets > 0` e `packetIndex >= totalPackets`, sem exigencia de contiguidade total;
- timeout foreground confirmado continua 15 segundos;
- payload D1 continua sem ano, mes, dia ou timezone proprios; timestamp completo depende do contexto da tentativa no aparelho.

### Estado real herdado da Fase 08

- `HealthMetricDetailScreens.kt` ainda entrega somente fluxo manual em `HeartRateDetailScreen`; grafico segue placeholder, lista segue `Histórico manual recente` e `Buscar dados do relógio` aparece apenas como texto futuro, sem CTA funcional;
- `HealthViewModel.kt` ainda modela somente state machine da medicao manual, ultimo BPM geral, recentes manuais, peso, passos e distancia; nao existe estado para importacao D1, periodo, agregacoes, grafico, ultima importacao D1 ou cancelamento/retry D1;
- `ChampionNavigation.kt` e `HeartRateDetailScreen` ainda protegem back/background somente para medicao manual em voo;
- `ChampionConnectionManager.kt`, `ChampionNotificationHub.kt`, `ChampionTransportSession.kt` e `ChampionPacketRouter.kt` ja oferecem sessao reutilizavel, subscriptions multiplexadas e observacao por canal, mas auditoria nao encontrou parser D1, collector multipart ou coordinator D1 de produto;
- `DeviceConnectionBootstrap.kt` ja garante handshake A1 confirmado ou assumido sobre `F008_NOTIFY`, reutilizavel por fluxo futuro D1;
- `RepositoryContracts.kt` e `InMemoryForegroundBleOperationGate.kt` ainda expõem apenas owners `DAILY_STEPS_REFRESH` e `MANUAL_HEART_RATE`; `WATCH_D1_HISTORY` ainda nao existe no gate;
- `HeartRateAndSync.kt` ja contem `HeartRateSource.WATCH_D1_HISTORY`, `D1RecordKey` e `HeartRateDeduplicationKey.D1History`; dominio ja marca D1 valido com `measuredAt` como elegivel para sync futuro;
- `ChampionDatabase.kt` v2 ja possui `heart_rate_measurements.deduplicationKey` unico e `health_sync_operations` unico por `(aggregateId, recordType)`, mas ainda nao possui storage dedicado para metadata de tentativa/importacao D1;
- `ChampionDaos.kt` ja possui `findValidPendingForSync()` e `observeLatestValidMeasurement()`, mas consulta historico da tela ainda e manual-only por `observeRecentValidManualMeasurements(limit)`; nao ha query por periodo nem historico consolidado manual + D1;
- `RoomRepositories.kt` ainda usa `import(measurements)` generico: insere por `deduplicationKey` e cria `HealthSyncOperationEntity(state = "PENDING")` apenas quando medicao D1 elegivel e nova; auditoria nao encontrou contagens tipadas de lote, conflito explicito, reparo de `PENDING` ausente para D1 ja existente, metadata D1 especifica ou queries/agregacoes por periodo;
- `DatabaseMappers.kt` codifica `HeartRateDeduplicationKey.D1History` em string persistida, mas isso ainda nao resolve politica de conflito quando mesma chave reaparece com BPM divergente;
- `HomeViewModel.kt`, `DeviceManagementViewModel.kt` e `DeviceScreen.kt` continuam usando `Device.importMetadata.lastImportedAt` como metadado generico de resumo/importacao do dispositivo; esse campo nao pode ser reinterpretado como "ultima importacao D1".

### Fronteira correta com Health Connect

- documento de direcao da Fase 09 prevalece sobre trecho antigo do roadmap: Fase 09 e local-first;
- Fase 09 deve importar D1, deduplicar, persistir localmente, garantir fila `PENDING`, expor historico/grafico/agregacoes e registrar metadata D1 propria;
- Fase 09 nao deve implementar `HealthConnectClient`, disponibilidade do provider, permissao, mapeamento `HeartRateRecord`, escrita real, update/delete ou transicoes `SYNCING`/`SYNCED`;
- Fase 12 permanece owner da integracao real com Health Connect;
- regra atual de persistencia ja confirma base local-first: falha futura de provider nao deve desfazer dado local.

### Riscos e lacunas registradas para proximas tasks

- falta parser D1 unico e reutilizavel;
- falta collector multipart com timeout, lacunas, cancelamento e callback tardio ignorado;
- falta owner `WATCH_D1_HISTORY` no gate foreground;
- falta metadata D1 especifica por dispositivo/tipo de importacao;
- falta query consolidada manual + D1 por periodo e agregacoes locais;
- falta politica documentada e implementada para conflito de mesma chave D1 com BPM divergente;
- falta reparo idempotente de `HealthSyncOperation.PENDING` ausente para D1 ja persistido;
- roadmap segue divergente e continua precisando correcao em task posterior autorizada.

## Atualizacao Do Overview

- marcar `009-01` como concluida;
- atualizar riscos e dependencias descobertas;
- confirmar que `009-02` depende de `009-01`.

## Validacao Final

- `git status --short` deve mostrar apenas alteracoes documentais desta task.

Validacao executada nesta task:

- diff documental revisado nos arquivos permitidos;
- nenhum arquivo `.kt`, roadmap, ADR ou schema foi alterado.
