# Task 009-01 - Auditar protocolo D1, historico e fronteira Health Connect

## Status

Pendente.

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

## Atualizacao Do Overview

- marcar `009-01` como concluida;
- atualizar riscos e dependencias descobertas;
- confirmar que `009-02` depende de `009-01`.

## Validacao Final

- `git status --short` deve mostrar apenas alteracoes documentais desta task.
