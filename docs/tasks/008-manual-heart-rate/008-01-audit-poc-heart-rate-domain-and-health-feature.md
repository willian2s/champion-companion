# Task 008-01 - Auditar PoC de frequencia cardiaca, dominio e feature Saude

## Status

Planejada.

## Objetivo

Auditar estado real da PoC D0/F002, dominio de frequencia cardiaca, MotionLog, feature Saude, Home e ownership BLE antes de qualquer implementacao funcional da Fase 08.

## Contexto

Fase 08 depende de componentes tecnicos existentes da PoC, de contratos de persistencia ja presentes e de restricoes fortes: medicao so por acao explicita, persistencia somente do resultado final e ausencia de Health Connect.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/specs/_inputs/phase-08-manual-heart-rate-direction.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/protocol/heart-rate-d0-f002.md`

## Dependencias

- Nenhuma task anterior da Fase 08.

## Entradas

- Codigo atual em `feature/health`, `feature/home`, `feature/device`, `core/bluetooth`, `core/model`, `core/database`, `data`.
- MotionLog apenas como referencia funcional.

## Arquivos Exatos Permitidos

- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-01-audit-poc-heart-rate-domain-and-health-feature.md`

## Arquivos Proibidos

- Qualquer `.kt`
- `AndroidManifest.xml`
- `app/build.gradle.kts`
- `settings.gradle.kts`
- `app/schemas/**`

## Escopo

- Confirmar pontos de extensao e riscos.
- Auditar MotionLog para regra real do resultado final e da suposta media.
- Confirmar se schema atual atende sem migration.
- Atualizar spec e overview com achados factuais.

## Fora Do Escopo

- Implementar codigo.
- Criar ADR.
- Criar testes.

## Passos

1. Ler docs obrigatorios.
2. Inspecionar `HealthScreen`, `HomeScreen`, `HomeViewModel`, `DeviceConnectionBootstrap`, `DeviceManagementViewModel`, `DevicePocCoordinator`, `DevicePocHeartRateSession`, `DevicePocF002BpmEstimator`, `ChampionConnectionManager`, `ChampionTransportSession`, `ChampionNotificationHub`, `ChampionPacketRouter`, `HeartRateAndSync.kt`, `RepositoryContracts.kt`, `ChampionDatabase.kt`, `ChampionDaos.kt`, `DatabaseMappers.kt`, `RoomRepositories.kt` e bindings Hilt relacionados.
3. Confirmar se MotionLog usa media ou somente `D0 -> F002` no fluxo manual da Saude.
4. Confirmar se `HeartRateMeasurement` atual suporta medicao manual final sem migration.
5. Registrar riscos de ownership BLE e conflito com Home refresh.
6. Atualizar status da task e checklist do overview.

## Criterios De Aceite

- Achados registrados com origem.
- Regra da media auditada explicitamente.
- Necessidade ou ausencia de migration registrada.
- Nenhum codigo alterado.

## Testes Pequenos Aplicaveis

- Nenhum nesta task documental.

## Validacao Minima

- Revisao documental.
- `git diff -- docs/specs/008-manual-heart-rate.md docs/tasks/008-manual-heart-rate/`.

## Evidencia Manual

- Lista de arquivos lidos e achados registrada no proprio arquivo e no overview.

## Atualizacao Do Overview

- Marcar `008-01` como concluida.
- Atualizar riscos de conflito e achados de auditoria.

## Validacao Final

- `git status --short` para confirmar alteracoes apenas documentais.
