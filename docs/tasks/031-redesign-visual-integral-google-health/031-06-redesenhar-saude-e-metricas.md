# 031-06 — Redesenhar Saúde e métricas

- **Ticker:** `031`
- **Número:** `06`
- **Status:** in_progress

## Objetivo

Reorganizar hub Saúde, detalhes de frequência cardíaca, peso, passos e distância,
incluindo gráficos, filtros e listas conforme referência e dados locais.

## Requisitos e aceite

- O hub usa lista colorida com card Atividades no topo, seguido por Frequência
  cardíaca, Peso, Passos e Distância; todos os cards mantêm mesma estrutura,
  acessibilidade e ação de abertura.
- Medição manual e importação D1 de BPM continuam explícitas.
- Gráficos preservam lacunas, unidades, períodos, origem e acessibilidade.
- Distância continua estimada quando essa for sua origem. Na tela de distância,
  `Semana` mostra domingo–sábado no gráfico com `D S T Q Q S S` e lista diária;
  `M` mostra dias no gráfico com labels `1, 5, 10, 15, 20, 25` e último dia,
  e lista semanal dentro do mês; `3M` e `A` mostram meses fechados no gráfico
  e na lista. Gráfico mantém colunas vazias; lista mostra somente valores
  válidos.
- Health Connect não vira fonte visual nem é duplicado sem necessidade.
- Registrar nomes dos componentes compartilhados usados também em `031-05`;
  promoção exige uso real nas duas superfícies, nunca apenas preview.

## Escopo

Inclui composição visual, filtros, headers, listas e uso da infraestrutura de
gráficos. Exclui novas métricas, leitura Health Connect, fórmulas e BLE.

## Dependências

- `031-03`, `031-04`, `031-05`.
- Specs 009, 015, 023, 028 e ADR-009.

## Arquivos e símbolos prováveis

- `feature/health/HealthScreen.kt`.
- `HealthMetricDetailScreens.kt`, `StepsHistoryShell.kt`,
  `DistanceHistoryShell.kt`.
- `HeartRateHistoryChart.kt`, `StepsHistoryChart.kt`,
  `DistanceHistoryChart.kt` e contratos/ViewModels.

## Passos

1. Criar matriz de métricas disponíveis e blocos equivalentes.
2. Reordenar hub/detalhes e adaptar filtros sem mudar intents.
3. Ajustar gráficos via `core/designsystem/chart`, sem reamostrar dados.
4. Validar estados vazios, períodos, retorno e ações explícitas.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado confirma hub, header, cards coloridos, card Atividades no
topo, quatro períodos da distância, buckets equivalentes entre gráfico/lista e
ausência de rolagem horizontal nos gráficos de Saúde. Não criar ou executar
testes automatizados.

## Definição de pronto

Hub e quatro detalhes aprovados visualmente; dados reais, filtros, gaps, estados
e callbacks permanecem equivalentes; evidência sanitizada, estado
inicial/restaurado do aparelho e revisão independente aprovados.

## Riscos

Visual denso pode incentivar valores sintéticos ou ocultar lacunas. Rejeitar
qualquer composição que não exponha unidade, período e ausência corretamente.

## Execução atual

  - **Arquivos alterados:** `HealthScreen.kt`, `HealthFeatureContract.kt`,
  `HealthViewModel.kt`, `HeartRateHistoryChart.kt`, `CalendarHistory.kt`,
   `StepsHistoryChart.kt`, `DistanceHistoryChart.kt`, `DistanceHistoryViewModel.kt`,
   `HealthMetricDetailScreens.kt`, `StepsHistoryShell.kt`, `DistanceHistoryShell.kt`,
   `ChampionCharts.kt` e
  `ChampionNavigation.kt`.
- **Decisões:** após solicitação explícita, o hub voltou ao estilo lista da
  implementação anterior, mantendo `ChampionRedesignSurfaceHeader`, ações de
  Dispositivo/Perfil, card de Atividades no topo seguido pelas quatro métricas
  locais, com card igual aos
  demais cards, com a paleta colorida restaurada; o card fica no topo da lista;
  o calendário não é exposto neste hub e permanece dentro de Exercícios; FC intraday usa início do dia como origem temporal, bandas e
  pontos estilo referência; detalhes preservam seus gráficos, filtros, lacunas
  e rolagem horizontal desativada. Distância usa buckets derivados dos mesmos
   dados para gráfico e lista: semana civil atual com sete entradas diárias,
   entradas diárias e semanas civis recortadas no mês fechado em `M`, e meses
   fechados em `3M` e `A`. Intervalos semanais respeitam domingo–sábado e os
   limites do mês, como `30/08 - 31/08`; o gráfico anual usa somente a inicial
   maiúscula de cada mês.
- **Desvios:** nenhum contrato de meta semanal foi criado. Unidade de Passos não
  é repetida no valor do card. O período anual foi adicionado ao contrato comum
  de histórico e permanece oculto no seletor de Passos.
- **Comandos executados:** `git diff --check`; `./gradlew app:compileDebugKotlin
  --no-daemon`; `./gradlew app:lintDebug --no-daemon`; `./gradlew lint assembleDebug
  assembleRelease --no-daemon`; `./gradlew installDebug --no-daemon`.
- **Comandos adicionais executados:** `git diff --check`; `./gradlew
  app:compileDebugKotlin --no-daemon`; `./gradlew lint assembleDebug
  assembleRelease --no-daemon`; `./gradlew installDebug --no-daemon`.
- **Intercorrência:** uma compilação intermediária falhou por referência
  ausente de `onOpenExercises` durante a inclusão do card; assinatura foi
  corrigida e compilação subsequente passou.
 - **Resultados/evidências:** compilação, lint e builds Debug/Release passaram
   após implementação dos períodos. Instalação Debug passou. A implementação
   foi inspecionada no código: Semana civil atual, M diário no gráfico e semanal
   no recorte domingo–sábado, 3M/A mensais, labels de M nos dias requeridos e
    colunas vazias preservadas. A tentativa anterior de smoke foi interrompida
    por atividade externa do aparelho; a execução posterior foi concluída após a
    correção do crash. Dumps e screenshots temporários foram descartados e não
    serão versionados. Não foram criados nem executados testes automatizados,
    conforme regra do projeto.
  - **Riscos residuais:** revisão independente pendente. Imagem de referência não
    deve entrar no versionamento.
  - **Correção pós-smoke:** o fechamento da tela foi diagnosticado via
   `dumpsys dropbox` como `IllegalStateException` do Vico por formatter de eixo
   retornando string vazia. O renderer agora aceita `labelValues` e usa
    `ItemPlacer` explícito; Semana e 3M passaram a fixar labels nos mesmos X das
    colunas, o chip semanal foi reduzido para `S` e os labels dos filtros foram
    centralizados.
 - **Atualização de intervalo civil:** `Semana` consulta a semana civil atual,
   de domingo a sábado; `M` consulta o mês calendário fechado anterior; `3M`
   consulta os três meses calendário fechados anteriores; `A` consulta doze
   meses calendário fechados anteriores. O gráfico mensal permanece diário com
    labels espaçados; a lista mensal usa semanas civis recortadas no mês; 3M usa
    meses abreviados em maiúsculas e A usa iniciais mensais.
 - **Validação adicional:** `./gradlew app:compileDebugKotlin --no-daemon`,
   `./gradlew lint assembleDebug assembleRelease --no-daemon` e `git diff --check`
   passaram após o ajuste de intervalo civil e correção do renderer. `./gradlew
   installDebug --no-daemon` passou. Smoke ADB confirmou abertura de Distância
   sem crash, labels `D S T Q Q S S`, chip `S`, e alinhamento visual entre
    colunas/eixos em Semana e 3M, iniciais mensais no gráfico A sem quebra de
    texto e labels dos filtros centralizados. Nenhum teste automatizado foi
    criado ou executado.
