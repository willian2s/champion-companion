# 015-02 — Formalizar read model local, ADR e roadmap

- **Status:** concluída em 27/07/2026.
- **Objetivo:** formalizar decisão arquitetural e ordem de roadmap.
- **Resultado esperado:** ADR-013 criada e roadmap atualizado durante a execução
  desta subtarefa, registrando Fases 13/14 adiadas e Fase 15 próxima.

## Documentos obrigatórios

015-01, spec 015, direção, roadmap, ADRs 003, 005, 009, 010, 011 e 012,
baseline completa e código de navegação, repositories e persistência.

## Dependências e entradas

- 015-01 concluída.
- Próximo número ADR disponível: 013.

## Arquivos permitidos

- Este arquivo e overview.
- Durante execução: `docs/decisions/adr-013-*.md` e roadmap, conforme escopo
  aprovado.

## Arquivos proibidos durante o planejamento desta fase

Kotlin, Compose, Room, migrations, testes, BLE e Health Connect. A subtarefa
015-02 deve alterar ADR-013 e roadmap quando for executada.

## Escopo

Criar ADR que registre calendário como projeção local derivada; fluxo
`CalendarScreen → CalendarViewModel → CalendarRepository → Room`; proibições de
acesso BLE/Health Connect/tabela duplicada; datas, timezone, intervalos,
dispositivos, ausência de soma entre resumo e atividades, fila local e ausência
de migration. Atualizar roadmap com Fase 12 concluída, Fases 13/14 adiadas e
Fase 15 próxima.

## Fora do escopo

Alterar ADR-005 sem justificativa, implementar repository, alterar schema ou
resolver inconsistências anteriores.

## Passos

1. Reconciliar decisões existentes.
2. Criar ADR-013 com conteúdo e alternativas descartadas.
3. Identificar eventual compatibilidade necessária com ADR-005.
4. Atualizar roadmap sem renumerar fases.
5. Registrar impactos e riscos.

## Critérios de aceite

- ADR-013 contém todas as proibições e semânticas exigidas.
- Roadmap registra adiamentos sem renumerar fases.
- Não há decisão de tabela materializada ou migration.
- Ausência de Health Connect/BLE está explícita.

## Validação manual

Revisão cruzada com ADR-009/010/011/012 e input da Fase 15. Screenshot: não
aplicável; task documental.

## Comandos de build

Nenhum build necessário. Não executar testes.

## Evidências e atualização

Registrar se ADR-013 e roadmap futuro cobrem cada requisito. Atualizar overview
somente após revisão documental.

## Validação final

Confirmar que ADR-013 e roadmap foram atualizados exclusivamente dentro do
escopo desta subtarefa. Não criar, alterar ou executar testes automatizados.

## Contexto

A direção exigia ADR-013 e atualização do roadmap nesta subtarefa documental.
Esta execução concluiu ambas as entregas sem alterar runtime.

## Arquivos alterados

- `docs/decisions/adr-013-local-calendar-temporal-read-model.md`;
- `docs/roadmap/champion-companion-roadmap.md`;
- este arquivo;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`.

## Decisões e desvios

- ADR-013 foi criada como especialização compatível com ADR-005: a rota
  parametrizada `CalendarDay(localDateIso)` atende requisito concreto sem
  trocar Navigation 3, back stack manual, `NavHost` ou `NavController`.
- ADRs 003, 009, 010, 011 e 012 permanecem compatíveis; nenhuma ADR existente
  foi alterada.
- Roadmap preserva conteúdo de Fases 13 e 14 e adiciona apenas estados de
  conclusão/adiamento/prioridade.
- Ressalva de evidência incompleta do Gate C foi preservada no status da Fase
  12; não bloqueia Fase 15.

## Comandos executados

- `git status --short`
- `git branch --show-current`
- `git log --oneline -5`

Não foram executados build, lint ou testes automatizados. Esta subtarefa é
documental e determina explicitamente não executar testes.

## Resultados e evidências

- ADR-013 aceita formaliza read model local derivado, fluxo de camadas, ausência
  de tabela/migration/índice preventivo, semântica temporal, timezone,
  múltiplos dispositivos, fontes, fila, ausência/zero, navegação e proibições.
- ADR-013 referencia compatibilidade com ADRs 003, 005, 009, 010, 011 e 012.
- Roadmap marca Fase 12 como concluída, Fases 13/14 como adiadas e Fase 15 como
  próxima em execução, sem renumerar ou remover backlog.
- `CalendarDay(localDateIso)` foi reconciliada como extensão concreta da
  navegação manual existente, sem alteração de ADR-005.
- Diff documental não inclui Kotlin, Compose, Room, migration, teste, BLE,
  Health Connect ou outro arquivo de runtime.

## Screenshot

Não aplicável; task documental.

## Riscos residuais

- Gate C da Fase 12 continua com evidência documental incompleta; a ressalva foi
  preservada no roadmap.
- ADR-005 continua exigindo manutenção manual do back stack para a rota
  parametrizada concreta.
- Desempate de dispositivos, consistência eventual entre fontes e desempenho
  de queries em lote serão validados nas subtarefas de contratos, queries e
  read model.
- Implementação ainda precisa resolver N+1, timezone operacional e consultas
  temporais sem alterar schema.

## Atualização do overview

Item 015-02 marcado `[x]`; progresso recalculado para `2/13`. Demais itens
permanecem `[ ]` e nenhuma subtarefa seguinte foi iniciada.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
