# 036-04 — Integrar zonas na página cardiovascular

- **Ticker:** `036`
- **Número:** `04`
- **Status inicial:** `pending`

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
