# 032-06 — Implementar calibração de distância contextual

- **Status:** completed
- **Depende de:** 032-04
- **Pode rodar em paralelo com:** 032-05 após contrato comum congelado

## Objetivo

Substituir, para `d4-v4` elegível, a calibração principal por passada por uma
correção proporcional da distância nativa, mantendo histórico antigo intacto.

## Escopo

- persistir evidência manual `referenceDistance / watchDistance`;
- perfis independentes RUNNING/WALKING;
- agregado ponderado `Σreference / Σwatch`;
- provenance explícita no snapshot;
- rederivação explícita da atividade usada como referência;
- correção/remoção de evidência sem duplicação de peso;
- fallback para distância nativa sem fator (`1.0`) e depois step length se a série
  nativa for inelegível;
- preservar calibrações legacy de comprimento de passo.

## Decisões de UI para 032-07

Esta task deve entregar contratos de estado/ação sem redesenhar a tela. A UI final
será encaixada depois da integração com a Spec 031.

## Guard rails

- referência externa é manual e pode conter erro;
- nenhuma integração GPS/Adidas/Strava;
- `TREADMILL` não herda RUNNING automaticamente;
- `CYCLING` fora do aprendizado;
- nenhuma atividade `SYNCED` é alterada silenciosamente.

## Validação

`./gradlew lint assembleDebug assembleRelease` e `git diff --check`.

## Execução

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DistanceCalibrationEvidence.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/17.json`

### Decisões e desvios

- Evidência de distância nativa usa entidade própria, índice único por atividade e flag `isActive`; calibrações legacy de comprimento de passo permanecem separadas.
- Perfis RUNNING/WALKING agregam somente evidências nativas ativas pela razão `Σ referência / Σ distância do relógio`.
- Atividade v4 nativa elegível usa fator explícito na rederivação da atividade de referência ou fator aprendido em treinos futuros; ausência de fator mantém `WATCH_NATIVE` com fator implícito `1.0`.
- Correção rederiva explicitamente atividade não sincronizada; remoção desativa evidência para o perfil futuro sem alterar snapshot histórico.
- Provenance de distância/speed Health Connect deriva do método do snapshot, incluindo distância nativa e distância calibrada, sem expor packing raw.
- Contratos de estado/ação nativa foram adicionados sem redesenho visual. Promoção geral do runtime para `d4-v4` permanece em 032-07.

### Comandos executados

- `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin app:lintDebug`
- `./gradlew lint assembleDebug assembleRelease`
- `git diff --check`

### Resultados e evidências

- Compilação Kotlin debug/release: passou.
- `lint`, `assembleDebug` e `assembleRelease`: passaram.
- Schema Room v17 e migration aditiva `16 -> 17` gerados/registrados.
- `git diff --check`: passou.
- Nenhum teste automatizado foi criado ou executado, conforme `AGENTS.md`.

### Riscos residuais

- Runtime de novas importações selecionava `d4-v3` na conclusão desta subtarefa;
  promoção coerente para `d4-v4` foi concluída em 032-07.
- Integração visual final da evidência contextual pertence a 032-07; esta task entrega contratos e ação sem redesenhar a tela.
- Validação física, comparação com referência manual e fechamento da spec permanecem em 032-08.
- Não há cobertura automatizada nova para agregação, migration ou round-trip; validação física e inspeção local permanecem necessárias.
