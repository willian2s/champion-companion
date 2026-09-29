# ADR-037 — Tendência de corrida: relação entre ritmo e frequência cardíaca

- **Status:** accepted
- **Data:** 2026-09-23
- **Relacionada:** [Spec 037](../specs/037-running-pace-heart-rate-trend.md)
- **Base:** Specs 032, 034, 035 e 036 concluídas; correção 035-08; Room v20

## Contexto

A Spec 037 precisa descrever relação factual entre ritmo e frequência cardíaca
em corridas, sem inferir eficiência, condicionamento, causalidade ou melhora.
O risco central é misturar snapshots com proveniências e versões diferentes ou
reintroduzir a regressão da 035-08 ao reidratar derivação histórica completa.

O design system possui um único eixo Y e não possui scatter. A média BPM
persistida em `WorkoutActivity.metrics.averageBpm` não preserva duração temporal.

## Decisão

### População e janela

Usar somente:

```text
origin = WATCH_D3_D4
confirmedType = RUNNING
workflowState != DISCARDED
startedAt ∈ [asOf - 56 dias, asOf)
```

`suggestedType` não substitui confirmação. `TREADMILL`, `WALKING`,
`FREE_WORKOUT`, `CYCLING`, `UNKNOWN` e origens phone ficam fora. A janela é
rolling única de 56 dias; não há current28/previous28, delta ou score.

### Pace escalar

Reconstruir pace somente de `canonicalDistanceMeters` e
`canonicalCoveredDurationNanos` do snapshot ativo:

```text
pace s/km =
    (canonicalCoveredDurationNanos / 1e9)
    / (canonicalDistanceMeters / 1000)
```

A query dedicada usa `LEFT JOIN` por `activeActivityId` e `isActive = 1` e
seleciona apenas escalares, provenance, versão, elegibilidade temporal e
plausibilidade. Não carrega `CanonicalWorkoutSeries`,
`DerivedWorkoutInterval`, `WorkoutSplit`, header, import ou `WorkoutActivity`
completo.

Na persistência, os escalares chegam de `WorkoutDerivationSnapshotEntity`:
`canonicalDistanceMeters`, `canonicalCoveredDurationNanos`,
`temporalEligibility` e `temporalEligibilityReasons`. O domínio atual expõe a
mesma informação por `canonicalDistanceMeters`,
`canonicalSeries?.coveredDuration` e `temporalResolution`; a projection futura
deve fazer esse mapeamento explicitamente, sem depender de uma propriedade de
domínio inexistente nem reidratar o snapshot completo.

### Comparabilidade

`WATCH_NATIVE` e `WATCH_NATIVE_CONTEXT_CALIBRATED` compartilham trend principal
quando escalares são válidos, temporalidade é elegível, versão é
`watch-d3-d4-v4` e não há indicador de plausibilidade. A UI diferencia:

- `Distância do relógio`;
- `Distância do relógio calibrada`.

`STEP_LENGTH_FALLBACK` e `FIXED_PERSONAL_STEP_LENGTH` podem aparecer na lista
como `Distância estimada por comprimento de passo`, mas não entram na série
comparável. Não haverá backfill ou uniformização de provenance.

Snapshot nativo com versão diferente de `watch-d3-d4-v4` pode aparecer na lista
com pace factual e label `Distância do relógio — versão não comparável`, usando
estado `AVAILABLE_UNSUPPORTED_VERSION_NOT_COMPARABLE`, mas fica fora dos
gráficos. Proveniência desconhecida ou escalares inválidos não produzem valor.

Indicadores de plausibilidade bloqueiam comparabilidade. A ausência de aceite
explícito não bloqueia esta análise local: aceite protege projeção funcional ou
remota, e a 037 não escreve dados nem Health Connect.

### FC média

Extrair primitiva pura compartilhada com a Spec 034 para filtro temporal,
ordenação, validação e sample-and-hold. A média será ponderada por duração:

```text
Σ(bpm × duração sample-and-hold) / coveredHeartRateDuration
```

034 continua autoridade de zonas e carga. 037 não usa média aritmética
importada, FC máxima, zonas ou thresholds novos.

### Read model e falhas

Criar `RunningPerformanceWorkoutInput` e
`RunningPerformanceHistoryPolicy`, com método bounded dedicado no
`WorkoutActivityRepository`. Atividades/snapshot e samples serão lidos em batch,
agrupados em transação e sem N+1.

Falha estrutural de uma linha recebe estado tipado. Falha estrutural da fonte
produz `UNAVAILABLE`. Ausência ou invalidez de FC preserva pace da corrida e
não torna o histórico inteiro indisponível. A matriz é: sem samples →
`NO_HEART_RATE_SAMPLES`; samples inválidos → `INVALID_SAMPLES`; samples válidos
parciais ou completos → `AVAILABLE`; falha batch da fonte → `UNAVAILABLE` para o
histórico, com `SOURCE_UNAVAILABLE` tipado na linha quando pace já tiver sido
composto. Qualquer sample inválido na janela, inclusive mistura com samples
válidos, recebe `INVALID_SAMPLES`; samples fora da janela são ignorados. Essa
precedência segue o fail-closed da 034. `activityId` fica somente no record
interno para agrupamento/desempate.

### Ordem e gráficos

Pontos são ordenados por `startedAt`, com desempate interno estável. Corridas no
mesmo dia não são agregadas. Empates exatos usam posição ordinal crescente no
gráfico, mantendo data/hora real no label; nenhum epsilon é fabricado.

`asOf` e `ZoneId` são parâmetros explícitos e estáveis. `ZoneId` serve para
labels civis de data/hora; não cria buckets nem altera pertencimento temporal.

Usar dois gráficos temporais alinhados:

- ritmo em eixo Y `s/km`, apresentado como `m:ss /km`;
- FC média em eixo Y `bpm`.

Não usar dual-axis, normalização artificial ou scatter simulado. Scatter exige
decisão posterior de design system.

### Persistência e Health Connect

Room permanece v20, sem migration, tabela, coluna, cache, snapshot analítico ou
backfill. Health Connect permanece inalterado e fora do read model.

## Auditoria 037-01

O código atual usa `WorkoutActivity` como contrato de atividade, mantém
`confirmedType` separado de `suggestedType` e ainda não filtra tipo confirmado
nas consultas watch genéricas. O contrato da 037, portanto, exige query dedicada
com `confirmedType = RUNNING`, além de origem, descarte e janela semiaberta.
`CardiovascularLoadWorkoutInput` e seu caminho bounded permanecem mínimos e não
serão ampliados. Exercícios e navegação ainda expõem somente o histórico de carga
cardiovascular; tendência terá estado, destino e tela próprios dentro da subtree.

`WorkoutHeartRateAnalysisPolicy` é a autoridade existente para filtro temporal,
validação e sample-and-hold. A média da 037 reutilizará essa timeline, com
denominador igual à duração coberta e sem FC máxima, threshold ou política
paralela. `ChampionChartSeries` exige X crescente, então empate de instante será
representado por ordinal determinístico.

Não existe banco físico local sanitizado no checkout auditado. A composição real
de corridas confirmadas `RUNNING`, métodos e versões fica pendente para 037-06 e
deve ser registrada como `N/A` se não surgir naturalmente. Nenhuma alteração de
código, Room, schema, Health Connect ou teste foi feita nesta auditoria.

## Consequências

Positivas:

- evita mistura silenciosa de distância nativa e fallback;
- preserva correção 035-08;
- mantém semântica temporal de FC da 034;
- mantém corrida sem FC visível com pace;
- preserva unidades honestas nos gráficos;
- rollback não exige alteração de dados.

Custos aceitos:

- snapshots antigos e fallback podem ficar fora do trend comparável;
- rederivação explícita pode alterar saída efêmera;
- eixo ordinal não representa distância proporcional entre eventos;
- dois gráficos ocupam mais espaço;
- query temporal sem índice composto continua risco separado.

## Alternativas rejeitadas

- ampliar `CardiovascularLoadWorkoutInput`;
- usar `CalendarWorkoutSummary` como fonte final;
- reidratar snapshot, splits ou intervalos;
- usar pace ou BPM agregado sem semântica temporal;
- misturar fallback ao trend;
- exigir FC máxima para média BPM;
- duplicar sample-and-hold;
- exigir aceite de plausibilidade para análise local;
- current28/previous28 sem requisito;
- agregar por dia;
- epsilon em timestamps duplicados;
- dual-axis, scatter falso ou alteração ampla do renderer;
- persistir resultado analítico.

## Limites

Esta decisão não altera parser, BLE, D1/D3/D4, derivação, calibração,
snapshots, workflow, treino phone, GPS, rota, permissões, Room, Health Connect
ou política de zonas/carga da 034. Não define eficiência, condicionamento,
recuperação, fadiga, risco, recomendação ou causalidade.

## Validação requerida

A implementação futura deve usar banco real existente e cálculo independente
para população, pace, provenance, média FC, coverage, ausência/parcialidade,
ordem, múltiplas corridas, Room v20 e ausência de mutações em snapshots,
derivação, splits, calibrações e Health Connect. Reabertura deve confirmar
ausência de persistência analítica. Cenários inexistentes naturalmente recebem
`N/A`; nenhum dado deve ser fabricado. O smoke deve ocorrer em Release com
instalação incremental e sem wipe, reimportação ou rederivação, devido à
regressão histórica corrigida pela 035-08.

## Validação e aceite — 2026-09-29

A decisão foi validada sobre banco físico existente e instalação incremental do
artefato Release. Room permaneceu v20 e não houve mutação em activities, samples,
snapshots, derived intervals, splits, calibrações ou fila Health Connect durante
a abertura, navegação, reabertura e recomposição.

A população real teve 21 corridas confirmadas. Cinco pontos com lineage nativa
`watch-d3-d4-v4` foram comparáveis e apareceram nos dois gráficos; 16 pontos de
fallback apareceram somente na lista, conforme a decisão de não misturar
proveniências. A ausência de pontos adicionais à direita do gráfico não é corte
visual: os fallbacks ficam deliberadamente fora da série comparável.

Pace independente, provenance e média FC sample-and-hold fecharam. Ausência de
FC, FC completa, amostras inválidas, snapshot ausente, empate temporal e
múltiplas corridas no mesmo dia ficaram `N/A` por não existirem naturalmente.
Evidência completa e screenshot sanitizada estão registradas na integração da
037-06.
