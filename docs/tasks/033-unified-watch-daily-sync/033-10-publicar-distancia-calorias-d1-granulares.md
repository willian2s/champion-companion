# 033-10 — Publicar distância e calorias D1 granulares no Health Connect

- **Spec:** `docs/specs/033-unified-watch-daily-sync.md`
- **ADR:** `docs/decisions/adr-033-d1-granular-distance-active-calories.md`
- **Status:** `in_progress` — implementação e validação técnica concluídas; validação física pendente
- **Dependências:** 033-03, 033-04, 033-05, 033-06; ADR-031

## Objetivo

Estender o cutover granular D1 já validado para publicar:

- `DISTANCE` como `DistanceRecord`;
- calorias D1 como `ActiveCaloriesBurnedRecord`;

preservando ownership D4 dentro de workouts, sem dupla contagem e sem rateio de
buckets de fronteira.

## Contrato

```text
sample D1 em t
=> bucket [t - 5 min, t)

STEPS > 0
  -> StepsRecord (contrato ADR-031)

DISTANCE > 0
  + sem overlap com D4 de distância positiva
    -> DistanceRecord
  + overlap total ou parcial com D4 de distância positiva
    -> fail-closed para DISTANCE D1

CALORIES > 0
  + sem overlap com D4 de calorias positivas
    -> ActiveCaloriesBurnedRecord
  + overlap total ou parcial com D4 de calorias positivas
    -> fail-closed para calorias D1
```

D4 com valor zero/ausente para a métrica **não** suprime D1 válido.

## Semântica de calorias

Dumps físicos de 2026-09-14 mostraram buckets parados em `0 kcal` e energia D1
acompanhando a energia observada no workout. O contrato D1 passa a classificar
`caloriesRaw / 10.0` como energia ativa para Health Connect.

A família D4 já existente permanece `TOTAL_CALORIES_BURNED` nesta task. Migrá-la
para `ACTIVE_CALORIES_BURNED` é decisão futura independente, porque atividades já
sincronizadas não podem receber uma segunda família remota sem reconciliação
explícita.

## Implementação realizada

- [x] adicionar `HealthSyncRecordType.ACTIVE_CALORIES_BURNED`;
- [x] adicionar `WRITE_ACTIVE_CALORIES_BURNED` ao manifesto/gateway;
- [x] mapear delete/permission para `ActiveCaloriesBurnedRecord`;
- [x] introduzir payload congelado D1 schema v5 para distância/calorias;
- [x] preservar schema v4 de STEPS sem alteração;
- [x] usar identidade estável por `sampleId + recordType`;
- [x] mapear D1 `DISTANCE` para `DistanceRecord`;
- [x] mapear D1 calorias para `ActiveCaloriesBurnedRecord`;
- [x] preservar janela exata `[t-5min,t)` e offsets congelados;
- [x] preparar somente valores positivos e finitos;
- [x] suprimir D1 por métrica quando bucket toca D4 positivo da mesma métrica;
- [x] não suprimir D1 quando D4 tem métrica zero/ausente;
- [x] não ratear bucket de fronteira;
- [x] esconder da contagem acionável D1 PENDING/FAILED que passou a ser coberto por D4 positivo;
- [x] fail-close de primeira projeção D4 quando já existe D1 `SYNCED` sobreposto da mesma métrica;
- [x] solicitar ACTIVE + TOTAL no grupo de calorias enquanto D1/D4 usam famílias distintas;
- [x] preservar sync explícito, write-only e ausência de auto-sync no pull da Home;
- [x] nenhuma migration Room;
- [x] nenhuma alteração na política de WorkoutSteps/StepsCadence.

## Compatibilidade deliberadamente deixada para 033-08

`RoomRepositories.hasSupportedHealthSyncPair()` ainda é um helper legado que
classifica genericamente `WATCH_DAILY_D1_SAMPLE` como não suportado na leitura de
integridade. O writer/queue efetivo é `GranularHealthSyncRepository`; portanto a
publicação desta task não depende desse helper.

A `033-08` deve alinhar esse supported-pair e os read models que eventualmente
incluam agregados D1, evitando falso `hasInvalidOperation` sem ampliar a superfície
de escrita remota.

## Validação técnica obrigatória

Executar localmente, sem criar/executar testes automatizados:

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Validação local concluída em 2026-09-14:

- `lint`, `assembleDebug` e `assembleRelease`: **BUILD SUCCESSFUL**;
- `git diff --check`: sem saída/erros;
- warnings observados permaneceram não bloqueantes e fora do escopo desta task.

## Validação física — próximo treino

### 1. Pré-sync remoto

1. realizar o workout no Champion C001;
2. executar pull-to-refresh na Home;
3. **não** tocar no sync Health Connect ainda;
4. extrair dump sanitizado do Room.

Verificar:

- D1 estruturalmente completo para o `totalPositions` atual;
- buckets continuam em 300 s;
- operações D1 `DISTANCE`/`ACTIVE_CALORIES_BURNED` existem apenas para valores
  positivos elegíveis;
- não há operações D1 dessas métricas nos buckets que intersectam D4 positivo da
  mesma métrica;
- bucket que intersecta D4 zero/ausente continua elegível se D1 for positivo;
- nenhum novo D1 já aparece `SYNCED` antes da ação explícita;
- STEPS granular permanece sem regressão;
- nenhum residual `FEA1 - ΣD1` é materializado.

### 2. Sync explícito

Usar o botão de Health Connect da Home ou `Configurações -> Sincronizar agora`.
Se o provider solicitar `calorias ativas`, conceder a nova permissão.

### 3. Pós-sync

Extrair novo dump e verificar:

```text
WATCH_DAILY_D1_SAMPLE / DISTANCE                -> SYNCED onde elegível
WATCH_DAILY_D1_SAMPLE / ACTIVE_CALORIES_BURNED -> SYNCED onde elegível
FAILED                                          -> 0 esperado
WorkoutSteps                                    -> BLOCKED/ALREADY_COVERED quando aplicável
pendências acionáveis                           -> 0 esperado após drenar fila
```

Também confirmar:

- D4 `DISTANCE` continua somente no workout;
- D4 `TOTAL_CALORIES_BURNED` continua no contrato histórico da atividade;
- nenhuma operação D1 sincronizada sobrepõe D4 positivo da mesma métrica na
  ordem normal do fluxo;
- `PENDING_REVIEW`/`PENDING_CALIBRATION` continuam sem primeira projeção remota.

### 4. Idempotência

Executar `Sincronizar com Health Connect` novamente sem novos dados.

Esperado:

- nenhum novo `clientRecordId` para o mesmo `sampleId + recordType`;
- nenhuma segunda operação remota equivalente;
- contadores permanecem estáveis;
- zero pendências acionáveis.

## Aceite

A task pode mudar para `completed` quando:

- [x] `lint`, `assembleDebug`, `assembleRelease` e `git diff --check` passarem;
- [ ] dump pré-sync confirmar preparação e supressão por métrica;
- [ ] dump pós-sync confirmar `DISTANCE` e `ACTIVE_CALORIES_BURNED` D1 sincronizados;
- [ ] não houver duplicação D1/D4 observável na ordem normal;
- [ ] D4 zero não suprimir D1 válido;
- [ ] STEPS/WorkoutSteps permanecerem sem regressão;
- [ ] segunda sincronização for idempotente;
- [ ] evidência sanitizada for registrada em `docs/integration/`.

## Fora do escopo

- migrar D4 `TOTAL_CALORIES_BURNED` para energia ativa;
- dividir proporcionalmente buckets de fronteira;
- readback do Health Connect;
- corrigir dados históricos remotos já publicados;
- reescrever `RoomRepositories`/read models legados antes da 033-08;
- criar ou executar testes automatizados.
