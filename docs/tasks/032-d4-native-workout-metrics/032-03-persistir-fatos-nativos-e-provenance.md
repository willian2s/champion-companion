# 032-03 — Persistir fatos nativos e provenance

- **Status:** completed
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

## Execução

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/16.json`

### Decisões e desvios

- Room foi confirmado em v15 antes da mudança; `MIGRATION_15_16` é aditiva e cria
  tabela própria para header D4 v4, sem reinterpretar dados legacy.
- Fatos nativos por intervalo usam campos semanticamente nomeados e separados de
  `rawMotionValue`, `rawDistanceValue`, `rawCaloriesValue` e `decodedSteps`.
- Header preserva totais, pausas, CRC, tipo, bytes reservados, provenance e flags
  de consistência. `parserVersion` continua distinguindo v1/v2/v3/v4.
- `d4-v4` foi aceito no domínio e nos mappers para permitir persistência, mas o
  coordenador de importação continua selecionando `d4-v3`; promoção de runtime
  permanece nas subtarefas posteriores.
- `RoomRepositories.kt` foi alterado somente pelo bloqueio técnico inevitável de
  inserir/atualizar fatos v4 e remover header obsoleto em reimportação legacy.
- Nenhuma regra de distância, calibração, UI, Health Connect ou reprocessamento foi
  ativada nesta subtarefa.

### Comandos executados

- `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin app:lintDebug`
- `./gradlew lint assembleDebug assembleRelease`
- `./gradlew lint assembleDebug assembleRelease && git diff --check`
- `git diff --check`

### Resultados e evidências

- Compilação Kotlin debug/release: passou.
- `lint`, `assembleDebug` e `assembleRelease`: passaram.
- Schema Room `16.json` foi exportado com versão 16, tabela de header, campos
  nativos, foreign keys e índices únicos.
- `git diff --check`: passou.
- Revisão independente final: aprovada, sem achados bloqueadores, altos, médios ou
  baixos.
- Nenhum teste automatizado foi criado ou executado, conforme AGENTS.md.

### Riscos residuais

- Header v4 ainda não é reidratado no domínio após reload; leitura integrada do
  header pertence à 032-04. Dados permanecem persistidos e consultáveis pelo DAO.
- Não há teste de upgrade real v15→v16 nem round-trip de persistência v4; validação
  física e promoção do runtime v4 pertencem às subtarefas posteriores.
