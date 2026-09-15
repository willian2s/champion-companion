# 033-10 — Validação física da política de distância nativa

**Data:** 2026-09-14
**Escopo:** `FREE_WORKOUT`, `TREADMILL`, publicação Health Connect e overlap D1/D4
**Fonte:** dumps físicos sanitizados 06..10

## FREE_WORKOUT

- parser D4 `d4-v4` válido;
- 173 steps;
- distância nativa de 140 m;
- aproximadamente 9.169 kcal;
- review confirmado como `FREE_WORKOUT`;
- `WATCH_NATIVE` preservado;
- workout publicado corretamente no Health Connect;
- segundo sync idempotente;
- `FAILED=0`.

No overlap D1/D4, `DISTANCE` e `ACTIVE_CALORIES_BURNED` D1 foram suprimidos,
enquanto `STEPS` foi mantido.

## TREADMILL

Atividade: `01a0a46a-68f8-70a9-8f86-54f38001137d`.

Dados D4 originais:

```text
steps: 274
TOTAL_CALORIES_BURNED: 12.821 kcal
native distance: 196 m
```

### Dump 06 — importação

`PENDING_REVIEW` com `WATCH_NATIVE` ativo e distância canônica de 196 m.

### Dump 07 — review

`confirmedType=TREADMILL`, `PENDING_CALIBRATION`, `WATCH_NATIVE` preservado em
196 m.

### Dump 08 — calibração manual

Referência informada: 250 m.

```text
factor = 250 / 196 = 1.2755102040816326
canonicalDistanceMeters = 249.99999999999997
```

- snapshot anterior `WATCH_NATIVE` ficou inativo;
- novo snapshot `WATCH_NATIVE_CONTEXT_CALIBRATED` ficou ativo;
- `DistanceCalibrationEvidence.workoutType=TREADMILL`;
- `watchDistance=196` e `referenceDistance=250`;
- nenhuma `treadmill_calibration` legada;
- nenhuma calibração por comprimento de passo de `TREADMILL`.

Buckets D4 nativos, em metros:

```text
0, 0, 19, 45, 86, 46
```

Buckets calibrados, em metros:

```text
0, 0, 24.23469387755102, 57.39795918367347,
109.6938775510204, 58.6734693877551
```

A soma dos buckets calibrados é aproximadamente 250 m. A forma temporal foi
preservada por multiplicação proporcional, sem redistribuição linear.

Já existia evidência `RUNNING` de 4760/4526, aproximadamente 1.051701. Ela não
contaminou `TREADMILL`: antes da calibração, a distância permaneceu em 196 m;
depois, somente o fator `1.2755102040816326` de `TREADMILL` foi aplicado.

### Dump 09 — primeiro sync Health Connect

- workout `SYNCED`;
- quatro records `DISTANCE` enviados com valores calibrados;
- `EXERCISE_SESSION`, `HEART_RATE`, `SPEED`, `STEPS_CADENCE` e
  `TOTAL_CALORIES_BURNED` `SYNCED`;
- `metricProvenance` dos records derivados de distância/speed:
  `EXTERNAL_REFERENCE_CALIBRATION`;
- `FAILED=0`.

### Dump 10 — segundo sync

Dump 09 e dump 10 são idênticos linha a linha nas 31 tabelas. Nenhum novo
record equivalente foi criado; idempotência confirmada.

## D1 positivo sem overlap D4

Também foram confirmados buckets D1 positivos fora da janela do
`FREE_WORKOUT`:

```text
D1 16:30 = 27 steps / 21 m / 1.4 kcal
D1 17:05 = 159 steps / 99 m / 6.4 kcal
```

O `FREE_WORKOUT` começou às 16:43 e terminou às 16:53:59. Portanto, ambos os
buckets não tiveram overlap D4 e foram publicados integralmente:

```text
WATCH_DAILY_D1_SAMPLE / STEPS                    -> SYNCED
WATCH_DAILY_D1_SAMPLE / DISTANCE                -> SYNCED
WATCH_DAILY_D1_SAMPLE / ACTIVE_CALORIES_BURNED  -> SYNCED
```

## Overlap D1/D4

Treino: aproximadamente `06:21:12..06:27:45`.

```text
D1 06:25 = 25 steps / 19 m / 1.2 kcal
D1 06:30 = 249 steps / 177 m / 11.6 kcal
```

Ambos os buckets tiveram `DISTANCE` e `ACTIVE_CALORIES_BURNED` suprimidos por
overlap D4 positivo, inclusive o bucket parcialmente sobreposto de 06:30.
`STEPS` foi mantido: `25 + 249 = 274`, exatamente o total do treino.

`WORKOUT_ACTIVITY/STEPS` permaneceu `PENDING/EXPECTED`, com `attemptCount=0` e
sem erro, consistente com reconciliação pelos D1 publicados.

Esta evidência confirma overlap por métrica, sem soma ingênua D1+D4, sem
prorrateamento de bucket e sem residual `FEA1 - ΣD1`.

## Limites da evidência

Validação física da nova política `FREE_WORKOUT`/`TREADMILL` e dos dois cenários
normais de D1/D4 concluída. O caso D4 zero/ausente permanece obrigatório como
guard técnico: D4 zero/ausente não suprime D1 válido. Sua reprodução física não
é exigida, pois D1 e D4 vêm da mesma fonte e atividade, com agregações de
aproximadamente cinco e um minutos; exigir D1 positivo com cobertura D4 e todos
os intervalos D4 zero/ausentes é contraditório ou excepcional.
