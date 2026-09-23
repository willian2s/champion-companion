# 037-02 — Implementar métricas de performance de corrida

- **Ticker:** `037`
- **Número:** `02`
- **Status inicial:** `pending`

## Objetivo e resultado esperado

Implementar domínio puro para pace canônico e frequência cardíaca média
ponderada, preservando invariantes e a semântica temporal da Spec 034.

Resultado: policy e contratos determinísticos, sem Room, repository, ViewModel,
Compose, Health Connect ou persistência.

## Requisitos cobertos

- fórmula de pace por distância canônica e duração coberta;
- elegibilidade de scalar, temporalidade e provenance;
- statuses `RunningPerformance*` congelados;
- média FC ponderada por sample-and-hold;
- coverage parcial, ausência sem zero e ponto parcial;
- nenhuma fórmula composta, score, slope ou interpretação.

## Escopo incluído

- contratos de `RunningPerformanceHistory`, ponto, estados e provenance;
- policy pura sobre inputs já mapeados;
- extração de `WorkoutHeartRateTimelinePolicy` compartilhada com 034;
- formatter de domínio somente se necessário para invariantes, mantendo
  apresentação `m:ss /km` fora do cálculo;
- preservação behavior-preserving da análise de zonas/carga.

## Escopo excluído

- DAO, SQL, Room, migration ou schema;
- composição de Flow ou repository;
- navegação, ViewModel e Compose;
- alteração de thresholds, zonas ou carga 034;
- testes automatizados novos, salvo autorização posterior explícita.

## Dependências

- `037-01` concluída;
- `WatchWorkoutDerivedMetrics.kt`;
- `WatchWorkoutHeartRateAnalysis.kt`;
- `WorkoutHeartRateLoadHistory.kt`;
- ADR-018 e ADR-034.

## Arquivos e símbolos prováveis

- `core/model/RunningPerformanceHistory.kt`;
- `core/model/WatchWorkoutHeartRateAnalysis.kt`;
- `core/model/WatchWorkoutDerivedMetrics.kt` somente se helper temporal exigir
  extração behavior-preserving;
- `WorkoutIntervalWindow`, `WorkoutHeartRateSample`,
  `WorkoutDerivationMethod`, `TemporalSeriesEligibility`.

## Passos de implementação

1. Criar input puro com escalares, provenance, temporalidade, plausibilidade e
   samples.
2. Validar distância/duração, derivation method/version, factor e indicadores.
3. Calcular pace somente após validação e sem arredondamento.
4. Extrair timeline compartilhada preservando filtro da 034 e sample-and-hold.
5. Calcular média BPM por duração coberta e coverage nullable sem samples.
6. Produzir statuses por ponto e histórico: ausência natural,
   `INVALID_SAMPLES` fail-closed e `SOURCE_UNAVAILABLE` para falha batch.
7. Produzir statuses por ponto e histórico, sem derrubar linhas independentes
   por ausência ou amostra inválida.
8. Verificar que zonas/carga continuam com mesmas regras e thresholds.

## Testes e comandos de validação

Não criar testes automatizados nesta task. Fazer revisão estática e executar:

```bash
git diff --check
```

Implementação futura completa usará o gate Gradle apenas quando código estiver
autorizado; esta subtarefa não deve alterar Room ou Health Connect.

## Definição de pronto

- pace fecha pela fórmula dos dois escalares;
- fallback e versão não suportada não entram como comparáveis;
- média FC usa timeline compartilhada e coverage separado;
- ausência não vira zero;
- domínio não acessa infraestrutura;
- diff não altera parser, Health Connect, schema ou testes.

## Riscos e cuidados

- `sampleAndHoldWindows` atual tem consumidores com semântica própria; extrair
  sem mudar comportamento dos splits;
- não exigir FC máxima para média;
- não usar `WorkoutActivity.metrics.averageBpm`;
- não usar `isProjectionEligible` como atalho que confunda projeção remota com
  análise local;
- preservar unidades internas e arredondar somente na apresentação.
