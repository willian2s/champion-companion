# Task 004-10 — Implementar Envelope De Comando, Pacote e Roteamento De Respostas

## Status

Pendente.

## Objetivo

Permitir que comandos futuros aguardem respostas por sessão, canal e matcher estrutural, sem parser funcional de A1/D0/D1/D3/D4.

## Contexto

Fase 04 prepara transporte para Fase 05 e posteriores. Precisa expor pacotes seguros e casar respostas com operações aguardando matcher.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-02-define-ble-contracts-states-and-errors.md`
- `docs/tasks/004-native-ble-core/004-09-implement-notification-and-indication-multiplexing.md`
- `docs/protocol/ble-command-mapping.md`

## Dependências

- `004-02`, `004-04` e `004-09` concluídas.

## Entradas

- `ChampionCommand`, `ChampionPacket`, channels e notification hub.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionPacketRouter.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionResponseMatcher.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportStreams.kt`
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `feature/**`
- `data/**`
- `core/model/**`

## Escopo

- envelope imutável de pacote e comando;
- matcher por sessão/canal/código inicial;
- timeout e cancelamento da espera por resposta;
- disponibilidade de streams observadores.

## Fora Do Escopo

- parsing multipart D1/D3/D4;
- sucesso funcional do handshake A1;
- persistência de payload.

## Regras De Segurança

- sem retry cego para comandos de protocolo;
- bytes copiados defensivamente;
- pacote tardio de sessão antiga é ignorado.

## Passos De Implementação

1. Implementar roteador de pacotes e registry de matchers pendentes.
2. Definir janela de espera, timeout e cancelamento por comando.
3. Garantir que pacote consumido por matcher possa coexistir com stream observador, se a política da spec assim definir.
4. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- comando pode aguardar resposta sem tocar callback Android;
- matcher considera sessão, canal e código inicial;
- bytes são copiados defensivamente;
- nada implementa lógica funcional de A1/D1/D3/D4.

## Testes

- JVM para response matcher, timeout, callback tardio, pacote não consumido e cópia defensiva.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*PacketRouter*"
```

## Validação Final

- confirmar ausência de parser profundo de protocolo.

## Atualização Documental Esperada

- `Status` deste arquivo concluído.
