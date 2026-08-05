# 013-08 — Implementar foreground service de treino ativo e recuperação

- **Ticker:** `013`
- **Número:** `08`
- **Status:** cancelled

## Objetivo e resultado esperado

Implementar owner operacional persistente da sessão com foreground service, notificação, checkpoints, ações seguras e recuperação explícita. Resultado: sessão continua com tela apagada/background e sobrevive a morte recuperável do processo.

## Contexto

Sessão/domain e Room estão preparados; FGS começa somente no zero com app visível. Sem boot restore, `WorkManager` ou background location.

## Documentos obrigatórios

Spec 013; ADR-014; `013-03/04/05/07`; regras Android de FGS API 28-36; AGENTS.

## Dependências

`013-07` concluída.

## Arquivos permitidos

Novo serviço/coordinator em pacote real aprovado; manifesto; notificações/resources; session repository; DI; navegação/feature para bind e recuperação; logging seguro; task/overview.

## Arquivos e símbolos prováveis

Novo `WorkoutRecordingService`, `WorkoutSessionCoordinator`, `WorkoutNotificationController`, actions/intents do serviço, `AppWorkoutSessionRepository` e destino/tela `WorkoutRecovery`.

## Arquivos proibidos

WorkManager, boot receiver, `ACCESS_BACKGROUND_LOCATION`, captura detalhada dos tipos 013-09/10/11, Health Connect, testes.

## Requisitos cobertos

FGS location/connectedDevice; notificação; duração/pausa; lifecycle; recuperação; sem reboot automático; ações idempotentes; encerramento.

## Escopo

- serviço não exportado, `START_NOT_STICKY`;
- start visível com tipos dinâmicos apropriados;
- canal/notificação persistente;
- ações abrir/pausar/retomar/finalizar;
- checkpoint de tempos/agregados e runtime generation;
- tela/destino recuperação com retomar/finalizar/descartar;
- session stale→`AWAITING_RECOVERY` por CAS.

## Fora do escopo

GPS route detalhado, BLE live, criação final da atividade, sync e tarefas adiáveis.

## Passos de implementação

1. Definir API idempotente UI↔service e single owner por sessão.
2. Criar sessão/start transacional em zero e promover imediatamente a FGS.
3. Declarar tipos conforme sessão/capacidade ativa.
4. Implementar canal e notificação sem dados sensíveis.
5. Implementar relógio monotônico, active/paused durations e checkpoints pequenos.
6. Implementar pause/resume/finalize intents com estado esperado/geração.
7. Tratar task removal/process death como interrupção recuperável.
8. No próximo app open, oferecer recuperação; não iniciar sensores/serviço sozinho.
9. Confirmar reboot sem restart automático.
10. Encerrar serviço/gate/produtores em caminhos terminais e atualizar overview.

## Critérios de aceite

- serviço inicia somente após zero;
- tela apagada/background mantêm duração e notificação;
- retorno pela notificação abre sessão correta;
- rotação/recriação não duplicam serviço;
- process death deixa sessão recuperável;
- reboot não reinicia automaticamente;
- notificação não contém BPM/coordenadas/IDs pessoais;
- sem WorkManager.

## Validação manual

Start, pause/resume por app/notificação, background, tela apagada, retorno, rotação, swipe task, morte de processo, recuperação e reboot; observar tipos FGS e encerramento.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Dump/inspeção sanitizada do serviço/notificação, state transitions, roteiro manual, comandos e screenshots.

## Screenshots

Obrigatórias: notificação ativa/pausada, recuperação, sessão ativa base e landscape/fonte aumentada.

## Definição de pronto

Base operacional estável desbloqueia três frentes paralelas; overview atualizado.

## Riscos e cuidados

Android pode rejeitar start fora de contexto visível. Não manter lock/retry infinito. Finalizar será completado transacionalmente em 013-12.

## Rollback

Desabilitar entrada de treino, parar serviços ativos e reverter declaração/classe; preservar sessões como recuperáveis/descartáveis. Sem downgrade DB.

Não criar, alterar ou executar testes automatizados.

## Registro de execução — correções de consistência e segurança

### Causa do escalonamento

- Revisão independente confirmou janela `CREATED` entre persistência e start do
  FGS, navegação antes do resultado persistido, ausência de confirmações,
  checkpoints dependentes de relógio civil e falta de fluxo runtime para
  `POST_NOTIFICATIONS`.
- `CREATED` e `FINISHING` também podiam bloquear nova sessão sem superfície
  suficiente para reconciliação.

### Implementação

- Repository recebeu `createStarted`: criação, validação single owner e insert
  já em `ACTIVE` ocorrem na mesma transação Room. Falhas de solicitação ou
  promoção do FGS compensam por CAS para `AWAITING_RECOVERY`.
- Sessões abertas são observáveis. `CREATED` legado permanece visível e só pode
  ser descartado após confirmação e sucesso persistido; `ACTIVE`/`PAUSED` sem
  owner pode ser reconciliado; `FINISHING` permanece visível e descartável com
  confirmação enquanto a atividade final pertence à `013-12`.
- Runtime e UI usam `elapsedRealtimeNanos`. Operações do serviço enviam
  acumulados monotônicos ao repository. Instantes persistidos servem somente a
  auditoria/ordenação e recebem piso arredondado para milissegundo, preservando
  as invariantes e o round-trip dos mappers existentes. Não houve migration,
  alteração de entidade ou schema.
- Recuperação recebeu estados explícitos por sessão, bloqueio de ações
  duplicadas, reload após conflito/falha e navegação somente após sucesso.
  Falhas permanecem visíveis após recomposição da mesma sessão.
- Finalização ativa/recuperável e descarte exigem confirmação. `Finalizar` da
  notificação abre confirmação sanitizada na Activity, sem mutação direta.
- Android 13+ exige `POST_NOTIFICATIONS` antes de iniciar ou retomar. Prompt só
  ocorre por ação explícita; recusa explica bloqueio e oferece retry ou
  configurações do app.

### Arquivos alterados

- `MainActivity.kt`;
- `core/database/ChampionDaos.kt`;
- `core/model/AppWorkoutSession.kt`;
- `core/model/RepositoryContracts.kt`;
- `core/navigation/ChampionNavigation.kt`;
- `core/notification/WorkoutNotificationBuilder.kt`;
- `data/RoomRepositories.kt`;
- `feature/exercises/ExercisesFeatureContract.kt`;
- `feature/exercises/ExercisesViewModel.kt`;
- `feature/exercises/WorkoutRecordingService.kt`;
- `feature/exercises/WorkoutSessionOwnershipCoordinator.kt`;
- `feature/exercises/WorkoutSessionActivity.kt`;
- `feature/exercises/WorkoutShells.kt`;
- `docs/baseline/current-baseline.md`;
- documento de integração, esta task e overview.

### Comandos e resultados

- `git diff --check`: passou.
- `./gradlew app:lintDebug`: `BUILD SUCCESSFUL`; lint de main e análises estáticas
  dos source sets de teste passaram sem executar testes.
- `./gradlew assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew app:lintDebug assembleDebug assembleRelease`: `BUILD SUCCESSFUL`;
  105 tarefas, 29 executadas e 76 atualizadas.
- Uma execução agregada anterior falhou nas análises lint por arquivos Hilt
  gerados ausentes. A execução focada posterior regenerou os artefatos; as
  repetições focada e conjunta passaram. Nenhum teste ou configuração de teste
  foi alterado.
- Nenhum teste automatizado foi criado, alterado ou executado.

## Encerramento da Fase 013 — cancelada por reorientação de produto

- **Status atual:** `cancelled`.
- **Motivo:** `product_reorientation`; novos treinos iniciados pelo celular não
  continuam como direção de produto da Fase 013.
- **Resultado aproveitável:** a base de foreground service, sessão persistida,
  ownership, recuperação, notificações e estados de falha permanece preservada
  como infraestrutura potencialmente reutilizável. Isso não equivale ao aceite
  integral desta subtarefa.
- **Execução e evidências preservadas:** todos os registros históricos acima,
  incluindo gates aprovados, bloqueios, riscos e referências a evidências
  sanitizadas em `docs/integration/013-08-evidence/`, continuam válidos como
  histórico. Nenhuma ausência de evidência foi convertida em sucesso.
- **Bloqueios não reclassificados:** CAS persistente, Room lento ou
  indisponível, relógio civil, composição dinâmica de tipos FGS, insets e
  demais lacunas físicas permanecem documentados; não foram executados novos
  gates nesta decisão.
- **Destino:** preservar migrations, schemas, sessões, dados e evidências. Não
  reabrir implementação phone nesta subtarefa; a desativação operacional de
  pontos de entrada é etapa posterior do fechamento da Fase 013.

## Registros históricos anteriores ao cancelamento

As seções de execução abaixo registram o estado observado antes da decisão de
produto. Menções históricas a `in_progress`, progresso e gates pendentes não
substituem o status atual `cancelled` desta subtarefa.

### Estado e riscos residuais

- Status permanece `in_progress`; item `013-08` continua desmarcado e progresso
  permanece `7/17` até validação manual e evidências visuais.
- Permanecem pendentes os cenários físicos de background/tela apagada,
  pause/resume por notificação, process death, swipe, reboot, falha forçada de
  promoção, alteração do relógio civil e recusa/concessão de notificações.
- Screenshots de sessão, notificação, recuperação, confirmações, landscape e
  fonte aumentada ainda precisam ser produzidos.
- GPS live, BLE live, Health Connect e criação da atividade revisável não foram
  implementados. `FINISHING` é deliberadamente observável e continua dependente
  de `013-12`, em vez de receber finalização fictícia nesta subtarefa.

## Registro de execução — bloqueadores pós-revisão

### Correções

- Removido descarte automático de `CREATED` em Activity, ViewModel e serviço.
  Recuperação mostra a sessão incompleta e permite apenas descarte confirmado.
- Removidos injeção, owner e ativação de `WorkoutLocationSource` no serviço.
  `foregroundServiceType=location` permanece somente como capacidade declarada;
  captura GPS pertence a `013-09`.
- Retomada de sessão pausada valida permissão e visibilidade de notificações na
  UI e no serviço. Sem autorização, estado permanece `PAUSED`.
- Dispatch de comandos captura falha síncrona, expõe aviso seguro e mantém
  confirmação disponível para retry.
- Notificação não recebeu GPS ou relógio sintéticos. Estados categóricos seguem
  pendentes dos handoffs reais de `013-09` e `013-11`.

### Comandos e evidências

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 26 executadas e 77 atualizadas.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalação concluída em um
  Galaxy S22 `SM-S901E - 16`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado

- Status permanece `in_progress`; item continua `[ ]` e progresso `7/17`.
- Validação manual de `CREATED`, revogação durante pausa, falha de dispatch,
  ausência de captura GPS e screenshots continua pendente.
- GPS/relógio categóricos da notificação permanecem risco documentado e
  follow-up de `013-09/11`.

## Registro de revisão independente final

- Revisão estática final: aprovada com ressalvas, sem bloqueadores estáticos.
- Checklist SDD confirmado: ticker `013`, 17 itens, `013-08` em `in_progress`,
  item desmarcado e progresso `7/17`.
- Pendência média: falha assíncrona após aceite de finalização ainda precisa
  manter confirmação/retry até mutação persistida.
- Gate manual, screenshots e dumps sanitizados continuam pendentes; a
  subtarefa não deve ser marcada como concluída antes dessas evidências.

## Registro de execução — reconciliação CAS e bloqueio do canal

### Correções

- Compensação do serviço passou a aceitar sucesso somente em
  `AWAITING_RECOVERY`. Em conflito, recarrega a revisão e tenta novamente uma
  única vez; falha persistente preserva estado aberto observável, sem loop.
- Sessão ainda `ACTIVE`/`PAUSED` com foreground válido mantém owner. Quando a
  promoção não existe, serviço para e `observeOpen` garante reconciliação no
  próximo app open. `CREATED` permanece intocada.
- Gate de notificação considera permissão runtime em Android 13+,
  `NotificationManagerCompat.areNotificationsEnabled()` em todas as versões e
  canal com `IMPORTANCE_NONE` quando disponível.
- UI explica bloqueio global ou do canal e abre configuração correspondente.
  Solicitação runtime continua exclusivamente acionada pelo usuário.

### Comandos e evidências

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 28 executadas e 75 atualizadas na validação final.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalação concluída no Galaxy
  S22 `SM-S901E - 16`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado

- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.

As pendências adversariais foram separadas em
[`013-08-foreground-service-recovery-follow-up.md`](../../integration/013-08-foreground-service-recovery-follow-up.md).

## Revisão independente pós-correção de fundo — 2026-07-31

- Veredito: aprovado com ressalvas; nenhum bloqueador estático em tema, Compose,
  confirmação ou ações de notificação.
- Confirmados `Surface` de tela inteira, background Compose, tema sem action bar,
  botão icon-only acessível, `singleTop` e `onNewIntent` preservados.
- Ressalvas restantes: gate físico de CAS/Room/relógio/countdown `3`, evidências
  duráveis no repositório/PR e validação de safe insets em aparelhos adicionais.
- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.

## Correções visuais e validação complementar — 2026-07-31

### Correções

- O controle visual de finalização na sessão ativa agora exibe somente o ícone
  de parada, com `contentDescription` de `Finalizar treino`; confirmação segura
  permanece obrigatória e o alvo de toque não depende do texto do botão.
- `WorkoutSessionActivity` usa `Theme.ChampionCompanion.WorkoutSession`, tema
  dedicado sem action bar e sem background da splash. A tela retomada passa a
  usar somente o layout atual da sessão, sem header `Champion Companion Dev` e
  sem fundo teal residual.

### Validação complementar

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado no Galaxy S22,
  Android 16.
- Em portrait e fonte aumentada, botão de finalização permaneceu icon-only,
  com anúncio acessível `Finalizar treino` e confirmação abriu corretamente.
- Abertura da notificação retornou à tela ativa sem header do nome do app.
- Ações `Pausar` e `Finalizar` pela notificação foram confirmadas manualmente;
  a segunda abriu confirmação, sem mutação direta.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado

- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.
- Permanecem pendentes CAS forçado, banco indisponível/lento, alteração do
  relógio civil e nova revisão independente final.

## Correção de fundo da Activity — 2026-07-31

- O tema transparente dedicado removia action bar, mas deixava a confirmação e
  o primeiro frame sem fundo determinístico. `WorkoutSessionActivity` agora
  envolve todos os shells em `Surface(fillMaxSize(), MaterialTheme.colorScheme.background)`.
- `WorkoutShellCard` também pinta o background Compose em toda a área rolável;
  a janela aberta por notificação não exibe mais a cor teal da splash.
- Build, lint, instalação e diff check passaram após esta correção. A validação
  visual anterior confirmou fundo de sessão escuro coerente, sem teal residual;
  Surface também cobre confirmação e primeiro frame.
- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.
- Gate manual ainda precisa cobrir conflito CAS persistente, banco indisponível,
  canal bloqueado, bloqueio global, revogação durante pausa e retorno das
  configurações.

## Registro de execução — confirmação persistida da perda de ownership

### Causa do escalonamento

- A revisão independente final confirmou que a compensação ainda podia encerrar
  o serviço após `Conflict`, `StorageUnavailable` ou outro resultado sem
  confirmar `AWAITING_RECOVERY` nem uma superfície persistida equivalente.
- `onTaskRemoved` possuía o mesmo risco ao sempre executar cleanup depois de uma
  tentativa de interrupção sem sucesso confirmado.

### Correção

- `interrupt` agora trata explicitamente `Success`, `Conflict`, `NotFound`,
  `UnexpectedState` e `StorageUnavailable`, com no máximo duas tentativas CAS e
  no máximo uma releitura pontual após cada tentativa, sem loop ou retry
  temporal.
- O contrato `loadOpenForRecovery` confirma por transação de leitura que a sessão
  permanece em estado aberto atendido pelo fluxo de recuperação no próximo app
  open. Resultado inválido ou indisponibilidade não é confundido com ausência.
- Somente `AWAITING_RECOVERY`, ausência persistida ou superfície aberta
  confirmada permitem cleanup. Sessão `ACTIVE`/`PAUSED` com foreground válido
  mantém owner e notificação após falha persistente. Sem confirmação de leitura,
  o serviço não remove foreground nem encerra explicitamente.
- `onTaskRemoved` reutiliza a mesma reconciliação. `CREATED` continua visível e
  não recebe descarte ou transição automática.

### Comandos e resultados

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 28 executadas e 75 atualizadas. Lint analisou source sets sem executar
  testes automatizados.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado e riscos residuais

- Status permanece `in_progress`; item continua `[ ]` e progresso `7/17`.
- Gate manual e nova revisão independente ainda precisam comprovar conflito nas
  duas tentativas, indisponibilidade do banco, swipe/process death, retenção do
  foreground e abertura posterior da superfície de recuperação.
- Screenshots e dumps sanitizados obrigatórios continuam pendentes. GPS live,
  BLE live, finalização em atividade e Health Connect permanecem fora de
  `013-08`.

## Registro de execução — ownership process-scoped e ack de finalização

### Causa do escalonamento

- `MainActivity` e cada `ExercisesViewModel` verificavam `isAlive` antes de uma
  leitura suspensa. O serviço podia promover a mesma sessão durante a suspensão
  e o reconciliador ainda executar `interrupt`, removendo um owner foreground
  válido.
- A confirmação aberta pela notificação considerava o mero aceite do intent
  como conclusão. Falha assíncrona de checkpoint ou `beginFinishing` deixava os
  botões bloqueados sem confirmação persistida nem retry.

### Correções

- Adicionado `WorkoutSessionOwnershipCoordinator` singleton de processo. Mutex
  único serializa leitura/reconciliação stale com attach e promoção do serviço;
  owner mantém somente token opaco, ID da sessão e fase operacional, sem
  referência a `Activity`, `Service` ou `Context`.
- Serviço reserva attach e confirma owner somente após `startForeground`. Toda
  operação capaz de anexar, atualizar ou liberar owner usa a mesma seção
  crítica. `onDestroy` e cleanup liberam o token de forma idempotente.
- Reconciliação relê a mesma sessão, revalida owner foreground dentro da seção
  crítica imediatamente antes do CAS e mantém no máximo duas tentativas. Owner
  válido retorna à sessão ativa; falha de persistência mantém a superfície
  observada acionável.
- `isAlive` global foi removido. `MainActivity`, ViewModels e compensação de
  start rejeitado usam identidade do owner da mesma sessão, impedindo corrida
  entre múltiplas superfícies do processo.
- Finalização ativa agora expõe `Idle`, `InProgress`, `Persisted` e `Failure`.
  Sucesso só ocorre quando observação ou releitura limitada confirma
  `FINISHING`/`FINISHED`. Após 10 segundos, releitura possui limite adicional de
  2 segundos; ausência de confirmação reabilita `Confirmar` e `Cancelar` e
  informa retry seguro. Criação da atividade permanece em `013-12`.

### Validação

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 24 executadas e 79 atualizadas. Nenhum teste automatizado foi
  executado.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E - 16`.

### Estado e riscos residuais

- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.
- Gate manual ainda precisa exercitar promoção concorrente com abertura de duas
  Activities/ViewModels e falhas assíncronas antes/depois do timeout de
  finalização.
- Screenshots, dumps sanitizados, reboot, process death, indisponibilidade do
  banco e demais evidências físicas continuam pendentes.

## Registro de execução — teardown coordenado e indisponibilidade explícita

### Causa do escalonamento

- `onDestroy` liberava o token fora da serialização compartilhada. Um
  reconciliador podia confirmar `ForegroundOwned` enquanto o callback removia o
  owner, deixando sessão `ACTIVE`/`PAUSED` sem serviço ou notificação.
- Falha na leitura de `observeOpen` era convertida em `NoOpenSession`, tornando
  indisponibilidade do banco indistinguível de ausência persistida.

### Correções

- O owner recebeu fase `TEARING_DOWN`. `onDestroy` invalida essa fase de forma
  síncrona e delega a reconciliação e a liberação final a escopo process-scoped
  do coordenador. A liberação definitiva aguarda o mesmo mutex usado por
  attach, promoção e reconciliação; não depende do escopo cancelado do serviço.
- Teardown relê a sessão sob a seção crítica, usa o último estado observado
  somente quando a leitura fica indisponível e tenta a mesma reconciliação CAS
  limitada. Owner foreground concorrente de outra instância continua
  preservado.
- `reconcileOpenSession` agora devolve `Unavailable` em falha de leitura e
  reserva `NoOpenSession` para ausência persistida confirmada. Fluxos com uma
  sessão já observada preservam `RecoverySurface` ou a sessão visível com erro
  acionável para retry.

### Validação

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 24 executadas e 79 atualizadas.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E - 16`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado e bloqueios restantes

- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.
- Gate manual ainda precisa comprovar teardown concorrente, indisponibilidade do
  banco, process death, swipe, reboot e retenção/recuperação correta do owner.
- Screenshots e dumps sanitizados continuam pendentes. GPS live, BLE live,
  atividade revisável e Health Connect permanecem fora desta subtarefa.

## Registro de execução — exclusão atômica do teardown

### Causa do escalonamento

- A marcação síncrona de `TEARING_DOWN` ainda ocorria fora do `Mutex` de
  reconciliação. Entre a leitura de `FOREGROUND` e o retorno de
  `ForegroundOwned`, `onDestroy` podia invalidar o owner e deixar a sessão
  `ACTIVE`/`PAUSED` sem serviço.
- Usar `runBlocking` sobre o `Mutex` no callback poderia bloquear a thread
  principal enquanto uma continuação do próprio reconciliador aguardasse essa
  thread.

### Correção

- A exclusão process-scoped passou a usar um `Semaphore` justo, não vinculado à
  thread. Attach, promoção, operações do serviço, consultas de owner e
  reconciliação mantêm o mesmo permit mesmo quando uma operação suspensa troca
  de thread.
- `onDestroy` adquire sincronamente esse mesmo permit antes de marcar
  `TEARING_DOWN`. A seção suspensa roda em `Dispatchers.Default`; portanto, o
  callback não depende de executar uma coroutine na thread bloqueada.
- A reconciliação final continua no escopo process-scoped. `release` comum não
  remove owner em `TEARING_DOWN`; somente o fechamento correspondente conclui
  essa fase, impedindo attach novo ou cleanup concorrente de reabrir a janela.
- Limite de duas tentativas CAS/reload, resultados explícitos de
  indisponibilidade e demais correções anteriores foram preservados.

### Validação

- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 28 executadas e 75 atualizadas.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E - 16`.
- `git diff --check`: passou após código e documentação.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado e riscos manuais

- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.
- Gate manual ainda precisa forçar `onDestroy` concorrente com reconciliação e
  promoção, verificar bloqueio breve do callback, attach durante
  `TEARING_DOWN`, indisponibilidade do banco e recuperação posterior.
- Screenshots, dumps sanitizados, swipe, process death e reboot continuam
  pendentes. GPS live, BLE live, atividade revisável e Health Connect seguem
  fora de `013-08`.

## Registro de execução — teardown não bloqueante e retry limitado

### Causa do escalonamento

- `runInterruptible { ownershipPermit.acquire() }` podia adquirir o permit e
  propagar cancelamento antes de alcançar o `finally`, vazando a exclusão.
- `onDestroy` usava `acquireUninterruptibly()` e podia bloquear
  indefinidamente a thread de lifecycle durante operação Room lenta ou presa.
- `Unavailable` era distinto de ausência, mas a abertura não repetia uma falha
  transitória e o `ExercisesViewModel` executava reconciliação duplicada sem
  consumir o resultado.

### Correções

- A aquisição suspensa registra o sucesso em `AtomicBoolean` dentro do bloco
  interruptível. O `finally` externo libera o permit mesmo quando o cancelamento
  vence imediatamente após `acquire()` e antes da operação protegida.
- `onDestroy` usa somente `tryAcquire()`. Sem permit, marca `TEARING_DOWN` por
  CAS imediatamente; se um `release` comum competir, reinstala um tombstone do
  mesmo owner. Novo attach fica bloqueado e a reconciliação é sempre enfileirada
  no `processScope`, independente do scope cancelado do serviço.
- A reconciliação process-scoped continua serializada. Owner concorrente válido
  é preservado; owner em teardown só é removido após tentativa de leitura e CAS.
  O callback de lifecycle não aguarda Room nem coroutine.
- A leitura de abertura repete no máximo duas vezes, com intervalo de `200 ms`,
  antes de devolver `Unavailable`. Falha de interrupção com sessão já observada
  também recebe uma segunda tentativa limitada e mantém `RecoverySurface`
  acionável se persistir.
- A reconciliação duplicada e sem consumidor do `ExercisesViewModel` foi
  removida. `MainActivity` permanece como consumidor único de abertura; após o
  retry limitado, `Unavailable` sem sessão conhecida não fabrica ID ou destino.
  Fluxos que já observaram uma sessão mantêm estado e mensagem de retry.

### Validação

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 28 executadas e 75 atualizadas.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E - 16`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado e riscos restantes

- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.
- Gate manual ainda precisa comprovar cancelamento concorrente à aquisição,
  `onDestroy` durante promoção/reconciliação, attach bloqueado em
  `TEARING_DOWN`, Room indisponível e recuperação posterior.
- Operação Room realmente presa pode manter tombstone e operações seguintes
  suspensas no escopo do processo, mas não bloqueia a thread de lifecycle; essa
  preferência fail-closed evita devolver owner em teardown como válido.
- Screenshots, dumps sanitizados, swipe, process death e reboot continuam
  pendentes. GPS live, BLE live, atividade revisável e Health Connect seguem
  fora de `013-08`.

## Registro de execução — reserva antecipada e ID pendente

### Causa do escalonamento

- `ensureAttached` fazia a primeira leitura suspensa antes de reservar
  `ATTACHING`. Se `onDestroy` ocorresse nessa janela, não havia owner para
  invalidar e a sessão persistida podia permanecer ativa sem FGS ou notificação.
- O mesmo risco existia quando o serviço era destruído depois de
  `onStartCommand`, mas antes da coroutine de comando executar.
- `Unavailable` já possuía leitura limitada, porém `MainActivity` encerrava a
  abertura silenciosamente.

### Correção

- `onStartCommand` registra sincronamente o ID válido antes do dispatch. IDs
  concorrentes não substituem o primeiro ID aceito pela instância.
- `ensureAttached` reserva `ATTACHING` antes de `reloadSession`. Falha da leitura
  entrega teardown ao coordenador process-scoped e encerra o serviço sem deixar
  o owner anexando indefinidamente.
- Teardown sem owner instala tombstone somente por CAS sobre `null`. Owner
  concorrente nunca é sobrescrito; owner `FOREGROUND` da mesma sessão é
  revalidado sob o permit e não recebe `interrupt`.
- Attach durante `TEARING_DOWN` continua recusado. Reconciliação roda em
  `SupervisorJob` process-scoped, com duas leituras e duas tentativas CAS no
  máximo, sem depender do scope cancelado do serviço.
- Aquisição suspensa do permit permanece cancellation-safe. `onDestroy` usa
  `tryAcquire`, não aguarda Room e não bloqueia indefinidamente. `CREATED`
  permanece apenas visível para decisão explícita, sem descarte automático.
- `MainActivity` agora mostra aviso explícito quando a leitura permanece
  indisponível, orientando nova abertura do app sem fabricar ID ou destino.

### Arquivos desta correção

- `MainActivity.kt`;
- `WorkoutRecordingService.kt`;
- `WorkoutSessionOwnershipCoordinator.kt`;
- `app/src/main/res/values/strings.xml`;
- esta task, review de integração e overview.

### Validação

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 28 executadas e 75 atualizadas.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E - 16`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado e riscos restantes

- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.
- Gate manual ainda precisa destruir o serviço antes do início da coroutine,
  durante a primeira leitura e durante `TEARING_DOWN`, além de confirmar
  tombstone, preservação de owner concorrente e aviso de indisponibilidade.
- Validação física completa, screenshots e dumps sanitizados continuam
  bloqueando conclusão. GPS live, BLE live, atividade revisável, Health Connect,
  migration e schema permanecem fora desta correção.

## Registro de gate manual — execução parcial no Galaxy S22

### Cenários aprovados

- Countdowns `0`, `5` e `10` iniciaram sessão somente após preparação; `10`
  exibiu `7 segundos` antes da promoção.
- FGS ativo foi observado com tipo `health`; notificação exibiu tipo e duração.
- Pausar/continuar pela tela, background, retorno pela notificação, rotação e
  fonte aumentada funcionaram sem duplicar o serviço.
- `force-stop` produziu recuperação explícita; retomar iniciou FGS novamente.
- Finalizar exibiu confirmação e persistiu `FINISHING`; descartar exibiu
  confirmação e persistiu `DISCARDED`.
- Swipe da task deixou recuperação visível.
- Negação de `POST_NOTIFICATIONS` bloqueou início; concessão posterior liberou o
  fluxo.

### Evidências sanitizadas capturadas

- `013-08-preparation.png`, `013-08-countdown-5s.png`;
- `013-08-recovery.png`;
- `013-08-finish-confirmation.png`, `013-08-discard-confirmation.png`;
- `013-08-active-font-130.png`, `013-08-active-landscape.png`.

Arquivos ficaram no diretório temporário do ambiente. Capturas da gaveta que
continham notificações de terceiros não são evidência e não devem ser anexadas.

### Bloqueios e cenários pendentes

- Registro intermediário anterior não reproduziu os botões no System UI; a
  validação manual posterior confirmou `Pausar` e `Finalizar` diretamente na
  notificação. `Finalizar` abriu confirmação, sem mutação direta.
- Reboot foi repetido após reconexão e confirmou ausência de restart automático.
- Conflitos CAS, banco indisponível/lento e alteração do relógio civil continuam
  sem execução física.
- Countdown `3` iniciou sessão, mas não houve captura explícita do estado
  intermediário antes de zero.

### Estado

- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Gate manual, evidências finais e nova revisão independente continuam
  obrigatórios antes de marcar subtarefa como concluída.

## Complemento do gate — reboot e bloqueios de notificação

- Após reconexão do Galaxy S22, não havia `WorkoutRecordingService` em
  foreground nem notificação antes da abertura do app.
- Ao abrir o app, sessão anterior apareceu como `Treino interrompido`; retomada
  posterior promoveu FGS `health` com sucesso.
- Bloqueio global de notificações apresentou superfície `Tentar preparar
  recuperação`; após reativação nas configurações, retry e retomada funcionaram.
- Bloqueio isolado do canal exibiu mensagem específica de canal desativado e
  manteve a sessão `PAUSED`; reativação permitiu continuar.
- `dumpsys notification` confirmou ações internas `Pausar` e `Finalizar`; o
  usuário confirmou toque físico funcional nas duas ações. `Finalizar` abriu
  confirmação conforme esperado.

### Estado atualizado

- Reboot sem restart automático, bloqueio global e bloqueio isolado do canal
  agora estão validados.
- Permanecem pendentes: conflito CAS, indisponibilidade persistente do banco,
  alteração do relógio civil e captura explícita do countdown `3` antes de zero.
- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.

## Gate adversarial complementar — 2026-07-31

### Escopo e resultado

- Executado no Galaxy S22 `SM-S901E`, Android 16, com serial ADB explícito.
- Countdown `3` exibiu estado intermediário antes de zero. Dump imediato de
  `dumpsys activity services` retornou `(nothing)`; após a transição final,
  `WorkoutRecordingService` apareceu em foreground com tipo `0x00000100`, canal
  `workout_session_active` e duas ações.
- `force-stop` seguido de abertura explícita exibiu recuperação. Retomada
  promoveu o FGS novamente; pausa atualizou a notificação para estado pausado.
- Gaveta de notificações exibiu `Continuar` e `Finalizar`; a repetição atual não
  considerou o toque em `Continuar` conclusivo por falta de mudança observável
  no estado após o toque.
- Capturas sanitizadas e dump resumido foram preservados em
  `docs/integration/013-08-evidence/` e descritos em
  `docs/integration/013-08-evidence.md`.

### Desvios e bloqueios

- Alteração do relógio civil não foi executável: `date` retornou `Operation not
  permitted` no dispositivo não-root. `auto_time` foi restaurado para `1`.
- Não existe injeção segura para provocar CAS persistente ou Room lento/
  indisponível sem alterar produção ou manipular banco privado; ambos seguem
  bloqueados e não foram simulados por meios inseguros.
- Landscape e fonte aumentada possuem capturas sanitizadas anteriores, mas a
  matriz completa com navegação de três botões e cutout permanece pendente.
- A captura de fonte aumentada foi substituída após `installDebug` e confirma
  controle icon-only sem quebra de texto. A captura landscape anterior permanece
  apenas como referência visual, não como aprovação de safe insets.

### Arquivos alterados nesta execução

- `docs/integration/013-08-evidence.md`;
- `docs/integration/013-08-evidence/`;
- esta task;
- overview e follow-up da integração.

Nenhum arquivo de produção, dependência, migration ou teste foi alterado.

### Comandos executados e evidências

- `adb -s SERIAL shell input ...`: preparação, countdown, force-stop,
  recuperação, pausa e gaveta de notificações; concluídos conforme registros
  acima.
- `adb -s SERIAL shell dumpsys activity services PACKAGE`: sem serviço durante
  countdown intermediário; FGS ativo após zero e após retomada.
- `adb -s SERIAL shell settings put global auto_time 0` e `adb -s SERIAL shell
  date ...`: bloqueado por permissão; `auto_time` restaurado para `1`.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado no Galaxy S22.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado e riscos residuais

- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.
- CAS persistente, Room lento/indisponível, relógio civil e matriz completa de
  insets continuam pendentes.
- Tela apagada com múltiplos checkpoints, morte recuperável real do processo e
  tipos FGS dinâmicos `location`/`connectedDevice` também não foram encerrados;
  dump atual cobre somente `health`.
- Evidências visuais e dump sanitizado estão preservados no working tree, mas
  ainda precisam ser incluídos no conjunto do PR. Revisão independente continua
  obrigatória antes de qualquer conclusão.

### Resultado da revisão independente — 2026-07-31

- Veredito: bloqueado para conclusão; `013-08` deve permanecer `in_progress`.
- Estrutura SDD, ticker, checklist único, progresso `7/17` e item `[ ]` foram
  aprovados.
- Revisão apontou CAS/Room/relógio/insets ainda pendentes, ação `Continuar` sem
  confirmação conclusiva nesta repetição e necessidade de proveniência da
  captura landscape.
- Serial ADB foi removido da evidência documental. Fonte aumentada foi
  recapturada após `installDebug` com botão icon-only.

## Análise da primeira pendência — CAS persistente e Room

### Causa do escalonamento confirmada

- Gate físico não consegue produzir duas revisões concorrentes por fluxo de
  produto: ownership process-scoped serializa reconciliação e serviço, e ações
  de recuperação duplicadas ficam bloqueadas enquanto uma mutação está ativa.
- Indisponibilizar somente Room exigiria manipular banco privado, writer/lock
  externo, fake de DI ou knob de atraso/falha em produção. Esses meios podem
  corromper estado, alteram o artefato observado ou equivalem a teste/
  instrumentação proibidos.

### Verificação estática

- `WorkoutSessionOwnershipCoordinator` diferencia `NoOpenSession` de
  `Unavailable`, limita leitura aberta a duas tentativas com `200 ms` entre elas
  e limita CAS/reload a duas tentativas.
- Conflito persistente no coordenador retorna `RecoverySurface` com a última
  sessão conhecida. Falha de leitura com sessão conhecida também preserva essa
  superfície; falha sem sessão conhecida retorna `Unavailable`, e
  `MainActivity` orienta nova abertura sem sintetizar ID.
- `WorkoutRecordingService` limita compensação a duas tentativas. Se o FGS já
  está confirmado e a sessão continua `ACTIVE`/`PAUSED`, owner e notificação são
  preservados. Sem FGS válido, estado aberto confirmado permanece no Room e é
  reconciliado na próxima abertura.
- Exceções de repository são convertidas em `StorageUnavailable`, não em
  `NotFound`. Ausência só é aceita após leitura persistida bem-sucedida.
- `Semaphore` justo, owner atômico e fases `ATTACHING`/`FOREGROUND`/
  `TEARING_DOWN` mantêm owner único. Nenhum caminho de reconciliação possui retry
  infinito.

### Decisão e risco residual

- Nenhuma mudança de produção foi feita. Hook debug, intent especial, fake de
  repository, atraso artificial e manipulação de banco foram rejeitados.
- Código já satisfaz fail-closed para conflitos, exceções e lentidão finita:
  preserva owner válido ou superfície persistida/acionável e nunca interpreta
  indisponibilidade como ausência.
- Room permanentemente suspenso pode manter permit/tombstone até a operação
  retornar ou o processo morrer. Adicionar timeout sem contrato de latência e
  sem comprovar cancelamento transacional seguro poderia cancelar operação que
  ainda venha a confirmar; mudança não é justificável apenas para o gate.
- Reprodução física de CAS persistente e Room lento/indisponível fica
  formalmente bloqueada. Próxima avaliação deve partir de falha real
  reproduzível ou decisão explícita sobre timeout, sem instrumentar produção.

### Evidências e validação desta análise

- Caminhos inspecionados: `WorkoutRecordingService`,
  `WorkoutSessionOwnershipCoordinator`, `MainActivity`, `ExercisesViewModel`,
  contrato `AppWorkoutSessionRepository`, `AppWorkoutSessionDao` e implementação
  em `RoomRepositories`.
- Teste existente de domínio foi somente lido; nenhum teste automatizado foi
  criado, alterado ou executado.
- `git diff --check`: passou após atualização documental.
- Nenhum código, Gradle, manifesto, migration ou schema foi alterado nesta
  análise documental.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL` após a
  análise; nenhum teste automatizado foi executado.

Status permanece `in_progress`; item `[ ]`; progresso `7/17`. CAS/Room deixam de
ser uma solicitação de simulação insegura e permanecem bloqueios formais de
evidência física. Demais pendências não foram trabalhadas.

## Registro de execução — bloqueio físico do relógio civil — 2026-07-31

- A opção `Data e hora automáticas` foi restaurada pela interface do sistema; o
  fuso horário automático permaneceu habilitado.
- Ao desativar a opção automática, os controles manuais apareceram. Ao tocar em
  `Definir data`, o sistema reativou a sincronização automática imediatamente e não
  abriu o seletor. Nenhuma data ou hora foi alterada.
- Não foram usados comando `date`, alteração direta de `settings`, manipulação do
  banco, hook, atraso artificial ou instrumentação de produção. Valores temporais
  brutos não foram registrados.

### Resultado e risco residual

- Alteração controlada do relógio civil permanece bloqueada pelo sistema do Galaxy
  S22. Não há evidência física para duração monotônica, ordenação de auditoria ou
  round-trip Room sob mudança civil.
- Nenhum código foi alterado. `013-08` permanece `in_progress`, item `[ ]` e
  progresso `7/17`; conclusão continua bloqueada por evidências físicas e revisão
  independente.

### Comandos executados

- `adb -s SERIAL shell input tap ...`: restauração pela interface do sistema.
- Consultas sanitizadas de `auto_time`, `auto_time_zone` e foco de janela:
  restauração confirmada; nenhuma informação temporal bruta foi preservada.
- `git diff --check`: passou após atualização documental.
- Nenhum teste automatizado foi criado, alterado ou executado.

## Resultado da revisão independente — 2026-07-31

- Veredito: bloqueado para conclusão; `013-08` permanece `in_progress`, item `[ ]`
  e progresso `7/17`.
- Estrutura SDD, ticker, checklist único com 17 itens e registro do bloqueio físico
  foram aprovados.
- Capturas anteriores em `docs/integration/013-08-evidence/` ainda não são
  evidência sanitizada: três imagens exibiam horários civis da notificação.
  Versões `*-sanitized.png` foram geradas; os originais foram movidos para fora do
  workspace e não devem ser versionados.
- Evidência física de duração monotônica, ordenação de auditoria e round-trip Room
  sob mudança civil continua ausente. Nenhum teste automatizado foi criado,
  alterado ou executado.

## Revisão final das evidências — 2026-07-31

- Veredito: documentação aprovada; `013-08` continua bloqueada para conclusão.
- Capturas gerais permanecem descritas como previamente sanitizadas. As três
  notificações usam somente versões `*-sanitized.png`, sem relógio civil; os
  originais foram removidos do workspace.
- Ticker, checklist único, progresso `7/17`, status `in_progress`, bloqueio físico
  e riscos residuais foram confirmados. Nenhum teste automatizado foi criado,
  alterado ou executado.

## Registro de execução — tela apagada e múltiplos checkpoints — 2026-07-31

### Resultado

- Sessão ativa permaneceu com `WorkoutRecordingService` em foreground durante 75
  segundos com o dispositivo em estado `Dozing`.
- Dumps sanitizados antes/depois confirmaram canal `workout_session_active`, duas
  ações e tipo FGS `health`. A janela cobriu mais de dois intervalos previstos de
  checkpoint, sem leitura de banco ou registro de instante civil bruto.
- Retorno pela notificação abriu a sessão correta. Pausa exibiu `PAUSED` e
  `Continuar`; finalização e descarte com confirmação removeram serviço e
  notificação.

### Evidência e limites

- Evidência textual sanitizada foi adicionada a
  `docs/integration/013-08-evidence.md`; screenshots com barra de status não foram
  adicionados ao conjunto durável.
- O dump sanitizado preserva entradas `screen-off-before`/`screen-off-after`, com
  estado `Dozing`, FGS, canal e ações; não contém timestamps civis.
- Gate de tela apagada com múltiplos checkpoints foi aprovado para o tipo FGS
  exercitado (`health`). Tipos dinâmicos `location`/`connectedDevice`, morte real
  de processo, CAS/Room, relógio civil e matriz completa de insets permanecem
  pendentes.
- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Comandos e resultados

- `adb -s SERIAL shell input keyevent KEYCODE_POWER`: tela entrou em `Dozing`.
- Espera de 75 segundos; dumps filtrados antes/depois confirmaram FGS persistente.
- Abertura pela notificação, pausa, finalização e descarte foram executados pela
  interface, com confirmações.
- `git diff --check`: passou após esta atualização documental.

## Resultado da revisão independente do gate de tela apagada — 2026-07-31

- Veredito: aprovado com ressalvas; `013-08` continua `in_progress`, item `[ ]` e
  progresso `7/17`.
- Dump sanitizado com `screen-off-before`/`screen-off-after`, `Dozing`, FGS
  `health`, canal e duas ações foi aprovado sem timestamps civis.
- Sem bloqueador documental. CAS/Room, relógio civil, morte real de processo,
  tipos FGS dinâmicos e insets permanecem riscos residuais.
- `git diff --check`: passou. Nenhum teste automatizado foi criado, alterado ou
  executado.

## Registro de execução — tentativa de morte real do processo — 2026-07-31

- Sessão limpa de treino livre foi iniciada explicitamente com countdown e FGS
  `health` confirmado.
- `am kill` não encerrou o processo nem o foreground service. `kill -9` foi recusado
  pelo Android com `Operation not permitted`; `force-stop`, root, hook e
  manipulação de banco não foram usados.
- Sessão foi finalizada e descartada pela UI após a tentativa; serviço e notificação
  foram removidos.

### Resultado e risco residual

- Morte real recuperável do processo permanece bloqueada neste aparelho. Nenhuma
  evidência foi fabricada ou tratada como aprovação.
- Status permanece `in_progress`; item `[ ]`; progresso `7/17`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Comandos e resultados

- `adb -s SERIAL shell am kill PACKAGE`: processo permaneceu vivo; FGS continuou
  presente.
- `adb -s SERIAL shell kill -9 PID`: recusado com `Operation not permitted`.
- Ações de finalização e descarte foram executadas pela interface, com confirmação.
- `git diff --check`: passou após esta atualização documental.

## Resultado da revisão independente da tentativa de process death — 2026-07-31

- Veredito: registro documental aprovado com ressalvas; gate de morte real e
  conclusão de `013-08` permanecem bloqueados.
- Estrutura SDD, ticker, checklist único, progresso `7/17`, privacidade e limpeza
  posterior foram confirmados.
- `am kill` preservou processo/FGS; `kill -9` foi recusado. Próxima tentativa pode
  avaliar `am crash` se o mecanismo for aceito como morte real, sem `force-stop`,
  root, hook ou banco, com saída sanitizada antes/depois e recuperação explícita.
- Nenhum teste automatizado foi criado, alterado ou executado.

## Registro de execução — `am crash` e recuperação após reabertura — 2026-07-31

### Decisão de aceitação

- `adb shell am crash PACKAGE` foi aceito como mecanismo de morte induzida real do
  processo para este gate: o Android encerrou o processo do app e removeu o FGS,
  sem usar `force-stop`, root, hook, banco ou instrumentação de produção.
- A aceitação cobre morte induzida pelo Android; não equivale a evidência de morte
  espontânea por OEM ou pressão de memória.

### Execução e resultado

- Treino livre foi iniciado explicitamente com countdown `3`; antes do crash,
  `WorkoutRecordingService` estava em foreground com duas ações e tipo `0x00000100`.
- Comando executado: `adb -s SERIAL shell am crash PACKAGE`.
- Após o comando, `pidof PACKAGE` não retornou processo e o dump de serviços não
  encontrou `WorkoutRecordingService`/FGS.
- Reabertura explícita de `MainActivity` exibiu `Treino interrompido`, `Tipo:
  Treino livre`, `Retomar treino`, `Finalizar treino` e `Descartar treino`.
- `Retomar treino` foi acionado após a reabertura. A tela exibiu `Treino ativo` e
  `Pausar`; dump posterior confirmou o FGS novamente com tipo `0x00000100` e duas
  ações.
- Sessão foi finalizada com confirmação e descartada com confirmação. Dump final
  não encontrou serviço ou notificação ativa.

### Arquivos alterados nesta execução

- esta task;
- `docs/tasks/013-app-workouts-gps-route/013-00-overview.md`;
- `docs/integration/013-08-evidence.md`;
- `docs/integration/013-08-foreground-service-recovery-follow-up.md`.

Nenhum arquivo de produção, dependência, migration, schema ou teste foi alterado.

### Comandos, resultados e evidências

- `adb -s SERIAL shell input ...`: preparação, countdown, retomada, finalização e
  descarte; concluídos.
- `adb -s SERIAL shell dumpsys activity services PACKAGE`: FGS presente antes do
  crash, ausente após o crash e presente novamente após `Retomar treino`.
- `adb -s SERIAL shell am crash PACKAGE`: processo e FGS ausentes após execução.
- `adb -s SERIAL shell am start -n PACKAGE/MainActivity`: reabertura exibiu a
  superfície de recuperação.
- `adb -s SERIAL shell uiautomator dump ...`: confirmou recuperação e retomada por
  textos sanitizados da UI.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103 tarefas,
  3 executadas e 100 atualizadas.
- Nenhum teste automatizado foi criado, alterado ou executado; `installDebug` não
  foi necessário porque nenhuma APK ou fonte de produção mudou nesta execução.

### Estado e riscos residuais

- Status permanece `in_progress`; item continua `[ ]`; progresso permanece `7/17`.
- Gate de morte induzida real do processo foi aprovado com `am crash` e recuperação
  confirmada após reabertura.
- Permanecem pendentes CAS persistente, Room lento/indisponível, alteração do
  relógio civil, matriz completa de safe insets, tipos FGS dinâmicos
  `location`/`connectedDevice`, evidências duráveis completas e revisão
  independente final.

## Resultado da revisão independente do gate `am crash` — 2026-07-31

- Gate `am crash`: aprovado. A ordem processo/FGS ausentes → reabertura →
  recuperação visível → retomada → FGS restaurado foi considerada coerente.
- `013-08`: reprovada para conclusão. Status permanece `in_progress`, item `[ ]`
  e progresso `7/17`.
- Bloqueadores: validação física dos tipos FGS dinâmicos `location`/
  `connectedDevice`; liveness sob Room permanentemente suspenso; evidência
  durável específica do gate ainda precisa preservar excertos sanitizados dos
  estados antes/depois e da recuperação.
- Ressalva documental corrigida nesta task: o build completo foi repetido após o
  gate e passou.
- Nenhum teste automatizado foi criado, alterado ou executado.

## Análise do bloqueio — tipos FGS dinâmicos — 2026-07-31

### Causa do escalonamento confirmada

- A revisão independente aprovou o gate `am crash`, mas reprovou a conclusão de
  `013-08` porque o dump durável exercitou somente o tipo `health`; faltavam
  `location` e `connectedDevice` nos cenários em que cada capacidade estivesse
  realmente ativa.
- O manifesto já declara `health|location|connectedDevice` e suas permissões. Em
  API 34+, `WorkoutRecordingService.promoteOrUpdateForeground` mantém `health` e
  acrescenta `location` somente para sessão `ACTIVE` de caminhada/corrida. O
  serviço não acrescenta `connectedDevice`.

### Avaliação de escopo e decisão

- A tentativa física de caminhada solicitou localização precisa por ação
  explícita e alcançou fix recente, porém permaneceu em `GPS fraco`. O gate de
  preparação impediu o countdown e nenhuma sessão, rota ou FGS foi criada. Não
  houve relaxamento da prontidão nem injeção de localização para fabricar o
  cenário.
- A captura GPS após o zero e seu lifecycle pertencem a `013-09`. O aquecimento
  descartável atual termina no handoff e não é produtor contínuo da sessão. A
  heurística `ACTIVE + recordsRoute` e os tipos `health`/`location` existentes
  foram preservados, sem antecipar captura, pontos, métricas ou contrato novo.
- Mesmo com relógio conectado ao app, não existe owner `APP_WORKOUT_SESSION`,
  protocolo esportivo live comprovado, lease de participação BLE nem handoff ao
  serviço. Esses contratos pertencem a `013-11`. Conexão geral do app não prova
  BLE contínuo da sessão e não autoriza o bit `connectedDevice`.
- Decisão fail-closed: não adicionar `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`
  à promoção runtime, não criar intent/flag/API especulativa e não alterar
  ownership. O manifesto continua declarando a capacidade permitida. Completar a
  composição dinâmica exige handoffs observáveis de produtor em `013-09` e
  `013-11`; fazê-lo em `013-08` criaria capacidade sem lifecycle e possível
  corrida entre produtor e owner.
- O grafo anterior criava dependência circular de aceite: `013-09` e `013-11`
  exigiam `013-08` concluída, enquanto a revisão exigia produtores dessas duas
  subtarefas para encerrar o gate dinâmico de `013-08`. A normalização posterior
  separou base operacional de aceite integrado e permitiu iniciar as três frentes;
  nenhum item foi marcado como concluído.
- Arquivos alterados nesta execução: esta task e
  `docs/tasks/013-app-workouts-gps-route/013-00-overview.md`. Nenhum arquivo de
  produção, manifesto, contrato ou teste foi alterado; nenhum artefato de
  evidência física foi criado. O bloqueio não é resolvível
  isoladamente sem ultrapassar o escopo explícito desta subtarefa.

### Comandos e resultados

- `adb devices -l`: Galaxy S22 disponível.
- `adb shell am start ...` e ações pela UI: preparação de caminhada aberta;
  permissão precisa concedida pelo prompt do sistema; estado final `GPS fraco`.
- `adb shell dumpsys activity services PACKAGE`: nenhum serviço ativo após a
  tentativa; comportamento fail-closed confirmado para prontidão insuficiente.
- Permissões de localização concedidas somente para a tentativa foram revogadas
  ao final; arquivo temporário do `uiautomator` foi removido.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado e riscos restantes

- Status permanece `in_progress`; item continua `[ ]`; progresso permanece
  `7/17`.
- `location` ainda precisa de gate físico com caminhada/corrida realmente
  iniciada e, depois, de vínculo ao produtor contínuo de `013-09`.
- `connectedDevice` e a combinação `health|location|connectedDevice` permanecem
  bloqueados por `013-11`; não devem ser simulados usando apenas associação ou
  conexão BLE geral do app.
- Nenhuma próxima subtarefa está elegível pelo grafo atual até decisão explícita
  sobre a dependência circular do gate dinâmico.
- Liveness sob Room permanentemente suspenso, relógio civil, safe insets e
  demais evidências/revisão final continuam bloqueando a conclusão de `013-08`.

## Resultado da revisão independente do bloqueio FGS — 2026-07-31

- Veredito: aprovado com ressalvas somente para execução documental; `013-08` não
  deve ser concluída.
- Estrutura SDD, ticker, checklist único, 17 itens e progresso `7/17` foram
  confirmados. Nenhuma inconsistência de checkbox foi encontrada.
- Achado corrigido: distinção explícita entre capacidade declarada no manifesto e
  adição runtime de `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE`.
- Bloqueador principal: dependência circular entre aceite de `013-08` e handoffs
  de produtores de `013-09`/`013-11`; exige decisão de planejamento antes de outra
  subtarefa avançar.
- Riscos mantidos: lease GPS ainda não observável, `connectedDevice` sem owner/
  lease/protocolo live, Room suspenso, relógio civil, safe insets e evidências
  físicas restantes.
- `git diff --check` e `./gradlew lint assembleDebug assembleRelease` passaram;
  nenhum teste automatizado foi criado, alterado ou executado.
