# 028-05 — Migrar histórico de passos

- **Ticker:** `028`
- **Número:** `05`
- **Status:** `pending`
- **Depends on:** `028-04`

## Objetivo

Migrar o histórico de passos para a infraestrutura compartilhada de gráficos, substituindo o desenho local em `StepsHistoryChart` sem alterar o read model ou a semântica dos dados.

## Escopo

- adaptar `CalendarStepsHistorySnapshot` para contratos Champion de gráfico na camada de apresentação;
- renderizar barras reais via componente compartilhado;
- apresentar eixo X coerente com o período selecionado;
- apresentar eixo Y/tooltip em passos reais;
- preservar meta diária quando disponível como referência visual, se isso puder ser feito sem mudar contratos de domínio;
- preservar gaps como ausência de dado;
- permitir tap/drag com marker e tooltip;
- manter 7/30/90 dias com comportamento adequado de densidade.

## Guard rails

- não alterar Room nem o read model do calendário;
- não transformar dias sem dado em zero;
- não introduzir scroll/zoom em 7 dias sem necessidade;
- não adicionar métricas novas;
- remover apenas o Canvas de passos efetivamente substituído.

## Critérios de aceite

1. `StepsHistoryChart` deixa de possuir engine própria de barras.
2. O gráfico usa `core/designsystem/chart` e a feature não importa Vico.
3. Tap/drag exibe data e passos do bucket selecionado.
4. Gaps continuam explicitamente distinguíveis de zero passos.
5. Eixos e labels permanecem legíveis nos períodos suportados.
6. Light/dark seguem o tema comum.
7. Lista/histórico acessível continua disponível e coerente com o gráfico.

## Validação física

Validar em aparelho:

- 7, 30 e 90 dias;
- toque e drag em barras;
- dias com dados e gaps;
- rolagem vertical da tela;
- tema claro e escuro;
- fonte ampliada representativa.

Registrar screenshot sanitizado da superfície migrada.

## Validação técnica

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem solicitação explícita.