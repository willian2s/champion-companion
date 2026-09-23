# 037 — Tendência de corrida: relação entre ritmo e frequência cardíaca

- **Status geral:** in_progress
- **Spec:** [037-running-pace-heart-rate-trend.md](../../specs/037-running-pace-heart-rate-trend.md)
- **Progresso:** 1/6 subtarefas concluídas

## Objetivo

Exibir, nos últimos 56 dias, a relação factual entre ritmo e frequência
cardíaca média das corridas confirmadas do relógio, preservando provenance,
coverage e ausência sem inferir melhora, eficiência ou condicionamento.

## Checklist

- [x] [037-01-auditar-comparabilidade-e-congelar-contrato.md](037-01-auditar-comparabilidade-e-congelar-contrato.md)
- [ ] [037-02-implementar-metricas-de-performance-de-corrida.md](037-02-implementar-metricas-de-performance-de-corrida.md)
- [ ] [037-03-implementar-read-model-bounded-de-corridas.md](037-03-implementar-read-model-bounded-de-corridas.md)
- [ ] [037-04-integrar-tendencia-de-corrida-no-viewmodel-e-navegacao.md](037-04-integrar-tendencia-de-corrida-no-viewmodel-e-navegacao.md)
- [ ] [037-05-renderizar-historico-de-ritmo-e-fc.md](037-05-renderizar-historico-de-ritmo-e-fc.md)
- [ ] [037-06-validar-fisicamente-e-fechar.md](037-06-validar-fisicamente-e-fechar.md)

## Observações

- Branch criada a partir de `main` em `201a7b8c80d176c2f726176fa9107a7326a62f22`.
- População: `WATCH_D3_D4`, `confirmedType == RUNNING`, não descartada.
- Range: `[asOf - 56 dias, asOf)`; sem current28/previous28 ou score.
- Pace usa escalares do snapshot ativo; fallback fica visível, mas não é
  comparável no trend principal.
- FC média usa timeline sample-and-hold compartilhada com a 034 e coverage sem
  threshold arbitrário.
- Query/read model é dedicado; `CardiovascularLoadWorkoutInput` não será
  ampliado. Room permanece v20 e Health Connect fica fora do escopo.
- 037-01 concluída sem alteração funcional. Código atual ainda não filtra
  `confirmedType == RUNNING` nas consultas watch genéricas; implementação deve
  usar query dedicada com snapshot ativo e projection mínima.
- Não há banco físico local sanitizado no checkout. Composição real de corridas,
  proveniências e cenários naturais ficam para 037-06, com `N/A` quando não
  surgirem sem fabricação de dados.
