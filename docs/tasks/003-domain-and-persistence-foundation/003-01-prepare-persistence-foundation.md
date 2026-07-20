# Task 003-01 — Preparar Fundação de Persistência

## Status

Concluída em 2026-07-20.

## Dependência

- `003-00` lida.

## Objetivo

Adicionar somente dependências, packages e configuração Android necessários a Room, DataStore e testes locais.

## Pode modificar

- `gradle/libs.versions.toml`, Gradle raiz e `app/build.gradle.kts`.
- Manifest, Application e módulos Hilt de infraestrutura.
- Estrutura vazia em `core/database`, `core/preferences`, `core/model`, `data` e testes.
- Este arquivo.

## Passos

1. Auditar dependências atuais; adicionar Room, KSP/Room compiler, DataStore Preferences e dependências de teste estritamente necessárias, em versões compatíveis com AGP/Kotlin atuais.
2. Configurar Room para schema exportado em diretório versionado e teste in-memory Android quando necessário.
3. Desativar Android Auto Backup e data extraction rules para debug/release; não criar mecanismo próprio de backup.
4. Criar módulos Hilt apenas para prover banco, DAOs, DataStore e dispatchers, sem BLE/Health Connect/GPS.
5. Definir packages físicos sem antecipar modelos ou DAOs genéricos.
6. Rodar `./gradlew testDebugUnitTest assembleDebug`.

## Aceite

- Dependências compilam; schemas Room poderão ser exportados.
- Auto Backup desativado no manifesto e XMLs aplicáveis.
- Nenhuma dependência/acesso MotionLog, BLE, Health Connect ou localização.

## Decisões

- Room 3 `3.0.0` usa KSP `2.2.10-2.0.2`; versão estável publicada em 2026-07-01. Package Maven/API: `androidx.room3`. Fonte: [Room 3 releases](https://developer.android.com/jetpack/androidx/releases/room3).
- DataStore Preferences `1.2.1`; versão estável publicada em 2026-03-11. Fonte: [DataStore releases](https://developer.android.com/jetpack/androidx/releases/datastore).
- Room 3 e Hilt usam KSP; KAPT legado removido. AGP `9.3` com KSP atual exige `android.disallowKotlinSourceSets=false`; flag experimental documentada no erro de configuração. KSP ainda gera fontes via Kotlin source set.
- `ChampionCompanionDatabase` e providers de DAOs ficam para `003-09`: schema V1 ainda não existe. Esta task prepara KSP, plugin `androidx.room3`, diretório versionado, DataStore e dispatchers sem criar banco vazio transitório.

## Validação

```text
./gradlew testDebugUnitTest assembleDebug --console=plain
BUILD SUCCESSFUL in 4m 44s
50 actionable tasks: 50 executed
```

- Backup: `android:allowBackup="false"`; regras cloud backup, device transfer e full backup excluem todos domínios.
- DataStore é singleton Hilt em `core.preferences`; dispatchers `Default` e `IO` têm qualifiers Hilt.
- Room 3 e Hilt processam código com KSP; não há `androidx.room:room-*`, `room-ktx`, KAPT nem `com.android.legacy-kapt`.
