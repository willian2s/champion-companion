# Handshake A1 - Champion C001

## Objetivo

Documentar o contrato do handshake A1 usado pelo MotionLog para liberar fluxos BLE do Champion C001.

## Fontes

- `../motionlog-app/src/lib/ble/champion-handshake.ts`
- `../motionlog-app/src/lib/ble/champion-client.ts`
- `../motionlog-app/src/lib/ble/champion-runtime.ts`
- `../motionlog-app/src/lib/ble/heart-history-sync.ts`
- `../motionlog-app/android/app/src/main/java/com/motionlog/app/MotionLogBackgroundBleService.kt`
- `../motionlog-app/docs/specs/003-ble-foreground.md`
- `../motionlog-app/docs/integrations/champion-c001-ble-protocol.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`

## Resumo Do Contrato

Nenhum detalhe deste documento foi classificado como `Confirmado no uso funcional atual`, porque esta task nao executou app, APK ou dispositivo fisico.

| Campo | Valor | Classificacao |
| --- | --- | --- |
| Comando | `A1` | Confirmado pelo codigo |
| Finalidade | handshake/senha e sincronizacao basica de data/hora/timezone antes de leituras | Inferido a partir do codigo |
| Service de escrita | `f0080001-0451-4000-b000-000000000000` | Confirmado pelo codigo |
| Characteristic de escrita | `f0080003-0451-4000-b000-000000000000` | Confirmado pelo codigo |
| Tipo de escrita foreground | `writeCharacteristicWithResponseForDevice` | Confirmado pelo codigo |
| Characteristic de resposta | `f0080002-0451-4000-b000-000000000000` notify | Confirmado pelo codigo |
| Tamanho do payload enviado | 20 bytes | Confirmado pelo codigo |
| Senha usada no app | `0000` | Confirmado pelo codigo |
| Condicao de sucesso | F008 notify com tamanho minimo 20, `bytes[0] == 0xA1` e `bytes[19] == 0x01` | Confirmado pelo codigo |

## Evidencia Fisica 2026-07-21

Na primeira validacao real da PoC Android, o fluxo nao chegou a A1. O ciclo sanitizado `a86ae7d2-6b02-4890-a330-2babd26ae2c0` falhou antes de `TransportReady` com `error_category=incompatible_capability`.

Consequencia pratica:

- A1 ainda nao tem confirmacao fisica no Champion Companion Android;
- o principal achado tecnico foi anterior ao protocolo A1: o canal `F008_WRITE` precisava aceitar characteristic Android anunciada como write-capable mesmo sem `PROPERTY_WRITE` estrita;
- a PoC foi ajustada para aceitar `WRITE` ou `WRITE_WITHOUT_RESPONSE` no canal logico de escrita e revalidacao fisica permanece necessaria.

Na segunda validacao real de 2026-07-21, o ciclo `859c7105-57d5-4667-8b77-461ba69084f8` atingiu `transport_ready` e entrou em `handshaking`, mas ainda falhou sem `handshake_confirmed`.

Nova leitura tecnica:

- A1 agora tem pre-condicoes fisicas confirmadas no Champion Companion Android: conexao, discovery e capabilities opcionais observadas;
- a categoria exportada `internal_failure` foi isolada como defeito do `ChampionPacketRouter`, que podia mascarar timeout/cancelamento do await da resposta A1;
- a PoC Android foi ajustada para preservar o erro especifico do await, o que deve tornar a proxima evidência fisica mais precisa;
- a confirmacao fisica do pacote A1 segue pendente.

Na terceira validacao real de 2026-07-21, o ciclo `a0971467-e876-4192-96c1-c0c306ed0b3e` repetiu `transport_ready -> handshaking -> failed`, desta vez com `error_category=timeout`.

Nova leitura tecnica apos o terceiro ciclo:

- o await da resposta A1 realmente expirou no hardware testado;
- transporte e discovery continuam confirmados fisicamente antes da falha;
- a PoC Android alinhou o A1 ao timing observado no MotionLog background: atraso inicial de `1_000 ms` antes da primeira tentativa e retry limitado a 3 tentativas quando a falha for timeout;
- a confirmacao fisica do notify `A1 ... 01` continua pendente.

Na quarta validacao real de 2026-07-21, o ciclo `bd57e034-aa94-43e1-987a-f0745ceea48b` repetiu `transport_ready -> handshaking -> failed` com `error_category=timeout`, mesmo apos atraso inicial e retry.

Nova leitura tecnica apos o quarto ciclo:

- timing sozinho nao explicou a falha do A1;
- ao comparar com `motionlog-app`, foi identificado que o app de referencia sobe `FEA1_NOTIFY`, `F008_NOTIFY` e `F002_NOTIFY` antes do A1;
- a PoC Android nativa ainda ativava apenas `F008_NOTIFY` no connect, embora `defaultSubscribedChannels` ja catalogasse os tres canais;
- o bootstrap do connect foi corrigido para ativar todos os `defaultSubscribedChannels` disponiveis antes de declarar `TransportReady`.

## Fluxo Foreground

1. Conectar ao device Champion.
2. Descobrir services e characteristics.
3. Assinar FEA1 notify, F008 notify e F002 notify.
4. Enviar A1 em F008 write quando `handshakeOk` ainda for falso.
5. Atualizar `handshakeOk` para verdadeiro somente ao receber confirmacao em F008 notify.
6. Prosseguir com comandos como D0, D1, D3 e D4.

Classificacao: `Confirmado pelo codigo`.

## Fluxo Background MotionLog

1. Apos descoberta de services, o servico agenda handshake com atraso inicial de `1_000 ms`.
2. Envia A1 em F008 write.
3. Se a escrita falhar, tenta novamente ate 3 vezes.
4. O intervalo entre tentativas e `1_500 ms`.
5. Se exceder tentativas, marca estado degradado e agenda reconexao.
6. Se a escrita retorna sucesso, agenda ciclo D1 apos `1_000 ms`.

Classificacao: `Confirmado pelo codigo`.

Observacao: no background Kotlin lido nesta task, a transicao para ciclo D1 depende do sucesso da escrita A1. Nao foi encontrada espera explicita pela confirmacao `A1 ... 01` antes de agendar D1.

Classificacao da observacao: `Confirmado pelo codigo`.

## Payload Enviado

Payload de 20 bytes:

```text
Offset  Conteudo
0       0xA1
1       senha low byte
2       senha high byte
3       0x00
4       ano high byte
5       ano low byte
6       mes, 1-12
7       dia do mes
8       hora, 0-23
9       minuto
10      segundo
11      0x01
12      0x01
13      timezone em blocos de 15 minutos, signed pelo byte bruto
14      0x00
15-19   0x00
```

Classificacao: `Confirmado pelo codigo`.

## Endianess E Escalas

| Campo | Regra | Classificacao |
| --- | --- | --- |
| Senha | `uint16` little-endian em bytes 1-2 | Confirmado pelo codigo |
| Ano | big-endian em bytes 4-5 | Confirmado pelo codigo |
| Timezone | offset UTC em minutos dividido por 15 e truncado para byte | Confirmado pelo codigo |
| Data/hora | valores locais do sistema no momento da montagem do payload | Confirmado pelo codigo |

No JavaScript, timezone usa `Math.round((-now.getTimezoneOffset()) / 15)`. No Kotlin, usa `TimeZone.getDefault().getOffset(now.timeInMillis) / 60_000`, dividido por 15 e arredondado. Ambos gravam apenas o byte baixo.

Classificacao: `Confirmado pelo codigo`.

## Senha

O builder aceita senha textual, faz `parseInt` em base 10 e limita minimo em zero. Valores invalidos viram zero. O MotionLog chama `sendHandshakeA1Async('0000')` em conexao, tela de saude e inicio de exercicio.

Classificacao: `Confirmado pelo codigo`.

Lacuna: nao ha documentacao confirmando politica de senha diferente de `0000`, limite maximo aceito pelo firmware ou comportamento de senha incorreta.

Classificacao da lacuna: `Ainda nao confirmado`.

## Resposta Esperada

Confirmacao aceita:

```text
bytes.length >= 20
bytes[0] == 0xA1
bytes[19] == 0x01
```

O MotionLog nao valida demais bytes da resposta A1.

Classificacao: `Confirmado pelo codigo`.

## Timeout, Retry E Cancelamento

| Contexto | Timeout | Retry | Cancelamento/cleanup | Classificacao |
| --- | --- | --- | --- | --- |
| Foreground `sendHandshakeA1Async` | sem timeout proprio no metodo | sem retry interno | erro da escrita propaga para chamador | Confirmado pelo codigo |
| Conexao foreground via runtime | handshake enviado apos subscribe; falha fica em `lastError`/toast conforme tela | sem retry especifico do A1 | `disconnectAsync` remove subscriptions, cancela conexao e zera `handshakeOk` | Confirmado pelo codigo |
| Background Kotlin | sem espera de ack A1; escrita tenta ate 3 vezes | 3 tentativas, `1_500 ms` entre elas | stop monitoring remove runnable de handshake e fecha GATT | Confirmado pelo codigo |
| Champion Companion PoC apos revalidacoes de 2026-07-21 | timeout explicito de 15s por tentativa | atraso inicial de `1_000 ms`; retry em timeout ate 3 tentativas com `1_500 ms` entre elas | connect ativa todos os `defaultSubscribedChannels` disponiveis; disconnect/reset limpam sessao e invalidam handshake | Confirmado pelo codigo |

Lacuna: ainda falta confirmar fisicamente se subscriptions default completas destravam o A1 ou se o firmware exige outra condicao alem de timing e bootstrap de notify.

Classificacao da lacuna: `Ainda nao confirmado`.

## Erros

- Sem device conectado: escrita F008 foreground falha com `No connected device for write`.
- Sem notificacoes: fluxo de UI tenta `subscribeCoreNotificationsAsync`; falha orienta reconectar.
- Falha de escrita: foreground propaga erro; background marca estado degradado apos retries.
- Desconexao: estado limpa `handshakeOk`; proxima sessao deve refazer A1.

Classificacao: `Confirmado pelo codigo`.

## Objeto/Estado Produzido

O handshake nao produz registro persistido. Produz estado em memoria:

```text
ChampionClientState.handshakeOk = true
```

Esse estado habilita fluxos subsequentes na UI e evita reenviar A1 em algumas chamadas quando ja confirmado.

Classificacao: `Confirmado pelo codigo`.

## Requisitos Originais Atendidos

- `21. Handshake A1.`

## Regras Para Champion Companion

- Enviar A1 somente depois de discovery e notificacoes principais ativas.
- Garantir `defaultSubscribedChannels` completos antes do A1 quando esses canais estiverem disponiveis.
- Usar F008 write com response.
- Considerar handshake confirmado apenas por F008 notify `A1` com byte 19 igual a `01`.
- Limpar estado de handshake em disconnect.
- Aplicar atraso inicial curto e retry limitado quando o await do A1 expirar.
- Nao tratar sucesso de escrita como sucesso de handshake no fluxo foreground.

## Lacunas Para `000-14`

- Confirmar fisicamente o conteudo completo da resposta A1 alem de `bytes[0]` e `bytes[19]`.
- Confirmar comportamento de senha incorreta e se senha diferente de `0000` deve existir no Champion Companion.
- Confirmar fisicamente se a politica atual de timeout, retry e subscriptions default do A1 e suficiente no app nativo.
- Confirmar se o background Kotlin atual deveria aguardar ack A1 antes de D1 ou se o firmware aceita D1 apos escrita bem-sucedida.
- Confirmar se o hardware real expoe `F008_WRITE` como `WRITE`, `WRITE_WITHOUT_RESPONSE` ou ambos no Android nativo.
