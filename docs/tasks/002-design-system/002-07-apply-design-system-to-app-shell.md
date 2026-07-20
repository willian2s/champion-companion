# Task 002-07 — Aplicar Design System ao Shell

## Status

Concluída em 2026-07-20.

## Dependências

- `002-04`, `002-05` e `002-06` concluídas.

## Objetivo

Aplicar tema central e componentes ao shell, navegação e placeholders existentes; remover cores locais.

## Pode modificar

- `MainActivity.kt`, shell/navegação e placeholders atuais.
- Tema antigo substituído pelo `core.designsystem`.
- Testes/previews afetados; este arquivo.

## Passos

1. Envolver conteúdo raiz com `ChampionCompanionTheme` mantendo seguimento do sistema e padrão dinâmico.
2. Migrar navegação, scaffold e placeholders para componentes/tokens Material 3 e design system.
3. Substituir `Color.White`, `Color.Black`, hex e cores locais em features por papéis/tokens adequados.
4. Não mudar rotas, contratos de navegação ou comportamento funcional da Fase 1.
5. Validar claro/escuro, fonte ampliada, semântica e ausência de cores literais em features.

## Aceite

- Shell inteiro usa tema central.
- Busca não encontra Material 2, `Color.White`, `Color.Black` ou hex em features.
- Navegação inicial continua com sete destinos e sem capacidades novas.
