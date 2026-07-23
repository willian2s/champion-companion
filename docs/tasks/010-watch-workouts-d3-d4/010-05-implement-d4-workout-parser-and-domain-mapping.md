# Task 010-05 - Implementar parser D4 e mapeamento de domínio

## Status

Pendente.

## Objetivo

Interpretar pacotes D4 ordenados e produzir saída tipada para `VALID`, `PARTIAL` ou `INVALID`, mapeando métricas, intervalos e amostras para o domínio atual.

## Contexto

Domínio e Room já possuem `WorkoutActivity`, `WatchWorkoutImport`, `WorkoutInterval` e `WorkoutHeartRateSample`, mas ainda não existe parser D4 nem regra explícita para timestamps, buckets de minuto, divergência de `recordCount` e CRC divergente.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-03-implement-d3-index-protocol-and-collector.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-04-implement-d4-block-collector-and-remounting.md`
- `docs/protocol/workouts-d3-d4.md`

## Dependências

- `010-03`
- `010-04`

## Entradas

- pacotes D4 ordenados;
- layouts preferido e legado auditados;
- regras de duração, `minuteIndex`, timestamp, passos, distância, calorias e BPM.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutParser.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutImportSync.kt`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-05-implement-d4-workout-parser-and-domain-mapping.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutParserTest.kt`

## Arquivos proibidos

- `core/database/**`
- `data/**`
- `feature/**`
- `DeviceManagementViewModel.kt`

## Escopo

- pacotes obrigatórios 1, 2 e 3;
- amostras 4+;
- layout preferido e legado;
- timestamp e duração;
- intervalos, passos, distância, calorias e BPM;
- agregados BPM;
- sugestão de tipo;
- `VALID`, `PARTIAL`, `INVALID`.

## Fora do escopo

- persistência Room;
- coordinator BLE;
- UI.

## Passos

1. Criar parser D4 puro e testável.
2. Mapear saída para `WatchWorkoutParserInput` e objetos de domínio.
3. Implementar regras de timestamp, duração, buckets, agregados e validade.
4. Tratar CRC divergente, `recordCount` divergente e amostras inválidas.
5. Atualizar spec e overview com decisões finais de parser.

## Critérios de aceite

- pacotes 1/2/3 obrigatórios respeitados;
- layout preferido e legado preservados;
- `minuteIndex` e `sampledAt` seguem baseline auditada;
- duração total e buckets de minuto não se contradizem por heurística nova;
- `PARTIAL` e `INVALID` são produzidos corretamente sem inventar dados.

## Testes pequenos aplicáveis

- layout preferido;
- layout legado;
- calorias por mil;
- distância raw para metros;
- BPM válido/inválido;
- agregados BPM;
- sugestão de tipo;
- `PARTIAL` por duplicado, ausente, timeout ou `recordCount` divergente;
- `INVALID` por pacote obrigatório ausente ou data inválida.

## Validação mínima

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest --tests "*ChampionD4WorkoutParserTest"`

## Evidências

- testes pequenos do parser;
- diff restrito a parser e domínio relacionado.

## Atualização do overview

- marcar `010-05` como concluída;
- registrar qualquer ajuste de paralelismo ou novo risco de conflito.

## Validação final

- confirmar que BLE real, Room e UI final continuaram fora desta task.
