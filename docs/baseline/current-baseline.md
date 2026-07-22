# Baseline Atual

## Produto E Fase

Champion Companion e aplicativo Android nativo do relogio Champion C001, escrito em Kotlin e Jetpack Compose.

Baseline atual inclui fundacao Android, design system proprio, dominio e persistencia iniciais com Room/DataStore, e nucleo BLE nativo reutilizavel validado na Fase 04. Escopo ainda nao inclui handshake A1 funcional, fluxos funcionais D0/D1/D3/D4, Health Connect, GPS/localizacao, foreground service, background BLE, auto-connect, nem tela funcional completa de dispositivo. Separacao entre transporte, protocolo, dominio e UI continua obrigatoria.

## Ambiente E Validacao

Use Android Studio, Android SDK Platform 36 e toolchain JDK 21. Gradle pode executar em JDK 17 ou superior, conforme AGP 9.3.0. Use somente o Gradle Wrapper do repositorio. Validacao automatizada atual usa `lint`, `assembleDebug` e `assembleRelease`; validacoes no Galaxy S22 via ADB seguem fluxo manual quando necessario.

```bash
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew lint assembleDebug assembleRelease
```

## Base De Codigo

Ha um modulo Android, `app`. Codigo-fonte: `app/src/main/java/br/com/mwconsulting/championcompanion`. `app` concentra estado global. Infra inclui `core.common`, `core.error`, `core.logging`, `core.navigation`, `core.preferences`, `core.database` e `core.bluetooth`. `core.bluetooth/android` concentra fronteira unica com APIs Android BLE. `core.model` guarda contratos e dominio. `data/` implementa persistencia via Room e repositories. `feature/` contem `home`, `health`, `calendar`, `exercises`, `device`, `profile` e `settings`; `feature/device` ainda nao oferece fluxo BLE funcional completo. Recursos: `app/src/main/res`; testes: `app/src/test` e `app/src/androidTest`.

Compose constroi UI. Hilt faz injecao. Coroutines e `StateFlow` modelam estado. Kotlin Serialization representa rotas. Navegacao contem sete destinos iniciais com back stack manual. Room 3 e DataStore ja estao ativos. Nucleo BLE oferece scanner, fila GATT serial por sessao, discovery, capability registry, subscriptions obrigatorias e `TransportReady`, sem tratar transporte como handshake concluido. Erros devem virar estado UI seguro; detalhes tecnicos ficam no Logcat de debug.

## Variantes

- `debug`: `br.com.mwconsulting.championcompanion.dev`, `Champion Companion Dev`, `1.0-dev`
- `release`: `br.com.mwconsulting.championcompanion`, `Champion Companion`, `1.0`

Release ainda nao recebe assinatura de producao.

## Privacidade E Fontes

Logs estruturados usam `android.util.Log` apenas para diagnostico em debug. Nao registrar payload BLE, bytes crus, hexadecimal, MAC, coordenadas/rotas, tokens, credenciais, identificadores pessoais ou dados de saude. Release deve manter logs diagnosticos inativos ou sem detalhes.

## Fontes Autoritativas

- Requisitos: `docs/specs/`
- Direcao de produto: `docs/roadmap/`
- Decisoes: `docs/decisions/`
- Protocolos: `docs/protocol/`
- Baseline de referencia detalhada: `docs/core/current-project-baseline.md`
