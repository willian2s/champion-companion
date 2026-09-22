# 035-08 — Corrigir leitura histórica no Release

- **Ticker:** `035`
- **Número:** `08`
- **Status inicial:** `pending`
- **Status atual:** `completed`

## Motivo do corretivo

No build Release, a página **Histórico de carga cardiovascular** mostra
`Carga cardiovascular indisponível`, embora a análise individual da Spec 034
funcione nos mesmos workouts. A validação anterior da `035-06` exercitou o
banco Debug separado e não cobriu a compatibilidade histórica reproduzida no
banco físico Release.

## Objetivo

Corrigir somente a leitura/agregação bounded usada pela Spec 035, preservando
fatos persistidos e mantendo a análise individual da Spec 034 inalterada.

## Escopo e critérios de aceite

- Reproduzir com banco Release, de forma sanitizada, o estágio que converte a
  leitura bounded em `Unavailable`.
- Separar read model cardiovascular dos requisitos de reconstrução de distância,
  derivação, intervalos derivados e splits.
- Consultar somente workouts `WATCH_D3_D4` não descartados na janela máxima de
  56 dias e samples correspondentes, sem N+1 e sem acesso DAO no ViewModel.
- Preservar `startedAt`, duração, workflow, tipo confirmado/sugerido e samples;
  manter FC máxima atual como segunda fonte da composição.
- Manter `WorkoutHeartRateAnalysisPolicy` como única fonte de análise individual.
- Garantir precedência histórica: falha estrutural da fonte gera `UNAVAILABLE`;
  resultado individual `Unavailable` torna período parcial quando houver
  resultado `AVAILABLE`, sem derrubar população válida; carga `0.0` disponível
  continua contando.
- Não modificar `WorkoutActivity`, snapshots, intervals, splits, Health Connect,
  calibrações, parser, BLE ou dados físicos; manter Room v20 sem migration.
- Validar população, análises, quatro janelas, buckets, tipos, cobertura e
  invariantes com banco Release somente para inspeção.
- Tentar validação física instalando por cima do Release existente, sem limpar,
  desinstalar, reimportar ou rederivar; registrar `N/A` se ambiente impedir.

## Dependências

- `035-02`, `035-03`, `035-04`, `035-06` e `035-07` concluídas.
- `WorkoutHeartRateAnalysisPolicy` e contratos de `WorkoutActivity`.
- Banco exportado em `champion-release-db/`, somente leitura.

## Arquivos previstos

- `core/model/WorkoutHeartRateLoadHistory.kt` — input mínimo e precedência da
  policy.
- `core/model/RepositoryContracts.kt` — contrato bounded cardiovascular.
- `core/database/ChampionDaos.kt` — leitura bounded de fatos necessários.
- `data/DatabaseMappers.kt` — hidratação mínima de input cardiovascular.
- `data/RoomRepositories.kt` — Flow agregado sem derivação.
- `feature/exercises/ExercisesViewModel.kt` — consumo do novo read model.
- este arquivo e `035-00-overview.md` — evidências e status.

## Validação obrigatória

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar testes automatizados novos sem autorização explícita.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WorkoutHeartRateLoadHistory.kt` — input mínimo e precedência da policy.
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt` — contrato bounded cardiovascular.
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt` — mapper mínimo de atividade e samples.
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt` — Flow bounded batched sem derivação.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt` — consumo do read model.
- `docs/specs/035-training-load-history-trends.md` — contrato corrigido e evidência da regressão.
- `docs/decisions/adr-035-training-load-history-trends.md` — decisão do read model mínimo.
- este arquivo e `035-00-overview.md` — status e evidências.
- `app/build.gradle.kts` — Release permanece `isDebuggable = false`; arquivo não
  faz parte da correção funcional.

## Decisões e desvios

- Planejado read model mínimo em vez de relaxar invariantes globais de
  `WorkoutActivity` ou `WorkoutDerivationSnapshot`.
- Decisão: reutilizar queries bounded existentes para atividade e samples, sem
  adicionar migration, índice, DAO novo ou tabela analítica.
- Decisão: falhar fechado somente quando fatos mínimos cardiovasculares não
  puderem ser mapeados; snapshots, intervals, derived intervals e splits ficam
  fora da composição.
- Decisão: `WorkoutHeartRateAnalysisResult.Unavailable` individual contribui
  para `partial`, mas não transforma resumo em `UNAVAILABLE` se houver resultado
  `AVAILABLE`.
- Desvio: o banco Release físico continha histórico além da evidência original
  da `035-06`; a validação anterior exercitou banco Debug separado e não foi
  reescrita.

## Comandos executados

- `git diff --check`
- `./gradlew lint assembleDebug assembleRelease`
- consultas SQLite read-only no fixture `champion-release-db/champion-companion.db`
- cálculo independente sanitizado de status, população e cobertura
- `adb install -r app/build/outputs/apk/release/app-release.apk`
- smoke físico Release: Exercícios → CTA → Histórico cardiovascular
- comparação sanitizada de tabelas antes/depois no banco físico

## Resultados e evidências

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL` na segunda
  execução; a primeira excedeu timeout sem erro de compilação reportado.
- Fixture Release: Room `user_version = 20`; 42 workouts watch não descartados,
  1.308 samples, 42 snapshots ativos e FC máxima configurada.
- Análise independente dos fatos necessários à FC: 42 resultados `AVAILABLE`,
  duas cargas `0,0` legítimas e nenhuma análise individual `UNAVAILABLE`.
- Reprodução lógica do caminho antigo: oito snapshots legados tinham contagem
  de splits persistidos incompatível com a contagem esperada pelo
  `WorkoutSplitCalculator`; isso fazia `mapActiveDerivation()` retornar `null`,
  o guard lançar `BoundedWatchWorkoutMappingException` e o ViewModel emitir
  `Unavailable` global antes da policy 034.
- Depois da correção, o read model não consulta imports, intervals, headers,
  snapshots, derived intervals ou splits; permanece limitado a 56 dias e sem
  N+1 reativo.
- Validação física Release por instalação incremental: página histórica abriu
  e exibiu estado calculado, com 3/3 treinos analisados em current7 e 22/22 em
  current28; nenhuma limpeza, desinstalação, reimportação ou rederivação foi
  executada.
- Hashes sanitizados e contagens confirmaram que `workout_activities`,
  `workout_heart_rate_samples`, `workout_derivation_snapshots`,
  `workout_derived_intervals` e `workout_splits` permaneceram iguais antes e
  depois da instalação/smoke.
- O detalhe individual de 18/09 foi reaberto no mesmo app Release e exibiu os
  fatos de duração, distância e frequência cardíaca; não foi feita comparação
  numérica independente da carga/zonas nesta rodada. O contrato e a policy
  individual não foram alterados.
- Após normalizar o artefato Release para `isDebuggable = false`, nova instalação
  incremental confirmou Exercícios, CTA e página histórica calculada; a UI não
  mostrou `Carga cardiovascular indisponível`.

## Riscos residuais

- A inicialização física do app criou sete operações `HEART_RATE` pendentes no
  banco existente durante o smoke, embora nenhum arquivo de Health Connect ou
  caminho de escrita tenha sido alterado pela correção. Por isso a ausência de
  qualquer mutação de `health_sync_operations` não pode ser declarada nesta
  rodada; investigar separadamente antes de usar essa instalação como prova
  isolada de efeitos colaterais.
- Não surgiu naturalmente workout individual `UNAVAILABLE`, workout sem
  samples ou configuração ausente; esses cenários permanecem N/A sem fixture
  artificial.
- Plano bounded continua sem índice temporal composto; risco de custo em escala
  permanece o mesmo da `035-03`.
