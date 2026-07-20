# Referencia Conceitual Do Banco MotionLog

## Escopo E Regra De Transicao

Este documento descreve SQLite do MotionLog na baseline `feat/015-watch-workouts-d3-d4`, commit `0a2a2d64f8eb88cec9b1fcdf84fe0e46ef704edb`. Evidencia principal: `../motionlog-app/src/lib/repositories/database/migrations.ts` e repositorios associados.

Champion Companion tera banco proprio, iniciara vazio. Nao havera migracao de dados, compatibilidade de schema, leitura do `motionlog.db`, importador ou transferencia do MotionLog. Documento e referencia conceitual; nao e schema Room nem plano de migrations.

Classificacao: itens de schema, SQL e fluxo marcados como `Confirmado pelo codigo`. Recomendacoes para banco novo sao `Decisao ja definida` quando derivam de roadmap/spec/ADR; demais sao `Inferido a partir do codigo`.

## Runtime E Evolucao Atual

- Banco: `motionlog.db`, aberto por `expo-sqlite`. `Confirmado pelo codigo`.
- Bootstrap executa migrations uma vez por processo; tambem remove tokens legados Strava do SecureStore. `Confirmado pelo codigo`.
- Migrations aplicam `PRAGMA journal_mode = WAL` e `PRAGMA foreign_keys = ON`. `Confirmado pelo codigo`.
- `PRAGMA user_version` final: 16. Versao 16 repete de forma idempotente etapas 12--15 para recuperar bancos indevidamente marcados como 15. `Confirmado pelo codigo`.
- Historico inclui criacao de GPS/rotas e Strava; migrations atuais removem tabelas Strava. Nao ha evidencia de backup/restauracao SQLite no caminho de persistencia analisado. `Confirmado pelo codigo` para remocao; ausencia de backup/restauracao limitada ao codigo localizado.

## Tabelas E Colunas Evidenciadas

Tipos abaixo sao tipos SQLite declarados; nullable salvo `NOT NULL`, `PRIMARY KEY` ou `DEFAULT` indicados.

### Perfil, dispositivo e resumo diario

| Tabela | Colunas |
| --- | --- |
| `user_profile` | `id TEXT PK`; `sex TEXT`; `birth_date TEXT`; `height_cm REAL`; `weight_kg REAL`; `stride_length_meters REAL`; `activity_level TEXT`; `unit_system TEXT`; `daily_steps_goal INTEGER`; `weekly_exercise_goal INTEGER`; `created_at TEXT NOT NULL`; `updated_at TEXT NOT NULL` |
| `devices` | `id TEXT PK`; `name TEXT`; `ble_id TEXT`; `last_connected_at TEXT`; `created_at TEXT NOT NULL`; `updated_at TEXT NOT NULL` |
| `daily_summaries` | `id TEXT PK`; `date TEXT NOT NULL`; `steps INTEGER`; `active_calories_estimated REAL`; `distance_meters_estimated REAL`; `source TEXT`; `synced_at TEXT`; `created_at TEXT NOT NULL`; `updated_at TEXT NOT NULL`; acrescentadas: `health_connect_steps_synced_at TEXT`, `health_connect_steps_synced_total INTEGER` |
| `weight_measurements` | `id TEXT PK`; `measured_at TEXT NOT NULL`; `weight_kg REAL NOT NULL`; `source TEXT`; `created_at TEXT NOT NULL` |

`daily_summaries` e atualizado pelo repositório usando ID deterministico `daily:<date>` e leitura previa por `date`; schema nao declara indice unico para `date`. `Confirmado pelo codigo`.

### Frequencia cardiaca e fila Health Connect

| Tabela | Colunas |
| --- | --- |
| `heart_rate_measurements` | `id TEXT PK`; `measured_at TEXT NOT NULL`; `bpm INTEGER`; `device_id TEXT`; `signal_quality TEXT`; `quality_score INTEGER`; `sample_rate REAL`; `peaks INTEGER`; `duration_seconds REAL`; `source TEXT`; `raw_session_id TEXT`; `raw_hex TEXT`; `health_connect_synced_at TEXT`; `health_connect_record_id TEXT`; `health_connect_sync_status TEXT`; `health_connect_last_error TEXT`; `updated_at TEXT`; `created_at TEXT NOT NULL` |
| `health_connect_sync_items` | `id TEXT PK`; `entity_type TEXT NOT NULL`; `entity_id TEXT NOT NULL`; `record_type TEXT NOT NULL`; `client_record_id TEXT NOT NULL`; `status TEXT NOT NULL`; `attempts INTEGER NOT NULL DEFAULT 0`; `last_error TEXT`; `last_attempt_at TEXT`; `synced_at TEXT`; `created_at TEXT NOT NULL`; `updated_at TEXT NOT NULL` |

Estado de BPM no modelo: `pending`, `synced`, `failed` ou `null`. Estado da fila: `pending`, `synced`, `failed`, `skipped`. Valores sao tipos TypeScript e nao possuem `CHECK` no SQLite. `Confirmado pelo codigo`.

### Sessoes, GPS legado e amostras cardiacas

| Tabela | Colunas |
| --- | --- |
| `exercise_sessions` | `id TEXT PK`; `type TEXT NOT NULL`; `metric_source TEXT`; `recording_mode TEXT`; `gps_used_for_metrics INTEGER`; `route_saved INTEGER`; `route_synced INTEGER`; `distance_source TEXT`; `started_at TEXT NOT NULL`; `ended_at TEXT`; `duration_seconds REAL`; `distance_meters REAL`; `avg_pace_seconds_per_km REAL`; `avg_speed_mps REAL`; `max_speed_mps REAL`; `calories_estimated REAL`; `calories_formula TEXT`; `user_weight_kg_snapshot REAL`; `user_height_cm_snapshot REAL`; `steps_start INTEGER`; `steps_end INTEGER`; `steps_delta INTEGER`; `avg_bpm INTEGER`; `max_bpm INTEGER`; `min_bpm INTEGER`; `gps_quality TEXT`; `hr_quality TEXT`; `gps_points_accepted_count INTEGER`; `gps_points_rejected_count INTEGER`; `route_polyline TEXT`; `notes TEXT`; `created_at TEXT NOT NULL`; `updated_at TEXT NOT NULL` |
| `exercise_gps_points` | `id TEXT PK`; `exercise_session_id TEXT NOT NULL FK -> exercise_sessions(id)`; `timestamp TEXT NOT NULL`; `latitude REAL NOT NULL`; `longitude REAL NOT NULL`; `altitude REAL`; `accuracy REAL`; `speed REAL`; `heading REAL` |
| `exercise_heart_rate_samples` | `id TEXT PK`; `exercise_session_id TEXT NOT NULL FK -> exercise_sessions(id)`; `sampled_at TEXT NOT NULL`; `bpm INTEGER NOT NULL`; `elapsed_seconds REAL`; `distance_meters REAL`; `pace_seconds_per_km REAL`; `speed_mps REAL` |

Valores de `metric_source` modelados: `gps`, `steps`, `treadmill_calibration`, `manual` ou `null`. `recording_mode` e `distance_source` sao normalizados por politica de exercicio, mas schema nao impoe enumeracoes. `Confirmado pelo codigo`.

### Importacao D3/D4

| Tabela | Colunas |
| --- | --- |
| `watch_workout_imports` | `id TEXT PK`; `exercise_session_id TEXT FK -> exercise_sessions(id)`; `device_id TEXT NOT NULL`; `crc INTEGER NOT NULL`; `source TEXT NOT NULL`; `external_block_index INTEGER NOT NULL`; `suggested_activity_type TEXT NOT NULL`; `review_status TEXT NOT NULL DEFAULT 'pending_review'`; `validation_status TEXT NOT NULL DEFAULT 'partial'`; `health_connect_sync_status TEXT NOT NULL DEFAULT 'pending'`; `health_connect_synced_at TEXT`; `health_connect_record_id TEXT`; `health_connect_last_error TEXT`; `duration_seconds REAL`; `distance_meters REAL`; `steps_total INTEGER`; `calories_kcal REAL`; `avg_bpm INTEGER`; `max_bpm INTEGER`; `min_bpm INTEGER`; `raw_hex TEXT`; `created_at TEXT NOT NULL`; `updated_at TEXT NOT NULL` |
| `watch_workout_samples` | `id TEXT PK`; `workout_import_id TEXT NOT NULL FK -> watch_workout_imports(id)`; `minute_index INTEGER NOT NULL`; `timestamp TEXT NOT NULL`; `bpm INTEGER`; `steps INTEGER`; `distance_meters REAL`; `calories_kcal REAL`; `calories_raw INTEGER`; `sport_count_raw INTEGER`; `pause_raw INTEGER`; `raw_hex TEXT`; `created_at TEXT NOT NULL` |

Estados modelados: revisao `pending_review`/`reviewed`; validacao `valid`/`partial`/`invalid`; Health Connect `not_synced`/`pending`/`synced`/`failed`/`skipped`. Schema aceita qualquer `TEXT`; enumeracoes vem do modelo/repositorio. `Confirmado pelo codigo`.

### Monitoramento em segundo plano

| Tabela | Colunas |
| --- | --- |
| `background_monitoring_state` | `id TEXT PK`; `enabled INTEGER NOT NULL`; `device_id TEXT`; `heart_rate_interval_minutes INTEGER NOT NULL`; `service_status TEXT NOT NULL`; `last_attempt_at TEXT`; `last_success_at TEXT`; `last_steps_at TEXT`; `last_heart_rate_at TEXT`; `last_error TEXT`; `execution_mode TEXT`; `created_at TEXT NOT NULL`; `updated_at TEXT NOT NULL` |
| `background_monitoring_logs` | `id TEXT PK`; `level TEXT NOT NULL`; `event TEXT NOT NULL`; `message TEXT`; `device_id TEXT`; `created_at TEXT NOT NULL` |
| `background_preferences` | `key TEXT PK`; `value TEXT NOT NULL`; `updated_at TEXT NOT NULL` |

Estado singleton usa ID `default`, inicia desabilitado, intervalo de 15 minutos, status `idle`, modo `unknown`. Preferencia inicial: `android_background_guide_seen=false`. Estados modelados: servico `idle`, `starting`, `running`, `degraded`, `stopped`, `error`; modo `foreground_service`, `opportunistic_task`, `app_open_fallback`, `unknown`; log `debug`, `info`, `warn`, `error`. `Confirmado pelo codigo`.

## Relacionamentos E Indices

| Relacao ou indice | Finalidade observada |
| --- | --- |
| `exercise_gps_points.exercise_session_id` FK | Pontos GPS legados pertencem a sessao. |
| `exercise_heart_rate_samples.exercise_session_id` FK | Amostras BPM de sessao pertencem a sessao. |
| `watch_workout_imports.exercise_session_id` FK | Importacao D3/D4 pode vincular sessao criada. |
| `watch_workout_samples.workout_import_id` FK | Amostras por minuto pertencem a importacao. |
| `idx_exercise_gps_points_session_id` | Consulta de pontos por sessao. |
| `idx_exercise_hr_samples_session_id`, `idx_exercise_hr_samples_sampled_at` | Consulta de BPM por sessao e tempo. |
| `idx_heart_rate_measurements_device_measured_source UNIQUE(device_id, measured_at, source)` | Protecao adicional para BPM por dispositivo, instante e origem. |
| `idx_hc_sync_items_entity_record UNIQUE(entity_type, entity_id, record_type)` | Um item de fila por entidade e tipo de record. |
| `idx_hc_sync_items_status_updated_at(status, updated_at)` | Busca de pendentes/falhos. |
| `idx_hc_sync_items_client_record_id(client_record_id)` | Busca por identificador estavel Health Connect. |
| `idx_background_monitoring_logs_created_at(created_at DESC)` | Logs recentes. |
| `idx_watch_workout_imports_device_crc_source UNIQUE(device_id, crc, source)` | Deduplicacao de importacao D3/D4. |
| `idx_watch_workout_samples_import_minute UNIQUE(workout_import_id, minute_index)` | Uma amostra por minuto/importacao. |

FKs nao declaram `ON DELETE CASCADE`. Exclusao de sessao apaga manualmente, na mesma transacao: amostras D3/D4, importacao, GPS, BPM e sessao. `Confirmado pelo codigo`.

## Escrita, Deduplicacao E Transacoes

### Deduplicacao

- BPM: antes de inserir, repositorio procura `(source, device_id, measured_at)` quando ambos existem; senao procura `(source, raw_session_id)`. Encontrando, atualiza mesma linha. Indice unico reforca primeira chave, mas `device_id` anulavel tem semantica SQLite para `NULL`; logo busca do repositorio continua necessaria. `Confirmado pelo codigo`.
- D3/D4: `watchWorkoutAlreadyExists` e upsert usam `(device_id, crc, source)`; conflito atualiza dados do relogio. Se ja revisado, tipo escolhido pelo usuario na sessao e preservado. `Confirmado pelo codigo`.
- Resumo diario: ID `daily:<date>` e upsert; busca previa por data evita criar outro ID no fluxo normal. `Confirmado pelo codigo`.
- Fila Health Connect: chave unica `(entity_type, entity_id, record_type)`; novo enqueue restaura `pending`, limpa erro e `synced_at`. `Confirmado pelo codigo`.
- GPS, BPM de sessao e sessoes manuais usam `INSERT OR IGNORE` por ID. Nao existe indice unico temporal adicional nessas tabelas. `Confirmado pelo codigo`.

### Transacoes Exclusivas

| Operacao | Unidade atomica observada |
| --- | --- |
| Criar sessao | Sessao, pontos GPS permitidos pela politica e amostras BPM. |
| Excluir sessao | Dependentes D3/D4, GPS, BPM; depois sessao. |
| Upsert D3/D4 | Leitura de importacao, criar/atualizar sessao, upsert de importacao, substituir amostras e releitura. |
| Revisar D3/D4 | Ler importacao, atualizar sessao, marcar revisada e reler. |
| Calibrar esteira importada | Ler importacao/sessao, recalcular metricas, atualizar ambas e reler. |

Health Connect e deliberadamente fora da transacao local: persistencia retorna antes; fila/sync ocorre depois e falha nao quebra gravacao local. Importacao D3/D4 nao enfileira sync automaticamente. `Confirmado pelo codigo`.

## Dados Temporarios, Operacionais E Legados

| Grupo | Dados | Tratamento de referencia |
| --- | --- | --- |
| Fila operacional | `health_connect_sync_items`, status e tentativas por entidade/record | Necessidade funcional de fila e retry controlado confirmada; desenho novo fica para fase Android. |
| Estado operacional | monitoramento, erros e logs background | Referencia de diagnostico; retenção atual limita logs para 500 quando chamada a limpeza. |
| Captura bruta | `raw_hex`, `raw_session_id`, campos raw por minuto | Util para diagnostico/parser. Necessidade de reter no banco novo: `Inferido a partir do codigo`; definir privacidade e retenção. |
| Snapshots/metricas | peso/altura snapshot, qualidade GPS/BPM, contadores, velocidade, ritmo, calorias | Necessarios para reproduzir historico/revisao de sessao. `Inferido a partir do codigo`. |
| GPS/rota legado | `exercise_gps_points`, latitude, longitude, altitude, heading, `route_polyline`, flags de rota | Proibidos no Champion Companion. `Decisao ja definida`. |
| Strava | tabelas removidas e tokens SecureStore apagados no bootstrap | Nao transportar. `Confirmado pelo codigo` para remocao; exclusao do novo modelo alinhada ao escopo atual. |

Para caminhada/corrida atuais, `resolveRoutePersistenceForCreate` sempre devolve `route_saved=false`, `route_synced=false`, polylines nulas e zero pontos persistidos. Isso confirma que fluxo novo ja evita salvar rota nesses tipos; nao elimina registros GPS legados em bancos existentes. `Confirmado pelo codigo`.

## Limites Para Banco Novo

### Proibido

Nao persistir nem enviar: latitude, longitude, pontos GPS, coordenadas brutas, `route_polyline`, polylines, rotas, mapas, GPX, TCX, FIT ou `ExerciseRoute`. Coordenadas de caminhada/corrida, se usadas para calcular metricas, existem somente em memoria durante sessao. `Decisao ja definida` pelo roadmap/spec.

### Referencia Funcional A Preservar

- Perfil, dispositivo, resumo diario, medidas de peso e BPM.
- Sessoes com metricas finais, amostras BPM e origem da distancia.
- Importacao D3/D4 com chave de deduplicacao, dados brutos suficientes para diagnostico, status de validacao e revisao obrigatoria.
- Fila local de Health Connect com chave estavel, estado, tentativas e erro.
- Estado operacional/diagnostico proporcional ao fluxo futuro.

Lista e referencia de comportamento, nao autorizacao para copiar tabelas/colunas ou criar Room. `Inferido a partir do codigo`, exceto restricoes de rota.

## Lacunas Para Task 000-14

- Definir retenção, acesso e necessidade real de `raw_hex`, IDs de sessao brutos, logs e erros de Health Connect; codigo atual nao define politica de privacidade/expurgo geral. `Ainda nao confirmado`.
- Definir entidades e limites de diagnostico/background do novo app; roadmap muda arquitetura para Foreground Services e WorkManager. `Ainda nao confirmado`.
- Confirmar modelo novo para exclusao de entidade e fila Health Connect: exclusao de sessao atual nao mostra exclusao de itens de fila relacionados. `Confirmado pelo codigo` para comportamento atual; decisao nova pendente.
- Definir unicidade desejada para BPM sem `device_id`; indice SQLite atual aceita multiplos `NULL`. `Inferido a partir do codigo`.
- Confirmar se samples D3/D4 e BPM por exercicio exigem retenção completa ou agregados atendem paridade futura. `Ainda nao confirmado`.
- Confirmar semantica de `synced_at` de `daily_summaries` versus cursor especifico de Steps; ambos coexistem no schema atual. `Ainda nao confirmado`.

## Validacao Da Task

- Nenhum schema Room, migration plan, importador ou codigo Android criado.
- `motionlog-app` somente lido; status Git permaneceu sem saida nesta verificacao.
- Roadmap, ADR e requisitos originais nao alterados.
