# Treinos D3/D4 - MotionLog Para Champion Companion

## Objetivo

Documentar importacao de treinos gravados no Champion C001: indice D3, download D4, remontagem, parser, persistencia e bloqueio de envio automatico.

## Fontes E Classificacao

- `../motionlog-app/src/lib/ble/champion-commands.ts`
- `../motionlog-app/src/lib/ble/watch-workout-sync.ts`
- `../motionlog-app/src/lib/ble/watch-workout-parser.ts`
- `../motionlog-app/src/lib/ble/watch-workout-import-adapter.ts`
- `../motionlog-app/src/lib/repositories/watch-workout-repository.ts`
- `../motionlog-app/src/lib/repositories/database/migrations.ts`
- `../motionlog-app/src/lib/health-connect/watch-workout-sync.ts`
- `../motionlog-app/src/models/watch-workout.ts`

Salvo indicacao contraria, informacoes deste documento sao `Confirmado pelo codigo`. Validacao fisica da Fase 10 em Galaxy S22 com Champion C001 confirmou fluxo funcional de importacao D3/D4, sem divergencia bloqueadora adicional de protocolo nos cenarios exercitados.

## Regra Obrigatoria Para Champion Companion

Treino importado por D3/D4 nasce `pending_review`. Nao enviar automaticamente ao Health Connect.

Envio manual somente depois de revisao e dados validos. No MotionLog, bloqueios adicionais incluem treino ja sincronizado, sincronizacao em andamento, sessao vinculada ausente e esteira sem calibracao confirmada.

## Transporte Compartilhado

| Item | Contrato atual |
| --- | --- |
| Escrita | F008 write com resposta |
| Resposta | F008 notify; listener filtra primeiro byte `D3` ou `D4` |
| Preparo | garantir conexao, assinar notificacoes principais, executar A1 se `handshakeOk` for falso |
| Execucao | foreground, um D3 seguido de D4 sequenciais |
| Retry | nenhum retry interno para D3/D4 |
| Timeout | D3: 10 s; D4: 15 s por bloco |
| Limpeza | remover listener especifico no `finally` |

Detalhes BLE gerais: `ble-transport.md`. Comandos gerais: `ble-command-mapping.md`.

## D3 - Indice De CRCs

### Requisicao

```text
D3 01 00 00
```

Finalidade: listar CRCs de blocos de treino disponiveis no relogio.

### Resposta E Conclusao

- Aceita somente notificacoes cujo primeiro byte seja `D3`.
- Parser le pares u16 little-endian a partir do offset 3.
- `0x0000` e ignorado.
- Cada CRC valido recebe `blockIndex` sequencial, pela ordem de coleta e aparicao; indice inicia em zero.
- Quando cabecalho multipart for plausivel (`totalPackets` entre 1 e 256, `packetIndex` entre 1 e total), encerra ao receber `packetIndex >= totalPackets`.
- Sem cabecalho multipart plausivel, primeira notificacao D3 encerra coleta.
- Timeout retorna CRCs ja coletados e `timedOut: true`; nao ha retry interno.

Contrato produzido:

```ts
type SportModelCrcEntry = {
  blockIndex: number;
  crc: number;
  rawHex: string; // bytes CRC na ordem recebida
};
```

`rawSessionId` identifica coleta em memoria/log. Codigo analisado nao o persiste como payload bruto duravel.

## D4 - Download E Remontagem De Bloco

### Requisicao

```text
D4 <blockIndex> 00 00
```

`blockIndex` ocupa byte menos significativo; construtor aplica `blockIndex & 0xff`. Origem do indice: ordem dos CRCs D3 validos, nao valor do CRC.

### Coleta

- Aceita somente notificacoes cujo primeiro byte seja `D4`.
- Cabecalho: `packetIndex = u16 LE bytes[1..2]`; `totalPackets = u16 LE bytes[3..4]`.
- Coleta cada pacote bruto.
- Encerra quando `totalPackets > 0` e `packetIndex >= totalPackets`.
- Ao finalizar, ordena por `packetIndex` crescente antes de parser.
- Timeout ou resposta vazia marca bloco como falho no orquestrador; timeout tambem chega ao parser.
- Erro de leitura de um bloco nao interrompe tentativa dos proximos CRCs; bloco entra em `timedOutBlocks`.

Contrato produzido:

```ts
type SportModelRawPacket = {
  packetIndex: number;
  totalPackets: number;
  rawBytes: Uint8Array;
};
```

Duplicatas de `packetIndex` nao sao removidas pela remontagem. Parser marca resultado `partial` quando quantidade de indices distintos diverge de `totalPackets`, ou amostras repetem `minuteIndex`.

## Contrato Do Parser D4

Entrada: pacotes D4 ordenados. Pacotes 1, 2 e 3 sao obrigatorios. Ausencia ou falha de parser em qualquer um retorna `null`, sem objeto parcialmente preenchido.

| Pacote | Campos e regra |
| --- | --- |
| 1 | inicio e fim. Layout preferido: inicio em offsets 7-13; fim em 14-19, anos u16 LE. Segundo final nao existe: usa zero. Se data preferida invalida, tenta layout legado. |
| 2 | totais: passos, distancia raw, calorias raw. Tenta layout observado: passos u16 LE [7..8], distancia u16 LE [11..12] multiplicada por 100 cm, calorias u24 LE [15..17]. Caso nao seja plausivel, usa layout legado. |
| 3 | `recordCount` e CRC. Layout observado usa `recordCount` byte 9 e CRC u16 LE [14..15]; senao usa layout legado com duracao u32 LE [5..8], contagem u16 LE [9..10], CRC u16 LE [11..12]. |
| 4+ | amostra por minuto: `minuteIndex = packetIndex - 4`; BPM byte 6; passos delta u24 LE [7..9]; distancia raw delta u24 LE [10..12]; calorias raw delta u16 LE [13..14]; bytes diagnosticos `sportCountRaw` e `pauseRaw`; hex bruto. |

Layout preferido e fallback legado sao `Confirmados pelo codigo como heuristicas`; offsets e semantica no dispositivo seguem `Ainda nao confirmados` sem fixtures reais.

### Campos Produzidos

| Campo solicitado | Origem e contrato |
| --- | --- |
| Data e horario | `startedAt` e `endedAt` do pacote 1. Duracao prefere diferenca inicio/fim; fallback duracao pacote 3. |
| Duracao | segundos; data final invalida ou nao posterior ao inicio usa inicio + duracao resolvida. |
| Passos | total do pacote 2; amostras guardam delta por minuto. |
| Distancia | total raw convertido de cm para metros: `round(raw / 100)`. |
| Distancia por intervalo | delta por minuto convertido pela mesma regra. |
| Calorias | total raw convertido: `caloriesRaw / 1000`, arredondado para uma casa decimal. Confirmado pelo codigo. |
| BPM | amostra por minuto; aceita somente 30 a 220. `0`, `255` e valores fora da faixa viram `null`. |
| BPM por intervalo | valor valido de cada amostra D4. |
| BPM medio, minimo e maximo | calculados apenas sobre BPMs numericos validos; sem BPM valido, tres campos `null`. |
| Tipo | sugestao: `running` se passos totais >= 1000; senao `free`. Sugestao, nao confirmacao de tipo. Usuario revisa/edita antes de enviar. |

Somas de amostras contra totais sao diagnostico: passos exatos, distancia com tolerancia 3 cm, calorias raw com tolerancia 1. Divergencia nao muda sozinha `validationStatus`.

### Dados Incompletos

`validationStatus` vira `partial` quando houve timeout, quantidade de indices D4 distintos nao coincide com total declarado, ha `minuteIndex` duplicado ou quantidade de amostras diverge de `recordCount`. Caso contrario vira `valid`.

Status `partial` bloqueia sincronizacao manual. Pacotes obrigatorios ausentes ou invalidos retornam `null`; chamador deve registrar falha sem inventar atividade.

## Persistencia, Deduplicacao E Payload Bruto

MotionLog persiste importacao em `watch_workout_imports` e amostras em `watch_workout_samples`.

- Chave unica de importacao: `(device_id, crc, source)`; source atual `champion_c001_d3_d4`.
- Chave unica de amostra: `(workout_import_id, minute_index)`.
- Importacao guarda CRC, bloco externo, sugestao, review, validacao, estado Health Connect, totais, resumo BPM e `raw_hex` com todos pacotes D4 unidos por ` | `.
- Cada amostra guarda BPM, passos, distancia, calorias em kcal e raw, bytes diagnosticos e `raw_hex` individual.
- Novo import recebe `pending_review` e `not_synced`; transacao nao enfileira Health Connect.
- Reimportacao do mesmo `(device, CRC, source)` atualiza dados. Se ja revisado, preserva tipo escolhido pelo usuario na sessao vinculada.
- Com amostras novas, repositorio apaga amostras anteriores daquele import e insere conjunto novo, dentro de transacao exclusiva. Esta limpeza e confirmada pelo codigo.

## D8 E A8 Relacionados

| Comando | Evidencia atual | Relacao com D3/D4 |
| --- | --- | --- |
| D8 | Nao encontrado em comandos, parser ou fluxo MotionLog analisados | Ainda nao confirmado; sem associacao documentavel com treinos. |
| A8 | Constante `SPORT_CURRENT`: `A8 00`; sem parser ou fluxo funcional | Pode representar esporte atual apenas como rotulo de prototipo/documentacao; nao substitui nem complementa D3/D4 no codigo analisado. |

## Lacunas Para `000-14`

- Expandir fixtures D3/D4 reais versionadas: vazio, multipacote, CRC repetido, bloco completo, timeout, pacote fora de ordem e pacote duplicado.
- Confirmar layouts preferido e legado D4, unidades de todos campos e significado de `sportCountRaw`, `pauseRaw` e assinatura `data`.
- Confirmar se CRC D3 corresponde sempre ao CRC pacote 3 D4 e como tratar divergencia.
- Confirmar semantica, payload e uso de D8; confirmar se A8 complementa D3/D4 em algum firmware.
- Definir retencao e limites de payload bruto no novo banco, inclusive limpeza apos diagnostico.
- Validar heuristica de tipo com dados reais; manter como sugestao revisavel ate entao.
- Confirmar politica final para bloco D4 `partial`: persistir para revisao, exibir diagnostico ou descartar.

## Referencias

- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/specs/_inputs/phase-zero-original-requirements.md`, requisitos 24, 25 e 40-49.
