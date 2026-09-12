# 033-06 — Integrar Health Connect granular e cutover

- **Status:** pending
- **Depende de:** 033-04 e 033-05

## Objetivo

Projetar dados diários granulares D1/D4 sem coexistir com total diário FEA1 no
mesmo dia e remover auto-sync remoto do watch sync.

## Escopo

- persistir data de cutover local;
- preservar datas/operações pré-cutover;
- pós-cutover bloquear novas projeções STEPS de `DailySummary`;
- preparar records granulares de D1 fora de workout e D4 dentro de workout;
- IDs estáveis, versões monotônicas e payload congelado;
- steps/distance/heart-rate conforme elegibilidade;
- preservar ledgers/credits legacy para histórico;
- remover chamadas automáticas a `syncPending()` de Home/D1;
- manter `Settings -> Sincronizar agora` e sync explícito de workout.

## Calorias

Somente implementar record Health Connect de calorias se a semântica for fechada
com evidência suficiente. Caso contrário, manter local e documentar `deferred`.

## Guard rails

- sem backfill granular histórico;
- sem delete remoto amplo;
- sem reabrir `SYNCED` silenciosamente;
- sem leitura Health Connect.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```
