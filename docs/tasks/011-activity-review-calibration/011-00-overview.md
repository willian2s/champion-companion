# Task 011-00 - Overview Revisão e calibração de atividades

## Status

Em andamento em 2026-07-24. `011-01`, `011-02`, `011-03`, `011-04`, `011-05`, `011-06`, `011-07`, `011-08`, `011-09`, `011-10` e `011-11` concluídas; `011-12` e `011-13` pendentes.

Observação de ordem real:

- `011-04` e `011-05` foram executadas antes de `011-03`.
- `011-03` foi concluída depois para formalizar contratos e estado de UI de forma documental e explícita, alinhando o que já havia sido introduzido parcialmente em `RepositoryContracts.kt` e o que ainda faltava na feature.

## Objetivo

Controlar escopo, ordem, dependências, paralelismo, conflitos, checklist, validação manual e fechamento documental da Fase 11, dedicada a levar atividades D3/D4 importadas de `PENDING_REVIEW` até `READY_TO_SYNC`, sem implementar Health Connect.

## Documentos autoritativos

- `AGENTS.md`
- `docs/specs/_inputs/phase-11-activity-review-calibration-direction.md`
- `docs/specs/011-activity-review-calibration.md`
- ADRs vigentes em `docs/decisions/`
- `docs/decisions/adr-010-d1-local-first-and-health-connect-phase-boundary.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- tasks concluídas da Fase 10
- specs e overviews das Fases 03 a 09
- `docs/protocol/workouts-d3-d4.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

## Status inicial

- `WorkoutActivity` já possui estados `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCING`, `SYNCED`, `SYNC_FAILED` e `DISCARDED`.
- `confirmReview(...)` já bloqueia `parserValidity != VALID`.
- `WorkoutActivityEntity` ainda não tem `title` nem `notes`.
- `ChampionDatabase` está na versão `3`.
- `ExercisesScreen.kt` ainda é lista read-only de pendências.
- `ExercisesViewModel.kt` ainda não possui filtros, detalhe, review ou calibração.
- `HomeViewModel.kt` e `HomeScreen.kt` ainda mostram pendências de `AppWorkoutSession`.
- `ChampionNavigation.kt` ainda não possui rota de detalhe de atividade importada.
- `RoomRepositories.kt` já preserva revisão e calibração na reimportação, mas não pode preservar `title` e `notes`.

## Achados de `011-01`

- `WorkoutActivity` já possui estados de workflow suficientes para a fase, mas ainda não possui `title` nem `notes` no domínio persistido.
- `WorkoutActivity.confirmReview(...)` já bloqueia `parserValidity != VALID` e já separa não esteira (`READY_TO_SYNC`) de esteira (`PENDING_CALIBRATION`).
- `TreadmillCalibration.forImportedActivity(...)` ainda exige `originalMeters > 0`; falta fallback por `estimatedMeters` e caminho com fator ausente para atividade importada.
- `WorkoutActivityEntity` ainda não possui `title` nem `notes`; `ChampionDatabase` continua em `v3`.
- `WorkoutActivityDao` ainda só observa `PENDING_REVIEW`; faltam lista ampla por workflow, detalhe observável por ID e resumo por workflow.
- `TreadmillCalibrationDao.findLatestForActivity(...)` já existe, mas ainda não sobe ao contract público.
- `RoomRepositories.planWatchWorkoutPersistence(...)` já preserva `confirmedType`, `workflowState`, `reviewedAt`, `calibrationId` e métricas calibradas na reimportação; ainda não pode preservar `title` e `notes`.
- `ExercisesFeatureContract.kt`, `ExercisesViewModel.kt` e `ExercisesScreen.kt` ainda cobrem só importação D3/D4 e lista read-only de pendências.
- `HomeViewModel.kt` e `HomeScreen.kt` ainda observam pendências de `AppWorkoutSession`, não `WorkoutActivity`.
- `AppDestination.kt` e `ChampionNavigation.kt` ainda não possuem destino por `activityId`.
- Baseline MotionLog confirma revisão manual e calibração antes do sync, mas descarte lá é exclusão persistida e fator separado de calibração não foi confirmado.

## Achados de `011-02`

- ADR `docs/decisions/adr-011-activity-ready-to-sync-phase-boundary.md` formalizou `READY_TO_SYNC` como saída obrigatória da Fase 11.
- Roadmap foi corrigido para remover sync funcional, retry do provider e responsabilidade por `SYNCING`, `SYNCED` e `SYNC_FAILED` da Fase 11.
- Roadmap agora reforça que a Fase 12 consome backlog local `READY_TO_SYNC` e usa tipo confirmado, título, observações e distância calibrada como entradas da integração real.
- Fronteira documental da fase ficou estável para seguir com contratos, migration, queries e UI local sem ambiguidade de escopo.

## Achados de `011-04`

- `WorkoutActivity` agora já contém `title` e `notes` como campos opcionais.
- `WorkoutActivityEntity` agora já contém `title` e `notes` como colunas opcionais.
- `ChampionDatabase` subiu de `v3` para `v4` com `AutoMigration(from = 3, to = 4)`.
- `RoomRepositories.planWatchWorkoutPersistence(...)` agora preserva `title` e `notes` durante reimportação, junto das demais decisões já preservadas.
- `app/schemas/.../4.json` foi exportado com `title` e `notes` nullable em `workout_activities`.

## Achados de `011-05`

- `WorkoutActivityRepository` agora já expõe lista por filtro, detalhe por `activityId` e resumo por workflow.
- `TreadmillCalibrationRepository` agora já expõe observação da calibração mais recente por atividade via snapshot de contrato.
- `WorkoutActivityDao` agora já possui queries reativas para lista completa, lista por workflow, detalhe por ID e resumo agregado por workflow.
- `TreadmillCalibrationDao` agora já possui observação reativa da calibração mais recente.
- `DatabaseMappers.kt` deixou de depender apenas do caminho específico de `toPendingReviewDomain(...)` e já reconstrói `WorkoutActivity` para múltiplos workflows observáveis.
- Hotspot confirmado: `RepositoryContracts.kt`, `ChampionDaos.kt`, `DatabaseMappers.kt` e `RoomRepositories.kt` ficam ainda mais compartilhados entre `011-05`, `011-06` e `011-07`.
- Como `011-05` ocorreu antes de `011-03`, a task `011-03` precisa consolidar e revisar formalmente os contratos já introduzidos, sem assumir automaticamente que a parte de UI state da fase já está resolvida.

## Achados de `011-03`

- `RepositoryContracts.kt` agora formaliza `SubmitWorkoutReviewInput` e `CalibrateImportedActivityInput` como inputs equivalentes da fase.
- `ExercisesFeatureContract.kt` agora separa explicitamente `selectedFilter`, `activityList`, `workflowSummary`, `selectedActivity`, `detailState`, `reviewFormState`, `calibrationFormState` e `operationError`, preservando `importState` como estado independente.
- `TreadmillCalibration.kt` agora já suporta fallback puro de fator por `estimatedMeters` e fator ausente quando não houver base positiva, alinhando o domínio ao escopo planejado sem tocar persistência nesta task.
- `011-03` consolidou documentalmente e em contratos o que `011-04` e `011-05` já haviam antecipado parcialmente.

## Achados de `011-06`

- `WorkoutActivity.submitReview(input)` agora centraliza validação e normalização de revisão no domínio.
- `RoomRepositories.submitReview(input)` agora persiste `confirmedType`, `title`, `notes`, `reviewedAt` e transição de workflow na mesma transação.
- `RoomRepositories.review(...)` foi mantido por compatibilidade e passou a delegar ao contrato novo sem apagar `title` ou `notes` existentes.
- Reimportação continua preservando `title`, `notes` e decisões já persistidas.

## Achados de `011-07`

- `RoomRepositories.calibrateImportedActivity(input)` agora executa fluxo atômico completo de calibração para atividade importada.
- A próxima revisão usa `latest + 1`, preservando `revision = 1` como primeira calibração.
- Fator segue `original`, senão `estimated`, senão ausente, sem bloquear distância calibrada.
- A atividade é atualizada para `READY_TO_SYNC` no mesmo write da calibração.

## Achados de `011-08`

- `ExercisesViewModel` agora concentra estado do fluxo local de review/calibração, sem acoplar formulários ao coordinator BLE.
- `ExercisesFeatureContract.kt` e `ExercisesViewModel.kt` agora possuem intents para filtro, abertura/fechamento de detalhe, edição local de formulários, submit de review, submit de calibração, descarte e dismiss de erro.
- Importação D3/D4 permaneceu separada do fluxo local e continuou usando `watchWorkoutImportCoordinator` apenas para importação.

## Achados de `011-09`

- `AppDestination.kt` agora inclui destino de detalhe parametrizado por `activityId` em formato string estável.
- `ChampionNavigation.kt` agora mantém subárvore manual de Exercícios, remove destinos de detalhe ao trocar de aba e preserva seleção da aba Exercícios durante o detalhe.
- `ExercisesScreen.kt` agora preserva área de importação, exibe resumo por workflow, filtros planejados, cards clicáveis e shell inicial do detalhe para preparar `011-10`.
- Hotspots confirmados: `ExercisesScreen.kt` concentra formatação e fallback visual da lista; `ChampionNavigation.kt` concentra regras de subfluxo e limpeza do back stack de Exercícios.
- Ajuste posterior: `ExercisesViewModel.kt` e `ExercisesScreen.kt` precisaram alinhar filtro inicial em `ALL` e trocar faixa aberta de chips por seletor em modal para evitar ruído visual e inconsistência entre contagem e lista.
- Ajuste posterior adicional: `ChampionDaos.kt` precisou alinhar lista e resumo ao mesmo subconjunto de atividades importadas com `watch_workout_imports`, evitando contagem visível sem itens carregáveis na lista.

## Achados de `011-10`

- `ExercisesScreen.kt` agora concentra tela de detalhe real com cabeçalho, métricas, gráfico local de BPM, intervalos e formulários conectados ao `ExercisesViewModel`.
- Risco visual conhecido: `ExercisesScreen.kt` ficou hotspot ainda maior, acumulando lista, detalhe, gráfico e formulários no mesmo arquivo até extração futura consciente.
- Risco de acessibilidade conhecido: gráfico tem resumo textual explícito, mas ainda depende de validação manual final em device para legibilidade e densidade visual.
- Regra de associação intervalo/BPM ficou conservadora: só usa amostras dentro da janela temporal acumulada de cada intervalo; fora disso, mostra degradação segura sem inventar média.
- Ajuste posterior: `PARTIAL` voltou a poder seguir revisão local e alcançar `READY_TO_SYNC`, alinhando fluxo ao comportamento anterior do app e removendo bloqueio excessivo introduzido na Fase 11.
- Ajuste posterior adicional: detalhe passou a tratar treino livre com métricas mais enxutas, intervalos em modal sob demanda e chips empilhados em coluna para evitar quebra visual.

## Achados de `011-11`

- `HomeViewModel.kt` agora observa `WorkoutWorkflowSummary` e produz banner de pendências locais com prioridade para calibração.
- `HomeScreen.kt` agora oferece atalho direto `Home -> Exercícios` pelo banner, sem acoplar Home ao detalhe de uma atividade específica.
- `ChampionNavigation.kt` agora diferencia confirmação de saída para importação em andamento e para rascunho não salvo do detalhe de Exercícios.
- `ExercisesScreen.kt` agora confirma descarte explicitamente antes de marcar treino como descartado.
- Risco residual aceito antes da validação manual final: ainda existe warning antigo de unchecked cast em `ExercisesViewModel.kt` sem relação funcional com a task.
- Follow-up técnico pendente: substituir o `combine(...)` indexado de `ExercisesViewModel.kt` por composição tipada para remover warning de unchecked cast em `values[3] as List<WorkoutActivity>`.

## Estado herdado da Fase 10

- Importação D3/D4 local-first entregue até `PENDING_REVIEW`.
- Importação explícita, sem auto-start.
- Persistência por bloco, deduplicação por `(deviceId, origin, crc)` e reimportação funcional.
- Intervalos, BPM e metadados diagnósticos privados já persistidos.
- Fronteira correta entre importação local e sincronização real já explicitada pela Fase 10 e pela ADR-010.

## Fronteira com Fase 12

- Fase 11 termina em `READY_TO_SYNC`.
- Fase 12 assume `SYNCING`, `SYNCED`, `SYNC_FAILED` e Health Connect real.
- Nenhuma task desta fase deve planejar cliente Health Connect, permissões, records, fila, envio, retry do provider ou sincronização automática.

## Lista completa de tasks

1. `011-01-audit-review-domain-persistence-and-phase-boundaries.md`
2. `011-02-formalize-ready-to-sync-adr-and-roadmap.md`
3. `011-03-define-review-calibration-contracts-and-ui-state.md`
4. `011-04-add-activity-review-fields-and-room-migration.md`
5. `011-05-implement-activity-list-detail-and-workflow-queries.md`
6. `011-06-implement-review-persistence-and-state-transitions.md`
7. `011-07-implement-treadmill-calibration-workflow.md`
8. `011-08-extend-exercises-viewmodel-for-filters-review-and-calibration.md`
9. `011-09-implement-exercises-filters-list-and-detail-navigation.md`
10. `011-10-implement-activity-detail-review-metrics-chart-and-intervals.md`
11. `011-11-integrate-home-banner-discard-validation-and-safe-errors.md`
12. `011-12-run-manual-activity-review-validation.md`
13. `011-13-close-activity-review-calibration-phase.md`

## Ordem

1. `011-01`
2. `011-02`
3. `011-03`
4. `011-04`
5. `011-05`
6. `011-06`
7. `011-07`
8. `011-08`
9. `011-09`
10. `011-10`
11. `011-11`
12. `011-12`
13. `011-13`

## Dependências

| Task | Depende de |
| --- | --- |
| `011-01` | leitura inicial completa |
| `011-02` | `011-01` |
| `011-03` | `011-02` |
| `011-04` | `011-03` |
| `011-05` | `011-04` |
| `011-06` | `011-03`, `011-04` |
| `011-07` | `011-03`, `011-04` |
| `011-08` | `011-05`, `011-06`, `011-07` |
| `011-09` | `011-08` |
| `011-10` | `011-09` |
| `011-11` | `011-08`, `011-09`, `011-10` |
| `011-12` | `011-11` |
| `011-13` | `011-12` |

## Paralelismo

- `011-05`, `011-06` e `011-07` ficam conceitualmente paralelizáveis depois de `011-04` estabilizar contratos e migration.
- Na prática, essas três tasks compartilham muitos arquivos críticos. Paralelizar só se houver coordenação fina por branch e merge rápido.
- `011-08` a `011-11` devem permanecer sequenciais.
- `011-12` e `011-13` são sequenciais.

## Arquivos com risco de conflito

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/TreadmillCalibration.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/AppDestination.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/3.json`
- próximo schema exportado `4.json`

## Checklist

- [x] `011-01` auditar domínio, Room, queries, Home, filtros, navegação, MotionLog e fronteira F11/F12
- [x] `011-02` planejar ADR e corrigir roadmap durante implementação
- [x] `011-03` congelar contratos, filtros, estados e limites funcionais
- [x] `011-04` adicionar `title` e `notes` com migration v4 e schema exportado
- [x] `011-05` implementar queries de lista, detalhe, resumo e calibração ativa
- [x] `011-06` implementar review atômico, descarte e preservação na reimportação
- [x] `011-07` implementar calibração atômica e métricas derivadas
- [x] `011-08` evoluir `ExercisesViewModel` e contratos da feature
- [x] `011-09` implementar filtros, lista clicável e navegação para detalhe
- [x] `011-10` implementar detalhe, gráfico, intervalos e formulários
- [x] `011-11` integrar banner Home, confirmações, validações e erros seguros
- [ ] `011-12` executar gate e validação manual em Galaxy S22
- [ ] `011-13` fechar spec, overview, roadmap, ADR e documentação factual necessária

## Comandos

Gate obrigatório da implementação:

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Roteiro manual

1. abrir Home;
2. confirmar banner;
3. tocar no banner;
4. abrir Exercícios;
5. alternar filtros;
6. abrir detalhe;
7. revisar atividade não esteira;
8. revisar atividade como esteira;
9. calibrar;
10. confirmar persistência após reinício;
11. reimportar;
12. confirmar preservação das decisões;
13. abrir atividade `PARTIAL`;
14. descartar;
15. confirmar filtro `Descartadas`;
16. confirmar ausência de fluxo funcional de Health Connect;
17. confirmar ausência de novos ou alterados testes.

## Dívida

- integração Health Connect real só na Fase 12;
- reabertura da revisão após sincronização futura;
- recalibração com múltiplas revisões visíveis;
- sessão de exercício iniciada pelo app;
- foreground service e background.

## Critérios gerais

- nenhuma task cria, altera ou executa testes automatizados;
- nenhuma task mistura escopo de Health Connect real;
- nenhuma task quebra fronteira `READY_TO_SYNC`;
- reimportação preserva decisões do usuário;
- `PARTIAL` nunca avança para pronto;
- `title` e `notes` entram só como expansão necessária demonstrada da persistência;
- mudanças visuais exigem screenshot no PR;
- overview deve ser atualizado em toda task executada.

## Instruções de atualização

- ao concluir cada task, marcar status real neste overview;
- registrar novos hotspots ou dependências descobertas;
- atualizar checklist e riscos aceitos;
- se a task alterar decisões duradouras, sincronizar spec, overview, ADR e roadmap no mesmo trabalho;
- manter texto em português brasileiro;
- manter regra explícita em toda task: `Não criar, alterar ou executar testes automatizados.`
