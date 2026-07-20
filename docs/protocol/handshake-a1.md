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

Lacuna: timeout final do handshake no Champion Companion nativo deve ser definido na implementacao futura.

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
- Usar F008 write com response.
- Considerar handshake confirmado apenas por F008 notify `A1` com byte 19 igual a `01`.
- Limpar estado de handshake em disconnect.
- Definir timeout explicito no comando A1 da nova camada BLE.
- Nao tratar sucesso de escrita como sucesso de handshake no fluxo foreground.

## Lacunas Para `000-14`

- Confirmar fisicamente o conteudo completo da resposta A1 alem de `bytes[0]` e `bytes[19]`.
- Confirmar comportamento de senha incorreta e se senha diferente de `0000` deve existir no Champion Companion.
- Definir timeout e retry finais do A1 no app nativo.
- Confirmar se o background Kotlin atual deveria aguardar ack A1 antes de D1 ou se o firmware aceita D1 apos escrita bem-sucedida.
