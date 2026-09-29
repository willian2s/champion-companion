# 037-05 — Renderizar histórico de ritmo e FC

- **Ticker:** `037`
- **Número:** `05`
- **Status inicial:** `pending`
- **Status atual:** `completed`

## Objetivo e resultado esperado

Renderizar tela própria de tendência com lista factual, dois gráficos temporais
de escala correta, provenance, coverage, estados e acessibilidade.

## Requisitos cobertos

- ritmo em `m:ss /km`;
- FC média em `bpm` e coverage separado;
- provenance perceptível sem enums internos;
- pontos por corrida, inclusive mesmo dia/instante;
- gráficos sem dual-axis ou scatter falso;
- estados de ausência, parcialidade e falha;
- acessibilidade e linguagem não avaliativa.

## Escopo incluído

- `RunningPerformanceHistoryScreen` e componentes locais;
- cards/lista paginada que não dependem do gráfico;
- `ChampionTimeSeriesChart`/`ChampionLineChart` com uma métrica por eixo;
- x ordinal crescente com label temporal real;
- `y = null` para FC ausente;
- semantics de período, quantidade, unidades, lacunas e provenance.

## Escopo excluído

- alteração de `ChampionChartContracts`, Vico ou tema compartilhado;
- scatter, dual-axis, normalização de valores ou slope;
- agregação por dia ou comparação de períodos;
- cálculo de domínio dentro de Compose;
- linguagem de melhor/pior, eficiência, ganho, condicionamento ou risco;
- Health Connect, persistência, snapshots ou calibração;
- testes automatizados novos.

## Dependências

- `037-04` concluída;
- `RunningPerformanceHistory` e estados da 037-02;
- `ChampionChartContracts.kt`, `ChampionCharts.kt`, `ChampionChartTheme.kt`;
- padrões visuais de Exercícios e Specs 028/036.

## Arquivos e símbolos prováveis

- novo `feature/exercises/RunningPerformanceHistoryScreen.kt`;
- `feature/exercises/ExercisesScreen.kt` para CTA/composição;
- `ChampionCartesianChartData`, `ChampionChartSeries`,
  `ChampionChartPoint`, `ChampionTimeSeriesChart`;
- formatter local de pace e label de provenance.

## Passos de implementação

1. Renderizar título, período e explicação factual.
2. Renderizar os gráficos antes da lista para priorizar a síntese temporal.
3. Renderizar lista ordenada por data/hora, com status de pace/FC e provenance.
   Versão nativa diferente de v4 mostra `Distância do relógio — versão não
   comparável` e fica fora dos gráficos.
4. Paginar a lista em páginas de 10 corridas quando necessário, preservando a
   ordem recebida e sem paginar os gráficos.
5. Mapear pontos para posição ordinal crescente; manter `startedAt` em labels.
6. Criar gráfico de ritmo com eixo Y `s/km` e formatter `m:ss /km`.
7. Criar gráfico de FC com eixo Y `bpm`, omitindo y de ausência sem zero.
8. Alinhar ambos por ordem e labels, sem usar a mesma escala Y.
9. Renderizar `SOURCE_UNAVAILABLE` somente como indisponibilidade segura, sem
   apresentar histórico parcial como disponível.
10. Adicionar semantics agregadas, descrição de gaps e suporte a múltiplas
   corridas no mesmo dia.
11. Revisar textos para remover qualquer inferência de desempenho.

## Testes e comandos de validação

Não criar testes automatizados. Executar:

```bash
git diff --check
```

Validação visual física e screenshot sanitizada pertencem à `037-06`.

## Definição de pronto

- lista explica dados mesmo sem gráfico;
- gráficos aparecem antes da lista; lista usa páginas de 10 quando necessário;
- pace/FC usam unidades e escalas corretas;
- provenance e coverage aparecem em cada linha relevante;
- ausência de FC é gap/estado, não zero;
- corridas distintas não são agregadas;
- semantics não dependem só de cor;
- nenhuma mudança no design system compartilhado.

## Riscos e cuidados

- `ChampionChartSeries` exige X estritamente crescente;
- não passar pace e BPM ao mesmo eixo;
- não usar BPM como X para simular scatter;
- arredondar apenas na apresentação;
- labels de provenance devem ser compreensíveis e não revelar enum interno;
- versão nativa não suportada permanece factual na lista, mas nunca entra na
  série comparável;
- screenshot não pode expor IDs, BPM sample a sample ou dados pessoais.

## Execução e evidências

### Implementação realizada

- `RunningPerformanceHistoryScreen` agora apresenta título, explicação factual,
  período de 56 dias, estados top-level seguros e lista independente dos gráficos.
- Gráficos aparecem antes da lista; corridas são paginadas localmente em páginas
  de 10, preservando a ordem e a janela de dados.
- Cada corrida exibe data/hora, ritmo em `m:ss /km`, distância, FC média,
  cobertura percentual, provenance traduzida e estados de ausência, invalidez,
  fallback e versão não comparável.
- Foram adicionados dois `ChampionTimeSeriesChart` independentes: ritmo em
  `s/km` com formatter `m:ss /km` e FC em `bpm`, usando índice ordinal e
  `y = null` para ausência de FC. Contratos, renderer e tema compartilhados não
  foram alterados.
- Semantics agregadas anunciam período, quantidade, unidades, lacunas,
  parcialidade, valores e provenance sem expor IDs ou dados de samples. Os
  controles de página usam linguagem factual.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/RunningPerformanceHistoryScreen.kt`
- `docs/specs/037-running-pace-heart-rate-trend.md` — requisito de paginação
  local em páginas de 10.
- `docs/tasks/037-running-pace-heart-rate-trend/037-00-overview.md` — decisão
  de ordenação e paginação registrada.
- este arquivo — status e evidências da subtarefa.

### Comandos executados e resultados

- `git diff --check` — passou.
- `./gradlew app:compileDebugKotlin` — passou (`BUILD SUCCESSFUL`).
- `./gradlew lint assembleDebug assembleRelease` — passou (`BUILD SUCCESSFUL`).
- Revisão estática confirmou eixo de ritmo com unidade `s/km`, formatter visual
  `m:ss /km`, eixo independente de FC em `bpm` e pontos ordinais crescentes.

### Riscos residuais

- Validação visual em dispositivo, TalkBack e cenários naturais de fallback,
  lacunas e fonte indisponível permanecem para `037-06`.

## Revisão independente

- Agente `review` aprovou após correção do formatter de eixo: o gráfico de ritmo
  combina `m:ss` com unidade `/km` sem duplicação, mantendo título semântico
  `s/km`.
- Semantics de `NO_COMPARABLE_RUNS` agora informa ausência de gráficos, sem
  anunciar lacunas inexistentes.
- Segunda revisão independente confirmou gráficos antes da lista, paginação em
  blocos de 10, ordem preservada, gráficos não paginados e controles acessíveis.
- Nenhum blocker, major ou minor permaneceu; validação física, TalkBack e
  screenshot sanitizada continuam em `037-06`.
