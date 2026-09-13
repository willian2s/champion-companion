# 033 — Sincronização unificada do relógio e dados diários granulares

- **Ticker:** `033`
- **Status:** `in_progress`
- **Tipo:** correção arquitetural de sincronização, protocolo D1, dados diários e projeção Health Connect
- **Branch:** `feat/033-unified-watch-daily-sync`
- **ADR:** [ADR-030](../decisions/adr-030-unified-watch-sync-and-granular-daily-data.md)
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
autoriza suprimir D1.

Evidência: `docs/integration/033-03-d1-v2-physical-validation.md`.

### FEA1 pode ficar à frente do último D1 sem divergência

Validação pós-033-05 mostrou D1 estruturalmente completo e FEA1 alguns minutos à
frente de `ΣD1`. Isso é uma cauda temporal legítima entre a leitura agregada atual
e os buckets discretos D1.

Logo, `FEA1 > ΣD1` não é automaticamente `DIVERGENT`, e `FEA1 - ΣD1` nunca pode
ser transformado em intervalo/StepsRecord sintético.

Evidência: `docs/integration/033-05-unified-watch-sync-physical-validation.md`.

### Cutover não pode eliminar STEPS antes de existir substituto seguro

A primeira implementação da 033-06 foi validada em instalação limpa. O cutover
solicitado entrou no próprio dia, mas D1 granular remoto permanecia `deferred`.
Com `DailySummary STEPS` desligado, nenhum StepsRecord era preparado. A mesma
validação mostrou que `Settings -> Sincronizar agora` não iniciava a primeira
projeção de uma atividade Watch `READY_TO_SYNC`.

Correção: a intenção/data de cutover continua persistida, mas o **cutover efetivo
de STEPS permanece adiado** enquanto D1 granular não tiver representação remota
comprovadamente segura. Nesse período o caminho incremental FEA1 das Specs 016/027
continua ativo.

Evidência: `docs/integration/033-06-health-connect-clean-install-validation.md`.

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

- não suprimir bucket D1 por mera interseção;
- não somar D1 + D4 como fontes independentes;
- comportamento D4 usa intervalos/ranges/somas, nunca header total como fonte.

### R8 — Autoridade é definida por métrica

#### Passos

- FEA1 = total diário;
- D1 = distribuição granular/diagnóstico;
- D4 = passos/cadência do workout;
- D4 não aumenta novamente o total diário.

Cobertura:

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

- D1 = linha diária nativa local quando elegível;
- D4 = distância canônica do workout;
- não calcular diário como `D1 + D4`;
- não voltar a `steps × stepLength` no caminho normal se houver D1 nativo;
- qualquer substituição D1 por D4 exige prova de cobertura/não duplicação.

#### Calorias

- D1 usa `raw / 10.0`;
- D4 usa `raw / 1000.0`;
- não somar D1 + D4;
- semântica Health Connect permanece bloqueada até evidência suficiente.

#### Frequência cardíaca

- D1 fornece pontos automáticos válidos;
- D4 fornece amostras do workout;
- D1 só é omitido no read model combinado quando houver cobertura D4 válida;
- não criar BPM sintético/interpolado.

### R9 — Substituição D1 -> D4 exige prova

Interseção temporal não é prova suficiente. Sem prova determinística de que a
contribuição D1 substituída corresponde ao mesmo fato D4 e não contém conteúdo
externo, manter D1 no diário e D4 no workout sem somá-los. Não dividir bucket D1
proporcionalmente por inferência.

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

Efeito remoto ocorre por ação explícita, incluindo `Settings -> Sincronizar agora`
e sync explícito de workout.

### R14 — Cutover de STEPS só é efetivo quando houver substituto remoto seguro

Persistir por dispositivo uma intenção/data civil de cutover. Essa data não pode
eliminar uma representação remota de passos antes de existir substituto granular
seguro.

Contrato atual:

```text
D1 granular remoto ainda DEFERRED
  -> data solicitada permanece persistida
  -> cutover efetivo de STEPS permanece adiado
  -> DailySummary/FEA1 continua incremental pelas Specs 016/027
  -> WorkoutSteps continua protegido por créditos/ledgers
  -> D1 granular permanece local

D1 granular remoto comprovado no futuro
  -> exige ativação/migração explícita
  -> somente então desligar novos DailySummary STEPS
```

Não existe flag de build que reinterprete silenciosamente datas antigas.

Quando o cutover real for implementado, a projeção pós-cutover não poderá assumir
`D1 fora + D4 dentro`; deverá preservar o total FEA1 e provar não sobreposição com
`WorkoutSteps`.

### R15 — Calorias exigem gate semântico próprio

Não escolher `ActiveCaloriesBurned` versus `TotalCaloriesBurned` por inferência.
Calorias diárias permanecem locais/deferred enquanto a semântica e a cobertura
D1/D4 não estiverem comprovadas.

### R16 — Idempotência, partial success e sync explícito global

D1 aceita reimportação sem duplicar; D3/D4 preserva identidade CRC/import. Falha
BLE posterior não faz rollback de componente já persistido.

`Settings -> Sincronizar agora` é uma ação explícita global e deve:

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

## Health Connect no contrato atual

```text
STEPS
  total local: FEA1
  remoto atual: DailySummary/FEA1 incremental + créditos Spec 027
  D1 granular remoto: deferred até prova de intervalo/não sobreposição
  FEA1-D1 residual: nunca record sintético

WORKOUT
  READY_TO_SYNC: pode ser preparado pelo sync explícito global
  PENDING_REVIEW/PENDING_CALIBRATION: nunca primeira projeção global
  filhos D4 elegíveis seguem manifesto/frozen payload/guards existentes

DISTANCE
  diário D1: local/deferred
  workout D4: pode ser publicado quando contrato do workout provar elegibilidade
  nunca publicar D1 diário + D4 sobrepostos sem prova

HEART_RATE
  D1 automático elegível
  D4 no contexto do workout quando cobertura válida

CALORIES
  diário: local/deferred
  remoto: somente com semântica comprovada
```

## Sequência de execução

```text
033-01 Auditoria/contratos
   |\
   | +--> 033-02 Parser/collector D1 v2
   | +--> 033-03 Persistência Room v18
   |          |
   +----------+
              v
        033-04 Composição diária
              |
        +-----+-----+
        |           |
        v           v
033-05 Watch sync  033-06 HC/cutover
        ✓           ✓* correção pós-validação
        +-----+-----+
              v
        033-07 Produto/UI
              |
        033-08 Read models/compatibilidade
              |
        033-09 Validação física/fechamento
```

A correção pós-validação da 033-06 deve ser revalidada fisicamente antes de iniciar
033-07.

## Validação obrigatória

Mudanças de código/Gradle:

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem pedido explícito do usuário.

A validação física deve exercitar Champion C001 real, migration v17->v18->v19,
idempotência, composição D1/D4 por métrica, `RECENT_TAIL`, ausência de residual
sintético, partial success, ausência de auto-sync HC, fallback STEPS enquanto D1
remoto estiver deferred, sync global de `READY_TO_SYNC` e ausência de duplicação.
