# 014-08 — Derivar cadência, velocidade, ritmo e acumulado

- **Ticker:** `014`
- **Número:** `08`
- **Status:** completed

## Objetivo e resultado esperado

Completar a série V1 sem suavização usando exclusivamente a distância canônica e
a duração efetiva.

Resultado: métricas totais/temporais e acumulado consistentes, com zero passos,
ausências e plausibilidade tratados explicitamente.

## Dependências

- `014-07` concluída.

## Arquivos permitidos

- políticas/contratos derivados em `core/model`;
- mappers/repository de snapshots;
- read models estritamente necessários;
- overview.

## Arquivos proibidos

- parser D4, perfil, UI final, splits e Health Connect;
- Gradle, manifesto, migrations e schemas;
- sobrescrita de dados raw/measured.

## Escopo

```text
cadência = passos ÷ duração em minutos
velocidade = distância canônica ÷ duração
ritmo = duração ÷ distância canônica em quilômetros
```

- distância acumulada soma deltas canônicos em ordem estável;
- total da atividade usa a distância canônica, nunca o total agregado estimado;
- zero passos produz distância, cadência e velocidade zero e ritmo ausente;
- série inelegível mantém métricas dependentes ausentes;
- plausibilidade é propagada, não corrigida;
- nenhuma média de médias.

## Fora do escopo

- suavização/interpolação de velocidade;
- zonas, VO2, calorias ou passada variável;
- splits de quilômetro;
- timestamp de `SpeedRecord`, já decidido para o ponto médio mas implementado em
  `014-13`;
- testes automatizados.

## Passos de implementação

1. Calcular métricas com `Double` sem arredondamento prematuro.
2. Aplicar fórmulas V1 por intervalo elegível.
3. Tratar zero passos conforme contrato.
4. Acumular somente deltas canônicos.
5. Calcular totais pela duração e soma canônica.
6. Proibir acesso funcional à estimativa agregada diagnóstica.
7. Propagar elegibilidade e plausibilidade para consumidores.
8. Persistir/fornecer read model reproduzível.
9. Atualizar overview.

## Critérios de aceite

- Fórmulas e unidades correspondem à spec.
- Acumulado é monotônico e termina exatamente na distância canônica.
- Intervalo zero segue contrato completo.
- Série ausente não produz `NaN`, infinito, zero sintético ou placeholder.
- Métricas importadas pela distância original deixam de alimentar consumidores
  de `WATCH_D3_D4`.
- Estimativa por passos totais nunca aparece como total funcional.
- Plausibilidade permanece visível no read model.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- tabela sanitizada intervalo → duração/passos/distância/cadência/velocidade/ritmo;
- acumulado final igual à distância canônica;
- casos zero, ausente e implausível;
- builds e smoke local.

## Resultado da execução

### Status

- `completed`

### Arquivos alterados

- `docs/tasks/014-watch-workout-derived-metrics/014-08-derive-cadence-speed-pace-and-cumulative-distance.md`;
- `docs/tasks/014-watch-workout-derived-metrics/014-00-overview.md`;
- `docs/specs/014-watch-workout-derived-metrics.md` (progresso alinhado).

Nenhum arquivo de produção foi alterado: a implementação já existente em
`core/model/WatchWorkoutDerivedMetrics.kt`, `DatabaseMappers.kt` e
`RoomRepositories.kt` atende ao escopo desta subtarefa.

### Decisões e desvios

- Inspeção crítica confirmou fórmulas `Double` sem arredondamento prematuro,
  zero passos, ritmo ausente sem distância, acumulados monotônicos por soma
  canônica, duração coberta e propagação de plausibilidade.
- Mappers persistem e reconstróem métricas temporais, acumulados e amostra de
  velocidade com validação de round-trip; repository persiste snapshot e filhos
  somente após preflight.
- Não houve gap delimitado nos arquivos permitidos; nenhuma alteração de código
  foi necessária.
- `ExercisesScreen`, Calendário e demais consumidores finais continuam usando
  contrato legado e permanecem reservados para `014-10`; não foram antecipados.
  O critério sobre consumidores foi validado na fronteira do read model derivado;
  a migração visual final é explicitamente responsabilidade de `014-10`, que
  depende de `014-09` e está fora dos arquivos permitidos desta subtarefa.

### Comandos executados

- `./gradlew lint assembleDebug assembleRelease`;
- `./gradlew installDebug`;
- `git diff --check`;
- `git status --short`.

### Resultados e evidências

- Lint e builds debug/release: PASS.
- Instalação debug: PASS.
- Diff sem erros de whitespace: PASS.
- Evidência de contrato, com `stepLengthMeters = 0,8 m`:

  | intervalo | duração | passos | distância canônica | cadência | velocidade | ritmo |
  |---|---:|---:|---:|---:|---:|---:|
  | 0 | 60 s | 100 | 80 m | 100 passos/min | 1,333333 m/s | 750 s/km |
  | 1 | 60 s | 0 | 0 m | 0 passos/min | 0 m/s | ausente |
  | 2 | 17 s | 50 | 40 m | 176,470588 passos/min | 2,352941 m/s | 425 s/km |

  A série termina em `120 m`, exatamente `80 + 0 + 40`, e o acumulado é
  `80 m → 80 m → 120 m`. A métrica total usa `150 passos / 137 s`, `120 m /
  137 s` e `137 s / 0,120 km`, sem média de médias.
- Caso ausente: `canonicalSeries = null`, sem `NaN`, infinito, zero sintético ou
  placeholder.
- Caso implausível: indicador recebido em `WorkoutSeriesPlausibility` permanece
  inalterado e bloqueia projeção; nenhum limiar foi inventado para classificar
  novos intervalos.
- Estimativa agregada permanece diagnóstica e não participa do total funcional.
- Testes automatizados não foram criados nem executados, conforme escopo.

### Riscos residuais

- Consumidores finais ainda exibem contratos legados até `014-10`.
- Importação D4 física e round-trip Room em dispositivo permanecem para `014-15`.
- Splits e projeções Health Connect permanecem fora desta subtarefa.

## Definição de pronto

Série V1 completa e consistente, sem fallback ou suavização, com item `014-08`
marcado.

## Rollback

Retornar métricas novas como ausentes e preservar snapshots. Não restaurar
original ou estimativa agregada como fallback canônico.
