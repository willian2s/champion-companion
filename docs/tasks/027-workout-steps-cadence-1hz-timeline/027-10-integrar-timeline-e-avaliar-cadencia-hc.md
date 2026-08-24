# 027-10 — Integrar timeline e avaliar cadência 1 Hz no Health Connect

- **Ticker:** `027`
- **Número:** `10`
- **Status:** `completed`
- **Depends on:** `027-06`, `027-07`, `027-08`, `027-09`

## Objetivo

Usar a timeline comum de BPM/cadência nas superfícies locais adequadas e decidir se a projeção de cadência do Health Connect deve permanecer por bucket D4 ou migrar para série 1 Hz derivada.

## UI/local

Integrar somente onde acrescentar valor real, prioritariamente detalhe da atividade/gráficos.

A visualização deve:

- usar eixo temporal comum;
- preservar gaps;
- não mostrar ponto ausente como zero;
- não alterar silenciosamente métricas históricas já exibidas;
- manter desempenho adequado para treinos longos.

Se houver mudança visual relevante, capturar screenshot conforme `AGENTS.md`.

## Comparação Health Connect

Usar a evidência de `027-06` para comparar:

### Opção A — cadência por bucket

- menos pontos;
- máxima fidelidade ao dado derivado diretamente do D4;
- gráfico externo potencialmente mais grosseiro.

### Opção B — cadência derivada 1 Hz

- gráfico temporal mais contínuo;
- muito mais samples;
- valores interpolados, não observados;
- pode melhorar ou não a interpretação pelo Health Sync/Strava.

## Decisão

Somente promover 1 Hz para `StepsCadenceRecord` se:

- Health Connect aceitar a série com comportamento estável;
- Health Sync/Strava demonstrar benefício concreto ou houver outro consumidor real;
- não houver regressão de performance/tamanho/retry;
- a proveniência derivada permanecer clara no app;
- a mesma identidade/versionamento remoto puder ser atualizada com segurança.

Caso contrário, manter HC por buckets e usar 1 Hz apenas localmente.

## BPM no Health Connect

Esta task não deve substituir automaticamente as amostras de BPM da atividade por uma série interpolada. Qualquer mudança desse tipo exige decisão explícita separada.

## Verificação

```bash
./gradlew lint assembleDebug assembleRelease
```

Mudança visual exige evidência visual; não criar nem executar testes automatizados sem pedido explícito.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/Workout1HzTimeline.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`;
- `docs/decisions/adr-026-workout-1hz-derived-timeline.md`;
- `docs/integration/027-10-detail-timeline.png`;
- este arquivo;
- `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-00-overview.md`.

## Decisões e desvios

- timeline comum foi integrada somente no detalhe da atividade, após gráfico BPM
  original;
- BPM raw, SOURCE D4, métricas históricas, intervalos e estatísticas existentes
  permanecem inalterados;
- cálculo ocorre sob demanda em `Dispatchers.Default`, com invalidação por
  atividade/geração e downsample de renderização para treinos longos;
- gaps permanecem ausentes, sem conversão para zero; provenance interpolada é
  indicada por linha tracejada;
- `StepsCadenceRecord` permanece publicado por buckets D4. A evidência de
  `027-06` confirma aceite e cadência no Health Sync/Strava, mas não demonstra
  benefício concreto de série 1 Hz, consumidor real, estabilidade para payload
  maior ou segurança de versionamento remoto. Migração para 1 Hz não foi
  promovida;
- não houve sincronização adicional com Health Connect durante a validação
  visual. A atividade foi apenas aberta localmente no dispositivo.

## Comandos executados e resultados

- `./gradlew app:compileDebugKotlin` — `BUILD SUCCESSFUL`;
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`;
- `./gradlew installDebug` — `BUILD SUCCESSFUL`;
- `git diff --check` — passou, sem erros de whitespace;
- `adb devices`, `adb shell getprop`, `adb shell pm path` — dispositivo
  `SM-S901E`, Android 16, APK debug instalada;
- validação visual local — detalhe de atividade abriu e exibiu timeline comum
  BPM/cadência, legenda e política de gaps;
- screenshot sanitizado: `docs/integration/027-10-detail-timeline.png`;
- nenhum teste automatizado foi criado ou executado, conforme regra do projeto.

## Riscos residuais e bloqueios

- não foi feita nova validação de interoperabilidade Health Connect/Health Sync
  para série 1 Hz; decisão conservadora mantém buckets D4;
- screenshot cobre atividade local existente e não valida comportamento de todos
  os tipos, gaps ou treinos longos;
- validação física final e fechamento documental da Spec permanecem em `027-11`.

Próxima subtarefa elegível: `027-11`.
