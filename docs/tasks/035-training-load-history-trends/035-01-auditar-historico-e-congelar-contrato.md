# 035-01 — Auditar histórico e congelar contrato

- **Ticker:** `035`
- **Número:** `01`
- **Status inicial:** `pending`
- **Status atual:** `completed`

## Objetivo e resultado esperado

Auditar estado real da branch, main, Spec 034, Room, policy cardiovascular,
perfil pessoal, tela de Exercícios e acesso temporal. Congelar decisões
materiais da Spec 035 antes de qualquer implementação.

Resultado: contrato histórico documentado, riscos conhecidos e caminho bounded
recomendado sem migration. Auditoria executada em 2026-09-21.

## Requisitos cobertos

- Confirmar branch existente e base com Spec 034 mergeada.
- Confirmar Room v20 e ausência de necessidade factual de migration.
- Reutilizar `WorkoutHeartRateAnalysisPolicy` e
  `WorkoutHeartRateAnalysis` como fonte única.
- Confirmar `maxHeartRateBpm` no perfil e no snapshot pessoal.
- Auditar histórico Semana/Mês de Exercícios.
- Auditar queries, repositories, DAO, samples e mappers.
- Definir população, janelas, cobertura, estados, comparação e bounded read.

## Escopo incluído

- Leitura de `AGENTS.md`, baseline, direção pós-Fase 15, Spec/ADR/overview e
  evidência física da Spec 034.
- Inspeção de código em `core.model`, `core.database`, `data` e
  `feature.exercises`.
- Consulta ao subagente de exploração para contratos 034, Room e Exercícios.
- Consulta arquitetural para comparar leitura completa bounded, summary + IDs e
  DAO/read model dedicado.
- Registro de discrepância documental baseline v19 versus código/schema v20.

## Escopo excluído

- Qualquer alteração em código, schema, migration, teste ou UI.
- Correção do baseline histórico.
- Criação de fixture, workout ou sample.
- Execução de build de produto.

## Dependências

- `main` com merge da Spec 034.
- `docs/specs/034-heart-rate-zones-training-load.md`.
- `docs/decisions/adr-034-heart-rate-zones-training-load.md`.
- Evidência `docs/integration/034-06-physical-validation.md`.
- `AGENTS.md` e `docs/roadmap/post-phase-15-direction.md`.

## Arquivos e símbolos auditados

- `WorkoutHeartRateAnalysisPolicy`, `WorkoutHeartRateAnalysis` e estados;
- `UserProfile.maxHeartRateBpm`, `PersonalMetricsSnapshot` e update local;
- `ChampionDatabase` v20, `MIGRATION_19_20` e schema `20.json`;
- `WorkoutActivityRepository`, `RoomRepositories`, `ChampionDaos`;
- `CalendarWorkoutSummary`, `WorkoutActivity` e mappers;
- `ExercisesScreen`, `ExercisesViewModel`, `ExercisesUiState` e
  `ExerciseDetailState`;
- `workout_heart_rate_samples` e leituras por activity.

## Passos executados

1. Confirmar status limpo e branch
   `feat/035-training-load-history-trends`.
2. Confirmar `HEAD` no merge da Spec 034, alinhado a `main`/`origin/main`.
3. Confirmar que a Spec 034 está `completed`, com seis subtarefas concluídas e
   evidência física existente.
4. Confirmar no código `ChampionDatabase.version = 20`, schema v20 e migration
   19 -> 20 limitada a `maxHeartRateBpm`.
5. Confirmar policy 034, janela canônica, sample-and-hold, cobertura, estados e
   Edwards TRIMP.
6. Confirmar FC máxima pessoal atual no perfil e recomposição local existente.
7. Confirmar histórico Semana/Mês, filtros, navegação e estado local da tela.
8. Confirmar que summaries bounded não têm samples e que leitura global não é
   adequada para a janela de 56 dias.
9. Avaliar alternativas de acesso com subagentes; escolher contrato repository
   bounded de atividades completas, com fluxo agregado e leituras batched.
10. Registrar decisões em Spec 035 e ADR-035.

## Decisões congeladas

- Janelas semiabertas de 7/14/28/56 dias, sempre relativas a `asOf` explícito.
- População exclusiva `WATCH_D3_D4` e `workflowState != DISCARDED`.
- `startedAt` define a janela; workout não é dividido.
- Cobertura usa soma de `coveredDuration` sobre duração total elegível; sem
  threshold mínimo.
- `AVAILABLE` é necessário para contar workout analisado e somar carga; carga
  só vem de `cardiovascularLoad` não nulo.
- Baseline zero vira `NO_PREVIOUS_BASELINE`, nunca infinito.
- Leitura recomendada: contrato bounded de atividades completas, sem N+1
  reativo, mantendo ViewModel distante do DAO.
- Room permanece v20; migration, tabela e snapshot de carga não fazem parte da
  035.
- Exercícios recebe seção na superfície atual; Semana/Mês e filtros continuam
  separados da população analítica.

## Testes e comandos de validação

Executados para auditoria:

- `git status --short --branch`;
- `git log --oneline --decorate`;
- inspeção de `HEAD` e parents;
- buscas e leitura dos contratos/caminhos citados.

Validação documental da subtarefa:

- Spec 034, ADR-034 e overview coerentes com código atual;
- Room v20 confirmado por código, schema e evidência física;
- nenhum arquivo de produto alterado.

Não criar testes automatizados nesta subtarefa.

## Definição de pronto

- [x] Branch/base e status auditados.
- [x] Room v20 confirmado.
- [x] Policy 034 e FC máxima confirmadas.
- [x] Histórico Semana/Mês confirmado.
- [x] Data path bounded e alternativas avaliadas.
- [x] População, ranges, cobertura, estados e comparação congelados.
- [x] Spec 035 e ADR-035 registram decisões e riscos.

## Riscos e cuidados

- Baseline documental v19 não representa estado real; não usar como fonte de
  versão durante implementação.
- Query de `startedAt` não possui índice temporal composto; medir antes de
  propor schema.
- Não tratar `CalendarWorkoutSummary` como atividade completa.
- Não transformar `NO_HEART_RATE_SAMPLES`, configuração ausente ou erro em
  carga zero.
- Não expor BPM, samples, identificadores pessoais ou payloads na documentação.
