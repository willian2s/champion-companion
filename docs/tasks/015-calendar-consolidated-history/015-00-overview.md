# 015 — Calendário e histórico consolidado local

- **Status geral:** em andamento
- **Spec:** [015-calendar-consolidated-history.md](../../specs/015-calendar-consolidated-history.md)
- **Progresso:** 11/14 subtarefas concluídas

## Estado herdado

- Fase 12: concluída documentalmente, com Room como fonte visual, Health Connect
  write-only e fila local foreground.
- Fase 13: adiada.
- Fase 14: adiada.
- Fase 15: próxima fase.
- `CalendarScreen`: grade mensal local, seleção e entrada para detalhe diário.
- Saúde: BPM funcional; históricos locais de passos e distância funcionais,
  aguardando dados de Perfil para exibir estimativas positivas de distância.
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
                                                    │         └→ 015-14 ┤
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
- 015-14 inclui amostras reais de FC de atividades no histórico dedicado e pode
  ser executada após 015-07, sem depender de 015-08 a 015-10.

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
- [x] [015-07-implement-month-calendar-grid-markers-and-accessibility.md](015-07-implement-month-calendar-grid-markers-and-accessibility.md)
- [x] [015-08-implement-calendar-day-detail-and-navigation.md](015-08-implement-calendar-day-detail-and-navigation.md)
- [x] [015-09-implement-steps-history-chart-and-list.md](015-09-implement-steps-history-chart-and-list.md)
- [x] [015-10-implement-distance-history-chart-and-list.md](015-10-implement-distance-history-chart-and-list.md)
- [ ] [015-11-integrate-sync-status-empty-states-errors-and-visual-polish.md](015-11-integrate-sync-status-empty-states-errors-and-visual-polish.md)
- [ ] [015-12-run-manual-offline-calendar-validation.md](015-12-run-manual-offline-calendar-validation.md)
- [ ] [015-13-close-calendar-consolidated-history-phase.md](015-13-close-calendar-consolidated-history-phase.md)
- [x] [015-14-include-workout-heart-rate-in-dedicated-history.md](015-14-include-workout-heart-rate-in-dedicated-history.md)

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
- `015-07` concluída com grade mensal própria, indicadores, seleção, navegação,
  estados seguros e descrições acessíveis; screenshots e build registrados na
  subtarefa. Refinamento posterior confirmou contraste explícito nos números,
  borda de Hoje e gaps horizontais iguais aos verticais. Correção posterior
  removeu largura forçada que causava overflow; TalkBack falado e landscape
  permanecem riscos residuais, com sobreposição da navegação inferior em
  landscape, sem bloqueio de contrato. Compactação posterior
  reduziu células para 56 dp, removeu empilhamento visual de Hoje com marcadores
  e adicionou respiro interno entre grade e bordas do card. Indicadores finais
  usam pontos azul/verde/amarelo para passos, FC e atividade física.
- Não há bloqueio para `015-11`; status `Planejada` no cabeçalho de `012-00`,
  status conflitante de `012-13` e evidência incompleta do Gate C permanecem
  riscos documentais da Fase 12.
- `015-14` foi adicionada para o novo requisito explícito de consolidar amostras
  reais de atividades não descartadas no histórico dedicado de frequência
  cardíaca. Implementação, build, screenshot e validação manual foram concluídos;
    usuário confirmou funcionamento esperado. Itens `015-11` a `015-13`
    permanecem não iniciados.
- `015-09` concluída com ViewModel temporal local, períodos `7D`, `30D` e `90D`,
  métricas calculadas sem preencher lacunas, gráfico de barras acessível e lista
  no padrão do histórico de FC, ordenada do mais recente para o mais antigo e
  paginada em blocos de 10 dias. Validação no Galaxy S22 registrou seis lacunas,
  um registro abaixo da meta e screenshots de chips, gráfico, lista e paginação;
  zero real, TalkBack e rotação permanecem riscos manuais residuais.
- `015-10` concluída com ViewModel temporal local, períodos `7D`, `30D` e `90D`,
  agregados exclusivos de `DailySummary.estimatedDistanceMeters`, gráfico com
  lacunas e lista acessível com origem, atualização e indicação de estimativa.
  O Galaxy S22 não possui dados pessoais suficientes para produzir valores de
  distância; estado vazio foi preservado e a validação com valores positivos
  ficou registrada como débito D-015-10 para a implementação de Perfil.
- `015-08` concluída com destino `CalendarDay(localDateIso)`, detalhe diário
  local, navegação contextual para métricas e `ExerciseDetail`, back stack
  persistível, estados seguros para falha da fila e screenshots no Galaxy S22.
  Validação offline com Bluetooth desligado confirmou calendário, dia com dados,
  dia sem dados, BPM, atividade, back e rotação; parsing externo inválido e
  morte completa do processo permanecem riscos residuais documentados na
  subtarefa.
- Débito D-015-10: após implementar Perfil com altura ou passada válida, gerar e
  persistir estimativas em `DailySummary.estimatedDistanceMeters` e repetir a
  validação física do histórico de distância com dias estimados e sem estimativa.

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
