# 015 — Calendário e histórico consolidado local

- **Status geral:** pending
- **Spec:** [015-calendar-consolidated-history.md](../../specs/015-calendar-consolidated-history.md)
- **Progresso:** 6/13 subtarefas concluídas

## Estado herdado

- Fase 12: concluída documentalmente, com Room como fonte visual, Health Connect
  write-only e fila local foreground.
- Fase 13: adiada.
- Fase 14: adiada.
- Fase 15: próxima fase.
- `CalendarScreen`: placeholder.
- Saúde: BPM funcional; passos e distância ainda shells.
- Exercícios: atividades importadas, revisadas, calibradas e com detalhe
  reutilizável.
- Room: versão 5.

## Objetivo

Construir calendário mensal, detalhe diário e históricos locais de passos e
distância usando read model derivado, sem tabela nova, sem migration e sem
acesso a BLE ou Health Connect.

## Fontes obrigatórias

- `docs/specs/_inputs/phase-15-calendar-consolidated-history-direction.md`;
- `docs/specs/015-calendar-consolidated-history.md`;
- `docs/baseline/current-baseline.md`;
- `docs/baseline/database-reference.md`;
- `docs/baseline/feature-parity-checklist.md`;
- `docs/baseline/gaps-and-open-questions.md`;
- roadmap do Champion Companion;
- ADRs 003, 005, 009, 010, 011 e 012;
- specs e overviews das Fases 07 a 12;
- código real em `core/model`, `core/database`, `data`, `feature/calendar`,
  `feature/health`, `feature/exercises` e `core/navigation`.

## Tasks e ordem

```text
015-01 → 015-02 → 015-03 → 015-04 → 015-05 → 015-06
                                                    ├→ 015-07 → 015-08 ─┐
                                                    ├→ 015-09 ──────────┼→ 015-11 → 015-12 → 015-13
                                                    └→ 015-10 ──────────┘
```

## Dependências

- 015-01 audita fatos e fronteiras.
- 015-02 formaliza decisões futuras.
- 015-03 estabiliza contratos e modelos.
- 015-04 implementa queries e projeções de persistência.
- 015-05 compõe read model consolidado.
- 015-06 estabiliza ViewModels e separa ownership visual.
- 015-07 e 015-08 implementam calendário e detalhe.
- 015-09 e 015-10 implementam históricos em frentes separadas.
- 015-11 integra estados, acessibilidade e acabamento.
- 015-12 valida manualmente offline.
- 015-13 fecha documentação e fase.

## Paralelismo

Após 015-06, 015-07, 015-09 e 015-10 podem avançar em paralelo. 015-08
depende de 015-07. Não paralelizar alterações nos mesmos hotspots: contratos,
DAOs, repositories, navegação, `HealthMetricDetailScreens.kt`,
`HealthViewModel.kt`, `CalendarScreen.kt` ou `CalendarViewModel.kt`.

## Hotspots

- `RepositoryContracts.kt`;
- `ChampionDaos.kt`;
- `DatabaseMappers.kt`;
- `RoomRepositories.kt`;
- `RepositoryModule.kt`;
- `AppDestination.kt`;
- `ChampionNavigation.kt`;
- `HealthMetricDetailScreens.kt`;
- `HealthViewModel.kt`;
- `CalendarScreen.kt` e futuro `CalendarViewModel`.

## Migration prevista

**Nenhuma.** O read model será derivado sobre entidades existentes. Não criar
tabela, coluna ou índice preventivo. Qualquer necessidade descoberta deve ser
registrada separadamente, com justificativa e decisão própria.

## Checklist

- [x] [015-01-audit-post-health-connect-data-sources-and-deferred-phase-boundaries.md](015-01-audit-post-health-connect-data-sources-and-deferred-phase-boundaries.md)
- [x] [015-02-formalize-local-calendar-read-model-adr-and-roadmap.md](015-02-formalize-local-calendar-read-model-adr-and-roadmap.md)
- [x] [015-03-define-calendar-history-contracts-date-semantics-and-ui-state.md](015-03-define-calendar-history-contracts-date-semantics-and-ui-state.md)
- [x] [015-04-add-calendar-range-queries-and-repository-projections.md](015-04-add-calendar-range-queries-and-repository-projections.md)
- [x] [015-05-implement-calendar-read-model-and-daily-consolidation.md](015-05-implement-calendar-read-model-and-daily-consolidation.md)
- [x] [015-06-implement-temporal-viewmodels-and-isolate-history-shells.md](015-06-implement-temporal-viewmodels-and-isolate-history-shells.md)
- [ ] [015-07-implement-month-calendar-grid-markers-and-accessibility.md](015-07-implement-month-calendar-grid-markers-and-accessibility.md)
- [ ] [015-08-implement-calendar-day-detail-and-navigation.md](015-08-implement-calendar-day-detail-and-navigation.md)
- [ ] [015-09-implement-steps-history-chart-and-list.md](015-09-implement-steps-history-chart-and-list.md)
- [ ] [015-10-implement-distance-history-chart-and-list.md](015-10-implement-distance-history-chart-and-list.md)
- [ ] [015-11-integrate-sync-status-empty-states-errors-and-visual-polish.md](015-11-integrate-sync-status-empty-states-errors-and-visual-polish.md)
- [ ] [015-12-run-manual-offline-calendar-validation.md](015-12-run-manual-offline-calendar-validation.md)
- [ ] [015-13-close-calendar-consolidated-history-phase.md](015-13-close-calendar-consolidated-history-phase.md)

## Observações

- `015-01` concluída com relatório documental reproduzível.
- `015-02` concluída com ADR-013 e roadmap alinhado à decisão de produto.
- `015-03` concluída com contratos temporais, read models imutáveis, semântica de
  datas, históricos 7/30/90 e estado/intents do calendário.
- `015-04` concluída com queries semiabertas de resumos, BPM, atividades e peso,
  projeção leve de atividades e consulta em lote da fila com chunking; schema v5
  permaneceu inalterado.
- `015-05` concluída com `LocalCalendarRepository` read-only, seleção
  determinística de resumo por dispositivo, agrupamento temporal, composição em
  lote das fontes locais e binding Hilt.
- `015-06` concluída com `CalendarViewModel` lifecycle-safe, observação passiva
  retryable e shells de passos/distância isolados em arquivos próprios.
- Não há bloqueio para `015-07`, `015-09` ou `015-10`; status `Planejada` no cabeçalho de `012-00`,
  status conflitante de `012-13` e evidência incompleta do Gate C permanecem
  riscos documentais da Fase 12.
- Nenhuma subtarefa posterior a `015-06` foi iniciada.

## Validação manual

No Galaxy S22, com Bluetooth desligado, relógio desconectado e Health Connect
indisponível ou sem permissão quando reproduzível, confirmar abertura,
navegação mensal, detalhe diário, históricos, ausência de prompts e ausência de
operações externas. Mudanças visuais exigem screenshots.

Não criar, alterar ou executar testes automatizados.

## Dívida e riscos

- `012-00` e `012-13` mantêm status textuais antigos apesar do fechamento da
  Fase 12; evidência do Gate C permanece incompleta.
- Roadmap registra Fases 13 e 14 como adiadas e Fase 15 como próxima.
- Query atual de resumo diário usa `BETWEEN` inclusivo.
- Política de seleção de dispositivo está formalizada na ADR-013 e implementada
  no read model; exposição visual ainda depende das subtarefas seguintes.
- `ExerciseDetail` é filho compartilhado e exige cuidado no back stack.
- Índices preventivos não serão adicionados sem evidência de necessidade.
- Timezone atual pode reprojetar atividades, BPM e peso para outra data civil.

## Critérios gerais

- Todas as subtarefas preservam escopo local read-only.
- Nenhuma task implementa Fase 13 ou 14.
- Nenhuma task acessa BLE ou Health Connect.
- Nenhuma migration é planejada inicialmente.
- Ausência nunca vira zero sintético.
- Progresso só muda quando subtarefa estiver concluída e evidenciada.

## Instruções de atualização

Cada subtarefa deve registrar status, arquivos alterados, decisões/desvios,
comandos, resultados/evidências e riscos residuais. Atualizar somente o item
correspondente deste checklist. Manter `[ ]` para `pending`, `in_progress` ou
`blocked`; marcar `[x]` apenas após aceite e validação manual. Não adicionar
outro checklist ao overview.
