# 014 — Refinamento, derivação e sincronização dos treinos do relógio

- **Status geral:** pending
- **Spec:** [014-watch-workout-derived-metrics.md](../../specs/014-watch-workout-derived-metrics.md)
- **Progresso:** 0/14 subtarefas concluídas

## Objetivo

Corrigir intervalos D4, derivar série canônica com comprimento pessoal do passo,
integrar métricas locais e sincronizar projeções granulares idempotentes de
treinos iniciados pelo Champion C001.

## Checklist

- [ ] [014-01-audit-d4-interval-layout-and-consumers.md](014-01-audit-d4-interval-layout-and-consumers.md)
- [ ] [014-02-define-temporal-and-derived-metrics-contracts.md](014-02-define-temporal-and-derived-metrics-contracts.md)
- [ ] [014-03-migrate-room-for-interval-and-derivation-metadata.md](014-03-migrate-room-for-interval-and-derivation-metadata.md)
- [ ] [014-04-correct-d4-interval-step-decoding.md](014-04-correct-d4-interval-step-decoding.md)
- [ ] [014-05-resolve-effective-interval-duration.md](014-05-resolve-effective-interval-duration.md)
- [ ] [014-06-persist-and-edit-personal-step-length.md](014-06-persist-and-edit-personal-step-length.md)
- [ ] [014-07-derive-canonical-distance-from-steps.md](014-07-derive-canonical-distance-from-steps.md)
- [ ] [014-08-derive-cadence-speed-pace-and-cumulative-distance.md](014-08-derive-cadence-speed-pace-and-cumulative-distance.md)
- [ ] [014-09-calculate-kilometer-splits-and-heart-rate.md](014-09-calculate-kilometer-splits-and-heart-rate.md)
- [ ] [014-10-integrate-estimated-metrics-charts-and-splits.md](014-10-integrate-estimated-metrics-charts-and-splits.md)
- [ ] [014-11-reprocess-existing-watch-activities.md](014-11-reprocess-existing-watch-activities.md)
- [ ] [014-12-sync-granular-distance-and-speed-with-health-connect.md](014-12-sync-granular-distance-and-speed-with-health-connect.md)
- [ ] [014-13-reconcile-legacy-health-connect-distance-projections.md](014-13-reconcile-legacy-health-connect-distance-projections.md)
- [ ] [014-14-run-physical-validation-and-close-phase.md](014-14-run-physical-validation-and-close-phase.md)

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
```

`014-14` também exige conclusão do ramo `014-11`.

## Observações

- Fase 013 permanece `closed`, com `partially_delivered` por
  `product_reorientation`; nenhuma subtarefa reabre direção phone.
- ADR-018 define separação raw/measured/derived, snapshots e projeções filhas.
- Migration prevista: aditiva `v8 → v9`, sem derivação, remoção de legado ou
  efeito Health Connect durante upgrade.
- Distância original, distância temporal raw e calorias temporais raw permanecem
  diagnósticas; não são fallback funcional.
- Testes automatizados não devem ser criados nem executados sem pedido explícito.
- Cada subtarefa atualiza este overview no mesmo trabalho e marca somente seu
  item após conclusão real.
