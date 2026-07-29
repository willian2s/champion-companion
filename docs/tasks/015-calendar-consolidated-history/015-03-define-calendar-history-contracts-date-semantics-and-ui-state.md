# 015-03 — Definir contratos, datas e estado temporal

- **Status:** concluída em 27/07/2026.
- **Objetivo:** estabilizar modelos e contratos antes de tocar Room ou UI.
- **Resultado esperado:** modelos imutáveis, APIs, estados e intents prontos para
  implementação independente.

## Documentos obrigatórios

Spec 015, 015-01/02, `RepositoryContracts.kt`, `DomainTime.kt`, modelos de
DailySummary/BPM/atividade/peso/sync, contratos de Saúde e Exercícios, ADRs
relevantes e baseline de banco.

## Dependências e entradas

- 015-02 concluída.
- Contratos atuais e intervalo inclusivo de resumo auditados.

## Arquivos permitidos

`core/model` para modelos/contratos; `feature/calendar/CalendarFeatureContract.kt`
para estado e intents; documentação da task e overview.

## Arquivos proibidos

DAOs, entities, mappers, `RoomRepositories`, `RepositoryModule`, telas Compose,
navegação, migrations, testes, BLE e Health Connect.

## Escopo

Definir `CalendarRepository`, queries de mês/dia/histórico, snapshots, estados
de ausência/loading/erro, semântica `[fromInclusive, untilExclusive)`, ZoneId,
seleção de dispositivo, resumos de BPM/atividade/peso/sync e separação entre
ausência e zero. APIs antigas devem permanecer compatíveis.

## Fora do escopo

Implementar consultas, composição de Flows, UI, navegação, gráficos ou alterar
dados persistidos.

## Passos

1. Modelar entradas e saídas sem entities.
2. Definir invariantes e estados seguros.
3. Definir datas por aggregate.
4. Definir janelas de 7/30/90 dias.
5. Documentar comportamento sem dados e falha parcial.
6. Atualizar evidências desta task.

## Critérios de aceite

- `observeMonth(query)` e `observeDay(localDate, zoneId)` retornam modelos.
- Contratos de histórico suportam 7, 30 e 90 dias.
- Semântica de data é exata e testável manualmente por inspeção.
- BPM exclui amostras de atividades.
- Passos/distância não recebem atividades.
- Sync é derivado da fila local.
- `CalendarDayPresence` cobre passos, BPM, atividades, peso e pendências/falhas.
- `CalendarDaySummary` cobre resumo, meta, BPM, atividades, peso e sync.
- Datas seguem `key.localDate`, `measuredAt`, `startedAt` e `syncedAt` proibido
  como data do aggregate.
- Estado inclui `selectedMonth`, `selectedDate`, snapshots, loadings e erro.

## Validação manual

Revisar modelos contra spec e símbolos existentes. Screenshot: não aplicável;
task de domínio/contrato.

## Comandos de build

Durante execução futura: `./gradlew lint assembleDebug assembleRelease` e
`./gradlew installDebug`. Não executar testes.

## Evidências e atualização

Registrar arquivos e símbolos alterados, decisões de compatibilidade e riscos.
Atualizar item correspondente do overview após aceite.

## Validação final

Confirmar ausência de entity na UI, ausência de operação externa e nenhum schema
novo. Não criar, alterar ou executar testes automatizados.

## Contexto

Contratos atuais não possuem CalendarRepository, read model diário ou histórico
temporal de atividades e pesos exposto pela camada de domínio.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/CalendarHistory.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarFeatureContract.kt`;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`;
- este arquivo.

## Decisões e desvios

- O contrato inclusivo existente de `DailySummaryRepository.observeSummaries`
  foi preservado. A API semiaberta foi adicionada como
  `observeSummariesBetween`, adaptada ao contrato antigo sem alterar consumidores.
- APIs temporais novas em BPM, atividades, peso e fila possuem implementações
  padrão vazias para preservar implementações e fakes existentes; 015-04 deve
  substituí-las por projeções Room em lote.
- `CalendarHistoryQuery` usa fim exclusivo explícito e deriva somente janelas de
  7, 30 ou 90 dias. Queries de mês e dia derivam início do período seguinte, sem
  fim de dia artificial.
- Snapshots mensais exigem presenças ordenadas, únicas e dentro do mês; snapshots
  históricos exigem uma entrada por data da janela, em ordem, e derivam agregados
  das entradas para preservar ausência, zero e contagens coerentes.
- Estado de calendário rejeita seleção fora do mês e snapshots incompatíveis com
  mês, data ou `ZoneId`; somas históricas rejeitam overflow numérico.
- Ausência usa tipos anuláveis, listas vazias e `NO_OPERATIONS`; zero persistido
  permanece valor real. Falhas parciais usam `CalendarDataIssue`, sem mensagem,
  payload ou identificador sensível.
- `CalendarSyncSummary.state` implementa a precedência exata da fila local.
  Modelos temporais não usam `syncedAt` como data.
- Atividades expõem distância somente no card/projeção e não expõem passos;
  totais históricos permanecem exclusivos de `DailySummary`.
- Nenhum DAO, entity, mapper, repository persistente, módulo DI, Compose,
  navegação, migration ou teste foi alterado.

## Comandos executados

- `git status --short && git diff --stat && git diff && git log --oneline -10`
- `git diff --check`
- `./gradlew :app:compileDebugKotlin :app:lintDebug`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew lint assembleDebug assembleRelease`

Não foram criados, alterados ou executados testes automatizados, conforme regra da
spec e desta subtarefa.

## Resultados e evidências

- Compilação Kotlin debug e lint debug concluíram com `BUILD SUCCESSFUL` em
  1 minuto e 52 segundos; 31 tasks, 13 executadas e 18 `UP-TO-DATE`.
- Recompilação final concluiu com `BUILD SUCCESSFUL` em 18 segundos; 7 tasks,
  2 executadas e 5 `UP-TO-DATE`.
- `lint assembleDebug assembleRelease` concluiu com `BUILD SUCCESSFUL`; lint,
  APKs debug/release e compilação Kotlin foram validados sem testes automatizados.
- `CalendarRepository` expõe `observeMonth`, `observeDay`, históricos de passos
  e distância.
- Read models cobrem presença mensal, detalhe diário, BPM manual/D1, atividades,
  pesos, fila, falha parcial e históricos 7/30/90.
- `CalendarUiState` cobre seleção, zona congelada, snapshots, loadings e erro
  seguro; `CalendarIntent` contém os oito intents especificados.
- Revisão independente final aprovou escopo, invariantes e critérios de aceite;
  `git diff --check` concluiu sem erros.

## Screenshot

Não aplicável; task de domínio e contrato.

## Riscos residuais

- Implementações padrão das novas APIs temporais retornam vazio até 015-04
  adicionar queries e projeções persistentes.
- Seleção determinística entre dispositivos, agrupamento por timezone e
  associação em lote da fila continuam para 015-05.
- ViewModel, telas e navegação continuam ausentes, conforme escopo.

## Atualização do overview

Item `015-03` marcado `[x]`; progresso recalculado para `3/13`. Demais itens
permanecem `[ ]` e nenhuma subtarefa seguinte foi iniciada.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
