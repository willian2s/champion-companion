# Histórico D1 de Frequência Cardíaca

## Objetivo

Documentar consulta D1, parser, persistência, deduplicação e fronteira correta entre fila local `PENDING` da Fase 09 e envio integral futuro de BPM válido ao Health Connect. Referência: MotionLog na baseline registrada em `docs/baseline/current-baseline.md`.

## Fontes

- `../motionlog-app/src/lib/ble/champion-commands.ts`
- `../motionlog-app/src/lib/ble/heart-history-sync.ts`
- `../motionlog-app/src/lib/ble/heart-history-parser.ts`
- `../motionlog-app/src/lib/ble/champion-runtime.ts`
- `../motionlog-app/android/app/src/main/java/com/motionlog/app/MotionLogBackgroundBleService.kt`
- `../motionlog-app/src/lib/background-ble/index.ts`
- `../motionlog-app/src/lib/repositories/heart-rate-measurement-repository.ts`
- `../motionlog-app/src/lib/repositories/database/migrations.ts`
- `../motionlog-app/src/lib/health-connect/index.ts`
- `../motionlog-app/src/lib/health-connect/sync.ts`

Salvo indicação contrária, classificação: `Confirmado pelo código`.

## Comando E Transporte

| Item | Contrato | Classificação |
| --- | --- | --- |
| Finalidade | Obter histórico automático de BPM do dia, identificado como `D1_AUTO_HISTORY`. | Confirmado pelo código |
| Escrita | `D1 01 00 00` (`ORIGINAL_TODAY`). | Confirmado pelo código |
| Characteristic de escrita | F008 write. | Confirmado pelo código |
| Resposta | Notifications F008 cujo byte 0 é `D1`. | Confirmado pelo código |
| Preparação foreground | Garantir conexão, assinar notificações centrais; enviar A1 se handshake ainda não está confirmado; adicionar listener F008 específico; escrever D1. | Confirmado pelo código |
| Conclusão | Pacote válido com `totalPackets > 0` e `packetIndex >= totalPackets`. | Confirmado pelo código |
| Timeout foreground | 15 000 ms padrão. | Confirmado pelo código |
| Timeout background MotionLog | 12 000 ms. | Confirmado pelo código |
| Retry foreground | Não há retry interno D1. Runtime bloqueia execução concorrente e limita nova tentativa bem-sucedida por 60 s. | Confirmado pelo código |
| Background MotionLog | Serviço inicia D1 em ciclos; após conclusão ou timeout agenda próximo ciclo pelo intervalo configurado. Este comportamento não é requisito para app nativo futuro. | Confirmado pelo código |

Fluxo foreground:

1. `ChampionRuntime.connectToDeviceAsync` conecta, assina, faz A1 e dispara D1 de modo assíncrono.
2. `syncHeartRateHistoryFromDeviceAsync` recusa concorrência, reanexa dispositivo salvo se necessário e chama `syncHeartRateHistoryFromDeviceAsync`.
3. Coletor aplica parser a cada notification F008; pacotes inválidos são ignorados.
4. Ao completar ou vencer timeout, listener é removido.
5. Pontos são ordenados/deduplicados, persistidos individualmente e fila Health Connect é processada até 100 itens.

`timedOut` é retornado pelo coletor, mas runtime foreground não persiste esse estado nem diferencia timeout de conclusão ao retornar sucesso: retorno é verdadeiro quando recebeu ao menos um ponto válido. Classificação: `Confirmado pelo código`.

## Contrato Do Parser D1

Entrada: `Uint8Array` recebido em F008. Pacote deve ter pelo menos 20 bytes. Bytes além de 20 são preservados apenas em `rawHex`; parser não lhes atribui semântica.

| Offset | Campo | Leitura | Regra |
| --- | --- | --- | --- |
| 0 | Código | `u8` | Deve ser `0xD1`. |
| 1-2 | `packetIndex` | `u16` little-endian: `b1 | b2 << 8` | Usado para ordem, deduplicação de coleta e conclusão. |
| 3-4 | `totalPackets` | `u16` little-endian: `b3 | b4 << 8` | Usado para conclusão; zero não conclui fluxo. |
| 5 | Hora | `u8` | Deve estar entre 0 e 23. |
| 14 | BPM | `u8` | Deve estar entre 30 e 220, inclusivo. Escala: 1 BPM por unidade. |
| 19 | Minuto | `u8` | Deve estar entre 0 e 59. |

Campos entre offsets 6-13 e 15-18: `Ainda não confirmado`. Código atual não os lê; não atribuir data, segundo, checksum, escala ou significado.

Transformação válida:

1. Copiar `baseDate`, padrão `new Date()` no início da sincronização.
2. Aplicar hora, minuto, segundo zero e milissegundo zero com APIs locais de data.
3. Produzir `timestamp` em epoch ms, `dateTime` ISO, BPM, `packetIndex`, `totalPackets`, `source = D1_AUTO_HISTORY` e hexadecimal bruto.

Timezone: parser JavaScript constrói data no timezone local do aparelho e depois serializa em ISO UTC. Serviço Kotlin background usa `Calendar.getInstance()` local e `Instant` para ISO UTC. Não há offset timezone no payload D1 confirmado pelo código. Data do calendário também não vem de offsets D1 confirmados: `baseDate`/data atual supre ano, mês e dia.

Erros/invalidos: tamanho menor que 20, byte 0 diferente de `D1`, hora/minuto fora das faixas, BPM fora de 30-220 ou data inválida retornam `null`. Coletor ignora `null`; nenhum erro de protocolo específico é exposto.

## Validação De BPM

Validação D1 canônica, usada por parser foreground e Kotlin background: BPM inteiro do byte 14 entre 30 e 220, inclusivo. Pontos fora dessa faixa não são persistidos por esses parsers.

Camada JS que recebe evento do serviço background apenas exige número finito maior que zero e arredonda antes de persistir. Essa regra é mais permissiva, mas eventos emitidos pelo serviço já passaram pelo parser 30-220. Classificação: `Confirmado pelo código`.

Validação de escrita Health Connect exige apenas BPM finito maior que zero e timestamp parseável. Para D1 normal, proteção principal permanece parser 30-220 anterior à persistência. Classificação: `Confirmado pelo código`.

## Deduplicação E Persistência

Deduplicação ocorre em camadas:

| Camada | Chave/regra | Efeito | Classificação |
| --- | --- | --- | --- |
| Coletor foreground D1 | `${packetIndex}:${totalPackets}:${timestamp}` | `Map` mantém último ponto da chave; saída ordena por `packetIndex`, depois timestamp. | Confirmado pelo código |
| Coletor Kotlin background | `packetIndex` | `Map` mantém último pacote daquele índice na sessão. | Confirmado pelo código |
| Repositório antes de inserir | `source + deviceId + measuredAt`, quando ambos source e device estão presentes | Atualiza registro existente. | Confirmado pelo código |
| Repositório antes de inserir | `source + rawSessionId`, quando ambos presentes | Atualiza registro existente. | Confirmado pelo código |
| SQLite | Índice único `(device_id, measured_at, source)` | Proteção adicional quando `device_id` não é nulo. | Confirmado pelo código |

Foreground gera `rawSessionId` determinístico por ponto: `D1_AUTO_HISTORY:<deviceId|unknown>:<timestamp>`. Background inclui ciclo, índice e timestamp. Registro persiste `measured_at`, `bpm`, `device_id`, `source`, `raw_session_id`, `raw_hex` e estado Health Connect, entre outros campos de referência.

Índice SQLite contém colunas anuláveis. Sem `deviceId`, semântica de unicidade para valores `NULL` depende de SQLite e não substitui a deduplicação no repositório. Classificação: `Inferido a partir do código`.

## Health Connect: Regra Obrigatória Futura

Champion Companion deve enviar **todos os registros D1 com BPM válido** ao Health Connect. Não haverá seleção manual registro a registro. Esta é regra de produto definida pela spec da Fase 0; não é opcional nem condicionada a tela de histórico.

Fronteira documental corrigida em `009-02`:

- Fase 09 e owner da importacao D1 local-first, persistencia e criacao/garantia de `HealthSyncOperation.PENDING`;
- Fase 09 nao implementa provider, permissao, `HealthConnectClient`, escrita real ou estados `SYNCING/SYNCED`;
- Fase 12 e owner do processamento real das operacoes pendentes no Health Connect.

Fluxo MotionLog observado:

1. Cada ponto persistido nasce `pending`.
2. Repositório cria/atualiza item de fila Health Connect, tipo `HeartRate`, com `clientRecordId` `motionlog:heart:<id>`.
3. Escrita cria um `HeartRate` com `startTime`, `endTime` e amostra no mesmo instante; BPM é arredondado.
4. Sucesso marca registro e item como `synced`.
5. Permissão ausente, indisponibilidade, erro de escrita ou falha parcial marcam item/registro como `failed`; itens `pending` e `failed` são listados para tentativas posteriores.

No MotionLog, criação também tenta sync imediato sem bloquear persistência; runtime D1 faz flush explícito de até 100 pendentes após salvar pontos. Falha de Health Connect não desfaz dado local. Classificação: `Confirmado pelo código`.

Requisito para implementação futura: manter estado pendente por registro e retry seguro; não descartar nem marcar todo lote como sincronizado quando somente parte das escritas tiver êxito. Estratégia final de scheduler, limites, `clientRecordId` e versão pertencem à implementação Health Connect futura da Fase 12.

## Falhas Parciais

| Caso | Comportamento MotionLog | Classificação |
| --- | --- | --- |
| Pacote D1 inválido | Ignora pacote; coleta continua. | Confirmado pelo código |
| Pacotes duplicados | Map sobrescreve chave; coleta continua. | Confirmado pelo código |
| Timeout com pontos | Retorna pontos disponíveis e `timedOut = true`; runtime os persiste. | Confirmado pelo código |
| Timeout vazio | Retorna sem pontos; runtime retorna falso. | Confirmado pelo código |
| Erro ao persistir ponto | Ignora erro daquele ponto; continua demais pontos. | Confirmado pelo código |
| Falha Health Connect | Registro local permanece; fila marca falha ou pendência para nova tentativa. | Confirmado pelo código |

Não há validação observada de contiguidade de `packetIndex`, nem exigência observada de receber todos os índices até `totalPackets`. Um pacote final pode encerrar coleta mesmo com lacunas anteriores. Classificação: `Confirmado pelo código`.

## Lacunas Para `000-14`

- Capturar fixtures D1 reais: pacote único, multipart completo, duplicado, fora de ordem, índice faltante, `totalPackets = 0`, timeout parcial e BPM limítrofe/inválido.
- Confirmar semântica de bytes 6-13 e 15-18, data do dispositivo, checksum/CRC e possíveis variações de firmware.
- Confirmar timezone real do relógio e efeito de cruzar meia-noite; payload atual não confirma dia/mês/ano nem offset próprio.
- Definir contrato nativo para lote parcial: critério de completude, telemetria e retry manual/agendado sem BLE agressivo.
- Definir identidade estável e versão para escrita Health Connect nativa; prefixo `motionlog:` é referência do app legado, não contrato do Champion Companion.
- Decidir tratamento para D1 sem `deviceId` e confirmar unicidade SQLite desejada no banco novo.

## Limites

Nenhuma fixture física D1 foi criada nesta task. Nenhum código Android foi implementado. `motionlog-app` foi somente lido.

## Contrato vigente do Champion Companion — Spec 033

As seções anteriores preservam a leitura histórica da baseline MotionLog e do
parser D1 legado. Para o app Android atual, prevalece o contrato nativo v2:

- pacote estrutural D1 tem 20 bytes; `dayOffset/hour` usa o byte 5 empacotado;
- `caloriesRaw / 10.0` produz quilocalorias e `distanceRaw` produz metros;
- BPM válido fica em `30..200`; BPM inválido vira `null` sem descartar passos,
  distância, calorias, raws ou conclusão estrutural;
- data/hora resolvida usa a data local congelada no início da coleta e
  `dayOffset`; fatos são persistidos como linha diária granular idempotente;
- a janela remota comprovada para D1 elegível é `[t - 5 min, t)`;
- FEA1 continua total diário autoritativo. `FEA1 - ΣD1` é cauda/diagnóstico e
  nunca gera bucket ou record sintético;
- D1 e D4 decidem autoridade por métrica. D4 positivo suprime somente a mesma
  métrica D1 sobreposta; D4 zero/ausente não suprime D1 válido;
- watch sync apenas lê e persiste localmente. Health Connect exige gesto explícito;
  retry reutiliza identidade e payload congelados.

Validação física final sanitizada: `docs/integration/033-09-final-physical-validation.md`.
