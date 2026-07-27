# 015-01 — Auditar fontes pós-Health Connect e fronteiras adiadas

- **Status:** concluída em 27/07/2026.
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

- `docs/tasks/015-calendar-consolidated-history/audit-report-015-01.md`;
- este arquivo;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`.

## Decisões e desvios

- Auditoria permaneceu read-only para runtime, conforme escopo permitido.
- Fase 12 foi tratada como concluída para fins de planejamento, apesar do status
  textual atrasado de `012-13` e da evidência incompleta do Gate C.
- Não foram alterados ADRs, roadmap, specs concluídas, código ou testes. A
  atualização de ADR-013 e roadmap fica planejada para 015-02.

## Comandos executados

- `git status --short`
- `git branch --show-current`
- `git log --oneline -5`
- `git diff --stat main...HEAD`

Não foram executados build, lint ou testes automatizados. A task determina
auditoria documental sem build e proíbe testes.

## Resultados e evidências

- Relatório reproduzível em
  `docs/tasks/015-calendar-consolidated-history/audit-report-015-01.md`.
- `CalendarScreen` confirmado como placeholder em
  `feature/calendar/CalendarScreen.kt:3-7`.
- `DailySummary`, BPM, atividades, peso e fila local foram mapeados com
  entities, DAOs, contracts, repositories e mappers reais.
- Intervalo inclusivo atual de resumo, intervalo semiaberto já existente para
  BPM, timezone baseado em `systemDefault()`, seleção ausente de dispositivo e
  riscos N+1 foram registrados com caminhos e símbolos.
- Fila Health Connect foi tratada somente como fonte de status local; provider
  externo não participa da projeção temporal.
- Fases 13 e 14 foram confirmadas como adiadas na spec/direção/overview da
  Fase 15; desalinhamento do roadmap foi registrado como trabalho futuro.
- Comparação com `main` confirmou ausência de implementação de calendário na
  branch atual e em `main`.
- Diff final deve permanecer restrito a documentação desta subtarefa e do
  overview.

## Screenshot

Não aplicável; task documental.

## Riscos residuais

- `012-00` mantém `Planejada` no cabeçalho apesar do fechamento no próprio
  documento; `012-13` também mantém status textual conflitante com o fechamento
  da Fase 12.
- Evidências do Gate C são declarativas e incompletas para reprodução.
- Roadmap ainda precisa registrar adiamento de Fases 13 e 14 em 015-02.
- Seleção de dispositivo, queries de intervalo para atividades/pesos, sync em
  lote e resolução dos hotspots N+1 continuam pendentes.

## Atualização do overview

Marcar somente o item 015-01 e recalcular progresso após aceite.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
