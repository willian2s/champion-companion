# 036-05 — Renderizar distribuição temporal

- **Ticker:** `036`
- **Número:** `05`
- **Status:** `completed`

## Objetivo e resultado esperado

Renderizar a distribuição diária de zonas dos últimos 28 dias usando a
infraestrutura gráfica suportada, sem simular stacked bars grouped e sem criar
uma abstração gráfica ampla.

## Requisitos cobertos

- R7, R8, R9 e R10 da Spec 036.
- Visual temporal de current28 e distinção de dias sem treino, sem análise e
  parciais.
- Acessibilidade da visualização.

## Escopo incluído

- Visual diário de `CardiovascularLoadDailyBucket` em
  `CardiovascularLoadHistoryScreen`/componente compartilhado.
- Uma série neutra por vez no `ChampionBarChart`, com seleção entre as seis
  zonas; a identidade vem de label, semantics e rows coloridas da Spec 034.
- Pontos em ordem civil, normalmente 28 e, conforme semântica 035, podendo
  incluir datas locais adicionais tocadas por `asOf`/DST; duração da zona no
  eixo Y e labels de zona/período.
- Ponto zero real para dia sem workout.
- Ponto nulo/ausente para workout sem análise, sem converter ausência em zero.
- Semantics agregadas com duração, estado do dia e parcialidade.
- Preservação do gráfico diário de carga existente da 035.

## Escopo excluído

- Alteração de `ChampionBarChart`, Vico ou contrato gráfico compartilhado para
  stacked bars.
- Seis séries grouped no mesmo gráfico.
- Novo design system de cores ou legenda baseada apenas em cor; a série temporal
  usa cor neutra e rows reutilizam cores 034.
- Cálculo de buckets, zona ou percentuais em Compose.
- Persistência, Health Connect e testes automatizados novos.

## Dependências

- `036-02` buckets com zonas concluídos.
- `036-04` rows/resumo da página concluídos.
- Auditoria de `ChampionBarChart` e decisão ADR-036.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesScreen.kt` ou componente extraído de distribuição
  temporal.
- `feature/exercises/CardiovascularLoadHistoryScreen.kt` se seleção ficar na
  página.
- `core/designsystem/chart/ChampionChartContracts.kt` e `ChampionCharts.kt`
  somente para consumo, não alteração.

## Passos de implementação

1. Exibir título factual de distribuição diária dos últimos 28 dias.
2. Criar seleção acessível das seis zonas, sem valor padrão interpretativo de
   intensidade.
3. Construir uma única `ChampionChartSeries` para a zona selecionada.
4. Mapear cada bucket: zero para dia sem workout, valor da zona para análise e
   ausência para workout sem análise.
5. Criar descrições por ponto distinguindo sem treino, sem análise e parcial.
6. Exibir legenda/label textual da zona selecionada e manter rows agregadas.
7. Confirmar que `ChampionChartFrame` não transforma null em zero.
8. Comparar fechamento visual com current28 e manter carga diária 035.

## Testes e comandos de validação

- Inspeção de gráfico com dados disponíveis, zeros e gaps naturais.
- Verificar `contentDescription`, estado selecionado e labels de zona.
- Confirmar ausência de `MergeMode.Grouped` usado como falso stacking.
- `./gradlew lint assembleDebug assembleRelease` após implementação.
- Não criar testes automatizados novos.

## Definição de pronto

- Distribuição temporal permite consultar todas as zonas sem gráfico enganoso.
- Dias sem treino e workout sem análise são semanticamente distintos.
- Renderer compartilhado não foi expandido desnecessariamente.
- Visual funciona na página contextual e não cria destino novo.

## Riscos e cuidados

- Não chamar zona de “ideal”, “excesso”, “insuficiente” ou equivalente.
- Não esconder gaps de análise convertendo `null` em zero.
- Não usar a seleção de zona como filtro de domínio ou nova consulta.
- Não alterar cores existentes nem expor dados sensíveis em descriptions.

## Execução e evidências

### Implementação realizada

- `CardiovascularLoadHistoryContent` mantém seleção local da zona, iniciando em
  `BELOW_ZONE_1`, sem alterar consulta, policy ou ViewModel.
- A seção diária preserva o gráfico de carga da 035 e acrescenta seleção
  acessível das seis zonas, com label textual da zona selecionada.
- A nova visualização cria uma única `ChampionChartSeries` por vez no
  `ChampionBarChart`, usando duração em segundos no eixo Y e labels civis no
  eixo X.
- Dia sem workout produz `y = 0.0`; workout sem análise produz `y = null`;
  duração analisada usa a entrada da zona selecionada sem recalcular domínio.
- Descrições dos pontos distinguem sem treino, treino sem análise, duração e
  dados parciais. O renderer compartilhado continua responsável por preservar
  gaps nulos e não foi alterado.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `docs/tasks/036-heart-rate-zone-history/036-05-renderizar-distribuicao-temporal.md`
- `docs/tasks/036-heart-rate-zone-history/036-00-overview.md`

### Decisões e desvios

- Decisão: reutilizar `ChampionBarChart` com uma série neutra por vez, conforme
  ADR-036; não simular stacking nem alterar contratos gráficos.
- Decisão: usar `BELOW_ZONE_1` como seleção inicial factual, sem interpretar
  intensidade ou recomendar comportamento.
- Decisão: manter o gráfico de carga diária existente e renderizar a
  distribuição de zona como visualização adicional independente.
- Nenhum desvio de requisito. Não houve cálculo de zona, percentual, bucket ou
  consulta em Compose; a UI apenas consome `CardiovascularLoadDailyBucket`.
- Não foram criados testes automatizados, conforme escopo e `AGENTS.md`.

### Comandos executados e resultados

- `./gradlew app:compileDebugKotlin` — passou (`BUILD SUCCESSFUL`).
- `./gradlew lint assembleDebug assembleRelease` — passou (`BUILD SUCCESSFUL`).
- `git diff --check` — passou.
- Revisão independente do agente `review` — aprovada, sem findings materiais.
- Inspeção estática confirmou uma série por seleção, seis chips com semantics,
  zero real para dia sem treino, `null` para ausência de análise e preservação
  do gráfico de carga 035.

### Riscos residuais

- Validação visual em dispositivo e TalkBack, incluindo cenários naturais de
  workout sem análise e cobertura parcial, permanece para `036-06`.
- Nenhum treino, sample ou dado sensível foi fabricado; cenários não existentes
  devem ser registrados como `N/A` no fechamento físico.
