# 014-07 — Derivar série e distância canônicas

- **Ticker:** `014`
- **Número:** `07`
- **Status:** completed
- **Concluída em:** 2026-08-06

## Objetivo e resultado esperado

Criar snapshot de derivação V1 por atividade `WATCH_D3_D4` e calcular uma única
distância canônica a partir dos intervalos elegíveis.

Resultado: distância reproduzível, ausente quando a série for inelegível e
independente da distância original, calibrada histórica ou estimativa agregada.

## Dependências

- `014-05` e `014-06` concluídas.

## Arquivos permitidos

- contratos/serviço de derivação em `core/model`;
- `RepositoryContracts.kt`;
- mappers e `RoomRepositories.kt` para persistência transacional;
- consumidores mínimos necessários para expor ausência/canônico;
- overview.

## Arquivos proibidos

- parser/protocolo já fechado;
- UI final, splits e Health Connect;
- Gradle, manifesto, migrations e schemas;
- infraestrutura phone/rota.

## Escopo

### Snapshot

Congelar:

```text
stepLengthMetersUsed
stepLengthSource
derivationMethod = FIXED_PERSONAL_STEP_LENGTH
derivationVersion
derivedAt
temporalEligibility
plausibility
```

### Série canônica

```text
intervalDistance = decodedSteps × stepLengthMetersUsed
canonicalDistance = soma exata dos intervalDistance
```

- Somar com precisão interna sem arredondamento prematuro.
- Série temporal inelegível mantém distância canônica ausente.
- Intervalo zero produz distância zero.
- Plausibilidade não modifica a fórmula.

### Estimativa agregada diagnóstica

`totalSteps × stepLengthMetersUsed` pode ser calculado apenas como diagnóstico
com nome distinto. Não deve alimentar nenhum consumidor funcional.

### Workflow de esteira

Para origem `WATCH_D3_D4`, confirmar tipo `TREADMILL` pode usar a série canônica
e avançar para `READY_TO_SYNC` sem exigir calibração histórica. Calibração
existente permanece preservada e não substitui a série.

## Fora do escopo

- cadência, velocidade, ritmo e splits;
- reprocessamento em lote de legado;
- calorias derivadas;
- UI final e Health Connect;
- testes automatizados.

## Passos de implementação

1. Resolver perfil elegível no instante da derivação.
2. Congelar valor, origem, método, versão e estado temporal.
3. Validar que a série é elegível antes de calcular canônico.
4. Calcular cada delta exclusivamente por passos decodificados.
5. Somar os deltas para obter a única distância canônica.
6. Persistir snapshot e série atomicamente.
7. Manter estimativa por passos totais em campo diagnóstico separado, quando
   útil.
8. Ajustar a transição de `TREADMILL` somente para `WATCH_D3_D4`.
9. Não substituir snapshot existente sem ação explícita.
10. Atualizar overview.

## Critérios de aceite

- Fórmula do intervalo é exatamente passos × `stepLengthMeters`.
- Distância da atividade é exatamente a soma dos intervalos.
- Não existe fallback para original, calibrada ou estimativa agregada.
- Série inelegível mantém distância canônica ausente.
- Mudança de perfil não altera snapshot antigo.
- Plausibilidade não redistribui passos.
- Zero passos produz zero local.
- `TREADMILL` de relógio não fica bloqueada por calibração histórica quando a
  série for elegível.
- Contratos phone históricos não mudam.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- cálculo manual sanitizado;
- comparação soma temporal = distância canônica;
- snapshot antes/depois de edição do perfil;
- matriz de workflow de esteira;
- builds e round-trip Room.

## Definição de pronto

Série e distância canônicas persistem atomicamente, workflow de esteira está
alinhado e item `014-07` está marcado.

## Rollback

Parar criação/consumo de snapshots e retornar ausência canônica; preservar dados
derivados para diagnóstico. Nunca restaurar distância original como fallback.

## Implementação

- `WorkoutActivityRepository` agora expõe leitura, derivação inicial e
  rederivação explícita de snapshot para atividades do relógio.
- `RoomRepositories` resolve o perfil vigente no instante da derivação, persiste
  durações efetivas e grava snapshot e intervalos derivados em transação Room.
  Importações novas `d4-v2` válidas podem criar snapshot inicial; reimportação não
  rederiva atividade existente.
- `WorkoutActivity` expõe snapshot, série e distância canônicas sem alterar
  campos originais ou calibrados. A transição de esteira usa série elegível
  somente para `WATCH_D3_D4`; contratos phone permanecem legados.
- Mappers preservam ausência canônica quando a série é inelegível e validam
  round-trip dos intervalos derivados. Splits continuam opcionais para task
  posterior.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `docs/decisions/adr-018-canonical-workout-time-series-and-health-projection.md`;
- `docs/specs/014-watch-workout-derived-metrics.md` (progresso alinhado);
- este arquivo e `014-00-overview.md`.

Não houve alteração de parser, UI final, Health Connect, Gradle, manifesto,
migration, schema ou infraestrutura phone/rota.

## Decisões e desvios

- A versão congelada da derivação é `watch-d3-d4-v1`.
- `decodedSteps × stepLengthMetersUsed` é calculado por intervalo e a distância
  canônica usa somente a soma desses intervalos. Estimativa por passos totais
  continua em campo diagnóstico separado.
- Snapshot existente não é substituído pela derivação inicial; rederivação exige
  operação explícita. Reimportação com fatos diferentes desativa snapshot ativo
  incompatível, preservando-o como histórico sem usar fallback.
- A regra de esteira verifica elegibilidade temporal da série canônica, não a
  guarda de projeção remota por plausibilidade; nenhuma plausibilidade altera
  passos ou fórmula.
- O caminho legado de sincronização permanece bloqueado para `WATCH_D3_D4`, pois
  ainda não consome a série canônica. A projeção granular fica para `014-12` e
  `014-13`, sem enviar distância original, calibrada ou agregada como fallback.
- A ausência de snapshot, perfil ou série elegível permanece ausência canônica.

## Correção final delimitada

- `HealthSyncOperationDao.findProcessable` exclui operações de atividade
  `WATCH_D3_D4` por `NOT EXISTS` antes de aplicar `LIMIT`; atividades phone não
  são bloqueadas. Este gate SQL é necessário para preservar o limite efetivo da
  fila; gates de repository e transição permanecem como defesa adicional.
- Cada intervalo novo do relógio recebe `ORDER_VALIDATED` na coluna existente
  `temporalEligibilityReason`, antes de qualquer `ORDER BY intervalIndex` do
  Room. Intervalos legados sem marcador, com marcador desconhecido ou com
  `OUT_OF_ORDER` falham fechados e mantêm distância canônica ausente. Reimportação
  explícita válida pode estabelecer o marcador, sem inventar distância.
- `rederiveWatchWorkout` retorna estado inesperado para atividades `SYNCING` ou
  `SYNCED`; confirmação específica e reprocessamento ficam bloqueados até
  `014-11`, sem criar snapshot novo ou substituir operações antigas.
- `resetInterrupted` ignora operações `WATCH_D3_D4`; elas permanecem na fila
  bloqueada sem mutação.
- O preflight agora constrói `snapshotEntity`, filhos e round-trip com
  `resolvedRawIntervals` antes de atualizar `workout_intervals` ou inserir
  snapshot. O retorno usa o snapshot já validado; falha impossível é lançada
  como exceção controlada para o `withWriteTransaction` desfazer toda a
  importação.
- Quando o snapshot canônico elegível é criado para atividade já revisada do
  relógio com `confirmedType = TREADMILL` e `workflowState = PENDING_CALIBRATION`,
  o CAS para `READY_TO_SYNC` acontece dentro da mesma transação, somente para
  `WATCH_D3_D4`. Falha da transição aborta a transação antes de deixar snapshot
  parcial. `PENDING_REVIEW` continua sendo promovida no `submitReview`.
- `persistImported` aceita somente sucesso ou ausência segura da derivação
  inicial. Perfil ausente e série inelegível permanecem persistíveis sem
  distância canônica; erro de preflight não confirma importação parcial.
- `transitionWorkoutSync` bloqueia origem `WATCH_D3_D4` para início, sucesso e
  falha. `resetInterruptedSyncing` migra somente atividades não relógio por
  query DAO com exclusão explícita de origem, sem migration ou schema.
- `markPersistedSourceOrder` separa ordem da elegibilidade: índices estritamente
  crescentes recebem `ORDER_VALIDATED` mesmo com lacuna ou primeiro índice não
  zero; o `TemporalSeriesValidator` então grava snapshot inelegível sem distância
  canônica. Somente ordem não crescente ou duplicada recebe `OUT_OF_ORDER`; legado
  sem marcador falha fechado.

## Comandos executados

- `./gradlew app:compileDebugKotlin` — primeiro resultado falhou por conversão
  ausente de amostras BPM; correção aplicada em `DatabaseMappers.kt`, resultado
  final PASS;
- `./gradlew app:compileDebugKotlin` — após correção de preflight e gates: PASS;
- `./gradlew lint assembleDebug assembleRelease` — PASS;
- `./gradlew installDebug` — PASS; instalado em `SM-S901E - 16`;
- `git diff --check` — PASS;
- `git status --short` — executado; somente código, ADR e planejamento desta
  subtarefa permanecem alterados;
- Validação final após gates de fila e ordem — `./gradlew lint
  assembleDebug assembleRelease`, `./gradlew installDebug` e `git diff --check`:
  PASS;
- Validação dos bloqueadores finais — `./gradlew app:compileDebugKotlin`,
  `./gradlew lint assembleDebug assembleRelease`, `./gradlew installDebug` e
  `git diff --check`: PASS;
- Validação após os ajustes de promoção atômica e marcador de ordem —
  `./gradlew app:compileDebugKotlin`: PASS, com somente avisos de depreciação
  já existentes em `RoomRepositories.kt`;
- `./gradlew lint assembleDebug assembleRelease`: a primeira execução encontrou
  falha interna do lint ao analisar `ExampleInstrumentedTest.kt` e ausência
  transitória de fonte Hilt gerada; sem erro de compilação do código alterado.
  Após os artefatos de build serem gerados, `./gradlew assembleDebug
  assembleRelease`: PASS, `./gradlew app:lintDebug`: PASS e `./gradlew lint`:
  PASS;
- `./gradlew installDebug`: PASS em `SM-S901E - 16`;
- `git diff --check`: PASS após os ajustes finais;
- testes automatizados — não criados nem executados, conforme escopo e
  `AGENTS.md`.

## Resultados e evidências

- Inspeção do contrato confirma cálculo sanitizado `100 × 0,8 + 0 × 0,8 +
  50 × 0,8 = 120 m`; a soma dos deltas é a distância canônica e o zero local
  permanece zero.
- Mapper de snapshot reconstrói a série a partir de raw/measured e compara
  distância, deltas, cumulados e amostras persistidas; série inelegível não cria
  filhos nem distância canônica.
- Edição posterior do perfil usa snapshot já persistido e não recalcula atividade;
  rederivação requer operação explícita.
- Matriz de workflow confirma `WATCH_D3_D4 + TREADMILL` elegível em
  `READY_TO_SYNC`, enquanto origem phone continua fora da regra.
- Lint, builds debug/release, instalação e `git diff --check` passaram.

## Riscos residuais

- Não houve importação D4 física nem round-trip Room exercitado em dispositivo;
  preflight local e rollback transacional estão validados por compilação, mas a
  evidência física permanece para `014-15`.
- Derivação de atividades legadas `d4-v1` continua sem passos decodificados e,
  portanto, sem distância canônica até reprocessamento explícito posterior.
- Consumidores finais, métricas adicionais, splits e Health Connect permanecem
  fora desta subtarefa; especificamente `ExercisesScreen`, Calendário,
  `HealthRecordMapper` e `core/healthconnect` não foram tocados. O sync legado de
  `WATCH_D3_D4` permanece fail-closed até as projeções canônicas posteriores.
- Projeções granulares, reprocessamento de legado e qualquer desbloqueio do
  writer de atividades do relógio continuam bloqueados para subtarefas
  posteriores (`014-11`/`014-12`/`014-13`).
