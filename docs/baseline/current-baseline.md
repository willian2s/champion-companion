# Baseline Atual do Projeto

## Produto e fase

Champion Companion é aplicativo Android nativo do relógio Champion C001, escrito em Kotlin e Jetpack Compose.

Baseline atual inclui fundação Android, design system próprio, persistência com Room/DataStore, núcleo BLE nativo reutilizável validado na Fase 04, PoC BLE validada na Fase 05 e feature utilizável de gerenciamento do Champion C001 concluída na Fase 06. Escopo atual já inclui handshake A1 funcional, associação persistida separada da sessão BLE efêmera, scan manual, conexão manual, auto-conexão controlada do dispositivo associado, disconnect, retry, esquecimento sem apagar histórico, diagnóstico seguro e exportação sanitizada. Escopo ainda não inclui D1/D3/D4 funcionais, Home com passos persistidos, BPM de produção, Health Connect, GPS/localização, foreground service, background BLE permanente ou auto-connect agressivo/amplo. Separação entre transporte, protocolo, domínio e UI continua obrigatória.

## Ambiente e validação

Use Android Studio, Android SDK Platform 36 e toolchain JDK 21. O Gradle pode executar em JDK 17 ou superior, conforme AGP 9.3.0. Use exclusivamente o wrapper do repositório. Validação automatizada atual usa `lint`, `assembleDebug` e `assembleRelease`; fechamento da Fase 06 também registrou `installDebug` e smoke test manual no Galaxy S22 com Champion C001.

```bash
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
./gradlew lint assembleDebug assembleRelease
```

## Base de código

Há um módulo Android, `app`. Código-fonte: `app/src/main/java/br/com/mwconsulting/championcompanion`. `app` concentra estado global. Infra inclui `core.common`, `core.error`, `core.logging`, `core/navigation`, `core/preferences`, `core/database` e `core.bluetooth`. `core.bluetooth/android` concentra fronteira única com APIs Android BLE. `core.model` guarda contratos e domínio. `data/` implementa persistência via Room e repositories. `feature/` possui `home`, `health`, `calendar`, `exercises`, `device`, `profile` e `settings`; `feature/device` agora oferece fluxo funcional de gerenciamento de dispositivo via `DeviceManagementViewModel` e `DeviceScreen`, enquanto PoC técnica debug permanece separada. Recursos: `app/src/main/res`; testes: `app/src/test` e `app/src/androidTest`.

Compose constrói UI; Hilt faz injeção; Coroutines e `StateFlow` modelam estado; Kotlin Serialization representa rotas. Navegação contém sete destinos iniciais com back stack manual. Room 3 e DataStore já estão ativos. Núcleo BLE oferece scanner, fila GATT serial por sessão, discovery, capability registry, subscriptions obrigatórias e `TransportReady`, e a feature final de dispositivo promove handshake A1, persistência de associação dedicada, reconnect manual e auto-connect controlado do associado sem background permanente. Erros devem virar estado UI seguro; detalhes técnicos ficam no Logcat de debug.

## Variantes

- `debug`: `br.com.mwconsulting.championcompanion.dev`, `Champion Companion Dev`, `1.0-dev`
- `release`: `br.com.mwconsulting.championcompanion`, `Champion Companion`, `1.0`

Release ainda não recebe assinatura de produção.

## Privacidade e fontes

Logs estruturados usam `android.util.Log` apenas para diagnóstico em debug. Não registrar payload BLE, bytes crus, hexadecimal, MAC, coordenadas/rotas, tokens, credenciais, identificadores pessoais ou dados de saúde. Release deve manter logs diagnósticos inativos ou sem detalhes.

## Fontes autoritativas

- Requisitos: `docs/specs/`
- Direção de produto: `docs/roadmap/`
- Decisões: `docs/decisions/`
- Protocolos: `docs/protocol/`
