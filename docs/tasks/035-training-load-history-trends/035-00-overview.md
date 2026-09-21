# 035 — Histórico de carga cardiovascular e tendência

- **Status geral:** completed
- **Spec:** [035-training-load-history-trends.md](../../specs/035-training-load-history-trends.md)
- **Progresso:** 7/7 subtarefas concluídas

## Objetivo

Agregar, em read model local e não persistido, a carga cardiovascular da Spec
034 em janelas de 7 e 28 dias, com comparação, distribuição diária, cobertura,
contagem de análises e contribuição por tipo na superfície histórica existente
de Exercícios, com histórico cardiovascular acessível em página contextual
própria.

## Checklist

- [x] [035-01-auditar-historico-e-congelar-contrato.md](035-01-auditar-historico-e-congelar-contrato.md)
- [x] [035-02-implementar-agregacao-de-carga.md](035-02-implementar-agregacao-de-carga.md)
- [x] [035-03-integrar-leitura-limitada-de-workouts.md](035-03-integrar-leitura-limitada-de-workouts.md)
- [x] [035-04-integrar-read-model-em-exercicios.md](035-04-integrar-read-model-em-exercicios.md)
- [x] [035-05-renderizar-historico-e-tendencia.md](035-05-renderizar-historico-e-tendencia.md)
- [x] [035-06-validar-fisicamente-e-fechar.md](035-06-validar-fisicamente-e-fechar.md)
- [x] [035-07-mover-carga-cardiovascular-para-pagina-propria.md](035-07-mover-carga-cardiovascular-para-pagina-propria.md)

## Observações

- Room atual confirmado: v20. Nenhuma migration ou tabela analítica está
  planejada.
- Fonte única de carga individual: `WorkoutHeartRateAnalysisPolicy` +
  `WorkoutHeartRateAnalysis` da Spec 034.
- População: `WATCH_D3_D4`, não descartada, atribuída por `startedAt`; workflow
  operacional não exclui estados elegíveis.
- Range máximo: `[asOf - 56 dias, asOf)`, com contrato bounded no repository,
  atividades completas e samples, sem N+1 reativo.
- `035-01` foi auditada e concluída na rodada documental anterior.
- `035-02` foi concluída com agregação pura em memória; leitura bounded,
  composição reativa, UI e validação física permanecem pendentes.
- `035-03` foi concluída com leitura reativa bounded de atividades completas e
  filhos agrupados, filtrada por `WATCH_D3_D4`, descarte e `startedAt`; mapeamento
  inválido falha fechado sem emitir população sintética.
- Auditoria revalidada sem alteração de produto; Room v20, policy 034 e
  contrato bounded permanecem confirmados. Riscos de custo temporal e cenário
  sem samples seguem pendentes para subtarefas posteriores.
- Mudança de FC máxima recompõe somente o read model visual; Health Connect,
  snapshots, workflow e atividades permanecem fora do escopo.
- `035-04` foi concluída com estado tipado de loading/dados/indisponibilidade,
  composição bounded independente da lista e recomputação por snapshot pessoal;
  renderização e validação física permanecem nas subtarefas seguintes.
- `035-05` foi concluída com seção histórica na superfície existente, estados
  seguros, comparações neutras, cobertura, contadores, buckets diários, tipos e
  semantics acessíveis. Inspeção visual física e screenshot permanecem na
  `035-06`.
- `035-06` foi concluída com banco físico Room v20, cálculo independente dos
  quatro períodos, fechamentos de buckets/tipos, recomposição e restauração por
  FC máxima, preservação de fatos/workflow/snapshots/Health Connect, reopen sem
  persistência analítica e screenshot sanitizada. N/A físicos estão registrados
  na evidência final.
- Após `035-06`, o usuário escolheu a opção 1 de apresentação: substituir a
  seção inline de Carga cardiovascular por CTA em Exercícios e página própria
  contextual. `035-07` altera somente apresentação/navegação, preserva
  read model e policy, não expande Home/Saúde e mantém `035-06` concluída.
- `035-07` foi concluída com destino contextual serializável, CTA acessível,
  página própria reutilizando o estado do `ExercisesViewModel`, retorno para
  Exercícios, smoke em dispositivo real e screenshot sanitizada.
