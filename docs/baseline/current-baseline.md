# Baseline Atual do Projeto

## Produto e fase

Champion Companion é aplicativo Android nativo do relógio Champion C001, escrito em Kotlin e Jetpack Compose.

Baseline atual inclui fundação Android, design system próprio, persistência com Room/DataStore, núcleo BLE nativo reutilizável validado na Fase 04, PoC BLE validada na Fase 05, gerenciamento utilizável do Champion C001 concluído na Fase 06, Home funcional de resumo diário local concluída na Fase 07, Fase 08 concluída com medição manual de frequência cardíaca, Fase 09 concluída com histórico D1 local-first de frequência cardíaca, Fase 10 concluída com importação local-first de treinos D3/D4, Fase 11 concluída com revisão, calibração, descarte lógico e atividades importadas locais até `READY_TO_SYNC`, Fase 12 concluída com integração nativa write-only ao Health Connect em foreground e Fase 15 concluída com calendário e histórico consolidado local.

Escopo factual atual já inclui:

- handshake A1 funcional;
- associação persistida separada da sessão BLE efêmera;
- scan manual, conexão manual, auto-conexão controlada do dispositivo associado, disconnect, retry e esquecimento sem apagar histórico;
- diagnóstico seguro e exportação sanitizada;
- resumo diário persistido via FEA1 fresco por pull-to-refresh, com merge atômico sem redução e `lastImportedAt` após import válido;
- `ForegroundBleOperationGate` compartilhado entre refresh diário, medição manual, importação D1 e importação D3/D4;
- medição manual de 60 segundos com D0/F002, persistência apenas do resultado final válido e último BPM persistido na Home;
- tela de frequência cardíaca com card `Resumo atual` sem CTAs fixos, medição manual acessível somente na superfície própria e pull-to-refresh explícito para importação D1, com persistência idempotente, metadata dedicada, histórico consolidado manual + D1 + amostras reais de atividades importadas não descartadas, agregações locais e gráfico funcional;
- tela Exercícios com CTA explícita `Buscar treinos no relógio`, progresso, cancelamento, retry, deduplicação, retomada por CRC e por `deviceId + origin + startedAt` quando necessário;
- lista de atividades importadas com filtros locais por workflow;
- detalhe de atividade importada com revisão local, tipo confirmado, título, observações, gráfico de BPM, intervalos, calibração de esteira, descarte lógico e estados `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `DISCARDED`, `SYNCED` e `SYNC_FAILED` observáveis;
- banner de pendências locais na Home com prioridade para calibração.

Escopo factual atual inclui também na Fase 15:

- read model temporal local derivado, sem tabela materializada ou migration;
- calendário mensal com seleção, navegação, indicadores, legenda, estados vazios
  e descrições acessíveis;
- detalhe diário com passos, meta atual, distância, BPM, atividades, peso e
  status da fila local, além de navegação contextual para detalhes relacionados;
- históricos locais de passos e distância em períodos de 7, 30 e 90 dias, com
  lacunas preservadas e ausência de valores sintéticos;
- histórico dedicado de frequência cardíaca consolidando medições manuais, D1 e
  amostras de atividades importadas não descartadas.

Escopo factual atual já inclui também na Fase 12:

- SDK `androidx.health.connect:connect-client:1.1.0` integrado ao app;
- provider, disponibilidade e rationale Android do Health Connect;
- fila persistida granular de sync para passos, BPM, treino, peso e altura;
- `clientRecordId` determinístico e `clientRecordVersion` monotônico por agregado elegível;
- reconciliação de backlog herdado de resumo diário, medição manual, D1, peso e altura;
- gatilhos foreground pós-persistência para passos, BPM manual e backlog D1;
- sync explícito de atividade com records granulares e falha parcial retomável;
- `Settings` funcional para disponibilidade, permissões, `Sincronizar agora` e `Gerenciar acesso`;
- feedback secundário de Health Connect em Home, Saúde e Exercícios.
- **Spec 016:** sincronização de passos migrou de operações cumulativas para
  incrementos imutáveis com checkpoint transacional, retry determinístico e
  ponte de compatibilidade com legado. Ver
  `docs/specs/016-health-connect-incremental-daily-steps.md`.

Escopo factual atual do código ainda não inclui:

- leitura do Health Connect;
- GPS de sessão, localização persistida, rotas ou `ExerciseRoute`;
- foreground service;
- `WorkManager`;
- background BLE permanente;
- sync agressivo ou automático ao abrir o app.

Backlog local herdado mapeado no início da Fase 12 passou a ser processado pela fila persistida do Health Connect, com retry foreground e preservação de sucesso local.

- atividades `READY_TO_SYNC` continuam dependentes de ação explícita do usuário para sincronização.

`SettingsScreen` deixou de ser placeholder. `AppPreferences.healthSyncEnabled` participa do fluxo funcional da integração. `ChampionDatabase` está em `v5`. Distância e calorias continuam dependentes de dados pessoais suficientes; quando indisponíveis, Home, Saúde e Exercícios preservam ausência segura sem inventar valores.

Fechamento factual da Fase 12 em 2026-07-28: correção de identidade por variante concluída, Gate C físico concluído no Galaxy S22 com Health Connect real e builds debug/release validados. Integração permanece write-only, foreground e local-first; leitura remota, `WorkManager`, foreground service da sessão e rotas continuam ausentes do código da Fase 12. Backup e device transfer continuam negados.

## Política autorizada da Fase 13

A ADR-014 autoriza, mas ainda não implementa, rota persistida somente para
caminhada/corrida iniciada explicitamente pelo usuário. A autorização inclui
GPS preciso durante a sessão, foreground service mínimo, persistência
incremental, recuperação e sincronização da mesma sessão Health Connect.

Treino livre e esteira continuam sem localização. Não há rastreamento passivo,
servidor, exportação, GPX, TCX, FIT, backup ou device transfer de rota. Logs e
diagnósticos continuam sem coordenadas, altitude, rota, BPM, payload BLE,
identificadores pessoais, título ou observações.

Registro de fechamento da Fase 11: revisão, calibração, descarte lógico e atividades locais até `READY_TO_SYNC` foram consolidados documentalmente. A validação física de 2026-07-28 confirmou build, instalação, lista, filtros, detalhe, métricas, gráfico, intervalos e persistência após reinício no Galaxy S22 `SM-S901E`, mas não reproduziu todos os estados pendentes porque o aparelho continha atividades `SYNCED`. Health Connect funcional pertence à Fase 12 e não altera a fronteira de responsabilidade da Fase 11.

## Ambiente e validação

Use Android Studio, Android SDK Platform 36 e toolchain JDK 21. O Gradle pode executar em JDK 17 ou superior, conforme AGP 9.3.0. Use exclusivamente o wrapper do repositório. Validação esperada para mudanças de código ou Gradle continua sendo `lint`, `assembleDebug`, `assembleRelease` e, para gates físicos das fases recentes, também `installDebug`.

```bash
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
./gradlew lint assembleDebug assembleRelease
```

## Base de código

Há um módulo Android, `app`. Código-fonte: `app/src/main/java/br/com/mwconsulting/championcompanion`. `app` concentra estado global. Infra inclui `core.common`, `core.error`, `core.logging`, `core.navigation`, `core.preferences`, `core.database` e `core.bluetooth`. `core.bluetooth/android` concentra fronteira única com APIs Android BLE. `core.model` guarda contratos e domínio. `data/` implementa persistência via Room e repositories. `feature/` possui `home`, `health`, `calendar`, `exercises`, `device`, `profile` e `settings`.

Compose constrói UI; Hilt faz injeção; Coroutines e `StateFlow` modelam estado; Kotlin Serialization representa rotas. Navegação continua com back stack manual e subtree de Saúde com destinos específicos para frequência cardíaca, peso, passos e distância, além de subfluxo manual para detalhe de Exercícios. Room 3 e DataStore já estão ativos.

`feature/home` expõe `HomeViewModel`, `ChampionDailyStepsSyncCoordinator` e Home com resumo diário persistido, retry manual, timeout seguro, day rollover local, pull-to-refresh como ação principal, atalho para detalhe de frequência cardíaca e banner de pendências de atividades importadas.

`feature/health` expõe `HealthViewModel`, hub de métricas, detalhe de frequência cardíaca com medição manual persistida na superfície própria, importação D1 explícita por pull-to-refresh e histórico consolidado de manual, D1 e amostras reais de atividades importadas não descartadas, com agregações e gráfico local; peso, passos e distância seguem usando dados já persistidos.

`feature/exercises` expõe fluxo D3/D4 funcional com coordinator foreground, `ExercisesViewModel`, CTA explícita, progresso textual, confirmação de saída, cancelamento em background, resultado seguro, filtros, lista, detalhe, revisão local, calibração e descarte lógico.

Erros devem virar estado UI seguro; detalhes técnicos ficam no Logcat de debug.

## Variantes

- `debug`: `br.com.mwconsulting.championcompanion.dev`, `Champion Companion Dev`, `1.0-dev`
- `release`: `br.com.mwconsulting.championcompanion`, `Champion Companion`, `1.0`

Release ainda não recebe assinatura de produção.

## Privacidade e fontes

Logs estruturados usam `android.util.Log` apenas para diagnóstico em debug. Não registrar payload BLE, bytes crus, hexadecimal, MAC, coordenadas, rotas, tokens, credenciais, identificadores pessoais ou dados de saúde. Release deve manter logs diagnósticos inativos ou sem detalhes.

## Fontes autoritativas

- Requisitos: `docs/specs/`
- Direção de produto: `docs/roadmap/`
- Decisões: `docs/decisions/`
- Protocolos: `docs/protocol/`
