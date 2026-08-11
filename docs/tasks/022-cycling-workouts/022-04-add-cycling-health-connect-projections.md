# 022-04 — Adicionar projeções Health Connect de bicicleta

- **Status:** `pending`
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

## Fora do escopo

- readback do Health Connect;
- route;
- speed samples sintéticos;
- power/RPM;
- edição pós-sync;
- testes automatizados novos.

## Critérios de aceite

- [ ] `CYCLING` mapeia para `EXERCISE_TYPE_BIKING`;
- [ ] `ManualDistance` possui chave estável própria;
- [ ] distância manual gera exatamente um `DistanceRecord` total;
- [ ] metadata da distância é manual/local;
- [ ] BPM/calorias mantêm provenance do relógio;
- [ ] `DistanceInterval` e `SpeedSeries` não são elegíveis para cycling;
- [ ] cycling sem distância sincroniza sem operação DISTANCE pendente;
- [ ] retry usa payload congelado;
- [ ] operações antigas continuam legíveis;
- [ ] build/lint passam;
- [ ] revisão independente aprova a mudança.
