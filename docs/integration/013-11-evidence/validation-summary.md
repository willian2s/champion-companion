# Validação física 013-11

- Data: 2026-08-05
- Dispositivo: Galaxy S22
- Build/commit: `9715592`; `lint assembleDebug` aprovado
- Resultado geral: BLOCKED

## Matriz

| Gate | Resultado | Evidência | Observações |
|---|---|---|---|
| Conexão inicial | PASS | `01-watch-connected.png`, `service-connected.txt` | Participação explícita conectou; treino continuou; FGS ativo. |
| Desconexão | FAIL | `02-watch-disconnected.png`, `03-active-after-disconnect-stale.png`, `service-disconnected.txt` | Dispositivo mostrou desconectado e FGS permaneceu; ao retornar a ActiveWorkout, estado exibido voltou a `Conectado`. |
| Reconexão explícita | BLOCKED | `logcat-sanitized.txt` | `Tentar novamente` não ficou observável em ActiveWorkout após retorno; segunda execução não alcançou sessão ativa por bloqueio do handoff. |
| Conflito de owner | BLOCKED | — | Sem segunda sessão ativa operacional para testar operações concorrentes. |
| Cleanup durante conexão | BLOCKED | `service-after-finish.txt` | Não foi possível iniciar tentativa controlada durante `CONNECTING`; não houve alteração em `013-08`. |
| Nova sessão após cleanup | BLOCKED | `service-after-discard.txt` | Descarte liberou serviço; nova preparação terminou em `Solicitação de início emitida`, sem sessão ativa. |
| Recuperação | BLOCKED | — | `am crash` não foi executado sem sessão ativa operacional. |
| Ausência segura de BPM/passos | PASS | `01-watch-connected.png`, `logcat-sanitized.txt` | UI informou ausência; nenhuma atribuição especulativa foi feita. |

## Comandos executados

```text
git branch --show-current
git status --short
git diff --check
adb devices
./gradlew lint assembleDebug
./gradlew installDebug
adb logcat -c
adb logcat --pid="$(adb shell pidof -s br.com.mwconsulting.championcompanion.dev)" -v threadtime > /tmp/013-11-logcat.txt
adb shell dumpsys activity services br.com.mwconsulting.championcompanion.dev
adb shell dumpsys notification --noredact
adb exec-out screencap -p
adb shell uiautomator dump
adb shell input tap/swipe
```

`testDebugUnitTest` não foi executado.

## Problemas encontrados

1. Após desconectar em `Mais → Dispositivo`, a tela de dispositivo mostrou
   `Desconectado`, mas o retorno a `ActiveWorkout` mostrou `Status: Conectado`,
   sem ação `Tentar novamente` observável.
2. Repetição controlada não alcançou sessão ativa: após countdown, a preparação
   ficou em `Solicitação de início emitida`, sem FGS e sem superfície ativa. O
   sintoma é compatível com bloqueio conhecido do handoff compartilhado de
   `013-08`, mas a causa não foi isolada nesta execução.

## Correções aplicadas

Nenhuma correção de produção. Nenhum arquivo de `013-08`, protocolo BLE,
contrato, banco ou teste foi alterado.

## Riscos residuais

- Reconexão `DISCONNECTED → CONNECTING → CONNECTED` não foi comprovada ponta a
  ponta.
- Conflito de owner, cancelamento durante handshake, rollover/perda de lease e
  recuperação pós-process-death continuam sem evidência física.
- Handoff da sessão ativa continua bloqueando repetição dos gates dependentes.
- BPM/passos live continuam sem protocolo esportivo seguro comprovado.

## Decisão de aceite

Manter `013-11` como `in_progress`. Não marcar checklist como concluído.
