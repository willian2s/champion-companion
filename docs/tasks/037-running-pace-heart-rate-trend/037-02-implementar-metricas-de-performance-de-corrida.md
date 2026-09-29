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

## Status atual

`completed`

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RunningPerformanceHistory.kt`
  — contratos, statuses, provenance, validação de pace, média ponderada de FC e
  política pura de histórico.
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutHeartRateAnalysis.kt`
  — extração behavior-preserving da timeline validada e do helper de média
  ponderada compartilhável.
- este arquivo — status, decisões, comandos, evidências e riscos.
- `037-00-overview.md` — checklist e progresso.

## Decisões e desvios

- Decisão: `RunningPerformanceWorkoutInput` recebe somente fatos mínimos,
  incluindo escalares canônicos, provenance, temporalidade, plausibilidade e
  samples; nenhum contrato de infraestrutura foi ampliado.
- Decisão: pace usa exclusivamente distância canônica e nanos de duração coberta;
  fallback e versão nativa não suportada podem manter valor factual, mas recebem
  status não comparável.
- Decisão: indicadores de plausibilidade bloqueiam comparabilidade sem reutilizar
  `isProjectionEligible`.
- Decisão: falha batch de samples é representada por
  `heartRateSourceAvailable = false`, preservando o pace já composto e elevando o
  status do histórico para `UNAVAILABLE`.
- Desvio: nenhum teste automatizado novo foi criado, conforme escopo da
  subtarefa; o gate de compilação, lint e builds foi executado por se tratar de
  alteração de código.

## Comandos executados

- `git diff --check`
- `./gradlew lint assembleDebug assembleRelease`
- revisão estática de Spec 037, ADR-018, ADR-034, ADR-037, overview, 037-01,
  `AGENTS.md`, baseline e consumidores da timeline.
- Primeira execução de `./gradlew lint assembleDebug assembleRelease` falhou na
  compilação por constantes de validação removidas durante a extração; constantes
  foram recolocadas em escopo compartilhado e a execução seguinte passou.

## Resultados e evidências

- `./gradlew lint assembleDebug assembleRelease` — passou; debug e release
  compilados, empacotados e lint concluído.
- `git diff --check` — passou.
- Timeline 034 preservada: filtro `[startedAt, startedAt + duration)`, BPM `30..220`,
  timestamps estritamente crescentes e sample-and-hold continuam centralizados.
- Média de FC usa duração coberta; cobertura permanece nullable na ausência de
  samples e parcial sem threshold arbitrário.
- Nenhum arquivo Room, schema, migration, parser, Health Connect, repository,
  ViewModel, Compose ou teste foi alterado.

## Riscos residuais

- A composição bounded de atividades, snapshot ativo e samples em batch ainda
  pertence à `037-03`.
- A policy depende de o mapper futuro representar falhas de fonte como
  `heartRateSourceAvailable = false`; ausência natural deve continuar usando
  `NO_HEART_RATE_SAMPLES`.
- A validação física de proveniências, cobertura e cenários naturais permanece na
  `037-06`.

## Revisão independente

- Agente `review` aprovou após correção de precedência de validação: método/fator
  inválidos agora falham fechados antes de plausibilidade, e escalares inválidos
  não chegam ao ponto como valores factuais.
- Riscos mantidos: composição bounded e mapeamento batch em `037-03`; validação
  física em `037-06`; ausência de testes automatizados por escopo explícito.
