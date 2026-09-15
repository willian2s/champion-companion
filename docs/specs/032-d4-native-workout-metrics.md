# 032 — D4 v4: métricas nativas, distância e calibração por referência

- **Ticker:** `032`
- **Status:** `completed`
- **Tipo:** correção arquitetural de parser, derivação e calibração de treinos `WATCH_D3_D4`
- **Branch:** `feat/032-d4-native-workout-metrics`
- **ADR:** [ADR-029](../decisions/adr-029-d4-native-metrics-and-distance-calibration.md)
- **Base de integração:** `main`

## Contexto

O Champion Companion importa treinos gravados no Champion C001 por D3/D4 e hoje
usa `parserVersion = d4-v3`. A linha atual preserva a duração oficial do treino,
normaliza `T/N` sobre a quantidade real de samples e deriva distância canônica a
partir de passos por intervalo multiplicados por um comprimento de passo.

A inspeção do APK Champion Smartstyle e a validação física posterior mostraram
que o layout fixo do Sport Model contém mais informação do que o parser atual
promove. Para cada pacote D4 de dados, o firmware transmite BPM, `sportValue`,
passos `u16`, calorias raw `u16`, distância `u16` em metros e estado de pausa. O
header D4 também carrega totais de passos, distância, calorias, sport count,
`recordCount`, pausas, CRC e tipo de esporte.

A auditoria `032-01` consolidou o APK e 34 blocos D4 completos preservados no
banco. Nos 34 blocos, `recordCount`, passos, distância, calorias raw, sport count
e CRC fecharam entre header e série temporal. A mesma auditoria confirmou
`distance = metros` e `kcal = caloriesRaw / 1000.0` para o layout fixo observado.

Após a conclusão da Spec 031, a branch 032 foi atualizada sobre a nova `main` e a
`032-01` foi revalidada em `docs/integration/032-01-post-031-reaudit.md`. Nenhuma
decisão técnica do contrato D3/D4 v4 foi invalidada; Room permanece em v15 e a
baseline pós-031 passa a ser a referência obrigatória para integração de produto.

A evidência mostrou ainda que:

- `recordCount`/quantidade de samples e duração oficial são conceitos distintos;
- uma atividade íntegra pode possuir quantidade de samples diferente de
  `ceil(duration / 60 s)` sem pausa registrada;
- a normalização temporal entregue pela Spec 021 continua necessária;
- os passos por intervalo são a série útil para passos/cadência do treino;
- o total de passos do header serve como consistência/diagnóstico e não deve
  substituir a série temporal nem o total diário autoritativo;
- o campo de distância D4 preserva variação de intensidade melhor do que uma
  reconstrução linear `steps × stepLength`;
- a distância externa informada pelo usuário deve ser tratada como referência,
  não como verdade absoluta nem como integração GPS.

Esta spec corrige o modelo para novas importações sem reescrever silenciosamente
histórico, sem introduzir GPS no app e sem alterar a natureza explícita do sync
Health Connect.

## Objetivo

Entregar `d4-v4` fiel ao layout observado do APK/C001 e promover os fatos nativos
do relógio para uma derivação mais completa:

```text
D3/D4 bruto
   ↓
parser d4-v4
   ↓
startedAt / endedAt / duração oficial
steps / BPM / sportValue / watchDistance / calories / pause
   ↓
normalização temporal T/N
   ↓
distância nativa do relógio
   ↓
correção contextual opcional por distância externa de referência
   ↓
pace / velocidade / cadência / splits / projeções Health Connect
```

Quando a distância nativa não for elegível, o comprimento de passo existente
permanece disponível como fallback controlado. `CYCLING` preserva a política
vigente de distância manual e não passa a usar passos ou distância D4 por esta
spec.

## Decisões obrigatórias

### R1 — Parser `d4-v4` fiel ao Sport Model fixo

Para o Champion C001 no layout fixo observado, cada sample D4 (`packetIndex >= 4`)
deve ser interpretado como:

```text
byte  6      BPM raw
bytes 7..8   sportValue u16 little-endian
bytes 9..10  steps u16 little-endian
bytes 11..12 caloriesRaw u16 little-endian
bytes 13..14 distanceMeters u16 little-endian
byte  15     pauseState
```

O domínio não deve continuar chamando `bytes 7..9` de um único
`rawMotionValue` para `d4-v4`, nem chamar `bytes 10..12` de distância ou
`bytes 13..14` de calorias.

O range válido de BPM permanece política separada do aceite estrutural do sample;
BPM inválido não descarta passos, distância, calorias, `sportValue` ou pausa.

### R2 — Header D4 completo e sem heurística estreita

Os três primeiros pacotes D4 fornecem 42 bytes de header por concatenação de
`packet[6..19]`. O parser v4 deve interpretar os campos completos observados no
APK, incluindo:

- início com segundos;
- fim com segundos;
- total de passos `u32`;
- total de distância em metros `u32`;
- total de calorias raw `u32`;
- total de sport count `u32`;
- `recordCount u16`;
- `pauseCount u8`;
- `pauseTime u16`;
- CRC `u16`;
- `sportType u8`.

Totais do header são fatos de origem e sinais de consistência. Divergência entre
soma temporal e header deve produzir diagnóstico/validade apropriada; não deve
ser corrigida silenciosamente por sobrescrita da série.

### R3 — Índice D3 preserva posição física

O índice usado em `D4 <blockIndex>` corresponde à posição física do slot no D3.
CRC zero pode ser ignorado como treino ausente, mas nunca comprime os índices dos
slots seguintes. CRC repetido também não autoriza renumeração física.

### R4 — Duração oficial e samples permanecem independentes

A decisão temporal da Spec 021 permanece vigente:

```text
startedAt + endedAt
  = fonte de verdade da janela oficial

N samples D4
  = sequência granular ordenada

60 s
  = granularidade nominal/proveniência
  ≠ duração obrigatória de cada sample
```

Para uma série estruturalmente elegível, a duração oficial `T` continua sendo
distribuída de forma determinística e contígua entre `N` samples, garantindo:

```text
Σ effectiveDuration[i] = T
start[0] = startedAt
end[N-1] = endedAt
end[i] = start[i+1]
```

A Spec 032 não volta a impor `N × 60 s`, não cria bucket inicial/final sintético
e não infere duração por `recordCount` quando o header temporal é válido.

### R5 — Passos por intervalo continuam sendo fatos do treino

Para `d4-v4`:

```text
sample.steps = u16 LE bytes[9..10]
```

Essa série é a fonte do treino para:

- passos da atividade;
- cadência;
- créditos/guardas de passos da Spec 027;
- análises temporais locais.

O total de passos do header é usado para consistência e diagnóstico. O
`DailySummary.steps` continua autoritativo para o acumulado diário e não é
substituído pelo header D4.

### R6 — Distância nativa D4 torna-se fonte primária elegível

A auditoria `032-01` confirmou para o layout fixo observado:

```text
sample.distance = metros
header.totalDistance = metros
```

A distância por sample deve ser promovida como distância nativa do relógio quando
a série for estruturalmente válida e fechar contra o total do header.

Para tipos elegíveis de caminhada/corrida:

```text
baseIntervalDistanceMeters = watchDistanceMeters
baseWorkoutDistanceMeters  = Σ watchDistanceMeters
```

A derivação não deve reconstruir por `steps × stepLength` quando existe uma série
nativa elegível. O comprimento de passo permanece fallback explícito para casos
sem distância nativa utilizável.

A elegibilidade estrutural da série é independente da revisão do tipo. Portanto,
uma nova importação `d4-v4` íntegra pode materializar `WATCH_NATIVE` antes de
`confirmedType` existir, sem inferir que a atividade é corrida ou caminhada e sem
aplicar fator contextual. Após a revisão, `RUNNING`/`WALKING` reavaliam o mesmo
snapshot para aplicar eventual fator independente do contexto; tipos com política
própria, como `TREADMILL`, `CYCLING` e `FREE_WORKOUT`, continuam sem promover a
distância D4 quando confirmados.

`CYCLING` continua usando a política própria da Spec 022. Nenhum comportamento de
bicicleta é alterado implicitamente.

### R7 — Calibração por distância externa corrige a distância nativa

Para `d4-v4` com distância nativa positiva e tipo elegível, uma distância externa
informada manualmente pelo usuário gera:

```text
observedWatchDistanceFactor =
    referenceDistanceMeters / watchDistanceMeters
```

A atividade de referência pode ser rederivada explicitamente aplicando o fator à
série nativa:

```text
canonicalIntervalDistanceMeters =
    watchIntervalDistanceMeters × distanceCalibrationFactor
```

Isso preserva a forma temporal produzida pelo relógio em vez de impor uma passada
fixa a todos os samples.

A distância externa permanece referência manual. A spec não coleta GPS, não lê
Adidas/Strava e não declara a referência como medição absoluta sem erro.

### R8 — Aprendizado contextual da correção

`RUNNING` e `WALKING` devem possuir evidências independentes. O agregado inicial
alvo é razão ponderada por distância, não média simples de fatores:

```text
learnedDistanceFactor =
    Σ referenceDistanceMeters
    ─────────────────────────
    Σ watchDistanceMeters
```

Requisitos:

- uma evidência ativa por atividade;
- editar/substituir evidência não duplica peso;
- remover evidência recalcula somente o perfil futuro;
- novos treinos usam fator contextual quando disponível;
- ausência de fator usa `1.0` sobre distância nativa elegível;
- alterar evidência não reescreve snapshots históricos silenciosamente;
- `TREADMILL` não herda automaticamente RUNNING ou WALKING;
- `FREE_WORKOUT` não é promovido a um contexto por heurística escondida;
- `CYCLING` não participa deste aprendizado.

A política de `TREADMILL` pode ser estendida somente com evidência própria e
decisão explícita durante a execução da spec.

### R9 — Comprimento de passo permanece como fallback, não é removido

Os contratos da Spec 023 permanecem válidos para contextos e históricos que não
possuam distância D4 elegível. Para `d4-v4`, o resolver deve distinguir pelo
menos:

```text
WATCH_NATIVE
WATCH_NATIVE_CONTEXT_CALIBRATED
REFERENCE_DISTANCE_EXPLICIT
STEP_LENGTH_FALLBACK
CYCLING_MANUAL
```

Nomes finais podem mudar na implementação, mas a provenance precisa continuar
explícita e congelada no snapshot.

As calibrações antigas de comprimento de passo não devem ser convertidas
silenciosamente em fatores de distância.

### R10 — Calorias, `sportValue` e pausa

`caloriesRaw`, `sportValue` e `pauseState` devem ser preservados como fatos de
origem em `d4-v4`.

A auditoria `032-01` confirmou a escala do layout fixo observado:

```text
caloriesKilocalories = caloriesRaw / 1000.0
```

A soma de `caloriesRaw` dos samples fechou contra o total raw do header em 34/34
blocos completos. O raw continua sendo preservado como provenance, mesmo quando
a projeção em kcal for utilizada.

`sportValue` permanece semanticamente nomeado conforme o APK e fecha contra o
total do header, mas não recebe unidade ou significado fisiológico inventado.

`pauseState`, `pauseCount` e `pauseTime` permanecem raw: os 34 blocos auditados
não possuíam pausa registrada, portanto valores não zero ainda não possuem
semântica operacional fisicamente validada suficiente para alterar a timeline.

### R11 — Persistência aditiva e compatibilidade

A `main` revalidada após a Spec 031 usa Room v15. Se novos fatos precisarem ser
persistidos, preferir migration aditiva `v15 -> v16`, após a auditoria normal da
própria task `032-03`.

Não sobrecarregar colunas legacy com novo significado quando isso destruir
provenance. `rawMotionValue`, `rawDistanceValue` e `rawCaloriesValue` permanecem
identificáveis para `d4-v1/v2/v3`; `d4-v4` deve possuir campos semanticamente
claros para os novos fatos, mesmo que adapters mantenham compatibilidade com
consumidores existentes.

A migration não pode:

- reinterpretar bytes legacy sem payload suficiente;
- criar calibração;
- ativar snapshot novo;
- mudar workflow;
- criar operação Health Connect;
- executar efeito remoto.

### R12 — Versionamento e histórico

- novas importações usam `parserVersion = d4-v4` somente após parser, schema e
  derivação estarem coerentes no mesmo fluxo;
- nova derivação usa versão distinta, inicialmente `watch-d3-d4-v4`;
- `d4-v1`, `d4-v2` e `d4-v3` permanecem históricos identificáveis;
- atividade antiga só pode migrar semanticamente para v4 por reprocessamento
  explícito quando houver fatos suficientes, preferencialmente payload D4 local
  preservado;
- atividade `SYNCED` nunca é alterada ou republicada silenciosamente;
- reprocessamento não exige nova leitura BLE quando os dados locais bastarem.

### R13 — Health Connect preserva guards existentes

Health Connect continua write-only, local-first e explícito.

A nova derivação deve alimentar `DISTANCE`, `SPEED`, `STEPS`, `STEPS_CADENCE`,
BPM e calorias conforme os contratos vigentes, preservando:

- payload congelado;
- IDs estáveis;
- versões monotônicas;
- retry idempotente;
- cobertura/creditagem de passos da Spec 027;
- nenhuma sincronização automática durante importação, calibração ou migration.

Mudança de fonte da distância não autoriza duplicar passos nem recalcular
records remotos já aceitos sem fluxo explícito de reconciliação.

### R14 — Integração sobre a baseline pós-031

A Spec 031 está concluída e integrada à `main`. A branch 032 já contém essa
baseline; a reauditoria de `032-01` confirmou `behind_by = 0` e não encontrou
mudança de protocolo, persistência D3/D4 ou projeção Health Connect que invalide o
contrato v4.

A integração tardia deve trabalhar diretamente sobre os read models e a UI
pós-031, especialmente nos arquivos compartilhados:

- `data/RoomRepositories.kt`;
- `feature/exercises/ExercisesScreen.kt`;
- contratos/read models de Saúde e Calendário que consomem FC de workout.

A Saúde pós-031 inclui `WATCH_WORKOUT_ACTIVITY` no histórico de frequência
cardíaca. Portanto, preservar corretamente BPM/sourceSampleIndex/timestamp D4 é
também requisito dos gráficos e análises de Saúde, sem transformar essas
superfícies em consumidoras de bytes raw.

A 032 não deve reverter o redesign, reintroduzir navegação antiga ou exigir novo
merge específico da 031.

## Não objetivos

- GPS de produção, rota, mapa ou treino iniciado pelo celular;
- integração com Adidas Running, Strava ou API externa;
- leitura do Health Connect;
- reconstrução de informação segundo a segundo que o D4 não fornece;
- uso do total do header como substituto silencioso da série de passos;
- descobrir fórmula interna de distância do firmware;
- transformar `sportValue` em métrica fisiológica sem evidência;
- reescrever histórico legacy automaticamente;
- apagar calibrações antigas de comprimento de passo;
- mudar comportamento de `CYCLING`;
- testes automatizados novos sem pedido explícito do usuário.

## Critérios de aceite

1. D3 preserva o índice físico de slots mesmo com CRC zero/repetido.
2. `d4-v4` interpreta header e samples conforme o layout fixo observado, sem
   packing u24 incorreto.
3. BPM inválido não descarta outros fatos válidos do sample.
4. Passos por intervalo usam `u16 bytes[9..10]`; header é consistência, não fonte
   substituta da série.
5. Distância nativa D4 elegível é persistida separadamente e alimenta a distância
   canônica antes do fallback por comprimento de passo.
6. A normalização temporal `T/N` da Spec 021 permanece e fecha exatamente em
   `startedAt -> endedAt`.
7. Calibração externa pode corrigir proporcionalmente a série nativa e possui
   provenance explícita.
8. Perfis de correção RUNNING/WALKING são independentes e derivados de evidências
   sem média simples escondida.
9. Legado `d4-v1/v2/v3` permanece reproduzível e não é reinterpretado em
   migration.
10. `CYCLING`, Daily Summary e contabilidade de passos da Spec 027 permanecem
    semanticamente intactos.
11. Health Connect continua explícito e idempotente, sem efeito remoto em
    migration/reprocessamento/calibração.
12. Integração final preserva a baseline pós-031, inclusive os consumidores de FC
    de workout em Saúde, sem expor packing raw à UI.
13. Validação física reproduz pelo menos uma atividade com variação de intensidade
    e compara header, série temporal e uma referência externa manual sem exigir
    igualdade perfeita entre relógio e referência.

## Escopo de arquivos provável

### Núcleo/protocolo

- `core/bluetooth/ChampionD3WorkoutIndexProtocol.kt`
- `core/bluetooth/ChampionD4WorkoutParser.kt`
- `core/model/WatchWorkoutActivity.kt`
- `core/model/WatchWorkoutDerivedMetrics.kt`
- `docs/protocol/workouts-d3-d4.md`

### Persistência

- `core/database/ChampionDatabase.kt`
- `core/database/ChampionMigrations.kt`
- schema Room correspondente;
- `data/DatabaseMappers.kt`;
- `data/RoomRepositories.kt` somente na fase de integração quando possível.

### Produto/projeção

- `feature/exercises/ExercisesFeatureContract.kt`;
- `feature/exercises/ExercisesViewModel.kt`;
- `feature/exercises/ExercisesScreen.kt` sobre a baseline pós-031;
- read models de Saúde/Calendário afetados por FC de workout, se necessário;
- projeções Health Connect relacionadas a treino/passos/cadência/distância.

## Validação

Conforme `AGENTS.md`, mudanças de código/Gradle devem passar por:

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados como etapa padrão sem pedido
explícito. Validação física e inspeção de banco devem usar evidência sanitizada,
sem versionar payload BLE integral, identificadores pessoais ou dados de saúde
não necessários.

## Fechamento — 2026-09-11

A validação física final do Champion C001 foi concluída com conferência por
dumps sanitizados do banco. Os cenários mínimos de D3/D4 v4, variação de
intensidade, coerência header/série, timeline `T/N`, distância nativa,
calibração externa, perfis RUNNING/WALKING, fallback, review-before-sync,
Health Connect, Daily Summary, cadência e `CYCLING` foram considerados válidos.

Evidência detalhada:
`docs/integration/032-08-physical-validation.md`.

Permanecem abertas limitações de firmware e semântica não promovida:
`pauseStateRaw` não zero, unidade funcional de `sportValue`, campos reservados
ou desconhecidos, política de reprocessamento de históricos, tolerâncias para
divergências futuras, cobertura de samples acima de 255 passos e retenção de
payload diagnóstico. A referência externa manual também permanece sujeita a
erro e serve somente como referência de calibração. A Spec 032 está encerrada
sem alterar essas fronteiras.

## Evolução de política pós-032 — 2026-09-14

A validação física da entrega 033 mostrou um `FREE_WORKOUT` D4 v4 íntegro com distância nativa positiva. Para novas operações, a restrição original de R6/R8 é supersedida: `FREE_WORKOUT` preserva `WATCH_NATIVE`; `TREADMILL` também usa a distância D4 nativa e continua exigindo a distância final da esteira para calibração. A calibração de esteira usa `referência / distância D4`, escala proporcionalmente cada intervalo nativo e aprende um fator contextual próprio de `TREADMILL`, sem compartilhar evidência com `RUNNING` ou `WALKING`. `CYCLING` permanece inalterado. Atividades já sincronizadas e fatos raw não são reescritos.
`TREADMILL` com série nativa íntegra permanece `PENDING_CALIBRATION` até que a
distância externa seja informada. O fator nativo não pode cair no caminho de
calibração por comprimento de passo: step length continua restrito a
`RUNNING`/`WALKING`. O mapper de snapshots aceita `FREE_WORKOUT` e `TREADMILL`
depois da confirmação do tipo, e a revisão reaplica fator contextual de
`TREADMILL` quando já houver evidência ativa. A mudança de política não altera
`TOTAL_CALORIES_BURNED`, `CYCLING`, dados raw ou atividades `SYNCED`.
