# Repository Guidelines

## Fonte de contexto

Leia [a baseline atual](docs/core/current-project-baseline.md) antes de alterar
código, dependências ou arquitetura. Especificações, roadmap e ADRs definem
requisitos; este arquivo define apenas como contribuir.

## Build, testes e desenvolvimento

Use o Gradle Wrapper; não substitua por Gradle global.

```bash
./gradlew lint                 # análise Android/Lint
./gradlew testDebugUnitTest    # testes JVM da variante debug
./gradlew assembleDebug        # APK debug
./gradlew assembleRelease      # APK release
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
```

Execute o último comando antes de abrir um PR que altere código ou Gradle.

## Estilo e organização

Siga o estilo Kotlin existente: quatro espaços, imports explícitos, funções e
propriedades em `camelCase`, classes/Composables em `PascalCase`. Mantenha um
recurso dentro de seu pacote. Prefira telas pequenas e estado UI seguro a
lógica de domínio na camada Compose.

## Testes

Adicione teste unitário para cada regra, mapeamento de erro ou comportamento não
visual; use nomes descritivos em crases, como
`` `network error maps to safe message` ``. Use teste instrumentado somente
quando depender de framework Android, Compose ou dispositivo. Execute o teste
afetado e `testDebugUnitTest`; não reduza cobertura existente.

## Segurança e mudanças

Nunca registre dados sensíveis, tokens, segredos ou identificadores. Não exponha
stack traces ou mensagens internas na UI. Confirme limites de escopo na baseline
e na especificação aplicável antes de introduzir uma nova capacidade.

## Commits e pull requests

Use Conventional Commits, como `feat: add device connection state` ou
`fix: map transport failure safely`. Faça commits pequenos e coesos. PRs devem
explicar objetivo e impacto, vincular issue/tarefa quando houver, listar os
comandos executados e incluir captura de tela para mudanças visuais. Não misture
refatorações não relacionadas.
