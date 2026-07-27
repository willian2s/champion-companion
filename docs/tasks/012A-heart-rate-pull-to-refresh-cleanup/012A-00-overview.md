# 012A — Simplificação da tela de frequência cardíaca

- **Status geral:** pending
- **Spec:** [012A-heart-rate-pull-to-refresh-cleanup.md](../../specs/012A-heart-rate-pull-to-refresh-cleanup.md)
- **Progresso:** 0/3 subtarefas concluídas

## Objetivo

Remover a redundância do card `Resumo atual e ações` da tela de frequência cardíaca, renomeando-o para `Resumo atual`, deslocando o gatilho explícito de importação D1 para pull-to-refresh e mantendo a medição manual na superfície própria.

## Checklist

- [ ] [012A-01-integrar-pull-to-refresh-d1.md](012A-01-integrar-pull-to-refresh-d1.md)
- [ ] [012A-02-simplificar-resumo-e-realocar-feedback-d1.md](012A-02-simplificar-resumo-e-realocar-feedback-d1.md)
- [ ] [012A-03-validar-fluxo-d1-e-fechar-documentacao-da-task.md](012A-03-validar-fluxo-d1-e-fechar-documentacao-da-task.md)

## Observações

- A execução deve seguir a ordem `012A-01`, `012A-02` e `012A-03`; cada subtarefa depende da anterior, exceto a primeira.
- O D1 continuará sendo uma ação explícita: pull-to-refresh substituirá `Buscar dados do relógio`, sem automação por lifecycle.
- O CTA de medição permanecerá apenas em `HeartRateSessionSurface`; cancelamento, retry e dismiss do D1 continuarão como ações contextuais do painel.
- `D1HeartRateImportSurfaceState` será a fonte preferencial para representar o refresh em andamento, evitando novos contratos quando possível.
- O fechamento atualizará `docs/baseline/current-baseline.md`; roadmap e nova ADR permanecem fora do escopo.
- Os principais riscos são regressões na convivência entre `PullToRefreshBox` e `verticalScroll`, perda de ações contextuais do D1 e textos que ainda mencionem o botão removido.
