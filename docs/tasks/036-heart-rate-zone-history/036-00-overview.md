# 036 — Histórico de zonas de frequência cardíaca e distribuição de intensidade

- **Status geral:** in_progress
- **Spec:** [036-heart-rate-zone-history.md](../../specs/036-heart-rate-zone-history.md)
- **Progresso:** 1/6 subtarefas concluídas

## Objetivo

Agregar historicamente as zonas de frequência cardíaca da Spec 034 em janelas
de 7 e 28 dias, exibir composição factual do tempo analisado e oferecer
distribuição diária dos últimos 28 dias na página contextual de histórico
cardiovascular da Spec 035, sem persistência ou efeitos Health Connect.

## Checklist

- [x] [036-01-auditar-zonas-historicas-e-congelar-contrato.md](036-01-auditar-zonas-historicas-e-congelar-contrato.md)
- [ ] [036-02-implementar-agregacao-historica-de-zonas.md](036-02-implementar-agregacao-historica-de-zonas.md)
- [ ] [036-03-integrar-read-model-cardiovascular.md](036-03-integrar-read-model-cardiovascular.md)
- [ ] [036-04-integrar-zonas-na-pagina-cardiovascular.md](036-04-integrar-zonas-na-pagina-cardiovascular.md)
- [ ] [036-05-renderizar-distribuicao-temporal.md](036-05-renderizar-distribuicao-temporal.md)
- [ ] [036-06-validar-fisicamente-e-fechar.md](036-06-validar-fisicamente-e-fechar.md)

## Observações

- Fonte única: `WorkoutHeartRateAnalysisPolicy` e
  `CardiovascularLoadWorkoutInput` da Spec 035.
- Fonte bounded permanece limitada a 56 dias; não criar query, N+1, migration,
  tabela, cache ou snapshot.
- `HistoryWindows.from(asOf)` continua única semântica temporal.
- `ChampionBarChart` não suporta stacked bars; planejamento usa uma zona por vez
  em série única e mantém rows acessíveis com as seis zonas.
- Room permanece v20 e Health Connect fica fora do escopo.
- `036-01` concluída sem alteração funcional; contrato de zonas, nulidade,
  denominadores, parcialidade, janelas e fallback grouped foi congelado para
  `036-02` em diante.
