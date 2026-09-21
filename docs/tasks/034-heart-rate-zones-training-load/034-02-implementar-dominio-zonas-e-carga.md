# 034-02 — Implementar domínio de zonas e carga

- **Ticker:** `034`
- **Número:** `02`
- **Status:** `completed`

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

- **Status:** `completed` — policy pura implementada após `034-01`.
- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutHeartRateAnalysis.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`.
- **Decisões e desvios:** `WorkoutHeartRateAnalysisPolicy` ficou em `core.model`,
  com resultado tipado para `AVAILABLE`, `CONFIGURATION_REQUIRED`,
  `NO_HEART_RATE_SAMPLES` e `UNAVAILABLE`. O helper sample-and-hold foi
  compartilhado com `weightedBpm`; a policy filtra amostras para `[S,E)` antes
  de gerar janelas, enquanto splits preservam suporte a amostras anteriores à
  sessão conforme comportamento existente. Nenhum desvio de requisito.
- **Comandos executados:** inspeção de Spec 034, ADR-034, ADR-018, overview,
  `034-01`, `AGENTS.md` e contratos de domínio; `./gradlew app:compileDebugKotlin`;
  `./gradlew lint assembleDebug assembleRelease`; `./gradlew clean` após falta
  de espaço temporária do filesystem de dados; `git diff --check`.
- **Resultados e evidências:** compilação, lint e builds debug/release passaram.
  Buckets usam razão `BPM / FC máxima`, cobertura usa sample-and-hold sem timeout,
  `BELOW_ZONE_1` fecha cobertura com peso zero, percentuais usam cobertura como
  denominador e carga permanece `Double`. Revisão independente não encontrou
  bloqueadores nem defeitos funcionais. Nenhum teste automatizado novo foi criado.
- **Riscos residuais:** validação numérica independente e validação física
  permanecem previstas para `034-06`; mudança do helper deve continuar sendo
  comparada contra splits em revisões futuras. Arquivo novo ainda precisa ser
  incluído no controle de versão pelo fluxo de entrega; não houve commit nesta
  task.
