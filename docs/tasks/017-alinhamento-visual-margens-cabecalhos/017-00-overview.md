# 017 — Alinhamento visual de margens e cabeçalhos

- **Status geral:** concluído
- **Spec:** [017-alinhamento-visual-margens-cabecalhos.md](../../specs/017-alinhamento-visual-margens-cabecalhos.md)
- **Progresso:** 3/3 subtarefas concluídas

## Objetivo

Alinhar margens e cabeçalhos de Calendário, detalhe diário, Dispositivo,
Selecionar treino e Preparar ao padrão visual predominante, sem alterar dados,
contratos, estado, comportamento ou navegação.

## Checklist

- [x] [017-01-alinhar-calendario-e-detalhe-diario.md](017-01-alinhar-calendario-e-detalhe-diario.md)
- [x] [017-02-alinhar-tela-dispositivo.md](017-02-alinhar-tela-dispositivo.md)
- [x] [017-03-alinhar-selecao-e-preparacao-de-treino.md](017-03-alinhar-selecao-e-preparacao-de-treino.md)

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
- `017-01` concluída com validação visual em dispositivo físico, screenshots
  sanitizados e comandos obrigatórios aprovados. Rótulos da navegação inferior
  quebram em duas linhas com fonte `1,3x`, sem corte ou bloqueio; acompanhar nas
  próximas superfícies.
- `017-02` concluída com estado associado/conectado validado em dispositivo
  físico, screenshots sanitizados e comandos obrigatórios aprovados. Estados de
  busca e vazio não foram acionados para não iniciar BLE; branches permanecem
  preservadas no diff e sem bloqueio.
- `017-03` concluída com seleção e preparação validadas em portrait, fonte
  `1,3x` e landscape. Header independente, botão outlined no padrão do detalhe
  diário e retorno antes do countdown foram confirmados; nenhum fluxo de sessão
  foi iniciado.
