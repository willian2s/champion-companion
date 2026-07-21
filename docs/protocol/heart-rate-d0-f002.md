# Heart Rate D0/F002 - Champion C001

## Objetivo

Documentar medicao manual de BPM, comandos D0, stream F002, valor em tempo real e valor final produzido pelo MotionLog.

## Fontes

- `../motionlog-app/src/lib/ble/champion-commands.ts`
- `../motionlog-app/src/lib/ble/champion-client.ts`
- `../motionlog-app/src/lib/ble/champion-heart-session.ts`
- `../motionlog-app/src/lib/ble/champion-bpm-estimator.ts`
- `../motionlog-app/src/lib/repositories/heart-rate-measurement-repository.ts`
- `../motionlog-app/src/models/heart-rate-measurement.ts`
- `../motionlog-app/src/app/(tabs)/health/index.tsx`
- `../motionlog-app/src/app/(tabs)/exercises/start.tsx`
- `../motionlog-app/src/app/(tabs)/exercises/active.tsx`
- `../motionlog-app/docs/specs/003-ble-foreground.md`
- `../motionlog-app/docs/specs/007-manual-workouts.md`
- `../motionlog-app/docs/integrations/champion-c001-ble-protocol.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`

## Resumo Do Contrato

Nenhum detalhe deste documento foi classificado como `Confirmado no uso funcional atual`, porque esta task nao executou app, APK ou dispositivo fisico.

| Item | Valor | Classificacao |
| --- | --- | --- |
| Start manual | `D0 01` | Confirmado pelo codigo |
| Stop manual | `D0 00` | Confirmado pelo codigo |
| Service de escrita | `f0080001-0451-4000-b000-000000000000` | Confirmado pelo codigo |
| Characteristic de escrita | `f0080003-0451-4000-b000-000000000000` | Confirmado pelo codigo |
| Resposta D0 | F008 notify com `bytes[0] == 0xD0` | Confirmado pelo codigo |
| Stream bruto | F002 notify `f0020002-0451-4000-b000-000000000000` | Confirmado pelo codigo |
| Parser F002 | pares `uint16` little-endian | Confirmado pelo codigo |
| Estimador | picos no sinal F002 local | Confirmado pelo codigo |
| Valor em tempo real | primeiro byte em resposta D0 entre 35 e 220, salvo em `lastD0Bpm` | Confirmado pelo codigo |
| Valor final manual | `lastD0Bpm` preferido; senao estimativa F002; senao `null` | Confirmado pelo codigo |

## Evidencia Fisica 2026-07-21

A primeira validacao real da PoC Android nao chegou a `D0 01`. O ciclo sanitizado `a86ae7d2-6b02-4890-a330-2babd26ae2c0` falhou antes de `TransportReady`, com `handshake_confirmed=false` e `error_category=incompatible_capability`.

Leitura tecnica:

- a falha bloqueou observacao fisica de D0 e F002 nesta data;
- o ajuste aplicado no app passou a aceitar `F008_WRITE` como canal compativel quando a characteristic Android anunciar `WRITE` ou `WRITE_WITHOUT_RESPONSE`;
- D0 e F002 continuam sem confirmacao fisica no Champion Companion Android ate nova execucao dirigida.

Na segunda validacao real de 2026-07-21, o ciclo `859c7105-57d5-4667-8b77-461ba69084f8` confirmou `transport_ready=true`, `steps_capability=available` e `heart_rate_capability=available`, mas ainda falhou durante o handshake. Portanto:

- F002 notify ja tem disponibilidade fisica confirmada como capability no Galaxy S22 com Champion C001;
- D0 e amostras F002 ainda nao tiveram observacao funcional, porque o fluxo continua bloqueado antes do inicio da sessao de batimento.

Na terceira validacao real de 2026-07-21, o ciclo `a0971467-e876-4192-96c1-c0c306ed0b3e` repetiu `transport_ready=true`, `handshake_confirmed=false`, `heart_rate_capability=available` e falhou com `error_category=timeout`. Portanto:

- F002 notify segue confirmado fisicamente como capability disponivel;
- D0 e amostras F002 continuam sem observacao funcional, porque o bloqueio permaneceu restrito ao A1;
- a PoC Android passou a aplicar atraso inicial curto e retry limitado no handshake antes da proxima revalidacao fisica da sessao de BPM.

## Fluxo De Medicao Manual Na Tela Saude

1. Usuario toca iniciar.
2. UI exige Champion conectado.
3. Se notificacoes nao estao ativas, chama `subscribeCoreNotificationsAsync`.
4. Se `handshakeOk` esta falso, envia A1 com senha `0000`.
5. Cria `ChampionHeartRateSession` com `durationSeconds: 60` e `maxPackets: 5000`.
6. Envia `D0 01` em F008 write.
7. Durante sessao, F002 e ingerido no buffer da sessao.
8. F008 D0 pode atualizar `lastD0Bpm` e `lastD0At`.
9. UI mostra BPM ao vivo apenas quando `lastD0At >= sessionStartedAt`.
10. Auto-stop ocorre ao fim da duracao ou por guarda de expiracao.
11. Stop chama `stopHeartRateSessionAsync(true)`, calcula estimativa F002 e envia `D0 00`.
12. UI salva `heart_rate_measurements`.

Classificacao: `Confirmado pelo codigo`.

## Fluxo Durante Exercicio Manual

1. Ao iniciar exercicio, se houver device conectado, UI garante notifications e A1.
2. Inicia sessao de batimento com `durationSeconds: 4 * 60 * 60`.
3. Envia D0 start pela sessao e tambem chama `startHeartRateAsync`.
4. Tela ativa coleta `lastD0Bpm` novo e adiciona a timeline do exercicio.
5. Ao finalizar, se sessao heart-rate esta ativa, chama `stopHeartRateSessionAsync(true, false)` e depois `stopHeartRateAsync`.
6. Calcula `avgBpm`, `maxBpm` e `minBpm` a partir das amostras D0 coletadas.

Classificacao: `Confirmado pelo codigo`.

Observacao: a chamada dupla de start/stop D0 durante exercicio manual existe no codigo atual. Nao ha evidencia nesta task de que seja exigencia do firmware.

Classificacao da observacao: `Confirmado pelo codigo` para existencia; `Ainda nao confirmado` para necessidade.

## Payloads

### D0 Start

```text
D0 01
```

Classificacao: `Confirmado pelo codigo`.

### D0 Stop

```text
D0 00
```

Classificacao: `Confirmado pelo codigo`.

### F002 Notify

F002 nao tem comando de escrita identificado no app atual. O stream chega pela characteristic notify:

```text
f0020002-0451-4000-b000-000000000000
```

Cada pacote e decodificado em pares:

```text
value = bytes[index] | (bytes[index + 1] << 8)
index += 2
```

Byte final impar, se existir, e ignorado porque o loop exige `index + 1 < bytes.length`.

Classificacao: `Confirmado pelo codigo`.

## Endianess, Offsets E Escalas

| Campo | Regra | Classificacao |
| --- | --- | --- |
| D0 codigo | offset 0, byte `0xD0` | Confirmado pelo codigo |
| D0 start/stop | offset 1, `0x01` para iniciar, `0x00` para parar | Confirmado pelo codigo |
| D0 BPM | primeiro byte apos offset 0 cujo valor esteja em 35..220 | Confirmado pelo codigo |
| F002 amostra | `uint16` little-endian por par de bytes | Confirmado pelo codigo |
| F002 escala fisica | valor bruto sem unidade confirmada | Ainda nao confirmado |
| BPM estimado | `round(60 / intervaloMedianoEntrePicos)` | Confirmado pelo codigo |
| BPM valido D0 | 35..220 | Confirmado pelo codigo |
| BPM valido estimado | 35..220 | Confirmado pelo codigo |

## Estimador F002

Entrada:

- amostras positivas vindas dos pacotes F002;
- duracao da sessao em segundos.

Regras principais:

- retorna `null` com menos de 250 amostras positivas;
- calcula `sampleRate = positiveSamples.length / durationSeconds`;
- descarta inicio do sinal: menor valor entre 25% das amostras e 6 segundos;
- exige pelo menos 250 amostras restantes;
- remove baseline por media movel;
- suaviza sinal;
- detecta picos acima de `mean + std * 0.35`;
- exige distancia minima entre picos de `sampleRate * 0.4`;
- aceita intervalos entre picos de `0.4..1.6 s`;
- exige pelo menos 3 intervalos validos para calcular BPM;
- usa mediana dos intervalos para BPM;
- invalida BPM fora de 35..220.

Classificacao: `Confirmado pelo codigo`.

## Qualidade Do Sinal

Pontuacao inicia em 100 e perde pontos:

| Condicao | Penalidade | Classificacao |
| --- | --- | --- |
| Sem BPM calculado | -50 | Confirmado pelo codigo |
| Menos de 2500 amostras positivas | -25 | Confirmado pelo codigo |
| Duracao menor que 20s | -20 | Confirmado pelo codigo |
| Menos de 12 picos | -20 | Confirmado pelo codigo |
| `intervalCv > 0.28` | -30 | Confirmado pelo codigo |
| `intervalCv > 0.18` | -15 | Confirmado pelo codigo |
| `dynamicRange < 250` | -15 | Confirmado pelo codigo |

Classificacao final:

- `boa`: score >= 75;
- `media`: score >= 45;
- `baixa`: score < 45.

Classificacao: `Confirmado pelo codigo`.

## Valor Em Tempo Real Vs Valor Final

### Valor Em Tempo Real

O valor em tempo real exibido na tela de saude vem de F008 D0:

```text
lastD0Bpm = primeiro byte D0 em 35..220
lastD0At = Date.now()
```

A tela mostra esse valor durante sessao somente se ele foi recebido depois de `sessionStartedAt`.

Classificacao: `Confirmado pelo codigo`.

### Valor Final Da Medicao Manual

Ao parar a medicao manual na tela de saude:

```text
estimatedBpm = result.estimate?.bpm ?? null
d0Bpm = lastD0BpmRef.current
finalBpm = d0Bpm ?? estimatedBpm ?? null
```

O registro salvo usa:

- `bpm`: `finalBpm`;
- `signalQuality`: qualidade da estimativa F002 ou `baixa`;
- `qualityScore`: score da estimativa F002 ou `0`;
- `sampleRate`, `peaks`, `durationSeconds`: metricas da estimativa F002 quando existem;
- `source`: `champion_c001`;
- `rawSessionId`: id da sessao local;
- `rawHex`: nao preenchido no fluxo manual atual.

Classificacao: `Confirmado pelo codigo`.

Risco: se `lastD0BpmRef` contem valor antigo e nao for limpo antes da nova sessao, o codigo da tela de saude prefere esse valor no final. A UI ao vivo filtra por timestamp, mas a selecao final usa apenas o ref. Nao foi confirmado nesta task se outro ponto do runtime limpa `lastD0Bpm` ao iniciar sessao.

Classificacao do risco: `Inferido a partir do codigo`.

## Timeout, Retry, Cancelamento E Erro

| Tema | Comportamento MotionLog | Classificacao |
| --- | --- | --- |
| Duracao padrao saude | 60s | Confirmado pelo codigo |
| Limite de parada manual | UI bloqueia stop por 10s apos inicio | Confirmado pelo codigo |
| Auto-stop | timeout agendado pelo fim da duracao e guarda a cada 1s | Confirmado pelo codigo |
| Buffer maximo saude | 5000 pacotes F002 | Confirmado pelo codigo |
| Duracao exercicio manual | 4 horas | Confirmado pelo codigo |
| Retry D0 | sem retry interno | Confirmado pelo codigo |
| Falha ao iniciar sessao | para sessao local, salva erro e propaga | Confirmado pelo codigo |
| Stop com envio D0 | `stopHeartRateSessionAsync(true)` envia D0 stop em fire-and-forget e salva erro se falhar | Confirmado pelo codigo |
| Stop sem sessao ativa | tela pode enviar `D0 00` direto se houver device conectado | Confirmado pelo codigo |
| Desconexao | `disconnectAsync` remove subscriptions e limpa `heartRateSession`, `lastD0Bpm`, `lastD0At` | Confirmado pelo codigo |

## Objeto Produzido

### Medicao Manual

Tabela/objeto `HeartRateMeasurement`:

```text
id
measuredAt
bpm
deviceId
signalQuality
qualityScore
sampleRate
peaks
durationSeconds
source
rawSessionId
rawHex
healthConnectSyncedAt
healthConnectRecordId
healthConnectSyncStatus
healthConnectLastError
updatedAt
createdAt
```

Classificacao: `Confirmado pelo codigo`.

### Exercicio Manual

Durante exercicio, D0 alimenta amostras em memoria (`bpmSamples` e `bpmTimeline`). Ao finalizar, sessao salva estatisticas agregadas (`avgBpm`, `maxBpm`, `minBpm`) e amostras derivadas para treino.

Classificacao: `Confirmado pelo codigo`.

## Requisitos Originais Atendidos

- `22. D0.`
- `28. F002.`
- `34. Medicao manual de BPM.`
- `35. Stream de BPM.`
- `36. Valor final da medicao.`

## Regras Para Champion Companion

- Comando D0 deve passar pela camada BLE/protocolo, nao por tela.
- Assinar F002 antes de iniciar medicao.
- Garantir A1 confirmado antes de D0 no fluxo manual.
- Armazenar stream F002 somente em memoria durante sessao manual.
- Separar explicitamente:
  - leitura ao vivo vinda de D0/F008;
  - estimativa local vinda de F002;
  - valor final salvo.
- Limpar valor D0 anterior ao iniciar nova medicao ou validar timestamp na selecao final.
- Definir timeout e politica de retry no protocolo nativo.

## Lacunas Para `000-14`

- Confirmar pacote real D0 de start e stop no Champion C001 fisico.
- Confirmar se D0 sempre retorna BPM final/confiavel ou se pode retornar status/zeros.
- Confirmar escala fisica do F002 bruto.
- Confirmar se F002 exige D0 start ou se pode emitir fora de sessao.
- Definir regra final de preferencia entre D0 e estimativa F002 para evitar valor antigo.
- Definir timeout/retry oficiais do D0 no Champion Companion.
- Coletar fixtures: `heart-rate-d0-start`, `heart-rate-f002-stream`, `heart-rate-d0-stop`.
