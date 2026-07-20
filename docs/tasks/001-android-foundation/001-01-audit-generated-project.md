# Task 001-01 — Auditar Projeto Gerado

## Status

Pendente.

## Dependência

- `001-00` lida.

## Objetivo

Registrar estado inicial do projeto Android Studio e compatibilidade base, sem alterar código.

## Entrada

- `settings.gradle.kts`, Gradle raiz, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `gradle.properties`
- `AndroidManifest.xml`, tema, `MainActivity.kt`

## Saída permitida

- Este arquivo.

## Passos

1. Registrar namespace, IDs, SDKs, AGP, Kotlin, Compose, JDK configurado e wrapper.
2. Registrar módulo existente, Activity inicial, manifest e tema.
3. Rodar `git status --short`; registrar estado.
4. Confirmar ausência de BLE, Room, Health Connect, GPS e serviços.

## Aceite

- Inventário reproduzível do projeto inicial.
- Nenhum arquivo fora desta task alterado.
