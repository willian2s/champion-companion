# 028-05 — Migrar histórico de passos

- **Ticker:** `028`
- **Número:** `05`
- **Status:** `completed`
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

## Evidências

- `StepsHistoryChart` agora usa `ChampionBarChart` e contratos de `core/designsystem/chart`.
- Pontos usam índice ordenável no período, data real em `xLabel`/descrição e `null` para lacunas.
- Eixo Y e tooltip mostram somente valores reais, começa em zero e inclui o máximo entre meta e registro; meta usa linha de referência compartilhada.
- Eixo X usa `dd/MM` para manter labels legíveis em 7, 30 e 90 dias; tooltip mantém data completa.
- Scroll horizontal fica opt-in somente para 30/90 dias; 7 dias permanece sem scroll/zoom.
- Lista de lacunas mostra todas as datas em períodos curtos e resume períodos densos para preservar legibilidade.
- Tema compartilhado usa acentos Champion estáveis para manter barras, marker e meta coloridos mesmo com dynamic color acromático.
- Lista acessível, read model, Room e estados de issue não foram alterados.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/StepsHistoryChart.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/chart/ChampionChartTheme.kt`
- `docs/integration/028-05-steps-history-final.png`
- `docs/integration/028-05-steps-history-marker-final.png`
- este arquivo;
- `docs/tasks/028-unified-interactive-charts/028-00-overview.md`.

## Decisões e desvios

- Unidade foi removida do eixo Y e do tooltip conforme revisão visual; série e textos de apoio continuam identificando métrica como `Passos`.
- Dynamic color permanece aplicado a superfícies, eixos e tooltip. Séries usam paleta Champion já existente para evitar barras pretas/brancas em temas dinâmicos acromáticos.
- Scroll horizontal foi ativado apenas quando período excede 7 dias, por densidade de buckets; zoom permanece desativado.
- Não houve alteração de Room, read model, ViewModel, lista acessível, domínio ou contratos persistentes.

## Comandos executados

- `git diff --check` — passou.
- `./gradlew :app:compileDebugKotlin` — passou.
- `./gradlew lint assembleDebug assembleRelease` — passou.
- `./gradlew installDebug` — passou no `SM-S901E`.
- Validação física 30/90 — passou com scroll horizontal até buckets registrados e lista paginada coerente.
- Não foram criados nem executados testes automatizados.

## Validação física e evidências

- `SM-S901E`: 7 dias com dados e gaps, rolagem vertical e gráfico colorido — passou.
- `SM-S901E`: tap/drag exibiu marker e tooltip com `Passos`, data completa e valor real — passou.
- `SM-S901E`: períodos 30 e 90 dias selecionados e agregados/lista coerentes — passou.
- Screenshots sanitizados:
  - `docs/integration/028-05-steps-history-final.png`
  - `docs/integration/028-05-steps-history-marker-final.png`

## Riscos residuais

- Tema claro e fonte ampliada não foram exercitados fisicamente nesta sessão.
- Densidade de labels em matrizes de idioma diferentes de `pt-BR` permanece dependente do formatter `dd/MM` e deve ser observada nas próximas migrações.
