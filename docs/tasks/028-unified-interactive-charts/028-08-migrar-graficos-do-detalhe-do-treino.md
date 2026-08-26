# 028-08 — Migrar gráficos do detalhe do treino

- **Ticker:** `028`
- **Número:** `08`
- **Status:** `completed`
- **Depends on:** `028-07`

## Objetivo

Migrar os gráficos atuais do detalhe do treino para a infraestrutura compartilhada, removendo o desenho local de frequência cardíaca e distância acumulada sem alterar a série canônica nem as amostras reais do treino.

## Escopo

Migrar:

- frequência cardíaca do treino;
- distância canônica acumulada por intervalo.

Para frequência cardíaca:

- usar apenas `WorkoutHeartRateSample` já persistido/disponível;
- eixo X temporal real;
- eixo Y em `bpm`;
- marker/tooltip com instante e BPM;
- preservar guidelines de BPM apenas se puderem ser representadas pela infraestrutura comum sem manter engine local paralela.

Para distância canônica:

- usar `CanonicalWorkoutSeries.intervals` vigente;
- eixo X baseado nas janelas efetivas reais;
- eixo Y em distância acumulada real;
- marker/tooltip por intervalo/ponto;
- preservar aviso de diagnóstico quando a série estiver em estado de plausibilidade bloqueada.

## Guard rails

- não reamostrar BPM;
- não interpolar distância entre intervalos além do que o renderer naturalmente conecta entre pontos existentes;
- não alterar derivação D4;
- não tocar revisão, calibração, workflow ou Health Connect;
- scroll/zoom só quando necessário para série longa;
- não adicionar cadência nesta task; isso pertence à `028-09`.

## Critérios de aceite

1. `WorkoutHeartRateChart` deixa de manter engine Canvas própria.
2. `WorkoutCanonicalDistanceChart` deixa de manter engine Canvas própria.
3. Ambos usam a mesma infraestrutura compartilhada.
4. BPM e distância exibem valores/unidades reais nos eixos e tooltip.
5. Estados indisponíveis, parciais e de diagnóstico continuam coerentes.
6. A interação não prejudica a rolagem vertical do detalhe.
7. A feature de exercícios não importa Vico diretamente.

## Validação física

Validar em treino real importado:

- BPM com toque/drag;
- distância acumulada com toque/drag;
- treino com número representativo de intervalos;
- rolagem vertical da tela;
- tema claro/escuro;
- fonte ampliada;
- estados de série indisponível quando houver fixture real disponível.

Registrar screenshots sanitizados dos dois gráficos.

## Validação técnica

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem solicitação explícita.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/chart/ChampionCharts.kt`;
- `docs/integration/028-08-workout-heart-rate-final.png`;
- `docs/integration/028-08-workout-distance-final.png`;
- `docs/tasks/028-unified-interactive-charts/028-00-overview.md`;
- este arquivo.

## Decisões e desvios

- `WorkoutCanonicalDistanceChart` passou a adaptar `CanonicalWorkoutSeries.intervals` para
  `ChampionTimeSeriesChart`, usando segundos decorridos desde o início da primeira janela
  efetiva e distância acumulada em quilômetros, sem usar índice, reamostrar ou interpolar.
  A escala usa máximo real da série, com mínimo visual de `0,01 km` somente para série zerada.
- `WorkoutHeartRateChart` ordena somente uma cópia visual das amostras persistidas, usa segundos
  decorridos desde a primeira amostra e preserva timestamps nos labels e no tooltip. Labels de
  interação usam `HH:mm:ss` para manter instante e BPM visíveis; descrição acessível mantém data
  completa. Amostras com X empatado são distribuídas em séries visuais paralelas.
- Guidelines de BPM passaram a `ChampionChartReferenceLine`; eixos, marker, tooltip e
  acessibilidade ficam no renderer compartilhado. O eixo usa `HH:mm` compacto e o tooltip
  preserva `HH:mm:ss`. Séries com mais de seis pontos habilitam scroll horizontal; zoom permanece
  desabilitado. O eixo X recebe padding visual de metade do menor intervalo temporal observado,
  sem criar ponto ou alterar valor persistido, para manter pontos e labels extremos visíveis. O
  eixo horizontal compartilhado usa `HorizontalAxis.ItemPlacer.aligned` com
  `addExtremeLabelPadding = true`, preservando labels temporais extremos completos.
- Estados externos de série nula/vazia, resumo acessível e aviso `PLAUSIBILITY_BLOCKED` foram
  preservados. Nenhuma feature importa Vico.

## Comandos executados

- `git diff --check` — passou.
- `./gradlew :app:compileDebugKotlin` — passou (`BUILD SUCCESSFUL`) após configurar o
  `ItemPlacer` compartilhado com padding dos labels extremos.
- `./gradlew lint assembleDebug assembleRelease` — passou (`BUILD SUCCESSFUL`).
- `./gradlew installDebug` — passou no `SM-S901E`; validação física executada em
  `br.com.mwconsulting.championcompanion.dev` (`1.0-dev`).
- `HorizontalAxis.ItemPlacer.aligned(addExtremeLabelPadding = true)` foi validado no
  renderer compartilhado; `./gradlew :app:compileDebugKotlin` passou (`BUILD SUCCESSFUL`).
- Não foram criados nem executados testes automatizados.

## Validação física e evidências

- `SM-S901E` / build `.dev`: treino real importado com 33 intervalos canônicos e 33 amostras
  reais de BPM; gráficos exibidos, passou.
- `SM-S901E` / build `.dev`: acessibilidade expôs todos os 33 pontos de distância, término em
  `4,06 km`, e todas as 33 amostras de BPM com média `146 bpm`, mínimo `96 bpm` e máximo
  `181 bpm` — passou.
- `SM-S901E` / build `.dev`: scroll horizontal percorreu série longa até janela final `07:34`–
  `07:40`, mantendo ponto final e resumo `4,06 km` visíveis — passou.
- `SM-S901E` / build `.dev`: label inicial completo na janela inicial e labels temporais da
  janela final completos após rolagem horizontal; label parcialmente visível na borda oposta
  pertence à janela anterior e sai naturalmente do viewport — passou.
- `SM-S901E` / build `.dev`: toque prolongado exibiu tooltip BPM `07:08:55 · 96 bpm` e distância
  `07:09:24 · 0,09 km` — passou.
- `SM-S901E` / build `.dev`: rolagem vertical do detalhe durante consulta dos gráficos — passou.
- `SM-S901E` / build `.dev`: tema escuro observado — passou.
- `SM-S901E` / build `.dev`: tema claro e fonte ampliada (`font_scale 1.15`) observados no
  detalhe/lista sem perda de leitura ou clipping estrutural — passou.
- Screenshots sanitizados:
  - `docs/integration/028-08-workout-heart-rate-final.png`;
  - `docs/integration/028-08-workout-distance-final.png`.

## Riscos residuais

- Não havia fixture real disponível para exercitar estado de série indisponível/parcial; o código
  preserva esses estados por inspeção e esse risco permanece condicional.
