# Task 008-02 - Definir contratos, state machine e gate da medicao manual

## Status

Planejada.

## Objetivo

Definir contratos minimos de dominio e feature para medicao manual, incluindo estado explicito da sessao, intents, erro seguro, relogio monotonic e gate compartilhado de operacao BLE foreground.

## Contexto

Fase 08 precisa de owner unico de estado e de ownership BLE de produto. Sem contrato explicito, risco alto de booleans contraditorios, medicao automatica acidental ou concorrencia com refresh diario.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-01-audit-poc-heart-rate-domain-and-health-feature.md`
- ADR-007
- ADR-008

## Dependencias

- `008-01`

## Entradas

- `feature/health/HealthFeatureContract.kt`
- `feature/home/HomeFeatureContract.kt`
- `core/model/RepositoryContracts.kt`
- `core/model/DomainTime.kt`

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DomainTime.kt`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-02-define-manual-heart-rate-contracts-state-and-operation-gate.md`

## Arquivos Proibidos

- `HealthScreen.kt`
- `HomeScreen.kt`
- `RoomRepositories.kt`
- `ChampionDaos.kt`
- `ChampionDatabase.kt`
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Criar `HealthUiState` real.
- Criar intents minimos `Load`, `StartMeasurement`, `CancelMeasurement`, `Retry`, `DismissError`, `ConfirmExitMeasurement`, `OpenDevice`.
- Modelar estados `Idle`, `Preparing`, `Connecting`, `Starting`, `Measuring`, `Stopping`, `Completed`, `Cancelled`, `Failed`.
- Modelar erro seguro.
- Definir contratos de `HeartRateRepository` e `ForegroundBleOperationGate`.
- Definir contrato de `MonotonicClock` ou equivalente testavel.

## Fora Do Escopo

- Implementar coordinator.
- Implementar persistencia.
- Implementar UI.

## Passos

1. Definir estado composto sem booleans contraditorios.
2. Definir dados de sessao: `manualMeasurementSessionId`, `bleSessionId`, `startedAt`, `progress`, `remaining`, `currentBpm`, `currentSource`, `signalQuality`, `finalResult`, `error`.
3. Definir categoria do gate: `DAILY_STEPS_REFRESH`, `MANUAL_HEART_RATE`.
4. Definir erro seguro para conflito de operacao BLE.
5. Definir contratos repository e clock.
6. Atualizar spec e overview.

## Criterios De Aceite

- Contratos compilaveis e minimos.
- State machine coerente com spec.
- Gate descrito de forma executavel.
- Nenhum DAO/Room alterado nesta task.

## Testes Pequenos Aplicaveis

- Se houver helper puro de state machine ou gate, pequeno teste JVM pode acompanhar.

## Validacao Minima

```bash
./gradlew assembleDebug
```

## Evidencia Manual

- Resumo do contrato criado e decisoes de nome.

## Atualizacao Do Overview

- Marcar `008-02` como concluida.
- Ajustar dependencias ou paralelismo se nomes finais mudarem.

## Validacao Final

- Confirmar que nao existe regra de auto medicao introduzida no contrato.
