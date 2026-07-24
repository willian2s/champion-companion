# Task 012-01 - Auditar fechamento da Fase 11, baseline Health Connect e backlogs atuais

## Status

Concluída em 2026-07-24.

## Objetivo

Reconciliar documentalmente a conclusão da Fase 11, atualizar baseline factual da fase atual e auditar o backlog local real que a Fase 12 herdará antes de qualquer integração funcional com o Health Connect.

## Contexto

A direção da Fase 12 considera a Fase 11 concluída, mas documentação factual ainda está atrasada. O código real já contém backlog local relevante para Health Connect, incluindo operações D1 persistidas e atividades em `READY_TO_SYNC`.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/_inputs/phase-12-native-health-connect-direction.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

## Dependências

- nenhuma.

## Entradas

- código real em `core/model`, `core/database`, `core/preferences`, `data`, `feature/home`, `feature/health`, `feature/exercises`, `feature/settings`;
- `AndroidManifest.xml`;
- `app/build.gradle.kts`;
- `gradle/libs.versions.toml`;
- schema exportado `v4`.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-01-audit-phase-11-closure-health-connect-baseline-and-current-backlogs.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

## Arquivos proibidos

- `app/src/main/java/**`
- `app/src/test/**`
- `app/src/androidTest/**`
- `docs/decisions/**`
- `docs/roadmap/**`

## Escopo

- reconciliar fechamento documental da Fase 11;
- atualizar baseline factual;
- auditar fila D1 existente;
- auditar medições manuais sem operação;
- auditar resumos diários sem operação;
- auditar atividades `READY_TO_SYNC`;
- auditar peso e altura locais;
- auditar schema `v4`;
- auditar manifesto;
- auditar catálogo de versões;
- auditar `healthSyncEnabled`;
- atualizar overview da Fase 12.

## Fora do escopo

- implementar SDK do Health Connect;
- alterar Kotlin, Compose, Room, Gradle ou Manifest;
- alterar ADR;
- alterar roadmap.

## Passos

1. Confirmar estado real entregue pela Fase 11 no código.
2. Alinhar overview e documentos factuais sem reabrir implementação funcional.
3. Auditar backlog local herdado para passos, manual, D1, atividades, peso e altura.
4. Registrar limitações atuais do schema `v4`, manifesto, catálogo e preferência `healthSyncEnabled`.
5. Atualizar spec e overview da Fase 12 com achados factuais.

## Critérios de aceite

- Fase 11 fica marcada como concluída nos documentos da fase apropriados;
- baseline factual reflete estado real do código;
- backlog herdado fica mapeado por tipo;
- limitações de manifesto, catálogo, schema e preferência ficam explícitas;
- nenhuma implementação funcional de Health Connect é introduzida.

## Validação manual

- conferência textual de referências citadas;
- revisão do diff documental restrito à Fase 11, baseline e Fase 12.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- lista de arquivos auditados;
- backlog local herdado categorizado;
- diff restrito à documentação.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `012-01` como concluída;
- registrar discrepâncias documentais reconciliadas;
- registrar backlog herdado confirmado.

## Execução

### Arquivos auditados

- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/TreadmillCalibration.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/AppPreferences.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/preferences/AppPreferencesStore.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/settings/SettingsScreen.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/4.json`

### Achados factuais confirmados

- Fase 11 já está concluída no código com revisão, calibração, descarte lógico e atividades em `READY_TO_SYNC`.
- `011-00-overview.md` estava atrasado, ainda listando `011-12` e `011-13` como pendentes e mantendo regra antiga de que `PARTIAL` nunca chegaria a pronto.
- `current-baseline.md` ainda tratava a Fase 10 como última concluída e ignorava revisão local, calibração, detalhe de atividade e backlog real da Fase 12.
- `HealthSyncOperation` real ainda cobre apenas `HEART_RATE` e backlog D1.
- `HealthSyncRepository` real ainda expõe apenas operações de falha e transições básicas, sem API única de fila processável.
- `DailySummary`, medição manual, peso e altura ainda não possuem operação funcional equivalente garantida na fila.
- `WorkoutActivity` já contém estados `READY_TO_SYNC`, `SYNCING`, `SYNCED`, `SYNC_FAILED` e `DISCARDED`, mas o fluxo funcional atual ainda termina localmente em `READY_TO_SYNC`.
- `ChampionDatabase` real permanece em `v4`.
- `AndroidManifest.xml` ainda não possui permissões write-only, queries do provider nem rationale Activity do Health Connect.
- `gradle/libs.versions.toml` e `app/build.gradle.kts` ainda não contêm `androidx.health.connect:connect-client:1.1.0`.
- `AppPreferences.healthSyncEnabled` já existe em DataStore e segue sem uso funcional.
- `SettingsScreen.kt` continua placeholder.

### Backlog herdado confirmado

- D1: backlog persistido com operações `PENDING` ou `FAILED` em `health_sync_operations`, já ligado a `HeartRateMeasurement` válido de origem `WATCH_D1_HISTORY`.
- BPM manual: medições válidas persistidas localmente, mas ainda sem garantia de operação de sync equivalente.
- Passos: `DailySummary` persistido localmente por dia, mas ainda sem operação de sync equivalente garantida.
- Atividades: backlog local em `READY_TO_SYNC`, aguardando ação explícita futura e ainda sem operações granulares persistidas por record.
- Peso e altura: dados locais elegíveis já existem na modelagem (`weight_measurements` e `user_profiles`), mas ainda sem pipeline funcional de sync.

### Reconciliação documental aplicada

- overview da Fase 11 alinhado para refletir fechamento documental e regra correta de `PARTIAL`.
- baseline factual atualizada para incluir Fase 11 como concluída e backlog real pré-Fase 12.
- checklist de paridade atualizado para refletir revisão, calibração e descarte lógico já entregues localmente.
- lacunas e mapping Health Connect atualizados com estado factual pré-Fase 12.
- `database-reference.md` recebeu nota factual do schema real `v4` e da fila atual limitada a D1.
- spec e overview da Fase 12 atualizados com achados de auditoria.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
