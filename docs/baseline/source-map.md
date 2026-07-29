# Source Map - MotionLog Para Champion Companion

## Objetivo E Limites

Mapa entre código MotionLog baseline `feat/015-watch-workouts-d3-d4` no commit `0a2a2d64f8eb88cec9b1fcdf84fe0e46ef704edb` e áreas conceituais permitidas Champion Companion. Destinos indicam responsabilidade, não nomes finais de módulos, classes, pacotes, tabelas ou telas.

`Confirmado pelo código` descreve origem e responsabilidade MotionLog. Destino Champion Companion é direcionamento conceitual para implementação futura; não é arquitetura aprovada além das áreas permitidas nesta task.

## Mapa Principal

| Funcionalidade | Origem MotionLog | Responsabilidade atual | Destino conceitual Champion Companion | Documentos relacionados | Status análise | Dúvidas |
| --- | --- | --- | --- | --- | --- | --- |
| Bootstrap, tema e navegação | `src/app/_layout.tsx`; `src/app/(tabs)/_layout.tsx`; `src/components/navigation/app-tabs.tsx`; `src/components/layout/` | Inicializa app, providers e abas Hoje, Saúde, Calendário, Exercícios | `feature/home`; `feature/heart`; `feature/exercises`; `feature/activitydetail` | `feature-inventory.md` | Confirmado pelo código | Estrutura de navegação Android futura não definida. |
| Home e resumo diário | `src/app/(tabs)/index.tsx`; `src/lib/repositories/daily-summary-repository.ts`; `src/models/local-estimation.ts` | Mostra passos, meta, distância/calorias estimadas, último BPM e revisão pendente | `feature/home`; `core/database` | `feature-inventory.md`; `database-reference.md` | Confirmado pelo código | Política de atualização visual e estimativas futuras permanece pendente. |
| Dispositivo, scan e conexão | `src/app/device/index.tsx`; `src/lib/ble/champion-client.ts`; `src/lib/ble/champion-runtime.ts`; `src/lib/repositories/device-repository.ts` | Permissões, scan, conexão, descoberta, runtime e dispositivo salvo | `feature/device`; `core/bluetooth`; `core/database` | `ble-transport.md`; `champion-c001.md` | Confirmado pelo código | Contrato nativo de reconexão e UI ainda não definido. |
| Transporte BLE e UUIDs | `src/lib/ble/champion-client.ts`; `champion-uuids.ts`; `ble-bytes.ts`; `champion-commands.ts` | Serviços, characteristics, escrita, notifications, bytes e comandos | `core/bluetooth` | `ble-transport.md`; `champion-c001.md`; `ble-command-mapping.md` | Confirmado pelo código | Validação física de UUIDs e variações firmware pendente. |
| Handshake A1 | `src/lib/ble/champion-handshake.ts`; `champion-client.ts`; `champion-runtime.ts` | Monta payload, escreve A1 e valida notificação de sucesso | `core/bluetooth`; `feature/device` | `handshake-a1.md`; `ble-command-mapping.md` | Confirmado pelo código | Sem execução com relógio nesta Fase 0. |
| Passos FEA1 e resumo | `src/lib/ble/champion-parsers.ts`; `champion-runtime.ts`; `daily-summary-repository.ts`; `src/models/daily-summary.ts` | Lê FEA1, persiste/upsert diário e calcula dados derivados | `core/bluetooth`; `core/database`; `feature/home` | `ble-command-mapping.md`; `database-reference.md` | Confirmado pelo código | Fonte e reconciliação final de passos futura pendentes. |
| BPM manual D0/F002 | `src/app/(tabs)/health/index.tsx`; `src/lib/ble/champion-heart-session.ts`; `champion-bpm-estimator.ts`; `heart-rate-measurement-repository.ts` | Inicia/para leitura, estima BPM, persiste medição e exibe saúde | `core/bluetooth`; `core/database`; `feature/heart` | `heart-rate-d0-f002.md`; `database-reference.md` | Confirmado pelo código | Precisão estimador versus dispositivo não validada fisicamente. |
| Histórico BPM D1 | `heart-history-sync.ts`; `heart-history-parser.ts`; `champion-runtime.ts`; `heart-rate-measurement-repository.ts` | Consulta D1, parseia, deduplica, persiste e dispara pendências Health Connect | `core/bluetooth`; `core/database`; `core/healthconnect`; `feature/heart` | `history-d1.md`; `health-connect-mapping.md` | Confirmado pelo código | Timezone do relógio, completude multipart e retry nativo pendentes. |
| Histórico e gráfico BPM | `src/app/(tabs)/health/heart-history/index.tsx`; `src/components/heart-rate/`; `heart-rate-measurement-repository.ts` | Filtros, lista, gráfico e intensidade de BPM | `feature/heart`; `core/database` | `feature-inventory.md`; `history-d1.md` | Confirmado pelo código | Paridade de gráfico e filtros Android não especificada. |
| Peso, altura e perfil | `src/app/profile/index.tsx`; `src/app/(tabs)/health/weight-history/index.tsx`; `user-profile-repository.ts`; `weight-measurement-repository.ts` | Perfil corporal, metas, pesagens e histórico | `feature/profile`; `feature/heart`; `core/database`; `core/healthconnect` | `feature-inventory.md`; `database-reference.md`; `health-connect-mapping.md` | Confirmado pelo código | Política futura perfil versus histórico ainda pendente. |
| Exercício pelo celular | `src/app/(tabs)/exercises/{start,active,index,history}.tsx`; `manual-session-store.ts`; `exercise-session-repository.ts` | Escolha, sessão ativa, pausa, retomada, finalização e histórico | `feature/exercises`; `core/database`; `feature/activitydetail` | `feature-inventory.md`; `database-reference.md` | Confirmado pelo código | Recuperação após processo encerrado requer decisão nativa. |
| GPS privado e métricas | `location-foreground.ts`; `gps-metrics-accumulator.ts`; `route-utils.ts`; `exercise-session-persistence.ts` | Coleta GPS foreground, deriva métricas e contém rota legada | `core/location`; `feature/exercises`; `core/database` | `feature-inventory.md`; `database-reference.md`; ADR-014 | Confirmado pelo código | ADR-014 autoriza rota persistida somente em sessão phone explícita de caminhada/corrida; demais tipos permanecem sem localização. |
| Esteira e calibração | `treadmill-workout-store.ts`; `treadmill-calibration.tsx`; `imported-treadmill-calibration.tsx`; `exercise-session-repository.ts` | Rascunho, distância informada, fator, recálculo e finalização | `feature/exercises`; `feature/activitydetail`; `core/database`; `core/healthconnect` | `database-reference.md`; `health-connect-mapping.md` | Confirmado pelo código | Fórmulas e UX final nativas ainda não definidas. |
| Importação treinos D3/D4 | `watch-workout-sync.ts`; `watch-workout-parser.ts`; `watch-workout-import-adapter.ts`; `watch-workout-repository.ts` | Índice CRC, download, remontagem, parser, deduplicação e persistência | `core/bluetooth`; `core/database`; `feature/exercises` | `workouts-d3-d4.md`; `database-reference.md` | Confirmado pelo código | Fixtures físicas, completude de bloco e variações firmware pendentes. |
| Revisão treino relógio | `watch-workout-review-card.tsx`; `pending-review-banner.tsx`; `watch-workout-repository.ts`; rotas `exercises/[id].tsx` | Exibe pendência, edita tipo/distância, revisa, descarta e abre calibração | `feature/exercises`; `feature/activitydetail`; `core/database` | `workouts-d3-d4.md`; `feature-inventory.md` | Confirmado pelo código | Modelo final de edição e descarte Android pendente. |
| Health Connect e fila | `src/app/(tabs)/health/integration.tsx`; `src/lib/health-connect/`; `health-connect-sync-repository.ts` | Disponibilidade, permissões, records, fila, sync manual e retry | `core/healthconnect`; `core/database`; `feature/heart`; `feature/profile` | `health-connect-mapping.md`; `database-reference.md` | Confirmado pelo código | IDs Champion, versão, backoff e recuperação parcial pendentes. |
| Sync manual D3/D4 Health Connect | `health-connect/watch-workout-sync.ts`; `watch-workout-sync-rules.ts`; `exercise-sync-records.ts` | Bloqueia envio sem revisão/válido/calibração; envia sessão e complementos | `core/healthconnect`; `feature/activitydetail`; `feature/exercises` | `workouts-d3-d4.md`; `health-connect-mapping.md` | Confirmado pelo código | Granularidade para falha parcial de records deve ser decidida. |
| Background BLE e estado operacional | `src/lib/background-ble/index.ts`; `background-monitoring-repository.ts`; `android/app/src/main/java/com/motionlog/app/MotionLogBackgroundBleService.kt`; `MotionLogBackgroundBleModule.kt`; `MotionLogBackgroundBleStateStore.kt` | Serviço nativo MotionLog, estado, logs, eventos e sync oportunista | `core/bluetooth`; `core/database`; `core/notifications`; `feature/diagnostics` | `ble-transport.md`; `history-d1.md`; `database-reference.md` | Confirmado pelo código | Roadmap altera estratégia: sem BLE permanente, D1 ao abrir ou retry infinito. Scheduler futuro pendente. |
| Diagnósticos D8/A8 | `champion-commands.ts`; `champion-client.ts`; docs protocolo MotionLog | Mantém comandos sem fluxo/parsers confirmados | `core/bluetooth`; `feature/diagnostics` | `diagnostics-d8-a8.md`; `ble-command-mapping.md` | Parcial: constantes confirmadas; utilidade não confirmada | Exige fixture e validação física antes de virar fluxo produto. |
| Persistência e migrations legadas | `src/lib/repositories/database/{connection,bootstrap,migrations}.ts`; repositórios e modelos | SQLite, schema, transações, deduplicação e dados operacionais | `core/database` | `database-reference.md`; `feature-inventory.md` | Confirmado pelo código | Banco novo inicia vazio; entidades, exclusões e retenção finais pendentes. |
| Calendário e detalhe dia | `src/app/(tabs)/calendar/{index,[date]}.tsx`; repositórios resumo, BPM e sessões | Consulta agregados por data e mostra detalhe diário | `feature/home`; `feature/activitydetail`; `core/database` | `feature-inventory.md` | Confirmado pelo código | Navegação e composição futura não definidas. |
| Notificações e permissões Android | `app.config.ts`; `android/app/src/main/AndroidManifest.xml`; `PermissionRationaleActivity.kt` | Declara BLE, localização, foreground service e notificações | `core/notifications`; `core/bluetooth`; `core/location`; `feature/device` | `feature-inventory.md`; `ble-transport.md`; ADR-014 | Confirmado pelo código | `WRITE_EXERCISE_ROUTE` só pertence à sessão phone autorizada da Fase 13; conjunto final depende implementação futura. |
| Rotas, mapa e OAuth legados | `components/maps/exercise-route-map.tsx`; `src/app/oauth.tsx`; `src/app/localhost/oauth.tsx` | Exibe rota persistida e callbacks integrações externas | Nenhum destino funcional definido | `feature-inventory.md`; `database-reference.md`; `health-connect-mapping.md` | Confirmado pelo código como legado/fora escopo | Mapas, OAuth, servidor e exportação permanecem fora; rota futura é somente a sessão F13 autorizada. |

## Regras Transversais De Destino

- `core/bluetooth` concentra protocolo e transporte, não define estrutura Kotlin final.
- `core/database` recebe fonte operacional e estados de sync; não migra banco MotionLog.
- `core/healthconnect` recebe destino derivado, nunca fonte operacional.
- `core/location` pode calcular métricas; persistência de rota só é autorizada
  para sessão explícita F13 de caminhada/corrida, sem logs, exportação ou
  servidor.
- `feature/diagnostics` abriga apenas ações diagnósticas confirmadas futuramente; D8/A8 não são fluxo principal atual.

## Lacunas Para `000-14`

- Decidir arquitetura Android, contratos entre áreas conceituais e nomes definitivos somente em fase de implementação.
- Consolidar decisões pendentes de retry/reconciliação BLE, Health Connect e recuperação de sessão.
- Validar fixtures e comportamento físico para A1, D0/F002, D1, D3/D4, D8 e A8.
- Aplicar política nativa de localização da ADR-014 sem reintroduzir dados em
  logs, diagnósticos, exportações, backup ou fluxos sem autorização.
- Confirmar paridade necessária para gráficos, calendário, estados vazios, notificações e diagnósticos.

## Fontes Consultadas

- `docs/baseline/current-baseline.md`
- `docs/baseline/feature-inventory.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/health-connect-mapping.md`
- `docs/protocol/*.md`
- Caminhos MotionLog citados na tabela.

## Limites

Nenhum código Android foi implementado. Nenhuma classe Kotlin foi nomeada. `motionlog-app` foi somente lido.
