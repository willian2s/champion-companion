# ADR-021 — Distância de bicicleta e projeção Health Connect

- **Status:** aceita para implementação na Spec 022
- **Data:** 2026-08-11

## Contexto

O Champion Companion deriva distância canônica de treinos D3/D4 a partir de
`decodedSteps × stepLengthMeters`. Essa política é adequada para walking/running
e para os fluxos já validados, mas não representa deslocamento de bicicleta.

Um treino de bicicleta pode ser indoor ou outdoor. O Champion C001 continua
fornecendo duração e amostras D4, porém movimentos detectados como passos não
podem ser convertidos em quilômetros pedalados.

Quando conhecida, a distância pode vir de informação externa observada pelo
usuário, como painel de bicicleta de academia ou ciclocomputador. O aplicativo
não precisa saber qual equipamento forneceu o valor.

## Decisão

### 1. Tipo de domínio

Adicionar um único tipo:

```text
WorkoutType.CYCLING
```

Rótulo de UI: `Bicicleta`.

Não distinguir indoor/outdoor no domínio principal.

### 2. Tipo Health Connect

`CYCLING` mapeia para:

```text
ExerciseSessionRecord.EXERCISE_TYPE_BIKING
```

A escolha do tipo genérico é deliberada. Não usar
`EXERCISE_TYPE_BIKING_STATIONARY`, porque o Champion Companion não conhece nem
solicita o ambiente do treino.

### 3. Passos não representam distância de bicicleta

Quando `confirmedType == CYCLING`, a série derivada por passos continua
preservada como fato/diagnóstico da importação, mas fica semanticamente
inaplicável para:

- distância da atividade;
- velocidade;
- pace;
- cadência de ciclismo;
- splits por distância;
- projeções `DistanceInterval`;
- projeção `SpeedSeries`.

Mudar o tipo confirmado não apaga snapshots históricos.

### 4. Distância manual é opcional e própria

Bicicleta pode possuir distância total informada pelo usuário.

A distância:

- é opcional;
- é persistida separadamente de `original`, `estimated` e `calibrated`;
- não utiliza o fluxo de calibração de esteira;
- não é inferida por passos, BPM ou calorias;
- não é obrigatória para revisão ou sincronização.

O armazenamento canônico é em metros.

### 5. Velocidade média é apenas derivado local

Quando distância manual e duração oficial estão disponíveis:

```text
averageSpeed = manualDistance / officialDuration
```

Esse valor pode ser exibido localmente, mas não cria amostras temporais e não
autoriza `SpeedRecord` sintético.

### 6. Projeção total de distância

Quando houver distância manual válida, criar uma única projeção filha de
`DISTANCE` cobrindo a janela completa da sessão.

Identidade estável planejada:

```text
HealthProjectionChildKey.ManualDistance
stableValue = "manual-distance"
```

Essa chave é distinta de `DistanceInterval`, evitando colisão de identidade e
de provenance.

### 7. Provenance Health Connect por filho

A sessão, BPM e calorias provenientes do relógio continuam usando metadata de
registro automático associado ao Champion C001.

A distância digitada pelo usuário deve usar metadata de entrada manual e
origem/dispositivo local do Champion Companion.

O mapper não deve rotular a distância manual como capturada automaticamente
pelo relógio.

### 8. Payload congelado

O contrato atual de payload congelado já possui `projectionChildKey` e
`distanceMeters`. A implementação deve preferir manter a versão atual quando a
nova chave `ManualDistance` for suficiente para reconstruir a provenance manual
no retry.

Não aumentar schema version apenas por conveniência.

### 9. Workflow

Bicicleta sem distância não entra em `PENDING_CALIBRATION`.

A atividade pode seguir para `READY_TO_SYNC` desde que os demais guards atuais
sejam satisfeitos.

Esta ADR não autoriza edição/reconciliação pós-sync.

## Consequências

- o mesmo tipo local atende bicicleta de academia e rua;
- distância pode ficar ausente sem invalidar o treino;
- nenhum passo do relógio vira quilômetro de bicicleta;
- UI pode mostrar velocidade média sem inventar velocidade granular;
- Health Connect recebe sessão de ciclismo genérica;
- distância manual, quando presente, possui provenance correta;
- parser e derivação D4 não precisam mudar;
- será necessária persistência aditiva para distância manual.

## Alternativas rejeitadas

### `STATIONARY_BIKING` como único tipo

Rejeitado porque impediria uso natural para bicicleta de rua.

### Dois tipos indoor/outdoor

Rejeitado por adicionar distinção que o fluxo atual não precisa conhecer.

### Reutilizar `calibratedDistanceMeters`

Rejeitado porque calibração representa correção de uma estimativa de esteira; a
distância da bicicleta é uma entrada independente.

### Usar passos × passada

Rejeitado porque passos não medem deslocamento de bicicleta.

### Gerar `SpeedRecord` a partir da média

Rejeitado porque uma média total não contém amostras temporais reais de
velocidade.

### Reativar GPS para bicicleta de rua

Rejeitado porque GPS/rota não fazem parte da direção atual do produto.

## Relação com decisões anteriores

- ADR-017: mantém direção watch-first;
- ADR-018: mantém separação raw/measured/derived/projection e reconciliação por
  filhos;
- ADR-019: mantém duração oficial e timeline normalizada;
- esta ADR somente define a política específica de ciclismo e distância manual.
