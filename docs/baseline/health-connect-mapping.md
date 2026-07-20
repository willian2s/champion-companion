# Mapeamento Health Connect - MotionLog Para Champion Companion

## Objetivo E Escopo

Mapear integração Health Connect da baseline MotionLog `feat/015-watch-workouts-d3-d4`, commit `0a2a2d64f8eb88cec9b1fcdf84fe0e46ef704edb`, como referência para Champion Companion. Banco local é fonte operacional; Health Connect é destino derivado. Este documento não define SDK, fila, schema Room ou código Android.

Salvo indicação contrária, itens são `Confirmado pelo código` em `../motionlog-app`. Regras marcadas `Decisão já definida` vêm de spec/requisitos Fase 0. Prefixos `motionlog:` são referência legada, não contrato para novo app.

## Permissões E Disponibilidade

| Classe | Permissões de escrita MotionLog | Situação |
| --- | --- | --- |
| Exigidas para estado `ready` | `Steps`, `HeartRate`, `ExerciseSession` | Confirmado pelo código |
| Opcionais | `Distance`, `TotalCaloriesBurned`, `Weight`, `Height` | Confirmado pelo código |
| Plataforma | Android; SDK verifica disponibilidade, inicializa e lê permissões antes de escrever | Confirmado pelo código |

Ausência de permissão exigida, provedor indisponível, plataforma não Android ou falha de inicialização impedem escrita e retornam falha. Permissões opcionais ausentes omitem somente record complementar de exercício. Peso e altura são solicitados como opcionais, embora possam ser enviados se concedidos. `Confirmado pelo código`.

## Registros, Campos, Unidades E Tempo

| Origem local | Records Health Connect | Campos enviados | Unidade e tempo |
| --- | --- | --- | --- |
| `daily_summaries` | `Steps` | `count`, metadata | Passos inteiros arredondados; início dia local; fim é agora para dia corrente ou próximo início local para dia concluído. |
| `heart_rate_measurements`, inclusive D1 | `HeartRate` | uma amostra `beatsPerMinute`, metadata | BPM inteiro arredondado; `startTime`, `endTime` e tempo amostra iguais a `measuredAt`. |
| `exercise_sessions` | `ExerciseSession` | tipo, início, fim, notas opcionais, metadata | ISO de `startedAt`/`endedAt`; tipo: caminhada, corrida, esteira, força para `free`, demais `OTHER_WORKOUT`. |
| Sessão exercício válida | `Distance` opcional | distância, metadata | metros; mesmo intervalo sessão; somente caminhada, corrida ou esteira; valor finito maior que zero. |
| Sessão exercício válida | `TotalCaloriesBurned` opcional | energia, metadata | quilocalorias; mesmo intervalo sessão; valor finito maior que zero. |
| Amostras BPM da sessão | `HeartRate` opcional | amostras dentro intervalo, metadata | BPM inteiro arredondado; mesmo intervalo sessão. Sem amostras, `avgBpm` válido gera pontos início/fim. |
| Perfil | `Weight`, `Height` opcionais | peso ou altura, metadata | peso em quilogramas; altura local cm convertida para metros; instante `updatedAt`, ou agora se ausente/inválido. |
| `weight_measurements` | `Weight` opcional | peso, metadata | quilogramas; `measuredAt`, ou agora se ausente/inválido. |

Validação observada: passos não negativos; BPM finito maior que zero; peso/altura finitos maiores que zero; sessão exige início, fim e fim posterior ao início. Para D1, parser anterior só persiste BPM inteiro de 30 a 220; Health Connect não reaplica essa faixa. `Confirmado pelo código`.

## IDs, Versões, Insert E Atualização

| Entidade | `clientRecordId` MotionLog | Versão | Escrita observada |
| --- | --- | --- | --- |
| Resumo diário | `motionlog:steps:<dailySummaryId>` | `clientRecordVersion = updatedAt` em epoch ms; fallback agora | Apaga `Steps` por intervalo diário e insere total atual. |
| BPM | `motionlog:heart:<measurementId>` | Não enviada | Insere um `HeartRate`. |
| Sessão | `motionlog:exercise:<sessionId>` | Não enviada | Insere sessão; complementos usam sufixos `:distance`, `:calories`, `:heart`. |
| Peso perfil | `motionlog:weight:<profileId>:<epochMs>` | Não enviada | Insere `Weight`. |
| Altura perfil | `motionlog:height:<profileId>:<epochMs>` | Não enviada | Insere `Height`. |
| Medida peso | `motionlog:weight-measurement:<measurementId>:<epochMs>` | Não enviada | Insere `Weight`. |

MotionLog usa `insertRecords`; não há update explícito por ID de record remoto. Para passos, substituição é delete por tempo seguido de insert. Para demais tipos, nova tentativa repete insert com mesmo `clientRecordId`; comportamento final depende da API Health Connect e não foi validado em dispositivo. `Confirmado pelo código` para chamadas; resultado remoto é `Ainda não confirmado`.

## Fila, Retry, Deduplicação E Erro

`health_connect_sync_items` mantém uma linha por `(entity_type, entity_id, record_type)`, com `client_record_id`, status, tentativas, timestamps e último erro. Enfileirar mesma chave restaura `pending`, limpa erro e `synced_at`. Processamento busca `pending` e `failed`, até limite padrão 100, em ordem pendente antes de falho. `Confirmado pelo código`.

- Persistência local vem antes de fila/escrita. Falha Health Connect não desfaz dado local. `Confirmado pelo código`.
- Sucesso marca item `synced`, incrementa tentativas e registra `synced_at`; falha marca `failed`, incrementa tentativas e guarda mensagem; dado inválido/ausente marca `skipped`. `Confirmado pelo código`.
- BPM também guarda estado, ID e erro na própria medição. D1 persiste cada ponto e pode fazer flush explícito de pendências. `Confirmado pelo código`.
- Falha parcial de múltiplos records de exercício não é granular: records são agrupados por tipo e inseridos sequencialmente; falha posterior retorna falha para item `ExerciseSession`, mesmo que grupo anterior já tenha escrito. `Confirmado pelo código`.
- Chave da fila evita múltiplos itens locais; `clientRecordId` determinístico reduz duplicação remota. Não há prova de idempotência remota nem teste físico. `Inferido a partir do código` para redução; idempotência é `Ainda não confirmada`.

## Regras Obrigatórias Champion Companion

- Todos registros D1 com BPM válido devem ser enviados. Sem seleção manual ponto a ponto. `Decisão já definida`.
- Falha parcial fica pendente por entidade/record para retry seguro. Não marcar lote inteiro como sincronizado. `Decisão já definida`.
- Importações D3/D4 nascem `pending_review`. Envio manual somente após revisão, validação `valid` e sessão vinculada. `Decisão já definida`; bloqueios atuais confirmados pelo código.
- Esteira envia distância calibrada. Importação sem calibração confirmada não envia. `Decisão já definida`; bloqueio atual confirmado pelo código.
- Não armazenar nem enviar latitude, longitude, coordenadas, polylines, rotas ou `ExerciseRoute`. GPS, se usado para métrica, fica apenas em memória. `Decisão já definida`.

## Exercícios D3/D4 E Sem Rotas

MotionLog mapeia `walking`, `running`, `treadmill` e `free` para sessão; distância é omitida para `free`. Treino importado não cria `Steps` separado: passos seguem pipeline diário. `Confirmado pelo código`.

`buildExerciseSessionHealthConnectRecords` declara explicitamente que não sincroniza `ExerciseRoute`; política retorna `route: false` para todo tipo. Nenhum record de rota, coordenada ou polyline é incluído. `Confirmado pelo código`.

## Lacunas Para `000-14`

- Definir IDs estáveis próprios Champion Companion, política de `clientRecordVersion` para todos records mutáveis e relação entre ID local e ID remoto.
- Definir atualização idempotente sem delete amplo de `Steps`; confirmar efeito de delete por intervalo sobre records de outros apps.
- Definir unidade de retry e recuperação para escrita parcial de sessão e records complementares já inseridos.
- Validar em dispositivo permissões, disponibilidade de provedor, conflito de `clientRecordId`, comportamento de reinsert e mensagens de erro reais.
- Definir política para peso/altura: perfil atual versus histórico de medidas, frequência de envio e timestamps de fallback.
- Definir limite, scheduler e backoff de retry sem BLE agressivo; código MotionLog processa até 100 itens, mas não prova política futura.
- Confirmar timezone e intervalos de `Steps` em mudança de horário/virada de dia.

## Fontes Lidas

- `../motionlog-app/src/lib/health-connect/index.ts`
- `../motionlog-app/src/lib/health-connect/sync.ts`
- `../motionlog-app/src/lib/health-connect/exercise-sync-records.ts`
- `../motionlog-app/src/lib/health-connect/watch-workout-sync.ts`
- `../motionlog-app/src/lib/health-connect/watch-workout-sync-rules.ts`
- `../motionlog-app/src/lib/repositories/health-connect-sync-repository.ts`
- `../motionlog-app/src/lib/exercises/workout-policy.ts`
- `docs/protocol/history-d1.md`, `docs/protocol/workouts-d3-d4.md`, `docs/baseline/database-reference.md`

## Limites

Nenhum SDK Android foi implementado. Nenhuma permissão ou escrita foi exercitada em dispositivo. `motionlog-app` foi somente lido.
