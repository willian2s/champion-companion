# 027-15 — Remover Gate B e timeline local 1 Hz

- **Ticker:** `027`
- **Número:** `15`
- **Status:** `completed`
- **Depends on:** `027-12`; decisão explícita de produto registrada nesta revisão

## Objetivo

Remover código, estado e UI da timeline local 1 Hz. Spec 027 passa a manter
somente Gate A: passos, cadência D4 e BPM raw sincronizados explicitamente.

## Escopo incluído

- remover `core/model/Workout1HzTimeline.kt`;
- remover modelos, estado, jobs e wiring 1 Hz de `ExercisesFeatureContract.kt`,
  `ExercisesViewModel.kt` e `ExercisesScreen.kt`;
- preservar gráfico BPM raw e métricas históricas;
- preservar `HeartRateSeries`, `HeartRateRecord`, `StepsCadenceSeries`,
  `StepsCadenceRecord`, Daily, crédito, retry e Health Connect;
- remover efeitos sem finalidade das correções específicas de `027-12`, sem
  reintroduzir validação antiga quebrada;
- registrar remoção em documentação sem apagar histórico das tasks 027-07 a
  027-14, ADR-026 ou evidências.

## Fora de escopo

- suavização BPM;
- alteração de BPM raw ou cadência D4;
- Health Connect, Health Sync, Strava, Daily, créditos ou schema;
- nova sincronização física;
- testes automatizados sem pedido explícito.

## Critérios de aceite

- nenhum uso ativo de `Workout1HzTimeline`, `deriveBpm1Hz` ou
  `deriveCadence1Hz` permanece;
- detalhe de atividade continua exibindo BPM raw quando disponível;
- `HeartRateSeries` e `StepsCadenceSeries` continuam elegíveis;
- `StepsCadenceRecord` continua usando intervalos D4;
- Gate A não sofre alteração de identidade, payload, retry ou crédito;
- lint e builds passam.

## Verificação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Auditar referências Kotlin após remoção. Não criar nem executar testes
automatizados sem pedido explícito.

## Definição de pronto

Código e UI removidos, superfícies Gate A preservadas, documentação atualizada,
subtarefa registrada e revisão independente solicitada. Não executar `027-16`
automaticamente.

## Resultado desta execução

- **Status:** `completed`; Gate B/timeline local 1 Hz removido sem alterar o
  fluxo vigente de Gate A.
- **Arquivos alterados:**
  - removido `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/Workout1HzTimeline.kt`;
  - atualizado `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`;
  - atualizado `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`;
  - atualizado `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`;
  - este arquivo.
- **Decisões:** o detalhe continua exibindo somente gráfico BPM raw e métricas
  históricas; a cadência Health Connect continua derivada dos buckets D4; o
  modelo/estado/job/wiring de 1 Hz foi removido por completo. Histórico em
  `027-07` a `027-14`, ADR-026, screenshot `027-10` e evidências foi preservado.
- **Desvios:** nenhum. Não houve alteração em `HeartRateSeries`,
  `HeartRateRecord`, `StepsCadenceSeries`, `StepsCadenceRecord`, Daily, crédito,
  retry, identidade ou payload do Health Connect. Nenhuma sincronização física
  nova foi executada.
- **Comandos e resultados:**
  - `git diff --check` — passou, sem erros de whitespace;
  - `./gradlew lint assembleDebug assembleRelease` — primeira tentativa excedeu
    timeout operacional de 300 s; repetição com timeout de 600 s terminou em
    `BUILD SUCCESSFUL` em 5m17s;
  - busca global por `Workout1HzTimeline`, `deriveBpm1Hz` e
    `deriveCadence1Hz` — nenhum uso ativo em `app/src/main`; ocorrências
    restantes ficam somente no histórico documental preservado;
  - testes automatizados — não criados nem executados, conforme solicitação.
- **Evidências:** o diff remove a seção `ActivityDerivedTimelineSection`, o
  gráfico/legenda 1 Hz, `PathEffect` exclusivo, estado derivado, job de cálculo,
  invalidação e derivadores. A busca de Gate A continua encontrando os mappers
  e contratos de BPM raw, `StepsCadenceRecord`, Daily e crédito em `core/model`,
  `core/healthconnect` e `data`.
- **Revisão:** revisão independente aprovou com ressalvas; nenhum bloqueador de
  código foi encontrado. Confirmou ausência de símbolos ativos, preservação de
  BPM raw, cadência D4, Health Connect, Daily, crédito e checklist SDD.

## Riscos residuais e bloqueios

- `027-16` não foi implementada nem iniciada; permanece pendente para revalidar
  somente Gate A e fechar a Spec 027.
- A busca documental ainda referencia símbolos e o caminho removido em tasks,
  ADR e evidências anteriores; isso é histórico intencional, não uso ativo.
- Validação física, Health Connect, Health Sync e Strava não foram reexecutados;
  evidências existentes continuam sendo a fonte do Gate A.

## Riscos

Os riscos de quebra em índices de `combine`, estado de detalhe ou gráfico BPM
raw foram cobertos por diff focado, busca global e build completo; nenhum
bloqueador foi observado. Permanecem somente os bloqueios documentados em
`027-16` e a ausência de nova validação física, que está fora desta subtarefa.
