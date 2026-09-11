# 032-04 — Integrar domínio, adapters e reprocessamento

- **Status:** completed
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

## Execução

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutParser.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`

### Decisões e desvios

- `MeasuredWorkoutIntervalSource` separa adapters legacy v1/v2/v3 do adapter
  nativo v4. O adapter v4 preserva `sportValue`, passos, distância, calorias e
  pausa sem fabricar `rawMotionValue`, `rawDistanceValue` ou `rawCaloriesValue`.
- Mappers e repositórios aceitam fatos v4 após reload, reidratam o header
  persistido e falham fechado quando header ou fatos obrigatórios estão ausentes.
  O caminho de candidatos do Health Connect também propaga o header.
- Reprocessamento permanece local e explícito, reutiliza payload D4 preservado
  quando necessário e mantém normalização temporal `T/N`. A derivação de
  distância continua usando política existente por comprimento de passo; a
  promoção da distância nativa permanece para 032-05.
- Reimportação diferente de atividade `SYNCED` retorna `Conflict` antes de
  qualquer escrita. Comparação inclui proveniência, janela, métricas, samples,
  fatos nativos e header v4.
- Compatibilidade v1 mantém decodificação transitória coerente para derivação,
  sem reinterpretar packing legacy. O coordinator continua com runtime `d4-v3`.

### Comandos executados

- `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin`
- `./gradlew lint assembleDebug assembleRelease`
- `git diff --check`

### Resultados e evidências

- Compilação Kotlin debug/release: passou.
- `lint`, `assembleDebug` e `assembleRelease`: passaram.
- `git diff --check`: passou.
- Nenhum teste automatizado foi criado ou executado, conforme AGENTS.md.
- Revisão independente foi solicitada; riscos encontrados foram corrigidos antes
  da validação final: rollback antes de writes para `SYNCED`, comparação ampliada,
  compatibilidade v1 e header v4 obrigatório.

### Riscos residuais

- A distância nativa ainda não é fonte canônica; derivação e calibração nativa
  pertencem às subtarefas 032-05 e 032-06.
- Não há cobertura automatizada nova para round-trip v4 ou reprocessamento, pois
  criação/execução de testes não foi autorizada.
- Validação física e confirmação de variação de intensidade permanecem em 032-08.
