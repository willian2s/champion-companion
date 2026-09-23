# 036-04 — Integrar zonas na página cardiovascular

- **Ticker:** `036`
- **Número:** `04`
- **Status:** `completed`

## Objetivo e resultado esperado

Adicionar à página contextual existente os resumos de distribuição por zona para
7 e 28 dias, preservando o histórico de carga da 035, estados seguros,
acessibilidade e a navegação atual.

## Requisitos cobertos

- R4, R5, R6, R8 e R9 da Spec 036.
- Labels, duração, percentual, cobertura, contadores e `Abaixo de Z1`.
- Seleção de período somente na apresentação, se escolhida.

## Escopo incluído

- Evolução de `CardiovascularLoadHistorySection` e/ou
  `CardiovascularLoadHistoryScreen`.
- Resumo selecionável de current7/current28 com rows das seis zonas.
- Exibição de duração coberta, coverage ratio, treinos analisados e parcialidade.
- Reutilização da ordem, labels e cores da seção individual da Spec 034.
- Estados de configuração, nenhum treino, nenhum treino analisável,
  indisponibilidade e parcialidade.
- Semantics/content descriptions para resumo e cada zona.

## Escopo excluído

- Alteração de domínio, policy, bounded read ou ViewModel além do necessário
  para consumir o resultado já composto.
- Gráfico temporal detalhado, pertencente à `036-05`.
- Nova aba, destino, entrada Home/Saúde ou alteração do detalhe individual.
- Textos que avaliem qualidade, risco, recuperação ou recomendação.
- Persistência, Health Connect e testes automatizados novos.

## Dependências

- `036-02` e `036-03` concluídas.
- `CardiovascularLoadHistoryScreen` contextual da 035.
- Helpers de apresentação e cores da Spec 034.

## Arquivos e símbolos prováveis

- `feature/exercises/CardiovascularLoadHistoryScreen.kt`.
- `feature/exercises/ExercisesScreen.kt`:
  `CardiovascularLoadHistorySection`, `CardiovascularLoadHistoryContent`,
  `HeartRateZoneRow` e labels de zona.
- eventual helper de apresentação em `feature/exercises`.

## Passos de implementação

1. Auditar composição final da página e manter os cards/carga da 035.
2. Adicionar seleção local `7 dias`/`28 dias` se reduzir repetição, com estado
   `rememberSaveable` e sem nova consulta.
3. Renderizar seis zones do resumo escolhido, inclusive zeros e
   `Abaixo de Z1`.
4. Mostrar percentual do tempo analisado, não do workout total.
5. Mostrar coverage ratio, duração coberta e `X de Y treinos analisados`.
6. Exibir `partial` quando houver workout sem análise ou cobertura parcial.
7. Preservar estados sem zeros sintéticos e linguagem factual.
8. Reutilizar cores/labels da 034 e garantir descrição acessível independente de
   cor.

## Testes e comandos de validação

- Inspeção visual dos estados disponíveis, parciais e vazios quando naturais.
- Verificar labels e semantics de todas as seis zonas.
- Verificar que filtros/lista não alteram histórico.
- `./gradlew lint assembleDebug assembleRelease` após integração.
- Não criar testes automatizados novos.

## Definição de pronto

- Página cardiovascular mostra 7/28 e seis zonas factuais.
- `Abaixo de Z1`, cobertura e contadores são visíveis.
- Estados e parcialidade são distinguíveis.
- Carga 035, navegação e acessibilidade permanecem funcionais.
- Nenhum cálculo de zona foi movido para Compose.

## Riscos e cuidados

- Não usar `summary.zones.orEmpty()` para mascarar ausência.
- Não usar cor para comunicar semântica de esforço.
- Não remover ou duplicar o conteúdo de carga cardiovascular existente.
- Sanitizar qualquer screenshot futura.

## Execução e evidências

### Implementação realizada

- A página contextual passou a selecionar localmente `current7` ou `current28`
  com `rememberSaveable`, sem alterar consulta, policy ou ViewModel.
- O card selecionado preserva carga, comparação, contadores, cobertura, duração
  coberta e parcialidade da Spec 035.
- Resumos disponíveis exibem as seis zonas na ordem visual da Spec 034,
  incluindo `Abaixo de Z1`, durações, percentuais do tempo analisado e barras
  com as cores já existentes.
- Ausência tipada de `zones` não é convertida em zeros; estados de configuração,
  vazio, ausência de análise e indisponibilidade continuam explícitos.
- Semantics do resumo e de cada zona informam labels, duração, percentual,
  cobertura, contadores e parcialidade de forma independente de cor.
- A carga diária, contribuição por tipo, navegação e distribuição temporal da
  subtarefa `036-05` permaneceram fora da alteração funcional.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `docs/tasks/036-heart-rate-zone-history/036-04-integrar-zonas-na-pagina-cardiovascular.md`
- `docs/tasks/036-heart-rate-zone-history/036-00-overview.md`

### Decisões e desvios

- Decisão: exibir um resumo por vez com seleção local de `7 dias`/`28 dias`,
  reduzindo duplicação visual e mantendo ambos os períodos disponíveis.
- Decisão: reutilizar ordem, labels, helper de rows e associação de cores da
  Spec 034; o contexto acessível do percentual foi ajustado para “do tempo
  analisado” no histórico.
- Nenhum desvio de requisito. Não houve cálculo de zonas, percentual ou
  cobertura em Compose; a UI somente consome o read model composto.
- Não foram criados testes automatizados, conforme escopo e `AGENTS.md`.

### Comandos executados e resultados

- `./gradlew app:compileDebugKotlin` — passou (`BUILD SUCCESSFUL`).
- `./gradlew lint assembleDebug assembleRelease` — passou (`BUILD SUCCESSFUL`).
- `git diff --check` — passou.
- Revisão independente do agente `review` — aprovado, sem bloqueadores.
- Inspeção estática de seleção, estados, semantics, rows, ordem visual e
  ausência de alteração em policy/read model/ViewModel — sem divergências.

### Riscos residuais

- Inspeção visual em dispositivo e screenshot sanitizada permanecem para
  `036-06`; nenhum dado foi fabricado para cobrir estados não existentes.
- Cenários naturais de carga zero, workout sem análise e cobertura parcial não
  foram fabricados; permanecem sujeitos à validação física da `036-06`.
- O resumo inicia em `7 dias`; conferência visual de rolagem e acessibilidade
  após alternar para `28 dias` permanece pendente da validação física.

## Revisão independente

- Agente `review` aprovou a integração para `036-04`, sem findings bloqueadores.
- Revisão confirmou R4/R5/R6/R8/R9, preservação da carga 035, seleção
  `rememberSaveable`, ausência de cálculo analítico em Compose e escopo da
  distribuição diária reservado à `036-05`.
