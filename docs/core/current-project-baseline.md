# Baseline Atual do Projeto

## Produto e fase

Champion Companion é o aplicativo Android nativo do relógio Champion C001, escrito em Kotlin e Jetpack Compose.

A Fase 1 estabelece a fundação: não inclui BLE, Room, Health Connect, GPS/localização, serviços, parsers ou domínio funcional. Fases futuras devem preservar a separação entre transporte, protocolo, domínio e UI definida no roadmap.

## Ambiente e validação

Use Android Studio, Android SDK Platform 36 e toolchain JDK 21. O Gradle pode executar em JDK 17 ou superior, conforme AGP 9.3.0. Use exclusivamente o wrapper do repositório. A validação física desta fase usa Galaxy S22 via ADB.

```bash
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
```

## Base de código

Há um módulo Android, `app`. Código-fonte: `app/src/main/java/br/com/mwconsulting/championcompanion`. `app` concentra estado global. `core.common`, `core.error`, `core.logging` e `core.navigation` são infraestrutura. `feature/` possui `home`, `health`, `calendar`, `exercises`, `device`, `profile` e `settings`. Recursos: `app/src/main/res`; testes: `app/src/test` e `app/src/androidTest`.

Compose constrói a UI; Hilt faz injeção; Coroutines e `StateFlow` modelam estado; Kotlin Serialization representa rotas. Navegação contém somente os sete destinos iniciais. Erros devem virar estado UI seguro; detalhes técnicos ficam no Logcat de debug.

As variantes são `debug` (`br.com.mwconsulting.championcompanion.dev`, `Champion Companion Dev`, `1.0-dev`) e `release` (`br.com.mwconsulting.championcompanion`, `Champion Companion`, `1.0`). Release ainda não recebe assinatura de produção.

## Privacidade e fontes

Logs estruturados usam `android.util.Log` apenas para diagnóstico em debug. Não registrar payload BLE, coordenadas/rotas, tokens, credenciais, identificadores pessoais ou dados de saúde. Release deve manter logs diagnósticos inativos ou sem detalhes.

Requisitos: `docs/specs/`; direção de produto: `docs/roadmap/`; decisões: `docs/decisions/`; protocolos: `docs/protocol/`.
