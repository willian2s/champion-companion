# 013-01 — Auditar fronteiras pós-Fase 15 para sessões, localização e correlação

- **Ticker:** `013`
- **Número:** `01`
- **Status:** concluída

## Objetivo e resultado esperado

Produzir auditoria documental reproduzível do estado real pós-Fase 15, confirmando contratos, schema v5, limitações de `WorkoutActivity`, sessão, BLE esportivo, localização, foreground service, Health Connect e correlação. Resultado: relatório factual anexado nesta task e insumos fechados para ADR/contratos.

## Contexto

Planejamento encontrou mapper incompleto de sessão, repository parcial, atividade orientada a D3/D4, queries com join obrigatório de import, ausência de rota/FGS e protocolo esportivo live não confirmado. Execução deve revalidar contra branch corrente antes de qualquer mudança estrutural.

## Documentos obrigatórios

`AGENTS.md`; spec 013; baselines `current-baseline`, `feature-inventory`, `gaps-and-open-questions`, `health-connect-mapping`; roadmap; ADR-003/004/008/011/012/013; specs 010/011/012/015; overview 015; protocolos D0/F002, D3/D4, transporte e comandos.

## Dependências

Nenhuma. Primeira subtarefa elegível.

## Arquivos permitidos

- esta task e `013-00-overview.md`;
- documentos de auditoria/baseline listados, somente se correção factual mínima for necessária e explicitamente registrada.

## Arquivos e símbolos prováveis

`AppWorkoutSession`, `WorkoutActivity`, `AppWorkoutSessionRepository`, `ChampionDatabase`, DAOs/mappers de treino, `ForegroundBleOperationGate`, `HealthRecordMapper`, `AppDestination` e `ExercisesViewModel`.

## Arquivos proibidos

- código de produção, Gradle, manifesto, schemas Room;
- `app/src/test/**` e `app/src/androidTest/**`.

## Requisitos cobertos

Estado autoritativo das fases; comportamento atual; conflito do guard; DB v5; protocolo BPM esportivo; fronteira F13/F14; hotspots e riscos de migration/correlação.

## Escopo

- confirmar símbolos e joins reais;
- inventariar permissões/dependências/destinos atuais;
- confirmar ADR sequencial;
- confirmar suporte do SDK atual a `ExerciseRoute` sem promover upgrade;
- registrar divergências entre documentação e código.

## Fora do escopo

Decidir política final, alterar documentação autoritativa, implementar ou migrar.

## Passos de implementação

1. Registrar commit/branch auditados e `git status` inicial.
2. Repassar todos documentos e caminhos da spec.
3. Auditar domínio, entities, DAOs, mappers e repositories.
4. Auditar BLE D0/F002, FEA1, A8/D8, gate e reconexão.
5. Auditar Health Connect, identidade, versões, permissões e mapper de sessão.
6. Auditar manifesto, DataStore, Navigation3 e superfícies.
7. Produzir tabela fato/evidência/lacuna/impacto/task dona.
8. Atualizar somente registro desta task e overview após aceite.

## Critérios de aceite

- relatório cita caminhos e símbolos reais;
- v5 e próxima migration são confirmadas;
- limitações de sessão/atividade/joins são explícitas;
- protocolo live não é inventado;
- guard antigo e dívida ficam destacados;
- nenhuma mudança funcional ocorre.

## Validação manual

Revisão cruzada de caminhos, símbolos, ADRs e documentação. Nenhuma validação física nesta task.

## Comandos

Nenhum comando Gradle necessário. Não executar suites. Comandos de inspeção Git são permitidos.

## Evidências

Registrar commit auditado, tabela de achados, caminhos, decisões ainda abertas e `git status` final.

## Screenshots

Não aplicável; nenhuma mudança visual.

## Definição de pronto

Auditoria revisável registrada; `013-02` possui insumos sem suposições ocultas; item `013-01` marcado no overview somente após aceite.

## Riscos e cuidados

Documentação pode estar atrasada; código é evidência factual, mas não substitui direção do usuário. Não reabrir Fase 12/15.

## Rollback

Reverter somente alterações documentais desta task.

Não criar, alterar ou executar testes automatizados.

## Registro de execução

### Estado auditado

- Branch: `feat/013-phone-workouts`, rastreando `origin/feat/013-phone-workouts`.
- Commit auditado: `71a0139 docs: adicionar spec e subtarefas da Fase 013 — exercícios phone com GPS e rota`.
- `git status` inicial: limpo; saída exibiu somente branch e upstream.
- Ticker confirmado como `013` em spec, pasta, overview e todas as 17 subtarefas. Overview possui uma única seção `## Checklist`, com 17 itens, um por subtarefa.
- Nenhuma mudança funcional foi feita. Arquivos de produção, Gradle, manifesto, schemas Room e testes permaneceram inalterados.

### Documentos revisados

Foram revisados `AGENTS.md`, `docs/specs/013-app-workouts-gps-route.md`, `docs/baseline/current-baseline.md`, `feature-inventory.md`, `gaps-and-open-questions.md`, `health-connect-mapping.md`, `docs/roadmap/champion-companion-roadmap.md`, ADR-003/004/008/011/012/013, specs 010/011/012/015, overview 015, os protocolos `heart-rate-d0-f002.md`, `workouts-d3-d4.md`, `ble-transport.md` e `ble-command-mapping.md`, além dos caminhos e símbolos reais listados abaixo.

### Matriz factual

| Fato | Evidência | Lacuna ou divergência | Impacto | Task dona |
|---|---|---|---|---|
| Fase 12 e Fase 15 estão concluídas no baseline; F13 continua próxima no spec, mas adiada no roadmap e no overview 015 | `docs/baseline/current-baseline.md:7-8,24-35`; `docs/specs/015-calendar-consolidated-history.md:37-44`; `docs/roadmap/champion-companion-roadmap.md:1845-1848` | `013-00` está `pending` como próxima execução, enquanto roadmap ainda chama F13 de adiada | Grafo documental de priorização não está uniforme | `013-02` |
| Room está em v5 e schemas 1 a 5 existem | `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt:439-465`; `app/schemas/.../1.json` a `5.json` | Não existe schema 6 | Qualquer sessão phone, correlação ou rota exige migration manual não destrutiva `v5 → v6` | `013-03`, `013-05` |
| Próxima migration planejada é manual `v5 → v6`, com `schema 6.json` | `docs/specs/013-app-workouts-gps-route.md:224-234`; `docs/tasks/013-app-workouts-gps-route/013-00-overview.md:31-37` | Ainda sem desenho implementado | Risco de perda de lifecycle, pontos ou vínculos se migration avançar sem contrato | `013-03`, `013-05` |
| `AppWorkoutSession` possui estados, tempos, agregados e vínculo opcional com atividade | `core/model/AppWorkoutSession.kt:6-39,40-149` | Comentário exclui localização e estado de serviço; não há rota, pontos, segmentos ou estado FGS no domínio | Base operacional existe, mas não cobre sessão phone da spec | `013-03`, `013-08` |
| Entity persiste lifecycle completo, mas mapper reconstrói somente sessão criada e agregados | `core/database/ChampionDatabase.kt:287-315`; `data/DatabaseMappers.kt:498-510` | `state`, tempos, `activeSince`, `pausedSince` e `workoutActivityId` são descartados no retorno | `AWAITING_RECOVERY` pode voltar como `CREATED`; recuperação não é confiável | `013-01`, `013-03` |
| Repository de sessão é parcial | `core/model/RepositoryContracts.kt:303-307`; `data/RoomRepositories.kt:779-798` | Expõe apenas observar recuperáveis, recuperar e finalizar; DAO tem insert, sem criação, pausa, retomada, agregados, interrupção ou descarte no contrato | FGS e finalização transacional não têm API suficiente | `013-03`, `013-08`, `013-12` |
| `finish` persistente atual altera estado e vínculo sem aplicar máquina temporal do domínio | `data/RoomRepositories.kt:791-798` | Não acumula duração ativa/pausada nem valida ordem temporal de `at` | Pode produzir sessão final inconsistente | `013-03` |
| `WorkoutActivity` continua orientada a D3/D4 | `core/model/WatchWorkoutActivity.kt:83-144,269-300`; origem `APP_WORKOUT_SESSION` já existe em `core/model/DomainMetadata.kt:3-10` | O domínio de BPM já possui `HeartRateSource.APP_WORKOUT_SESSION` e deduplicação por sessão/timestamp (`core/model/HeartRateAndSync.kt:7-12,41-49,164-166`), mas atividade, factories, persistência e mappers ainda exigem `deviceId`, `WatchWorkoutImportKey`, parser, import timestamp e diagnóstico | Atividade phone não pode ser criada nem correlacionada sem generalização; contratos preexistentes de BPM devem ser preservados | `013-03`, `013-11`, `013-12` |
| Queries principais dependem de importação do relógio | `core/database/ChampionDaos.kt:173-224,245-255,371-386` | `INNER JOIN watch_workout_imports` exclui atividades phone; consulta de calendário por intervalo é exceção sem join (`:229-243`) | Lista, sync, workflow e histórico precisam aceitar atividade sem import | `013-03`, `013-13`, `013-15` |
| Correlação estrutural atual valida import e atividade como relação obrigatória | `data/DatabaseMappers.kt:393-441` | Não há algoritmo sessão phone → D3/D4 com janela, sobreposição, unicidade e dispositivo | Risco de falso merge ou duplicação | `013-03`, `013-13` |
| Dedupe D3/D4 atual usa CRC e fallback por dispositivo/origem/início | `data/RoomRepositories.kt:540-575`; `core/database/ChampionDatabase.kt:209-248` | Não existe candidato phone nem merge transacional de importação correlacionada | Correlação segura ainda não implementada | `013-13` |
| Gate BLE é singleton em memória e cobre quatro owners | `core/model/RepositoryContracts.kt:128-169`; `core/model/InMemoryForegroundBleOperationGate.kt:11-45` | Owners atuais são `DAILY_STEPS_REFRESH`, `MANUAL_HEART_RATE`, `WATCH_D1_HISTORY` e `WATCH_WORKOUT_IMPORT`; falta `APP_WORKOUT_SESSION` | Sessão phone não arbitra conflitos com D1, D3/D4 e medição manual | `013-08`, `013-11` |
| Transporte encerra sessão em desconexão inesperada | `core/bluetooth/ChampionConnectionManager.kt:229-277` | Não há loop de reconexão de treino ativo nem preservação de producer live | Sessão deve sobreviver sem inventar protocolo ou reiniciar coleta insegura | `013-08`, `013-11` |
| D0/F002 atual é medição manual; protocolo esportivo live não está confirmado | `docs/protocol/heart-rate-d0-f002.md:62-92,231-310`; `core/bluetooth` não possui contrato esportivo live | Uso de D0 durante exercício existe no baseline MotionLog, mas não comprova modo esportivo nativo; escala F002 continua não confirmada | Não iniciar D0/A8 automaticamente nem tratar D3/D4 como stream | `013-11` |
| D3/D4 é importação posterior, não live; A8/D8 permanecem sem contrato funcional | `docs/protocol/workouts-d3-d4.md:20-38,152-167`; `docs/protocol/ble-command-mapping.md:129-145` | Protocolo live não pode ser inventado | Relógio deve ser opcional e ausência de BPM/passos permanecer explícita | `013-11` |
| Health Connect nativo está em SDK 1.1.0 e sync é write-only foreground | `gradle/libs.versions.toml:15-19,42-47`; `docs/specs/012-native-health-connect.md:73-103`; `core/healthconnect/HealthConnectSyncCoordinator.kt:318-420` | App usa somente sessão, distância, calorias e BPM; não usa route | F13 precisa estender identidade, fila, permissões e retry sem romper F12 | `013-14` |
| SDK 1.1.0 possui suporte público de modelo para rota | cache local `connect-client-1.1.0-api.jar`: `androidx.health.connect.client.records.ExerciseRoute`, `ExerciseRouteResult`, construtor público de `ExerciseSessionRecord` com `ExerciseRoute` e `HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE`; confirmação por `jar tf` e `javap` | Requests `ReadExerciseRouteRequest`/`UpsertExerciseRouteRequest` encontrados no artefato pertencem a `androidx.health.platform.client.request` interno; não devem virar dependência do app. Comportamento do provider e Gate físico para rota ainda não foram confirmados | Não há necessidade comprovada de upgrade; implementação deve usar API pública, confirmar permissão, identidade e provider na task própria | `013-14`, `013-16` |
| Mapper HC não inclui rota e usa metadata de relógio para atividade | `core/healthconnect/HealthRecordMapper.kt:229-379` | `ExerciseRoute`/`WRITE_EXERCISE_ROUTE` ausentes; `watchDevice()` não representa atividade phone | Proveniência phone e projeção route ainda abertas | `013-14` |
| Manifesto não declara localização moderna, FGS, notificação ou rota | `app/src/main/AndroidManifest.xml:9-32,34-83` | Só `ACCESS_FINE_LOCATION` legado até SDK 30; sem service declarado e sem `WRITE_EXERCISE_ROUTE` | Preparação, GPS e sessão ativa ainda inexistentes | `013-04`, `013-08`, `013-14` |
| Backup e device transfer permanecem negados | `app/src/main/AndroidManifest.xml:34-38`; `docs/decisions/adr-004-operational-privacy-guards.md:13-34` | Guard antigo também bloqueia termos de localização/rota em domínio, banco, dados, logging e testes | Nova exceção precisa ser formalizada, sem nomes obscuros nem alteração oportunista do guard | `013-02` |
| `Phase003PrivacyGuardTest` é incompatível com a decisão de F13 e dívida conhecida | `docs/tasks/013-app-workouts-gps-route/013-00-overview.md:35-38`; `AGENTS.md:46-50` | Não foi alterado nem executado | Validação automatizada permanece deliberadamente fora desta task | `013-02`, `013-17` |
| DataStore só possui preferências de tema, sync, diagnóstico e meta | `core/preferences/AppPreferencesStore.kt:52-76`; nome `champion_preferences` em `core/preferences/PreferencesModule.kt:15-17` | Não há countdown, retenção de rota, GPS ou sessão | Preparação não tem persistência das escolhas exigidas | `013-04`, `013-07` |
| Navigation3 usa back stack manual; Exercícios só tem lista/detalhe | `core/navigation/AppDestination.kt:41-68`; `core/navigation/ChampionNavigation.kt:104-139`; `feature/exercises/ExercisesViewModel.kt:223-469` | Não existem seleção, preparação, sessão ativa ou recuperação | F13 precisa ampliar destinos sem `NavHost`/`NavController` | `013-04`, `013-07`, `013-15` |
| Não há localização persistida, rota, pontos incrementais, FGS ou `ExerciseRoute` no código atual | `docs/baseline/current-baseline.md:48-55`; manifesto; ausência de entidades/DAOs e referências em `app/src/main` | Baseline MotionLog contém GPS em memória e legado de rota, mas não é contrato nativo | Não copiar baseline externo sem decisão e limites de privacidade | `013-03` a `013-09`, `013-14` |

### Fronteiras e decisões abertas

1. Formalizar exceção controlada à ADR-004, atualizar roadmap/baselines/`AGENTS.md` e criar ADR-014 antes de introduzir localização/rota.
2. Resolver divergência de priorização: roadmap/015 dizem F13 adiada, enquanto spec/overview 013 iniciam execução.
3. Generalizar `WorkoutActivity` preservando D3/D4, sem dispositivo/importação fictícios e sem `INNER JOIN` obrigatório.
4. Corrigir round-trip e contrato completo de `AppWorkoutSession` antes de FGS, recuperação ou captura.
5. Definir migration manual v5→v6, ownership sessão→rota→atividade, cascatas, retenção e schema exportado.
6. Definir owner `APP_WORKOUT_SESSION`, política de reconexão e comportamento após morte de processo sem reinício após reboot.
7. Confirmar fisicamente somente se BPM/passos live são associação segura; D0 não é substituto automático e A8/D8 continuam fora.
8. Definir identidade phone e metadata HC, índice lógico `(aggregateType, aggregateId, recordType)`, rota na mesma `ExerciseSessionRecord` e versão monotônica.
9. Validar no Galaxy S22 permissões, provider e retry de rota em `013-16`; nenhuma confirmação física de rota foi feita nesta auditoria.

### Arquivos alterados

- `docs/tasks/013-app-workouts-gps-route/013-01-audit-post-phase-15-app-workout-location-and-correlation-boundaries.md`
- `docs/tasks/013-app-workouts-gps-route/013-00-overview.md`

### Comandos, resultados e evidências

- `git status --short --branch && git log -1 --oneline`: branch `feat/013-phone-workouts`, upstream correspondente, commit `71a0139`; status inicial limpo.
- `jar tf "/Users/willian/.gradle/caches/9.5.0/transforms/15258b8c77d7e38e8ab6ce89f6f627ad/transformed/connect-client-1.1.0-api.jar"`: classes públicas de `ExerciseRoute`/`ExerciseRouteResult` e classes internas de request de rota presentes.
- `javap -classpath "/Users/willian/.gradle/caches/9.5.0/transforms/15258b8c77d7e38e8ab6ce89f6f627ad/transformed/connect-client-1.1.0-api.jar" androidx.health.connect.client.records.ExerciseRoute androidx.health.connect.client.records.ExerciseSessionRecord androidx.health.connect.client.permission.HealthPermission`: construtor de rota, construtor de sessão com rota e `PERMISSION_WRITE_EXERCISE_ROUTE` presentes.
- Nenhum comando Gradle foi executado. Nenhum teste automatizado foi criado, alterado ou executado, conforme escopo desta task e regras do repositório.
- Validação manual física não se aplica; não houve mudança visual nem screenshot.
- `git status --short`: status final contém somente os dois arquivos documentais permitidos listados acima.

### Resultados e riscos residuais

- Critérios de aceite documental atendidos: caminhos e símbolos reais citados; v5 e v6 planejada confirmados; limitações de sessão, atividade e joins explícitas; protocolo live não inventado; guard antigo e dívida destacados; nenhuma mudança funcional.
- Riscos principais permanecem em migration, generalização de atividade, recuperação de sessão, correlação ambígua, proveniência HC, provider de rota e validação BLE física.
- A auditoria não decide política final nem altera documentação autoritativa; entrega insumos para `013-02` e contratos de `013-03`.

### Estado final

- Status: concluída.
- Item `013-01` marcado no overview após revisão independente aprovada com ressalvas corrigidas.
- Próxima subtarefa elegível: `013-02`, somente após aceite desta auditoria; execução não avançada automaticamente.
