# 015-01 — Auditar fontes pós-Health Connect e fronteiras adiadas

- **Status:** pending
- **Objetivo:** registrar baseline factual para Fase 15 sem implementar funcionalidade.
- **Resultado esperado:** auditoria reproduzível de Fase 12, dados locais,
  calendário placeholder, contratos, queries, timezone, dispositivos, agregados,
  fila, navegação, shells e Fases 13/14.

## Documentos obrigatórios

Spec 015, direção em `docs/specs/_inputs/`, AGENTS.md, baseline completa,
roadmap, todas ADRs, ADRs 009/011/012, spec/overview/tasks da Fase 12,
specs/overviews das Fases 07–11 e código real em `core/model`, `core/database`,
`data`, `feature/calendar`, `feature/health`, `feature/exercises` e navigation.

## Dependências e entradas

- Nenhuma subtarefa 015.
- Estado do branch e `main`.
- Fase 12 documentalmente concluída.
- Fases 13 e 14 adiadas por decisão de produto.

## Arquivos permitidos

- Novo relatório documental desta task.
- `015-00-overview.md`, apenas para evidência de planejamento se necessário.

## Arquivos proibidos

Kotlin, Compose, Room, migrations, ADRs, roadmap, specs concluídas, tasks
concluídas, testes, BLE, Health Connect e qualquer arquivo de runtime.

## Escopo

Auditar fechamento da Fase 12; status factual da baseline; placeholder do
calendário; `RepositoryContracts.kt`; queries de resumo/BPM/atividades/peso;
timezone; múltiplos dispositivos; DailySummary; BPM; atividades; pesos; fila
Health Connect; navegação; shells de passos/distância; duplicação; e adiamentos.
Planejar atualizações documentais futuras, sem executá-las.

## Fora do escopo

Qualquer implementação, correção de status em documentos existentes, criação de
ADR, alteração de roadmap, migration, teste ou validação física.

## Passos

1. Conferir Fase 12 e inconsistências residuais.
2. Catalogar fontes, campos e contratos reais.
3. Mapear semântica temporal e intervalos atuais.
4. Mapear lacunas e hotspots.
5. Registrar riscos de duplicação e N+1.
6. Registrar adiamentos de Fases 13/14.
7. Atualizar este arquivo com evidências.

## Critérios de aceite

- Auditoria cita caminhos e símbolos reais.
- Cada fonte solicitada pelo pedido está coberta.
- Calendário é identificado como placeholder.
- Fila é tratada como fonte de status, nunca de dado clínico.
- Fases 13/14 aparecem como adiadas.
- Nenhuma funcionalidade é implementada.

## Validação manual

Revisar arquivos auditados e comparar conclusões com branch atual e `main`.
Screenshot: não aplicável; task documental.

## Comandos de build

Nenhum build necessário para auditoria documental. Não executar testes.

## Evidências e atualização

Registrar arquivos lidos, divergências, decisões pendentes e risco residual.
Atualizar item 015-01 do overview somente quando aceite.

## Validação final

Confirmar ticker, caminhos, escopo read-only, adiamentos e ausência de mudança
em código. Não criar, alterar ou executar testes automatizados.

## Contexto

Fase 15 parte de dados locais pós-Fase 12, mas o calendário ainda é placeholder.

## Arquivos alterados

Inicialmente nenhum; registrar arquivos documentais alterados durante execução.

## Decisões e desvios

Registrar decisões de escopo, divergências factuais e qualquer desvio aprovado.

## Screenshot

Não aplicável; task documental.

## Riscos residuais

Status conflitante de `012-13` e evidências incompletas do Gate C podem exigir
revisão documental posterior.

## Atualização do overview

Marcar somente o item 015-01 e recalcular progresso após aceite.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
