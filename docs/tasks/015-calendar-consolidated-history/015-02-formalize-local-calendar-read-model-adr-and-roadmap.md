# 015-02 — Formalizar read model local, ADR e roadmap

- **Status:** pending
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

A direção exige ADR-013 e atualização de roadmap durante implementação futura,
mas esta execução cria somente o planejamento.

## Arquivos alterados

Inicialmente nenhum; registrar documentos alterados durante execução futura.

## Decisões e desvios

Registrar decisão sobre compatibilidade com ADR-005 e qualquer desvio aprovado.

## Screenshot

Não aplicável; task documental.

## Riscos residuais

Roadmap e ADR podem permanecer temporariamente desalinhados até execução desta
task.

## Atualização do overview

Marcar somente o item 015-02 após revisão e aceite de 015-01.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
