# 035 — Histórico de carga cardiovascular e tendência

- **Status geral:** pending
- **Spec:** [035-training-load-history-trends.md](../../specs/035-training-load-history-trends.md)
- **Progresso:** 1/6 subtarefas concluídas

## Objetivo

Agregar, em read model local e não persistido, a carga cardiovascular da Spec
034 em janelas de 7 e 28 dias, com comparação, distribuição diária, cobertura,
contagem de análises e contribuição por tipo na superfície histórica existente
de Exercícios.

## Checklist

- [x] [035-01-auditar-historico-e-congelar-contrato.md](035-01-auditar-historico-e-congelar-contrato.md)
- [ ] [035-02-implementar-agregacao-de-carga.md](035-02-implementar-agregacao-de-carga.md)
- [ ] [035-03-integrar-leitura-limitada-de-workouts.md](035-03-integrar-leitura-limitada-de-workouts.md)
- [ ] [035-04-integrar-read-model-em-exercicios.md](035-04-integrar-read-model-em-exercicios.md)
- [ ] [035-05-renderizar-historico-e-tendencia.md](035-05-renderizar-historico-e-tendencia.md)
- [ ] [035-06-validar-fisicamente-e-fechar.md](035-06-validar-fisicamente-e-fechar.md)

## Observações

- Room atual confirmado: v20. Nenhuma migration ou tabela analítica está
  planejada.
- Fonte única de carga individual: `WorkoutHeartRateAnalysisPolicy` +
  `WorkoutHeartRateAnalysis` da Spec 034.
- População: `WATCH_D3_D4`, não descartada, atribuída por `startedAt`; workflow
  operacional não exclui estados elegíveis.
- Range máximo: `[asOf - 56 dias, asOf)`, com contrato bounded no repository,
  atividades completas e samples, sem N+1 reativo.
- `035-01` foi auditada e concluída nesta rodada documental. O restante está
  pendente e não há implementação de produto neste commit.
- Auditoria revalidada sem alteração de produto; Room v20, policy 034 e
  contrato bounded permanecem confirmados. Riscos de custo temporal e cenário
  sem samples seguem pendentes para subtarefas posteriores.
- Mudança de FC máxima recompõe somente o read model visual; Health Connect,
  snapshots, workflow e atividades permanecem fora do escopo.
