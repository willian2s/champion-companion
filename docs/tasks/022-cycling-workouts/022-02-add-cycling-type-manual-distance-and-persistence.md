# 022-02 — Adicionar tipo bicicleta, distância manual e persistência

- **Status:** `completed`
- **Depende de:** `022-01`
- **Desbloqueia:** `022-03`

## Objetivo

Adicionar `WorkoutType.CYCLING`, persistir distância manual opcional em campo
próprio e integrar esse dado ao formulário de revisão sem reutilizar calibração
de esteira.

## Mudanças planejadas

### Domínio

- adicionar `WorkoutType.CYCLING`;
- label de UI `Bicicleta`;
- expor distância manual separadamente em `WorkoutDistanceMetrics` ou contrato
equivalente definido pela auditoria;
- validar somente valor positivo e finito quando presente;
- não criar sugestão automática de bicicleta baseada em passos.

### Persistência

Adicionar campo nullable próprio em `workout_activities`:

```text
manualDistanceMeters REAL NULL
```

Criar migration aditiva:

```text
MIGRATION_11_12
```

Requisitos:

- linhas existentes permanecem `NULL`;
- nenhum backfill por distância original/estimada/calibrada;
- nenhuma derivação durante migration;
- nenhum efeito Health Connect;
- atualizar schema exportado v12;
- atualizar mappers e round-trip Room.

### Revisão

Adicionar campo opcional de distância quando o tipo selecionado for `CYCLING`.

UI:

```text
Distância
[ 16,4 ] km
Opcional
```

A revisão deve:

- aceitar ausência;
- aceitar valor positivo válido;
- permitir limpar;
- armazenar em metros;
- não direcionar bicicleta para `PENDING_CALIBRATION`;
- manter os guards atuais de título, notas, parser/plausibilidade e workflow.

A política congelada em `022-01` deve definir o comportamento ao salvar tipo
não `CYCLING` quando existir valor manual temporário; não deixar esse valor ser
consumido por outro tipo.

## Compatibilidade

- nenhum parser D3/D4 é alterado;
- snapshots derivados não são apagados;
- treadmill continua usando `calibratedDistanceMeters` e seu fluxo próprio;
- atividades antigas não mudam de tipo;
- atividades `SYNCED` continuam sem edição nova por esta spec.

## Validação

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- inspeção do schema Room v12;
- upgrade local v11→v12 sem wipe em `022-05`.

## Execução

- **Status:** `completed`
- **Arquivos alterados:**
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/AppWorkoutSession.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/notification/WorkoutNotificationBuilder.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarDayScreen.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
  - `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/12.json`
- **Decisões:**
  - `manualDistanceMeters` fica separado das distâncias de origem, estimada e calibrada, com `NULL`/ausência como valor padrão;
  - distância digitada em km é validada como positiva e finita, convertida para metros e limpa quando revisão salva tipo diferente de `CYCLING`;
  - `CYCLING` permanece exclusivo de revisão de atividade do relógio; treino phone não aceita esse tipo;
  - mapping específico e projeções Health Connect permanecem para `022-04`.
- **Desvios:** nenhum requisito de `022-02` alterado. Labels fechados foram atualizados somente para manter compilação e exibir `Bicicleta`; consumers de métricas e projeções específicas ficaram nas subtarefas seguintes.
- **Comandos executados:**
  - `git diff --check`
  - `./gradlew app:compileDebugKotlin`
  - `./gradlew lint assembleDebug assembleRelease`
  - `./gradlew app:compileDebugKotlin app:lintDebug`
- **Resultados e evidências:** `git diff --check` passou; compilação Kotlin, lint, assembleDebug e assembleRelease passaram; schema Room v12 foi gerado com `workout_activities.manualDistanceMeters REAL`; revisão independente aprovou o escopo em 2026-08-11.
- **Testes automatizados:** não executados, conforme regras do repositório e escopo da spec.
- **Riscos residuais:** `022-03` ainda precisa bloquear consumidores locais de métricas derivadas por passos para `CYCLING`; `022-04` ainda precisa implementar sessão/projeções Health Connect de ciclismo; upgrade físico v11→v12, `PRAGMA user_version` e round-trip em dispositivo permanecem para `022-05`.

## Fora do escopo

- adaptar todos os consumers locais;
- Health Connect;
- GPS/rota;
- velocidade granular;
- testes automatizados novos.

## Critérios de aceite

- [x] `CYCLING` existe no domínio e persiste corretamente;
- [x] UI usa rótulo `Bicicleta`;
- [x] distância manual é opcional e separada das três distâncias existentes;
- [x] migration v11→v12 é aditiva e sem backfill semântico;
- [x] bicicleta sem distância pode concluir revisão sem calibração;
- [x] treadmill mantém comportamento existente;
- [x] nenhuma atividade antiga é reprocessada;
- [x] build/lint passam;
- [x] revisão independente aprova a mudança.
