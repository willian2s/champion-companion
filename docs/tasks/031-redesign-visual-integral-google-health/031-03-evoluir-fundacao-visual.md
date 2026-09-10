# 031-03 — Evoluir fundação visual

- **Ticker:** `031`
- **Número:** `03`
- **Status:** completed

## Objetivo

Criar ou ajustar primitives e componentes Compose necessários para reproduzir a
composição da referência sem acoplar UI a dados ou alterar telas não migradas.

## Requisitos e aceite

- Tokens de espaçamento, forma, tipografia e elevação suportam composição alvo.
- Componentes são stateless, baseados em slots, acessíveis e reutilizáveis.
- Recursos são próprios/nativos; nenhuma fonte, logo, asset ou dependência
  Google/Fitbit entra no app.
- Tema raiz e consumidores fora da superfície migrada não mudam implicitamente.

## Escopo

Inclui design system, componentes de cards, headers, listas, filtros, estados e
gráficos quando necessário. Exclui ViewModel, repository, domínio e navegação.

## Dependências

- `031-01`, `031-02`.
- Material 3, Vico e componentes atuais.

## Arquivos e símbolos prováveis

- `core/designsystem/ChampionTokens.kt`.
- `ChampionTheme.kt`, `ChampionColorScheme.kt`.
- `ChampionDisplayComponents.kt`, feedback/status/action components.
- `core/designsystem/chart/*`.

## Passos

1. Comparar primitives existentes com baseline aprovado.
2. Implementar menor conjunto de slots exigido por duas superfícies reais.
3. Preservar semântica, contraste, gaps e unidades dos gráficos.
4. Remover somente duplicações sem consumidores após migração comprovada.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke visual sanitizado em portrait, landscape, fonte ampliada e temas. Não
criar ou executar testes automatizados.

## Definição de pronto

Fundação compilada, acessível e preparada para dois consumidores reais, a serem
confirmados nas subtarefas `031-05` e `031-06`; previews não contam. Nenhuma tela
não migrada sofre efeito visual não intencional. Evidência sanitizada, estado
inicial/restaurado do aparelho e revisão independente aprovados.

## Riscos

Mudança global de tokens pode redesenhar o app inteiro. Preferir tokens opt-in e
comparar diff de consumidores.

## Execução e evidências

- **Status da execução:** concluída; revisão independente aprovada com ressalva
  documental não bloqueante.
- **Arquivos alterados:** `ChampionRedesignTokens.kt`,
  `ChampionRedesignTheme.kt` e `ChampionRedesignComponents.kt`, todos em
  `core/designsystem`; este arquivo e `031-00-overview.md` foram atualizados
  para registrar o aceite.
- **APIs:** `ChampionRedesignTheme`,
  `ChampionRedesignSurfaceHeader`, `ChampionRedesignHeaderAction`,
  `ChampionRedesignHeaderNavigationAction` e `ChampionRedesignSummaryCard`;
  tokens separados `ChampionRedesignSpacing`, `ChampionRedesignShapes`,
  `ChampionRedesignTypography`, `ChampionRedesignElevation` e
  `LocalChampionRedesignTokens`.
- **Decisões:** tema local preserva `MaterialTheme.colorScheme` vigente e não é
  aplicado por `ChampionCompanionTheme`; componentes são stateless, usam slots,
  heading semântico e alvos mínimos de 48 dp; tipografia usa somente
  `FontFamily.Default`; nenhum gráfico ou contrato Vico foi alterado.
- **Desvios:** nenhum consumidor foi migrado, conforme escopo. Home e Saúde,
  tema raiz, navegação, ViewModel, repository, domínio, Room, BLE e Health
  Connect permaneceram intocados.
- **Comandos executados:** `git diff --check`; `./gradlew lint assembleDebug
  assembleRelease`; `./gradlew installDebug`; `adb devices -l`.
- **Resultados/evidências:** `git diff --check` passou; lint, builds debug/release
  e instalação debug passaram; um aparelho autorizado foi detectado pelo ADB.
  Não houve smoke visual porque componentes ainda não possuem consumidor real.
- **Testes:** não criados nem executados, conforme instrução explícita.
- **Revisão independente:** aprovada em 2026-09-03, após correção dos alvos de
  toque e da composição do conjunto de tokens. Ressalva não bloqueante: os
  consumidores reais serão validados nas subtarefas `031-05` e `031-06`.
- **Riscos residuais:** valores visuais ainda precisam de validação em duas
  superfícies reais nas subtarefas `031-05` e `031-06`; o checklist geral deve
  permanecer pendente somente para subtarefas posteriores até uso real. Smoke
  visual desta fundação isolada não foi executado porque ainda não há consumidor
  real; nenhum dado, estado ou configuração do aparelho foi alterado.
- **Correção após revisão independente reprovada:** `ChampionRedesignTokenSet`
  agora agrega spacing, shapes, typography e elevation; o tema aceita conjunto
  customizado e preserva o `MaterialTheme.colorScheme` vigente. Componentes
  usam shapes/typography do conjunto diretamente. Headers mantêm slots genéricos
  para conteúdo não interativo e expõem wrappers nativos `IconButton` para ações
  e navegação, com descrição obrigatória e alvo mínimo de 48 dp.
- **Validação da correção:** `./gradlew app:compileDebugKotlin` passou em
  2026-09-03; `git diff --check` e verificações equivalentes dos três arquivos
  novos passaram. Testes automatizados não foram criados nem executados.
