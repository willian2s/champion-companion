# 014 — Refinamento, derivação e sincronização dos treinos do relógio

- **Status geral:** in_progress
- **Spec:** [014-watch-workout-derived-metrics.md](../../specs/014-watch-workout-derived-metrics.md)
- **Progresso:** 10/16 subtarefas concluídas

## Objetivo

Corrigir intervalos D4, preservar legado `d4-v1`, derivar uma única série
canônica com `stepLengthMeters`, integrar métricas locais e sincronizar projeções
filhas idempotentes de treinos iniciados pelo Champion C001.

## Decisões de execução

- Distância canônica total é exatamente a soma dos intervalos canônicos.
- Estimativa por passos totais é somente diagnóstica.
- Índices precisam ser contínuos, únicos e iniciar em zero.
- `d4-v1` pode fornecer `rawMotionValue` legado; parser corrigido usa `d4-v2`.
- Último intervalo recebe o restante positivo; plausibilidade é registrada à
  parte e não redistribui passos.
- `TREADMILL` de `WATCH_D3_D4` usa distância canônica e não depende da calibração
  histórica.
- BPM por split usa sample-and-hold e sobreposição temporal semiaberta.
- `SpeedRecord` usa amostra no ponto médio do intervalo.
- Cada filho Health Connect possui uma operação própria.
- Migration local e migration de projeções Health Connect são separadas.

## Checklist

- [x] [014-01-audit-d4-interval-layout-and-consumers.md](014-01-audit-d4-interval-layout-and-consumers.md)
- [x] [014-02-define-temporal-and-derived-metrics-contracts.md](014-02-define-temporal-and-derived-metrics-contracts.md)
- [x] [014-03-migrate-room-for-interval-and-derivation-metadata.md](014-03-migrate-room-for-interval-and-derivation-metadata.md)
- [x] [014-04-correct-d4-interval-step-decoding.md](014-04-correct-d4-interval-step-decoding.md)
- [x] [014-05-resolve-effective-interval-duration.md](014-05-resolve-effective-interval-duration.md)
- [x] [014-06-persist-and-edit-personal-step-length.md](014-06-persist-and-edit-personal-step-length.md)
- [x] [014-07-derive-canonical-distance-from-steps.md](014-07-derive-canonical-distance-from-steps.md)
- [x] [014-08-derive-cadence-speed-pace-and-cumulative-distance.md](014-08-derive-cadence-speed-pace-and-cumulative-distance.md)
- [x] [014-09-calculate-kilometer-splits-and-heart-rate.md](014-09-calculate-kilometer-splits-and-heart-rate.md)
- [x] [014-10-integrate-estimated-metrics-charts-and-splits.md](014-10-integrate-estimated-metrics-charts-and-splits.md)
- [ ] [014-11-reprocess-existing-watch-activities.md](014-11-reprocess-existing-watch-activities.md)
- [ ] [014-12-model-and-migrate-health-connect-child-projections.md](014-12-model-and-migrate-health-connect-child-projections.md)
- [ ] [014-13-sync-granular-distance-and-speed-with-health-connect.md](014-13-sync-granular-distance-and-speed-with-health-connect.md)
- [ ] [014-14-reconcile-legacy-health-connect-distance-projections.md](014-14-reconcile-legacy-health-connect-distance-projections.md)
- [ ] [014-15-run-physical-validation.md](014-15-run-physical-validation.md)
- [ ] [014-16-close-watch-workout-derived-metrics-phase.md](014-16-close-watch-workout-derived-metrics-phase.md)

## Dependências

```text
014-01
   ↓
014-02
   ↓
014-03
   ├── 014-04 → 014-05 ─┐
   └── 014-06 ──────────┤
                        ↓
                      014-07
                        ↓
                      014-08
                     ┌──┴──┐
                     ↓     ↓
                   014-09  014-11
                     ↓
                   014-10
                     ↓
                   014-12
                     ↓
                   014-13
                     ↓
                   014-14
                     ↓
                   014-15
                     ↓
                   014-16
```

`014-15` também exige conclusão de `014-11`. `014-16` é exclusivamente
documental e não contém correções funcionais.

## Resultado de 014-01

A auditoria documental confirmou:

- o parser de produção atual é `d4-v1`;
- o u24 de `bytes[7..9]` chega integralmente a
  `WorkoutIntervalEntity.steps`;
- a ponte `d4-v1 steps → rawMotionValue` é recuperável sob guardas de origem,
  versão e faixa u24;
- a sequência sanitizada decodificada fecha em `3.737` passos;
- distância e calorias temporais permanecem raw, sem semântica comprovada;
- lacuna, duplicata e índice inicial incorreto precisam bloquear a série V1;
- intervalos atuais permanecem fixos em 60 segundos;
- UI, calendário e Health Connect priorizam distância calibrada/original;
- `TREADMILL` atual exige calibração independentemente da origem;
- a fila Health Connect não possui filho de atividade e o mapper produz um único
  `DistanceRecord` agregado.

Relatório:

```text
docs/integration/014-01-d4-layout-and-consumer-audit.md
```

Esses resultados são entrada obrigatória de `014-02`.

## Migrações

- `014-03`: evolução aditiva local `v8→v9` concluída para perfil, raw/measured,
  compatibilidade `d4-v1`, duração e snapshot derivado.
- `014-12`: evolução aditiva posterior para manifesto, `projectionChildKey`,
  payload congelado, ações e checkpoints Health Connect.

A migration local efetiva é `MIGRATION_8_9`; não há derivação, mudança de
workflow ou efeito remoto durante upgrade. A migration de projeções Health
Connect continua pendente.

## Observações

- Fase 013 permanece `closed`, com `partially_delivered` por
  `product_reorientation`.
- ADR-018 define raw/measured/derived, série canônica e projeções filhas.
- Distância original, calibrada histórica e campos temporais raw não são fallback.
- Testes automatizados não devem ser criados nem executados sem pedido explícito.
- Cada subtarefa atualiza este overview e marca somente seu item após conclusão
  real e revisão.
- `014-01` não alterou código, schema, migration, Gradle ou testes.
- `014-02` fechou contratos Android-free de raw/measured/derived, série temporal,
  plausibilidade, splits e projeções filhas; builds e revisão independente foram
  aprovados. `SPEED` permanece sem writer por guarda fail-closed. A guarda exigiu
  alteração mínima em `core/healthconnect`, fora da lista original de arquivos
  permitidos, para manter compilação após adicionar o enum.
- `014-03` concluiu migration manual `8→9`, schema Room v9, ponte raw controlada
  `d4-v1`, perfil de comprimento do passo e persistência local de snapshot,
  intervalos derivados e splits. Instalação limpa usa índice UNIQUE de
  `activeActivityId`; filhos apontam somente para `snapshotId`; timestamps
  derivados preservam nanos. Importações `d4-v1` também preservam raw após
  upgrade; `persistImported` não cria nem recalcula snapshot.
  `lint`, builds debug/release e `installDebug` passaram. Projeções Health
  Connect continuam pendentes para `014-12`; nenhuma outra subtarefa foi
  iniciada.
- `014-04` passou a ler o u24 temporal D4 uma única vez, separar
  `rawMotionValue`/`decodedSteps`, preservar distância e calorias temporais como
  raw nos caminhos `d4-v1` e `d4-v2` e usar `d4-v2` em novas importações.
  `d4-v1` permanece identificável, sem backfill de `decodedSteps`; valores
  temporais legados já persistidos permanecem preservados; a sequência
  sanitizada retornou `3.737` passos. Lookup de retomada agora carrega
  `parserVersion` e só ignora importação `VALID` com versão D4 conhecida.
   `lint`, builds debug/release, `installDebug` e `git diff --check` passaram. Não
   houve teste automatizado nem importação física D4; esta última permanece risco
   residual para `014-15`. Revisão independente após correções foi aprovada sem
   bloqueadores; auditoria de `014-01` ainda contém nomenclatura histórica e fica
   registrada para atualização documental futura.
- `014-05` corrigiu o resolvedor temporal para validar índices antes de calcular
  janelas por `T/N`, mantendo lacunas, duplicatas, primeiro índice não zero e
  ordem inconsistente inelegíveis sem síntese temporal. O residual de `1.637 s`
  permanece `27 × 60 s + 17 s`; cauda longa permanece em `tailUncovered`, sem
  bucket. O parser agora propaga `resolvedEndedAt` para `WorkoutMetrics` sem
  alterar fatos raw/measured nem D4 v1/v2. Foram atualizados o contrato e a
  documentação de protocolo; snapshot, persistência, distância, Health Connect,
  UI, migration e schema continuam pendentes. `lint`, builds debug/release,
  `installDebug` e `git diff --check` passaram; testes automatizados não foram
 executados. Riscos residuais: validação física D4 e plausibilidade definida por
 evidência permanecem para subtarefas posteriores.
 - `014-06` adicionou edição de `stepLengthMeters` no Perfil, com persistência
   transacional de valor, origem e atualização. A sugestão adotada é
   `alturaCentímetros / 100 × 0,415`, permanece proposta sem persistência
   automática e exige confirmação explícita para registrar
   `HEIGHT_SUGGESTION_ACCEPTED`; entrada manual registra `MANUAL`. Perfil sem
   altura não recebe default. Build, lint, instalação e reinício no Galaxy S22
   confirmaram persistência manual e ausência de efeito retroativo; screenshot
   foi capturada fora do repositório. Revisão independente aprovou sem
   bloqueadores. Aceite físico da sugestão permanece risco residual porque o
   dispositivo não possuía altura persistida.
- `014-07` adicionou snapshot V1 e derivação transacional da série canônica para
  `WATCH_D3_D4`, com distância por soma de `decodedSteps × stepLengthMetersUsed`,
  ausência segura para série inelegível e estimativa agregada somente
  diagnóstica. Importações novas `d4-v2` válidas podem criar snapshot inicial;
  edição de perfil não altera snapshot antigo e rederivação é explícita. A regra
  de `TREADMILL` do relógio usa elegibilidade canônica sem exigir calibração
  histórica; phone permanece legado. Lint, builds debug/release, instalação e
  `git diff --check` passaram. Não houve testes automatizados, importação D4
  física ou round-trip Room em dispositivo; esses riscos permanecem para fases
  posteriores. O writer legado de `WATCH_D3_D4` permanece bloqueado até as
  projeções granulares de `014-12`/`014-13`, sem fallback de distância.
- Progresso da spec `014-watch-workout-derived-metrics.md` alinhado para `7/16`.
- Revisão independente de `014-07`: `RoomRepositories` agora bloqueia
  operações legadas de `WATCH_D3_D4` antes de entregar ou garantir itens da
  fila (`findProcessable`, `ensureOperation`, leituras por agregado e transições
  de estado). Linhas antigas permanecem locais; nenhum mapper legado recebe
  atividade do relógio e nenhum arquivo de `core/healthconnect` foi alterado.
- A ordem da lista de intervalos é validada antes da persistência. Quando a
  origem está fora de ordem, marcador na coluna já existente de elegibilidade
  impede rederivação após o `ORDER BY intervalIndex`; snapshot ativo é ocultado
  ou desativado e distância canônica permanece ausente. Não houve migration,
  schema, parser, protocolo ou alteração de consumidores phone.
- Progresso permanece `7/16`; nenhuma subtarefa posterior foi iniciada.
- Validação final de `014-07` após gates de fila e ordem: lint, builds
  debug/release, instalação e `git diff --check` passaram.
- Bloqueadores finais de `014-07` corrigidos: derivação faz preflight completo
  de snapshot, filhos e round-trip antes de qualquer write; `persistImported`
  mantém ausência segura ou provoca rollback controlado em inconsistência.
- `transitionWorkoutSync` bloqueia todas as transições do writer legado para
  `WATCH_D3_D4`, e `resetInterruptedSyncing` exclui essa origem por query DAO;
  gates HealthSync e ordem temporal foram preservados.
- Risco físico permanece: round-trip Room e importação D4 em dispositivo ainda
  aguardam `014-15`; projeções granulares e desbloqueio do writer continuam
  bloqueados para `014-11`/`014-12`/`014-13`.
- Ajuste final de `014-07`: snapshot canônico elegível de atividade do relógio
  já revisada como `TREADMILL` em `PENDING_CALIBRATION` promove o workflow para
  `READY_TO_SYNC` na mesma transação; falha do CAS desfaz a derivação. Atividade
  em `PENDING_REVIEW` continua sendo promovida somente por `submitReview`.
- Ajuste final de ordem: `ORDER_VALIDATED` registra ordem estritamente crescente
  mesmo com lacuna ou primeiro índice não zero; o `TemporalSeriesValidator`
  mantém snapshot inelegível e distância canônica ausente nesses casos.
  `OUT_OF_ORDER` fica reservado para ordem não crescente/duplicada, e legado sem
  marcador continua fail-closed.
- Risco residual explícito: `ExercisesScreen`, Calendário e consumidores finais,
  `HealthRecordMapper` e `core/healthconnect` permanecem fora do escopo de
  `014-07` e não foram alterados. Integração Health Connect continua bloqueada
  para subtarefas posteriores.
- `014-08` confirmou que as fórmulas V1, zero passos, ritmo ausente, acumulados,
  persistência reproduzível e propagação de plausibilidade já estavam entregues
  nos contratos e mappers de `014-07`; nenhum código adicional foi necessário.
  Consumidores finais legados permanecem reservados para `014-10`.
- `014-10` alinhou Exercícios, detalhe e Calendário à série canônica persistida.
  Distância, ritmo, velocidade, acumulado, gráficos, intervalos, splits e BPM
  por quilômetro não usam distância original, calibrada ou estimativa agregada
  como fallback para `WATCH_D3_D4`. Proveniência, versão, plausibilidade,
  ausência e orientação de perfil/reprocessamento são exibidas; `TREADMILL` do
  relógio não exige calibração histórica. Fluxos observam snapshots ativos para
  refletir materialização explícita sem recriar a tela. `compileDebugKotlin`,
  `lintDebug`, `lint assembleDebug assembleRelease`, `installDebug` e
  `git diff --check` passaram; testes automatizados não foram criados nem
  executados. Screenshots smoke estão em
  `docs/tasks/014-watch-workout-derived-metrics/screenshots/`; o dispositivo não
  possuía atividade canônica para validar visualmente detalhe, splits, BPM,
  plausibilidade, zero passos e esteira, risco mantido para `014-15`.
- Progresso da spec e do overview alinhado para `10/16`; nenhuma subtarefa
  posterior foi iniciada.
- `014-09` foi corrigida transversalmente: snapshots legados sem projeção de
  splits continuam legíveis e só recebem linhas após rederivação explícita;
  splits novos são persistidos apenas para série elegível sem indicadores de
  implausibilidade; a política remota sem limiar global continua bloqueando
  Health Connect, sem ocultar o read model local. Reimportação compara
  instante/BPM das amostras antes de preservar snapshot. O cálculo estrito falha
  fechado quando não consegue materializar todas as fronteiras.
  `app:compileDebugKotlin`, `app:lintDebug` e
  `./gradlew lint assembleDebug assembleRelease`, `installDebug` e
  `git diff --check` passaram; APK final foi instalado no Galaxy S22
  `SM-S901E`. Testes automatizados não foram criados nem executados.
  Round-trip Room físico, validação D4 em detalhe, Health Connect e
  reprocessamento de legado continuam bloqueados para
  `014-11`/`014-12`/`014-13`/`014-15`.
