# Task 012-00 - Overview Health Connect nativo

## Status

Planejada em 2026-07-24.

## Status inicial

- Fase 11 considerada concluída como fato autoritativo desta fase.
- Código real já possui `READY_TO_SYNC`, revisão, calibração e descarte lógico.
- Documentação factual ainda está atrasada em relação ao código da Fase 11.
- `HealthSyncOperation` e `HealthSyncRepository` ainda cobrem somente backlog D1 de `HEART_RATE`.
- `SettingsScreen.kt` ainda é placeholder.
- `AppPreferences.healthSyncEnabled` já existe, mas ainda não participa de fluxo funcional.
- `ChampionDatabase` está em `v4`.
- `AndroidManifest.xml` ainda não contém permissões ou queries de Health Connect.
- `libs.versions.toml` e `app/build.gradle.kts` ainda não contêm o SDK do Health Connect.

## Estado herdado

- Fase 07: resumo diário local-first.
- Fase 08: medição manual e hub de Saúde.
- Fase 09: D1 local-first com fila `PENDING` para backlog futuro.
- Fase 10: D3/D4 até `PENDING_REVIEW`.
- Fase 11: revisão, calibração e `READY_TO_SYNC` locais.

## Discrepâncias documentais

- `docs/baseline/current-baseline.md` ainda descreve Fase 10 como última concluída.
- `docs/tasks/011-activity-review-calibration/011-00-overview.md` ainda mostra `011-12` e `011-13` pendentes.
- `docs/baseline/feature-parity-checklist.md` e `docs/baseline/gaps-and-open-questions.md` ainda não refletem toda a fronteira pós-Fase 11.
- `docs/roadmap/champion-companion-roadmap.md` ainda descreve Fase 12 com `Steps` de atividade e outros pontos que precisam de atualização futura.

## Objetivo

Controlar escopo, ordem, dependências, paralelismo, hotspots, migration, Gate C e fechamento documental da Fase 12, dedicada a implementar integração write-only com Health Connect via fila persistida e retry foreground, sem leitura remota e sem `WorkManager`.

## Fontes

- `AGENTS.md`
- `docs/specs/_inputs/phase-12-native-health-connect-direction.md`
- `docs/specs/012-native-health-connect.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`
- ADRs vigentes em `docs/decisions/`
- `docs/decisions/adr-010-d1-local-first-and-health-connect-phase-boundary.md`
- `docs/decisions/adr-011-activity-ready-to-sync-phase-boundary.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`

## Lista de tasks

1. `012-01-audit-phase-11-closure-health-connect-baseline-and-current-backlogs.md`
2. `012-02-formalize-health-connect-sync-policy-adr-and-roadmap.md`
3. `012-03-add-health-connect-sdk-manifest-rationale-and-availability.md`
4. `012-04-expand-sync-domain-record-types-identities-and-room-schema.md`
5. `012-05-implement-sync-queue-reconciliation-and-repository-contracts.md`
6. `012-06-implement-health-connect-gateway-and-steps-heart-profile-mappers.md`
7. `012-07-implement-workout-session-and-complementary-record-mappers.md`
8. `012-08-implement-health-connect-sync-coordinator-partial-results-and-retry.md`
9. `012-09-integrate-post-persistence-steps-manual-bpm-and-d1-triggers.md`
10. `012-10-integrate-explicit-workout-sync-and-workflow-transitions.md`
11. `012-11-create-health-connect-settings-permissions-and-manage-access-ui.md`
12. `012-12-integrate-sync-feedback-in-home-health-and-exercises.md`
13. `012-13-run-physical-health-connect-gate-c.md`
14. `012-14-close-native-health-connect-phase.md`

## Ordem

1. `012-01`
2. `012-02`
3. `012-03` e `012-04`
4. `012-05`
5. `012-06` e `012-07`
6. `012-08`
7. `012-09`, `012-10` e `012-11`
8. `012-12`
9. `012-13`
10. `012-14`

## Dependências

| Task | Depende de |
| --- | --- |
| `012-01` | leitura inicial completa |
| `012-02` | `012-01` |
| `012-03` | `012-02` |
| `012-04` | `012-02` |
| `012-05` | `012-04` |
| `012-06` | `012-03`, `012-05` |
| `012-07` | `012-03`, `012-05` |
| `012-08` | `012-06`, `012-07` |
| `012-09` | `012-08` |
| `012-10` | `012-08` |
| `012-11` | `012-08` |
| `012-12` | `012-09`, `012-10`, `012-11` |
| `012-13` | `012-12` |
| `012-14` | `012-13` |

## Paralelismo

- `012-03` e `012-04` podem avançar em paralelo depois de `012-02`.
- `012-06` e `012-07` podem avançar em paralelo quando contratos e schema estiverem estáveis.
- `012-09`, `012-10` e `012-11` podem avançar em paralelo depois do coordinator estabilizado.
- `012-12`, `012-13` e `012-14` devem permanecer sequenciais.

## Hotspots

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/preferences/AppPreferencesStore.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/settings/SettingsScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

## Migrações

- migration obrigatória a partir do schema `v4`;
- expansão de `health_sync_operations`;
- preservação de backlog D1 existente;
- possível congelamento de timezone para `DailySummary`;
- repair idempotente para operações antigas sem campos novos.

## Checklist da fase

- reconciliar fechamento documental da Fase 11;
- formalizar ADR de política da Fase 12;
- atualizar roadmap e baseline durante a fase;
- integrar SDK estável `connect-client:1.1.0`;
- modelar disponibilidade, permissões e rationale;
- expandir schema e contratos para fila granular;
- implementar gateway e mapeadores de passos, BPM, atividade, peso e altura;
- implementar coordinator foreground com partial e retry;
- integrar triggers pós-persistência de passos, manual e D1;
- integrar sync explícito de atividade;
- substituir placeholder de `Settings`;
- expor feedback secundário em Home, Saúde e Exercícios;
- registrar Gate C físico;
- fechar documentação final da fase.

## Gate C

Validação física obrigatória no Galaxy S22 com Health Connect real para:

- disponibilidade;
- permissões parciais;
- passos;
- update sem duplicação;
- BPM manual;
- backlog D1;
- atividade explícita;
- atividade calibrada;
- ausência de `Steps` duplicado;
- ausência de rota;
- falha parcial;
- retry;
- preferência pausada;
- recuperação após morte de processo;
- peso e altura quando elegíveis;
- privacidade.

## Dívida e riscos

- conflito documental ainda aberto sobre política de `PARTIAL` da Fase 11;
- timezone de `DailySummary` ainda não resolvido em schema;
- `HealthSyncRepository` atual é estreito e dividido em relação ao `HeartRateRepository`;
- fila atual não cobre atividade, passos, peso nem altura;
- idempotência remota depende de validação física;
- `Settings` começa praticamente do zero;
- não misturar ajustes não relacionados em theme, dynamic colors ou preferências já existentes.

## Critérios

- tasks executáveis em sessão nova;
- nenhuma task depende de contexto implícito anterior;
- nenhuma task cria, altera, executa ou recomenda testes automatizados;
- toda task informa arquivos permitidos e proibidos;
- toda task informa evidências e atualização do overview;
- fase fecha só após Gate C e documentação factual atualizada.

## Instruções de atualização

- ao concluir cada task, atualizar este overview com status e achados relevantes;
- manter ordem e dependências explícitas;
- registrar desvios reais de execução quando ocorrerem;
- não reabrir implementação funcional da Fase 11;
- manter esta fase estritamente dentro da fronteira write-only e foreground.
