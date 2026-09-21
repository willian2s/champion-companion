# 035-07 — Mover carga cardiovascular para página própria

- **Ticker:** `035`
- **Número:** `07`
- **Status atual:** `completed`

## Motivo do escalonamento

As subtarefas `035-05` e `035-06` entregaram e validaram a carga cardiovascular
como seção inline na página de Exercícios. O usuário escolheu a opção 1 para a
apresentação: retirar essa seção do fluxo inline e abrir o histórico completo em
uma página própria por CTA na página de Exercícios.

O escalonamento é de apresentação e navegação. Não há correção pendente na
policy, no read model ou nos fechamentos numéricos validados em `035-06`.

## Objetivo e resultado esperado

Substituir a seção inline **Carga cardiovascular** por um CTA acessível na
página de Exercícios e disponibilizar o mesmo conteúdo em destino contextual
próprio, usando o back stack manual e os padrões de navegação já existentes.

O resultado deve preservar integralmente o read model, a
`CardiovascularLoadHistoryPolicy`, os estados tipados, a seleção Semana/Mês e a
lista de atividades. A mudança não deve criar entrada em Home ou Saúde.

## Requisitos cobertos

- Apresentação e navegação da carga cardiovascular após `035-05`/`035-06`.
- CTA acessível e identificável na página de Exercícios.
- Destino próprio, contextual e serializável conforme o padrão existente.
- Retorno contextual para Exercícios pelo back stack manual.
- Reuso do estado e do read model já compostos pelo `ExercisesViewModel`.
- Preservação dos estados `Loading`, `Ready`, `AVAILABLE`,
  `CONFIGURATION_REQUIRED`, `NO_WORKOUTS`, `NO_ANALYZABLE_WORKOUTS` e
  `UNAVAILABLE`.

## Escopo incluído

- Remover a renderização inline da seção de carga cardiovascular da composição
  principal de `ExercisesScreen`.
- Adicionar CTA explícito, acessível e com alvo de toque compatível com os
  padrões existentes de Exercícios, preferencialmente rotulado como
  `Ver histórico de carga cardiovascular`.
- Criar destino contextual próprio para o histórico de carga cardiovascular.
- Integrar destino em `AppDestination` e `ChampionNavigation`, preservando
  serialização, animação, retorno contextual e guards de saída existentes.
- Extrair ou reutilizar os composables da seção em uma página própria sem mover
  cálculo para Compose.
- Manter a página própria ligada ao mesmo `ExercisesViewModel`/estado, sem
  segunda leitura bounded, segundo read model ou recomposição concorrente.
- Manter filtros, navegação Semana/Mês, lista, detalhe de atividade e operação
  de importação na superfície de Exercícios sem mudança semântica.
- Registrar screenshot sanitizada e evidência de navegação, se a execução
  futura exigir validação visual.

## Escopo excluído

- Alteração de `CardiovascularLoadHistoryPolicy`,
  `WorkoutHeartRateAnalysisPolicy` ou contratos de domínio/read model.
- Alteração de repository, DAO, Room, migration, schema, entidades, snapshots,
  workflow, parser, BLE ou Health Connect.
- Nova fórmula, nova janela, novo filtro analítico, novo estado ou nova
  persistência de carga.
- CTA, card, seção ou destino em Home, Saúde, Perfil ou Configurações.
- Nova aba principal, nova barra de navegação ou tela global independente de
  Exercícios.
- Mudança no detalhe individual da Spec 034.
- Criação, alteração ou execução de testes automatizados novos, conforme
  `AGENTS.md`.

## Dependências

- `035-05` — renderização inline, estados, semantics e conteúdo histórico
  concluídos.
- `035-06` — validação física, screenshot e preservação dos efeitos colaterais
  concluídas.
- `AppDestination` e `ChampionNavigation`, que usam back stack manual com
  destinos contextuais.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/AppDestination.kt` — destino próprio contextual.
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt` — roteamento, retorno e classificação da subtree de Exercícios.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt` — CTA, remoção da seção inline e callback de navegação.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/CardiovascularLoadHistoryScreen.kt` — página própria e consumo do estado existente, caso a extração seja necessária.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt` — somente se o padrão de estado exigir adaptador de apresentação; não alterar contrato do read model.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt` — somente para evitar carga duplicada ou ajustar ownership de emissão; não alterar policy/composição.
- `docs/integration/035-07-cardiovascular-load-navigation.png` — screenshot sanitizada, se produzida na validação.

## Passos de implementação

1. Auditar a composição atual e confirmar que a seção inline de `035-05` é o
   único conteúdo a mover; preservar o diff válido anterior.
2. Definir destino sem parâmetro ou outro formato mínimo compatível com os
   destinos contextuais existentes, sem transformá-lo em item da navegação
   principal.
3. Ligar CTA da página Exercícios ao destino e atualizar somente as funções de
   back stack necessárias (`contextualBackLabel`, classificação e saver quando
   aplicável).
4. Extrair a seção para a página própria, mantendo título, conteúdo, estados,
   semantics, gráfico, comparações, cobertura, contadores e contribuição por
   tipo.
5. Remover a chamada inline e confirmar que não há conteúdo duplicado nem
   chamada adicional à fonte bounded.
6. Validar retorno para Exercícios, troca de área primária, restauração do
   back stack e guards de saída sem reativar fluxos de treino phone.
7. Comparar a saída da página própria com a evidência de `035-06` e registrar
   qualquer cenário naturalmente indisponível como `N/A`, sem fabricar dados.

## Critérios de aceite

- Exercícios apresenta CTA explícito para abrir o histórico de carga
  cardiovascular; CTA é alcançável por acessibilidade e possui descrição
  inequívoca.
- A seção detalhada não é mais renderizada inline no fluxo principal de
  Exercícios.
- Acionar CTA abre página própria em destino contextual existente no back
  stack manual, sem nova aba principal ou entrada em Home/Saúde.
- A página própria mostra o mesmo read model e os mesmos estados validados
  em `035-05`/`035-06`, sem cálculo local novo ou zero sintético.
- Voltar retorna para Exercícios com label contextual correto; troca de
  área e restauração não deixam destino órfão nem quebram guards de saída.
- Semana/Mês, filtros, lista, detalhe de atividade e importação continuam
  com a semântica vigente; filtro da lista não contamina a população analítica.
- `CardiovascularLoadHistoryPolicy`, repository bounded, contratos de
  dados, Room v20, fatos de workout, snapshots e operações Health Connect não
  sofrem alteração funcional.
- Não existe CTA ou destino equivalente em Home, Saúde, Perfil ou
  Configurações.
- Screenshot/evidência sanitizada, quando produzida, mostra CTA, página
  própria e retorno sem expor dados pessoais, BPM individual, samples ou IDs.

## Evidências do planejamento

- `035-05` registra a seção inline e seus estados; `035-06` registra a
  validação física, a screenshot e a preservação do read model/policy.
- A composição atual de `ExercisesScreen` chama a seção inline no fluxo da
  página, enquanto `AppDestination` e `ChampionNavigation` já fornecem destinos
  contextuais, back stack manual, labels de retorno e classificação da subtree
  de Exercícios.
- A execução concluiu a implementação e a validação física; resultados e
  limitações estão registrados nas seções abaixo.

## Testes e validação

Não criar testes automatizados novos. Validar a implementação futura por:

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- `./gradlew installDebug`, quando houver validação física;
- inspeção de que `ExercisesScreen` não renderiza a seção inline e de que a
  página própria não acessa DAO nem cria fluxo bounded adicional;
- smoke manual: abrir Exercícios, alcançar CTA por navegação acessível, abrir a
  página, usar voltar, trocar para Home/Saúde e retornar a Exercícios;
- smoke dos estados disponíveis, parciais, configuração, vazio, sem análise e
  falha quando os dados existentes permitirem; registrar `N/A` quando não
  surgirem naturalmente;
- comparação visual e semântica com os resultados/screenshot de `035-06`;
- confirmação de que alterar a FC máxima continua recompondo somente o read
  model compartilhado e de que nenhuma tabela, operação remota ou fato de
  workout muda.

## Definição de pronto

- CTA, destino contextual e retorno foram implementados e revisados.
- Conteúdo histórico existe somente na página própria, com acessibilidade e
  estados anteriores preservados.
- Read model e policy continuam únicos, bounded e não persistidos.
- Home, Saúde e navegação principal permanecem sem expansão.
- Validações técnicas e físicas possuem evidência ou `N/A` justificado.
- Overview, Spec 035 e ADR-035 refletem a decisão sem marcar a subtarefa como
  concluída antes da execução.

## Riscos e cuidados

- Classificar destino como primário pode alterar barra, subtree ou retorno;
  mantê-lo contextual a Exercícios.
- Remover a seção antes de ligar o CTA pode ocultar a métrica; validar origem e
  destino no mesmo fluxo.
- Reutilizar `ExercisesViewModel` sem duplicar `Load`, consulta bounded ou
  recomposição.
- Extrair composables pode perder semantics, estados de ausência, comparação
  neutra ou distinção entre dia sem treino e treino sem análise.
- Navegação pode criar destino órfão em restauração ou back; revisar saver,
  `isExercisesDestination`, label de retorno e guards existentes.
- Mudança visual pode introduzir captura com dados sensíveis; sanitizar toda
  evidência e não registrar BPM, samples, IDs ou payloads.
- Não usar a decisão como justificativa para expandir Exercícios em treino
  iniciado pelo celular, GPS, rota ou BLE live.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/AppDestination.kt` — destino contextual serializável.
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt` — roteamento, back stack, saver e subtree de Exercícios.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt` — CTA, remoção da seção inline e reutilização do conteúdo.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/CardiovascularLoadHistoryScreen.kt` — página própria com cabeçalho e retorno contextual.
- `docs/integration/035-07-cardiovascular-load-navigation.png` — screenshot sanitizada.
- Este arquivo — status, decisões, comandos, evidências e riscos residuais.
- `docs/tasks/035-training-load-history-trends/035-00-overview.md` — checklist, progresso e observações.
- `docs/specs/035-training-load-history-trends.md` e `docs/decisions/adr-035-training-load-history-trends.md` — apresentação e navegação finais.

## Decisões e desvios

- Decisão: adicionar `AppDestination.CardiovascularLoadHistory` como destino
  contextual da subtree de Exercícios, sem nova aba primária.
- Decisão: manter `ExercisesViewModel` activity-scoped como única fonte do
  estado; a página própria recebe `cardiovascularLoadHistory` já composto.
- Decisão: reutilizar `CardiovascularLoadHistorySection` e seus estados,
  semantics, gráfico, comparações, cobertura e tipos; nenhum cálculo foi movido
  para Compose nem duplicado.
- Decisão: CTA usa rótulo e descrição acessíveis `Ver histórico de carga
  cardiovascular`; retorno usa o label contextual `Voltar para Exercícios`.
- Desvio: nenhum.

## Comandos executados

- `git diff --check`
- `./gradlew app:compileDebugKotlin`
- `./gradlew lint assembleDebug assembleRelease`
- `./gradlew app:compileDebugKotlin installDebug`
- `adb shell uiautomator dump /sdcard/window.xml`
- smoke automatizado por bounds/UIAutomator: Home → Exercícios → CTA → página
  própria → voltar → Exercícios
- navegação física por CTA, página própria, voltar, Home e retorno a Exercícios
- `adb exec-out screencap -p`

## Resultados e evidências

- Primeira compilação encontrou anotação `@Composable` duplicada na extração;
  correção removida na segunda tentativa, sem alteração de comportamento.
- `./gradlew app:compileDebugKotlin`: `BUILD SUCCESSFUL` após correção.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew app:compileDebugKotlin installDebug`: `BUILD SUCCESSFUL`, instalado
  no `SM-S901E`.
- Exercícios exibiu CTA com descrição `Ver histórico de carga cardiovascular`;
  acionamento abriu destino próprio com `505,6 pontos`, `8 de 8` e cobertura
  `97,7%`, preservando o read model validado em `035-06`.
- Cabeçalho exibiu `Voltar para Exercícios`; voltar retornou à tela Exercícios.
  Troca para Home e retorno a Exercícios não deixou destino órfão.
- Revalidação final após instalação encontrou CTA por `content-desc`, abriu a
  página com `505,6 pontos`, confirmou o label de retorno e voltou à CTA em
  Exercícios.
- UI dump confirmou destino contextual fora da barra primária e CTA presente
  somente em Exercícios.
- Screenshot sanitizada: `docs/integration/035-07-cardiovascular-load-navigation.png`.
- Screenshot sanitizada do CTA: `docs/integration/035-07-exercises-cta.png`.
- Não foram criados nem executados testes automatizados novos, conforme escopo
  da Spec 035 e `AGENTS.md`.

## Riscos residuais

- Estados de ausência não surgiram naturalmente durante smoke físico; seguem
  cobertos pelo mesmo composable e contratos tipados validados em `035-05`/`035-06`.
- Não houve medição nova de custo bounded; implementação não adicionou leitura,
  DAO, repository ou recomposição concorrente.
- Restauração de back stack foi exercitada em navegação manual, não em morte de
  processo com destino contextual persistido; saver recebeu token explícito e
  parser correspondente.
