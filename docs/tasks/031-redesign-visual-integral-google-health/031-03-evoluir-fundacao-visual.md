# 031-03 — Evoluir fundação visual

- **Ticker:** `031`
- **Número:** `03`
- **Status:** pending

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
