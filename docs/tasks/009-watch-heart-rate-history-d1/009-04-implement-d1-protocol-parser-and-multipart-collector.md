# Task 009-04 - Implementar protocolo D1, parser e collector multipart

## Status

Pendente.

## Objetivo

Implementar comando D1, parser unico, resolucao de timestamp e collector multipart com suporte a fora de ordem, timeout parcial e callbacks tardios ignorados.

## Contexto

Codigo atual ja possui transporte BLE, fila GATT, `ChampionNotificationHub`, `ChampionPacketRouter` e handshake A1 reutilizaveis. Falta camada especifica de protocolo D1 para transformar `F008_NOTIFY` em pacotes interpretados e resultado de coleta consistente antes de tocar persistencia final.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-03-define-d1-contracts-import-state-and-import-metadata-models.md`
- `docs/protocol/history-d1.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/ble-transport.md`

## Dependencias

- `009-03`

## Entradas

- contratos D1 definidos em `009-03`;
- offsets confirmados no protocolo;
- timeout de 15 segundos;
- limite do comando `D1 01 00 00`.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD1HistoryProtocol.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleTimeouts.kt`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-04-implement-d1-protocol-parser-and-multipart-collector.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD1HistoryProtocolTest.kt`

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- `feature/health/**`
- `feature/home/**`
- `feature/device/DeviceConnectionBootstrap.kt`

## Escopo

- builder exato `D1 01 00 00`;
- parser de pacote D1 com validacao de tamanho, comando, indices, hora, minuto e BPM;
- contexto de tentativa com `baseLocalDate` e `zoneId` congelados;
- collector multipart com fora de ordem, dedup em memoria, lacunas e timeout parcial;
- resultado tipado sem payload sensivel.

## Fora Do Escopo

- persistencia Room;
- criacao de `PENDING`;
- UI Compose;
- coordinator de conexao/gate.

## Passos

1. Criar `ChampionD1HistoryProtocol.kt` com builder, parser, collector e modelos puros necessarios.
2. Implementar little-endian de `packetIndex` e `totalPackets`.
3. Implementar resolucao `baseLocalDate + hora + minuto + zoneId -> Instant` fora do parser de bytes.
4. Implementar collector com timeout centralizado, deduplicacao em memoria, ordenacao final e resultado `Completed`, `CompletedWithGaps`, `Partial`, `Failed`, `Cancelled`.
5. Adicionar testes pequenos de parser, timestamp, conclusao, fora de ordem, timeout parcial e callback tardio ignorado.
6. Atualizar spec e overview com nomes reais dos tipos criados.

## Criterios De Aceite

- comando D1 exato implementado;
- offsets e validacoes corretos;
- bytes desconhecidos seguem sem semantica;
- collector aceita fora de ordem;
- timeout parcial preserva pontos validos;
- timeout vazio falha;
- callback tardio e ignorado;
- nenhum acesso a Room ou UI nesta task.

## Testes Pequenos Aplicaveis

- builder `D1 01 00 00`;
- pacote `< 20` bytes;
- comando incorreto;
- little-endian de indices;
- hora invalida;
- minuto invalido;
- BPM limite e fora da faixa;
- resolucao de timestamp;
- pacote fora de ordem;
- conclusao com lacunas;
- timeout parcial;
- timeout vazio.

## Validacao Minima

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest` focado nos testes de protocolo D1.

## Evidencias

- resultados de testes pequenos;
- diff do protocolo D1 com referencias cruzadas aos offsets confirmados.

## Atualizacao Do Overview

- marcar `009-04` como concluida;
- registrar nomes reais do protocolo e dos testes criados;
- confirmar dependencia de `009-07` em `009-04`.

## Validacao Final

- confirmar que persistencia e UI permaneceram fora desta task.
