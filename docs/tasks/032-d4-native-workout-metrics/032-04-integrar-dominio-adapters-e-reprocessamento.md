# 032-04 — Integrar domínio, adapters e reprocessamento

- **Status:** pending
- **Depende de:** 032-02 e 032-03

## Objetivo

Conectar parser e persistência novos ao domínio sem mudar ainda a política final
de distância/calibração.

## Escopo

- definir contratos measured/source para samples v4;
- adaptar persistência/importação para novos fatos;
- manter adapters explícitos de v1/v2/v3;
- definir reprocessamento local explícito a partir de payload D4 quando elegível;
- preservar workflow, review, IDs e snapshots antigos;
- atividade `SYNCED` permanece fail-closed para mudança silenciosa.

## Arquivos prováveis

- `core/model/WatchWorkoutActivity.kt`
- `core/model/WatchWorkoutDerivedMetrics.kt`
- `data/DatabaseMappers.kt`
- coordinator/parser contracts necessários

## Fora do escopo

- fator contextual final;
- UI da calibração;
- promoção do runtime default antes de 032-07.

## Validação

`./gradlew lint assembleDebug assembleRelease` e `git diff --check`.
