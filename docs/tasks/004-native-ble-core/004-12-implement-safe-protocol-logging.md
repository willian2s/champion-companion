# Task 004-12 — Implementar Logging Seguro Do Núcleo BLE

## Status

Concluida.

Resumo: implementados `ChampionProtocolLogger` e `ChampionDiagnosticPacketSink` com schema seguro de metadados BLE, sink opcional desacoplado de Room e extensão controlada do `StructuredLogger`. Logs BLE de debug permanecem sem payload, sem MAC, sem dados de saúde e sem identificadores persistentes de device.

## Objetivo

Adicionar logger BLE de debug com metadados seguros e preparar contrato opcional para captura diagnóstica privada futura, sem Room nesta fase.

## Contexto

Roadmap antigo citava payload hexadecimal em debug. Fase 04 deve seguir guard rails atuais: sem bytes brutos, sem MAC, sem dados de saúde.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/specs/_inputs/phase-04-native-ble-core-direction.md`
- `docs/decisions/adr-004-operational-privacy-guards.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/logging/DebugStructuredLoggerTest.kt`

## Dependências

- `004-02` e `004-10` concluídas.

## Entradas

- logger estruturado atual;
- contratos BLE e packet router.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionProtocolLogger.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionDiagnosticPacketSink.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/logging/StructuredLogger.kt`, somente se precisar extensão segura
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/logging/LoggingModule.kt`, somente se precisar binding seguro
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `data/**`
- `core/database/**`
- `AndroidManifest.xml`

## Escopo

- eventos BLE seguros em debug;
- esquema aprovado de campos;
- sink opcional e desacoplado para captura privada futura;
- sem integração com Room.

## Fora Do Escopo

- persistir pacotes;
- exportar logs;
- tela diagnóstica.

## Regras De Segurança

- não logar hex, bytes, MAC, BPM, passos, payload A1 ou IDs persistentes;
- release continua sem logs diagnósticos detalhados;
- sink opcional não retém bytes indefinidamente.

## Passos De Implementação

1. Definir eventos BLE seguros e campos permitidos.
2. Integrar logger BLE ao schema debug atual sem ampliar dados sensíveis.
3. Criar contrato opcional de sink privado desacoplado de Room.
4. Atualizar testes de logging seguro.
5. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- logs debug BLE contêm só metadados seguros;
- release segue sem log diagnóstico detalhado;
- sink privado é opcional, copiado defensivamente e desacoplado de persistência.

## Testes

- JVM para sanitização de logs, ausência de bytes/MAC/saúde e cópia defensiva do sink.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Logger*"
```

## Validação Final

- confirmar que nenhum arquivo de Room foi tocado para logging BLE.

## Atualização Documental Esperada

- `Status` deste arquivo concluído;
- registrar se política de logging exige ADR própria.

## Implementacao Registrada

Arquivos criados:

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionProtocolLogger.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionDiagnosticPacketSink.kt`

Arquivos atualizados:

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/logging/StructuredLogger.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/logging/LoggingModule.kt`

Conclusoes:

- `StructuredLogger` ganhou extensão segura para eventos com metadados allowlistados por `LogEvent`;
- novos eventos BLE aprovados: pacote observado, estado de transporte e resultado de operação;
- `ChampionProtocolLogger` registra apenas campos aprovados como `session_id`, `channel`, `direction`, `command_code`, `byte_count`, `state`, `operation_type`, `result` e `error_code`;
- nenhum log BLE inclui hex, bytes brutos, MAC, BPM, passos, payload A1, credenciais ou IDs persistentes de device;
- `ChampionDiagnosticPacketSink` é opcional, desacoplado de Room e recebe cópia defensiva de bytes para captura diagnóstica privada futura;
- binding default ficou em `NoOpChampionDiagnosticPacketSink`, mantendo release sem diagnóstico detalhado adicional.

Validacoes executadas:

- `./gradlew app:compileDebugKotlin`: sucesso;
- `./gradlew app:testDebugUnitTest --tests "*DebugStructuredLoggerTest*"`: sucesso;
- `./gradlew app:testDebugUnitTest --tests "*Logger*"`: falha de infraestrutura do task runner no ambiente atual (`NoSuchFileException` em `test-results`), nao por erro de compilacao do logger;
- nenhum arquivo de Room foi tocado.

ADR:

- politica de logging BLE ficou dentro de ADR-004 e nao exigiu ADR propria nesta task.
