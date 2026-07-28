# 015-09 — Implementar gráfico e lista de histórico de passos

- **Status:** concluída em 28/07/2026.
- **Objetivo:** transformar shell de passos em histórico local funcional.
- **Resultado esperado:** períodos de 7, 30 e 90 dias com métricas, barras e
  lista acessível.

## Documentos obrigatórios

Spec 015, 015-06, 015-05, Health contracts, `HealthMetricDetailScreens.kt`,
`HealthViewModel.kt`, `DailySummary` e design system.

## Dependências e entradas

- 015-06 e 015-05 concluídas.

## Arquivos permitidos

Componentes isolados de passos, ViewModel temporal de passos e modelos de
apresentação; documentação e screenshots.

## Arquivos proibidos

BLE, refresh, D1, activities como fonte de passos, Room/schema, navigation,
Health Connect, testes e alteração de workflow.

## Escopo

Exibir total, média por dia registrado, máximo, dias registrados, dias na meta
atual, barras e lista. Diferenciar sem resumo de resumo com zero. Rotular meta
como **Meta atual**. Preservar lacunas e acessibilidade textual.

## Fora do escopo

Editar meta, criar histórico de metas, somar atividades, sincronizar ou mudar
Home.

## Passos

1. Conectar snapshot diário selecionado.
2. Implementar períodos 7/30/90.
3. Calcular métricas sem preencher lacunas.
4. Renderizar barras e lista acessível.
5. Integrar empty/loading/error.

## Critérios de aceite

- Dados vêm somente de DailySummary.
- Zero real não é confundido com ausência.
- Lacunas não entram como zero nas médias.
- Meta é identificada como atual.
- Períodos funcionam sem BLE ou provider.
- Histórico mostra data, valor, origem e última atualização.
- Períodos exibem total, média, máximo, dias registrados e dias na meta.
- Barras têm lacunas reais e lista textual acessível.
- A lista distingue `sem dados` de `0 passos` e identifica **Meta atual**.

## Validação manual

Galaxy S22 com dias ausentes, zero e acima/abaixo da meta. Conferir gráfico,
lista, rotação/escala e leitor de tela. Screenshot exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar cálculos exibidos, períodos, lacunas e screenshots. Atualizar overview.

## Validação final

Confirmar nenhum passo de atividade, BLE ou Health Connect. Não criar, alterar
ou executar testes automatizados.

## Contexto

Passos possuem somente resumo diário atual e o shell existente não oferece
histórico consolidado.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/StepsHistoryShell.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/StepsHistoryChart.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/StepsHistoryFeatureContract.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/StepsHistoryViewModel.kt`;
- `docs/tasks/015-calendar-consolidated-history/015-09-steps-history-chips.png`;
- `docs/tasks/015-calendar-consolidated-history/015-09-steps-history-chart-list.png`;
- este arquivo e `015-00-overview.md`.

## Decisões e desvios

- `StepsHistoryViewModel` observa `CalendarRepository.observeStepsHistory` com
  `ZoneId` congelado e janela semiaberta incluindo o dia atual; nenhuma operação
  externa é iniciada.
- O seletor usa `OutlinedButton`, pesos iguais e rótulos compactos `7D`, `30D` e
  `90D`, seguindo o padrão visual do histórico de FC e evitando quebra de linha.
- A lista segue o padrão do histórico de FC, com metadados à esquerda, valor à
  direita e **Meta atual** explícita no cabeçalho e na semântica acessível.
- A lista é ordenada do dia mais recente para o mais antigo e usa páginas de até
  10 dias, com controles `Anterior` e `Próxima` quando necessário.
- Lacunas usam contorno no gráfico e `Sem dados` na lista; zero real mantém barra
  mínima e exibe `0 passos`.
- Nenhum desvio aprovado; labels compactos são somente apresentação.

## Screenshot

Screenshots registrados:

- `015-09-steps-history-chips.png`: períodos sem quebra de linha;
- `015-09-steps-history-chart-list.png`: gráfico com lacunas, lista textual e
  **Meta atual**.
- `015-09-steps-history-paginated.png`: janela de 90 dias com página `1 de 9`,
  começando pelo dia mais recente.

## Riscos residuais

Escala de barras pode sugerir zero para lacuna; representação visual deve manter
diferença explícita.

- Validação física observou seis lacunas e um registro abaixo da meta; zero real,
  TalkBack e rotação não foram reproduzidos nesta sessão e permanecem validações
  manuais residuais.
- Histórico de 90 dias ainda compõe snapshot completo antes de paginar a
  apresentação; monitorar desempenho antes de qualquer decisão de lazy list.

## Atualização do overview

Item `015-09` marcado `[x]`; progresso recalculado para `10/14`. Demais itens
 permanecem `[ ]` e nenhuma subtarefa seguinte foi iniciada.

## Comandos executados

- `./gradlew :app:compileDebugKotlin`;
- `./gradlew app:lintDebug`;
- `./gradlew lint assembleDebug assembleRelease`;
- `./gradlew installDebug`;
- `git diff --check`.

Não foram criados, alterados ou executados testes automatizados, conforme regra
da spec e desta subtarefa.

## Resultados e evidências

- Compilação Kotlin debug e lint concluíram com `BUILD SUCCESSFUL`.
- `lint assembleDebug assembleRelease` concluiu com `BUILD SUCCESSFUL` após
  recompilação final.
- `installDebug` concluiu com `BUILD SUCCESSFUL` e instalou no Galaxy S22
  `SM-S901E`.
- Smoke manual abriu Saúde → Passos com dados locais, sem iniciar BLE, refresh,
  Health Connect ou provider.
- Tela exibiu `7D`, `30D` e `90D` em uma linha; lista exibiu data, valor, origem,
  última atualização e **Meta atual**, ordenada de 28/07 para datas anteriores.
- Janela de 90 dias exibiu `Página 1 de 9`, com 10 itens por página e controles
  de navegação disponíveis após a lista.
- Com dados locais disponíveis, gráfico mostrou um dia registrado e seis
  lacunas visualmente distintas; métricas mostraram total, média, máximo, dias
  registrados e dias na meta.
- Revisão independente final aprovou implementação, com ressalva documental
  resolvida neste registro.
- `git diff --check` concluiu sem erros.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
