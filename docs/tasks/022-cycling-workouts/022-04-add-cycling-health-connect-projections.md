# 022-04 — Adicionar projeções Health Connect de bicicleta

- **Status:** `completed`
- **Depende de:** `022-03`
- **Desbloqueia:** `022-05`

## Objetivo

Adaptar a projeção granular de atividades do relógio para `CYCLING`, usando
sessão genérica de biking, preservando BPM/calorias e criando distância total
manual opcional com provenance correta.

## Sessão

Adicionar mapping:

```text
WorkoutType.CYCLING
→ ExerciseSessionRecord.EXERCISE_TYPE_BIKING
```

Não mapear para `EXERCISE_TYPE_BIKING_STATIONARY`.

## Filhos elegíveis

Para atividade `CYCLING`, o manifesto esperado pode conter:

```text
ExerciseSession       obrigatório quando atividade elegível
HeartRateSeries       quando houver samples elegíveis
TotalCaloriesBurned   quando houver calorias válidas
ManualDistance        quando houver manualDistanceMeters válido
```

Não incluir:

```text
DistanceInterval
SpeedSeries
```

mesmo que exista snapshot canônico por passos preservado.

## Nova chave de distância manual

Adicionar chave filha estável:

```text
HealthProjectionChildKey.ManualDistance
stableValue = "manual-distance"
recordTypeName = DISTANCE
```

Requisitos:

- participar de `fromStableValue`;
- possuir identidade determinística normal da fila;
- participar de manifesto/reconciliação;
- representar exatamente um `DistanceRecord` total;
- janela = início/fim oficial da sessão;
- valor = `manualDistanceMeters`.

Não reutilizar `DistanceInterval` com índice artificial.

## Metadata/provenance

Filhos vindos do relógio mantêm metadata vigente do Champion C001.

`ManualDistance` deve usar:

```text
Metadata.manualEntry(...)
```

com dispositivo/origem local do Champion Companion, porque o valor foi digitado
pelo usuário.

Não marcar distância manual como `autoRecorded(watchDevice())`.

## Frozen payload e retry

O frozen payload atual já possui:

- `projectionChildKey`;
- `start`/`end`;
- `distanceMeters`.

Preferir manter o schema version atual e selecionar metadata manual a partir da
chave `ManualDistance`. Alterar schema version somente se a implementação provar
que esses campos não bastam para reconstrução determinística.

Retry deve usar o payload congelado existente; não reler o formulário nem
recalcular valor após a operação ser criada.

## Sem distância

Atividade `CYCLING` sem `manualDistanceMeters`:

- não cria filho `DISTANCE`;
- continua elegível para sessão;
- pode sincronizar BPM/calorias quando presentes;
- não cria falha ou pendência artificial de distância.

## Sem velocidade granular

Não gerar `SpeedRecord` com uma única média nem samples interpolados.

A velocidade média local não é projeção Health Connect nesta spec.

## Compatibilidade/reconciliação

- operações antigas permanecem legíveis;
- outros tipos mantêm filhos atuais;
- não reescrever operações `SYNCED` durante upgrade;
- não criar reconciliação pós-sync nova para edição de distância;
- abrir calendário/detalhe continua sem efeitos remotos;
- Health Connect permanece write-only.

## Validação

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- validar frozen payload e identity por inspeção local;
- gate físico final em `022-05`.

## Execução

- **Status:** `completed`
- **Arquivos alterados:**
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectSyncCoordinator.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HealthProjectionContracts.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- **Decisões:**
  - `CYCLING` usa `EXERCISE_TYPE_BIKING`, sem distinção de bicicleta estacionária;
  - `ManualDistance` usa chave estável `manual-distance`, manifesto e identidade determinística existentes;
  - distância manual cria um único `DistanceRecord` na janela oficial, com payload congelado v1 e `Metadata.manualEntry` local;
  - cycling exclui `DistanceInterval` e `SpeedSeries`, mas mantém sessão, BPM e calorias elegíveis quando presentes;
  - reconciliação pode remover distância legada conhecida sem exigir uma série substituta para cycling sem distância;
  - operações antigas e regras de walking/running/treadmill permanecem preservadas.
- **Desvios:** nenhum. Schema do payload permaneceu na versão 1; nenhum teste automatizado novo foi criado ou executado.
- **Comandos executados:**
  - `git diff --check`
  - `./gradlew app:compileDebugKotlin`
  - `./gradlew lint assembleDebug assembleRelease`
- **Resultados e evidências:** `git diff --check`, compilação Kotlin, lint, `assembleDebug` e `assembleRelease` passaram. Inspeção local confirmou chave estável, identidade baseada em `stableValue`, manifesto, janela oficial, payload congelado e seleção de metadata por filho. Revisão independente aprovou a subtarefa em 2026-08-11.
- **Testes/lint/typecheck/build:** nenhum teste automatizado executado, conforme regras do repositório; lint e builds completos passaram.
- **Riscos residuais:** validação física com Health Connect, banco existente e cenários de cycling com e sem distância permanece em `022-05`. Não foi feito readback remoto.

## Fora do escopo

- readback do Health Connect;
- route;
- speed samples sintéticos;
- power/RPM;
- edição pós-sync;
- testes automatizados novos.

## Critérios de aceite

- [x] `CYCLING` mapeia para `EXERCISE_TYPE_BIKING`;
- [x] `ManualDistance` possui chave estável própria;
- [x] distância manual gera exatamente um `DistanceRecord` total;
- [x] metadata da distância é manual/local;
- [x] BPM/calorias mantêm provenance do relógio;
- [x] `DistanceInterval` e `SpeedSeries` não são elegíveis para cycling;
- [x] cycling sem distância sincroniza sem operação DISTANCE pendente;
- [x] retry usa payload congelado;
- [x] operações antigas continuam legíveis;
- [x] build/lint passam;
- [x] revisão independente aprova a mudança.
