# Baseline Atual

## Produto E Fase

Champion Companion e aplicativo Android nativo do relogio Champion C001, escrito em Kotlin e Jetpack Compose.

A Fase 1 estabelece fundacao atual do projeto. Escopo nao inclui BLE, Room, Health Connect, GPS/localizacao, servicos, parsers ou dominio funcional. Fases futuras devem preservar separacao entre transporte, protocolo, dominio e UI definida no roadmap.

## Ambiente E Validacao

Use Android Studio, Android SDK Platform 36 e toolchain JDK 21. Gradle pode executar em JDK 17 ou superior, conforme AGP 9.3.0. Use somente o Gradle Wrapper do repositorio. Validacao fisica desta fase usa Galaxy S22 via ADB.

```bash
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
```

## Base De Codigo

Ha um modulo Android, `app`.

- Codigo-fonte: `app/src/main/java/br/com/mwconsulting/championcompanion`
- Recursos: `app/src/main/res`
- Testes JVM: `app/src/test`
- Testes instrumentados: `app/src/androidTest`

`app` concentra estado global. `core.common`, `core.error`, `core.logging` e `core.navigation` sao infraestrutura. `feature/` contem `home`, `health`, `calendar`, `exercises`, `device`, `profile` e `settings`.

Compose constroi UI. Hilt faz injecao. Coroutines e `StateFlow` modelam estado. Kotlin Serialization representa rotas. Navegacao contem somente sete destinos iniciais. Erros devem virar estado UI seguro; detalhes tecnicos ficam no Logcat de debug.

## Variantes

- `debug`: `br.com.mwconsulting.championcompanion.dev`, `Champion Companion Dev`, `1.0-dev`
- `release`: `br.com.mwconsulting.championcompanion`, `Champion Companion`, `1.0`

Release ainda nao recebe assinatura de producao.

## Privacidade E Logs

Logs estruturados usam `android.util.Log` apenas para diagnostico em debug. Nao registrar payload BLE, coordenadas ou rotas, tokens, credenciais, identificadores pessoais ou dados de saude. Release deve manter logs diagnosticos inativos ou sem detalhes.

## Fontes Autoritativas

- Requisitos: `docs/specs/`
- Direcao de produto: `docs/roadmap/`
- Decisoes: `docs/decisions/`
- Protocolos: `docs/protocol/`
- Baseline de referencia detalhada: `docs/core/current-project-baseline.md`
