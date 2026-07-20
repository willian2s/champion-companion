# Spec 001 — Fundação Android Nativa

## Objetivo

Criar fundação Android nativa para Champion Companion. Preservar projeto Android Studio existente, módulo `app`, versões geradas e tema básico. Escopo não inclui BLE, Room, Health Connect, GPS, serviços, parsers ou domínio funcional.

## Decisões

- App único. Namespace atual: `br.com.mwconsulting.championcompanion`.
- `debug`: `applicationIdSuffix ".dev"`, nome `Champion Companion Dev`, versão com sufixo `-dev`.
- `release`: ID e nome atuais. Sem assinatura produção nesta fase.
- Navigation Compose: abas Home, Saúde, Calendário, Exercícios; menu adicional Dispositivo, Perfil, Configurações.
- Pacotes iniciais:
  - `app`
  - `core.common`
  - `core.error`
  - `core.logging`
  - `core.navigation`
  - `feature.home`
  - `feature.health`
  - `feature.calendar`
  - `feature.exercises`
  - `feature.device`
  - `feature.profile`
  - `feature.settings`
- Usar Hilt, Coroutines, StateFlow e Kotlin Serialization.
- Adicionar versões somente após checagem compatibilidade oficial com AGP `9.3.0`, Kotlin `2.2.10`, JDK 21 e catálogo gerado.
- Logging JSON estruturado via `android.util.Log`, sem biblioteca adicional. Nunca registrar payload BLE, coordenadas, tokens, identificadores pessoais ou dados saúde.
- Erro comum mapeia para estado UI seguro. Detalhes somente Logcat debug.
- Testes: contratos unitários básicos; inicialização e navegação instrumentadas.
- CI GitHub Actions: wrapper Gradle, JDK 21, `lint`, testes unitários, `assembleDebug`, `assembleRelease`; sem dispositivo físico.

## Tasks

| Task | Depende de | Pode modificar |
|---|---|---|
| `001-00-overview` | — | próprio arquivo; somente `001-09` consolida checklist final |
| `001-01-audit-generated-project` | `001-00` | próprio arquivo |
| `001-02-configure-build-foundation` | `001-01` | `gradle/libs.versions.toml`, Gradle raiz/app, `gradle.properties`, próprio arquivo |
| `001-03-configure-dependency-injection` | `001-02` | catálogo, Gradle necessário, `AndroidManifest.xml`, `app/**`, próprio arquivo |
| `001-04-create-package-structure` | `001-03` | novos arquivos pacotes iniciais, `MainActivity.kt` se necessário, próprio arquivo |
| `001-05-configure-navigation` | `001-04` | `core/navigation/**`, `app/**`, dependências Navigation necessárias, próprio arquivo |
| `001-06-create-placeholder-screens` | `001-05`, `001-07` | `feature/**`, textos necessários, próprio arquivo |
| `001-07-configure-error-handling-and-logging` | `001-03`, `001-04` | `core/error/**`, `core/logging/**`, módulo Hilt, testes unitários, próprio arquivo |
| `001-08-configure-build-variants` | `001-03` | `app/build.gradle.kts`, recursos variante, testes instrumentados afetados, próprio arquivo |
| `001-10-project-governance` | `001-01` | `README.md`, `AGENTS.md`, próprio arquivo |
| `001-11-configure-ci` | `001-02`, `001-06`, `001-07`, `001-08`, `001-10` | `.github/workflows/**`, próprio arquivo |
| `001-09-validate-android-foundation` | todas anteriores | testes básicos, próprio arquivo, `001-00-overview.md` somente consolidar sucesso |

## Ordem

1. `001-00`, `001-01`
2. `001-02`
3. `001-03`
4. `001-04`
5. `001-05` e `001-07` em paralelo
6. `001-06`; `001-08` paralelo etapas 4–6
7. `001-10`
8. `001-11`
9. `001-09`

Cada task altera apenas próprio campo `Status`. `001-09` lê todas e atualiza checklist central após validação completa.

## Rastreabilidade

| Requisito | Tasks |
|---|---|
| Projeto Android Studio, Kotlin, Compose, Material 3 | `001-01`, `001-02` |
| Navigation Compose, telas iniciais | `001-05`, `001-06` |
| Hilt | `001-03` |
| Coroutines, StateFlow, Serialization | `001-02`, `001-04` |
| Pacotes iniciais | `001-04` |
| Logging, erros | `001-07` |
| Debug/release, ID desenvolvimento, lint, versão | `001-02`, `001-08` |
| Testes básicos, Galaxy S22 | `001-09` |
| CI | `001-11` |
| README, AGENTS.md, commits | `001-10` |
| Sem BLE/Room/Health Connect/GPS/protocolo | todas; validação explícita `001-09` |

## Validação `001-09`

- Executar `./gradlew lint`.
- Executar testes unitários.
- Executar `assembleDebug` e `assembleRelease`.
- Executar testes instrumentados em Galaxy S22 conectado.
- Instalar debug `.dev` em paralelo.
- Abrir sete telas; navegar e retornar.
- Confirmar Logcat estruturado para erro controlado.
- Inspecionar: sem permissões BLE/localização/Health Connect, Room, serviços, parsers ou coordenadas.
- Rodar `git diff`: roadmap, ADR-001 e Fase 0 intactos.
- Rodar `git status --short`; sem commit/push.

## ADRs futuras e ambiguidades

- Retenção, redaction, exportação e nível release de logs diagnósticos.
- Assinatura, distribuição e política versão release.
- Modularização Gradle futura; Fase 1 mantém módulo `app`.
- Versões Hilt/KSP/Navigation/Serialization: obter compatibilidade oficial em `001-02`/`001-03`; não atualizar versões geradas sem justificativa registrada.
- Galaxy S22 deve estar conectado em `001-09`; ausência bloqueia somente validação física.
