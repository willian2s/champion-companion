# Task 005-05 — Implementar Dispatch De Comando E Handshake A1

## Status

Planejada.

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
