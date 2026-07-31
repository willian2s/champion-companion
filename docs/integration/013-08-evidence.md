# Evidências sanitizadas — `013-08`

- **Data:** 2026-07-31
- **Dispositivo:** Galaxy S22 `SM-S901E`, Android 16
- **Serial usado:** serial ADB selecionado explicitamente; valor omitido
- **Pacote:** variante debug

## Arquivos

As capturas gerais abaixo já removem barra de status, gaveta de terceiros e dados
fora da sessão. Não contêm coordenadas, BPM, payload BLE, tokens, credenciais ou
identificadores pessoais.

- `013-08-evidence/preparation-3.png`
- `013-08-evidence/countdown-3-intermediate.png`
- `013-08-evidence/active-portrait.png`
- `013-08-evidence/active-landscape.png`
- `013-08-evidence/active-font-130.png`
- `013-08-evidence/recovery.png`
- `013-08-evidence/finish-confirmation.png`
- `013-08-evidence/discard-confirmation.png`

Capturas de notificação sanitizadas nesta execução, com horários civis removidos:

- `013-08-evidence/notification-active-sanitized.png`
- `013-08-evidence/notification-paused-sanitized.png`
- `013-08-evidence/notification-paused-actions-sanitized.png`

## Gate executado

Countdown `3` exibiu estado intermediário antes de zero. Dump imediato de
`dumpsys activity services` retornou nenhum serviço ativo. Após a transição
final, o mesmo dump confirmou `WorkoutRecordingService` em foreground, canal
`workout_session_active`, duas ações e tipo FGS `0x00000100`.

Force-stop seguido de abertura explícita exibiu recuperação. Retomada promoveu
FGS novamente. Pausa pela tela atualizou notificação para `pausado`; expansão da
notificação exibiu `Continuar` e `Finalizar`. Ação direta `Continuar` não foi
considerada evidência conclusiva nesta repetição.

Gate adicional de tela apagada: com sessão ativa e notificação persistente, a
tela permaneceu em estado `Dozing` por 75 segundos. Dumps sanitizados antes e
depois confirmaram `WorkoutRecordingService` em foreground, canal
`workout_session_active`, duas ações e tipo FGS `0x00000100`. A janela cobriu mais
de dois intervalos previstos de checkpoint; não foi extraída revisão ou duração
temporal bruta do banco. Ao retornar pela notificação, a sessão correta abriu,
pausa exibiu `PAUSED`/`Continuar`, e finalização seguida de descarte confirmado
removeu o serviço e a notificação.

Tentativa de morte real do processo: `am kill` não encerrou o processo nem o FGS;
a tentativa de `kill -9` foi recusada pelo Android com `Operation not permitted`.
Não foi usado `force-stop` para fabricar evidência. A sessão foi finalizada e
descartada pela UI após a tentativa. Gate de process death permanece sem prova.

O registro acima corresponde somente à tentativa anterior com `am kill`/`kill -9`.

## Gate `am crash` — recuperação após reabertura — 2026-07-31

- Treino livre foi iniciado com countdown `3`; dump anterior confirmou
  `WorkoutRecordingService` em foreground, tipo `0x00000100` e duas ações.
- `adb -s SERIAL shell am crash PACKAGE` encerrou o processo do app. Após o
  comando, `pidof PACKAGE` não retornou processo e o dump de serviços não
  encontrou `WorkoutRecordingService`.
- Reabertura explícita de `MainActivity` exibiu `Treino interrompido`, `Tipo:
  Treino livre`, `Retomar treino`, `Finalizar treino` e `Descartar treino`.
- Após tocar `Retomar treino`, a UI exibiu `Treino ativo`/`Pausar`; dump posterior
  confirmou novamente FGS, tipo `0x00000100` e duas ações.
- Finalização e descarte confirmados removeram FGS e notificação. Nenhum
  `force-stop`, root, hook, banco ou instrumentação foi usado.

Este gate aceita `am crash` como morte induzida real do processo. Não representa
morte espontânea por OEM ou pressão de memória.

### Excertos sanitizados do gate

```text
before-am-crash:
  pidof: present
  WorkoutRecordingService: present
  isForeground: true
  foregroundType: 0x00000100
  notificationActions: 2

after-am-crash:
  pidof: absent
  WorkoutRecordingService: absent
  foregroundService: absent

after-reopen:
  surface: Treino interrompido
  recoveryAction: Retomar treino

after-resume:
  surface: Treino ativo
  action: Pausar
  WorkoutRecordingService: present
  isForeground: true
  foregroundType: 0x00000100
  notificationActions: 2

after-cleanup:
  WorkoutRecordingService: absent
  foregroundService: absent
  notification: absent
```

Os valores acima são estados categóricos, sem serial ADB, identificadores de
sessão, timestamps, coordenadas, BPM ou payloads.

`active-font-130.png` foi recapturada após `installDebug` e mostra controle de
finalização icon-only sem quebra de texto. `active-landscape.png` permanece
captura sanitizada de gate anterior; não é considerada prova final da matriz de
safe insets, que continua pendente.

Tentativa de alterar relógio civil foi bloqueada pelo Android com
`Operation not permitted`; `auto_time` foi restaurado para `1`. CAS persistente
e Room lento/indisponível não possuem injeção segura no build atual e continuam
bloqueados sem alteração de produção.

As três capturas originais de notificação foram movidas para fora do workspace
como artefatos locais de origem e não devem ser versionadas. As versões
`*-sanitized.png` removem os horários civis visíveis, preservando estado, duração
operacional e ações da notificação.

## Evidência estática da primeira pendência

- `WorkoutSessionOwnershipCoordinator.reconcileLocked` limita reconciliação a
  duas tentativas; conflito recarrega a revisão uma vez por tentativa e termina
  em superfície conhecida quando persiste.
- `WorkoutRecordingService.reconcileInterruptedOwnership` também limita a duas
  tentativas. FGS confirmado conserva owner/notificação; sem FGS, estado aberto
  permanece persistido para próxima abertura.
- Leitura aberta possui duas tentativas separadas por `200 ms` quando falha com
  exceção. Resultado final é `Unavailable`, distinto de ausência confirmada.
- `MainActivity` mostra aviso acionável para `Unavailable`. Fluxo com sessão já
  observada conserva `RecoverySurface`; nenhum caminho inventa ID de sessão.
- Sem teste automatizado permitido e sem writer concorrente, fake de repository,
  controle de atraso ou manipulação segura do banco, não foi produzida evidência
  física artificial. Nenhum payload, ID, instante bruto ou dado de saúde foi
  registrado nesta análise.

Limite da evidência: Room permanentemente suspenso pode reter exclusão/tombstone
até retorno ou morte do processo. Segurança permanece fechada, mas
responsividade e recuperação no mesmo processo não foram comprovadas.

## Dump sanitizado

```text
countdown-3-immediate:
ACTIVITY MANAGER SERVICES
  (nothing)

countdown-3-after:
ACTIVITY MANAGER SERVICES
  WorkoutRecordingService
  isForeground=true
  foreground type=0x00000100
  notification channel=workout_session_active
  actions=2

after recovery/retomada:
ACTIVITY MANAGER SERVICES
  WorkoutRecordingService
  isForeground=true
  foreground type=0x00000100
  notification channel=workout_session_active
  actions=2

screen-off-before:
ACTIVITY MANAGER SERVICES
  WorkoutRecordingService
  isForeground=true
  foreground type=0x00000100
  notification channel=workout_session_active
  actions=2

screen-off-after:
POWER
  wakefulness=Dozing
ACTIVITY MANAGER SERVICES
  WorkoutRecordingService
  isForeground=true
  foreground type=0x00000100
  notification channel=workout_session_active
  actions=2
```

## Estado

Evidências visuais sanitizadas e dump sanitizado estão preservados no working tree;
somente versões sanitizadas devem integrar o conjunto do PR. Gate de tela
apagada com múltiplos checkpoints foi aprovado para `health`; isso não conclui
gate de CAS, Room, relógio civil, tipos FGS dinâmicos ou matriz completa de insets.
O gate de morte induzida por `am crash` foi aprovado com recuperação confirmada;
isso não cobre morte espontânea/OEM.

## Verificação do repositório

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado no Galaxy S22.
- Nenhum teste automatizado foi criado, alterado ou executado.

## Resultado da revisão independente — gate `am crash` — 2026-07-31

- Gate `am crash` aprovado: morte de processo e recuperação após reabertura foram
  confirmadas na ordem correta.
- Subtask `013-08` permanece reprovada para conclusão por tipos FGS dinâmicos,
  liveness sob Room suspenso e demais evidências físicas pendentes.
- `git diff --check` e `./gradlew lint assembleDebug assembleRelease` passaram.
