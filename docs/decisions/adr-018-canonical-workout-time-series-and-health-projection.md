# ADR-018 — Série temporal canônica e projeção Health Connect de treinos

- **Status:** aceita como decisão documental da Fase 014
- **Data:** 2026-08-05

## Contexto

A ADR-017 definiu a direção de produto: Champion C001 controla início e fim, a
distância canônica de `WATCH_D3_D4` deriva de passos válidos e comprimento
pessoal do passo, e sincronização permanece explícita. A implementação histórica
partiu de valores raw misturados a interpretações medidas e métricas calculadas;
intervalos tinham duração fixa e o Health Connect recompunha um agregado mutável
no envio.

Esta ADR define mecanismo durável para reprodução, temporalidade, compatibilidade
com legado e idempotência. Não redefine o abandono de treinos phone, a política
write-only geral nem os limites de GPS/rota já registrados.

## Decisão

### 1. Fluxo raw, measured, derived e projection

```text
raw de origem
    ↓ normalização versionada
measured
    ↓ derivação determinística
derived snapshot
    ↓ projeção congelada
filhos Health Connect
```

- **Raw:** valor recebido sem semântica não comprovada.
- **Measured:** fato normalizado com semântica confirmada.
- **Derived:** duração efetiva, distância canônica, cadência, velocidade, ritmo,
  acumulado, splits, plausibilidade e motivos tipados de ausência.
- **Projection:** payload congelado e identidade de cada record remoto.

Revisão do usuário não sobrescreve raw ou measured. Retry remoto nunca recalcula
a atividade mutável.

### 2. `stepLengthMeters`

Contratos novos usam `stepLengthMeters` para a distância de um passo individual.
O valor precisa ser positivo, finito, persistido com origem e confirmado
explicitamente. Sugestão não aceita não é perfil. Valor pessoal de referência não
é constante global nem fallback.

### 3. Snapshot por atividade

Cada derivação congela pelo menos:

- `stepLengthMetersUsed`;
- `stepLengthSource`;
- `derivationMethod = FIXED_PERSONAL_STEP_LENGTH`;
- `derivationVersion`;
- `derivedAt` para auditoria;
- política temporal, elegibilidade e plausibilidade;
- série derivada ou motivo de ausência.

Mudança posterior de perfil não altera snapshot. `derivedAt`, parser version e
client record version são conceitos distintos.

### 4. Compatibilidade `d4-v1` e `d4-v2`

O parser histórico `d4-v1` persistiu o u24 empacotado de `bytes[7..9]` no campo
legado `steps`. A evolução aditiva pode copiar esse valor para `rawMotionValue`
apenas quando origem e parser version forem conhecidos. Isso não é decodificação
nem backfill derivado.

O parser corrigido usa nova versão, inicialmente `d4-v2`, e produz:

```text
rawMotionValue = u24 LE bytes[7..9]
decodedSteps = rawMotionValue ushr 16
```

Os 16 bits inferiores, distância temporal raw e calorias temporais raw permanecem
sem semântica funcional comprovada.

### 5. Índices e duração efetiva

A série V1 exige índices únicos e contínuos `0..N-1`. Lacuna, duplicata, primeiro
índice não zero ou ordem inconsistente torna a série inelegível, preservando os
fatos. O marcador persistido `ORDER_VALIDATED` registra somente que a origem foi
observada em ordem estritamente crescente antes da ordenação do Room; lacunas e
primeiro índice não zero continuam sendo rejeitados pelo `TemporalSeriesValidator`
e produzem snapshot inelegível com distância canônica ausente. `OUT_OF_ORDER` fica
reservado para ordem não crescente ou índice duplicado, e registros legados sem
marcador falham fechados.

Com duração total `T`, `N` intervalos elegíveis e quantum `Q = 60 s`:

- `T == NQ`: todos usam `Q`;
- `(N−1)Q < T < NQ`: primeiros `N−1` usam `Q`; último usa o restante;
- `T <= (N−1)Q`: série inelegível;
- `T > NQ`: todos usam `Q`; cauda permanece sem intervalo e sem síntese.

Janelas válidas são positivas, ordenadas, não sobrepostas e contidas na sessão.

### 6. Plausibilidade separada da política temporal

A duração residual definida no item anterior não é redistribuída mesmo quando
produz cadência ou velocidade implausível. A derivação registra um indicador
tipado, como `IMPLAUSIBLE_CADENCE`, preserva raw/measured e pode bloquear a
projeção granular remota. Limiar e política exatos pertencem aos contratos de
`014-02`, após auditoria.

Quando não há indicador tipado, a política inicial
`NO_GLOBAL_THRESHOLD_BLOCKS_PROJECTION` continua bloqueando a projeção: ausência
de limiar não vira limiar implícito. O usuário pode confirmar explicitamente a
série sem indicadores; essa ação atualiza somente `plausibilityPolicy` do
snapshot ativo para `EXPLICITLY_ACCEPTED`, sem recalcular fatos, série ou
payloads. Indicadores `IMPLAUSIBLE_*` impedem aceite genérico.

### 7. Distância e série canônicas

Somente intervalos measured elegíveis e `stepLengthMetersUsed` produzem distância
canônica:

```text
canonicalIntervalDistanceMeters = decodedSteps × stepLengthMetersUsed
canonicalDistanceMeters = Σ canonicalIntervalDistanceMeters
```

Não existe uma segunda distância canônica calculada apenas pelo total de passos.
`totalSteps × stepLengthMetersUsed` pode existir como estimativa agregada
diagnóstica, nunca como fallback ou fonte funcional.

Se a série temporal for inelegível, `canonicalDistanceMeters` permanece ausente.
Distância original, calibrada histórica e raw temporal não participam de resumo,
ritmo, velocidade, splits, UI ou Health Connect para `WATCH_D3_D4`.

### 8. Métricas derivadas

A V1 usa a mesma série congelada:

```text
cadência = passos ÷ duração em minutos
velocidade = distância ÷ duração
ritmo = duração ÷ distância em quilômetros
```

Zero passos produz distância, cadência e velocidade zero; ritmo ausente. Não há
suavização, residual, escala para fechar total ou média de médias.

### 9. Splits e frequência cardíaca

Splits usam limites cumulativos de 1 km e interpolação linear quando o limite
cai dentro de um intervalo.

Janelas são semiabertas `[start, end)`. Cada amostra BPM usa sample-and-hold em
`[sampledAt_i, sampledAt_i+1)`, limitada pela sessão. BPM do split é média
ponderada pela duração de sobreposição. Sem cobertura elegível, permanece
ausente.

### 10. Esteira importada do relógio

Para `WATCH_D3_D4`, confirmar `TREADMILL` não exige calibração histórica quando a
série canônica é elegível. O workflow pode avançar para `READY_TO_SYNC` usando a
distância canônica. Se atividade já revisada estiver em `PENDING_CALIBRATION`, a
criação do snapshot elegível e promoção para `READY_TO_SYNC` ocorrem na mesma
transação; falha da transição desfaz snapshot parcial. `PENDING_REVIEW` continua
sendo promovida pelo `submitReview`. Calibrações existentes permanecem
históricas/diagnósticas e não substituem a série canônica. Contratos phone
históricos não são reabertos.

### 11. Reprocessamento explícito

Nova importação pode criar derivação inicial. Atividade existente só recebe novo
snapshot após ação confirmada. Mudança de perfil, migration, abertura do app,
reimportação ou scheduler não recalcula silenciosamente. Reprocessar preserva
revisão, `recordedAt`, tipo, título, observações, calibração histórica e raw; não
ressincroniza automaticamente.

A derivação local faz preflight de snapshot, filhos e round-trip com intervalos
resolvidos antes de qualquer escrita DAO. Perfil ausente ou série inelegível
produz ausência segura; inconsistência de preflight durante importação atravessa
`withWriteTransaction` como exceção controlada e desfaz fatos importados parciais.

### 12. Projeções filhas Health Connect

```text
Workout projection
├── EXERCISE_SESSION / singleton
├── DISTANCE / interval-000..N
├── SPEED / series
├── HEART_RATE / series
└── TOTAL_CALORIES_BURNED / singleton
```

Cada filho possui uma `HealthSyncOperation` própria. A identidade natural inclui:

```text
aggregate + recordType + projectionChildKey
```

Cada operação congela payload, ação, ID, versão e estado. Isso evita uma segunda
fila interna dentro de uma operação agregada e permite retry/falha parcial por
filho.

Nenhum filho é preparado antes do aceite explícito quando a série está sem
indicadores e sob `NO_GLOBAL_THRESHOLD_BLOCKS_PROJECTION`. Atividade `SYNCED` não
aceita alteração silenciosa de política nem reprocessamento.

Durante a implementação, até a projeção granular estar disponível, o writer
legado bloqueou `WATCH_D3_D4` em todas as transições de atividade (`startSync`,
`markSynced` e `markSyncFailed`) e na recuperação de `SYNCING`. Após a entrega da
projeção granular e da reconciliação, operações elegíveis do relógio usam filhos
canônicos; operações de atividades phone continuam seguindo seu contrato legado.

Filho temporal usa atividade, tipo e índice estável da fonte. IDs não contêm
passo pessoal, BPM, distância ou outro valor de saúde. `clientRecordVersion` é
monotônico por `clientRecordId`; retry idêntico reutiliza ID, versão e payload.

`DistanceRecord` existe somente para intervalo positivo. `SpeedRecord` usa uma
amostra no ponto médio de cada intervalo:

```text
sampledAt = intervalStart + effectiveDuration / 2
```

`HeartRateRecord` usa amostras reais. Calorias usam somente total elegível.

### 13. Reconciliação e remoção

A projeção local persiste conjunto esperado, enviado e obsoleto. Remoção usa
somente IDs próprios conhecidos e exatos.

Agregado legado conhecido deve ser removido antes da série granular para impedir
dupla contagem. Checkpoint local é persistido antes e depois do efeito remoto.
Sem identidade legada conhecida, falhar fechado; não usar leitura ampla nem
deleção por janela temporal.

Delete-before-insert cria janela temporária sem distância, mas o fluxo precisa
retomar com os mesmos IDs, versões e payloads congelados.

### 14. Migrações separadas

A persistência local de perfil, raw/measured e derivação é uma migration aditiva
independente da evolução da fila/projeção Health Connect. Não misturar as duas
mudanças em uma task ou migration única.

A migration local pode criar a ponte `d4-v1` raw, mas não deriva métricas, não
muda workflow e não executa efeito remoto. A migration de projeções é posterior
e adiciona manifesto, `projectionChildKey`, payload, ação e checkpoints.

## Consequências

- uma única distância canônica para todos os consumidores;
- atividades sem série elegível preservam ausência segura;
- histórico `d4-v1` pode ser reprocessado quando o raw legado for recuperável;
- último intervalo parcial permanece determinístico, com plausibilidade separada;
- esteira do relógio deixa de depender de calibração incompatível;
- retry Health Connect é granular por filho;
- inconsistência de derivação não confirma importação parcial;
- writer legado permanece fechado para atividades do relógio até projeção
  canônica granular;
- maior volume local e duas evoluções de schema;
- delete-before-insert exige retomada explícita.

## Estado efetivo e limites de validação

A decisão foi implementada inicialmente na Fase 014 com parser `d4-v2`, Room v9
por `MIGRATION_8_9`, projeções em schema v10 por `MIGRATION_9_10`, snapshots com
`stepLengthMeters` individual congelado, política temporal histórica `T/N/60 s` e
filhos Health Connect com IDs, versões, payloads, retry e reconciliação
determinísticos. A Spec 021 supersedeu a política temporal para `WATCH_D3_D4`:
runtime `d4-v3`, timeline normalizada e Room schema v11 por `MIGRATION_10_11`.
O provider continua sendo destino write-only: a evidência de records e permissões
é observação manual no dispositivo, não leitura executada pelo aplicativo.

A validação física final não cobre todos os contratos. Gate F foi aprovado apenas
para os cenários exercitados de autorização, falha parcial e retry após
`014-17`; os demais gates físicos foram registrados como `N/A` quando não havia
fixture ou repetição suficiente. Midpoint, delete-before-insert, identidade
congelada em toda a matriz e variações de firmware permanecem limites de
evidência, não mudanças na decisão.

## Alternativas rejeitadas

- total canônico independente da soma temporal;
- usar distância original ou calibrada como fallback;
- preencher lacunas ou distribuir residual;
- interpretar 16 bits inferiores sem evidência;
- usar `0,8818 m` como constante global;
- recalcular dinamicamente no mapper/UI;
- uma operação com subfila interna de múltiplos records;
- média simples/global de BPM por split;
- timestamp indefinido para `SpeedRecord`;
- reprocessamento ou resync automático;
- leitura remota ampla ou delete temporal.

## Relação com decisões anteriores

- **ADR-017:** direção de produto e distância derivada de passos.
- **ADR-012:** Room local-first, Health Connect write-only e sync explícito.
- **ADR-016:** integridade temporal e preservação de `recordedAt`.
- **ADR-014:** registro histórico, sem autorização nova de rota phone.

## Limites

Esta ADR não define schema SQL final, fórmula nova de sugestão por altura,
limiar final de plausibilidade, WorkManager, GPS, rota, mapa, BLE live, calorias
por intervalo, suavização, remoção de legado da Fase 013 ou implementação já
concluída.
