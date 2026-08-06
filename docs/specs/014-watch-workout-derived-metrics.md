# 014 — Refinamento, derivação e sincronização dos treinos do relógio

- **Ticker:** `014`
- **Status:** in_progress
- **Progresso:** `6/16`
- **Base:** `main`, Fase 013 encerrada como `closed` / `partially_delivered`
- **Decisões relacionadas:** ADR-012, ADR-016, ADR-017 e ADR-018

## Contexto

A Fase 013 foi encerrada por `product_reorientation`, sem reabrir treinos
iniciados pelo celular e sem remover migrations, schemas, dados ou infraestrutura
histórica. A direção vigente prioriza treinos iniciados e encerrados pelo
Champion C001:

```text
Champion C001 inicia e encerra
        ↓
importação D3/D4 explícita
        ↓
normalização raw/measured
        ↓
derivação de série canônica
        ↓
revisão local
        ↓
sincronização Health Connect granular
```

O parser atual interpreta os 24 bits em `bytes[7..9]` como um único contador de
passos, fixa todos os intervalos em 60 segundos e calcula ritmo/velocidade usando
a distância original do relógio. Evidência física posterior confirmou que o
valor é empacotado: passos estão no byte superior; os 16 bits inferiores seguem
sem significado comprovado.

## Objetivo

Corrigir a leitura temporal D4, preservar fatos de origem, configurar
`stepLengthMeters`, derivar uma única série canônica reproduzível, apresentar
métricas e splits, reprocessar atividades existentes somente por ação explícita
e sincronizar projeções filhas idempotentes com o Health Connect.

## Terminologia normativa

- `stepLengthMeters`: distância de um passo individual.
- **Comprimento do passo:** texto de interface correspondente.
- `raw`: valor recebido sem semântica inventada.
- `measured`: fato de origem com semântica confirmada.
- `derived`: valor calculado por algoritmo versionado.
- `canonical`: única métrica aceita por consumidores funcionais.
- `diagnostic`: valor preservado para comparação, auditoria ou investigação, mas
  proibido como fallback funcional.

## Requisitos

### R1 — Origem e controle

1. Somente atividades `WATCH_D3_D4` entram na nova derivação.
2. Importação, reprocessamento e sincronização permanecem explícitos e foreground.
3. Alteração de perfil não recalcula nem ressincroniza atividades antigas.
4. A Fase 013 permanece fechada e sua infraestrutura histórica é preservada.
5. GPS, rota, mapa, `ExerciseRoute` e BLE live não são requisitos.

### R2 — Comprimento pessoal do passo

1. V1 mantém um valor pessoal, fixo, positivo e finito de `stepLengthMeters`.
2. Origens previstas: `MANUAL`, `HEIGHT_SUGGESTION_ACCEPTED` e
   `REFERENCE_DISTANCE_CALIBRATION`.
3. A entrega inicial implementa entrada manual e sugestão baseada na altura,
   sempre com confirmação explícita antes de persistir.
4. Sugestão não aceita não é perfil.
5. Ausência ou invalidade mantém distância canônica ausente.
6. Evidência combinada de `7.201` passos e `6.350 m` produz aproximadamente
   `0,8818 m`, mas não vira constante global, default ou fallback.

### R3 — Decodificação e preservação D4

Para cada pacote temporal `4+`:

```kotlin
val rawMotionValue = bytes.readUInt24LittleEndian(7)
val decodedSteps = rawMotionValue ushr 16
```

1. Preservar `rawMotionValue` integral.
2. Usar somente `decodedSteps` como passos confirmados do intervalo.
3. Preservar os 16 bits inferiores como componente desconhecido.
4. Preservar distância e calorias temporais como valores raw, sem promovê-las a
   métricas semânticas enquanto unidade e fechamento não forem comprovados.
5. A sequência de referência dos 28 intervalos deve somar `3.737` passos.
6. O parser corrigido deve usar nova `parserVersion`, inicialmente `d4-v2`.
7. `d4-v1` permanece identificável como parser que persistiu o u24 empacotado no
   campo legado `steps`.

### R4 — Compatibilidade com atividades `d4-v1`

1. A migration não deriva métricas automaticamente.
2. Para atividade `WATCH_D3_D4` com `parserVersion = d4-v1`, o valor legado de
   `workout_intervals.steps` pode ser copiado para `rawMotionValue`, sem ser
   tratado como passos decodificados.
3. `decodedSteps` permanece ausente até reprocessamento explícito.
4. A ponte só se aplica a versões de parser conhecidas; versões desconhecidas
   falham fechadas.
5. Reprocessamento aplica a regra da versão atual sem exigir BLE quando os fatos
   raw necessários estiverem persistidos.

### R5 — Elegibilidade e duração efetiva

A série V1 exige índices contínuos e únicos:

```text
0, 1, 2, ..., N-1
```

Primeiro índice diferente de zero, lacuna, duplicata ou ordem inconsistente torna
a série inelegível, preservando raw/measured.

Sejam `T` a duração total positiva, `N` a quantidade de intervalos elegíveis e
`Q = 60 s`:

1. `T == N × Q`: todos os intervalos usam 60 segundos.
2. `(N−1) × Q < T < N × Q`: primeiros `N−1` usam 60 segundos e o último usa
   `T − (N−1) × Q`.
3. `T <= (N−1) × Q`: série inelegível; não truncar, redistribuir ou sintetizar.
4. `T > N × Q`: todos usam 60 segundos; a cauda permanece sem intervalo e sem
   métricas inventadas.
5. Janelas válidas são positivas, ordenadas, não sobrepostas e contidas na
   sessão.

Exemplo confirmado: `T = 1.637 s`, 27 intervalos completos e último de 17 s.

### R6 — Plausibilidade

1. A política temporal e a plausibilidade são conceitos separados.
2. O último intervalo residual continua com a duração determinada por R5.
3. Cadência ou velocidade implausível gera indicador tipado, por exemplo
   `IMPLAUSIBLE_CADENCE`, sem redistribuir passos.
4. Série temporal implausível pode permanecer visível para diagnóstico, mas é
   inelegível para projeção granular Health Connect até aceite explícito da
   política definida em `014-02`.
5. Não inventar limiar global sem auditoria; a task de contratos registra o
   intervalo adotado e sua justificativa.

### R7 — Snapshot e distância canônica

Cada derivação congela:

```text
stepLengthMetersUsed
stepLengthSource
derivationMethod = FIXED_PERSONAL_STEP_LENGTH
derivationVersion
derivedAt
```

Para cada intervalo elegível:

```text
canonicalIntervalDistanceMeters = decodedSteps × stepLengthMetersUsed
```

A distância canônica da atividade é obrigatoriamente:

```text
canonicalDistanceMeters = Σ canonicalIntervalDistanceMeters
```

Consequências:

1. Não existe uma segunda distância canônica calculada somente pelo total de
   passos da atividade.
2. `totalSteps × stepLengthMeters` pode ser preservado como estimativa agregada
   diagnóstica, nunca como canônica.
3. Série temporal inelegível mantém `canonicalDistanceMeters` ausente.
4. Distância original, distância calibrada histórica e estimativa agregada não
   são fallback.
5. Intervalo com zero passos produz distância zero.

### R8 — Métricas derivadas

Com a mesma série congelada:

```text
cadência = passos ÷ duração em minutos
velocidade = distância ÷ duração
ritmo = duração ÷ distância em quilômetros
```

1. Zero passos produz cadência, velocidade e distância iguais a zero; ritmo é
   ausente.
2. Distância acumulada soma somente deltas canônicos em ordem estável.
3. Métricas totais usam a soma canônica e a duração coberta pela política; não
   usam média de médias.
4. Não suavizar, redistribuir ou normalizar a série na V1.

### R9 — Splits e BPM temporal

1. Produzir limites cumulativos em `1.000 m`, `2.000 m`, `3.000 m` e seguintes.
2. Cruzamento interno de limite usa interpolação linear dentro do intervalo.
3. Janelas de split usam fronteiras semiabertas `[start, end)`.
4. Cada amostra BPM vale por sample-and-hold no intervalo
   `[sampledAt_i, sampledAt_i+1)`, limitado pela sessão.
5. BPM do split é média ponderada pela duração da sobreposição temporal.
6. Sem duração de cobertura elegível, BPM do split permanece ausente.
7. Série inelegível não produz splits.

### R10 — Workflow de esteira

Para atividade `WATCH_D3_D4`:

1. Confirmar tipo `TREADMILL` não exige a calibração histórica da Fase 11 quando
   existir derivação canônica elegível.
2. O fluxo pode avançar para `READY_TO_SYNC` usando a distância canônica.
3. Calibração antiga permanece preservada como histórico/diagnóstico.
4. Distância calibrada não substitui distância canônica e não participa do
   Health Connect para `WATCH_D3_D4`.
5. Atividades phone históricas mantêm seu contrato legado, sem reabertura de
   produto.

### R11 — Consumidores canônicos

Resumo, calendário, detalhe, ritmo, velocidade, acumulado, splits, BPM por
quilômetro, gráficos e Health Connect usam exclusivamente a série canônica.
Distância original aparece somente em contexto diagnóstico claramente rotulado.
Ausência não vira zero, salvo intervalo real com zero passos.

### R12 — Reprocessamento explícito

1. Nova importação elegível pode criar derivação inicial.
2. Atividade existente só é reprocessada após ação confirmada.
3. Reprocessamento preserva `recordedAt`, revisão, tipo, título, observações,
   calibração histórica, dados raw e workflow compatível.
4. Reprocessamento não dispara sincronização automaticamente.
5. Atividade já sincronizada exige confirmação específica antes de preparar
   nova projeção.
6. Mesmos inputs e mesma `derivationVersion` não criam revisão inútil.

### R13 — Projeções filhas Health Connect

O manifesto local contém:

```text
Workout projection
├── EXERCISE_SESSION / singleton
├── DISTANCE / interval-000..N
├── SPEED / series
├── HEART_RATE / series
└── TOTAL_CALORIES_BURNED / singleton
```

1. Cada filho possui uma `HealthSyncOperation` própria.
2. A chave local inclui agregado, tipo e `projectionChildKey`.
3. Cada filho mantém estado, payload congelado, ID e versão próprios.
4. Retry idêntico reutiliza ID, versão e payload.
5. Alteração de payload incrementa versão monotônica por `clientRecordId`.
6. IDs não contêm valores de saúde.
7. Distância gera um `DistanceRecord` por intervalo positivo.
8. `SpeedRecord` usa uma amostra no ponto médio de cada intervalo elegível:

```text
sampledAt = intervalStart + effectiveDuration / 2
```

9. `HeartRateRecord` usa somente amostras reais.
10. `TotalCaloriesBurnedRecord` usa apenas total elegível da atividade.
11. Distância original/calibrada não é enviada para `WATCH_D3_D4`.

### R14 — Reconciliação Health Connect

1. Agregado legado próprio conhecido deve ser removido por ID exato antes da
   inserção de filhos de distância.
2. Agregado legado e série granular não podem coexistir.
3. Persistir checkpoint local antes e depois de cada efeito remoto.
4. Sem ID legado conhecido, falhar fechado; não apagar por janela temporal.
5. Filhos próprios obsoletos são removidos por ID exato.
6. Delete-before-insert pode criar janela temporária sem distância, mas deve ser
   retomável e idempotente.
7. Health Connect permanece write-only, foreground e explícito.

## Critérios de aceite

1. Perfil aceita valor manual e sugestão por altura somente após confirmação.
2. Parser `d4-v2` preserva raw e decodifica a sequência de referência em `3.737`.
3. Legado `d4-v1` preserva o u24 como raw sem backfill derivado automático.
4. Lacuna, duplicata ou primeiro índice não zero torna a série inelegível.
5. `1.637 s` produz último intervalo de 17 s.
6. Plausibilidade não altera duração nem passos.
7. Distância canônica total é exatamente a soma dos intervalos canônicos.
8. Estimativa por passos totais permanece apenas diagnóstica.
9. Métricas, acumulado, splits e UI usam a mesma série.
10. BPM por split usa sample-and-hold e sobreposição temporal semiaberta.
11. `TREADMILL` de relógio não depende de calibração histórica para usar a série
    canônica.
12. Reprocessamento é explícito, local e idempotente.
13. Uma operação Health Connect existe por filho da projeção.
14. `SpeedRecord` usa o ponto médio dos intervalos.
15. Agregado legado e filhos granulares não coexistem após reconciliação.
16. Retry não duplica records nem recalcula payload mutável.
17. Nenhuma task reabre GPS, rota, mapa, treino phone ou BLE live.

## Migrações previstas

### Migration local de métricas

Evolução aditiva `v8 → v9`, fechada em `014-02` e executada em `014-03`:

- `stepLengthMeters`, origem e instante de atualização no perfil;
- valores raw/measured e duração efetiva por intervalo;
- ponte `d4-v1` de `steps` legado para `rawMotionValue`;
- snapshot de derivação e série canônica;
- índices e constraints de derivação ativa por atividade.

Não deriva métricas, não acessa Health Connect, não altera workflow e não remove
estrutura histórica.

### Migration de projeções Health Connect

Evolução aditiva posterior, planejada em `014-12`:

- `projectionChildKey` na identidade natural;
- manifesto e payload congelado por filho;
- ação `INSERT`/`DELETE`, checkpoint, estado e versão;
- unicidade por atividade, tipo e filho.

A versão exata será definida após a migration local efetiva. Não misturar as duas
migrações na mesma task.

## Tasks

```text
014-01  Auditar D4, consumidores, legado e protocolo
014-02  Definir contratos canônicos e temporais
014-03  Migrar Room para perfil, raw/measured e derivação
014-04  Corrigir parser e incrementar parserVersion
014-05  Resolver duração efetiva, índices e plausibilidade
014-06  Persistir comprimento pessoal do passo
014-07  Derivar série e distância canônicas
014-08  Derivar cadência, velocidade, ritmo e acumulado
014-09  Calcular splits e BPM temporal
014-10  Integrar métricas, gráficos e splits à UI
014-11  Reprocessar atividades existentes explicitamente
014-12  Modelar e migrar projeções filhas Health Connect
014-13  Enviar distância granular e SpeedRecord
014-14  Reconciliar agregado e filhos legados
014-15  Executar validação física completa
014-16  Encerrar documentalmente a fase
```

## Fora do escopo

- GPS, rota, mapa e `ExerciseRoute`;
- treino phone e BLE live;
- importação silenciosa;
- WorkManager como objetivo funcional;
- TCX, FIT ou GPX;
- passada variável por cadência;
- suavização ou machine learning;
- calorias derivadas por intervalo;
- remoção de migrations, schemas ou tabelas históricas da Fase 013;
- leitura ampla do Health Connect.

## Riscos

- semântica D4 incompleta: preservar raw e marcar desconhecido;
- legado sem raw recuperável: falhar com segurança e exigir nova importação;
- último intervalo residual implausível: sinalizar sem redistribuir;
- conflito com workflow antigo de esteira: aplicar nova regra apenas a
  `WATCH_D3_D4`;
- fan-out Health Connect: usar uma operação por filho;
- delete-before-insert: checkpoint retomável;
- migrations extensas: separar persistência local de projeções remotas.
