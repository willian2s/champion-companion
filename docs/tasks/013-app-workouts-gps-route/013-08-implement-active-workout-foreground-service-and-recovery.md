# 013-08 — Implementar foreground service de treino ativo e recuperação

- **Ticker:** `013`
- **Número:** `08`
- **Status:** completed

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
