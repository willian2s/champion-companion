# Task 002-02 — Definir Fundações de Tema

## Status

Concluída em 2026-07-20.

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

## Implementação

- Criado `core.designsystem` com `LightColorScheme` e `DarkColorScheme` completos em `ChampionColorScheme.kt`. Paletas próprias derivam de `#006782`; superfícies escuras usam `#101416` e variações, nunca preto puro como padrão.
- Criado `ChampionCompanionTheme(darkTheme, useDynamicColors, content)` em `ChampionTheme.kt`. Em API 31+ e com `useDynamicColors = true`, usa Material You; em API anterior, preferência desativada ou esquema dinâmico indisponível, seleciona esquema próprio.
- Seleção isolada em `selectChampionColorScheme`, sem `Context`, para teste JVM de claro, escuro, dinâmico e fallback.
- Criados tokens públicos em `ChampionTokens.kt`: `ChampionSpacing` (`4/8/12/16/24/32/40/48.dp`), `ChampionShapes` (`8/12/16/24.dp`), `ChampionTypography` com `FontFamily.Default`, `ChampionElevation`, `LocalChampionSpacing` e `LocalChampionElevation`.
- Criados previews base claro, escuro e fonte ampliada em `ThemePreviews.kt`.
- Tema legado em `ui/theme` permanece temporariamente para `002-07` migrar shell e remover imports antigos sem mudar navegação nesta task.

## Testes

- `ChampionThemeTest` cobre seleção claro/escuro/dinâmico/fallback, contraste mínimo `4.5:1` dos pares de texto e escala exata de spacing.

## Validação

```text
./gradlew testDebugUnitTest
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
```

Resultado: `BUILD SUCCESSFUL`. A validação completa executou 110 tarefas, com lint, testes JVM e APKs debug/release concluídos.

## Aceite

- [x] Contrato público do tema e fundações existe em `core.designsystem`.
- [x] Esquemas próprios completos funcionam com e sem Material You.
- [x] Escalas exatas centralizadas; contraste e seleção cobertos por testes.
