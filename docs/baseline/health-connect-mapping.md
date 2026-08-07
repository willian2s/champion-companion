# Mapeamento Health Connect - MotionLog Para Champion Companion

## Direção atual após encerramento da Fase 013

Fase 013 está `closed`, com resultado `partially_delivered` por
`product_reorientation`. O fluxo futuro não exige GPS, rota ou `ExerciseRoute`:
o relógio inicia e encerra o treino, o app importa D3/D4, deriva métricas,
permite revisão e sincroniza de forma granular com Health Connect. A distância
canônica deverá usar passos válidos e comprimento pessoal da passada; a
distância original do relógio permanece somente diagnóstica.

A desativação operacional dos pontos de entrada phone foi implementada em
`013-17`, com aceite final confirmado por revisão independente. As seções históricas
de rota phone abaixo não são backlog ativo. WorkManager fica fora do objetivo
funcional da Fase 014 e pode ser usado somente para trabalho finito e adiável.
Infraestrutura, filas, dados e evidências existentes devem ser preservados.

## Objetivo E Escopo

Mapear integração Health Connect da baseline MotionLog `feat/015-watch-workouts-d3-d4`, commit `0a2a2d64f8eb88cec9b1fcdf84fe0e46ef704edb`, como referência para Champion Companion. Banco local é fonte operacional; Health Connect é destino derivado. Este documento não define SDK, fila, schema Room ou código Android.

Salvo indicação contrária, itens são `Confirmado pelo código` em `../motionlog-app`. Regras marcadas `Decisão já definida` vêm de spec/requisitos Fase 0. Prefixos `motionlog:` são referência legada, não contrato para novo app.

## Atualização factual pós-Fase 12 e direção da Fase 014

No estado atual do Champion Companion apos a integracao nativa real:

- passos, BPM manual, backlog D1, peso e altura ja possuem fila persistida funcional com IDs estaveis e retry foreground;
- atividades revisadas e calibradas continuam exigindo acao explicita para sync, agora com records granulares independentes;
- `SettingsScreen` ja expõe provider, disponibilidade, permissões, `Sincronizar agora` e `Gerenciar acesso`;
- SDK Android do Health Connect, provider e rationale Android ja estao integrados.

Esta task de fechamento atualiza o documento para refletir estado final entregue da Fase 12.

Para a Fase 014, atividades `WATCH_D3_D4` usarão somente distância canônica
derivada de passos válidos e comprimento individual do passo (`stepLengthMeters`).
O valor original do relógio é
diagnóstico e não pode ser fallback. O contrato futuro adiciona `SPEED`, permite
múltiplos `DISTANCE` por intervalo e exige snapshot/projeção congelados conforme
ADR-018. A migration local de projeções filhas está concluída em `014-12`;
writer e efeitos remotos permanecem pendentes para `014-13`.

## Fechamento Factual Da Fase 12

Em 2026-07-28, `012-15` corrigiu a identidade de `clientRecordId` por variante e `012-13` registrou Gate C físico concluído no Galaxy S22 com provider real. Disponibilidade, permissões, escrita foreground, retry, falha parcial, preferência pausada, recuperação e privacidade foram confirmados na validação manual adicional informada pelo usuário. A integração continua write-only; não há leitura remota nem rota.

## Politica Champion Companion Formalizada Na Fase 12

As decisoes abaixo passam a ser consideradas contrato documental do app novo para a Fase 12:

- integracao write-only;
- Room como fonte operacional e visual primaria;
- fila persistida granular por agregado e tipo de record;
- retries foreground com o mesmo `clientRecordId`;
- `clientRecordVersion` monotônico para agregados mutaveis;
- passos sem delete amplo por intervalo;
- atividade sincronizada somente por acao explicita;
- atividade sem `StepsRecord` separado;
- atividade da Fase 12 e D3/D4 sem rota; sessão phone de caminhada/corrida
  somente conforme ADR-014;
- falha parcial mantida por record complementar, sem desfazer sessao ja sincronizada;
- peso e altura so quando houver dado local elegivel;
- ausencia de `WorkManager` e leitura remota nesta fase.

## Política histórica de rota da Fase 13 — não vigente

A Fase 13 pode incluir `ExerciseRoute` somente na mesma
`ExerciseSessionRecord` de uma sessão phone explícita de caminhada/corrida.
Treino livre, esteira, Fase 12 e D3/D4 permanecem sem rota. A sessão base usa
`clientRecordId` canônico; enriquecimento posterior usa o mesmo ID e versão
monotônica maior. Sem permissão `WRITE_EXERCISE_ROUTE`, sessão e complementos
elegíveis sincronizam e a rota local permanece pendente para retry posterior.

Esta política foi documentada para a execução histórica. A implementação e a
validação física incompletas permanecem evidência; não pertencem a backlog
ativo nem autorizam novos gates ou expansão phone.

### Correção planejada da distância temporal na Fase 13

O mapeamento agregado de `DISTANCE` descrito para a Fase 12 é histórico. No
fluxo de exercícios da Fase 13, `013-14` deve enviar uma série de
`DistanceRecord` por intervalo temporal, com delta positivo, início/fim válidos,
ordenação determinística e sem sobreposição. Sessão phone usa somente deltas
phone; watch-only usa intervalos D3/D4 elegíveis; atividade phone correlacionada
ignora distância e intervalos do relógio.

O record agregado legado não pode coexistir com a série corrigida. A fila deve
preservar IDs e versões para retry/reconciliação, sem apagar records de terceiros
ou inventar distribuição temporal para uma fonte que só possua total.

## Mapeamento Planejado Champion Companion

| Origem local Champion Companion | Record planejado | Politica Champion Companion Fase 12 |
| --- | --- | --- |
| `daily_summaries` | `Steps` | um record por resumo diario, mesmo `clientRecordId` em refreshes do mesmo dia, sem `delete` amplo por intervalo |
| `heart_rate_measurements` manual | `HeartRate` | uma operacao por medicao persistida, timestamp exato, retry idempotente |
| `heart_rate_measurements` D1 | `HeartRate` | uma operacao por ponto persistido valido, sem selecao manual |
| `workout_activities` | `ExerciseSession` | sessao obrigatoria, so por acao explicita do usuario |
| `workout_activities` | `Distance` | complementar opcional e independente; Fase 12 usou distância final, Fase 13 usa série temporal da fonte elegível |
| `workout_activities` + série derivada | `Speed` | Fase 014 planeja amostras derivadas; não enviar velocidade original ou estimativa sem snapshot |
| `workout_activities` | `TotalCaloriesBurned` | complementar opcional e independente |
| `workout_activities` + `workout_heart_rate_samples` | `HeartRate` | complementar opcional com amostras reais da sessao |
| `weight_measurements` | `Weight` | so quando houver dado local elegivel |
| `user_profiles.heightCentimeters` | `Height` | so quando houver altura local elegivel |

## IDs E Versoes Champion Companion

- Para operações legadas, `clientRecordId` permanece determinístico por
  `applicationId + aggregateType + aggregateId + recordType`.
- Para filhos, `projectionChildKey` é acrescentado ao ID. A chave não contém
  valores de saúde.
- `clientRecordVersion` é monotônico por `clientRecordId`; retry idêntico mantém
  ID, versão e payload congelado.
- IDs `motionlog:` deixam de ser referencia de implementacao e permanecem apenas como baseline comparativa legada.

### Projeções filhas persistidas

`health_sync_operations` mantém uma linha independente por filho:

| Record | Filho | Persistência |
| --- | --- | --- |
| `EXERCISE_SESSION` | `exercise-session` | ação, payload, estado e checkpoint próprios |
| `DISTANCE` | `interval-<índice>` | uma operação por intervalo, sem subfila |
| `SPEED` | `speed-series` | uma operação independente, sem mapeamento nesta task |
| `HEART_RATE` | `heart-rate-series` | uma operação independente, sem mapeamento nesta task |
| `TOTAL_CALORIES_BURNED` | `total-calories-burned` | uma operação independente, sem mapeamento nesta task |

Preparação é local e transacional. `action`, payload serializado, origem da
derivação, estado esperado/enviado/obsoleto e checkpoint são persistidos antes
de qualquer writer. `MIGRATION_9_10` apenas aplica defaults seguros a operações
legadas; não insere nem remove records no provider.

## Diferencas Deliberadas Em Relacao Ao MotionLog

- Champion Companion nao apagara `Steps` por intervalo antes de inserir.
- Champion Companion nao criara `Steps` separado para atividade.
- Champion Companion tratara falha parcial de atividade de forma granular por record, nao como item unico indivisivel.
- Champion Companion nao usara fallback silencioso por `now` para peso ou altura sem decisao local valida.

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
| Sessão exercício válida | `Distance` opcional | distância, metadata | Fase 12: metros no intervalo da sessão; Fase 13: deltas em intervalos temporais válidos, ordenados e não sobrepostos; valor finito maior que zero. |
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
- Falha parcial de atividade é granular por record complementar; sessão sincronizada é preservada e retry processa somente operações pendentes ou falhas. `Confirmado pelo código e pelo Gate C`.
- Chave da fila evita múltiplos itens locais; `clientRecordId` determinístico e update por versão preservam idempotência por variante. `Confirmado pelo código e pelo Gate C`.

## Regras Obrigatórias Champion Companion

- Todos registros D1 com BPM válido devem ser enviados. Sem seleção manual ponto a ponto. `Decisão já definida`.
- Falha parcial fica pendente por entidade/record para retry seguro. Não marcar lote inteiro como sincronizado. `Decisão já definida`.
- Importações D3/D4 nascem `pending_review`. Envio manual somente após revisão, validação `valid` e sessão vinculada. `Decisão já definida`; bloqueios atuais confirmados pelo código.
- Esteira envia distância calibrada. Importação sem calibração confirmada não envia. `Decisão já definida`; bloqueio atual confirmado pelo código.
- Não armazenar nem enviar latitude, longitude, coordenadas, polylines, rotas ou
  `ExerciseRoute` em Fase 12 e D3/D4. A exceção de rota persistida para sessão
  phone explícita de caminhada/corrida está definida na ADR-014; treino livre e
  esteira continuam sem localização. `Decisão já definida`.

## Exercícios D3/D4 E Sem Rotas

MotionLog mapeia `walking`, `running`, `treadmill` e `free` para sessão; distância é omitida para `free`. Treino importado não cria `Steps` separado: passos seguem pipeline diário. `Confirmado pelo código`.

`buildExerciseSessionHealthConnectRecords` da Fase 12 declara explicitamente
que não sincroniza `ExerciseRoute`; política retorna `route: false` para os
tipos atuais. A projeção route da Fase 13 será adicionada somente à mesma
sessão phone autorizada, na subtarefa 013-14.

## Lacunas Históricas Da Baseline MotionLog

Itens abaixo registram perguntas existentes na baseline MotionLog antes da implementação nativa do Champion Companion. Não representam bloqueios da Fase 12; decisões e validações aplicáveis ao app novo estão registradas nas seções pós-fase e no Gate C.

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

O registro acima descreve somente a baseline MotionLog e sua leitura histórica. No Champion Companion, o SDK Android foi integrado, permissões e escrita foram exercitadas no Galaxy S22 no Gate C, e o provider permanece write-only.
