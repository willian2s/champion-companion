# Reteste físico 013-11 — fc43550

## Ambiente

- Build testado: `lint assembleDebug`; `installDebug`
- Commit: `fc43550384f745f0b5bb42537e12ffbf786da210`
- Dispositivo: Galaxy S22
- Relógio: Champion C001
- Package: `br.com.mwconsulting.championcompanion.dev`

## Matriz

| Gate | Resultado | Evidência | Observação |
|---|---|---|---|
| Conexão inicial | PASS | `01-active-workout-connected.png`, `service-connected.txt` | Associação explícita; FGS ativo; BPM/passos ausentes. |
| Tela Dispositivo reconhece conexão do treino | PASS | `02-device-screen-connected.png` | Mostrou `Conectado`. |
| Desconexão concorrente bloqueada | FAIL | `03-device-disconnect-result.png`, `logcat-sanitized.txt` | Toque em `Desconectar` foi aceito; esperado era recusa por `APP_WORKOUT_SESSION`. |
| Desconexão física real | NOT EXECUTED | `service-after-real-disconnect.txt` | Execução encerrada após primeira divergência. |
| Retry explícito | NOT EXECUTED | `service-after-reconnect.txt` | Nenhum retry seguro após falha. |
| Ausência de reconexão automática | NOT EXECUTED | — | Gate dependia de desconexão física real. |
| Mesma sessão preservada | INCONCLUSIVE | `04-active-after-disconnect-stale.png` | Mesma tela e cronômetro continuaram, mas estado stale impediu validar retry. |
| FGS durante desconexão | PASS | `service-after-device-disconnect.txt` | `isForeground=true`, `startForegroundCount=1` após desconexão pela tela. |
| Ausência segura de BPM/passos | PASS | `01-active-workout-connected.png`, `04-active-after-disconnect-stale.png` | Nenhuma atribuição sem origem esportiva comprovada. |

## Resultado das repetições

### Repetição 1

- Treino livre iniciou e ficou ativo.
- Associação explícita mostrou `Status: Conectado`.
- Tela Dispositivo mostrou `Conectado`.
- `Desconectar` não foi bloqueado: tela mudou para `Desconectado`.
- FGS permaneceu ativo.
- Retorno ao treino mostrou `Status: Conectado`, sem `Tentar novamente`.
- Primeira divergência comprovada: tela Dispositivo `DISCONNECTED` versus
  ActiveWorkout `CONNECTED`.

### Repetição 2

- Sessão anterior foi finalizada e descartada pela UI.
- Nova preparação atingiu `Solicitação de início emitida`.
- Não houve `ActiveWorkout` operacional nem FGS.
- Repetição crítica não alcançou conexão; causa do handoff não foi isolada.

## Primeira divergência técnica

Estado interno não foi exposto por UI, dumpsys ou Logcat sanitizado:

```text
connectionManager.state = não observável
connectionManager.currentSession() = não observável
owner ForegroundBleOperationGate = não observável
WatchWorkoutParticipationCoordinator.state = não observável diretamente
ExercisesViewModel = não observável; status visual renderizado = CONNECTED
ActiveWorkout = CONNECTED
Device screen = DISCONNECTED
```

Não há evidência temporal suficiente para atribuir causa ao manager,
coordinator, owner ou ViewModel. O fato comprovado é divergência entre telas após
operação de desconexão aceita.

## Problemas encontrados

1. `Mais → Dispositivo → Desconectar` executou durante sessão com participação
   do relógio, contrariando bloqueio esperado por `APP_WORKOUT_SESSION`.
2. Após operação aceita, a tela Dispositivo mostrou `Desconectado`, mas o retorno
   à mesma sessão mostrou `Status: Conectado` e não mostrou `Tentar novamente`.
3. Nova sessão posterior ficou em `Solicitação de início emitida`, sem FGS e sem
   superfície `ActiveWorkout`.

## Decisão

- manter `013-11 in_progress`;
- manter overview `[ ]` e progresso `8/17`;
- não aplicar correção nesta execução;
- parar investigação física após preservação da primeira divergência e tentativa
  de repetição bloqueada pelo handoff.
