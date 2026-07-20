# Task 001-01 — Auditar Projeto Gerado

## Status

Concluída em 2026-07-20.

## Dependência

- `001-00` lida.

## Objetivo

Registrar estado inicial do projeto Android Studio e compatibilidade base, sem alterar código.

## Entrada

- `settings.gradle.kts`, Gradle raiz, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle.properties`
- `AndroidManifest.xml`, tema, `MainActivity.kt`

## Saída permitida

- Este arquivo.

## Inventário auditado

### Gradle e SDK

| Item | Estado |
| --- | --- |
| Projeto raiz | `Champion Companion` |
| Módulos | somente `:app` |
| Namespace | `br.com.mwconsulting.championcompanion` |
| Application ID | `br.com.mwconsulting.championcompanion` |
| `compileSdk` | 36, minor API level 1 |
| `minSdk` | 28 |
| `targetSdk` | 36 |
| AGP | 9.3.0 |
| Kotlin | 2.2.10 |
| Gradle wrapper | 9.5.0, SHA-256 configurado |
| Java compilação | 11 (`sourceCompatibility` e `targetCompatibility`) |
| Toolchain JDK Gradle | não declarada |
| JDK disponível na auditoria | Zulu OpenJDK 17.0.19 LTS |
| Compose | habilitado; plugin Kotlin Compose e BOM `2026.02.01` |
| Build release | otimização desabilitada |

`./gradlew --version` não concluiu: sandbox não permite criar lock em `~/.gradle/wrapper/dists`. Versão do wrapper e JDK foram confirmadas por `gradle-wrapper.properties` e `java -version`; nenhum arquivo foi alterado por essa tentativa.

### Aplicação inicial

- `app` único módulo Android.
- `MainActivity` é `ComponentActivity`, atividade `MAIN`/`LAUNCHER` exportada.
- `AndroidManifest.xml` declara apenas aplicação e `MainActivity`; sem permissões.
- Tema Material 3 Compose `Theme.ChampionCompanion`, com cores dinâmicas Android 12+ habilitadas.
- Conteúdo inicial: `Greeting("Android")` em `Scaffold`; sem fluxo de domínio.

### Integrações ainda ausentes

Busca em `app`, Gradle raiz, catálogo de versões e `gradle.properties` não encontrou BLE/Bluetooth, Room, Health Connect, GPS/localização nem serviços/Foreground Services. Manifest também não declara permissões ou componentes dessas integrações.

### Estado Git na auditoria

`git status --short` já tinha 29 alterações staged, todas de documentação: criação de especificação/tasks Fase 1 e renomeação de tasks Fase 0. Nenhuma alteração nos arquivos Gradle ou no módulo `app` foi encontrada. Estado preservado.

## Passos

1. [x] Registrar namespace, IDs, SDKs, AGP, Kotlin, Compose, JDK configurado e wrapper.
2. [x] Registrar módulo existente, Activity inicial, manifest e tema.
3. [x] Rodar `git status --short`; registrar estado.
4. [x] Confirmar ausência de BLE, Room, Health Connect, GPS e serviços.

## Aceite

- [x] Inventário reproduzível do projeto inicial.
- [x] Nenhum código ou configuração do projeto alterado.
