# Baseline Atual do Projeto

## Registro atual da Fase 013

- A Fase 013 está `closed`, com resultado `partially_delivered` e motivo
  `product_reorientation`.
- Treinos iniciados pelo celular foram descontinuados como direção de produto.
  A desativação operacional dos pontos de entrada phone foi implementada em
  `013-17`; aceite final foi confirmado por revisão independente.
- GPS, rota, `ExerciseRoute` e BLE live não devem ser ampliados em sessão phone.
  Migrations, schemas, sessões, dados e evidências existentes permanecem
  preservados até task explícita de remoção.
- Prioridade futura: importar D3/D4 do Champion C001, derivar métricas,
  permitir revisão e sincronizar com Health Connect. A distância canônica será
  derivada de passos válidos e `stepLengthMeters`; a distância original do
  relógio permanece somente diagnóstica.
- A Fase 014 foi concluída como refinamento de treinos iniciados pelo relógio,
  com progresso `17/17`. WorkManager permanece fora do objetivo funcional e só
  pode apoiar trabalho finito e adiável. Ver
  `docs/specs/014-watch-workout-derived-metrics.md`.

## Exceção experimental isolada da Spec 026

A Spec `026-maplibre-openfreemap-gps-route-poc.md` registra uma PoC técnica
planejada e independente para avaliar GPS nativo, MapLibre Compose e OpenFreeMap
em fluxo `debug-only`. Essa autorização é estritamente efêmera: a rota ficará
somente em memória e a implementação deve permanecer no source set
`app/src/debug`. A revisão de 2026-08-20 autoriza, para teste externo, um botão
técnico na página de Exercícios somente no build debug, disparando `Intent`
explícito para a Activity da PoC. ADB permanece apenas como fallback diagnóstico;
não é mais a única forma prevista de abertura. A autorização não adiciona
destino permanente à navegação, não libera CTA em Home, Saúde, Dispositivo ou
Configurações e não permite que botão, `Intent` executável ou entrypoint da PoC
apareça na variante release.

A exceção não reabre a Fase 013, não altera `PhoneWorkoutPolicy`, não reativa
`WorkoutRecordingService` e não autoriza persistência, Health Connect, BLE,
treino iniciado pelo celular ou feature de produção. O CTA é affordance técnica
para abrir a PoC; não é CTA de treino phone. O histórico e os guards da Fase 013
permanecem preservados. O CTA técnico foi implementado em `026-03` com provider
separado por variante e validação de build; validação física de GPS/rota continua
pertencendo às subtarefas posteriores.

MapLibre Compose será o renderizador experimental e OpenFreeMap, o provedor
inicial do mapa-base, com atribuição obrigatória e sem SLA assumido. A escolha
anterior de Mapbox foi abandonada antes da implementação por exigir
configuração de billing/forma de pagamento incompatível com a premissa da PoC.
Não há conta, API key, token ou pagamento previsto; termos, disponibilidade e
política gratuita do provedor devem ser revalidados na implementação. Nenhuma
decisão de produção foi tomada. Ver ADR-024 e o overview da Spec 026.

### Fechamento factual da Spec 026 — 2026-08-21

A Spec 026 foi concluída com classificação `go_with_adjustments`. Validação
física sanitizada no `SM-S901E` confirmou MapLibre Compose, OpenFreeMap/Positron,
atribuição, GPS real, rotas visuais de aproximadamente 538 m e 557 m, câmera,
stop, fit bounds, reset, segundo ciclo e casos negativos principais. O conceito
não apresentou bloqueio estrutural para a PoC. O crescimento visual foi
observado, mas não houve medição final explícita de jank após as correções de
responsividade.

Falha real do mapa em cold-cache permaneceu inconclusiva porque o mapa continuou
`Pronto` offline, provavelmente por cache. Uma rejeição sem categoria foi aceita
como resíduo de observabilidade, e a variação do fused entre ambientes continua
risco residual. Nenhum dado sensível foi preservado em Logcat ou evidências.

O fechamento não autoriza feature de produção, não reabre a Fase 013 e não
altera `PhoneWorkoutPolicy`. Qualquer efetivação exige nova spec e decisão
explícita, com revalidação de provider, disponibilidade, termos, privacidade,
cache/offline e observabilidade.

## Produto e fase

Champion Companion é aplicativo Android nativo do relógio Champion C001, escrito em Kotlin e Jetpack Compose.

Baseline atual inclui fundação Android, design system próprio, persistência com Room/DataStore, núcleo BLE nativo reutilizável validado na Fase 04, PoC BLE validada na Fase 05, gerenciamento utilizável do Champion C001 concluído na Fase 06, Home funcional de resumo diário local concluída na Fase 07, Fase 08 concluída com medição manual de frequência cardíaca, Fase 09 concluída com histórico D1 local-first de frequência cardíaca, Fase 10 concluída com importação local-first de treinos D3/D4, Fase 11 concluída com revisão, calibração, descarte lógico e atividades importadas locais até `READY_TO_SYNC`, Fase 12 concluída com integração nativa write-only ao Health Connect em foreground e Fase 15 concluída com calendário e histórico consolidado local.

## Governança de roadmap após a Fase 15

Número de fase e ticker de spec/task são conceitos independentes. Fases representam evolução macro do produto; tickers identificam unidades de trabalho executáveis. Coincidência numérica não cria relação semântica.

A Spec/Task `021` de normalização D4 e integridade do calendário é um trabalho corretivo posterior e **não representa a Fase 21**.

A direção autoritativa após a Fase 15 está em `docs/roadmap/post-phase-15-direction.md` e a decisão de governança está em `docs/decisions/adr-020-roadmap-phases-and-work-item-tickers.md`.

Estado final do roadmap:

- Fase 16: `completed`, executada pela Spec 023, com perfil pessoal, calibração
  contextual RUNNING/WALKING, fallback BASE, snapshots preservados e sync
  Health Connect explícito/write-only;
- Fases 17–25: `closed` por `product_maturity_no_further_phases`; não foram
  executadas nem permanecem como backlog macro.

O roadmap está `complete` após a Fase 16. `closed` não significa `completed`:
não há crédito por entregas não realizadas. Specs corretivas independentes
continuam possíveis sem reabrir fases. Código legado, migrations, schemas,
dados e evidências permanecem preservados; limpeza exige task explícita.

### Fase 16 concluída — baseline anterior à Spec 032

- A Spec 023 adicionou altura/meta editáveis, histórico local de peso, calibração
  por distância externa e perfis aprendidos independentes para corrida e
  caminhada.
- Novos treinos RUNNING/WALKING usam perfil aprendido quando disponível e BASE
  como fallback; TREADMILL e Daily Summary continuam BASE; CYCLING não usa
  passos para distância.
- Room está na versão `13`; migration `12 -> 13` cria tabela/índices de
  calibração sem inserir snapshots ou operações Health Connect. Derivação
  contextual efetiva é `watch-d3-d4-v3`, parser permanece `d4-v3`, e payload
  congelado v2 mantém provenance compatível.
- Calibração/reprocessamento não inicia sincronização automática; atividades
  históricas permanecem reproduzíveis por snapshots e guards remotos são
  fail-closed. Evidência física: `docs/integration/023-07-phase-16-physical-validation.md`.
- Validação de migration usou fixture v12 reconstruída por ausência de arquivo
  histórico v12 original; limitação permanece registrada na evidência.

### Estado atual após 032-08

- Novas importações D3/D4 usam `parserVersion = d4-v4`; `d4-v1`, `d4-v2` e
  `d4-v3` permanecem históricos identificáveis e reprocessáveis somente por ação
  explícita.
- Room está na versão `17`, com `MIGRATION_15_16` para fatos nativos D4 e
  `MIGRATION_16_17` para evidências de calibração contextual. Migrations não criam
  snapshots, alteram workflow ou executam efeitos Health Connect.
- Derivação efetiva nativa usa `watch-d3-d4-v4` para séries D4 v4 elegíveis,
  podendo materializar `WATCH_NATIVE` antes da revisão sem inferir tipo; após
  confirmação, RUNNING/WALKING aplicam `WATCH_NATIVE` ou
  `WATCH_NATIVE_CONTEXT_CALIBRATED`, enquanto fallback por comprimento de passo
  permanece explícito para séries inelegíveis e políticas não nativas.
- Review-before-sync, Health Connect write-only/local-first, projeções congeladas,
  crédito de passos da Spec 027 e histórico de BPM `WATCH_WORKOUT_ACTIVITY` foram
  preservados. Calibração e reprocessamento permanecem locais e explícitos.
- A baseline visual pós-031 permanece vigente em Exercícios; UI não expõe offsets,
  packing raw ou semântica não confirmada de `sportValue`/pausa.
- A Spec 032 foi concluída após validação manual no Champion C001 e conferência
  por dumps sanitizados do banco. Header/série, timeline `T/N`, distância nativa,
  calibração contextual, fallback, review-before-sync, Health Connect, Daily
  Summary, cadência e `CYCLING` foram validados sem regressão. Evidência:
  `docs/integration/032-08-physical-validation.md`.
- Permanecem como riscos técnicos semântica de `pauseStateRaw` não zero,
  unidade funcional de `sportValue`, campos reservados ou desconhecidos,
  política de reprocessamento de históricos, tolerâncias para divergências
  futuras, cobertura de samples acima de 255 passos, retenção de payload
  diagnóstico e variações de firmware/layout não observadas. A referência
  externa manual permanece sujeita a erro e serve somente como referência de
  calibração. A Spec 032 não promove essas lacunas.

### Estado atual após 033-08

- A Spec 033 permanece em execução, com 033-01 a 033-08 concluídas e 033-09
  reservada à validação física e ao fechamento. Room está na versão `19`; as
  migrations D1 e calibração permanecem aditivas e não executam efeitos remotos.
- `WatchDailyReadModel` é fonte local composta para Home, Saúde e Calendário:
  FEA1 fornece total diário de passos, D1 fornece distribuição e métricas diárias
  elegíveis, e D4 permanece detalhe do workout. Lacunas e divergências continuam
  explícitas, sem residual ou zero sintético.
- D1 granular usa `STEPS`, `DISTANCE` e `ACTIVE_CALORIES_BURNED` com payloads
  congelados. `RoomRepositories.hasSupportedHealthSyncPair()` reconhece esses
  agregados na leitura de integridade; o writer continua em
  `GranularHealthSyncRepository`.
- Home, Saúde e Calendário preservam campos e históricos legacy, usando D1 medido
  somente quando elegível. Perfil e Exercícios preservam contratos legacy,
  snapshots e workflow de workout sem reintroduzir entrypoints BLE removidos.

### Validação física parcial da Spec 033 — 2026-09-15

- A validação técnica e física positiva da Spec 033 ocorreu no Champion C001 real
  (`SM-S901E`). `lint`, builds debug/release, instalação debug e `git diff
  --check` passaram; a Spec permanece em execução por gates físicos pendentes.
- Dois refreshes da Home preservaram o fluxo local-first FEA1 + D1 + D3/D4. O
  segundo mostrou partial success sem apagar fatos já persistidos.
- O banco físico Room v19 confirmou, em 2026-09-15, D1 completo em `184/184`
  posições, `ΣD1.steps = 2466`, distância `1863,0 m`, calorias `121,8 kcal` e
  `FEA1 = 2488`. A diferença foi apresentada como `RECENT_TAIL`; nenhum residual
  sintético foi criado.
- D1 granular `STEPS`, `DISTANCE` e `ACTIVE_CALORIES_BURNED` permaneceu separado
  de `WorkoutSteps`; Health Connect só foi acionado por gesto explícito. Saúde,
  Exercícios e Perfil não reexpuseram entrypoints BLE, importação ou calibração
  por passo.
- A fila Health Connect passou a ordenar `operationSequence` antes de recência
  nas três camadas de seleção/composição, preservando cadeias incrementais.
- Evidência: `docs/integration/033-09-final-physical-validation.md`.

Riscos e bloqueios residuais: `dayOffset > 0` não foi exercitado; upgrade direto
físico v17 → v19 não foi reproduzido nesta rodada (há evidência física v18 → v19 e
banco v19 com dados legacy); `FEA1 < ΣD1`, lacuna estrutural e frescor inexplicado
não foram reproduzidos; partial success artificial não foi injetado; D4
zero/ausente permanece guard técnico. Reidratação visual nativa de
`FREE_WORKOUT`/`TREADMILL` foi corrigida e validada após o primeiro build mostrar
distância indisponível. O fluxo de revisão também aceita distância de referência
para `TREADMILL` e mantém perfil contextual separado. A transição de calibração
foi corrigida para não promover estado antes da persistência da referência.
Validação posterior confirmou calibração e sincronização de `TREADMILL` com
sucesso no dispositivo.

O Champion Companion é um aplicativo pessoal, sem objetivo de distribuição pública, beta formal, matriz multi-device ou gate de lançamento comercial. Validação física, testes e builds continuam sendo usados quando uma task específica exigir evidência.

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

O refinamento visual da **Spec 017** foi concluído de forma independente das
fases anteriores: margens e cabeçalhos foram alinhados nas superfícies de
Calendário, detalhe diário, Dispositivo, Selecionar treino e Preparar. O
comportamento factual dessas telas permanece o mesmo; a spec não alterou
estado, contratos, navegação, BLE ou persistência. Ver
`docs/specs/017-alinhamento-visual-margens-cabecalhos.md` e o overview
`docs/tasks/017-alinhamento-visual-margens-cabecalhos/017-00-overview.md`.

Escopo funcional futuro atual não inclui, embora o código preserve infraestrutura
histórica da Fase 13 protegida por guard operacional:

- leitura do Health Connect;
- GPS de sessão, localização persistida, rotas ou `ExerciseRoute`;
- `WorkManager`;
- background BLE permanente;
- sync agressivo ou automático ao abrir o app.

Backlog local herdado mapeado no início da Fase 12 passou a ser processado pela fila persistida do Health Connect, com retry foreground e preservação de sucesso local.

- atividades `READY_TO_SYNC` continuam dependentes de ação explícita do usuário para sincronização.

`SettingsScreen` deixou de ser placeholder. `AppPreferences.healthSyncEnabled` participa do fluxo funcional da integração. `ChampionDatabase` está em `v20`; as migrations efetivas da Fase 014, da Spec/Task `021`, da Spec/Task `022`, da Spec 023 e da Spec 032 são `MIGRATION_8_9` (Room v9, métricas locais), `MIGRATION_9_10` (schema 10, projeções filhas), `MIGRATION_10_11` (schema 11, índice de origem de amostras D4), `MIGRATION_11_12` (schema 12, distância manual de bicicleta), `MIGRATION_12_13` (schema 13, calibrações de comprimento do passo), `MIGRATION_15_16` (schema 16, fatos nativos D4 e header), `MIGRATION_16_17` (schema 17, evidências de calibração contextual), `MIGRATION_17_18` (fatos D1 completos), `MIGRATION_18_19` (cutover diário) e `MIGRATION_19_20` (FC máxima pessoal), todas aditivas e sem efeitos remotos durante upgrade. Distância e calorias continuam dependentes de dados pessoais suficientes; quando indisponíveis, Home, Saúde e Exercícios preservam ausência segura sem inventar valores.

## Registro factual final da Fase 014

- O parser efetivo de novas importações é `d4-v3`; `d4-v1` e `d4-v2` permanecem
  legados identificáveis e reprocessáveis somente por ação explícita.
- A derivação congela `stepLengthMeters` como distância de um passo individual,
  origem, método e versão por atividade. Distância canônica usa somente a soma
  dos intervalos elegíveis; valores originais, calibrados históricos e agregados
  permanecem diagnósticos.
- Projeções filhas Health Connect usam operações, IDs, versões e payloads
  congelados por filho, com retry foreground explícito e reconciliação por ID
  exato/delete-before-insert. Room permanece fonte local; Health Connect é
  destino write-only.
- A validação física confirmou somente os cenários exercitados: Gate A passou,
  Gates B, C, D, E e G ficaram `N/A`, e Gate F passou para autorização, falha
  parcial e retry observados após `014-17`. Provider foi observado manualmente;
  o app não executa readback.
- Permanecem riscos de cobertura física de fixtures, midpoint,
  delete-before-insert, identidade congelada em matriz completa e variações de
  firmware. Fase 013 continua `closed` / `partially_delivered` /
  `product_reorientation`.

A Spec/Task `021` complementou o estado efetivo posterior da Fase 014 com parser `d4-v3`, derivação `watch-d3-d4-v2`, duração oficial autoritativa, normalização dos samples D4 dentro da janela da atividade e correção do candidate semântico usado pelo calendário. Esse trabalho não altera a numeração das fases.

Spec 018 corrigiu falso `UNRESOLVED_AGGREGATE` no Calendário após reimportação
D3/D4: `recordedAt` original é preservado e caminhos de reimportação falham
com segurança quando a nova atividade não mantém ordem temporal válida. Ver
`docs/specs/018-integridade-do-calendario-apos-importacao-de-treino.md` e ADR-016.

Fechamento factual da Fase 12 em 2026-07-28: correção de identidade por variante concluída, Gate C físico concluído no Galaxy S22 com Health Connect real e builds debug/release validados. Integração permanece write-only, foreground e local-first; leitura remota, `WorkManager`, foreground service da sessão e rotas continuam ausentes do código da Fase 12. Backup e device transfer continuam negados.

## Estado factual da Spec 027 em 2026-08-24

Gate A da Spec 027 permanece `go_with_adjustments`: atividade antes do Daily e
Daily antes da atividade foram observados sem inflação, retry não criou segundo
crédito, `StepsCadenceRecord` permaneceu independente e Health Sync/Strava exibiu
cadência. A timeline 1 Hz foi integrada localmente até `027-10`, sem substituir
SOURCE, estatísticas históricas ou a projeção Health Connect por buckets D4.

A validação final tentou importar as duas atividades disponíveis no relógio
(uma longa e uma curta) no `SM-S901E`, mas o fluxo BLE expirou após cerca de 35
segundos e a UI registrou zero atividades inseridas; foi confirmado que o
relógio estava descarregado. Após recarga, duas atividades foram importadas e
sincronizadas individualmente: uma de `32:17` com `4.605` passos e outra de
`05:30` com `639` passos; ambas terminaram `SYNCED`. A validação física foi
concluída com limitações: mudança de dia não foi exercitada, Health Sync/Strava
não foi repetido para essas duas atividades e `SYNCED` não é readback do Health
Connect. Os achados de timeline 1 Hz foram corrigidos em `027-12`, e a
implementação/UI do Gate B foi removida em `027-15` por decisão de produto. O
fechamento documental foi concluído em `027-16`: Gate A permanece
`go_with_adjustments` e Gate B está `retired_by_product_decision`. Não há
implementação, estado ou UI ativos da timeline local 1 Hz. BPM raw continua
projetado para `HeartRateRecord`, cadência D4 para `StepsCadenceRecord`, e
identidades, payloads, retry, Daily e crédito de Gate A permanecem inalterados.
Health Connect continua write-only, sem nova sincronização física ou readback;
o histórico de timeline permanece preservado em documentação e screenshot.

## Política histórica autorizada da Fase 13 — não vigente

A ADR-014 autoriza rota persistida somente para caminhada/corrida iniciada
explicitamente pelo usuário. A base de foreground service mínimo, persistência
de sessão e recuperação explícita já está implementada parcialmente na Fase 13;
GPS preciso, captura incremental de rota e sincronização da mesma sessão Health
Connect foram requisitos das subtarefas históricas. Não são backlog ativo nem
autorizam expansão phone após o encerramento documental.

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

`feature/health` expõe `HealthViewModel`, hub de métricas, detalhe de frequência cardíaca com medição manual persistida na superfície própria, importação D1 explícita por pull-to-refresh e histórico consolidado de manual, D1 e amostras de atividades importadas não descartadas, com agregações e gráfico local; peso, passos e distância seguem usando dados já persistidos.

`feature/exercises` expõe fluxo D3/D4 funcional com coordinator foreground, `ExercisesViewModel`, CTA explícita, progresso textual, confirmação de saída, cancelamento em background, resultado seguro, filtros, lista, detalhe, revisão local, calibração e descarte lógico.

Erros devem virar estado UI seguro; detalhes técnicos ficam no Logcat de debug.

## Variantes

- `debug`: `br.com.mwconsulting.championcompanion.dev`, `Champion Companion Dev`, `1.0-dev`
- `release`: `br.com.mwconsulting.championcompanion`, `Champion Companion`, `1.0`

Release permanece útil para validar o build, mas não existe objetivo atual de distribuição pública ou gate de lançamento comercial.

## Privacidade e fontes

Logs estruturados usam `android.util.Log` apenas para diagnóstico em debug. Não registrar payload BLE, bytes crus, hexadecimal, MAC, coordenadas, rotas, tokens, credenciais, identificadores pessoais ou dados de saúde. Release deve manter logs diagnósticos inativos ou sem detalhes.

## Fontes autoritativas

- Requisitos: `docs/specs/`
- Direção de produto: `docs/roadmap/README.md` e `docs/roadmap/post-phase-15-direction.md`
- Decisões: `docs/decisions/`
- Protocolos: `docs/protocol/`
