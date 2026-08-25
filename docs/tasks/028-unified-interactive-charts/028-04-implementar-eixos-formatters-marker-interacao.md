# 028-04 — Implementar eixos, formatters, marker e interação

- **Ticker:** `028`
- **Número:** `04`
- **Status:** `pending`
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

Não criar nem executar testes automatizados sem solicitação explícita.

## Saída esperada

Infraestrutura funcional pronta para a primeira migração real, que será o histórico de passos na `028-05`.