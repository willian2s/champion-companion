# 035-03 — Integrar leitura limitada de workouts

- **Ticker:** `035`
- **Número:** `03`
- **Status inicial:** `pending`
- **Status atual:** `completed`

## Objetivo e resultado esperado

Disponibilizar ao domínio uma única fonte reativa e bounded de atividades
completas `WATCH_D3_D4` nos últimos 56 dias, incluindo samples necessários à
policy 034, sem N+1 reativo e sem alterar schema.

## Requisitos cobertos

- R1 e R8 da Spec 035.
- Limite `[asOf - 56 dias, asOf)`.
- Filtros persistentes de origem, descarte e `startedAt`.
- Preservação de todos os estados operacionais elegíveis.
- ViewModel sem acesso direto ao DAO.

## Escopo incluído

- Contrato bounded aditivo em `WorkoutActivityRepository`.
- Query temporal específica no DAO para atividades watch não descartadas.
- Hidratação completa de atividade e samples por fluxo agregado, preferindo
  leituras filhas batched.
- Mapeamento fail-closed para registros inválidos, sem converter em carga.
- Reatividade a mudanças locais relevantes dentro do range.
- Verificação de plano/custo da query sem adicionar índice especulativo.

## Escopo excluído

- Migration, tabela, coluna, snapshot ou índice novo.
- Alteração de `observeActivities(ALL)`, calendário ou read model diário.
- Novo parser, BLE, importação, derivação ou Health Connect.
- `flatMapLatest` por activity no ViewModel.
- Carregamento de histórico sem limite.

## Dependências

- `035-01` concluída.
- Contratos de domínio de `035-02`.
- `WorkoutActivityRepository`, `RoomRepositories`, `ChampionDaos` e mappers.

## Arquivos e símbolos prováveis

- `core/model/RepositoryContracts.kt` — contrato bounded;
- `core/database/ChampionDaos.kt` — query temporal e consultas batched;
- `data/RoomRepositories.kt` — fluxo, hidratação e `loadWorkoutActivity`;
- `data/DatabaseMappers.kt` — records e reconstrução;
- `WorkoutActivityEntity`, `WorkoutHeartRateSampleEntity`;
- `CalendarInstantRange` ou contrato específico bounded.

## Passos de implementação

1. Definir operação de leitura específica, mantendo summaries de calendário
   separados.
2. Adicionar query com `origin = 'WATCH_D3_D4'`,
   `workflowState != 'DISCARDED'` e
   `startedAtEpochMillis >= from AND startedAtEpochMillis < until`.
3. Garantir ordenação determinística por `startedAt` e ID.
4. Planejar/implementar leitura agrupada de imports, intervalos, samples,
   headers e snapshots ativos para os IDs bounded.
5. Reusar mappers existentes e preservar `WorkoutActivity` completo.
6. Expor um único `Flow<List<WorkoutActivity>>` para o histórico.
7. Tratar falha de fonte como estado seguro; não emitir zero sintético.
8. Conferir que o caminho não observa atividades fora dos 56 dias.
9. Inspecionar plano/custo; registrar N/A ou risco se não houver dados de
   escala, sem criar migration automaticamente.

## Testes e comandos de validação

- Verificar limites inclusivo/exclusivo com `startedAt` exatamente nas bordas.
- Verificar exclusão de `DISCARDED` e origens não watch.
- Verificar inclusão de todos os outros workflows.
- Verificar samples completos para a policy 034.
- Verificar ausência de N+1 reativo por activity.
- Inspecionar schema e confirmar Room v20 sem migration nova.
- Executar `./gradlew lint assembleDebug assembleRelease` após implementação.
- Não criar testes automatizados novos.

## Definição de pronto

- Repository entrega exatamente população bounded até 56 dias.
- Policy recebe atividades completas com samples.
- ViewModel não acessa DAO.
- Calendário e lista global permanecem sem mudança de semântica.
- Falhas de hidratação não viram carga zero.
- Nenhum arquivo de schema/migration foi criado ou alterado.

## Riscos e cuidados

- Query atual de summaries não filtra origem; não reutilizar sem filtro novo.
- Índice de activity começa por `activityId`; não presumir índice temporal global
  de samples.
- Evitar emissão intermediária por activity e recomposição em cascata.
- Não usar `endedAt` para definir pertencimento analítico.
- Não registrar samples, BPM ou payloads em logs.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt` — contrato aditivo de leitura completa bounded.
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt` — queries reativas temporais para população e filhos.
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt` — hidratação agregada, reconstrução completa e falha fechada.
- Este arquivo — status, decisões, validação e riscos.
- `docs/tasks/035-training-load-history-trends/035-00-overview.md` — checklist e progresso.

## Decisões e desvios

- Decisão: adicionar `observeWatchActivitiesBetween(CalendarInstantRange)` sem alterar contratos globais ou summaries de calendário.
- Decisão: aplicar filtros de origem, descarte, `startedAt` semiaberto e ordenação `startedAtEpochMillis ASC, id ASC` no DAO.
- Decisão: usar flows bounded somente como gatilhos de invalidação e reidratar todos os fatos em uma única `withReadTransaction` por emissão; não há Flow nem consulta por activity.
- Decisão: rejeitar `CalendarInstantRange` superior a 56 dias no repository, preservando o limite máximo mesmo com chamador incorreto.
- Decisão: reusar mappers e derivação ativa existentes, incluindo intervalos derivados, splits, header e samples.
- Decisão: registros inválidos lançam falha segura de mapeamento; não são omitidos como lista vazia nem convertidos em carga.
- Desvio: nenhum. Nenhum índice, migration, schema, entidade, parser, BLE ou Health Connect foi alterado.

## Comandos executados

- `git diff --check`
- `./gradlew app:compileDebugKotlin`
- `./gradlew app:lintDebug`
- `./gradlew assembleDebug assembleRelease`
- `./gradlew lint assembleDebug assembleRelease`
- `EXPLAIN QUERY PLAN` em SQLite in-memory para a query bounded principal.

## Resultados e evidências

- `git diff --check`: passou.
- `app:compileDebugKotlin`: `BUILD SUCCESSFUL`.
- `app:lintDebug`: `BUILD SUCCESSFUL`.
- `assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- Após tornar a reidratação transacional, `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- A primeira execução combinada falhou em `:app:lintAnalyzeDebugAndroidTest` por exceção interna do Lint ao analisar `ExampleInstrumentedTest.kt`; não apontou erro nos arquivos da task. A repetição de `./gradlew lint assembleDebug assembleRelease` passou com `BUILD SUCCESSFUL`.
- Plano bounded reproduzido sem índice temporal composto: `SCAN activity` e `USE TEMP B-TREE FOR ORDER BY`. Não há dataset físico representativo para medir escala; risco registrado sem criar índice especulativo.
- Inspeção dos arquivos alterados confirma ausência de schema/migration e Room v20 preservado.
- Revisão independente identificou risco de `mapNotNull` mascarar falha; correção aplicada para propagar `BoundedWatchWorkoutMappingException` em mapeamento inválido.
- Não foram criados nem executados testes automatizados novos, conforme escopo da Spec 035 e `AGENTS.md`.

## Riscos residuais

- A query principal não possui índice temporal composto e o plano usa scan e ordenação temporária; desempenho em escala real permanece não medido.
- O repository rejeita ranges superiores a 56 dias; a composição da 035 ainda deve fornecer `[asOf - 56 dias, asOf)` para manter semântica temporal correta.
- Não houve validação física com workouts nas bordas, estados operacionais, samples ou registros inválidos; isso permanece para as subtarefas posteriores.
