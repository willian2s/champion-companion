# 032-07 — Integrar produto, Health Connect e baseline pós-031

- **Status:** completed
- **Depende de:** 032-05 e 032-06

## Objetivo

Promover `d4-v4` ao fluxo efetivo e integrar repository, UI e Health Connect sobre
a baseline visual já estabilizada e incorporada pela Spec 031.

## Pré-condição

**Satisfeita em 2026-09-10.** A branch 032 já foi atualizada sobre uma `main` que
contém a Spec 031. A reauditoria `032-01` confirmou `behind_by = 0` e merge-base
`6025fbaaf119727fbb95c2f51e6f16946ef646ea`.

Referência:

`docs/integration/032-01-post-031-reaudit.md`.

Não realizar novo merge específico da 031 nesta task; trabalhar sobre a baseline
pós-031 já presente e preservar seus read models, navegação e desenho visual.

## Escopo

- integrar `RoomRepositories.kt` e persistência transacional;
- tornar `d4-v4` parser efetivo para novas importações;
- encaixar novos estados/labels de distância e calibração em Exercícios sem
  reverter o redesign visual;
- preservar review-before-sync;
- preservar a participação de `WATCH_WORKOUT_ACTIVITY` nos históricos de Saúde;
- atualizar projeções congeladas de DISTANCE/SPEED e consumidores relacionados;
- preservar STEPS/STEPS_CADENCE e créditos da Spec 027;
- atualizar baseline/documentação autoritativa.

## Arquivos compartilhados relevantes

- `data/RoomRepositories.kt`;
- `feature/exercises/ExercisesScreen.kt`;
- contratos/read models de Saúde/Calendário que consomem métricas canônicas e FC de workout.

Esses arquivos não são mais pontos de conflito entre branches 031/032. Qualquer
mudança deve preservar semanticamente o baseline pós-031 e manter offsets/packing
D4 confinados às camadas de protocolo/adapters apropriadas.

## Validação

`./gradlew lint assembleDebug assembleRelease` e `git diff --check`.

## Execução

### Arquivos alterados nesta subtarefa

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ChampionWatchWorkoutImportCoordinator.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectSyncCoordinator.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`.

### Decisões e desvios

- Novas importações passaram a selecionar `d4-v4` somente no coordinator; versões
  `d4-v1/v2/v3` permanecem suportadas para leitura e reprocessamento histórico.
- Persistência inicial de `d4-v4` mantém transação única e materializa derivação
  `WATCH_NATIVE` quando a série é estruturalmente elegível, mesmo sem tipo
  confirmado; após review, atividade v4 é rederivada com tipo confirmado antes de
  ficar `READY_TO_SYNC`.
- Calibração contextual usa passos medidos nativos como denominador, sem converter
  fatos v4 em packing legacy. UI exibe fonte, distância nativa e fator sem alterar
  redesign pós-031.
- Health Connect reutiliza projeções congeladas, provenance existente,
  `WATCH_WORKOUT_ACTIVITY`, crédito de passos e fluxo review-before-sync. Nenhum
  efeito remoto ocorre durante importação, review, calibração ou reprocessamento.
- Após revisão independente, review v4 não-CYCLING rederiva quando snapshot está
  ausente ou incompatível com tipo/fonte, com rollback se falhar; Health Connect
  bloqueia sync v4 não-CYCLING sem snapshot para evitar preparação parcial.
  `CYCLING` mantém a política própria da Spec 022, sem exigir snapshot de distância
  por passos.

### Comandos executados

- `./gradlew :app:compileDebugKotlin`;
- `./gradlew lint assembleDebug assembleRelease`;
- `git diff --check`.

### Resultados e evidências

- Compilação Kotlin debug: passou.
- `lint`, `assembleDebug` e `assembleRelease`: passaram.
- `git diff --check`: passou.
- Nenhum teste automatizado foi criado ou executado, conforme `AGENTS.md`.
- Validação física e comparação com referência externa permanecem em `032-08`.

### Riscos residuais

- Validação física ainda precisa confirmar atividade com variação de intensidade,
  fechamento header/série e referência externa manual.
- Semântica operacional de pausa e `sportValue` não foi promovida além dos fatos
  raw preservados.
- Atividades sincronizadas continuam protegidas contra reprocessamento ou
  republicação silenciosa.
- ADR-029 permanece `proposed` até validação física e fechamento da Spec 032 em
  032-08.

### Correção após revisão independente

**Causa raiz:** `submitReview()` atualizava a atividade com o estado calculado
antes de concluir `deriveWatchWorkoutInternal()`. Como `withWriteTransaction`
confirma retornos `RepositoryResult` e só desfaz exceções, um `UnexpectedState`
retornado pela derivação podia confirmar `READY_TO_SYNC` sem snapshot. O
coordenador de Health Connect também aceitava a preparação parcial de uma
atividade v4 sem snapshot, pois filhos de passos/sessão podiam ser elegíveis
independentemente da série derivada.

**Correção:** falhas ou resultado nulo da derivação v4 atravessam a transação como
`WatchWorkoutDerivationPreflightException`, são convertidos em
`RepositoryResult.UnexpectedState` após rollback e não deixam `READY_TO_SYNC`
persistido. `prepareWatchActivityOperations()` agora bloqueia explicitamente
atividades `WATCH_D3_D4` com parser `d4-v4` sem snapshot; legado e demais tipos
mantêm política anterior.

**Comandos executados:**

- `./gradlew :app:compileDebugKotlin`;
- `./gradlew lint assembleDebug assembleRelease`;
- `git diff --check`.

**Resultados:** compilação Kotlin debug, lint, builds debug/release e verificação
de whitespace passaram. Nenhum teste automatizado foi criado ou executado, em
conformidade com `AGENTS.md`.

**Riscos:** não houve validação física nem readback do Health Connect; atividade
v4 com derivação ausente agora falha fechada e exige correção/reprocessamento
local antes de nova tentativa explícita.

### Correção após validação física da revisão automática

**Causa raiz:** `requiresWatchWorkoutReprocessing` considerava a versão
`watch-d3-d4-v3` compatível para `RUNNING`/`WALKING`. Uma atividade `d4-v4`
importada com `confirmedType == null` podia receber fallback por comprimento de
passo nessa versão; ao confirmar o tipo, `submitReview()` portanto não identificava
que a política de distância precisava mudar e persistia o snapshot fallback.

**Correção:** para `d4-v4`, `RUNNING` e `WALKING` exigem snapshot com provenance
`WATCH_NATIVE` ou `WATCH_NATIVE_CONTEXT_CALIBRATED`. Qualquer fallback dispara o
mesmo `deriveWatchWorkoutInternal(..., replaceActive = true)` usado pelo
reprocessamento, dentro da transação existente. `CYCLING`, `TREADMILL`,
`FREE_WORKOUT`, legacy `d4-v1/v2/v3` e `SYNCED` mantêm suas políticas e guards.

Quando um tipo não nativo troca um snapshot pré-revisão, o snapshot ativo é
desativado antes da rederivação; snapshots históricos permanecem preservados. A
transição de `TREADMILL` é feita somente pelo helper de derivação, evitando CAS
duplicado no submit da revisão. Se não houver step length para reconstituir o
fallback, `TREADMILL` permanece explicitamente `PENDING_CALIBRATION`; demais tipos
não podem confirmar `READY_TO_SYNC` sem snapshot válido.

**Arquivos alterados nesta correção:**

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`;
- este arquivo de task;
- `docs/tasks/032-d4-native-workout-metrics/032-00-overview.md`.

**Comandos executados nesta correção:**

- `./gradlew lint assembleDebug assembleRelease`;
- `git diff --check`.

**Resultados:** build debug/release e lint passaram; nenhum teste automatizado foi
criado ou executado, conforme `AGENTS.md`; verificação de whitespace passou.

### Complemento: distância nativa antes da revisão

**Decisão:** a ausência temporária de `confirmedType` não impede a promoção de uma
série `d4-v4` íntegra para `WATCH_NATIVE`. O snapshot preserva distância nativa e
não aplica fator contextual nem infere `RUNNING`/`WALKING`.

**Fluxo:** ao confirmar `RUNNING` ou `WALKING`, o review sempre reavalia a política
usando o pipeline compartilhado. Sem fator, permanece `WATCH_NATIVE`; com fator
aprendido, torna-se `WATCH_NATIVE_CONTEXT_CALIBRATED`. Ao confirmar política não
elegível, como `TREADMILL` ou `FREE_WORKOUT`, a derivação volta ao fallback próprio;
`CYCLING` mantém seu fluxo manual.

**Arquivos adicionais:**

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`;
- `docs/specs/032-d4-native-workout-metrics.md`;
- `docs/decisions/adr-029-d4-native-metrics-and-distance-calibration.md`.

**Evidência esperada:** nova atividade física íntegra com distância D4 de `5760 m`
recebe `WATCH_NATIVE` e `5760 m` antes da revisão; referência manual de `5850 m`
produz fator `1.015625` e `WATCH_NATIVE_CONTEXT_CALIBRATED`.

### Ajuste visual

O valor de `Fonte da distância` passou a ocupar largura flexível, limitar-se a
uma linha e usar reticências quando exceder o espaço disponível. O conteúdo
completo permanece na descrição semântica da linha.

**Comandos adicionais:** `./gradlew lint assembleDebug assembleRelease` e
`git diff --check` — ambos passaram.

### Ajuste final de estado fail-closed

Review `d4-v4` sem snapshot agora entra no pipeline de derivação para todo tipo
não-CYCLING. Ausência de derivação permite somente `TREADMILL` permanecer em
`PENDING_CALIBRATION`; `FREE_WORKOUT` e demais tipos não confirmam
`READY_TO_SYNC` sem snapshot válido. Última execução de `./gradlew lint
assembleDebug assembleRelease` e `git diff --check`: passou. Nenhum teste
automatizado foi criado ou executado.
