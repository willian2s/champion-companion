# Revisão da subtask 013-08 — Foreground service e recuperação

- **Data da revisão:** 2026-07-30
- **PR:** `#16 — Enhance phone workout features and finalize documentation for Fase 013`
- **Branch:** `feat/013-phone-workouts`
- **Head revisado:** `3dcf4df29a9f65754e7a992a50d9396fb9813b50`
- **CI:** Android CI `#80` concluído com sucesso
- **Subtask relacionada:** `docs/tasks/013-app-workouts-gps-route/013-08-implement-active-workout-foreground-service-and-recovery.md`
- **Status recomendado:** `in_progress`

## Objetivo deste documento

Registrar as lacunas funcionais remanescentes encontradas após a correção inicial da implementação da subtask `013-08`. O código compila, passa por lint, testes e assemble no CI, mas o resultado ainda não satisfaz completamente os requisitos de consistência transacional, recuperação explícita, confirmação segura e duração monotônica.

Este documento não substitui a validação manual exigida pela subtask.

## Resumo executivo

A implementação atual já cobre corretamente os principais fundamentos:

- `WorkoutRecordingService` não exportado e `START_NOT_STICKY`;
- foreground service com tipos `health` e `location` aplicados dinamicamente;
- notificação sanitizada e vinculada à sessão correta;
- checkpoints periódicos com revisão e runtime generation;
- ações de pausar, retomar e finalizar serializadas por mutex;
- recuperação de sessões interrompidas sem reinício automático após reboot;
- ausência de `WorkManager`, boot receiver e `ACCESS_BACKGROUND_LOCATION`;
- Android CI passando no head revisado.

Ainda permanecem três bloqueadores funcionais e duas lacunas de prioridade média.

---

## Bloqueador 1 — Criação e início da sessão não são atômicos

### Comportamento atual

Ao terminar o countdown, `ExercisesViewModel`:

1. cria uma `AppWorkoutSession` no estado `CREATED`;
2. persiste a sessão com `appWorkoutSessionRepository.create`;
3. solicita o início de `WorkoutRecordingService`;
4. o serviço carrega a sessão e executa a transição `CREATED -> ACTIVE`.

Arquivos envolvidos:

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
  - criação da sessão após o zero;
  - chamada de `ContextCompat.startForegroundService`.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutRecordingService.kt`
  - `handleStart`;
  - `ensureAttached`;
  - chamada de `sessionRepository.start`.

### Falha possível

Existe uma janela entre a persistência da sessão `CREATED` e a promoção efetiva para `ACTIVE`. Se o processo morrer, o Android rejeitar o início do FGS ou ocorrer outra falha nesse intervalo, uma sessão `CREATED` pode permanecer no banco sem owner operacional.

O fluxo de stale-session atual procura apenas sessões `ACTIVE` ou `PAUSED`. Portanto, a sessão `CREATED` órfã não é transformada em `AWAITING_RECOVERY` e não é oferecida ao usuário.

### Impacto

- sessão órfã no banco;
- tentativa de treino aparentemente iniciada, mas sem serviço;
- ausência de recuperação explícita;
- divergência do requisito de criação/start transacional no zero.

### Correção esperada

Adotar uma destas estratégias, preservando CAS e single owner:

1. persistir a sessão já em `ACTIVE` por uma operação transacional de repository específica para início; ou
2. manter `CREATED`, mas implementar compensação explícita e determinística caso o FGS não assuma a sessão; ou
3. incluir sessões `CREATED` expiradas no mecanismo de recuperação/limpeza, com regra temporal documentada.

A solução deve garantir que não exista sessão `CREATED` indefinidamente sem owner.

---

## Bloqueador 2 — A recuperação navega antes da confirmação da mutação

### Comportamento atual

Em `WorkoutRecoveryShell`, as ações disparam métodos assíncronos do ViewModel e navegam imediatamente:

- **Retomar treino:** chama `recoverSession()` e abre `ActiveWorkout` na sequência;
- **Finalizar treino:** chama `finishRecoveredSession()` e fecha a tela;
- **Descartar treino:** chama `discardRecoveredSession()` e fecha a tela.

Arquivos envolvidos:

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutSessionActivity.kt`

### Falha possível

A navegação não aguarda `RepositoryResult.Success`. Em caso de conflito CAS, falha de persistência ou estado inválido:

- a tela ativa pode ser exibida sem a sessão ter sido recuperada;
- o serviço pode não iniciar;
- a tela de recuperação pode ser fechada mesmo com a sessão ainda pendente;
- o descarte pode limpar `_recoverableSession` localmente sem confirmação da mutação persistida.

### Impacto

- UI divergente do estado persistido;
- sessão escondida sem ter sido finalizada ou descartada;
- tela ativa sem runtime válido;
- recuperação não determinística.

### Correção esperada

Modelar as operações de recuperação como eventos de resultado explícitos, por exemplo:

- `RecoveryActionState.Idle`;
- `RecoveryActionState.InProgress`;
- `RecoveryActionState.Success(destination)`;
- `RecoveryActionState.Failure(message)`.

A navegação deve ocorrer somente depois de `RepositoryResult.Success` ou depois de reconciliação comprovada de um conflito idempotente.

Durante a operação:

- desabilitar ações duplicadas;
- manter a tela aberta em caso de falha;
- mostrar mensagem segura e acionável;
- recarregar a sessão após conflito CAS.

---

## Bloqueador 3 — Finalização e descarte não possuem confirmação segura

### Regra aplicável

O ADR-014 determina que finalização e descarte exigem confirmação segura na UI:

- `docs/decisions/adr-014-controlled-workout-route-and-session-service.md`

### Comportamento atual

- o botão **Finalizar** da tela ativa envia imediatamente `ACTION_FINISH`;
- a tela de recuperação finaliza imediatamente;
- a tela de recuperação descarta imediatamente;
- a ação **Finalizar** da notificação chama diretamente o serviço.

Arquivos envolvidos:

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/notification/WorkoutNotificationBuilder.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutRecordingService.kt`

### Impacto

- toque acidental encerra ou descarta treino;
- ação destrutiva sem oportunidade de cancelamento;
- divergência explícita do ADR-014.

### Correção esperada

Adicionar confirmação na UI para:

- finalizar sessão ativa;
- finalizar sessão recuperável;
- descartar sessão recuperável.

A ação **Finalizar** da notificação não deve executar a transição terminal diretamente. Ela deve abrir `WorkoutSessionActivity` no modo de confirmação da sessão correspondente.

A confirmação deve:

- identificar apenas o tipo e a duração do treino;
- não exibir dados sensíveis;
- possuir ações claras de cancelar e confirmar;
- executar a mutação somente após confirmação explícita.

---

## Prioridade média 1 — Duração persistida depende do relógio civil

### Comportamento atual

`WorkoutRecordingService` possui `WorkoutTimer` baseado em `System.nanoTime()`, mas ele é usado apenas para renderizar a duração na notificação.

Os checkpoints e transições persistidas continuam recebendo `Instant.now()`. O domínio calcula duração com `Duration.between(activeSince, at)` ou `Duration.between(pausedSince, at)`.

Arquivos envolvidos:

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutRecordingService.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/AppWorkoutSession.kt`

### Risco

Se o relógio civil for ajustado durante o treino, manualmente ou por sincronização do sistema:

- a duração persistida pode saltar;
- o checkpoint pode ser rejeitado por ordem temporal inválida;
- a duração exibida e a duração persistida podem divergir.

### Correção esperada

Definir uma estratégia explícita para converter o delta monotônico do runtime em duração persistida, mantendo `Instant` apenas para timestamps de auditoria e ordenação.

Uma opção é persistir o delta acumulado calculado pelo runtime monotônico, em vez de recalcular toda duração a partir do relógio civil em cada checkpoint.

---

## Prioridade média 2 — Permissão runtime de notificações não está integrada ao fluxo

### Comportamento atual

O manifesto declara:

- `android.permission.POST_NOTIFICATIONS`;
- permissões de foreground service.

O fluxo de preparação observado solicita localização quando aplicável, mas não apresenta solicitação ou tratamento explícito para notificações em Android 13 ou superior.

Arquivos envolvidos:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`

### Risco

Com notificações negadas, o serviço pode continuar em foreground, mas a notificação pode não ficar disponível na gaveta. Isso compromete:

- retorno pela notificação;
- pause/resume pela notificação;
- visibilidade operacional esperada pelo usuário.

### Correção esperada

Antes do início do treino, verificar o estado da permissão de notificações quando aplicável e apresentar uma decisão explícita:

- solicitar a permissão em contexto visível;
- explicar o impacto da recusa;
- definir se o início será bloqueado ou permitido com funcionalidade reduzida;
- documentar esse comportamento na spec e na task.

Não solicitar a permissão automaticamente ao apenas abrir um shell passivo.

---

## Validação manual obrigatória ainda pendente

O CI aprovado comprova compilação, lint, testes automatizados existentes e assemble. Ele não comprova os cenários físicos exigidos pela `013-08`.

Executar e registrar:

1. iniciar treino após countdown `0`, `3`, `5` e `10`;
2. pausar e retomar pela tela;
3. pausar e retomar pela notificação;
4. apagar a tela e manter o treino por período suficiente para múltiplos checkpoints;
5. enviar o app para background e retornar pela notificação;
6. rotacionar/recriar a Activity sem duplicar serviço;
7. remover a task pela tela de recentes;
8. matar o processo e abrir novamente o app;
9. recuperar, finalizar e descartar uma sessão interrompida;
10. reiniciar o aparelho e confirmar ausência de restart automático;
11. validar tipos de FGS em caminhada/corrida e treino sem GPS;
12. negar a permissão de notificações e validar o comportamento definido;
13. alterar o relógio civil durante um treino e observar checkpoints/duração.

## Evidências obrigatórias

Anexar à PR ou referenciar em documento de validação:

- notificação de sessão ativa;
- notificação de sessão pausada;
- tela de recuperação;
- confirmação de finalização;
- confirmação de descarte;
- sessão ativa em portrait;
- sessão ativa em landscape;
- sessão ativa com fonte aumentada;
- dump sanitizado do serviço e dos tipos de FGS;
- transições persistidas antes/depois de process death;
- resultado do reboot sem reinício automático.

Nenhuma evidência deve conter coordenadas, BPM, payload BLE, tokens, credenciais ou identificadores pessoais.

---

## Critérios para retornar a subtask para `completed`

A `013-08` pode voltar para `completed` quando:

- não houver sessão `CREATED` órfã após falha de start;
- recuperação, finalização e descarte navegarem somente após sucesso persistido;
- conflitos CAS forem reconciliados sem esconder ou duplicar sessões;
- ações destrutivas exigirem confirmação segura;
- a ação de finalizar da notificação abrir confirmação em vez de finalizar diretamente;
- duração/checkpoints possuírem estratégia monotônica documentada e validada;
- o comportamento sem permissão de notificação estiver implementado e documentado;
- todos os cenários manuais obrigatórios forem executados;
- screenshots e evidências sanitizadas forem anexadas;
- `./gradlew lint assembleDebug assembleRelease` permanecer verde.

## Veredito

O head revisado representa uma melhoria significativa e corrige os defeitos mais graves encontrados na primeira análise. Entretanto, o sucesso do CI não elimina as lacunas de consistência e segurança de interação descritas acima.

Até a resolução dos três bloqueadores e a execução da validação manual, a recomendação é manter a subtask `013-08` como `in_progress`.

---

## Atualização da validação — 2026-07-31

### Resultado

Validação independente local confirmou os bloqueadores descritos neste documento.
O documento de integração foi atualizado antes da implementação das correções.
O código compila, mas a subtask não pode permanecer `completed`.

### Achados confirmados

- `ExercisesViewModel` persiste sessão `CREATED` e inicia o FGS em operações
  separadas, permitindo sessão órfã entre as duas etapas.
- Recuperação, finalização e descarte navegam ou limpam estado local antes da
  confirmação de `RepositoryResult.Success`.
- Finalização e descarte não possuem confirmação segura; a ação destrutiva da
  notificação chama o serviço diretamente.
- `System.nanoTime()` alimenta somente a renderização da notificação; duração e
  checkpoints persistidos continuam baseados em `Instant.now()`.
- `POST_NOTIFICATIONS` está declarado, mas não há solicitação runtime nem
  comportamento documentado para recusa.
- Não há evidência registrada para os cenários manuais e screenshots exigidos.

### Escopo de correção iniciado

- operação de início com ownership persistente e compensação/CAS para falha de
  promoção do FGS;
- estados explícitos de recuperação, confirmação de mutação e tratamento de
  conflitos/falhas;
- confirmações para finalizar/descartar e ação de notificação que abre a UI;
- duração persistida derivada de relógio monotônico, com `Instant` reservado a
  auditoria e ordenação;
- preparação contextual da permissão de notificações e documentação de recusa;
- recuperação observável de estados intermediários e evidências manuais
  sanitizadas.

### Estado

- Status recomendado: `in_progress`.
- Nenhum teste automatizado será criado, alterado ou executado.
- `013-08` deve permanecer desmarcada no overview até nova revisão independente
  e validação manual.

## Correções implementadas — 2026-07-31

### Consistência e duração

- `createStarted` agora valida a sessão `CREATED`, aplica a transição de domínio
  e insere diretamente `ACTIVE` dentro da mesma transação Room. A consulta de
  sessão aberta preserva single owner antes do insert.
- Falha síncrona ao solicitar o serviço e falha durante promoção a foreground
  executam compensação CAS para `AWAITING_RECOVERY`. Sessões `CREATED` legadas
  nunca são descartadas automaticamente: permanecem visíveis até descarte
  confirmado e persistido pelo usuário.
- Runtime usa `SystemClock.elapsedRealtimeNanos()`. Checkpoint, pausa, retomada,
  interrupção e início de finalização persistem acumulados derivados desse
  relógio. `Instant` ficou restrito a auditoria/ordenação e é elevado ao piso
  temporal dos acumulados, com arredondamento para cima em milissegundos para
  preservar o round-trip Room sem rejeição após atraso do relógio civil.
- Nenhuma coluna, migration, versão de banco ou schema foi alterado.

### Recuperação e ações destrutivas

- Recuperação possui `Idle`, `InProgress`, `Success` e `Failure` associados à
  sessão. Operação em progresso desabilita duplicação; falha recarrega a fonte
  persistida e permanece visível. Erro não é limpo por recomposição da mesma
  sessão.
- Navegação para sessão ativa e fechamento após descarte acontecem somente
  depois de sucesso persistido. Conflitos mantêm a tela e oferecem retry.
- Finalizar na tela ativa, finalizar na recuperação e descartar exigem diálogo
  com tipo, duração, `Confirmar` e `Cancelar`.
- A ação `Finalizar` da notificação abre `WorkoutSessionActivity` em modo de
  confirmação; não envia mais mutação direta ao serviço.
- `CREATED`, `ACTIVE`/`PAUSED` sem owner, `AWAITING_RECOVERY` e `FINISHING`
  possuem reconciliação ou superfície explícita. `FINISHING` continua pendente
  da criação transacional da atividade em `013-12`; nesta subtarefa pode ser
  mantida ou descartada com confirmação, sem bloqueio silencioso.

### Permissão de notificações

- Em Android 13 ou superior, início e retomada ficam bloqueados até
  `POST_NOTIFICATIONS` ser concedida.
- Solicitação ocorre somente por ação explícita nas superfícies de preparação
  ou recuperação. Recusa apresenta impacto, nova solicitação quando cabível e
  atalho para configurações quando a recusa se torna permanente.
- Abrir shell passivo não dispara prompt.

### Validação desta correção

- `git diff --check`: passou.
- `./gradlew app:lintDebug`: `BUILD SUCCESSFUL`; análises lint de main,
  `debugUnitTest` e `debugAndroidTest` concluíram sem executar testes.
- `./gradlew assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- Validação final conjunta `./gradlew app:lintDebug assembleDebug assembleRelease`:
  `BUILD SUCCESSFUL`; 105 tarefas, 29 executadas e 76 atualizadas.
- Execução agregada anterior de `./gradlew lint assembleDebug assembleRelease`
  havia falhado em análises lint por artefatos Hilt gerados ausentes. A execução
  focada posterior regenerou os artefatos; repetições focada e conjunta passaram
  sem alteração de testes para contornar a falha.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Pendências

- validação física integral, screenshots e evidências sanitizadas listadas
  neste documento;
- cenário de alteração do relógio civil durante checkpoints;
- falha forçada de start/promoção, morte de processo, swipe da task e reboot;
- recusa temporária/permanente de notificações e retomada após concessão;
- GPS live, relógio/BLE live, atividade revisável e Health Connect permanecem,
  respectivamente, em `013-09`, `013-11`, `013-12` e `013-14`.

---

## Revisão independente pós-correção — 2026-07-31

### Bloqueadores adicionais

- Sessões legadas `CREATED` ainda eram descartadas automaticamente por
  `MainActivity`, `ExercisesViewModel` ou `WorkoutRecordingService`, sem decisão
  explícita do usuário. A recuperação deve exibir o estado e permitir somente
  descarte confirmado.
- `WorkoutRecordingService` ainda iniciava `WorkoutLocationSource` para
  caminhada/corrida. Captura GPS live pertence a `013-09` e não pode ser
  ativada nesta subtarefa.

### Achados adicionais

- Retomada `PAUSED → ACTIVE` ainda precisa respeitar `POST_NOTIFICATIONS` quando
  a permissão for revogada durante pausa.
- Falha ao despachar comando ativo pode não produzir estado UI acionável.
- Estados de GPS/relógio ainda não aparecem de forma categórica na notificação;
  integração real permanece fora de `013-08`.

### Correção seguinte

- Remover descarte automático de `CREATED`; manter estado visível até descarte
  confirmado e persistido.
- Remover ativação da fonte Android de localização do FGS; `013-09` fará o
  handoff explícito para captura, filtro e persistência.
- Bloquear retomada sem notificação, tratar falha de dispatch com mensagem segura
  e manter ações recuperáveis.

O status permanece `in_progress`, item desmarcado e progresso `7/17`. Nenhum
teste automatizado será criado, alterado ou executado.

### Resolução dos bloqueadores adicionais

- `MainActivity`, `ExercisesViewModel` e `WorkoutRecordingService` deixaram de
  descartar `CREATED`. A Activity encaminha o estado à recuperação; o serviço
  apenas encerra a tentativa. A UI oferece somente descarte confirmado, sem
  retomar sessão que nunca recebeu `startedAt`.
- `WorkoutRecordingService` não injeta nem inicia mais
  `WorkoutLocationSourceFactory`. O tipo FGS `location` continua declarado para
  capacidade futura de caminhada/corrida, mas captura, callbacks e handoff
  pertencem integralmente a `013-09`.
- Retomada `PAUSED → ACTIVE` valida `POST_NOTIFICATIONS` e visibilidade via
  `NotificationManagerCompat` na UI e novamente no serviço. Recusa mantém a
  sessão pausada e oferece solicitação contextual ou configurações do app.
- Dispatch ativo captura falha síncrona e publica aviso acionável. Confirmação
  de finalização permanece habilitada quando o comando não foi aceito.
- GPS e relógio continuam ausentes da notificação. Estados categóricos dependem
  dos handoffs reais de `013-09` e `013-11`; nenhum valor foi inventado.

### Validação dos bloqueadores adicionais

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 26 executadas e 77 atualizadas.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E - 16`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Riscos ainda manuais

- confirmar `CREATED` legado visível e descarte somente após confirmação;
- revogar notificações durante pausa e confirmar que comando pela UI ou
  PendingIntent mantém `PAUSED`;
- forçar falha síncrona de dispatch e validar aviso/retry nas duas superfícies de
  confirmação;
- confirmar ausência de location source/callback enquanto o tipo FGS `location`
  estiver apenas declarado;
- GPS/relógio categóricos na notificação continuam follow-up de `013-09/11`.

## Correção final de reconciliação e canal — 2026-07-31

### Implementação

- Compensação de perda de ownership agora executa no máximo duas tentativas CAS.
  `Success` só é aceito como compensação quando confirma
  `AWAITING_RECOVERY`; `Conflict` recarrega a sessão e permite uma única nova
  tentativa com revisão atualizada.
- Falha persistente recarrega o estado final. Serviço já em foreground preserva
  ownership de sessão ainda `ACTIVE`/`PAUSED`; falha de promoção encerra o
  processo operacional, mas mantém a sessão aberta observável por `observeOpen`
  para reconciliação no próximo app open. Não há loop ou retry ilimitado.
- `CREATED` continua fora da compensação e nunca é descartada automaticamente.
- Disponibilidade da notificação agora combina: runtime
  `POST_NOTIFICATIONS` em Android 13+, `areNotificationsEnabled()` em todas as
  versões e importância do canal `workout_session_active`. Canal com
  `IMPORTANCE_NONE` bloqueia início/retomada.
- UI diferencia permissão runtime, bloqueio global e bloqueio do canal. Prompt
  runtime só ocorre após toque explícito; bloqueios direcionam às configurações
  do app ou do canal.

### Validação

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 28 executadas e 75 atualizadas na validação final.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E - 16`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Gate manual restante

- provocar conflito nas duas tentativas CAS e indisponibilidade persistente do
  banco, confirmando sessão aberta no próximo app open;
- bloquear somente o canal, bloquear o app inteiro e revogar runtime durante
  pausa, confirmando `PAUSED` até ação válida;
- validar retorno das telas de configuração e ausência de prompt passivo.
- validar falha assíncrona de finalização após aceite do dispatch, mantendo
  confirmação e retry disponíveis até mutação persistida.

## Pendências consolidadas após correção final — 2026-07-31

### Estado atual

- Correções estáticas concluídas para `013-08`.
- Status permanece `in_progress`.
- Item do overview permanece `[ ]`; progresso permanece `7/17`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Validação executada

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado em Galaxy S22
  `SM-S901E`, Android 16.

### Pendências manuais obrigatórias

- validar countdowns `0/3/5/10` e início somente após zero;
- validar pausa/retomada pela tela e pela notificação;
- validar background, tela apagada e múltiplos checkpoints;
- validar rotação/recriação sem duplicação de serviço;
- validar swipe da task e morte recuperável do processo;
- validar recuperação explícita de `CREATED`, `ACTIVE`/`PAUSED` e `FINISHING`;
- validar confirmação, finalização e descarte com persistência confirmada;
- validar reboot sem restart automático;
- validar conflito CAS e indisponibilidade persistente do banco;
- validar permissão runtime, bloqueio global e bloqueio isolado do canal;
- validar retomada pausada após revogação de notificações;
- validar alteração do relógio civil durante checkpoints;
- capturar screenshots sanitizados de notificação ativa/pausada, recuperação,
  confirmações, portrait, landscape e fonte aumentada;
- capturar dump sanitizado do serviço/tipos FGS e transições antes/depois de
  process death.

### Riscos residuais e fronteiras

- Tipos categóricos de GPS e relógio na notificação dependem dos handoffs de
  `013-09` e `013-11`; nenhum dado sintético foi adicionado.
- Captura GPS live permanece fora de `013-08` e pertence a `013-09`.
- Criação da atividade revisável e finalização transacional completa pertencem a
  `013-12`.
- Sincronização Health Connect permanece fora desta subtarefa.
- Nenhuma evidência manual deve conter coordenadas, BPM, payload BLE, tokens,
  credenciais ou identificadores pessoais.

### Decisão

Não marcar `013-08` como concluída até execução do gate manual e anexação das
evidências sanitizadas. Solicitar nova revisão independente após esse gate.

---

## Revisão independente final antes do gate manual — 2026-07-31

### Bloqueador

- `compensateFailedOwnership` ainda ignorava o resultado de `interrupt`. Conflito
  CAS ou falha de persistência podia deixar sessão `ACTIVE`/`PAUSED` sem owner,
  notificação ou recuperação confirmada.

### Achado adicional

- A verificação de notificações tratava Android anterior ao 13 como sempre
  habilitado e não verificava canal desativado. Retomada pode ocorrer sem
  visibilidade operacional quando canal ou notificações globais estiverem
  bloqueados.

### Correção seguinte

- Tratar `Success`, `Conflict` e falha da compensação com reconciliação CAS
  limitada e estado recuperável confirmado antes de encerrar serviço.
- Verificar `NotificationManagerCompat.areNotificationsEnabled()` e importância
  do canal em todas as APIs antes de iniciar/retomar.

Status permanece `in_progress`; item desmarcado, progresso `7/17`. Gate manual,
screenshots e evidências sanitizadas continuam obrigatórios.

## Correção da reconciliação de ownership — 2026-07-31

### Resultado

- `interrupt` agora trata explicitamente `Success`, `Conflict`, `NotFound`,
  `UnexpectedState` e `StorageUnavailable`.
- A compensação executa no máximo duas tentativas CAS, com releitura limitada e
  determinística entre tentativas; não há retry infinito.
- O novo `loadOpenForRecovery` confirma por leitura persistida se a sessão ainda
  pertence à superfície de recuperação. `AWAITING_RECOVERY`, superfície aberta
  confirmada ou ausência persistida confirmada são os únicos estados que
  permitem cleanup.
- Serviço já em foreground preserva owner e notificação enquanto a sessão
  permanece `ACTIVE` ou `PAUSED` após falha persistente. Sem foreground válido,
  superfície aberta confirmada fica disponível para reconciliação no próximo
  app open.
- `CREATED` continua sem descarte ou transição automática.
- O bloqueio global/do canal de notificações permanece aplicado em todas as APIs;
  a solicitação runtime continua restrita a ação explícita do usuário.

### Validação

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E`, Android 16.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado e riscos residuais

- Status permanece `in_progress`; item desmarcado, progresso `7/17`.
- Gate manual ainda precisa provocar conflitos CAS consecutivos, indisponibilidade
  persistente do banco, swipe/process death e confirmar retenção do foreground e
  recuperação no próximo app open.
- Screenshots e dumps sanitizados continuam obrigatórios. GPS live, BLE live,
  finalização da atividade e Health Connect permanecem fora de `013-08`.

## Correção pós-revisão — coordenação process-scoped e ack persistido

### Achados confirmados

- A leitura suspensa entre a verificação global de serviço e o CAS permitia que
  um FGS recém-promovido fosse interrompido por `MainActivity` ou por outro
  `ExercisesViewModel`.
- A confirmação da notificação bloqueava ações após aceite do dispatch, sem
  observar se `FINISHING` havia sido persistido.

### Implementação

- `WorkoutSessionOwnershipCoordinator` singleton passou a serializar attach,
  `startForeground` e reconciliação stale. Owner identifica sessão e token
  operacional; não retém componentes Android.
- Revalidação do owner da mesma sessão ocorre dentro da seção crítica antes de
  cada CAS. Conflito possui no máximo duas tentativas/reloads; owner foreground
  válido nunca é convertido em `AWAITING_RECOVERY` pelo reconciliador.
- Serviço confirma ownership somente após promoção foreground e libera token em
  cleanup/`onDestroy`. `isAlive` deixou de participar da decisão.
- Finalização ganhou estado observável. Dispatch aceito permanece
  `InProgress`; somente `FINISHING`/`FINISHED` persistido produz `Persisted`.
  Timeout de 10 segundos com releitura limitada a 2 segundos produz falha
  acionável e reabilita confirmação/cancelamento.

### Validação

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E`, Android 16.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Decisão

- Status continua `in_progress`; item permanece desmarcado e progresso `7/17`.
- Gate manual e evidências visuais/sanitizadas continuam obrigatórios, incluindo
  concorrência entre Activities/ViewModels e falha assíncrona de finalização.

## Revisão residual — teardown e leitura indisponível

### Achados

- `WorkoutRecordingService.onDestroy` liberava o token fora de
  `withSerializedServiceOperation`. Teardown concorrente podia invalidar uma
  confirmação `ForegroundOwned` e deixar sessão aberta sem FGS/notificação.
- Exceção de `observeOpen` era convertida em `NoOpenSession`, confundindo banco
  indisponível com ausência persistida.

### Resolução implementada

- Owner agora entra sincronamente em `TEARING_DOWN` no callback. Reconciliação e
  liberação definitiva rodam no escopo process-scoped do coordenador e aguardam
  o mesmo mutex de attach, promoção e reconciliação. O escopo do serviço pode
  ser cancelado sem cancelar esse fechamento.
- Novo attach não substitui owner em teardown. Revalidação não considera
  `TEARING_DOWN` como foreground válido; CAS limitado tenta expor
  `AWAITING_RECOVERY`, preservando owner foreground concorrente válido.
- `Unavailable` representa falha de leitura aberta. `NoOpenSession` permanece
  exclusivo para ausência confirmada; sessão previamente observada continua em
  `RecoverySurface` ou visível com mensagem de retry.

### Validação e decisão

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado no Galaxy S22
  `SM-S901E`, Android 16.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Status recomendado continua `in_progress`; item permanece desmarcado e
  progresso `7/17`. Gate manual, screenshots e dumps sanitizados continuam
  bloqueando conclusão.

## Correção da corrida residual de teardown — 2026-07-31

### Achado confirmado

- `markTearingDown` era atômico apenas em relação ao `AtomicReference`, mas
  permanecia fora do `Mutex` compartilhado. Um reconciliador podia observar
  owner `FOREGROUND`, sofrer teardown antes de retornar e publicar
  `ForegroundOwned` para sessão já sem runtime válido.

### Resolução

- Um `Semaphore` justo process-scoped substituiu o `Mutex` como exclusão comum.
  Por não possuir afinidade de thread, ele permanece adquirido durante leituras
  e mutações suspensas e também pode ser adquirido sincronamente por
  `onDestroy`.
- Operações suspensas protegidas executam em `Dispatchers.Default`; a espera do
  callback não depende de continuação agendada na thread principal. Attach,
  promoção, `isForegroundOwner`, CAS/reload e marcação de teardown agora têm
  uma única ordem total.
- Owner em `TEARING_DOWN` não pode ser removido por `release` comum nem
  substituído por attach. Somente a rotina process-scoped correspondente o
  libera após reconciliação, mantendo cleanup idempotente.
- Retry/CAS permanece limitado a duas tentativas. Escopo do serviço destruído
  não controla a conclusão.

### Evidências e decisão

- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 28 executadas e 75 atualizadas.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado no Galaxy S22
  `SM-S901E`, Android 16.
- `git diff --check`: passou. Nenhum teste automatizado foi criado, alterado ou
  executado.
- Status recomendado continua `in_progress`; item permanece `[ ]` e progresso
  `7/17`. Concorrência real de teardown/promoção, attach em `TEARING_DOWN`,
  banco indisponível, screenshots e dumps sanitizados ainda exigem gate manual.

## Correção dos bloqueadores finais de exclusão — 2026-07-31

### Achados confirmados

- Cancelamento após `Semaphore.acquire()` e antes do `try/finally` podia vazar
  o permit e paralisar ownership, reconciliação e teardown futuros.
- `acquireUninterruptibly()` em `onDestroy` podia bloquear thread de lifecycle
  sem limite durante Room lento ou operação presa.
- A primeira indisponibilidade em `observeOpen` encerrava silenciosamente a
  tentativa de abertura; uma reconciliação duplicada no ViewModel descartava o
  próprio resultado.

### Resolução implementada

- Aquisição suspensa ganhou flag atômica definida no mesmo bloco de
  `runInterruptible`; `finally` externo devolve o permit inclusive na janela de
  cancelamento pós-aquisição.
- `onDestroy` não espera mais. `tryAcquire` preserva ordem quando disponível;
  sem permit, CAS marca `TEARING_DOWN`. Corrida com `release` reinstala tombstone
  do owner antigo, impedindo attach até reconciliação process-scoped. Owner novo
  concorrente não é substituído e é revalidado durante a reconciliação.
- Leitura inicial e falha transitória de interrupção recebem no máximo duas
  tentativas, separadas por `200 ms`. `Unavailable` continua diferente de
  `NoOpenSession`; sessão conhecida continua exposta por `RecoverySurface` e
  mensagem de retry.
- `MainActivity` ficou como único consumidor da reconciliação de abertura. Sem
  sessão conhecida, esgotar retry não cria ID nem destino fictício. A chamada
  duplicada e sem tratamento do ViewModel foi removida.

### Validação e decisão

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 28 executadas e 75 atualizadas.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado no Galaxy S22
  `SM-S901E - 16`.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Status recomendado continua `in_progress`; item `[ ]`; progresso `7/17`.
- Gate manual deve validar cancelamento na aquisição, teardown concorrente,
  tombstone, indisponibilidade e recuperação. Screenshots e dumps sanitizados
  continuam bloqueando conclusão.

## Revisão final — janela anterior à reserva de attach

### Bloqueador confirmado

- `WorkoutRecordingService.ensureAttached` executava `reloadSession` antes de
  `reserveAttachment`. Destruição nessa suspensão encontrava owner nulo e
  `releaseAfterServiceTeardown` não instalava tombstone nem reconciliava a
  sessão persistida.
- Destruição entre `onStartCommand` e execução da coroutine possuía o mesmo
  resultado porque o ID existia somente dentro do comando ainda não iniciado.

### Resolução

- ID da sessão fica registrado sincronamente em `onStartCommand` e não pode ser
  substituído por comando concorrente de outra sessão.
- `ATTACHING` é reservado antes da primeira leitura suspensa. Exceção de leitura
  entrega reconciliação ao escopo process-scoped antes de parar o serviço.
- Sem owner, teardown cria tombstone somente se o slot continuar nulo. Owner
  concorrente é preservado, e owner foreground válido da mesma sessão impede
  `interrupt` após revalidação serializada.
- `TEARING_DOWN` continua bloqueando novo attach. Leitura e CAS permanecem
  limitados a duas tentativas; permit continua seguro contra cancelamento e
  callback de lifecycle não aguarda operação suspensa.
- `Unavailable` esgotado apresenta aviso com instrução de tentar nova abertura;
  nenhum ID ou destino é sintetizado.

### Evidências

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas, 28 executadas e 75 atualizadas.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado no Galaxy S22
  `SM-S901E - 16`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Veredito

- Bloqueador estático corrigido. Status recomendado continua `in_progress`, item
  `[ ]` e progresso `7/17`.
- Gate manual deve forçar destruição nas duas janelas, owner concorrente,
  indisponibilidade persistente e attach durante teardown. Evidências visuais e
  dumps sanitizados continuam obrigatórios.

## Revisão independente pós-correções finais — 2026-07-31

### Veredito

- Aprovado com ressalvas; nenhum bloqueador estático restante.
- `013-08` permanece `in_progress`, item `[ ]` e progresso `7/17` até gate
  manual e evidências sanitizadas.

### Ressalva

- Leituras Room possuem tentativas limitadas, mas não timeout por chamada. Uma
  operação permanentemente suspensa pode manter permit/tombstone no escopo do
  processo. Comportamento fail-closed está documentado e exige validação física;
  timeout adicional fica como melhoria futura se o cenário for reproduzido.

### Validação independente

- Confirmados `pendingSessionId`, reserva `ATTACHING` antes da leitura,
  tombstone por CAS, teardown não bloqueante, aquisição cancellation-safe,
  `Unavailable` acionável, confirmação persistida de finalização e preservação
  do escopo sem GPS/BLE live, WorkManager, Health Connect ou atividade final.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado no Galaxy S22
  `SM-S901E`, Android 16.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Gate manual restante

- destruição antes da coroutine, durante leitura e promoção;
- cancelamento após aquisição do permit e attach durante `TEARING_DOWN`;
- owner concorrente, Room lento/indisponível, process death, swipe e reboot;
- múltiplas Activities/ViewModels, notificações e confirmações;
- screenshots e dumps sanitizados obrigatórios.
