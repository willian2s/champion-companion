# Inventario Funcional Do MotionLog

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
| Exercicios pelo celular | Funcional | `src/app/(tabs)/exercises/start.tsx`, `active.tsx`, `manual-session-store.ts` |
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
- Manifest atual tambem contem `WRITE_EXERCISE_ROUTE`; roadmap do Champion Companion proibe `ExerciseRoute`.
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

### Exercicios Iniciados Pelo Celular

- Estado: Funcional.
- Evidencia: `start.tsx`, `active.tsx`, `manual-session-store.ts`, `exercise-session-repository.ts`.
- Tipos: caminhada, corrida, treino livre e esteira.
- Comportamento: sessoes rodam em foreground; caminhada/corrida usam GPS para metricas; esteira pode iniciar sem relogio conectado.

### GPS E Privacidade

- Estado: Funcional.
- Evidencia: `workout-policy.ts`, `location-foreground.ts`, `exercise-session-persistence.ts`, `active.tsx`.
- Comportamento: GPS usado para metricas em caminhada/corrida; politica Health Connect define `route: false`.
- Observacao: banco ainda possui campos/tabelas de rota legada; novo app nao deve persistir coordenadas.

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
| 100. Backup e restauracao atuais | Nao confirmado | Docs 013 existem; codigo atual nao encontrado em `src` |

## Lacunas Para Consolidacao Futura

- Backup/restauracao Firebase nao confirmado no codigo atual.
- Service nativo `MotionLogBackgroundBle` precisa ser analisado em task de background.
- Detalhes de parsers BLE, offsets, endianess e CRC ficam fora desta task.
- Mapeamento completo de Health Connect fica para `000-11`.
- Banco detalhado e regras por tabela ficam para `000-10`.
- `WRITE_EXERCISE_ROUTE` existe no MotionLog atual, mas Champion Companion nao deve usar `ExerciseRoute`.
- Coordenadas e rota legada aparecem no schema do MotionLog; novo app deve evitar latitude, longitude, rotas, polylines e coordenadas brutas.

## Conclusao

O MotionLog baseline contem app funcional com Home, Saude, Calendario, Exercicios, Dispositivo, Perfil, BLE Champion C001, persistencia SQLite, Health Connect, exercicios foreground, GPS privado para metricas e fluxo D3/D4 com revisao. Backup/restauracao nao foi confirmado no codigo atual e permanece como lacuna documental para consolidacao posterior.
