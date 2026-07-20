# BLE Command Mapping - Champion C001

## Objetivo

Mapear comandos BLE gerais do Champion C001 conforme MotionLog, sem detalhar parsers profundos que pertencem as tasks `000-05`, `000-06` e `000-07`.

## Fontes

- `../motionlog-app/src/lib/ble/champion-commands.ts`
- `../motionlog-app/src/lib/ble/champion-client.ts`
- `../motionlog-app/src/lib/ble/champion-parsers.ts`
- `../motionlog-app/src/lib/ble/champion-handshake.ts`
- `../motionlog-app/src/lib/ble/champion-heart-session.ts`
- `../motionlog-app/src/lib/ble/heart-history-sync.ts`
- `../motionlog-app/src/lib/ble/watch-workout-sync.ts`
- `../motionlog-app/docs/integrations/champion-c001-ble-protocol.md`
- `../motionlog-app/docs/specs/003-ble-foreground.md`

## Mapa Geral

| Codigo | Nome no MotionLog | Payload enviado | Canal | Resposta/stream | Status |
| --- | --- | --- | --- | --- | --- |
| A1 | handshake | 20 bytes gerados em runtime | F008 write | F008 notify, `bytes[0] == A1` e `bytes[19] == 01` | Confirmado pelo codigo |
| A0 | `READ_BATTERY_OR_CONFIG` | `A0 00` | F008 write | nao ha parser no app atual | Confirmado pelo codigo como constante; uso nao confirmado |
| A8 | `SPORT_CURRENT` | `A8 00` | F008 write | prototipo rotula como esporte atual; sem parser | Confirmado pelo codigo como constante; uso nao confirmado |
| D0 | `HR_START`/`HR_STOP` | `D0 01` / `D0 00` | F008 write | F008 notify D0 e F002 notify bruto | Confirmado pelo codigo |
| D1 | `ORIGINAL_TODAY` | `D1 01 00 00` | F008 write | F008 notify D1 multipart | Confirmado pelo codigo |
| D3 | `SPORT_MODEL_CRCS` | `D3 01 00 00` | F008 write | F008 notify D3, CRCs | Confirmado pelo codigo |
| D4 | `buildSportModelWorkoutCommand(blockIndex)` | `D4 <blockIndex> 00 00` | F008 write | F008 notify D4 multipart | Confirmado pelo codigo |
| DF | `ORIGINAL_DF_TODAY` | `DF 01 00 00` | F008 write | sem parser no app atual | Confirmado pelo codigo como constante; uso nao confirmado |
| E0 | `SLEEP_TODAY` | `E0 00` | F008 write | sem parser no app atual | Confirmado pelo codigo como constante; uso nao confirmado |
| F002 | stream sensor | nao identificado | F002 notify | amostras uint16 little-endian | Confirmado pelo codigo |
| FEA1 | passos/status | sem comando identificado | FEA1 notify | `01` + passos 24-bit little-endian | Confirmado pelo codigo |

## FEA1 Passos

Pacote aceito:

```text
01 XX XX XX
```

Parser confirmado:

```text
steps = bytes[1] | (bytes[2] << 8) | (bytes[3] << 16)
```

Regras:

- pacote deve ter exatamente 4 bytes;
- `bytes[0]` deve ser `0x01`;
- valor de passos usa 24-bit little-endian;
- runtime persiste resumo diario com debounce: se valor igual ao ultimo e intervalo menor que 4s, ignora.

Classificacao: `Confirmado pelo codigo`.

## A1 Resumo

Payload tem 20 bytes:

- `bytes[0] = A1`;
- senha decimal em bytes 1-2;
- ano em bytes 4-5;
- mes/dia/hora/minuto/segundo em bytes 6-10;
- `bytes[11] = 1`;
- `bytes[12] = 1`;
- timezone em blocos de 15 minutos no byte 13;
- `bytes[14] = 0`;
- bytes restantes ficam zerados.

Resposta confirmada quando F008 notify tem tamanho minimo 20, primeiro byte `A1` e byte 19 igual a `01`.

Classificacao: `Confirmado pelo codigo`. Detalhamento fica em `handshake-a1.md`.

## D0/F002 Resumo

- `D0 01` inicia medicao.
- `D0 00` para medicao.
- F002 notify recebe bytes brutos interpretados como `uint16` little-endian.
- `parseD0Bpm` tambem aceita primeiro valor no pacote F008 D0 entre 35 e 220.

Classificacao: `Confirmado pelo codigo`. Detalhamento fica em `heart-rate-d0-f002.md`.

## D1 Resumo

- Comando atual usado: `D1 01 00 00`.
- Fluxo: subscribe, handshake se necessario, listener F008, write, coleta ate `packetIndex >= totalPackets` ou timeout.
- Timeout foreground: 15s.
- Background Kotlin tambem usa D1 com timeout 12s.

Classificacao: `Confirmado pelo codigo`. Parser profundo fica em `history-d1.md`.

## D3/D4 Resumo

- `D3 01 00 00` lista CRCs de treinos.
- CRCs sao u16 little-endian e `0x0000` e ignorado.
- `blockIndex` e atribuido pela ordem dos CRCs validos retornados.
- `D4 <blockIndex> 00 00` baixa bloco especifico.
- D4 coleta ate `packetIndex >= totalPackets` ou timeout.
- Timeouts foreground: D3 10s, D4 15s por bloco.

Classificacao: `Confirmado pelo codigo`. Parser profundo fica em `workouts-d3-d4.md`.

## Comandos Catalogados Sem Parser Atual

| Comando | Evidencia | Risco |
| --- | --- | --- |
| A0 `A0 00` | constante MotionLog e prototipo | pode ser bateria/config, mas resposta nao esta documentada no app |
| A8 `A8 00` | constante MotionLog e prototipo | pode ser esporte atual, mas sem parser nem fluxo funcional |
| DF `DF 01 00 00` | constante MotionLog e prototipo | historico alternativo nao usado no app atual |
| E0 `E0 00` | constante MotionLog e prototipo | sono nao faz parte do escopo atual documentado |

Classificacao: `Ainda nao confirmado` para payload recebido, offsets, escalas e condicao de conclusao.

## Regras Para Nova Implementacao

- Mapa de comandos nao autoriza telas a escrever em GATT.
- Cada comando deve virar caso de uso/protocolo sobre fila serial.
- Comandos sem parser atual devem ficar atras de diagnostico ou feature flag ate confirmacao fisica.
- FEA1 pode alimentar resumo diario, mas sincronizacao BLE automatica ao abrir continua fora do roadmap futuro.

## Lacunas Para `000-14`

- Capturar pacote real de A0, A8, DF e E0 se estes fluxos forem mantidos.
- Confirmar se A8 substitui ou complementa D3/D4 em algum firmware.
- Confirmar se FEA1 e push espontaneo ou depende de comando anterior no dispositivo fisico.
