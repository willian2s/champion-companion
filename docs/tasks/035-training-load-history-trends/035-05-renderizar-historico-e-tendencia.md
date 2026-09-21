# 035-05 — Renderizar histórico e tendência

- **Ticker:** `035`
- **Número:** `05`
- **Status:** `completed`

## Objetivo e resultado esperado

Adicionar a seção **Carga cardiovascular** à tela histórica existente de
Exercícios, mostrando 7/28 dias, comparações, cobertura, distribuição diária e
tipos, com acessibilidade e estados de ausência seguros.

## Requisitos cobertos

- R5, R6, R7 e R9 da Spec 035.
- Preservação de Semana/Mês, navegação, filtros e lista existentes.
- Linguagem neutra, sem classificação clínica ou recomendação.

## Escopo incluído

- Cards/resumos de current7 e current28.
- Texto de comparação apenas com baseline positivo ou “Sem base anterior”.
- Indicador de cobertura e `analyzedWorkoutCount/totalWorkoutCount`.
- Gráfico/buckets diários de 28 dias.
- Lista ou agrupamento por tipo, incluindo `UNKNOWN`.
- Estados sem FC máxima, sem workouts, sem análise e falha.
- Semantics, headings, content descriptions e contraste compatíveis.

## Escopo excluído

- Nova aba principal, tela global ou Home.
- Nova fórmula, cálculo em composable ou mudança no detalhe individual 034.
- Linguagem de bom/ruim, excesso, fadiga, recuperação, risco,
  overtraining ou recomendação.
- Persistência, Health Connect, parser, BLE ou workflow.
- Screenshot nesta subtarefa; pertence à validação 035-06.

## Dependências

- `035-04` read model integrado.
- Componentes e design system atuais de `ExercisesScreen`.
- Contratos tipados de períodos, buckets e tipos.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesScreen.kt`;
- `ExercisesUiState` e contratos de estado;
- componentes de gráfico/histórico existentes;
- traduções/strings, se o padrão do arquivo exigir recursos.

## Passos de implementação

1. Inserir seção na superfície de histórico sem substituir selector Semana/Mês.
2. Renderizar current7/current28 e comparações neutras.
3. Renderizar cobertura, contadores e `partial` sem transformar ausência em zero.
4. Renderizar 28 buckets; mostrar dia sem treino como carga zero legítima e dia
   com workout sem análise com contadores/estado parcial.
5. Renderizar tipos resolvidos, labels em português e `UNKNOWN` explícito.
6. Implementar estados textuais exigidos e erro seguro.
7. Adicionar headings e descrições acessíveis para carga, cobertura, gráfico e
   contagens.
8. Conferir que filtros/navegação existentes não alteram população do read model.

## Testes e comandos de validação

- Inspeção visual em estados disponível, parcial, configuração, vazio e sem
  análise, quando dados existentes permitirem.
- Verificar semântica acessível e ausência de texto proibido.
- Verificar que baseline zero mostra “Sem base anterior”.
- Verificar que workout sem análise não parece dia sem workout.
- Executar futuramente `./gradlew lint assembleDebug assembleRelease`.
- Não criar testes automatizados novos.

## Definição de pronto

- Nova seção está na tela histórica existente, sem navegação nova.
- Valores e estados vêm do read model, não de cálculo na UI.
- Comparações não classificam tendência como boa/ruim.
- UI acessível e sem zero enganoso.
- Detalhe individual e histórico Semana/Mês continuam preservados.

## Riscos e cuidados

- Não confundir totalLoad zero com ausência de workouts.
- Não esconder `partial` quando houver workouts não analisados.
- Não usar percentual com baseline zero.
- Não expor dados pessoais desnecessários em labels, semantics ou screenshots.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt` — seção de carga cardiovascular, estados, cards, comparações, gráfico diário, tipos e semantics.
- Este arquivo — status, decisões, comandos, evidências e riscos residuais.
- `docs/tasks/035-training-load-history-trends/035-00-overview.md` — checklist, progresso e observação da subtarefa.

## Decisões e desvios

- Decisão: inserir seção antes do histórico existente, sem alterar seleção Semana/Mês, navegação, filtros ou lista.
- Decisão: consumir somente `CardiovascularLoadHistoryUiState` e contratos prontos; nenhum cálculo de carga, janela ou comparação foi movido para Compose.
- Decisão: manter ausência tipada como mensagem de configuração, vazio, falta de análise ou falha, sem substituir por carga zero.
- Decisão: usar `bucket.partial` e contadores para diferenciar dia sem treino de treino sem análise ou cobertura parcial.
- Decisão: exibir `UNKNOWN (tipo indefinido)` explicitamente na contribuição por tipo.
- Desvio: inspeção visual física e screenshot não executadas; pertencem à 035-06 conforme escopo.

## Comandos executados

- `./gradlew app:compileDebugKotlin`
- `./gradlew lint assembleDebug assembleRelease`
- `git diff --check`
- inspeção de diff para termos proibidos de classificação clínica ou recomendação.

## Resultados e evidências

- `./gradlew app:compileDebugKotlin`: `BUILD SUCCESSFUL` após implementação e após correções da revisão.
- `./gradlew lint assembleDebug assembleRelease`: primeira execução excedeu 120 s sem erro reportado; segunda execução terminou com `BUILD SUCCESSFUL`.
- `git diff --check`: passou.
- Inspeção confirma seção na tela histórica existente, read model separado da lista, preservação de Semana/Mês/filtros/navegação, cards current7/current28, comparação neutra, cobertura, contadores, 28 buckets, tipos e estados seguros.
- Revisão independente inicial encontrou quatro ajustes de acessibilidade/estado; correções aplicadas. Revalidação independente aprovou sem blocker, major ou minor.
- Não foram criados nem executados testes automatizados novos, conforme escopo da Spec 035 e `AGENTS.md`.

## Riscos residuais

- Estados reais com workouts disponíveis, ausência de FC máxima, sem samples e falha ainda dependem de inspeção física da 035-06; nenhum fixture foi criado.
- Screenshot sanitizada e inspeção visual permanecem pendentes da 035-06.
- Gráfico usa renderer compartilhado; diferenciação entre dia sem treino e dados parciais é reforçada por descriptions e texto de estado, não por nova codificação visual do renderer.
