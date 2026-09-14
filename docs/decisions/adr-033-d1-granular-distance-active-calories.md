# ADR-033 — Distância e calorias ativas D1 granulares no Health Connect

- **Status:** accepted
- **Data:** 2026-09-14
- **Relacionado:** ADR-030, ADR-031, ADR-032; Spec 033
- **Refina:** ADR-030 e ADR-031 para `DISTANCE` e calorias D1

## Contexto

A ADR-031 promoveu o timestamp D1 observado a uma janela de cinco minutos:

```text
sample D1 em t
=> bucket [t - 5 minutos, t)
```

A validação física de 2026-09-14 confirmou novamente essa direção temporal com
workout real e passos D1/D4 consistentes. Os mesmos pacotes D1 também carregam
distância nativa em metros e calorias em `raw / 10.0`.

Os dumps físicos mostraram ainda duas propriedades relevantes:

1. D1 inclui fatos ocorridos durante workouts; portanto não é seguro publicar
   ingenuamente D1 e D4 para a mesma métrica e janela;
2. buckets D1 sem movimento observado permaneceram em `0 passos / 0 m / 0 kcal`,
   enquanto buckets em movimento acumularam calorias. Durante workout, a soma de
   calorias D1 acompanhou de perto a energia D4 observada.

No Health Connect, `ActiveCaloriesBurnedRecord` representa energia ativa, enquanto
`TotalCaloriesBurnedRecord` possui semântica total. A evidência local é suficiente
para classificar a energia D1 como **ativa**, mas não autoriza migrar
silenciosamente a família D4 histórica já publicada como
`TOTAL_CALORIES_BURNED`.

Também foi observado anteriormente um workout D4 com métricas locomotoras zero
intersectando D1 não nulo. Logo, overlap temporal isolado não é motivo para
suprimir todas as métricas D1: ownership precisa ser decidido **por métrica**.

## Decisão

### 1. Janela D1 comum

`STEPS`, `DISTANCE` e `ACTIVE_CALORIES_BURNED` D1 compartilham a mesma janela
comprovada:

```text
start = resolvedAt - 5 minutos
end   = resolvedAt
```

Buckets que atravessam a data civil resolvida continuam inelegíveis. Não fabricar,
interpolar ou ratear timestamps.

### 2. Distância D1

Um bucket D1 com `distanceMeters > 0` pode gerar `DistanceRecord` quando não
intersecta um workout D4 do mesmo dispositivo com distância positiva.

A identidade remota é determinística por `sampleId + DISTANCE`, e o payload fica
congelado antes do primeiro efeito remoto.

### 3. Calorias D1

Um bucket D1 com `caloriesKilocalories > 0` é projetado como
`ActiveCaloriesBurnedRecord` e exige `WRITE_ACTIVE_CALORIES_BURNED`.

A identidade remota é determinística por `sampleId + ACTIVE_CALORIES_BURNED`, e o
payload também fica congelado.

### 4. D4 mantém a família histórica de calorias nesta entrega

Workouts D4 continuam usando o contrato já existente de
`TOTAL_CALORIES_BURNED`/`TotalCaloriesBurnedRecord`.

Migrar D4 para calorias ativas exigiria política própria para records históricos,
IDs, deleção/substituição e compatibilidade com atividades já sincronizadas. Essa
mudança fica explicitamente fora desta decisão.

### 5. Ownership D1/D4 é por métrica

Para cada bucket `[start, end)`:

```text
DISTANCE D1
  + intersecta D4 com distância positiva
    -> D4 é proprietário da métrica
    -> não publicar o bucket D1 de DISTANCE

ACTIVE_CALORIES_BURNED D1
  + intersecta D4 com calorias positivas
    -> D4 é proprietário da energia do workout
    -> não publicar o bucket D1 de calorias

D4 com a métrica igual a zero/ausente
  -> não suprime D1 válido daquela métrica
```

Não existe regra global “tocou workout, suprime bucket”.

### 6. Bucket de fronteira não é dividido

Se um bucket de cinco minutos intersectar parcialmente um workout que possui a
métrica positiva, o bucket D1 inteiro daquela métrica fica fail-closed.

Exemplo:

```text
workout termina 07:36:55
bucket D1       07:35:00 -> 07:40:00

se D4 possui distância positiva:
  DISTANCE D1 desse bucket não é publicada
```

Não estimar que fração pertence ao workout nem ao período externo.

### 7. Ordem normal e caso tardio

O fluxo normal permanece:

```text
Home pull
  -> FEA1 + D1 + D3/D4
  -> persistência local
  -> nenhuma escrita HC

sync explícito
  -> prepara/publica somente projeções seguras
```

Assim, D4 normalmente já existe antes da preparação D1 remota.

Como guard adicional, se um bucket D1 de `DISTANCE` ou
`ACTIVE_CALORIES_BURNED` já estiver `SYNCED` e um D4 sobreposto da mesma métrica
for preparado depois, a primeira projeção D4 correspondente deve falhar fechado,
em vez de introduzir duplicidade remota.

### 8. Payload e retry

- schema v4 permanece exclusivo de STEPS D1 já validado;
- schema v5 representa `DISTANCE` ou `ACTIVE_CALORIES_BURNED` D1;
- `clientRecordId` inclui `sampleId + recordType`;
- `clientRecordVersion` permanece derivada do fato persistido;
- retry reutiliza exatamente identidade, janela e valor congelados;
- nenhuma migration Room é necessária.

### 9. Gate diário permanece conservador

A preparação granular continua exigindo dia D1 estruturalmente completo, FEA1
válido e `ΣD1.steps <= FEA1.steps`. `FEA1 - ΣD1` permanece somente cauda/frescor e
nunca vira distância, energia ou passos sintéticos.

### 10. Integridade/read models legados

`GranularHealthSyncRepository` é o owner do novo writer e da fila granular.
`RoomRepositories.hasSupportedHealthSyncPair()` ainda classifica genericamente
`WATCH_DAILY_D1_SAMPLE` como legado não suportado em uma leitura de integridade.
O alinhamento desse helper e dos consumidores de read model pertence à `033-08`;
não deve reabrir nem alterar os efeitos remotos definidos aqui.

## Consequências

### Positivas

- distância diária real do relógio passa a chegar ao Health Connect fora da
  cobertura positiva de workouts;
- calorias D1 ganham semântica remota explícita de energia ativa;
- D4 continua canônico dentro do workout sem somar novamente os mesmos fatos D1;
- workouts D4 com valor zero não apagam fatos D1 válidos;
- fronteiras permanecem conservadoras e reproduzíveis;
- passos granulares e crédito `WorkoutSteps` não são alterados.

### Trade-offs

- minutos externos contidos em bucket de fronteira podem ficar sem DISTANCE ou
  ACTIVE_CALORIES_BURNED remoto;
- D1 e D4 usam famílias de calorias diferentes enquanto a migração D4 não for
  decidida;
- caso tardio D1 já sincronizado -> D4 prefere bloquear nova projeção a duplicar,
  exigindo decisão futura se for necessário reconciliar o record remoto.

## Guard rails

- Health Connect continua write-only;
- toda escrita remota continua explícita;
- nenhuma escrita durante Home pull/watch sync;
- nenhum rateio proporcional de bucket;
- nenhum `D1 + D4` ingênuo;
- supressão sempre por métrica positiva, nunca por overlap global;
- D4 com zero não suprime D1 válido;
- não reabrir records `SYNCED` silenciosamente;
- não alterar a família de calorias D4 nesta entrega;
- não criar testes automatizados sem pedido explícito.
