# Task 009-06 - Implementar consultas de historico, agregacoes e politica do grafico

## Status

Concluida em 2026-07-23.

## Objetivo

Implementar consultas locais por periodo, historico consolidado manual + D1, agregacoes e serie de dados necessaria para o grafico funcional.

## Contexto

Depois de `009-05`, persistencia D1 e metadata especifica ja devem existir. Ainda faltara superficie de leitura eficiente para `Hoje`, `7 dias` e `30 dias`, sem carregar banco inteiro e sem depender de Health Connect.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-05-implement-d1-persistence-deduplication-and-pending-queue.md`
- `docs/decisions/adr-009-health-metrics-hub-navigation.md`

## Dependencias

- `009-05`

## Entradas

- schema e contratos D1 estabilizados por `009-05`;
- periodos locais `Hoje`, `7 dias` e `30 dias`;
- regra de consolidar apenas `MANUAL_MEASUREMENT` e `WATCH_D1_HISTORY`.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-06-implement-heart-rate-history-queries-aggregations-and-chart-policy.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/data/RoomRepositoriesHeartRateHistoryQueriesTest.kt`

## Arquivos Proibidos

- `core/bluetooth/**`
- `feature/device/**`
- `feature/home/**`
- `feature/health/HealthMetricDetailScreens.kt`
- `feature/health/HealthViewModel.kt`

## Escopo

- query observavel consolidada por periodo e origem;
- query observavel de metadata D1;
- calculo de `quantidade`, `media`, `minimo`, `maximo`;
- serie do grafico para `Hoje`, `7 dias` e `30 dias`;
- buckets diarios para periodos maiores;
- nenhum agregado persistido.

## Fora Do Escopo

- BLE;
- UI Compose;
- gate;
- Health Connect real.

## Passos

1. Evoluir `RepositoryContracts.kt` e `RoomRepositories.kt` com APIs observaveis de historico, metadata e agregacoes.
2. Adicionar queries e projections em `ChampionDaos.kt` para periodo local e buckets diarios.
3. Consolidar apenas fontes `MANUAL_MEASUREMENT` e `WATCH_D1_HISTORY` validas.
4. Implementar politicas de agregacao e serie do grafico sem dados sinteticos.
5. Adicionar testes pequenos de limites de periodo, agregacoes e buckets diarios.
6. Atualizar spec e overview com nomes reais das APIs.

## Criterios De Aceite

- `Hoje`, `7 dias` e `30 dias` funcionam sem BLE;
- historico consolidado usa so manual + D1;
- agregacoes retornam somente valores validos;
- serie de `Hoje` usa pontos reais;
- `7 dias` e `30 dias` usam buckets diarios;
- nenhum agregado e persistido.

## Testes Pequenos Aplicaveis

- limites `Hoje`, `7 dias` e `30 dias`;
- consolidacao manual + D1;
- media, minimo, maximo e quantidade;
- bucket diario sem dados sinteticos.

## Validacao Minima

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest` focado nas queries e agregacoes.

## Evidencias

- resultados dos testes pequenos;
- diff de DAO/repository mostrando consultas locais e buckets diarios.

## Implementacao Executada

- `HeartRateDao` ganhou `observeValidHistoryBetween(...)` para janela local e fontes consolidadas.
- `RoomRepositories.observeHistory(...)` agora consulta apenas periodo necessario, consolida manual + D1 e monta `HeartRateHistorySnapshot` completo.
- helper de janela local usa `[fromInclusive, untilExclusive)` para `TODAY`, `LAST_7_DAYS` e `LAST_30_DAYS`.
- agregacoes e serie do grafico sao calculadas em memoria; `TODAY` usa pontos reais e periodos maiores usam buckets diarios sem sintetizar dias vazios.
- `RoomRepositoriesHeartRateHistoryQueriesTest.kt` cobre limites de periodo, consolidacao, agregacoes e buckets diarios.

## Atualizacao Do Overview

- marcar `009-06` como concluida;
- registrar APIs finais de historico/agregacao;
- liberar `009-08` e `009-09`.

## Validacao Final

- confirmar que nenhuma UI, gate ou operacao BLE foi implementada nesta task.
