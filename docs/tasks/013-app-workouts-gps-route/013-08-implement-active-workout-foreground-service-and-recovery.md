# 013-08 — Implementar foreground service de treino ativo e recuperação

- **Ticker:** `013`
- **Número:** `08`
- **Status:** in_progress

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
[`013-08-foreground-service-recovery-follow-up.md`](../../../integration/013-08-foreground-service-recovery-follow-up.md).

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
