# 015-14 — Incluir frequência cardíaca de atividades no histórico dedicado

- **Status:** concluída em 2026-07-28.
- **Objetivo:** incluir amostras reais de frequência cardíaca de atividades
  importadas não descartadas na tela dedicada de Saúde.
- **Resultado esperado:** gráfico, lista e agregações do período consolidam
  medições manuais, histórico D1 e amostras persistidas de atividades do relógio.

## Documentos obrigatórios

Spec 015, overview 015, ADRs 010 e 013, baseline atual,
`HeartRateAndSync.kt`, `WatchWorkoutActivity.kt`, `RepositoryContracts.kt`,
`ChampionDaos.kt`, `RoomRepositories.kt`, `HealthViewModel.kt`,
`HealthMetricDetailScreens.kt`, contratos e testes existentes relacionados a
histórico de frequência cardíaca e importação de atividades.

## Dependências e entradas

- Histórico dedicado de frequência cardíaca manual e D1 já funcional.
- Atividades D3/D4 e amostras reais já persistidas em
  `workout_heart_rate_samples`.
- `HeartRateSource.WATCH_WORKOUT_ACTIVITY` já existente no domínio e na
  rotulagem da UI.
- 015-07 concluída; esta subtarefa é independente de 015-08, 015-09 e 015-10.

## Arquivos permitidos

Contratos/read models mínimos em `core/model`, query Room em `core/database`,
composição em `data`, apresentação mínima em `feature/health`, spec/ADR/baseline
e documentação desta subtarefa/overview.

## Arquivos proibidos

Entidades, schema, migration, escrita em `heart_rate_measurements`, importadores
BLE, Health Connect, navegação, workflows de atividade, arquivos das subtarefas
015-08 a 015-13, testes automatizados, roadmap e dependências.

## Escopo

1. Evoluir o read model do histórico para identificar uma medição persistida ou
   uma amostra de atividade sem fabricar `HeartRateMeasurementId`.
2. Observar amostras em `workout_heart_rate_samples` dentro da janela semiaberta,
   associadas à respectiva atividade importada não `DISCARDED`.
3. Garantir reatividade a importação/reimportação, substituição das amostras e
   mudança de workflow para `DISCARDED`.
4. Combinar amostras reais com `MANUAL_MEASUREMENT` e `WATCH_D1_HISTORY` no
   snapshot, ordenação, gráfico e agregações do período.
5. Incluir `WATCH_WORKOUT_ACTIVITY` nos filtros permitidos do histórico dedicado.
6. Mostrar origem e contexto seguro da atividade na lista, com fallback legível
   quando título ou tipo confirmado estiver ausente.

## Fora do escopo

Copiar ou deduplicar amostras na tabela `heart_rate_measurements`, sintetizar BPM,
alterar última medição da Home/Resumo atual, importar dados, iniciar BLE/D1/D3/D4,
ler ou escrever Health Connect, alterar descarte/workflow, incluir atividade
descartada, implementar detalhe diário 015-08 ou criar histórico de outros dados.

## Critérios de aceite

- Snapshot do histórico dedicado usa somente BPM real persistido e válido.
- Fontes permitidas são `MANUAL_MEASUREMENT`, `WATCH_D1_HISTORY` e
  `WATCH_WORKOUT_ACTIVITY`; `MEASUREMENT_STREAM` e `APP_WORKOUT_SESSION`
  permanecem excluídas.
- Amostra de atividade é identificada pelo agregado de atividade, sem fingir ser
  `HeartRateMeasurement` e sem criar linha duplicada.
- Query usa `sampledAtEpochMillis >= from` e `< until`, associa atividade
  existente e exclui `workflowState = 'DISCARDED'`.
- Importação/reimportação e descarte invalidam o Flow e atualizam a tela sem ação
  externa.
- Gráfico, contagem, média, mínimo, máximo e lista incluem as três fontes.
- Lista exibe `Atividade do relógio` e contexto seguro de título/tipo; ausência de
  metadado usa fallback, sem expor identificadores.
- Fontes manual e D1 mantêm comportamento e identidade existentes.
- Resumo atual continua baseado em `heart_rate_measurements`; amostras de
  atividade não mudam essa política.
- Nenhum schema, migration, workflow, BLE, Health Connect, teste ou dependência é
  alterado.

## Validação manual

Abrir frequência cardíaca com atividade importada contendo amostras reais,
alternar Hoje/7 dias/30 dias e conferir gráfico, agregações e lista. Descartar
atividade elegível em fluxo já existente e confirmar remoção reativa do histórico.
Confirmar manual/D1 preservados e ausência de operações externas. Mudança visual
exige screenshot quando validação em dispositivo estiver disponível.

## Comandos de build

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar, alterar ou executar testes automatizados.

## Evidências e atualização

Registrar status, arquivos alterados, decisões/desvios, comandos, resultados,
evidências e riscos residuais. Atualizar somente item 015-14 no overview após
aceite.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HeartRateHistoryChart.kt`
- `docs/baseline/current-baseline.md`
- `docs/decisions/adr-013-local-calendar-temporal-read-model.md`
- `docs/specs/015-calendar-consolidated-history.md`
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`
- `docs/tasks/015-calendar-consolidated-history/015-14-include-workout-heart-rate-in-dedicated-history.md`
- `docs/tasks/015-calendar-consolidated-history/015-14-workout-heart-rate-history.png`

## Decisões e desvios

- Read model deve preservar identidade do agregado de origem.
- Amostras permanecem na tabela própria; consolidação ocorre somente na leitura.
- Query Room reativa une amostras persistidas a atividades importadas não
  descartadas; não houve alteração de entidade, schema ou migration.
- `HeartRateHistoryEntry` passou a distinguir medição persistida de amostra de
  atividade, sem fabricar `HeartRateMeasurementId`.
- Histórico dedicado inclui atividade; resumo atual e read model diário do
  calendário mantêm políticas anteriores.
- Desvio: `ADR-013` e baseline foram alinhadas para registrar a fonte adicional;
  nenhuma subtarefa de calendário diário foi executada.

## Riscos residuais

- Atividades com muitas amostras podem aumentar custo do snapshot; não adicionar
  índice ou paginação sem evidência e decisão separada.
- Nenhum risco funcional residual identificado na validação manual; custo de
  snapshots com muitas amostras permanece risco técnico monitorável.

## Comandos executados e resultados

- `git diff --check`: sucesso.
- `./gradlew :app:compileDebugKotlin`: sucesso.
- `./gradlew lint assembleDebug assembleRelease`: sucesso.
- `./gradlew installDebug`: sucesso no Galaxy S22.
- Validação manual: concluída; usuário confirmou funcionamento esperado em
  Hoje/7 dias/30 dias, gráfico, agregações, lista e descarte reativo.
- Testes automatizados: não criados, alterados ou executados, conforme
  `AGENTS.md` e spec.

## Evidências

- `015-14-workout-heart-rate-history.png` registra amostras de atividade na
  lista da página dedicada.
- Fluxo de leitura preserva manual/D1, agrega atividade real no gráfico,
  agregações e lista, e exclui atividades descartadas por query reativa.
- Validação manual concluída em dispositivo; usuário confirmou que tudo
  funciona como esperado.

## Atualização do overview

Item 015-14 foi marcado `[x]` após conclusão e validação manual. Itens 015-08 a
015-13 permanecem inalterados e pendentes.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
executar 015-08 nem avançar para outra subtarefa.
