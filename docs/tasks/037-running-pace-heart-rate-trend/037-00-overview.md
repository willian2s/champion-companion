# 037 — Tendência de corrida: relação entre ritmo e frequência cardíaca

- **Status geral:** completed
- **Spec:** [037-running-pace-heart-rate-trend.md](../../specs/037-running-pace-heart-rate-trend.md)
- **Progresso:** 6/6 subtarefas concluídas

## Objetivo

Exibir, nos últimos 56 dias, a relação factual entre ritmo e frequência
cardíaca média das corridas confirmadas do relógio, preservando provenance,
coverage e ausência sem inferir melhora, eficiência ou condicionamento.

## Checklist

- [x] [037-01-auditar-comparabilidade-e-congelar-contrato.md](037-01-auditar-comparabilidade-e-congelar-contrato.md)
- [x] [037-02-implementar-metricas-de-performance-de-corrida.md](037-02-implementar-metricas-de-performance-de-corrida.md)
- [x] [037-03-implementar-read-model-bounded-de-corridas.md](037-03-implementar-read-model-bounded-de-corridas.md)
- [x] [037-04-integrar-tendencia-de-corrida-no-viewmodel-e-navegacao.md](037-04-integrar-tendencia-de-corrida-no-viewmodel-e-navegacao.md)
- [x] [037-05-renderizar-historico-de-ritmo-e-fc.md](037-05-renderizar-historico-de-ritmo-e-fc.md)
- [x] [037-06-validar-fisicamente-e-fechar.md](037-06-validar-fisicamente-e-fechar.md)

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
- 037-02 concluída: domínio puro de pace, statuses, provenance e média FC
  ponderada; timeline compartilhada com 034 preservada. Read model bounded,
  integração, UI e validação física permanecem nas subtarefas seguintes.
- 037-03 concluída: projection SQL dedicada filtra corrida confirmada do relógio
  em janela bounded, une somente snapshot ativo e lê samples em batch dentro de
  transação. Room v20, entidades, schema, migrations, índices e
  `CardiovascularLoadWorkoutInput` permanecem inalterados. 037-04, 037-05 e
  037-06 foram concluídas.
- 037-04 concluída: Exercícios expõe estado dedicado da tendência, com janela de
  56 dias, `asOf` e `ZoneId` congelados no ViewModel; a policy roda fora do
  Compose e falhas de fonte viram `UNAVAILABLE`. CTA e destino serializável
  permanecem contextuais na subtree de Exercícios, separados da carga
  cardiovascular. 037-05 e 037-06 foram concluídas.
- 037-05 concluída: tela renderiza lista factual independente dos gráficos,
  provenance, coverage, estados, semantics e dois gráficos temporais separados
  com eixos `s/km` e `bpm`. Gráficos aparecem antes da lista, que usa páginas
  locais de 10 corridas quando necessário; 037-06 foi concluída com validação
  física, árvore de acessibilidade e screenshot sanitizada.
- 037-06 concluída em 2026-09-29 sobre instalação Release incremental e banco
  físico existente. Room v20, cálculos independentes, provenance, coverage,
  reabertura e preservação de activities, samples, snapshots, derivados, splits,
  calibrações e fila Health Connect fecharam. A população teve 21 corridas
  confirmadas; 5 pontos nativos comparáveis entraram nos gráficos e 16 fallbacks
  permaneceram somente na lista, conforme contrato. Cenários sem FC, FC completa,
  amostra inválida, snapshot ausente, empate temporal e múltiplas corridas no
  mesmo dia ficaram `N/A` por ausência natural, sem fabricação.
- Evidência sanitizada: [037-06-physical-validation.md](../../integration/037-06-physical-validation.md)
  e [037-06-running-performance.png](../../integration/037-06-running-performance.png).
