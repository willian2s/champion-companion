# 031-06 — Redesenhar Saúde e métricas

- **Ticker:** `031`
- **Número:** `06`
- **Status:** pending

## Objetivo

Reorganizar hub Saúde, detalhes de frequência cardíaca, peso, passos e distância,
incluindo gráficos e filtros conforme referência e dados locais.

## Requisitos e aceite

- Quatro métricas seguem hierarquia, cards, filtros e detalhes da referência.
- Medição manual e importação D1 de BPM continuam explícitas.
- Gráficos preservam lacunas, unidades, períodos, origem e acessibilidade.
- Distância continua estimada quando essa for sua origem.
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

Smoke ADB sanitizado em todos os detalhes, períodos, gráficos, fonte ampliada,
landscape e temas. Não criar ou executar testes automatizados.

## Definição de pronto

Hub e quatro detalhes aprovados visualmente; dados reais, filtros, gaps, estados
e callbacks permanecem equivalentes; evidência sanitizada, estado
inicial/restaurado do aparelho e revisão independente aprovados.

## Riscos

Visual denso pode incentivar valores sintéticos ou ocultar lacunas. Rejeitar
qualquer composição que não exponha unidade, período e ausência corretamente.
