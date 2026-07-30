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
