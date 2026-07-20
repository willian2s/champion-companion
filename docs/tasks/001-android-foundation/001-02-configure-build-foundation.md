# Task 001-02 — Configurar Build Foundation

## Status

Concluída em 2026-07-20.

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

1. [x] Conferir documentação oficial de compatibilidade AGP `9.3.0`, Kotlin `2.2.10`, JDK 21 e versões adicionadas.
2. [x] Registrar justificativa de versão no catálogo ou nesta task.
3. [x] Adicionar plugins e dependências estritamente necessários.
4. [x] Definir toolchain Java/Kotlin 21.
5. [x] Rodar `./gradlew assembleDebug`.

## Fora do escopo

- Features, navegação, telas, variantes e CI.

## Decisões de compatibilidade

- AGP `9.3.0` requer Gradle `9.5.0`; wrapper atual já atende. AGP requer JDK `17` mínimo para executar Gradle. Fonte: [release notes AGP 9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes).
- JDK `21` fica como toolchain de compilação Java/Kotlin. Gradle pode executar em JDK `17`; toolchain separa JDK de execução da compilação e alinha Java/Kotlin. Fonte: [Kotlin Gradle toolchains](https://kotlinlang.org/docs/gradle-configure-project.html#toolchains-for-jvm-projects).
- Plugin Serialization usa mesma versão do Kotlin (`2.2.10`), exigência do plugin do compilador. Biblioteca JSON `1.9.0` é compatível com Kotlin `2.2.x`. Fonte: [Kotlin Serialization](https://kotlinlang.org/docs/serialization-get-started.html).
- Coroutines Android `1.10.2` fornece `Dispatchers.Main` Android e `StateFlow` vem de `kotlinx-coroutines-core`, dependência transitiva. Fonte: [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines).
- Hilt e Navigation não são declarados nesta task: `001-03` aplica Hilt e `001-05` adiciona Navigation, evitando dependências sem uso.

## Alterações

- Catálogo centralizado: Coroutines, Serialization JSON e plugin Serialization.
- Plugin Serialization aplicado no módulo `app`; dependências Android Coroutines e Serialization JSON adicionadas.
- `compileOptions` e toolchain Kotlin definidos para Java 21.

## Validação

Executado com Gradle wrapper `9.5.0`, cache isolado e toolchain Eclipse Temurin `21.0.7` provisionada pelo resolvedor Foojay:

```text
GRADLE_USER_HOME=/private/tmp/champion-companion-gradle-001-02 \
  ./gradlew --offline --no-daemon assembleDebug --console=plain
```

Resultado: `BUILD SUCCESSFUL in 28s` — 36 tarefas, 10 executadas e 26 atualizadas. Execução offline confirmou dependências resolvidas e build compilado com Java/Kotlin 21.

## Aceite

- [x] Catálogo único de versões.
- [x] Build debug resolve e compila.
