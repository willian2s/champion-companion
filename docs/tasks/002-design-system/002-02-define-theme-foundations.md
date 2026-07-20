# Task 002-02 — Definir Fundações de Tema

## Status

Pendente.

## Dependência

- `002-01` concluída.

## Objetivo

Criar `core.designsystem` com tema Material 3, paletas próprias, escalas e testes de seleção/contraste.

## Pode modificar

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/designsystem/`
- Dependências estritamente necessárias; este arquivo.

## Passos

1. Criar `LightColorScheme` e `DarkColorScheme` completos, documentando seed `#006782` e papéis tonais; não usar preto puro como superfície escura padrão.
2. Criar `ChampionCompanionTheme(darkTheme, useDynamicColors, content)` com `isSystemInDarkTheme()` e `useDynamicColors = true` por padrão.
3. Em API 31+, selecionar `dynamicLightColorScheme`/`dynamicDarkColorScheme` somente quando solicitado; manter esquema próprio como fallback obrigatório.
4. Centralizar `ChampionSpacing` em `4/8/12/16/24/32/40/48.dp`, shapes `8/12/16/24.dp`, tipografia Material 3/sistema e tokens de elevação tonal.
5. Expor tokens via `CompositionLocal` ou API pública estável; componentes consomem `MaterialTheme`/tokens, nunca hex ou decisão manual de tema.
6. Testar seleção claro/escuro/dinâmico/fallback e contraste dos pares usados; criar previews-base claro/escuro e fonte ampliada onde aplicável.

## Fora do escopo

- Preferência de tema, fonte externa, componentes de feature e recursos launcher/splash.

## Aceite

- Contrato público do tema e fundações existe em `core.designsystem`.
- Esquemas próprios completos funcionam com e sem Material You.
- Escalas exatas centralizadas; contraste e seleção cobertos por testes.
