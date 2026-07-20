# Task 001-02 — Configurar Build Foundation

## Status

Pendente.

## Dependência

- `001-01` concluída.

## Objetivo

Preparar catálogo e build para JDK 21, Compose, Coroutines, StateFlow, Serialization, Hilt e Navigation.

## Pode modificar

- `gradle/libs.versions.toml`
- Gradle raiz e `app/build.gradle.kts`
- `gradle.properties`
- Este arquivo.

## Passos

1. Conferir documentação oficial de compatibilidade AGP `9.3.0`, Kotlin `2.2.10`, JDK 21 e versões adicionadas.
2. Registrar justificativa de versão no catálogo ou nesta task.
3. Adicionar plugins e dependências estritamente necessários.
4. Definir toolchain Java/Kotlin 21.
5. Rodar `./gradlew assembleDebug`.

## Fora do escopo

- Features, navegação, telas, variantes e CI.

## Aceite

- Catálogo único de versões.
- Build debug resolve e compila.
