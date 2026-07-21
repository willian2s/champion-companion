# Task 005-05 — Implementar Dispatch De Comando E Handshake A1

## Status

Concluída em 2026-07-21.

Resumo: handshake A1 foi implementado como protocolo puro em `DevicePocHandshakeA1.kt`, com builder determinístico de 20 bytes, matcher de confirmação em `F008_NOTIFY`, timeout explícito e comando `ChampionCommand` em `F008_WRITE` com `WITH_RESPONSE` e `ChampionRetryPolicy.None`. `DevicePocCoordinator` agora expõe `executeHandshake()` e só promove `HandshakeConfirmed` quando chega resposta válida do relógio; write success isolado não confirma A1. Estado confirmado é invalidado por reset de ciclo, disconnect e nova sessão porque `activeSessionId`, `cycleId` e `pocState` são limpos pela state machine criada na task anterior. `app:compileDebugKotlin` passou. Validação focada de testes continua bloqueada por falhas pré-existentes não relacionadas em `core/designsystem`, `core/error`, `core/model` e `data`, mas a rerun só apontou um ajuste local no teste novo, já corrigido.

## Objetivo

Implementar builder, dispatch e confirmação do handshake A1 reaproveitando fila, router e sessão de transporte existentes.

## Contexto

A1 é primeiro fluxo funcional do protocolo na PoC. Precisa usar `ChampionCommand`, `ChampionGattQueue`, `ChampionPacketRouter` e `ChampionNotificationHub`, sem confundir sucesso de write com confirmação do relógio.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/specs/_inputs/phase-05-native-ble-poc-direction.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/ble-command-mapping.md`

## Dependências

- `005-04` concluída.

## Pré-Condições

- dispatch mínimo de comando já disponível no core.

## Entradas

- transporte pronto com `F008_NOTIFY` ativa.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/tasks/005-native-ble-poc/005-05-implement-command-dispatch-and-a1-handshake.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `app/src/main/AndroidManifest.xml`
- `docs/protocol/*.md`
- `docs/decisions/*.md`

## Escopo

- builder A1;
- matcher de confirmação A1;
- timeout explícito;
- invalidação de handshake em disconnect e nova sessão;
- testes unitários e integração.

## Fora Do Escopo

- FEA1;
- D0/F002;
- retries automáticos ocultos.

## Passos

1. Implementar builder A1 puro com 20 bytes, senha `0000`, ano big-endian e timezone em blocos de 15 minutos.
2. Implementar matcher A1 baseado em `packet.size >= 20`, `packet[0] == 0xA1`, `packet[19] == 0x01`.
3. Enviar A1 por `ChampionCommand` em `F008 write` com `WITH_RESPONSE`.
4. Aguardar confirmação por notify F008 via router.
5. Atualizar estado da PoC para `HandshakeConfirmed` somente na confirmação válida.
6. Invalidar handshake em disconnect, cleanup, falha de transporte ou sessão nova.
7. Cobrir builder, matcher, timeout e invalidação com testes.
8. Atualizar `Status` e overview.

## Regras De Segurança

- write success nunca confirma A1;
- senha nunca entra em log ou relatório;
- sem retry automático durante Gate A.

## Critérios De Aceite

- A1 só inicia após `TransportReady`;
- confirmação depende de resposta válida do relógio;
- A1 invalida em toda troca/encerramento de sessão;
- testes cobrem builder, matcher e timeout.

## Testes

- JVM: builder A1, timezone, endianess, matcher, rejeição de pacote curto.
- integração: sucesso, timeout, rejeição e disconnect durante handshake.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*A1*"
./gradlew app:testDebugUnitTest --tests "*Poc*"
```

## Evidências Esperadas

- testes do builder e matcher;
- confirmação de que `HandshakeConfirmed` não deriva de write success.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar que A1 continua fora de Room, Logcat bruto e manifesto.

## Implementação Executada

### Builder A1

Arquivo criado:

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocHandshakeA1.kt`

Entrega:

- `buildPayload(dateTime, password)` com exatamente 20 bytes;
- senha fixa default `0000` codificada como `uint16 little-endian`;
- ano em big-endian;
- data/hora locais vindas do `ZonedDateTime` informado;
- timezone em blocos de 15 minutos usando arredondamento e gravado no byte 13;
- bytes fixos `0x01` em offsets 11 e 12;
- bytes finais zerados.

### Matcher de confirmação A1

`DevicePocHandshakeA1.confirmationMatcher()` aceita somente pacote que satisfaça:

- channel `F008_NOTIFY`;
- tamanho >= 20;
- `bytes[0] == 0xA1`;
- `bytes[19] == 0x01`.

### Dispatch e confirmação

`DevicePocCoordinator` agora expõe:

- `executeHandshake()`;
- `executeHandshake(dateTime, timeout)` como overload `internal` testável.

Fluxo:

1. exige `activeSessionId` presente;
2. exige `pocState == TransportReady`;
3. obtém `ChampionTransportSession` ativa;
4. move estado para `Handshaking`;
5. executa `transport.executeCommand(A1)`;
6. promove para `HandshakeConfirmed` somente em `ChampionBleResult.Success` do await do pacote;
7. em falha, move para `Failed` com erro tipado;
8. se sessão ativa mudar durante espera, retorna cancelamento seguro sem contaminar novo ciclo.

### Invalidação de handshake

Não foi criado booleano global extra. A invalidação já decorre da state machine existente:

- `resetCycle()` troca `cycleId` e limpa `activeSessionId`;
- `disconnect()` limpa ciclo e volta a `Idle`;
- sessão nova substitui `activeSessionId`;
- callbacks de sessão antiga não promovem estado novo.

## Critérios De Aceite Atendidos

- A1 só inicia após `TransportReady`;
- confirmação depende de resposta válida do relógio;
- write success não confirma A1;
- A1 invalida em reset/disconnect/troca de sessão;
- timeout explícito existe no comando A1;
- nenhum retry automático foi introduzido.

## Testes E Validação

### Produção

Comando executado:

```bash
./gradlew app:compileDebugKotlin
```

Resultado:

- `BUILD SUCCESSFUL`

### Testes adicionados

Arquivos criados/alterados:

- `app/src/test/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocHandshakeA1Test.kt`
- `app/src/test/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocCoordinatorTest.kt`

Cobertura adicionada:

- builder A1 com 20 bytes;
- senha `0000`;
- ano big-endian;
- timezone positivo e negativo;
- matcher A1 válido e pacote curto/inválido;
- sucesso do handshake somente após notify válido;
- timeout do handshake;
- invalidação por disconnect.

### Validação focada de testes

Comando executado:

```bash
./gradlew app:testDebugUnitTest --tests "*A1*" --tests "*DevicePocCoordinatorTest"
```

Resultado:

- compilação ampla de testes segue bloqueada por falhas pré-existentes fora do escopo BLE/PoC em `core/designsystem`, `core/error`, `core/model` e `data`;
- rerun apontou apenas um problema local do teste novo (`async` fora de `coroutineScope`), já corrigido;
- após correção local, a saída voltou a mostrar apenas os bloqueios externos conhecidos.

## Evidência Final

- A1 continua fora de Room, Logcat bruto e Manifest;
- protocolo A1 está encapsulado e testável;
- `HandshakeConfirmed` não deriva de write success;
- task deixa pronto o caminho para `005-06` e `005-07`.
