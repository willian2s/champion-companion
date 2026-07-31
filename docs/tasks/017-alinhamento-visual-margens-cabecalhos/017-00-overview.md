# 017 — Alinhamento visual de margens e cabeçalhos

- **Status geral:** pending
- **Spec:** [017-alinhamento-visual-margens-cabecalhos.md](../../specs/017-alinhamento-visual-margens-cabecalhos.md)
- **Progresso:** 0/3 subtarefas concluídas

## Objetivo

Alinhar margens e cabeçalhos de Calendário, detalhe diário, Dispositivo,
Selecionar treino e Preparar ao padrão visual predominante, sem alterar dados,
contratos, estado, comportamento ou navegação.

## Checklist

- [ ] [017-01-alinhar-calendario-e-detalhe-diario.md](017-01-alinhar-calendario-e-detalhe-diario.md)
- [ ] [017-02-alinhar-tela-dispositivo.md](017-02-alinhar-tela-dispositivo.md)
- [ ] [017-03-alinhar-selecao-e-preparacao-de-treino.md](017-03-alinhar-selecao-e-preparacao-de-treino.md)

## Observações

- Pacote 017 é visual e independente. Não reabre Fases 006 e 015, não reabre a
  subtarefa concluída `013-07`, não substitui `013-15` e não altera dependências
  da Fase 13.
- Ordem operacional: `017-01 → 017-02 → 017-03`. Subtarefas não possuem
  dependência técnica entre si, mas devem ser executadas e aceitas uma por vez.
- Preservar dados, contratos, estado, intents, callbacks, permissões, serviços,
  navegação e back stack existentes.
- Cada execução deve produzir screenshots de todas as superfícies alteradas,
  incluindo fonte aumentada e landscape representativos.
- Validação de implementação: `./gradlew lint assembleDebug assembleRelease` e
  `./gradlew installDebug`, além de smoke visual e `git diff --check`.
- Não criar, alterar ou executar testes automatizados.
- Cada subtarefa atualiza próprio status e evidências e marca somente seu item
  após conclusão real. Itens `pending`, `in_progress` ou `blocked` permanecem
  desmarcados.
- Indisponibilidade de dispositivo, screenshot ou validação manual necessária
  deve permanecer explícita como bloqueio; não fabricar evidência.
