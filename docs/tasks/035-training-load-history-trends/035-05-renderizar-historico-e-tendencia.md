# 035-05 — Renderizar histórico e tendência

- **Ticker:** `035`
- **Número:** `05`
- **Status:** `pending`

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
