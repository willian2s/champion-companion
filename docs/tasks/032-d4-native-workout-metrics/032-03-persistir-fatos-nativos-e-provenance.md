# 032-03 — Persistir fatos nativos e provenance

- **Status:** pending
- **Depende de:** 032-01
- **Pode rodar em paralelo com:** 032-02

## Objetivo

Preparar schema/domínio persistente para fatos v4 sem reinterpretar dados legacy.

## Escopo

- auditar Room v15 real e confirmar necessidade de `v15 -> v16`;
- adicionar campos/tabelas necessários para `sportValue`, passos nativos,
  distância nativa, calorias raw e pausa;
- adicionar provenance suficiente para distinguir v1/v2/v3/v4;
- manter campos legacy intactos;
- atualizar mappers e schema exportado;
- migration estritamente aditiva e sem reprocessamento.

## Arquivos prioritários

- `core/database/ChampionDatabase.kt`
- `core/database/ChampionMigrations.kt`
- `data/DatabaseMappers.kt`
- schema Room

## Evitar nesta task

- `RoomRepositories.kt`, salvo bloqueio técnico inevitável;
- `ExercisesScreen.kt`;
- mudança da regra de distância.

## Validação

`./gradlew lint assembleDebug assembleRelease` e `git diff --check`.
