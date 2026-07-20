# Task 003-01 — Preparar Fundação de Persistência

## Status

Pendente.

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
