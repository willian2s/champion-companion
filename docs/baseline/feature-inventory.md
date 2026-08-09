# Inventario Funcional Do MotionLog

## Registro de encerramento da Fase 013

Este inventário permanece referência histórica do MotionLog. A Fase 013 do
Champion Companion está `closed`, com resultado `partially_delivered` por
`product_reorientation`. A direção de treinos phone
foi descontinuada; a desativação operacional dos pontos de entrada foi
implementada em `013-17`, com aceite final confirmado por revisão independente.
Não continuar GPS, rota,
`ExerciseRoute` ou BLE live em sessão phone. Priorizar importação D3/D4;
distância canônica usa passos válidos e `stepLengthMeters`, enquanto distância
original do relógio fica diagnóstica. A Fase 014 está `completed`, com progresso
`17/17`, após entregar derivação por `stepLengthMeters`, revisão local e
sincronização granular com Health Connect. Seus limites físicos e riscos
residuais estão registrados nas evidências da fase.

Os registros sobre GPS, rotas, exercícios phone e background abaixo descrevem o
baseline MotionLog e a execução histórica. Não constituem backlog ativo nem
autorização para copiar ou expandir esses fluxos.

## Objetivo

Documentar estrutura geral, telas, funcionalidades, estados, permissoes e backup atual do MotionLog como referencia funcional para paridade futura do Champion Companion.

## Baseline Consultada

- Repositorio: `../motionlog-app`
- Branch: `feat/015-watch-workouts-d3-d4`
- Commit: `0a2a2d64f8eb88cec9b1fcdf84fe0e46ef704edb`
- Regra: `motionlog-app` somente leitura

## Caminhos Consultados

- `../motionlog-app/package.json`
- `../motionlog-app/app.config.ts`
- `../motionlog-app/android/app/src/main/AndroidManifest.xml`
- `../motionlog-app/src/app/`
- `../motionlog-app/src/components/`
- `../motionlog-app/src/hooks/`
- `../motionlog-app/src/lib/`
- `../motionlog-app/src/lib/background-ble/`
- `../motionlog-app/src/lib/ble/`
- `../motionlog-app/src/lib/exercises/`
- `../motionlog-app/src/lib/health-connect/`
- `../motionlog-app/src/lib/location/`
- `../motionlog-app/src/lib/repositories/`
- `../motionlog-app/src/models/`
- `../motionlog-app/docs/tasks/013-firebase-backup-restore/`

## Estado Geral

| Area | Estado | Evidencia |
| --- | --- | --- |
| App React Native/Expo | Funcional | `package.json`, `app.config.ts`, `src/app/_layout.tsx` |
| Navegacao por abas | Funcional | `src/components/navigation/app-tabs.tsx` |
| Persistencia local SQLite | Funcional | `src/lib/repositories/database/migrations.ts`, `connection.ts`, `bootstrap.ts` |
| BLE Champion C001 | Funcional | `src/lib/ble/champion-client.ts`, `champion-runtime.ts` |
| Passos e resumo diario | Funcional | `champion-runtime.ts`, `daily-summary-repository.ts` |
| BPM manual e historico | Funcional | `src/app/(tabs)/health/index.tsx`, `heart-history-sync.ts`, `heart-rate-measurement-repository.ts` |
| Treinos D3/D4 do relogio | Funcional | `watch-workout-sync.ts`, `watch-workout-parser.ts`, `watch-workout-repository.ts` |
| Revisao de treinos importados | Funcional | `watch-workout-review-card.tsx`, `watch-workout-repository.ts` |
| Exercícios pelo celular | Legado preservado, direção encerrada | `src/app/(tabs)/exercises/start.tsx`, `active.tsx`, `manual-session-store.ts` |
| GPS privado para metricas | Funcional | `src/lib/location/location-foreground.ts`, `gps-metrics-accumulator.ts`, `workout-policy.ts` |
| Health Connect | Funcional | `src/lib/health-connect/index.ts`, `sync.ts` |
| Background BLE e sync oportunista | Parcial | `src/lib/background-ble/index.ts`, native module `MotionLogBackgroundBle` esperado |
| Backup/restauracao Firebase | Nao confirmado | docs da fase 013 existem, mas nao ha `src/lib/firebase` ou `src/lib/cloud-backup` em `src` |
| Strava | Experimental | `src/lib/strava/` existe, mas fora do escopo do Champion Companion |

## Estrutura De Diretorios

- `src/app`: rotas Expo Router e telas.
- `src/app/(tabs)`: abas principais.
- `src/app/device`: tela de conexao e monitoramento do relogio.
- `src/app/profile`: perfil local do usuario.
- `src/components`: componentes visuais reutilizaveis.
- `src/hooks`: hooks de tema.
- `src/lib/ble`: cliente BLE, runtime Champion, comandos e parsers.
- `src/lib/background-ble`: monitoramento em segundo plano e sync oportunista.
- `src/lib/exercises`: regras de treino manual, GPS, esteira e metricas.
- `src/lib/health-connect`: inicializacao, permissoes e escrita Health Connect.
- `src/lib/location`: GPS foreground e utilitarios de rota legada.
- `src/lib/repositories`: acesso SQLite.
- `src/models`: tipos de dominio e calculos locais.

## Navegacao E Telas

| Tela | Rota | Estado | Funcao |
| --- | --- | --- | --- |
| Hoje | `src/app/(tabs)/index.tsx` | Funcional | Resumo diario, passos, distancia estimada, calorias, ultimo BPM e alerta de revisao pendente. |
| Saude | `src/app/(tabs)/health/index.tsx` | Funcional | Medicao manual de BPM, metricas de saude, historico recente. |
| Historico de BPM | `src/app/(tabs)/health/heart-history/index.tsx` | Funcional | Lista e grafico de BPM com filtros 7 dias, 30 dias e periodo customizado. |
| Peso | `src/app/(tabs)/health/weight-history/index.tsx` | Funcional | Registro e historico de peso. |
| Integracao | `src/app/(tabs)/health/integration.tsx` | Funcional | Health Connect, permissoes, fila de sync, background. |
| Calendario | `src/app/(tabs)/calendar/index.tsx` | Funcional | Mes com marcadores de passos, BPM e exercicios. |
| Detalhe do dia | `src/app/(tabs)/calendar/[date].tsx` | Funcional | Resumo diario, BPM e exercicios de uma data. |
| Exercicios | `src/app/(tabs)/exercises/index.tsx` | Funcional | Sessao manual, treinos recentes, pendencias de revisao e busca D3/D4. |
| Iniciar treino | `src/app/(tabs)/exercises/start.tsx` | Funcional | Escolha entre livre, caminhada, corrida e esteira. |
| Treino ativo | `src/app/(tabs)/exercises/active.tsx` | Funcional | Sessao em foreground com BPM, passos, GPS quando aplicavel e finalizacao. |
| Historico de exercicios | `src/app/(tabs)/exercises/history.tsx` | Funcional | Lista filtrada por periodo. |
| Detalhe de exercicio | `src/app/(tabs)/exercises/[id].tsx` | Funcional | Detalhes, rota legada quando existir, revisao de treino importado e exclusao. |
| Calibracao de esteira manual | `src/app/(tabs)/exercises/treadmill-calibration.tsx` | Funcional | Finalizacao/calibracao de treino de esteira iniciado pelo app. |
| Calibracao de esteira importada | `src/app/(tabs)/exercises/imported-treadmill-calibration.tsx` | Funcional | Calibracao de treino do relogio revisado como esteira. |
| Dispositivo | `src/app/device/index.tsx` | Funcional | Scan, conectar, desconectar, background e sync de treinos do relogio. |
| Perfil | `src/app/profile/index.tsx` | Funcional | Dados corporais, meta de passos e envio de peso/altura ao Health Connect. |
| OAuth | `src/app/oauth.tsx`, `src/app/localhost/oauth.tsx` | Diagnostico | Callback OAuth usado por integracoes externas. |

## Abas Principais

Confirmado em `src/components/navigation/app-tabs.tsx`:

- `Hoje`
- `Saude`
- `Calendario`
- `Exercicios`

Rotas fora das abas:

- `Dispositivo`
- `Perfil`

## Hooks, Services, Repositories E Stores

### Hooks

- `src/hooks/use-color-scheme.ts`
- `src/hooks/use-color-scheme.web.ts`
- `src/hooks/use-theme.ts`

Uso principal: tema, cor e adaptacao web.

### Services E Runtimes

- `src/lib/ble/champion-client.ts`: cliente BLE, permissoes, scan, conexao, notifications, handshake, comandos.
- `src/lib/ble/champion-runtime.ts`: orquestracao do cliente, auto-connect, persistencia de passos, D1, D3/D4.
- `src/lib/background-ble/index.ts`: monitoramento background, eventos nativos e sync oportunista Health Connect.
- `src/lib/location/location-foreground.ts`: runtime GPS foreground.
- `src/lib/health-connect/index.ts`: inicializacao e escrita Health Connect.
- `src/lib/health-connect/sync.ts`: fila local de sincronizacao.

### Repositories

- `background-monitoring-repository.ts`
- `daily-summary-repository.ts`
- `device-repository.ts`
- `exercise-session-repository.ts`
- `health-connect-sync-repository.ts`
- `heart-rate-measurement-repository.ts`
- `user-profile-repository.ts`
- `watch-workout-repository.ts`
- `weight-measurement-repository.ts`

### Stores Em Memoria

- `manual-session-store.ts`: sessao manual ativa, BPM e metadados temporarios.
- `treadmill-workout-store.ts`: rascunho de esteira antes da calibracao.
- `champion-runtime.ts`: estado de runtime BLE e descoberta de dispositivos.
- `location-foreground.ts`: estado de GPS foreground.

## Modelos De Dominio

- `daily-summary.ts`: resumo diario e ID por data.
- `device.ts`: dispositivo pareado/conectado.
- `heart-rate-measurement.ts`: BPM, qualidade e estado Health Connect.
- `exercise-session.ts`: sessao, GPS legado, amostras BPM e metricas.
- `watch-workout.ts`: treino importado D3/D4, revisao, validacao e sync.
- `health-connect-sync-item.ts`: fila de sync.
- `background-monitoring.ts`: estado/logs/preferencias de background.
- `user-profile.ts`: perfil, sexo, altura, peso, metas.
- `weight-measurement.ts`: pesagens.
- `local-estimation.ts`: distancia, passada, calorias e IMC.

## Persistencia Local

Confirmado em `src/lib/repositories/database/migrations.ts`:

- SQLite via `expo-sqlite`.
- `SCHEMA_VERSION = 16`.
- Tabelas principais:
  - `user_profile`
  - `devices`
  - `daily_summaries`
  - `heart_rate_measurements`
  - `weight_measurements`
  - `exercise_sessions`
  - `exercise_gps_points`
  - `exercise_heart_rate_samples`
  - `health_connect_sync_items`
  - `background_monitoring_state`
  - `background_monitoring_logs`
  - `background_monitoring_preferences`
  - `watch_workout_imports`
  - `watch_workout_samples`

Observacao para Android nativo: banco novo nao deve migrar schema antigo, mas este schema orienta entidades e regras de paridade.

## Permissoes E Configuracao Android

Confirmado em `app.config.ts` e `AndroidManifest.xml`:

- BLE: `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`.
- Localizacao: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` no manifest gerado.
- Foreground services: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`.
- Notificacoes: `POST_NOTIFICATIONS`.
- Health Connect: escrita de passos, BPM, exercicio, distancia, calorias, peso e altura.
- Manifest do MotionLog também contém `WRITE_EXERCISE_ROUTE`; isso é referência
  histórica e não configuração do Champion Companion. ADR-014 autoriza essa
  capacidade somente para sessão phone explícita de caminhada/corrida na Fase
  13.
- `react-native-ble-plx` configurado com `isBackgroundEnabled: true`.
- `expo-location` configurado com foreground service habilitado e background location desabilitado no config.

## Estados Loading, Erro E Vazio

Padroes observados:

- Loading por `isLoading`, `isSubmitting`, `isSaving`, `isFinishing`, `isRequestingHealthPermission`, `watchWorkoutSync.inProgress`.
- Refresh por `RefreshControl` em Home, Calendario, detalhes e Exercicios.
- Erros via `ToastAndroid.show(...)` e cards de erro em telas de detalhe/calibracao.
- Estados vazios com textos como:
  - `Nenhum dado registrado neste dia.`
  - `Nenhum exercício registrado neste dia.`
  - `Nenhuma sessão salva neste período.`
  - `Nenhuma medição salva neste período.`
  - `Nenhum dispositivo conectado no momento.`
  - `Nenhuma sessão em andamento.`
  - `Treino importado não encontrado.`

## Funcionalidades Principais

### Resumo Diario E Passos

- Estado: Funcional.
- Evidencia: `champion-runtime.ts`, `daily-summary-repository.ts`, `src/app/(tabs)/index.tsx`.
- Comportamento: packets FEA1 atualizam passos; runtime persiste `daily_summaries`; Home mostra passos, meta, distancia e calorias.
- Requisitos cobertos: 30, 31, 32, 33.

### Estimativas Locais

- Estado: Funcional.
- Evidencia: `src/models/local-estimation.ts`, `src/app/(tabs)/index.tsx`.
- Comportamento: distancia usa passada resolvida por perfil; calorias usam peso, passos e passada quando dados existem.

### Medicao Manual De BPM

- Estado: Funcional.
- Evidencia: `src/app/(tabs)/health/index.tsx`, `champion-heart-session.ts`, `champion-bpm-estimator.ts`, `heart-rate-measurement-repository.ts`.
- Comportamento: tela inicia sessao de BPM, mantém estado de medicao, grava resultado e trata falhas por Toast.

### Historico Automatico D1

- Estado: Funcional.
- Evidencia: `champion-runtime.ts`, `heart-history-sync.ts`, `heart-history-parser.ts`.
- Comportamento: runtime tenta sincronizar historico do dispositivo no startup/conexao, persiste pontos e aciona sync pendente de BPM ao Health Connect.
- Observacao: detalhe de protocolo fica fora desta task.

### Treinos Importados Do Relogio D3/D4

- Estado: Funcional.
- Evidencia: `watch-workout-sync.ts`, `watch-workout-parser.ts`, `watch-workout-import-adapter.ts`, `watch-workout-repository.ts`.
- Comportamento: tela `Exercicios` e `Dispositivo` disparam busca; importa treinos, salva status de revisao, preserva deduplicacao e nao sincroniza automaticamente antes da revisao.

### Revisao De Treinos Importados

- Estado: Funcional.
- Evidencia: `watch-workout-review-card.tsx`, `watch-workout-repository.ts`, `watch-workout-sync-rules.ts`.
- Comportamento: usuario revisa tipo, pode sincronizar manualmente com Health Connect, e esteira exige calibracao quando aplicavel.

### Atividades Importadas: Estados, Revisao E Descarte

Fontes: `src/models/watch-workout.ts`, `src/lib/repositories/watch-workout-repository.ts`, `src/components/exercises/watch-workout-review-card.tsx`, `src/app/(tabs)/exercises/[id].tsx` e `src/lib/repositories/exercise-session-repository.ts`.

Salvo indicacao contraria, itens desta secao sao `Confirmado pelo codigo`. Fluxos em dispositivo nao foram executados.

| Dimensao | Estados | Transicao ou efeito |
| --- | --- | --- |
| Revisao | `pending_review`, `reviewed` | Importacao D3/D4 nova inicia em `pending_review`. Usuario escolhe tipo e salva revisao; status passa a `reviewed`. |
| Validacao de parser | `valid`, `partial`, `invalid` | `partial` e `invalid` bloqueiam sync manual. Import `invalid` nao cria sessao vinculada. |
| Sync Health Connect | `not_synced`, `pending`, `synced`, `failed`, `skipped` | Importacao inicia `not_synced`; tentativa manual marca `pending`; sucesso marca `synced`; falha marca `failed` e persiste erro. Resultado bloqueado retorna `skipped` sem iniciar escrita. |
| Calibracao de esteira | Pendente ou confirmada, derivada | Pendente quando sessao revisada e tipo `treadmill`, mas `metricSource` nao e `treadmill_calibration`. Confirmacao troca origem para `treadmill_calibration`. |

Fluxo confirmado:

1. D3/D4 persiste importacao com chave unica `(device_id, crc, source)`, `review_status = pending_review` e `health_connect_sync_status = not_synced`.
2. Importacao valida cria `exercise_sessions` vinculada; tipo inicial e sugestao, nao confirmacao do usuario. Reimportacao atualiza dados e amostras; se revisada, preserva tipo escolhido.
3. Lista de pendencias consulta somente `review_status = pending_review`.
4. Revisao permite `running`, `walking`, `treadmill` ou `free`. Salvar atualiza tipo, modo de registro, origem de distancia e `review_status = reviewed`, em transacao exclusiva.
5. Se tipo revisado for `treadmill`, calibracao permanece pendente ate gravar tempo e distancia. Sem calibracao, sync manual fica bloqueado.
6. Usuario pode excluir registro pelo detalhe. Transacao remove amostras do import, importacao, pontos GPS, amostras de BPM e sessao. Isto e descarte persistido; nao ha estado separado `discarded`.

Sugestao automatica confirmada: `free_workout` do protocolo vira `free`; demais casos usam passos totais, `>= 1000` sugere `running`, abaixo disto ou sem total sugere `free`. Heuristica e revisavel; nao classifica automaticamente como esteira.

Edicoes confirmadas para treino importado: tipo na revisao; para esteira revisada, tempo e distancia. Nao foi encontrada edicao generica de distancia para corrida, caminhada ou treino livre importado nesta analise.

### Sincronizacao Manual De Treinos Importados

Fonte: `src/lib/health-connect/watch-workout-sync.ts`, `watch-workout-sync-rules.ts`, `exercise-sync-records.ts` e `watch-workout-review-card.tsx`.

Nao existe enfileiramento automatico no `upsertWatchWorkout`. Acao manual usa `clientRecordId` `motionlog:watch-workout:<importId>` e exige:

- revisao concluida;
- validacao `valid`;
- status diferente de `synced` e `pending`;
- sessao vinculada existente;
- esteira calibrada, quando tipo `treadmill`.

Depois dos bloqueios, processo marca import como `pending`, le amostras BPM da sessao e escreve Health Connect. Sucesso grava `synced`, data e ID do registro; erro ou resultado nao sincronizado grava `failed` e mensagem, permitindo nova tentativa manual.

Registros produzidos dependem do tipo: `running` e `walking` enviam sessao, distancia, calorias e BPM; `treadmill` envia sessao, distancia calibrada, calorias e BPM; `free` envia sessao, calorias e BPM sem distancia. Treinos importados nao criam registro `Steps` separado. Nenhuma rota e enviada.

### Calibracao De Esteira Importada

Fontes: `src/app/(tabs)/exercises/imported-treadmill-calibration.tsx`, `src/lib/exercises/exercise-session-utils.ts`, `src/lib/repositories/watch-workout-repository.ts` e `src/lib/health-connect/watch-workout-sync.ts`.

Criacao manual confirmada: tela de inicio oferece `treadmill`; pode iniciar sem Champion conectado e nao inicia GPS. Ao finalizar, app guarda rascunho somente em memoria e abre `treadmill-calibration`; sessao somente e criada depois de calibracao valida com `metric_source = treadmill_calibration`. Descartar esta calibracao limpa rascunho e sessao manual em memoria, sem persistir atividade. Fluxo detalhado de exercicios iniciados pelo celular permanece para `000-09`.

Treino D3/D4 revisado como `treadmill` abre tela propria de calibracao. Tela preenche tempo e distancia registrados pelo relogio apenas como sugestoes. Usuario informa painel da esteira; tempo aceita segundos inteiros, `MM:SS` ou `HH:MM:SS`, maior que zero e ate 24 h. Distancia aceita km numericos, ponto ou virgula, maior que zero e ate 200 km. Cancelar volta ao detalhe sem alterar registro.

Confirmacao recalcula e persiste, em transacao exclusiva:

| Valor | Regra confirmada |
| --- | --- |
| Inicio | Preservado da sessao importada. |
| Fim | `startedAt + durationSeconds` calibrados. |
| Distancia final | `distanciaKm informada * 1000`. Substitui distancia importada no import e sessao. |
| Velocidade media e maxima | `distanceMeters / durationSeconds`; maxima recebe mesma velocidade media por falta de valor maximo confiavel. |
| Ritmo medio | `durationSeconds / distanciaKm`. |
| Calorias estimadas | Formula de corrida/esteira pelo peso do perfil e distancia; sem peso valido, permanece `null`. |
| Origem | `metric_source = treadmill_calibration`; sem GPS, rota ou mapa. |
| Sync | Mantem estados de revisao e sync; somente desbloqueia nova acao manual. |

Valor enviado ao Health Connect apos calibracao e distancia final da sessao em metros, condicionada a permissao e valor valido. Sessao, calorias estimadas e BPM seguem mesmas condicoes de sync manual; rota nao e enviada.

Fator de calibracao separado nao foi encontrado em codigo, schema nem tela analisados. Codigo substitui valores do relogio pelos valores informados e nao persiste formula `distancia_usuario / distancia_estimada`. Este requisito permanece lacuna documental da Fase 014; nao deve ser assumido no novo app sem decisao posterior.

### Exercicios Iniciados Pelo Celular

Fontes: `src/app/(tabs)/exercises/start.tsx`, `active.tsx`, `treadmill-calibration.tsx`, `src/lib/exercises/manual-session-store.ts`, `treadmill-workout-store.ts`, `gps-metrics-accumulator.ts`, `workout-heart-rate-samples.ts`, `workout-policy.ts`, `src/lib/location/location-foreground.ts`, `src/lib/repositories/exercise-session-repository.ts` e `exercise-session-persistence.ts`.

Salvo indicacao contraria, itens desta secao sao `Confirmado pelo codigo`. Fluxos em dispositivo nao foram executados.

#### Inicio E Sessao Ativa

Tela de inicio oferece `free`, `walking`, `running` e `treadmill`. Inicio cria `ManualExerciseSession` somente em memoria com ID, tipo, inicio, passos iniciais, estado BLE, dispositivo, amostras BPM e linha do tempo BPM. Sessao e aberta em `active.tsx`.

| Tipo | Relogio | GPS | Persistencia ao iniciar | Finalizacao |
| --- | --- | --- | --- | --- |
| Caminhada | Obrigatorio pelo fluxo da tela | Foreground, para metricas | Nenhuma | Cria `exercise_sessions` |
| Corrida | Obrigatorio pelo fluxo da tela | Foreground, para metricas | Nenhuma | Cria `exercise_sessions` |
| Treino livre | Obrigatorio pelo fluxo da tela | Nao usado | Nenhuma | Cria `exercise_sessions` |
| Esteira | Opcional | Nao usado | Nenhuma | Cria rascunho em memoria; persiste somente apos calibracao |

Com relogio conectado, inicio garante notifications core e handshake A1 quando necessario, abre sessao de frequencia cardiaca com duracao de quatro horas e inicia leitura. Erro neste inicio limpa sessao em memoria. Esteira pode iniciar sem Champion conectado; demais tipos sao bloqueados pela tela enquanto nao houver dispositivo conectado.

Tela ativa mostra duracao desde `startedAt`, delta de passos quando contadores inicial/final existem, estado de conexao, BPM atual/ultimo e qualidade recente da leitura. Watchdog tenta restaurar sessao/leitura BPM se conexao estiver `subscribed`, handshake concluido e intervalo de 12 segundos transcorrer; erros desse restart nao encerram sessao.

#### Pausa, Retomada, Descarte E Recuperacao

- `Nao confirmado no codigo`: acao, estado ou marcador persistido de pausa/retomada para exercicio manual. `active.tsx` nao expoe controles de pausa ou retomada.
- `Confirmado pelo codigo`: nao ha persistencia da sessao ativa. `manual-session-store.ts` e `treadmill-workout-store.ts` usam variaveis de modulo em memoria; recarregamento do processo perde estado ativo e rascunho de esteira. Navegacao durante processo vivo pode reencontrar a sessao pelo mesmo store, mas recuperacao apos encerramento do app/processo nao foi encontrada.
- `Confirmado pelo codigo`: nao ha acao de descarte na tela ativa para caminhada, corrida ou treino livre. A sessao so vira registro ao finalizar com sucesso.
- `Confirmado pelo codigo`: descarte na calibracao de esteira limpa sessao e rascunho em memoria, sem criar atividade persistida.

Estas ausencias sao lacunas históricas; não representam requisito decidido para
Champion Companion e não reabrem a Fase 013.

#### GPS, Metricas E Privacidade

Somente caminhada e corrida chamam `Location.watchPositionAsync` em foreground. Runtime pede permissao foreground, usa precisao maxima, intervalo de dois segundos e tres metros. Pontos alimentam acumulador em memoria; pontos sem coordenadas validas, precisao acima de 30 m, intervalo abaixo de um segundo, segmento abaixo de dois metros ou velocidade acima do limite do tipo sao rejeitados. Limites de velocidade: caminhada `3,5 m/s`; corrida `8,5 m/s`.

Distancia, velocidade atual/media/maxima e ritmo medio sao derivados dos pontos aceitos. A linha do tempo BPM pode receber metricas derivadas do GPS correspondentes ao instante da amostra. Ao finalizar, runtime de GPS e acumulador sao interrompidos e limpos.

**Regra histórica do baseline MotionLog:** coordenadas de caminhada/corrida
existem somente em memória durante a sessão. Para Champion Companion, ADR-014
autoriza persistência controlada somente em sessão explícita de
caminhada/corrida; treino livre, esteira, Fase 12 e D3/D4 permanecem sem rota.
Mapas, GPX, TCX, FIT, servidor, exportação, backup e device transfer continuam
proibidos.

MotionLog atual ainda possui schema e leitura de rota legada. Fluxo manual atual chama persistencia com `routeSaved: false`, `routeSynced: false` e nao fornece `gpsPoints`; politica de persistencia devolve zero pontos para caminhada/corrida. Isto confirma ausencia de nova rota no fluxo manual, nao elimina dados legados existentes.

#### BPM Durante Exercicio

Com Champion conectado, BPM vem de estado D0 do cliente. Amostra valida deve ser positiva e ter timestamp estritamente maior que ultima amostra. Sessao mantem valor arredondado e timestamp em memoria. Na finalizacao, calcula media, minimo e maximo de valores positivos; persiste amostras BPM na sessao. Para caminhada/corrida com distancia GPS valida, amostras tambem recebem distancia, ritmo e velocidade derivados; demais tipos recebem somente BPM e tempo decorrido quando disponivel.

Sem relogio, esteira ainda inicia, mas nao ha fonte alternativa de BPM neste fluxo. Falha de leitura/restart BPM nao encerra automaticamente treino.

#### Finalizacao E Persistencia

Finalizar tenta encerrar sessao/leitura cardiaca, calcula fim, duracao, passos finais/delta, BPM, metricas GPS e estimativas. Caminhada/corrida usam distancia GPS valida; sem distancia valida, codigo usa estimativa por passos para campo de distancia, mas so calcula calorias desses tipos quando GPS valido. Treino livre usa estimativa por passos para distancia/calorias. `free` nao envia distancia pela politica Health Connect; `walking`, `running` e `treadmill` podem enviar distancia. No baseline MotionLog esses fluxos não enviam rota; no Champion Companion, somente caminhada/corrida explícita da Fase 13 poderá fazê-lo conforme ADR-014.

Para caminhada, corrida e treino livre, `createExerciseSession` persiste sessao e amostras BPM somente apos finalizacao bem-sucedida. Erro deixa sessao em memoria para nova tentativa enquanto processo estiver vivo. Para esteira, finalizacao nao cria sessao: cria rascunho em memoria e abre calibracao. Confirmar dados do painel cria sessao com `metric_source = treadmill_calibration`; descartar nao persiste nada.

#### Background E Notificacoes Atuais

Fontes: `src/lib/background-ble/index.ts`, `android/app/src/main/java/com/motionlog/app/MotionLogBackgroundBleService.kt`, `MotionLogBackgroundBleModule.kt`, `MotionLogBackgroundBleStateStore.kt`, `AndroidManifest.xml` e tela de integracao.

`Confirmado pelo codigo`: background atual e referencia funcional do MotionLog, separado de sessao manual. Sessao manual declara explicitamente execucao em foreground e nao coleta GPS em background. Nenhum vinculo foi encontrado entre `manual-session-store`/`active.tsx` e `MotionLogBackgroundBleService`.

Servico nativo e Foreground Service `connectedDevice`, iniciado/parado por modulo `MotionLogBackgroundBle`. Mantem notificacao baixa e permanente `MotionLog ativo`, com status/passos e acoes `Parar` e `Sincronizar agora`. Tenta localizar Champion, conectar GATT, assinar FEA1/F008, executar handshake A1, receber passos e buscar historico D1 em ciclos de cinco a 120 minutos; reconexao usa atraso de cinco segundos enquanto monitoramento estiver habilitado. Eventos enviados ao JavaScript persistem passos, pontos D1 e tentam sync pendente de BPM ao Health Connect.

JS tambem registra task oportunista de sync Health Connect com minimo de 15 minutos e fallback no abrir app. Disponibilidade depende das restricoes do dispositivo; erros e estado ficam no repositorio de monitoramento. Acao nativa `Sincronizar agora` somente emite `sync_now_requested`; encaminhamento para sincronizacao efetiva depende de listener JavaScript ativo e nao foi validado em dispositivo.

Para Champion Companion, isto e **referencia do baseline**, nao especificacao de implementacao: ADR-014 define foreground service mínimo para sessão ativa da Fase 13; Fase 14 reserva WorkManager para tarefas adiáveis. Permanecem proibidos BLE permanente, sincronizacao agressiva, BPM continuo, D1 ao abrir e retry infinito. `013-08` e `013-16` devem validar continuidade, notificacao e limites de background antes da implementação Android.

### GPS E Privacidade

- Estado: Funcional.
- Evidencia: `workout-policy.ts`, `location-foreground.ts`, `exercise-session-persistence.ts`, `active.tsx`.
- Comportamento: GPS usado para metricas em caminhada/corrida no baseline; ADR-014 autoriza rota persistida somente para sessão phone explícita desse tipo.
- Observacao: banco ainda possui campos/tabelas de rota legada; novo app não deve persistir rota fora da política F13.

### Health Connect

- Estado: Funcional.
- Evidencia: `health-connect/index.ts`, `health-connect/sync.ts`, `health-connect/watch-workout-sync.ts`.
- Comportamento: permissoes, escrita de Steps, HeartRate, ExerciseSession, Weight e Height; fila local controla pending/synced/failed/skipped.

### Background

- Estado: Parcial.
- Evidencia: `background-ble/index.ts`, `AndroidManifest.xml`, `src/app/(tabs)/health/integration.tsx`, `src/app/device/index.tsx`.
- Comportamento: modulo JS espera native module `MotionLogBackgroundBle`, registra task oportunista de Health Connect, processa eventos de passos/BPM e expõe status.
- Lacuna: comportamento real do service nativo Android deve ser documentado em task propria.

### Backup E Restauracao

- Estado: Nao confirmado.
- Evidencia: `docs/tasks/013-firebase-backup-restore/` contem plano de implementacao; `src` nao contem `src/lib/firebase`, `src/lib/cloud-backup` ou tela de conta/backup confirmada.
- Tratamento para Champion Companion: apenas referencia funcional planejada/nao confirmada; nao deve virar requisito de migracao de dados.

## Dependencias Observadas

Principais dependencias funcionais:

- `expo-router`
- `expo-sqlite`
- `react-native-ble-plx`
- `react-native-health-connect`
- `expo-health-connect`
- `expo-location`
- `expo-background-task`
- `expo-task-manager`
- `expo-notifications`
- `expo-maps`
- `react-native-calendars`
- `react-native-gifted-charts`
- `@react-native-vector-icons/lucide`

Observacao: nenhuma dependencia Expo ou componente React Native foi copiado para Champion Companion; esta task apenas documenta.

## Mapeamento Dos Requisitos Originais

| Requisito | Cobertura nesta task | Evidencia |
| --- | --- | --- |
| 1. Estrutura de diretorios | Coberto | Secao "Estrutura De Diretorios" |
| 2. Arquitetura atual | Coberto | Secoes "Hooks, Services, Repositories E Stores" e "Persistencia Local" |
| 3. Telas | Coberto | Secao "Navegacao E Telas" |
| 4. Navegacao | Coberto | Secao "Abas Principais" |
| 5. Hooks, services, repositories e stores | Coberto | Secao propria |
| 6. Modelos de dominio | Coberto | Secao "Modelos De Dominio" |
| 7. Persistencia local | Coberto em nivel geral | Detalhe completo fica para `000-10` |
| 8. Servicos em segundo plano | Coberto em nivel geral | Detalhe completo fica para `000-09` |
| 30. Leitura de passos | Coberto em nivel geral | `champion-runtime.ts`, `champion-parsers.ts` |
| 31. Consolidacao diaria | Coberto | `daily-summary-repository.ts` |
| 32. Estimativa de distancia | Coberto | `local-estimation.ts` |
| 33. Estimativa de calorias | Coberto | `local-estimation.ts` |
| 50. Estados das atividades | Coberto | Secao "Atividades Importadas: Estados, Revisao E Descarte" |
| 51. Atividades pendentes | Coberto | `getPendingReviewWorkouts`, `review_status = pending_review` |
| 52. Sugestao automatica de tipo | Coberto como heuristica revisavel | `mapSuggestedToLocalType` |
| 53. Edicao do tipo | Coberto | `markWorkoutAsReviewed`, `WatchWorkoutReviewCard` |
| 54. Edicao de distancia | Coberto somente para esteira revisada | Secao "Calibracao De Esteira Importada" |
| 55. Revisao | Coberto | `markWorkoutAsReviewed` |
| 56. Descarte | Coberto como exclusao persistida | `deleteExerciseSessionById` |
| 57. Sincronizacao manual | Coberto | `syncWatchWorkoutToHealthConnectManually` |
| 58. Criacao da atividade de esteira | Coberto | Inicio manual guarda rascunho ate calibracao; import revisado como `treadmill` preserva sessao vinculada e exige calibracao |
| 59. Estimativa de distancia | Coberto como distancia importada, usada apenas como sugestao | `watch-workout-parser.ts`, tela de calibracao |
| 60. Calibracao | Coberto | `applyTreadmillCalibrationToImportedWorkout` |
| 61. Distancia informada pelo usuario | Coberto | Campo em km; conversao para metros |
| 62. Fator de calibracao | Nao confirmado | Nenhum fator separado encontrado; lacuna documental da Fase 014 |
| 63. Recalculo de metricas | Coberto | Distancia, ritmo, velocidade, fim e calorias |
| 64. Valor enviado ao Health Connect | Coberto | Distancia calibrada da sessao, em metros |
| 65. Caminhada | Coberto | Secao "Exercicios Iniciados Pelo Celular" |
| 66. Corrida | Coberto | Secao "Exercicios Iniciados Pelo Celular" |
| 67. Treino livre | Coberto | Secao "Exercicios Iniciados Pelo Celular" |
| 68. Esteira | Coberto | Secao "Exercicios Iniciados Pelo Celular" |
| 69. Sessao ativa | Coberto em memoria/foreground | `manual-session-store.ts`, `active.tsx` |
| 70. Pausa | Nao confirmado | Nenhum controle ou estado encontrado para sessao manual |
| 71. Retomada | Nao confirmado | Nenhum controle ou estado encontrado para sessao manual |
| 72. Recuperacao apos encerramento | Nao confirmada; ausente no fluxo analisado | Stores de sessao/rascunho somente em memoria |
| 73. GPS | Coberto para metricas em foreground, sem rota nova | `location-foreground.ts`, `gps-metrics-accumulator.ts` |
| 74. BPM durante exercicio | Coberto com Champion conectado | `active.tsx`, `manual-session-store.ts` |
| 75. Persistencia da sessao | Coberto somente ao finalizar/calibrar | `exercise-session-repository.ts`, `treadmill-calibration.tsx` |
| 76. Finalizacao | Coberto | `active.tsx`, `treadmill-calibration.tsx` |
| 100. Backup e restauracao atuais | Nao confirmado | Docs 013 existem; codigo atual nao encontrado em `src` |

## Lacunas históricas — não são backlog phone ativo

- Backup/restauracao Firebase nao confirmado no codigo atual.
- Pausa e retomada de exercicio iniciado pelo celular nao foram encontradas no
  baseline. O registro permanece histórico; não abrir correção phone na Fase
  013.
- Recuperacao de sessao manual/rascunho de esteira apos morte do processo nao
  existe no fluxo analisado. O registro permanece histórico e não autoriza
  continuação phone.
- Background do MotionLog usa Foreground Service conectado ao dispositivo, reconexao recorrente e ciclos D1. Champion Companion deve tratar isto somente como referencia e reconciliar com ADR-014 e limites do roadmap antes de implementar.
- Notificacao e continuidade de exercicio iniciado pelo celular em background nao foram implementadas no MotionLog. Não reabrir `013-08` por esse registro.
- Detalhes de parsers BLE, offsets, endianess e CRC ficam fora desta task.
- Mapeamento completo de Health Connect fica para `000-11`.
- Banco detalhado e regras por tabela ficam para `000-10`.
- Fator de calibracao separado para esteira nao existe no codigo analisado. A
  Fase 014 não promove esse legado para distância canônica de `WATCH_D3_D4`;
  não inferir uma das opções sem decisão própria.
- `WRITE_EXERCISE_ROUTE` existe no MotionLog atual; no Champion Companion só
  pode ser usado pela sessão phone explícita de caminhada/corrida autorizada
  pela ADR-014.
- Coordenadas e rota legada aparecem no schema do MotionLog; novo app deve
  evitar latitude, longitude, rotas, polylines e coordenadas brutas fora da
  sessão F13 autorizada, e nunca deve expô-las em logs, backup ou exportação.

## Conclusao

O MotionLog baseline contem app funcional com Home, Saude, Calendario, Exercicios, Dispositivo, Perfil, BLE Champion C001, persistencia SQLite, Health Connect, exercicios foreground, GPS privado para metricas e fluxo D3/D4 com revisao. Backup/restauracao nao foi confirmado no codigo atual e permanece como lacuna documental para consolidacao posterior.
