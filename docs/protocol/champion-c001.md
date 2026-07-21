# Champion C001 - Catalogo BLE

## Objetivo

Catalogar o dispositivo Champion C001 conforme baseline MotionLog, com UUIDs, services, characteristics e limites conhecidos para futura implementacao Android nativa.

## Nivel De Confirmacao

- `Confirmado pelo codigo`: existe no codigo MotionLog analisado.
- `Confirmado no uso funcional atual`: consta como fluxo funcional da baseline ou documento MotionLog de validacao.
- `Confirmado fisicamente na PoC Android`: observado na execucao real do Champion Companion no Galaxy S22.
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

## Evidencia Fisica 2026-07-21

Primeira validacao exploratoria real da PoC Android no Galaxy S22 produziu o ciclo sanitizado `a86ae7d2-6b02-4890-a330-2babd26ae2c0` com sequencia `idle -> ready_to_scan -> scanning -> device_found -> connecting -> failed`.

Confirmacoes fisicas desta execucao:

- scan real encontrou um dispositivo Champion e levou a PoC ate `device_found`;
- tentativa de conexao falhou antes de `TransportReady`;
- `transport_ready=false`, `handshake_confirmed=false` e `error_category=incompatible_capability`;
- nenhuma capability opcional foi observada nesta execucao.

Leitura tecnica resultante:

- o achado fisico mostrou que a validacao antiga de propriedade GATT estava estreita demais para o canal de escrita principal;
- a PoC Android passou a tratar `F008_WRITE` e `F002_WRITE` como canais write-capable quando o Android anunciar `WRITE` ou apenas `WRITE_WITHOUT_RESPONSE`;
- isso corrige o bloqueio principal sem afirmar ainda qual property exata o firmware publica no hardware real, porque o relatorio sanitizado nao exporta bitmap de properties.

Segunda validacao real da mesma data produziu o ciclo sanitizado `859c7105-57d5-4667-8b77-461ba69084f8` com sequencia `idle -> permissions_required -> ready_to_scan -> scanning -> device_found -> connecting -> transport_ready -> handshaking -> failed`.

Confirmacoes fisicas adicionais desta segunda execucao:

- `transport_ready=true`;
- `steps_capability=available`;
- `heart_rate_capability=available`;
- falha migrou do transporte para a etapa de handshake;
- o relatorio exportado marcou `error_category=internal_failure`, mas a PoC Android foi ajustada para preservar erro especifico de espera do pacote, em vez de mascarar timeout/cancelamento como falha interna.

Terceira validacao real de 2026-07-21 produziu o ciclo sanitizado `a0971467-e876-4192-96c1-c0c306ed0b3e` com sequencia `idle -> ready_to_scan -> scanning -> device_found -> connecting -> transport_ready -> handshaking -> failed`.

Confirmacoes fisicas adicionais desta terceira execucao:

- `transport_ready=true`;
- `steps_capability=available`;
- `heart_rate_capability=available`;
- falha do A1 ficou confirmada como `error_category=timeout`, nao mais `internal_failure`.

Leitura tecnica adicional:

- o caminho de transporte continuou estavel entre a segunda e a terceira execucao;
- a divergencia restante ficou concentrada no handshake A1 e no timing da espera pela confirmacao;
- a PoC Android passou a aplicar atraso inicial curto e retry limitado por timeout para aproximar o comportamento do MotionLog background antes da proxima revalidacao fisica.

Quarta validacao real de 2026-07-21 produziu o ciclo sanitizado `bd57e034-aa94-43e1-987a-f0745ceea48b` com sequencia `idle -> ready_to_scan -> scanning -> device_found -> connecting -> transport_ready -> handshaking -> failed`.

Confirmacoes fisicas adicionais desta quarta execucao:

- `transport_ready=true`;
- `steps_capability=available`;
- `heart_rate_capability=available`;
- falha do A1 permaneceu `error_category=timeout` mesmo apos atraso inicial e retry.

Leitura tecnica adicional:

- a comparacao com `motionlog-app` mostrou que o bootstrap nativo ainda nao ativava todos os `defaultSubscribedChannels` antes do A1;
- o catalogo da PoC ja marcava `F008_NOTIFY`, `F002_NOTIFY` e `FEA1_NOTIFY` como subscriptions default, mas apenas `F008_NOTIFY` era ativado no connect;
- o connect foi corrigido para subir todos os `defaultSubscribedChannels` disponiveis antes de `TransportReady`.

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
- Confirmar propriedades GATT exatas de cada characteristic no Android nativo. Em 2026-07-21 houve falha fisica `incompatible_capability` antes de `TransportReady`; a PoC ja aceita `WRITE` e `WRITE_WITHOUT_RESPONSE`, mas a property exata ainda nao foi exportada em evidencias publicas.
- Confirmar se F002 write tem uso real ou se deve permanecer fora do MVP.
- Confirmar nome/localName observado em scan fisico para evitar filtro estreito demais.
- Revalidar fisicamente se atraso+retry no A1 eliminam o timeout observado no ciclo `a0971467-e876-4192-96c1-c0c306ed0b3e`.
- Revalidar fisicamente se subscriptions default completas eliminam o timeout observado no ciclo `bd57e034-aa94-43e1-987a-f0745ceea48b`.
