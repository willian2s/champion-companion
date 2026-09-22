# 036-01 — Auditar zonas históricas e congelar contrato

- **Ticker:** `036`
- **Número:** `01`
- **Status inicial:** `pending`

## Objetivo e resultado esperado

Confirmar no código atual que a análise individual da Spec 034, a fonte bounded
da Spec 035, as janelas, a página cardiovascular, as cores e o renderer gráfico
suportam a 036. Congelar contrato de agregação, estados, denominadores,
parcialidade e fallback visual antes de implementação.

## Requisitos cobertos

- R1 a R10 da Spec 036, especialmente fonte única, população, janelas,
  invariantes de fechamento e decisão stacked/fallback.
- ADR-034 e ADR-035.
- Preservação da correção 035-08.

## Escopo incluído

- Revisar `WorkoutHeartRateAnalysisPolicy` e todos os estados individuais.
- Revisar `CardiovascularLoadWorkoutInput` e
  `observeCardiovascularLoadWorkoutsBetween()`.
- Revisar `HistoryWindows.from(asOf)` e determinar se extração compartilhada é
  necessária; preferir não extrair se a extensão ficar na mesma policy.
- Revisar `ExercisesViewModel`, `ExercisesScreen` e
  `CardiovascularLoadHistoryScreen`.
- Revisar `ChampionBarChart`, `ChampionCartesianChartData`, Vico e tema de
  gráfico.
- Revisar ordem, labels, cores e semantics já usados nas zonas da Spec 034.
- Congelar `zones`/coleção equivalente, nulidade, denominador e semântica de
  bucket sem alterar código.

## Escopo excluído

- Alteração de código funcional, Room, repository, DAO, UI ou renderer.
- Nova query, nova policy, migration, schema, teste automatizado ou fixture.
- Criação de workout/sample.

## Dependências

- `main` em `91bbaca` com 034/035 e 035-08.
- Spec/ADR 034 e 035 e todas as tasks 035 concluídas.
- Código de `core/model`, `data`, `core/database` e `feature/exercises`.

## Arquivos e símbolos prováveis

- `core/model/WatchWorkoutHeartRateAnalysis.kt`:
  `HeartRateZone`, `HeartRateZoneSummary`, `WorkoutHeartRateAnalysisPolicy`.
- `core/model/WorkoutHeartRateLoadHistory.kt`:
  `CardiovascularLoadHistoryPolicy`, `HistoryWindows`, resumos e buckets.
- `core/model/RepositoryContracts.kt`:
  `observeCardiovascularLoadWorkoutsBetween`.
- `data/RoomRepositories.kt`, `data/DatabaseMappers.kt` e
  `core/database/ChampionDaos.kt`.
- `feature/exercises/ExercisesViewModel.kt`, `ExercisesScreen.kt` e
  `CardiovascularLoadHistoryScreen.kt`.
- `core/designsystem/chart/ChampionChartContracts.kt`, `ChampionCharts.kt` e
  `ChampionChartTheme.kt`.

## Passos de implementação

1. Registrar branch/base e confirmar branch de trabalho
   `feat/036-heart-rate-zone-history` criada a partir da `main` auditada.
2. Confirmar que a fonte bounded contém todos os fatos da 036 e não depende de
   derivação, snapshots, splits, headers ou distância.
3. Confirmar uma única chamada da policy 034 por workout na composição atual.
4. Comparar a lógica privada de janelas com a semântica documentada da 035.
5. Confirmar que `ChampionBarChart` é grouped e não stacked.
6. Congelar extensão aditiva do read model, incluindo seis zonas, zonas zero,
   nulidade semântica e percentuais sobre duração analisada.
7. Registrar riscos e cenários físicos que poderão ser N/A sem fabricar dados.

## Testes e comandos de validação

- `git diff --check` para documentação.
- Inspeção de símbolos e referências, sem build obrigatório nesta subtarefa.
- Não criar nem executar testes automatizados.

## Definição de pronto

- Contrato escrito e coerente com 034/035.
- Fonte bounded aprovada sem alteração.
- Janelas compartilhadas sem segunda implementação.
- Decisão de UI e gráfico congelada.
- Riscos, nulidade, denominadores e estados documentados.

## Riscos e cuidados

- Não relaxar o read model mínimo que corrigiu a regressão Release.
- Não tratar seis séries grouped como stacked.
- Não criar paleta nova nem permitir classificação visual de esforço.
- Não expor BPM, samples, IDs ou payloads na documentação.
