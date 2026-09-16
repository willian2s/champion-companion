# ADR-034 — Zonas de frequência cardíaca e carga cardiovascular

- **Status:** proposta para a Spec 034
- **Data:** planejamento da Spec 034
- **Relacionada:** [Spec 034](../specs/034-heart-rate-zones-training-load.md)

## Contexto

O Champion Companion já persiste samples temporais de frequência cardíaca em
workouts importados do relógio. ADR-018 congelou a semântica temporal da série:
janela semiaberta, sample-and-hold limitado à sessão, ausência sem fabricação e
projeção Health Connect baseada em fatos locais congelados.

A nova métrica precisa responder duas perguntas locais no detalhe do workout:

1. quanto tempo coberto pela série ficou em cada zona relativa à FC máxima
   configurada pelo usuário;
2. qual carga resulta da soma ponderada das durações nas zonas.

FC máxima é uma métrica pessoal mutável. Zonas e carga não serão persistidas por
atividade; devem ser recalculadas com a FC máxima atual.

## Decisão

### 1. Fonte e ownership

Usar somente:

- `WorkoutActivity` selecionada;
- `WorkoutHeartRateSample` persistido da atividade;
- `UserProfile.maxHeartRateBpm` atual.

A regra pura ficará em `core.model`, sem dependência de Room, Health Connect,
BLE, parser, ViewModel ou Compose. O detalhe receberá read model efêmero
composto a partir desses fatos.

Não adicionar análise, zonas ou carga a entidades Room, snapshots de derivação ou
payloads remotos.

### 2. FC máxima pessoal

Adicionar `maxHeartRateBpm: Int?` ao perfil pessoal e a
`PersonalMetricsSnapshot`. O valor é manual, opcional e válido em `30..220`
BPM, inclusive. `NULL` representa não configurado.

Persistir em `user_profiles` por migration aditiva Room v19 -> v20. A migration
não insere valor, não recalcula atividade, não cria snapshot, não altera workflow
e não chama Health Connect.

O update da FC máxima não usa o gatilho remoto existente para altura. Health
Connect não recebe essa métrica nesta entrega.

### 3. Política temporal única

Reutilizar o contrato de ADR-018. Para samples ordenados dentro da janela
canônica `[S, E)`:

```text
sample_i -> [sampledAt_i, sampledAt_{i+1})
último sample -> [sampledAt_last, E)
```

Cada janela deve ser contida na sessão e somente samples que gerem sobreposição
real em `[S, E)` participam. O trecho `[S, sampledAt_first)` permanece sem
cobertura. Não criar timeout, interpolação, amostra sintética ou política de
lacuna paralela. A janela `E` é `startedAt + duration`, não `endedAt` divergente
por pausas. Um clamp de implementação existente não autoriza usar sample
externo como fato da sessão.

`coveredDuration` é a soma das janelas positivas. `coverageRatio` é
`coveredDuration / sessionDuration`. A carga parcial é válida e não é eliminada
por threshold global.

### 4. Buckets de zona

Classificar por `bpm / maxHeartRateBpm` em `Double`, nunca por limites BPM
arredondados:

```text
< 0.50       BELOW_ZONE_1, peso 0
0.50..<0.60  Z1, peso 1
0.60..<0.70  Z2, peso 2
0.70..<0.80  Z3, peso 3
0.80..<0.90  Z4, peso 4
>= 0.90      Z5, peso 5
```

O bucket `BELOW_ZONE_1` fecha a cobertura, embora não seja uma zona ponderada.
Sample acima da máxima configurada é Z5, preservando BPM real e ativando
`observedAboveConfiguredMaximum`.

### 5. Carga

Usar Edwards TRIMP / summated heart-rate-zones local:

```text
Σ (durationInMinutes(zone) × zoneWeight)
```

`Double` é mantido até a UI. O nome de produto é **Carga cardiovascular**. A
métrica é local do Champion Companion; não reproduz escala Google, Fitbit ou
Health Connect e não será enviada a nenhum serviço.

### 6. Ausência e estados

Não representar ausência com zero. O read model distingue:

- `AVAILABLE` — samples e FC máxima permitem zonas/carga;
- `CONFIGURATION_REQUIRED` — samples permitem cobertura, mas falta FC máxima;
- `NO_HEART_RATE_SAMPLES` — nenhum sample utilizável dentro da sessão;
- `UNAVAILABLE` — erro ou inconsistência de leitura.

Com samples e sem configuração, cobertura e máxima observada podem ser
apresentadas; zonas e carga permanecem ausentes de forma tipada. Sem samples,
não há `WorkoutHeartRateAnalysis` numérica para a UI.

### 7. Perfil mutável e histórico

Mudar FC máxima recalcula visualmente workouts históricos porque zonas e carga
são read model derivado da configuração atual. A mudança não reprocessa
`WorkoutDerivationSnapshot`, não muda `workflowState`, não atualiza atividades
`SYNCED` e não ressincroniza Health Connect.

### 8. Health Connect

Nenhuma mudança funcional em Health Connect: sem permissão, record, payload,
ID, versão, operação, retry ou reconciliação nova. A fila local e as atividades
remotas permanecem exatamente como estão.

## Consequências

### Positivas

- uma única semântica temporal para splits e análise de FC;
- alteração pessoal recalcula históricos sem reescrever fatos congelados;
- migration pequena e compatível com `NULL`;
- nenhum acoplamento novo com Health Connect ou parser;
- cobertura parcial fica visível em vez de ser descartada;
- cálculo pode ser validado independentemente da UI.

### Negativas e riscos aceitos

- sample-and-hold até o fim pode manter último BPM quando sensor parou;
- carga histórica pode mudar quando usuário altera FC máxima;
- v20 exige schema exportado e validação sobre banco real;
- cobertura não representa qualidade do sensor, apenas tempo governado pelo
  contrato temporal de ADR-018;
- a métrica local não é comparável automaticamente a escalas externas.

## Alternativas rejeitadas

- persistir carga por atividade;
- usar header agregado de BPM;
- usar `maximumBpm` observado como FC máxima pessoal;
- usar idade ou fórmula estimada;
- introduzir timeout para lacunas;
- arredondar limites antes de classificar;
- criar write Health Connect;
- atualizar snapshots ou workflow ao alterar perfil.

## Limites

Esta ADR não define classificação de esforço baseada em baseline pessoal, metas,
histórico ou comparação entre workouts. Também não altera parser D1/D4, BLE,
Health Connect, duração canônica, snapshots de derivação ou contratos de treino
phone históricos.
