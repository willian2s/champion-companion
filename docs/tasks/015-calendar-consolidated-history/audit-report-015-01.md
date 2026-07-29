# Relatório de auditoria 015-01

## Escopo e método

Auditoria somente documental e de código existente, executada em 27/07/2026.
Nenhum arquivo de runtime foi alterado. A comparação `main...HEAD` foi usada
para confirmar que a branch atual contém as entregas da Fase 12, mas ainda não
contém implementação de calendário.

### Fontes documentais consultadas

- `AGENTS.md`;
- `docs/baseline/current-baseline.md`;
- `docs/baseline/database-reference.md`;
- `docs/baseline/feature-parity-checklist.md`;
- `docs/baseline/gaps-and-open-questions.md`;
- `docs/baseline/health-connect-mapping.md`;
- `docs/specs/015-calendar-consolidated-history.md`;
- `docs/specs/_inputs/phase-15-calendar-consolidated-history-direction.md`;
- `docs/roadmap/champion-companion-roadmap.md`;
- todas as ADRs em `docs/decisions/`, especialmente ADRs 003, 005, 009,
  010, 011 e 012;
- `docs/specs/007-daily-summary-home.md` até
  `docs/specs/011-activity-review-calibration.md`;
- overviews `007-00`, `008-00`, `009-00`, `010-00` e `011-00`;
- `docs/specs/012-native-health-connect.md` e
  `docs/specs/_inputs/phase-12-native-health-connect-direction.md`;
- `docs/tasks/012-native-health-connect/012-00-overview.md` e tasks
  `012-01` a `012-14`;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md` e
  `015-02-formalize-local-calendar-read-model-adr-and-roadmap.md`.

## Estado das fases e fronteiras

### Fase 12

Fase 12 está declarada concluída na baseline, spec, fechamento do overview e
roadmap, mas o cabeçalho do overview ainda está atrasado:

- `docs/baseline/current-baseline.md:24-49` registra SDK 1.1.0, fila granular,
  reconciliação de backlog, gatilhos foreground, sync explícito de atividades,
  Settings e feedback secundário;
- `docs/specs/012-native-health-connect.md:3-5` declara conclusão;
- `docs/tasks/012-native-health-connect/012-00-overview.md:3-5` ainda informa
  `Planejada`, enquanto `:232-270` marca todas as tasks e descreve Gate C e
  `:297-301` registra fechamento;
- `docs/roadmap/champion-companion-roadmap.md:1624-1626` registra fechamento em
  24/07/2026.

O Health Connect permanece write-only. Room e repositories locais são a fonte
operacional e visual. O provider não participa da formação das telas da Fase 15.

Resíduo documental confirmado: `012-13-run-physical-health-connect-gate-c.md`
mantém `Status: Planejada` em `:1-6`, embora o fechamento do overview, spec,
`012-14` e roadmap tratem Gate C e Fase 12 como concluídos. O roteiro de
`012-13:69-115` também não contém checklist executado, resultados por cenário,
screenshots ou evidência reproduzível de falha parcial, retry, permissões e
recuperação de processo. A Fase 12 pode ser tratada como concluída para o
planejamento da Fase 15, mas os dois status textuais e a evidência incompleta
permanecem riscos documentais.

### Fases 13 e 14

As fontes da Fase 15 registram, sem renumerar fases:

- Fase 13 — adiada: exercícios iniciados pelo aplicativo;
- Fase 14 — adiada: background, WorkManager e foreground service;
- Fase 15 — próxima fase.

Evidências: `docs/specs/015-calendar-consolidated-history.md:35-45`,
`docs/specs/_inputs/phase-15-calendar-consolidated-history-direction.md:23-54`
e `docs/tasks/015-calendar-consolidated-history/015-00-overview.md:7-18`.

O roadmap ainda apresenta Fases 13 e 14 como seções funcionais, sem marcador de
adiamento, em `docs/roadmap/champion-companion-roadmap.md:1842-1984`. ADR-012
adianta mecanismos de background da Fase 14, mas não formaliza o adiamento da
Fase 13. Atualização planejada para 015-02; não executada nesta subtarefa por
ser arquivo proibido.

## Fontes locais e contratos reais

### Resumo diário

- Domínio: `core/model/DeviceProfileDailySummary.kt:109-113,171-199`,
  `DailySummaryKey` e `DailySummary`.
- Entity: `core/database/ChampionDatabase.kt:105-130`; Room está na versão 5
  em `core/database/ChampionDatabase.kt:439-463`.
- Query: `core/database/ChampionDaos.kt:101-118`,
  `DailySummaryDao.observeBetween` usa intervalo `BETWEEN` inclusivo.
- Repository e merge: `data/RoomRepositories.kt:232-287`.
- Mapper: `data/DatabaseMappers.kt:228-241`.

A chave real preserva `deviceId`, `origin` e `localDateIso`, além de passos,
distância/calorias estimadas, validade e timestamps. Ainda não existe seleção
da fonte válida do dispositivo associado nem fallback determinístico por última
atualização. `DailySummary.key.localDate` é a semântica correta futura; não se
deve reprojetar `collectedAt` para decidir o dia.

### BPM

- Domínio e sync: `core/model/HeartRateAndSync.kt:7-13,53-175`.
- Fontes do histórico: `HeartRateHistoryQuery` em
  `core/model/HeartRateAndSync.kt:255-262`, somente
  `MANUAL_MEASUREMENT` e `WATCH_D1_HISTORY`.
- Query Room: `core/database/ChampionDaos.kt:141-146`.
- Janela e agregação: `data/RoomRepositories.kt:1267-1324`.

O histórico já usa `[fromInclusive, untilExclusive)` e exclui amostras de BPM
de atividades. A tela e o ViewModel ainda obtêm `ZoneId.systemDefault()` em
`feature/health/HealthViewModel.kt:105-110` e a UI reagrupa com a zona padrão em
`feature/health/HealthMetricDetailScreens.kt:709-714`; a zona ainda não é
congelada por snapshot temporal.

### Atividades

- Domínio: `core/model/WatchWorkoutActivity.kt:125+`.
- Entity e DAO: `core/database/ChampionDatabase.kt:132-183` e
  `core/database/ChampionDaos.kt:153-212`.
- Contract: `core/model/RepositoryContracts.kt:201-228`.
- Repository e mapper: `data/RoomRepositories.kt:469-549` e
  `data/DatabaseMappers.kt:283-350`.
- Lista, workflow e detalhe reutilizável:
  `feature/exercises/ExercisesScreen.kt:107-197,669-965`.

O contrato atual observa atividades por filtro, sem intervalo temporal. Não há
`observeActivitiesBetween`. Atividades `DISCARDED` devem ser excluídas da futura
projeção; `PENDING_REVIEW`, `PENDING_CALIBRATION` e `READY_TO_SYNC` continuam
visíveis. A data é `startedAt` no `ZoneId` selecionado, inclusive quando a
atividade cruza meia-noite; `syncedAt` não pode mover o registro.

### Peso

- Domínio: `core/model/DeviceProfileDailySummary.kt:65-87`.
- Entity e query: `core/database/ChampionDatabase.kt:86-103` e
  `core/database/ChampionDaos.kt:75-88`.
- Contract atual: `core/model/RepositoryContracts.kt:59-63`.
- Repository: `data/RoomRepositories.kt:676-688`.

O contract expõe snapshot/lookup, não intervalo. O snapshot atual usa somente o
último peso (`data/RoomRepositories.kt:102-119`). Falta consulta em lote para
pesos do período; a futura associação deve usar `measuredAt` convertido pela
zona selecionada, ordenando medidas do dia da mais recente para a mais antiga.

### Fila Health Connect

- Domínio: `core/model/HeartRateAndSync.kt:177-237,418-534`.
- Entity: `core/database/ChampionDatabase.kt:388-412`.
- DAO: `core/database/ChampionDaos.kt:311-350`.
- Contract: `core/model/RepositoryContracts.kt:295-322`.
- Implementação e reconciliação: `data/RoomRepositories.kt:711-800,
  936-1024,1027-1169`.

`HealthSyncOperation` guarda aggregate, estado, erro seguro, identidade do
Health Connect e timestamps. É fonte local de status de sync, nunca prova de
validade do aggregate, origem de dado clínico ou substituto do aggregate. O dia
futuro deve vir do aggregate (`localDate`, `measuredAt`, `startedAt`), nunca de
`syncedAt`. O contrato atual busca operações por aggregate individual; não há
consulta em lote por tipo e IDs, o que cria risco de N+1.

## Tempo, dispositivos e ausência

### Timezone

`DailySummary` permanece em `key.localDate`. BPM, atividade e peso possuem
timestamps instantâneos e precisarão ser projetados no `ZoneId` explícito da
consulta. Hoje existem `LocalDate.now()` e `ZoneId.systemDefault()` espalhados
em Saúde/Home. `DailySummaryEntity` possui `collectedZoneId`, mas
`DatabaseMappers.kt:228-231` não o preenche. Uma mudança extrema de timezone
pode deslocar BPM, peso ou atividade para data civil adjacente sem alterar
registros persistidos.

Todas as consultas novas devem usar `[fromInclusive, untilExclusive)`, com
início do mês/dia até início do período seguinte. Não usar fim artificial
`23:59:59.999`.

### Múltiplos dispositivos

Associação operacional é singular (`id = 'primary'`) em
`core/database/ChampionDaos.kt:49-61`, mas históricos preservam vários
`deviceId`. `DailySummaryDao.observeBetween` não filtra validade, dispositivo
associado ou origem; `DailySummaryRepository.observeSummaryForDate` usa o
primeiro resultado. Não há desempate determinístico.

Política necessária para próxima implementação:

1. resumo `VALID` do dispositivo associado;
2. se ausente, resumo `VALID` mais recentemente atualizado;
3. nunca somar resumos completos de dispositivos diferentes;
4. exibir origem quando vier de dispositivo histórico.

Ausência de resumo deve permanecer diferente de resumo válido com zero passos.
Passos e distância diária vêm exclusivamente de `DailySummary`; métricas de
atividades não entram nos totais.

## Calendário, navegação e shells atuais

- `feature/calendar/CalendarScreen.kt:3-7` chama somente `PlaceholderScreen`.
- `core/navigation/AppDestination.kt:39-43` possui `Calendar`, mas não possui
  `CalendarDay(localDateIso)` nem validação por `LocalDate.parse`.
- `core/navigation/ChampionNavigation.kt:447` renderiza `CalendarScreen()` sem
  callbacks. O back stack manual usa `navigateForward`,
  `navigateToTopLevel` e `removeLastOrNull` em `:121-139,469-487`.
- `ExerciseDetail` e o fluxo de Exercícios já são reutilizáveis em
  `ChampionNavigation.kt:448-460`; o calendário deve abrir essa superfície sem
  duplicar tela e preservar o dia no back stack.
- `feature/health/HealthMetricDetailScreens.kt:272-309` mantém
  `StepsDetailScreen` como resumo atual, meta e progresso, com histórico/gráfico
  placeholder.
- `feature/health/HealthMetricDetailScreens.kt:311-348` mantém
  `DistanceDetailScreen` como distância estimada atual, com histórico/gráfico
  placeholder.
- `HealthViewModel.kt:196-214` usa `dailyStepsGoal`; não existe meta histórica.

Não existem `CalendarRepository`, `CalendarViewModel`, read models temporais ou
destino de dia. Fase 15 ainda não adicionou runtime no branch nem em `main`.

## Hotspots e N+1

Hotspots confirmados: `RepositoryContracts.kt`, `ChampionDaos.kt`,
`DatabaseMappers.kt`, `RoomRepositories.kt`, `RepositoryModule.kt`,
`AppDestination.kt`, `ChampionNavigation.kt`, `HealthMetricDetailScreens.kt`,
`HealthViewModel.kt` e `CalendarScreen.kt`.

Riscos de N+1:

1. `RoomRepositories.kt:519-524,866-873` carrega lista de atividades e depois
   detalhes, intervalos e amostras por item; aproximadamente três consultas por
   atividade.
2. `RepositoryContracts.kt:316` e `RoomRepositories.kt:733-741` consultam
   `HealthSyncOperation` por aggregate individual.
3. Reconciliação em `RoomRepositories.kt:936-1001` resolve operações e metadata
   individualmente para resumos, BPM e peso.
4. A reconciliação de backlog usa `findWeights(profile.id)` em
   `ChampionDaos.kt:87-88` e garante operação individual por medida em
   `RoomRepositories.kt:986-1001`; o snapshot visual usa consulta separada de
   último peso. Essa diferença não deve ser confundida com uma query temporal
   em lote para o calendário.

A implementação futura deve usar projeções leves de atividade e consulta em
lote da fila; não adicionar índice preventivo ou tabela materializada sem
evidência própria.

## Plano documental futuro, sem execução

- 015-02 deve criar ADR-013 e atualizar roadmap com Fase 12 concluída, Fases
  13/14 adiadas e Fase 15 próxima.
- 015-03 deve estabilizar contratos, read models, timezone, intervalos,
  ausência/zero e estado de UI sem tocar DAO ou runtime visual.
- 015-04/015-05 devem adicionar queries/projeções e composição local em lote,
  preservando contratos antigos e Room v5 inicialmente.
- 015-06 em diante deve implementar ViewModels, calendário, detalhe e shells,
  mantendo ausência de BLE, Health Connect, operações de sync e migrations.

## Conclusão e riscos residuais

Critérios de aceite de 015-01 atendidos: caminhos e símbolos reais foram
citados; todas as fontes solicitadas foram cobertas; calendário foi confirmado
como placeholder; fila foi classificada somente como fonte de status; Fases 13 e
14 foram identificadas como adiadas; nenhuma funcionalidade foi implementada.

Riscos residuais:

- status textual conflitante de `012-13`;
- Gate C declarado, mas sem evidência documental reproduzível suficiente;
- roadmap ainda não marca adiamento de Fases 13/14;
- seleção de múltiplos dispositivos, intervalos de atividades/pesos e consulta
  agrupada da fila ainda não existem;
- timezone ainda depende de `systemDefault()` em superfícies atuais;
- N+1 de atividades e sync exige desenho em lote nas subtarefas seguintes.

Nenhum risco bloqueia 015-02, mas todos devem permanecer explícitos até as
decisões e implementações correspondentes.
