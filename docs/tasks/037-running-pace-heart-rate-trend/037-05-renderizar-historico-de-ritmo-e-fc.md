# 037-05 — Renderizar histórico de ritmo e FC

- **Ticker:** `037`
- **Número:** `05`
- **Status inicial:** `pending`

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
- cards/lista que não dependem do gráfico;
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
2. Renderizar lista ordenada por data/hora, com status de pace/FC e provenance.
   Versão nativa diferente de v4 mostra `Distância do relógio — versão não
   comparável` e fica fora dos gráficos.
3. Mapear pontos para posição ordinal crescente; manter `startedAt` em labels.
4. Criar gráfico de ritmo com eixo Y `s/km` e formatter `m:ss /km`.
5. Criar gráfico de FC com eixo Y `bpm`, omitindo y de ausência sem zero.
6. Alinhar ambos por ordem e labels, sem usar a mesma escala Y.
7. Renderizar `SOURCE_UNAVAILABLE` somente como indisponibilidade segura, sem
   apresentar histórico parcial como disponível.
8. Adicionar semantics agregadas, descrição de gaps e suporte a múltiplas
   corridas no mesmo dia.
9. Revisar textos para remover qualquer inferência de desempenho.

## Testes e comandos de validação

Não criar testes automatizados. Executar:

```bash
git diff --check
```

Validação visual física e screenshot sanitizada pertencem à `037-06`.

## Definição de pronto

- lista explica dados mesmo sem gráfico;
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
