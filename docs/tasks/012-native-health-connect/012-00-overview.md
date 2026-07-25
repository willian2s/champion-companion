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

## Achados de `012-01`

- Fase 11 foi reconciliada como concluída documentalmente, sem reabertura funcional.
- `current-baseline.md` foi alinhada ao estado real do código, incluindo revisão, calibração, descarte lógico e `READY_TO_SYNC`.
- Backlog herdado factual para a Fase 12 foi mapeado como:
  - D1 com operações `PENDING` ou `FAILED` já persistidas;
  - medições manuais válidas sem operação garantida;
  - resumos diários válidos sem operação garantida;
  - atividades `READY_TO_SYNC` aguardando ação explícita futura;
  - peso e altura locais elegíveis sem pipeline funcional de sync.
- `ChampionDatabase` permanece em `v4`.
- `AndroidManifest.xml` segue sem integração Health Connect.
- `libs.versions.toml` e `app/build.gradle.kts` seguem sem o SDK `connect-client`.
- `healthSyncEnabled` já existe em DataStore e segue sem uso funcional.

## Achados de `012-02`

- ADR `docs/decisions/adr-012-health-connect-write-only-and-granular-sync.md` formalizou política write-only, local-first e granular da Fase 12.
- Roadmap da Fase 12 foi alinhado para remover `Steps` de atividade, reforçar triggers assimétricos, explicitar permissões por capacidade e manter `WorkManager` fora do escopo.
- `health-connect-mapping.md` deixou de ser apenas leitura comparativa do MotionLog e passou a registrar explicitamente as diferenças deliberadas do app novo para a Fase 12.
- Fronteira documental da fase ficou estável para início da implementação funcional sem ambiguidades sobre IDs, versões, fila, atividade explícita e falha parcial.

## Achados de `012-03`

- SDK `connect-client:1.1.0` foi integrado ao catálogo e ao módulo `:app`.
- Manifesto passou a declarar permissões write-only do escopo da fase, `queries` do provider e rationale Activity com alias compatível com Android 14+.
- Nova infraestrutura Android de Health Connect foi criada em `core/healthconnect` para disponibilidade e intents de provider.
- `installDebug` foi validado com sucesso no dispositivo físico já conectado.

## Achados de `012-04`

- Domínio de sync agora comporta passos, BPM, atividade, peso e altura.
- `ChampionDatabase` subiu para `v5` com `AutoMigration(from = 4, to = 5)`.
- `daily_summaries` ganhou `collectedZoneId` nullable.
- `health_sync_operations` ganhou metadados de identidade estável, versão, sincronização, timezone e auditoria temporal, todos nullable para preservar backlog D1 existente.
- `DatabaseMappers.kt` agora reconstrói aggregates de sync além de `HeartRateMeasurement`, preparando a fila granular da Fase 12.

## Achados de `012-05`

- `HealthSyncRepository` foi estabilizado como API única da fila persistida, cobrindo ensure, busca processável, resumo observado, consulta por agregado, transições de estado, reset de interrupção e reconciliação de backlog.
- `RoomRepositories` passou a derivar `clientRecordId`, versão e metadados temporais por agregado compatível, incluindo repair idempotente de operações antigas sem identidade nova.
- A reconciliação genérica agora cobre backlog D1, medições manuais válidas, resumos diários válidos, pesos elegíveis e altura elegível do perfil atual.
- Atividades em `READY_TO_SYNC` continuam fora da reconciliação genérica e preservam trigger explícito próprio.
- `./gradlew :app:compileDebugKotlin` foi validado com sucesso após estabilização da fila.

## Achados de `012-06`

- `HealthConnectGateway` Android-free e implementação concreta Android foram adicionados sobre `HealthConnectClient`, cobrindo disponibilidade, permissões concedidas e insert write-only.
- `HealthRecordMapper` passou a mapear `DailySummary`, `HeartRateMeasurement`, `WeightMeasurement` e `UserProfile` para `StepsRecord`, `HeartRateRecord`, `WeightRecord` e `HeightRecord` com `clientRecordId` e versão reutilizados da fila.
- contratos mínimos de repositório ganharam busca por aggregate para resumo diário, BPM, peso, altura e atividade, preparando coordinator sem acoplar mapper a Room.

## Achados de `012-07`

- o mesmo `HealthRecordMapper` passou a mapear `WorkoutActivity` em records granulares de `ExerciseSessionRecord`, `DistanceRecord`, `TotalCaloriesBurnedRecord` e `HeartRateRecord`.
- a elegibilidade de atividade foi estabilizada por tipo de record, respeitando `READY_TO_SYNC|SYNC_FAILED`, ausência de `StepsRecord`, prioridade calibrada > original > estimada e amostras reais de BPM.
- `ParserValidity.PARTIAL` permanece elegível apenas quando sessão temporal é válida e os complementos realmente existem no aggregate local.
- `./gradlew :app:compileDebugKotlin` foi validado com sucesso após gateway e mapeadores.

## Achados de `012-08`

- `HealthConnectSyncCoordinator` foi estabilizado com mutex foreground, limite padrão de 100 operações, repair de interrupção e resultados tipados para backlog geral e sync explícito de atividade.
- o coordinator agora orquestra preferência `healthSyncEnabled`, disponibilidade do provider, permissões write-only, mapeamento por agregado, `insertRecords`, retry idempotente e transições da fila.
- falha parcial de atividade passou a preservar operações já sincronizadas, marcar complementos falhos individualmente e reconciliar workflow final em `SYNCED` ou `SYNC_FAILED`.
- `resetInterrupted` da fila foi combinado com repair do workflow local de atividades em `SYNCING`, evitando estados presos após morte de processo.
- `012-09`, `012-10` e `012-11` podem avançar em paralelo a partir deste ponto, usando coordinator já estabilizado.
- `./gradlew :app:compileDebugKotlin` foi validado com sucesso após coordinator e wiring.

## Achados de `012-09`

- Home e Saúde passaram a disparar `syncPending()` do Health Connect apenas após persistência local bem-sucedida e liberação do `ForegroundBleOperationGate`.
- gatilhos automáticos agora cobrem resumo diário, medição manual persistida e backlog D1 com dados realmente salvos.
- quando `healthSyncEnabled=false`, os fluxos continuam locais-first e não iniciam tentativa de sync secundário.

## Achados de `012-10`

- detalhe de Exercícios passou a expor CTA explícita `Sincronizar com Health Connect` para `READY_TO_SYNC` e retry para `SYNC_FAILED`.
- `ExercisesViewModel` passou a consumir o coordinator e mapear resultados seguros de permissão, provider, disabled e falha parcial em feedback local do detalhe.
- workflow explícito da atividade agora percorre `READY_TO_SYNC|SYNC_FAILED -> SYNCING -> SYNCED|SYNC_FAILED` sem sync automático ao atingir prontidão.

## Achados de `012-11`

- `Settings` deixou de ser placeholder e agora mostra disponibilidade, toggle de integração, permissões amigáveis, provider, fila local e última sincronização.
- `SettingsViewModel` passou a integrar preferência, resumo da fila, elegibilidade de peso/altura, permissões concedidas e ação `Sincronizar agora`.
- tela passou a suportar `Conectar`, `Sincronizar agora` e `Gerenciar acesso`, usando launcher real de permissões write-only do Health Connect.
- `./gradlew :app:compileDebugKotlin` foi validado com sucesso após 012-09, 012-10 e 012-11.

## Achados de `012-12`

- Home passou a mostrar estado secundário de Health Connect separado do refresh local, evitando confusão entre persistência do relógio e envio ao provider.
- Saúde passou a exibir feedback secundário do Health Connect no hub e no detalhe de frequência cardíaca, preservando medição manual e D1 como mensagem principal.
- Exercícios passou a resumir estados `READY_TO_SYNC`, `SYNCING`, `SYNCED` e `SYNC_FAILED` com linguagem segura na lista e no detalhe.
- falhas do provider permanecem subordinadas ao dado local já persistido nas três superfícies.
- `./gradlew :app:compileDebugKotlin` foi validado com sucesso após 012-12.

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

## Checklist

- [x] `012-01` reconciliar fechamento documental da Fase 11 e auditar backlogs atuais
- [x] `012-02` formalizar ADR de política write-only e alinhar roadmap
- [x] `012-03` integrar SDK estável `connect-client:1.1.0`, manifesto, rationale e disponibilidade
- [x] `012-04` expandir domínio de sync, record types, identidades e schema Room
- [x] `012-05` implementar fila de sync, reconciliação de backlog e contratos de repositório
- [x] `012-06` implementar gateway e mapeadores de passos, BPM e perfil
- [x] `012-07` implementar mapeadores de sessão de exercício e records complementares
- [x] `012-08` implementar coordinator foreground com partial e retry
- [x] `012-09` integrar triggers pós-persistência de passos, manual e D1
- [x] `012-10` integrar sync explícito de atividade e transições de workflow
- [x] `012-11` criar tela de Settings, permissões e gerenciamento de acesso
- [x] `012-12` integrar feedback secundário em Home, Saúde e Exercícios
- [ ] `012-13` rodar Gate C físico com Health Connect real
- [ ] `012-14` fechar documentação final da fase

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
