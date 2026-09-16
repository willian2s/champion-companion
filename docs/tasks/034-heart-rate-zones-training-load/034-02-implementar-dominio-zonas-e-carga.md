# 034-02 — Implementar domínio de zonas e carga

- **Ticker:** `034`
- **Número:** `02`
- **Status:** `pending`

## Objetivo

Criar policy pura e determinística para converter samples temporais persistidos
em cobertura, buckets de zona e carga cardiovascular, sem persistir o resultado e
sem depender de UI ou infraestrutura.

## Resultado esperado

Domínio capaz de produzir `WorkoutHeartRateAnalysis` para samples e FC máxima
válidos, retornar estados seguros quando não houver configuração/samples e manter
os invariantes temporais e matemáticos da Spec 034.

## Requisitos cobertos

- R2, R3, R4 e R5 da Spec 034;
- ADR-034, seções de política temporal, buckets e carga;
- critérios de aceite 5 a 13.

## Escopo incluído

- `HeartRateZone` com `BELOW_ZONE_1` e `Z1` a `Z5`;
- `HeartRateZoneSummary` com duração, percentual e peso;
- `WorkoutHeartRateAnalysis` e estado/result wrapper de disponibilidade;
- `WorkoutHeartRateAnalysisPolicy` em `core.model`;
- reuso/extração segura do helper sample-and-hold de `weightedBpm`;
- classificação por razão BPM/FC máxima;
- cobertura parcial, flag acima da máxima e Edwards TRIMP em `Double`;
- ausência tipada, sem zero sintético.

## Escopo excluído

- Room, migration, Perfil e repository;
- ExercisesViewModel/Screen;
- parser, BLE, Health Connect e workflow;
- persistência de zonas, carga ou snapshot analítico;
- baseline pessoal ou classificação de intensidade.

## Dependências

- `034-01` concluída;
- `WorkoutHeartRateSample` e janela da ADR-018;
- contratos de `Duration` e helpers matemáticos existentes no domínio.

## Arquivos e símbolos prováveis

- `core/model/WatchWorkoutHeartRateAnalysis.kt` ou nome equivalente;
- `core/model/WatchWorkoutDerivedMetrics.kt` — `weightedBpm`,
  `WorkoutIntervalWindow` e helpers temporais;
- `core/model/WatchWorkoutActivity.kt` — limites de BPM e timestamps;
- `core/model/RepositoryContracts.kt` somente se o tipo de read model exigir
  contrato compartilhado.

## Passos de implementação

1. Definir tipos e estados sem misturar disponibilidade analítica com workflow.
2. Aceitar apenas FC máxima `30..220` quando presente.
3. Resolver sessão canônica e filtrar samples para `[S, E)`.
4. Gerar janelas sample-and-hold semiabertas, limitadas à sessão.
5. Somar `coveredDuration` e calcular `coverageRatio`.
6. Classificar cada janela pela razão direta, preservando BPM observado.
7. Somar duração por bucket e calcular percentuais pela cobertura.
8. Calcular carga ponderada por minutos e pesos 0..5.
9. Retornar configuração necessária ou ausência de samples sem inventar análise.
10. Reusar o helper existente sem alterar os resultados de splits.

## Testes e comandos de validação

- revisão manual contra cálculo independente em fronteiras e amostras acima da
  máxima;
- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease` somente no gate final, não criar
  testes automatizados nesta task.

## Definição de pronto

- policy não acessa Room, Health Connect, parser ou UI;
- classificação usa razão, não limite BPM arredondado;
- soma de buckets fecha cobertura;
- carga permanece `Double` até apresentação;
- no-samples, no-config e partial-coverage têm semântica distinta;
- nenhuma tabela/coluna de análise foi criada.

## Riscos e cuidados

- Não adicionar stale timeout para corrigir risco percebido do sample-and-hold.
- Não usar `lastSample - firstSample` como cobertura.
- Não transformar `BELOW_ZONE_1` em carga negativa ou excluir da cobertura.
- Não arredondar duração intermediária.
- Não emitir zero como substituto de ausência.

## Registro da execução

- **Status:** `pending` — dependência de auditoria concluída; implementação não
  iniciada.
- **Arquivos alterados:** nenhum arquivo de produto nesta fase de planejamento.
- **Decisões e desvios:** policy ficará em `core.model`; sample-and-hold será
  compartilhado com `weightedBpm`, mas somente com janelas contidas na sessão.
- **Comandos executados:** inspeção de `WorkoutHeartRateSample`,
  `weightedBpm` e ADR-018; gates de build não executados por escopo documental.
- **Resultados e evidências:** buckets, pesos, cobertura e fórmula Edwards
  congelados na Spec/ADR.
- **Riscos residuais:** mudanças no helper podem afetar splits; comparar
  resultados existentes antes/depois na implementação.
