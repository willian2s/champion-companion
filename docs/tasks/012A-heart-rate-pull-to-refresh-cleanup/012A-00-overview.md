# 012A — Simplificação da tela de frequência cardíaca

- **Status geral:** completed
- **Spec:** [012A-heart-rate-pull-to-refresh-cleanup.md](../../specs/012A-heart-rate-pull-to-refresh-cleanup.md)
- **Progresso:** 3/3 subtarefas concluídas

## Objetivo

Remover a redundância do card `Resumo atual e ações` da tela de frequência cardíaca, renomeando-o para `Resumo atual`, deslocando o gatilho explícito de importação D1 para pull-to-refresh e mantendo a medição manual na superfície própria.

## Checklist

- [x] [012A-01-integrar-pull-to-refresh-d1.md](012A-01-integrar-pull-to-refresh-d1.md)
- [x] [012A-02-simplificar-resumo-e-realocar-feedback-d1.md](012A-02-simplificar-resumo-e-realocar-feedback-d1.md)
- [x] [012A-03-validar-fluxo-d1-e-fechar-documentacao-da-task.md](012A-03-validar-fluxo-d1-e-fechar-documentacao-da-task.md)

## Observações

- A execução seguiu a ordem `012A-01`, `012A-02` e `012A-03`.
- D1 continua ação explícita por pull-to-refresh, sem automação por lifecycle.
- O CTA de medição permanece somente em `HeartRateSessionSurface`; o painel D1 preserva progresso, metadata, retry e dismiss, sem ação de cancelamento. A confirmação de retorno segura durante D1 permaneceu inalterada.
- `D1HeartRateImportSurfaceState` representa o refresh em andamento, sem novos contratos.
- Baseline atualizado; roadmap e ADR permaneceram fora do escopo.
