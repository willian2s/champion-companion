# Champion C001 - Catalogo BLE

## Objetivo

Catalogar o dispositivo Champion C001 conforme baseline MotionLog, com UUIDs, services, characteristics e limites conhecidos para futura implementacao Android nativa.

## Nivel De Confirmacao

- `Confirmado pelo codigo`: existe no codigo MotionLog analisado.
- `Confirmado no uso funcional atual`: consta como fluxo funcional da baseline ou documento MotionLog de validacao.
- `Inferido a partir do codigo`: deduzido por relacao entre constantes, handlers e parsers.
- `Ainda nao confirmado`: presente como lacuna, constante sem fluxo completo, ou requisito sem evidencia fisica nesta task.

## Fontes Lidas

- `../motionlog-app/src/lib/ble/champion-uuids.ts`
- `../motionlog-app/src/lib/ble/champion-client.ts`
- `../motionlog-app/src/lib/ble/champion-commands.ts`
- `../motionlog-app/src/lib/ble/champion-runtime.ts`
- `../motionlog-app/src/lib/ble/champion-parsers.ts`
- `../motionlog-app/src/lib/ble/champion-handshake.ts`
- `../motionlog-app/src/lib/ble/heart-history-sync.ts`
- `../motionlog-app/src/lib/ble/watch-workout-sync.ts`
- `../motionlog-app/android/app/src/main/AndroidManifest.xml`
- `../motionlog-app/android/app/src/main/java/com/motionlog/app/MotionLogBackgroundBleService.kt`
- `../motionlog-app/docs/integrations/champion-c001-ble-protocol.md`
- `../motionlog-app/docs/specs/003-ble-foreground.md`

## Identificacao Do Dispositivo

| Item | Valor | Classificacao | Evidencia |
| --- | --- | --- | --- |
| Nome de scan foreground | prefixo `champion`, case-insensitive via `toLowerCase()` | Confirmado pelo codigo | `isChampionNamedDevice` em `champion-client.ts` |
| Nome em docs | `Champion C001` | Confirmado no uso funcional atual | roadmap e docs MotionLog |
| Filtro por service no scan foreground | nenhum; `startDeviceScan(null, ...)` | Confirmado pelo codigo | `champion-client.ts` |
| Busca de conectados | services F008, F002 e FEE7 | Confirmado pelo codigo | `getConnectedChampionDevicesAsync` |
| Background Android | device salvo por address, conectado, ou pareado contendo `champion` no nome | Confirmado pelo codigo | `resolveChampionDevice` em `MotionLogBackgroundBleService.kt` |

## Services E Characteristics

| Service | Characteristic | Direcao no MotionLog | Uso observado | Classificacao |
| --- | --- | --- | --- | --- |
| `f0080001-0451-4000-b000-000000000000` | `f0080002-0451-4000-b000-000000000000` | notify | respostas A1, D0, D1, D3, D4 e pacotes de protocolo F008 | Confirmado pelo codigo |
| `f0080001-0451-4000-b000-000000000000` | `f0080003-0451-4000-b000-000000000000` | write with response no JS; writeCharacteristic no background Kotlin | comandos A1, D0, D1, D3, D4 e comandos diagnosticos/prototipo | Confirmado pelo codigo |
| `f0020001-0451-4000-b000-000000000000` | `f0020002-0451-4000-b000-000000000000` | notify | stream bruto F002 usado para estimativa local de BPM durante D0 | Confirmado pelo codigo |
| `f0020001-0451-4000-b000-000000000000` | `f0020003-0451-4000-b000-000000000000` | nao usado no fluxo atual | constante catalogada, sem chamada de escrita encontrada | Confirmado pelo codigo, uso ainda nao confirmado |
| `0000fee7-0000-1000-8000-00805f9b34fb` | `0000fea1-0000-1000-8000-00805f9b34fb` | notify | passos/status, pacote `01 xx xx xx` | Confirmado pelo codigo |
| `0000fee7-0000-1000-8000-00805f9b34fb` | `0000fea2-0000-1000-8000-00805f9b34fb` | indicate opcional no prototipo; nao assinado pelo app atual | status/ack no prototipo Web BLE | Ainda nao confirmado no app atual |
| `0000fee7-0000-1000-8000-00805f9b34fb` | `0000fec8-0000-1000-8000-00805f9b34fb` | indicate opcional no prototipo; nao assinado pelo app atual | handshake/status no prototipo Web BLE | Ainda nao confirmado no app atual |

## Comandos Catalogados

| Comando | Payload | Characteristic de escrita | Resposta esperada | Documento detalhado |
| --- | --- | --- | --- | --- |
| A1 handshake | 20 bytes iniciando em `A1` | F008 write | F008 notify com `A1` e `bytes[19] == 0x01` | `handshake-a1.md` em task `000-05` |
| D0 iniciar BPM | `D0 01` | F008 write | F008 notify com D0 e/ou F002 notify bruto | `heart-rate-d0-f002.md` em task `000-05` |
| D0 parar BPM | `D0 00` | F008 write | nao ha conclusao confirmada nesta task | `heart-rate-d0-f002.md` em task `000-05` |
| D1 historico BPM | `D1 01 00 00` | F008 write | F008 notify `D1`, pacotes multipart | `history-d1.md` em task `000-06` |
| D3 listar treinos | `D3 01 00 00` | F008 write | F008 notify `D3`, CRCs u16 little-endian | `workouts-d3-d4.md` em task `000-07` |
| D4 baixar bloco | `D4 <blockIndex> 00 00` | F008 write | F008 notify `D4`, pacotes multipart | `workouts-d3-d4.md` em task `000-07` |
| FEA1 passos | sem comando de escrita identificado | FEA1 notify | `01` + passos 24-bit little-endian | `ble-command-mapping.md` |
| A8 esporte atual | `A8 00` | F008 write | prototipo rotula como esporte atual; parser nao encontrado | `diagnostics-d8-a8.md` |
| D8 diagnostico/fallback | nao encontrado no codigo analisado | nao confirmado | nao confirmado | `diagnostics-d8-a8.md` |

## Regras Para Champion Companion

- Todas as operacoes GATT futuras devem passar por fila serial unica.
- Telas nao devem acessar `BluetoothGatt` diretamente.
- Operacoes devem ter timeout explicito e retry limitado.
- Reconexao infinita nao e aceitavel para Champion Companion.
- BLE permanente e D1 automatico ao abrir nao sao requisitos futuros, mesmo existindo como comportamento MotionLog.

## Lacunas Para `000-14`

- Confirmar com dispositivo fisico se FEA2/FEC8 devem ser tratados como indications reais no app nativo.
- Confirmar propriedades GATT exatas de cada characteristic no Android nativo.
- Confirmar se F002 write tem uso real ou se deve permanecer fora do MVP.
- Confirmar nome/localName observado em scan fisico para evitar filtro estreito demais.
