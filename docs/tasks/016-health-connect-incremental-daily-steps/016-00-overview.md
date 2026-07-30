# 016 — Sincronização incremental de passos diários

- **Status geral:** concluída
- **Spec:** [016-health-connect-incremental-daily-steps.md](../../specs/016-health-connect-incremental-daily-steps.md)
- **Progresso:** 2/2 subtarefas concluídas

## Objetivo

Manter `DailySummary` cumulativo local e sincronizar seus passos no Health
Connect por operações incrementais imutáveis, com retry determinístico,
concorrência segura e compatibilidade com operações cumulativas existentes.

## Checklist

- [x] [016-01-implementar-sync-incremental-de-passos.md](016-01-implementar-sync-incremental-de-passos.md)
- [x] [016-02-revisar-cenarios-e-fechar-sync-incremental.md](016-02-revisar-cenarios-e-fechar-sync-incremental.md)

## Observações

- Ordem obrigatória: `016-01 -> 016-02`.
- Escopo limitado a `DailySummary + STEPS` na fila e no mapper.
- Não executar em paralelo com mudanças da Fase 13 em Room, mapper ou
  coordinator.
- Testes automatizados não serão criados, alterados ou executados.
- Task 016 concluída. Revisão independente realizada; dois achados médios
  corrigidos (validação de janela entre dias; uniformidade de fuso na cadeia);
  um bug de reconciliação corrigido (`allowNewOperation` removido do backlog).
