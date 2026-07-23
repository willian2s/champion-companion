# Baseline Atual do Projeto

## Produto e fase

Champion Companion é aplicativo Android nativo do relógio Champion C001, escrito em Kotlin e Jetpack Compose.

Baseline atual inclui fundação Android, design system próprio, persistência com Room/DataStore, núcleo BLE nativo reutilizável validado na Fase 04, PoC BLE validada na Fase 05, feature utilizável de gerenciamento do Champion C001 concluída na Fase 06, primeira Home funcional de resumo diário local concluída na Fase 07, Fase 08 concluída com medição manual de frequência cardíaca e Fase 09 concluída com histórico D1 local-first de frequência cardíaca. Escopo atual já inclui handshake A1 funcional, associação persistida separada da sessão BLE efêmera, scan manual, conexão manual, auto-conexão controlada do dispositivo associado, disconnect, retry, esquecimento sem apagar histórico, diagnóstico seguro, exportação sanitizada, resumo diário persistido via FEA1 fresco por pull-to-refresh, merge atômico sem redução, `lastImportedAt` após import válido, UI Home rolável com indicadores de refresh, `ForegroundBleOperationGate` compartilhado entre refresh diário, medição manual e importação D1, medição manual de 60 segundos com D0/F002, persistência apenas do resultado final válido, último BPM persistido na Home, ação explícita `Buscar dados do relógio`, importação D1 com persistência idempotente e metadata dedicada, histórico consolidado manual + D1, agregações locais e gráfico funcional de frequência cardíaca na área Saúde. Escopo ainda não inclui escrita real D1 no Health Connect, D3/D4 funcionais, Health Connect como provider ativo, GPS/localização, foreground service, background BLE permanente ou auto-connect agressivo/amplo. Distância e calorias seguem dependentes de dados pessoais suficientes; quando indisponíveis, Home e Saúde mantêm valores ausentes sem inventar números. Separação entre transporte, protocolo, domínio e UI continua obrigatória.

## Ambiente e validação

Use Android Studio, Android SDK Platform 36 e toolchain JDK 21. O Gradle pode executar em JDK 17 ou superior, conforme AGP 9.3.0. Use exclusivamente o wrapper do repositório. Validação automatizada atual usa `lint`, `assembleDebug` e `assembleRelease`; fechamento da Fase 09 também registrou `assembleDebug`, `assembleRelease`, `installDebug` e validação local aceita para histórico D1, gráfico local e navegação da área Saúde.

```bash
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
./gradlew lint assembleDebug assembleRelease
```

## Base de código

Há um módulo Android, `app`. Código-fonte: `app/src/main/java/br/com/mwconsulting/championcompanion`. `app` concentra estado global. Infra inclui `core.common`, `core.error`, `core.logging`, `core/navigation`, `core/preferences`, `core/database` e `core.bluetooth`. `core.bluetooth/android` concentra fronteira única com APIs Android BLE. `core.model` guarda contratos e domínio. `data/` implementa persistência via Room e repositories. `feature/` possui `home`, `health`, `calendar`, `exercises`, `device`, `profile` e `settings`; `feature/device` agora oferece fluxo funcional de gerenciamento de dispositivo via `DeviceManagementViewModel` e `DeviceScreen`, enquanto PoC técnica debug permanece separada. Recursos: `app/src/main/res`; testes: `app/src/test` e `app/src/androidTest`.

Compose constrói UI; Hilt faz injeção; Coroutines e `StateFlow` modelam estado; Kotlin Serialization representa rotas. Navegação continua com back stack manual e subtree de Saúde com destinos específicos para frequência cardíaca, peso, passos e distância, além de transição slide direcional entre telas. Room 3 e DataStore já estão ativos. Núcleo BLE oferece scanner, fila GATT serial por sessão, discovery, capability registry, subscriptions obrigatórias e `TransportReady`, e a feature final de dispositivo promove handshake A1, persistência de associação dedicada, reconnect manual e auto-connect controlado do associado sem background permanente. `feature/home` expõe `HomeViewModel`, `ChampionDailyStepsSyncCoordinator` e Home com resumo diário persistido, retry manual, timeout seguro, day rollover local, pull-to-refresh como ação principal e atalho para detalhe de frequência cardíaca. `feature/health` agora expõe `HealthViewModel`, hub de métricas, detalhe de frequência cardíaca com medição manual persistida, importação D1 explícita, histórico consolidado, agregações e gráfico local; peso/passos/distância seguem usando dados já persistidos. Erros devem virar estado UI seguro; detalhes técnicos ficam no Logcat de debug.

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
