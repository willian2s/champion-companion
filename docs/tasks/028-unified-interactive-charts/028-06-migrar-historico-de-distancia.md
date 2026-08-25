# 028-06 — Migrar histórico de distância

- **Ticker:** `028`
- **Número:** `06`
- **Status:** `completed`
- **Depends on:** `028-05`

## Objetivo

Migrar o histórico de distância para a infraestrutura compartilhada, eliminando a implementação Canvas paralela ao histórico de passos sem alterar a semântica de estimativa já vigente.

## Escopo

- adaptar `CalendarDistanceHistorySnapshot` para contratos Champion de gráfico;
- renderizar barras via componente compartilhado;
- formatar eixo/tooltip em quilômetros ou metros conforme a escala escolhida pela camada de apresentação;
- preservar dias sem estimativa como gaps;
- preservar o texto que deixa claro que ausência não significa zero quilômetros;
- reutilizar marker, tooltip, eixos e tema da infraestrutura comum;
- manter períodos de 7, 30 e 90 dias.

## Guard rails

- não recalcular distância;
- não alterar `estimatedDistanceMeters` nem suas regras de derivação;
- não preencher gaps;
- não criar escala divergente apenas para melhorar aparência;
- remover somente o Canvas local de distância substituído.

## Critérios de aceite

1. `DistanceHistoryChart` deixa de implementar barras diretamente.
2. A feature não importa Vico.
3. Tooltip informa data e distância do bucket selecionado.
4. Dias sem estimativa permanecem identificados como ausência de dado.
5. Eixo Y apresenta valores reais com unidade explícita.
6. 7/30/90 dias permanecem legíveis e consistentes com passos.
7. Tema e interação seguem a mesma linguagem do gráfico migrado na `028-05`.

## Validação física

Validar em aparelho:

- 7, 30 e 90 dias;
- dias com e sem estimativa;
- tap/drag;
- rolagem vertical;
- tema claro/escuro;
- fonte ampliada representativa.

Registrar screenshot sanitizado.

## Validação técnica

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem solicitação explícita.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryChart.kt`;
- `docs/integration/028-06-distance-history-final.png`;
- este arquivo;
- `docs/tasks/028-unified-interactive-charts/028-00-overview.md`.

## Decisões e desvios

- `DistanceHistoryChart` passou a adaptar `CalendarDistanceHistorySnapshot` para
  `ChampionCartesianChartData` e renderizar com `ChampionBarChart`.
- `estimatedDistanceMeters` continua nullable na adaptação: valores são
  convertidos para quilômetros somente na apresentação, e `null` permanece gap.
- Eixo Y e tooltip usam quilômetros com unidade explícita; datas reais do bucket
  permanecem no eixo X e no tooltip.
- Períodos 7/30/90 continuam usando a política existente, com scroll horizontal
  somente acima de 7 dias. Shell, resumo e lista acessível permanecem intactos.
- Canvas local, escalas, cores e labels manuais do gráfico de distância foram
  removidos sem alteração em domínio, repository, ViewModel, Room ou Health
  Connect.

## Comandos executados

- `git diff --check` — passou.
- `./gradlew lint assembleDebug assembleRelease` — passou.
- `./gradlew installDebug` — passou no `SM-S901E`.
- Não foram criados nem executados testes automatizados.

## Validação física e evidências

- `SM-S901E`: períodos 7D, 30D e 90D exibidos com eixos e valores em km — passou.
- `SM-S901E`: dias com estimativa e gaps visíveis; texto informa que lacunas não
  representam zero quilômetros — passou.
- `SM-S901E`: rolagem vertical — passou; scroll horizontal permanece configurado
  para períodos longos, sem evidência física reproduzível nesta sessão.
- `SM-S901E`: tema escuro, tema claro e fonte ampliada representativa — passou.
- Screenshot sanitizado: `docs/integration/028-06-distance-history-final.png`.

## Riscos residuais

- Tap/drag não gerou evidência física reproduzível nesta sessão; interação usa o
  marker/tooltip compartilhado já validado em `028-05`.
- Scroll horizontal permanece sem evidência física reproduzível nesta sessão.
