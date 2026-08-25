# 028-04 — Implementar eixos, formatters, marker e interação

- **Ticker:** `028`
- **Número:** `04`
- **Status:** `completed`
- **Depends on:** `028-03`

## Objetivo

Implementar o renderer compartilhado sobre Vico dentro de `core/designsystem/chart`, expondo componentes Champion reutilizáveis para gráficos de linha, barras e séries temporais.

## Escopo

Implementar pelo menos:

- `ChampionLineChart`;
- `ChampionBarChart`;
- `ChampionTimeSeriesChart`;
- eixo X temporal/categórico;
- eixo Y com valores reais;
- formatters comuns;
- marker visual;
- tooltip padronizado;
- seleção por toque/drag;
- scroll e zoom configuráveis e desabilitados por padrão;
- estado vazio e resumo acessível comuns.

## Regras de interação

Fluxo esperado:

```text
tap/drag
  -> localizar ponto ou bucket mais próximo
  -> exibir marker
  -> exibir tooltip
```

O tooltip deve mostrar somente informação existente no ponto/bucket selecionado.

## Guard rails

- features não importam Vico;
- o renderer não recebe `WorkoutActivity`, snapshots de calendário ou modelos de domínio;
- não normalizar silenciosamente valores para `0..1` na apresentação final;
- gestos do gráfico não devem bloquear `verticalScroll` sem necessidade;
- zoom e scroll são opt-in;
- gaps não devem ser conectados como se houvesse dados intermediários.

## Critérios de aceite

1. Há pelo menos um renderer comum para linha e barras.
2. Eixos exibem valores/unidades reais via contratos Champion.
3. Marker/tooltip possuem comportamento comum.
4. Tap e drag selecionam o valor mais próximo de forma previsível.
5. Scroll/zoom podem ser habilitados por configuração sem virar padrão global.
6. Acessibilidade e estado vazio não dependem da feature consumidora.
7. Nenhuma tela de produto é migrada nesta task, salvo preview técnico se necessário.

## Validação

Executar:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Execução desta subtarefa:

- `git diff --check` — passou;
- `./gradlew :app:compileDebugKotlin` — passou após correção do import de
  `LocalLayoutDirection`;
- `./gradlew lint assembleDebug assembleRelease` — passou (`BUILD SUCCESSFUL`);
- não foram criados nem executados testes automatizados.

## Implementação realizada

- `ChampionCharts.kt` encapsula Vico e expõe `ChampionLineChart`,
  `ChampionBarChart` e `ChampionTimeSeriesChart`.
- Eixos X/Y delegam para os formatters Champion e preservam unidades reais no
  eixo Y e no tooltip.
- Marker, tooltip e callback de seleção compartilham nearest-point por press e
  movimento; o controller rejeita seleção em gaps quando a coordenada do gesto
  corresponde a bucket sem dado.
- Linhas particionam séries em segmentos contíguos e barras omitem `y = null`,
  sem converter gaps em zero, interpolar ou suavizar.
- Scroll e zoom permanecem opt-in; zoom habilitado também habilita scroll, como
  exigido pelo Vico para produzir efeito observável.
- Estado vazio, resumo acessível, descrições de pontos, lacunas e referências
  são tratados no renderer comum.
- Cores de linhas, barras, eixos, grid, marker e referências derivam do
  `ChampionChartTheme` baseado em Material 3.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/chart/ChampionCharts.kt`
- este arquivo de subtarefa;
- `docs/tasks/028-unified-interactive-charts/028-00-overview.md`.

## Decisões e desvios

- A implementação usa segmentação para preservar gaps porque o modelo de série
  do Vico não aceita `null` em Y.
- O controller usa a coordenada bruta de interação exposta por Vico e os
  espaçamentos configurados no renderer para evitar mostrar o ponto vizinho como
  se fosse um valor de gap. Não há valor sintético quando o alvo não pode ser
  mapeado.
- Bounds intrínsecos incluem pontos nulos; faixas singleton recebem apenas
  padding visual seguro, sem alterar valores entregues aos formatters.
- Nenhuma tela de produto, feature, domínio, Room, BLE ou Health Connect foi
  alterada. Nenhum ADR adicional foi necessário, pois ADR-027 já formaliza a
  boundary e a engine.

Não criar nem executar testes automatizados sem solicitação explícita.

## Riscos residuais

- Não houve validação física de drag, RTL, zoom ou convivência com
  `verticalScroll`; builds não comprovam comportamento gestual em dispositivo.
- A classificação de gap depende dos espaçamentos Vico mantidos em sincronia
  com os tokens internos do renderer; séries com espaçamento X muito irregular
  devem ser validadas durante migrações consumidoras.
- Ainda não há consumidor de feature; a primeira migração está planejada na
  `028-05`.

## Saída esperada

Infraestrutura funcional pronta para a primeira migração real, que será o histórico de passos na `028-05`.
