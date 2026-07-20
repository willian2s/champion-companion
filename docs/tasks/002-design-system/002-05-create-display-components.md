# Task 002-05 — Criar Componentes de Exibição

## Status

Concluída em 2026-07-20.

## Dependência

- `002-03` concluída.

## Objetivo

Criar `HealthCard`, `MetricCard` e `HealthChart` como apresentação pura.

## Pode modificar

- Arquivos novos de componentes em `core.designsystem`.
- Testes e previews destes componentes; este arquivo.

## Passos

1. Definir `HealthCard` e `MetricCard` com textos, valores formatados pelo chamador, slots e estado visual necessário.
2. Definir `HealthChart` que renderiza pontos fornecidos; não calcula agregados, tendências nem persiste métricas.
3. Usar papéis Material 3/tokens e spacing/shapes/elevação centralizados.
4. Incluir estado sem dados compreensível, sem inventar regra de domínio.
5. Criar previews claro/escuro, fontes ampliadas e testes de renderização, semântica e escala.

## Aceite

- Cards e gráfico não contêm regra de saúde nem dependência técnica.
- Gráfico suporta dados fornecidos, sem dados e acessibilidade básica.
