# 033 — Sincronização unificada do relógio e dados diários granulares

- **Ticker:** `033`
- **Status:** `in_progress`
- **Tipo:** correção arquitetural de sincronização, protocolo D1, dados diários e projeção Health Connect
- **Branch:** `feat/033-unified-watch-daily-sync`
- **ADRs:** [ADR-030](../decisions/adr-030-unified-watch-sync-and-granular-daily-data.md), [ADR-031](../decisions/adr-031-d1-granular-steps-health-connect.md), [ADR-032](../decisions/adr-032-sync-entrypoints-and-watch-management.md), [ADR-033](../decisions/adr-033-d1-granular-distance-active-calories.md)
- **Base de integração:** `main` após conclusão da Spec 032

## Contexto

A Spec 032 consolidou `d4-v4`, distância nativa do relógio, calibração contextual
por referência externa e Room v17. A Spec 033 unifica as antigas leituras BLE
separadas de Home/FEA1, Saúde/D1 e Exercícios/D3-D4 em um único watch sync iniciado
pela Home e promove D1 para fatos diários completos.

A direção arquitetural é:

```text
sincronizar relógio != sincronizar Health Connect
```

Room continua sendo fonte operacional local. Health Connect continua write-only e
somente recebe efeitos por ação explícita.

## Evidência física que corrigiu o contrato

### D1 também contém fatos de workout

Validação pós-033-02/033-03 mostrou:

- `Σ D1.steps == FEA1.steps` em leituras sucessivas;
- corrida D4 em que D1 fechou os mesmos passos/distância e praticamente as mesmas
  calorias do workout;
- workout D4 locomotoramente nulo que ainda intersectava bucket D1 não nulo.

Portanto não existe regra global `D1 fora / D4 dentro` e overlap temporal não
autoriza suprimir D1 indiscriminadamente.

Evidência: `docs/integration/033-03-d1-v2-physical-validation.md`.

### FEA1 pode ficar à frente do último D1 sem divergência

Validação pós-033-05 mostrou D1 estruturalmente completo e FEA1 alguns minutos à
frente de `ΣD1`. Isso é uma cauda temporal legítima entre a leitura agregada atual
e os buckets discretos D1.

Logo, `FEA1 > ΣD1` não é automaticamente `DIVERGENT`, e `FEA1 - ΣD1` nunca pode
ser transformado em intervalo ou record sintético.

Evidência: `docs/integration/033-05-unified-watch-sync-physical-validation.md`.

### A janela D1 foi promovida fisicamente

Os dumps de 2026-09-13 e a revalidação de 2026-09-14 fecharam a direção temporal
observada do D1:

```text
sample D1 em t
=> bucket [t - 5 minutos, t)
```

A revalidação real de STEPS confirmou records D1 de 300 s, cutover granular em
2026-09-14, ausência de DailySummary/STEPS no novo dia e `WorkoutSteps`
`BLOCKED/ALREADY_COVERED` sem pendência acionável.

Os mesmos dumps mostraram buckets parados com `0 passos / 0 m / 0 kcal` e calorias
D1 acompanhando a energia de workout. A ADR-033 promove essa energia D1 para
`ActiveCaloriesBurnedRecord`, sem alterar a família D4 já existente de
`TOTAL_CALORIES_BURNED`.

## Objetivo

```text
Home pull-to-refresh
        |
        v
WatchSyncCoordinator
  |       |       |
 FEA1     D1     D3/D4
  |       |       |
  +-------+-------+
          |
          v
   persistência local
          |
     +----+----+
     |         |
 read models  fila HC local
                   |
                   v
             sync explícito
```

Preservar arquitetura watch-first, review-before-sync, ausência de GPS/rota em
produção e compatibilidade histórica.

## Decisões obrigatórias

### R1 — Home é o único entrypoint de leitura do relógio

O pull-to-refresh da Home coordena FEA1, D1 e D3/D4. Saúde não inicia D1 nem
medição manual como fluxo de produto; Exercícios não inicia importação D3/D4.
Abrir tela, voltar ao foreground ou trocar filtro não inicia histórico BLE.

### R2 — Um coordinator controla gate, sessão e handshake

`WatchSyncCoordinator` ou equivalente deve:

- usar owner foreground compartilhado (`WATCH_SYNC`);
- reutilizar sessão BLE quando possível;
- executar handshake uma vez por tentativa quando necessário;
- preservar FEA1, D1 e D3/D4 como componentes independentes;
- preservar sucesso já persistido quando componente posterior falhar;
- expor resultado global `COMPLETED`/`PARTIAL`/`FAILED` e status por componente.

### R3 — FEA1 permanece total diário autoritativo de passos

`DailySummary.steps`/FEA1 é a melhor leitura do total diário atual. FEA1 não
fornece distribuição temporal e não autoriza fabricação de buckets.

### R4 — Parser D1 v2 completo

Layout observado:

```text
byte 0      D1
1..2        currentPosition u16 LE
3..4        totalPositions u16 LE
5           dayOffset/hour packed
6..7        caloriesRaw u16 BE
8..9        distanceRaw u16 BE
10..11      steps u16 BE
12..13      sportValue u16 BE
14          bpmRaw u8
15          pressureHighRaw u8
16          pressureLowRaw u8
17          wearRaw u8
18          tempOneRaw u8
19          minute u8
```

Conversões confirmadas:

```text
caloriesKilocalories = caloriesRaw / 10.0
distanceMeters       = distanceRaw
```

`pressureHighRaw`, `pressureLowRaw`, `wearRaw`, `tempOneRaw` e semântica
fisiológica de `sportValue` permanecem raw.

### R5 — BPM inválido não invalida D1

Faixa D1 v2 adotada: `30..200 BPM`. Fora dela, BPM fica ausente, mas passos,
distância, calorias, `sportValue`, raws e estrutura multipart permanecem válidos.
Conclusão do collector depende da estrutura/posição, não da validade do BPM.

### R6 — Persistir D1 como fatos completos

`watch_daily_d1_samples` preserva identidade natural por dispositivo/data/posição,
data/hora/instante/zoneId, passos, distância, calorias, sportValue, BPM nullable,
raws e timestamps de importação/atualização. Room v18 é aditivo e não fabrica
histórico D1 que nunca foi persistido.

### R7 — D1 é linha diária; D4 é fonte do workout

```text
FEA1 -> total diário de passos
D1   -> linha granular diária, inclusive fatos ocorridos em workout
D4   -> detalhe/canônico do workout
```

Consequências:

- não suprimir bucket D1 por mera interseção global;
- não somar D1 + D4 como fontes independentes;
- comportamento D4 usa intervalos/ranges/somas, nunca header total como fonte.

### R8 — Autoridade é definida por métrica

#### Passos

- FEA1 = total diário/controle;
- D1 = distribuição granular remota a partir do cutover efetivo;
- D4 = passos/cadência do workout;
- D4 não aumenta novamente o total diário;
- `WorkoutSteps` fica localmente `BLOCKED/ALREADY_COVERED` quando a linha D1 granular cobre o workout.

Cobertura local:

```text
FEA1 == ΣD1 + D1 completo
  -> COMPLETE

FEA1 > ΣD1 + D1 completo + último bucket recente
  -> RECENT_TAIL

FEA1 < ΣD1
  -> DIVERGENT

D1 estruturalmente incompleto
  -> PARTIAL
```

Nunca criar residual temporal de `FEA1 - ΣD1`.

#### Distância

- D1 positivo pode gerar `DistanceRecord` no bucket `[t-5min,t)`;
- D4 continua dono da distância canônica do workout;
- se o bucket D1 tocar um D4 com distância positiva, a projeção D1 `DISTANCE`
  daquele bucket fica fail-closed;
- D4 com distância zero/ausente não suprime D1 válido;
- bucket parcialmente sobreposto não é dividido nem rateado;
- nunca calcular total como `D1 + D4`.

#### Calorias

- D1 usa `raw / 10.0` e, com base na evidência física, representa energia ativa
  para a projeção Health Connect;
- D1 positivo pode gerar `ActiveCaloriesBurnedRecord` no bucket `[t-5min,t)`;
- se o bucket D1 tocar um D4 com calorias positivas, a projeção D1 de calorias
  daquele bucket fica fail-closed;
- D4 com calorias zero/ausentes não suprime D1 válido;
- D4 preserva nesta entrega o contrato histórico `TOTAL_CALORIES_BURNED`;
- migrar D4 para energia ativa exige decisão/reconciliação futura explícita;
- bucket parcialmente sobreposto não é dividido nem rateado.

#### Frequência cardíaca

- D1 fornece pontos automáticos válidos;
- D4 fornece amostras do workout;
- D1 só é omitido no read model combinado quando houver cobertura D4 válida;
- não criar BPM sintético/interpolado.

### R9 — Substituição D1 -> D4 exige prova por métrica

Interseção temporal isolada não suprime o bucket inteiro. A decisão usa ownership
por métrica:

```text
D4 metric > 0 + overlap
  -> D4 possui aquela métrica no intervalo
  -> D1 da mesma métrica fica fail-closed

D4 metric == 0/ausente
  -> D1 positivo continua elegível
```

Não dividir bucket D1 proporcionalmente por inferência.

### R10 — Comprimento de passo sai da experiência normal

Remover da UI normal edição/sugestão/perfis/histórico de calibração por passada.
Preservar domínio/schema legacy para d4-v1/v2/v3, v4 inelegível, snapshots,
reprocessamento e fallback técnico. Calibração D4 por referência externa permanece.

### R11 — Saúde vira leitura/análise

Remover entrypoints de produto para medição manual de FC, importação D1 e seus
retry/cancel/progresso BLE. Histórico existente permanece. Peso local continua
independente/editável.

### R12 — Exercícios vira revisão/análise/sync

Remover CTA de buscar treinos e state machine de importação BLE da tela.
Exercícios continua responsável por lista, review/classificação, calibração,
referência externa, manutenção e sync explícito de workout revisado.

### R13 — Watch sync não escreve automaticamente no Health Connect

```text
watch sync
  -> ler
  -> validar
  -> persistir
  -> preparar/garantir estado local
  -> NÃO chamar syncPending automaticamente
```

Efeito remoto ocorre por ação explícita, incluindo botão Sync da Home,
`Settings -> Sincronizar agora` e sync explícito de workout.

### R14 — Cutover de STEPS granular é efetivo e exclusivo por data

A ADR-031 e a revalidação física promoveram o substituto remoto seguro.

Contrato vigente:

```text
data < cutover efetivo
  -> DailySummary/FEA1 STEPS incremental legacy

data >= cutover efetivo
  + já existe DailySummary/STEPS na data
  -> preservar família legacy daquela data
  -> não misturar D1 STEPS granular

data >= cutover efetivo
  + não existe DailySummary/STEPS na data
  -> D1 STEPS granular
  -> WorkoutSteps resolvido localmente como ALREADY_COVERED
```

O piso observado do cutover granular é **2026-09-14**. Não existe flag de build
que reinterprete silenciosamente datas antigas.

### R15 — Calorias D1 são ativas; D4 não é migrado implicitamente

A combinação de buckets estacionários em zero e acompanhamento do gasto observado
em movimento é evidência suficiente para o uso de `ActiveCaloriesBurnedRecord` no
D1. A permissão necessária é `WRITE_ACTIVE_CALORIES_BURNED`.

Isso **não** redefine records D4 históricos. Enquanto não existir política de
migração/reconciliação própria, D4 continua usando `TOTAL_CALORIES_BURNED`.

### R16 — Idempotência, partial success e sync explícito global

D1 aceita reimportação sem duplicar; D3/D4 preserva identidade CRC/import. Falha
BLE posterior não faz rollback de componente já persistido.

`Settings -> Sincronizar agora` e o botão Sync da Home são a mesma ação explícita
global e devem:

1. processar a fila local existente;
2. poder executar múltiplos lotes finitos enquanto houver progresso;
3. preservar a ordenação causal interna já estabelecida para passos/créditos;
4. em seguida, iniciar `syncActivity()` para cada Watch `READY_TO_SYNC`;
5. nunca iniciar primeira projeção para `PENDING_REVIEW`, `PENDING_CALIBRATION`,
   `DISCARDED` ou `SYNCED`.

O loop global é foreground e finito. Não usar “mais recente primeiro” se isso
puder inverter cadeias incrementais.

### R17 — Privacidade e produto watch-first permanecem

Não introduzir GPS/rota/mapa, Adidas/Strava, polling BLE background, WorkManager de
relógio ou logs com payload BLE integral/dados de saúde sensíveis.

### R18 — Payloads D1 granulares são congelados por família

- schema v4 permanece para STEPS D1;
- schema v5 representa `DISTANCE` ou `ACTIVE_CALORIES_BURNED` D1;
- identidade remota D1 inclui `sampleId + recordType`;
- retry reutiliza valor, janela, offsets, identidade e versão congelados;
- valores zero não geram record;
- nenhuma migration Room é necessária para a extensão de métricas.

Se um D1 `DISTANCE`/`ACTIVE_CALORIES_BURNED` já estiver `SYNCED` e um D4 tardio
sobreposto da mesma métrica for preparado, a primeira projeção D4 correspondente
falha fechado para não criar duplicidade remota.

## Health Connect no contrato atual

```text
STEPS
  total/controle local: FEA1
  pós-cutover: D1 granular [t-5min,t)
  legacy anterior/existente na data: DailySummary incremental
  WorkoutSteps: ALREADY_COVERED quando D1 granular cobre o workout
  FEA1-D1 residual: nunca record sintético

DISTANCE
  D1 positivo fora de ownership D4: DistanceRecord granular
  D4 positivo: distância canônica do workout
  bucket D1 que toca D4 positivo: fail-closed para DISTANCE
  D4 zero: não suprime D1

CALORIES
  D1 positivo fora de ownership D4: ActiveCaloriesBurnedRecord granular
  D4 existente: TOTAL_CALORIES_BURNED
  bucket D1 que toca D4 com calorias positivas: fail-closed para D1 calories
  D4 zero: não suprime D1

HEART_RATE
  D1 automático elegível
  D4 no contexto do workout quando cobertura válida

WORKOUT
  READY_TO_SYNC: pode ser preparado pelo sync explícito global
  PENDING_REVIEW/PENDING_CALIBRATION: nunca primeira projeção global
  filhos D4 elegíveis seguem manifesto/frozen payload/guards existentes
```

## Sequência de execução

```text
033-01 Auditoria/contratos                         ✓
   |\
   | +--> 033-02 Parser/collector D1 v2            ✓
   | +--> 033-03 Persistência Room v18             ✓
   |          |
   +----------+
              v
        033-04 Composição diária                   ✓
              |
        +-----+-----+
        |           |
        v           v
033-05 Watch sync  033-06 HC/cutover               ✓
        ✓             ✓
        +-----+-----+
              v
033-07 Produto/UI                           ✓
              |
        033-08 Read models/compatibilidade
              |
        033-09 Validação física/fechamento

follow-up após evidência granular:
033-10 D1 DISTANCE + ACTIVE_CALORIES_BURNED         ✓
```

A implementação e validação física da 033-10 foram concluídas. O alinhamento do
helper legado `RoomRepositories.hasSupportedHealthSyncPair()` para agregados D1
pertence à 033-08 e não altera o writer granular.

## Validação obrigatória

Mudanças de código/Gradle:

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem pedido explícito do usuário.

A validação física deve exercitar Champion C001 real, idempotência, composição
D1/D4 por métrica, `RECENT_TAIL`, ausência de residual sintético, ausência de
auto-sync HC, STEPS granular, `WorkoutSteps` coberto, `DISTANCE` D1 granular,
`ACTIVE_CALORIES_BURNED` D1 granular, buckets de fronteira fail-closed e D4 zero
sem suprimir D1 válido. O dump pré-sync deve provar a preparação local antes do
efeito remoto; o dump pós-sync deve provar estados/ausência de duplicação local.
