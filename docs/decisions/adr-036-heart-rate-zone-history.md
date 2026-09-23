# ADR-036 — Histórico de zonas de frequência cardíaca e distribuição de intensidade

- **Status:** aceita e validada na Spec 036
- **Data:** 2026-09-22
- **Relacionada:** [Spec 036](../specs/036-heart-rate-zone-history.md)
- **Base:** Spec 034 e Spec 035 concluídas, correção 035-08, Room v20

## Contexto

A análise individual da Spec 034 já é a autoridade para zonas, sample-and-hold,
cobertura e carga cardiovascular. A Spec 035 já agrega essa análise em janelas
bounded, mas ainda não expõe a distribuição do tempo por zona. A nova projeção
precisa preservar os contratos existentes e evitar a regressão Release causada
quando o histórico dependia de snapshots, splits e derivação.

O design system atual renderiza barras agrupadas por meio de Vico. Não existe
contrato de empilhamento, cores por seis séries ou semantics específica para
segmentos empilhados.

## Decisão

### 1. Estender read model existente

Estender `CardiovascularLoadHistoryPolicy`,
`CardiovascularLoadPeriodSummary` e `CardiovascularLoadDailyBucket`. Não criar
policy, repository, query ou read model paralelo.

Os novos campos reutilizarão `HeartRateZoneSummary` da Spec 034. Períodos e dias
com análise `AVAILABLE` terão exatamente seis entradas: `BELOW_ZONE_1`, `Z1`,
`Z2`, `Z3`, `Z4` e `Z5`, inclusive durações zero. Sem análise disponível, a
coleção permanecerá ausente de forma tipada.

### 2. Uma análise por workout

`WorkoutHeartRateAnalysisPolicy` será chamada uma vez por workout elegível. O
mesmo resultado será usado para current7/current28, buckets e demais agregações.
Nenhuma camada histórica classificará BPM ou recalculará sample-and-hold.

### 3. Uma semântica temporal

`HistoryWindows.from(asOf)` continua única fonte das janelas de 7, 28 e 56 dias.
O bounded read permanece `[asOf - 56 dias, asOf)`, com pertencimento por
`startedAt`. `ZoneId` somente converte início para `LocalDate` nos buckets.
Nenhum workout será dividido por duração ou por mudança de dia.

### 4. Denominadores independentes

Para cada zona:

```text
zoneDuration = soma da duração da zona em análises AVAILABLE
zonePercentage = zoneDuration / soma de todas as zoneDuration AVAILABLE
```

`coverageRatio` mantém outro denominador:

```text
coveredHeartRateDuration / totalWorkoutDuration
```

Assim, percentual de distribuição mede composição do tempo analisado e
`coverageRatio` mede cobertura do treino total.

### 5. Estados e parcialidade

Mantêm-se os estados da 035 e sua precedência. Um resultado `AVAILABLE` misto
com workout sem análise permanece `AVAILABLE`, contando apenas análises
disponíveis e marcando `partial`. Carga zero disponível continua análise válida.
Não haverá threshold mínimo de cobertura.

### 6. Visualização temporal sem stacking novo

Não alterar `ChampionBarChart`, `ChampionCartesianChartData` ou Vico para criar
stacked bars nesta spec. A página contextual existente exibirá uma distribuição
diária de uma zona selecionada por vez, em uma única série neutra do
`ChampionBarChart`. Label e semantics identificam a zona; as rows agregadas
reutilizam exatamente as cores da Spec 034. A seleção é de apresentação, não
altera leitura nem agregação. O gráfico diário de carga da 035 permanece.

Rows de 7/28 dias exibem as seis zonas ao mesmo tempo. Labels, duração,
percentual, contadores e descriptions acessíveis não dependem apenas de cor.
As cores da Spec 034 serão reutilizadas nas rows, não duplicadas no tema de
gráficos.

Os buckets civis seguem a 035: normalmente haverá 28 datas, mas uma janela
`current28` não alinhada a início de dia ou atravessada por DST pode tocar 29
datas locais. A implementação deve preservar esses buckets e seus fechamentos,
sem cortar a lista para forçar quantidade fixa.

### 7. Persistência e efeitos remotos

Nenhum resultado analítico será persistido. Room permanece v20, sem migration,
tabela, coluna, snapshot, cache ou backfill. Health Connect permanece sem
mudança de permissões, records, payloads, IDs, versões, fila, retry,
reconciliation ou readback.

## Consequências

### Positivas

- uma única fonte de verdade para zonas e cobertura;
- nenhum risco de divergência entre detalhe individual e histórico;
- correção 035-08 permanece protegida por read model mínimo;
- alteração de FC máxima recompõe histórico sem reescrever fatos;
- gráfico usa infraestrutura existente e evita falsa representação de stacking;
- fechamento por zona e por bucket torna validação independente objetiva.

### Negativas e riscos aceitos

- a seleção de uma zona por vez é menos imediata que stacked bars;
- distribuição histórica muda quando FC máxima muda;
- sessões que atravessam meia-noite continuam atribuídas ao dia de início;
- custo de query sem índice temporal composto permanece risco da 035;
- ausência de testes automatizados novos mantém dependência de inspeção, build e
  validação física.

## Alternativas rejeitadas

- policy paralela de zonas;
- nova query ou `CardiovascularLoadWorkoutInput` paralelo;
- reidratação completa de `WorkoutActivity`;
- classificação independente de zonas;
- percentuais com `totalWorkoutDuration`;
- omitir `BELOW_ZONE_1` ou zonas de duração zero;
- simular stacked bars com seis séries grouped;
- criar renderer genérico de stacking somente para esta spec;
- persistir histórico ou enviar distribuição ao Health Connect;
- comparação de zona com período anterior na primeira versão.

## Limites

Esta decisão não altera `WorkoutHeartRateAnalysisPolicy`, thresholds, BPM,
sample-and-hold, parser, BLE, D1/D3/D4, derivação, snapshots, workflow,
Health Connect, treino phone, GPS, rota, permissões ou Room. Não cria avaliação
de intensidade como boa/ruim, meta, risco, fadiga, recuperação ou recomendação.

## Validação e fechamento

Validação física concluída em 2026-09-23 com workouts reais no `SM-S901E`.
Room permaneceu v20, a distribuição fechou por período e bucket, a seleção
temporal usou uma série suportada, e a recomposição por FC máxima foi observada
com restauração do valor original. O caminho cardiovascular não criou operação
Health Connect nem persistiu analytics. Carga zero e workout sem análise ficaram
N/A por ausência natural; nenhum dado foi fabricado. Ver
[`036-06-physical-validation.md`](../integration/036-06-physical-validation.md).
