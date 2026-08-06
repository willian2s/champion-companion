# 014 — Refinamento, derivação e sincronização dos treinos do relógio

- **Status geral:** in_progress
- **Spec:** [014-watch-workout-derived-metrics.md](../../specs/014-watch-workout-derived-metrics.md)
- **Progresso:** 3/16 subtarefas concluídas

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
- [ ] [014-04-correct-d4-interval-step-decoding.md](014-04-correct-d4-interval-step-decoding.md)
- [ ] [014-05-resolve-effective-interval-duration.md](014-05-resolve-effective-interval-duration.md)
- [ ] [014-06-persist-and-edit-personal-step-length.md](014-06-persist-and-edit-personal-step-length.md)
- [ ] [014-07-derive-canonical-distance-from-steps.md](014-07-derive-canonical-distance-from-steps.md)
- [ ] [014-08-derive-cadence-speed-pace-and-cumulative-distance.md](014-08-derive-cadence-speed-pace-and-cumulative-distance.md)
- [ ] [014-09-calculate-kilometer-splits-and-heart-rate.md](014-09-calculate-kilometer-splits-and-heart-rate.md)
- [ ] [014-10-integrate-estimated-metrics-charts-and-splits.md](014-10-integrate-estimated-metrics-charts-and-splits.md)
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
