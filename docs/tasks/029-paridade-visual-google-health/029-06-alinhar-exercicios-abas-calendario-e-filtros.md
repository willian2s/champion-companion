# 029-06 — Alinhar exercícios, abas, calendário e filtros

- **Ticker:** `029`
- **Número:** `06`
- **Status:** `pending`
- **Depende de:** `029-02`, `029-03`, `029-05` e `028-10`

## Objetivo e resultado esperado

Reorganizar a superfície de exercícios conforme o baseline: somente abas
`Semana` e `Mês`, calendário de atividades dentro de `Mês` e filtros abaixo do
gráfico ou calendário, preservando workflow, lista, detalhe e ações explícitas.

## Requisitos e critérios de aceite cobertos

- R2, R4, R6, R7 e R8 da Spec 029.
- Critérios 2, 4, 6, 7, 9, 10 e 11.
- Cada um dos sete filtros é selecionável e possui evidência ou `N/A` justificado.

## Escopo incluído

- abas de período `Semana` e `Mês`, sem aba diária;
- gráfico/resumo semanal usando dados existentes;
- calendário de atividades na aba mensal;
- posicionamento dos filtros abaixo do conteúdo visual;
- filtro `ALL`, pendências, prontas, sincronizadas, erro e descartadas;
- estado vazio por filtro, paginação/lista, cards e abertura de detalhe;
- preservação de revisão, calibração, descarte, exclusão e sync explícitos;
- screenshots sanitizados de cada aba e filtro.

## Escopo excluído

- iniciar treino phone, GPS, rota, localização ou BLE live;
- mudar estados ou semântica de workflow;
- persistir aba/filtro ou criar migração;
- ler Health Connect para preencher gráfico;
- criar funcionalidade Google inexistente no Champion sem spec própria.

## Dependências

- matriz de referência 029-01 e decisão de 029-02;
- componentes 029-03;
- calendário 029-05;
- `028-10` concluída se a alteração alcançar gráficos;
- `WorkoutActivityFilter`, `WorkoutWorkflowSummary`,
  `WorkoutActivityRepository` e `ExercisesViewModel`.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesScreen.kt`;
- `feature/exercises/ExercisesFeatureContract.kt`;
- `feature/exercises/ExercisesViewModel.kt`;
- `feature/exercises/WorkoutShells.kt`;
- `feature/calendar/CalendarScreen.kt`, se componente for compartilhado;
- `core/model/RepositoryContracts.kt` e `CalendarHistory.kt`, somente para
  estado de apresentação ou mapper já suportado.

## Passos de implementação

1. Confirmar no baseline a ordem exata de tabs, conteúdo visual e posição do
   filtro em Semana e Mês.
2. Modelar período como estado de apresentação local, sem persistência.
3. Implementar Semana com dados locais disponíveis e Mês com calendário de
   atividades existente.
4. Mover visualmente o filtro para abaixo do gráfico/calendário sem alterar
   enum, contagens, query ou workflow.
5. Exercitar os sete filtros, inclusive zero resultados e paginação.
6. Abrir cards de exercícios e validar detalhe, retorno e ações explícitas.
7. Registrar evidências sanitizadas e qualquer estado não alcançado.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Smoke físico: abas Semana/Mês, calendário mensal, todos os filtros, lista vazia
e preenchida, abertura de cards, detalhe, retorno, rolagem, tema, orientação e
fonte ampliada. Não criar testes automatizados.

## Definição de pronto

- somente Semana e Mês ficam disponíveis;
- Mês contém calendário de atividades;
- filtros aparecem abaixo do gráfico/calendário;
- sete filtros mantêm contagens, queries, estados e ações atuais;
- dados e sync não mudam;
- build, lint, acessibilidade, smoke e evidências sanitizadas passam.

## Riscos e cuidados

- “Somente Semana e Mês” vale para esta superfície, não autoriza remover
  períodos de histórico de outras métricas.
- Não duplicar calendário nem criar fonte divergente de atividade.
- Filtro abaixo do conteúdo pode ficar fora da primeira dobra; validar foco e
  rolagem acessível.
- Não fabricar atividade para preencher mês, semana ou filtro vazio.
