# Baseline Atual do Projeto

## Produto e fase

Champion Companion é aplicativo Android nativo do relógio Champion C001, escrito em Kotlin e Jetpack Compose.

Baseline atual inclui fundação Android, design system próprio, domínio e persistência iniciais com Room/DataStore, e núcleo BLE nativo reutilizável validado na Fase 04. Escopo ainda não inclui handshake A1 funcional, fluxos funcionais D0/D1/D3/D4, Health Connect, GPS/localização, foreground service, background BLE, auto-connect, nem tela funcional completa de dispositivo. Separação entre transporte, protocolo, domínio e UI continua obrigatória.

## Ambiente e validação

Use Android Studio, Android SDK Platform 36 e toolchain JDK 21. O Gradle pode executar em JDK 17 ou superior, conforme AGP 9.3.0. Use exclusivamente o wrapper do repositório. Validação automatizada atual usa `lint`, testes JVM, `assembleDebug`, `assembleRelease` e `connectedDebugAndroidTest` quando houver Galaxy S22 conectado via ADB.

```bash
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
./gradlew app:connectedDebugAndroidTest
```

## Base de código

Há um módulo Android, `app`. Código-fonte: `app/src/main/java/br/com/mwconsulting/championcompanion`. `app` concentra estado global. Infra inclui `core.common`, `core.error`, `core.logging`, `core.navigation`, `core.preferences`, `core.database` e `core.bluetooth`. `core.bluetooth/android` concentra fronteira única com APIs Android BLE. `core.model` guarda contratos e domínio. `data/` implementa persistência via Room e repositories. `feature/` possui `home`, `health`, `calendar`, `exercises`, `device`, `profile` e `settings`; `feature/device` ainda não oferece fluxo BLE funcional completo. Recursos: `app/src/main/res`; testes: `app/src/test` e `app/src/androidTest`.

Compose constrói UI; Hilt faz injeção; Coroutines e `StateFlow` modelam estado; Kotlin Serialization representa rotas. Navegação contém sete destinos iniciais com back stack manual. Room 3 e DataStore já estão ativos. Núcleo BLE oferece scanner, fila GATT serial por sessão, discovery, capability registry, subscriptions obrigatórias e `TransportReady`, sem tratar transporte como handshake concluído. Erros devem virar estado UI seguro; detalhes técnicos ficam no Logcat de debug.

As variantes são `debug` (`br.com.mwconsulting.championcompanion.dev`, `Champion Companion Dev`, `1.0-dev`) e `release` (`br.com.mwconsulting.championcompanion`, `Champion Companion`, `1.0`). Release ainda não recebe assinatura de produção.

## Privacidade e fontes

Logs estruturados usam `android.util.Log` apenas para diagnóstico em debug. Não registrar payload BLE, bytes crus, hexadecimal, MAC, coordenadas/rotas, tokens, credenciais, identificadores pessoais ou dados de saúde. Release deve manter logs diagnósticos inativos ou sem detalhes.

Requisitos: `docs/specs/`; direção de produto: `docs/roadmap/`; decisões: `docs/decisions/`; protocolos: `docs/protocol/`.
