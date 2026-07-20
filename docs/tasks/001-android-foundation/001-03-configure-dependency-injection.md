# Task 001-03 — Configurar Hilt

## Status

Pendente.

## Dependência

- `001-02` concluída.

## Objetivo

Configurar Hilt no processo Android, sem criar dependências de domínio.

## Pode modificar

- Catálogo e Gradle necessários.
- `app/src/main/AndroidManifest.xml`
- `app/src/main/**`
- Este arquivo.

## Passos

1. Aplicar plugin Hilt e compilador compatível.
2. Criar `Application` com `@HiltAndroidApp`.
3. Declarar Application no manifest.
4. Anotar Activity inicial com `@AndroidEntryPoint`.
5. Criar somente módulos necessários para infraestrutura.
6. Rodar `./gradlew testDebugUnitTest`.

## Aceite

- App inicia com Hilt.
- Sem singleton de BLE, banco, localização ou Health Connect.
