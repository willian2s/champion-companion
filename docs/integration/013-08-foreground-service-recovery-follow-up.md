# Follow-up — validação adversarial do foreground service e recuperação

## Objetivo

Registrar pendências separadas após correções estáticas e gate físico parcial da
subtask `013-08`. Este documento não reabre requisitos já validados e não
substitui nova revisão independente.

## Contexto validado

- Subtask: `docs/tasks/013-app-workouts-gps-route/013-08-implement-active-workout-foreground-service-and-recovery.md`.
- Dispositivo: Galaxy S22 `SM-S901E`, Android 16.
- Reboot sem restart automático, recuperação pós-reboot, bloqueios runtime/global/
  canal de notificações, retry, pausa, retomada, ações `Pausar`/`Finalizar` da
  notificação e confirmações foram validados.
- Botão `Finalizar` icon-only, retorno pela notificação sem header do app e fundo
  da Activity sem teal residual foram corrigidos e validados.
- Build/lint/install passaram. Nenhum teste automatizado foi criado, alterado ou
  executado.

## Pendências

1. **Conflito CAS persistente:** provocar duas falhas consecutivas de
   compensação/reconciliação e confirmar sessão aberta, owner único, estado
   recuperável e ausência de retry infinito.
2. **Room lento ou indisponível:** interromper leitura e mutação persistentes,
   confirmar comportamento fail-closed, permit/tombstone liberado ou mantido de
   forma observável e superfície acionável no próximo app open.
3. **Relógio civil:** alterar o relógio durante treino ativo/pausado e observar
   checkpoints, duração monotônica, ordenação de auditoria e round-trip Room,
   sem registrar valores temporais brutos.
4. **Countdown de `3` segundos:** capturar estado intermediário antes de zero e
   confirmar que nenhum serviço/sessão `ACTIVE` surge antes da transição final.
5. **Insets de janela:** repetir portrait, landscape, fonte aumentada e
   navegação de três botões/cutout, confirmando que controles não ficam sob
   barras do sistema.
6. **Evidências duráveis:** anexar ao PR screenshots sanitizados de preparação,
   countdown, notificação ativa/pausada, recuperação, confirmações, portrait,
   landscape e fonte aumentada, além de dumps sanitizados de FGS e transições.

## Procedimento e restrições

- Usar somente dispositivo de desenvolvimento; selecionar serial ADB
  explicitamente quando houver conexões duplicadas.
- Não registrar coordenadas, BPM, payload BLE, tokens, credenciais,
  identificadores pessoais ou notificações de terceiros.
- Não criar nem executar testes automatizados nesta fase.
- Não adicionar WorkManager, boot receiver, rastreamento passivo, exportação,
  backup, GPX/TCX/FIT, GPS live fora de `013-09` ou Health Connect.
- Após execução, solicitar revisão independente e atualizar task/review sem
  marcar `013-08` como concluída até todas as evidências estarem anexadas.

## Estado

- `013-08`: `in_progress`.
- Overview: item `[ ]`, progresso `7/17`.
- Follow-up permanece aberto até validação adversarial e evidências duráveis.
