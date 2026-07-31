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
4. **Insets de janela:** repetir portrait, landscape, fonte aumentada e
   navegação de três botões/cutout, confirmando que controles não ficam sob
   barras do sistema.
5. **Evidências duráveis:** anexar ao PR screenshots sanitizados de preparação,
   countdown, notificação ativa/pausada, recuperação, confirmações, portrait,
   landscape e fonte aumentada, além de dumps sanitizados de FGS e transições.
6. **Cobertura operacional restante:** comprovar morte recuperável real do
   processo e tipos FGS dinâmicos de `location`/`connectedDevice` quando
   aplicáveis; dump atual cobre somente `health`.

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

## Atualização de execução — 2026-07-31

- Countdown `3` foi validado com captura intermediária e dump imediato sem FGS;
  dump posterior confirmou FGS somente após a transição final.
- Force-stop, recuperação, retomada, pausa e visualização expandida das ações da
  notificação foram repetidos no Galaxy S22.
- Capturas e dump sanitizado agora estão preservados no working tree em
  `docs/integration/013-08-evidence/`; ainda não integram conjunto versionado do
  PR.
- A captura de fonte aumentada foi substituída após `installDebug`; a captura
  landscape anterior permanece referência, sem aprovação de safe insets.
- Relógio civil não pôde ser alterado pelo shell do dispositivo; `auto_time` foi
  restaurado. CAS persistente e Room lento/indisponível permanecem sem método
  seguro de reprodução no build atual.
- Ação direta `Continuar` não foi considerada conclusiva nesta repetição.

Estado permanece `in_progress`; item `[ ]`; progresso `7/17`. Bloqueios reais:
CAS, Room, relógio civil, matriz completa de insets, ação `Continuar` inconclusiva
nesta repetição, morte real de processo, tipos FGS dinâmicos e revisão
independente. Arquivos de evidência ainda estão untracked e precisam ser incluídos
no conjunto do PR.

## Atualização de execução — tela apagada e múltiplos checkpoints — 2026-07-31

- Sessão ativa permaneceu com `WorkoutRecordingService` em foreground durante 75
  segundos com o dispositivo em estado `Dozing`; dumps antes/depois confirmaram
  notificação persistente, canal ativo, duas ações e tipo FGS `health`.
- A janela cobriu mais de dois intervalos previstos de checkpoint. Não houve
  leitura de banco, revisão, instante civil ou valor temporal bruto.
- Retorno pela notificação abriu sessão correta. Pausa exibiu estado `PAUSED` e
  `Continuar`; finalização e descarte, ambos confirmados, removeram serviço e
  notificação.

Resultado: gate de tela apagada com múltiplos checkpoints foi aprovado para o tipo
FGS atualmente exercitado (`health`). Tipos dinâmicos `location`/`connectedDevice`,
morte real de processo, CAS/Room, relógio civil e demais gates permanecem abertos.

## Revisão independente do gate de tela apagada — 2026-07-31

- Veredito: aprovado com ressalvas; `013-08` continua bloqueada para conclusão.
- Dump sanitizado com `screen-off-before`/`screen-off-after`, `Dozing`, FGS
  `health`, canal e duas ações foi aprovado sem timestamps civis.
- Estrutura SDD, checklist único com 17 itens, progresso `7/17`, item `[ ]` e
  riscos residuais foram confirmados. `git diff --check` passou.
- Nenhum teste automatizado foi criado, alterado ou executado.

## Decisão formal sobre CAS persistente e Room

- Não existe ação de produto que gere duas revisões concorrentes de forma
  controlada. O coordenador serializa attach, promoção, teardown e reconciliação;
  as superfícies bloqueiam ações duplicadas. Forçar o conflito exigiria writer
  externo, alteração do repository/DI ou manipulação do banco privado.
- Não existe mecanismo Android seguro para suspender ou indisponibilizar somente
  Room nesse build. Alterar permissões/arquivos do banco, manter lock externo ou
  adicionar atraso/falha por intent criaria risco de corrupção ou instrumentação
  de produção sem requisito funcional.
- Inspeção estática confirmou dois limites independentes: reconciliação de
  abertura usa `MAX_RECONCILIATION_ATTEMPTS = 2`; compensação do serviço usa
  `MAX_OWNERSHIP_RECONCILIATION_ATTEMPTS = 2`. Cada conflito recebe somente uma
  releitura antes da próxima tentativa. Não há loop de retry nesses caminhos.
- Conflito persistente preserva sessão conhecida em `RecoverySurface` ou mantém
  owner/notificação quando o FGS já está confirmado. Sem FGS válido, a sessão
  aberta persiste para reconciliação na próxima abertura.
- Exceção de leitura vira `StorageUnavailable`/`Unavailable`, nunca
  `NotFound`. Sessão conhecida permanece visível; sem sessão conhecida, abertura
  mostra aviso acionável e não fabrica ID ou destino.
- Operação Room que nunca retorna pode manter permit/tombstone até concluir ou o
  processo morrer. Isso é fail-closed para ownership, mas permanece risco não
  reproduzido: não há timeout contratual seguro para adicionar sem evidência de
  latência, garantia de cancelamento transacional e validação física.

Decisão: não alterar código de produção nem criar hook. Pendências 1 e 2 ficam
formalmente bloqueadas para reprodução física, com segurança estática confirmada
para conflito, exceção e falha finita. Travamento permanente de Room continua
risco residual explícito; nova implementação exige cenário reproduzível ou
contrato de timeout aprovado.

## Atualização de execução — restauração e bloqueio do relógio civil — 2026-07-31

- A opção `Data e hora automáticas` foi restaurada pela interface do sistema após
  a tentativa de preparação do cenário; o fuso horário automático também permaneceu
  habilitado.
- Com a opção automática desativada, a tela exibiu controles manuais, mas tocar em
  `Definir data` reativou imediatamente a sincronização automática antes de abrir o
  seletor. Nenhuma alteração civil foi aplicada.
- Não foram usados `adb shell date`, alteração direta de `settings`, manipulação do
  banco, hook, atraso artificial ou instrumentação de produção. Não foram
  registrados valores temporais brutos nesta execução. O conjunto de capturas
  anterior em `docs/integration/013-08-evidence/` ainda contém horários civis na
  tela de notificações e não é evidência sanitizada para versionamento.

Resultado: alteração controlada do relógio civil permanece bloqueada pelo sistema do
dispositivo. Não há evidência física para duração monotônica, ordenação de auditoria
ou round-trip Room sob mudança civil; não alterar código nem marcar `013-08` como
concluída.

Comandos executados: `adb -s SERIAL shell input tap ...`; consultas sanitizadas de
`auto_time`, `auto_time_zone` e foco de janela; `git diff --check`.

Resultado dos comandos: restauração confirmada para opções automáticas; nenhum teste
automatizado criado, alterado ou executado. Risco residual permanece restrito ao
cenário físico não reproduzido e à sanitização das capturas anteriores.

## Resultado da revisão independente — 2026-07-31

- Veredito: bloqueado para conclusão; estado `in_progress`, item `[ ]` e progresso
  `7/17` permanecem corretos.
- Estrutura SDD, ticker, checklist único com 17 itens e bloqueio físico foram
  aprovados.
- Riscos adicionais confirmados: capturas anteriores ainda não são duráveis nem
  sanitizadas, pois três imagens exibiam horários civis; versões
  `*-sanitized.png` foram geradas sem esses horários. Os originais permanecem
  fora do workspace e não devem ser versionados.
- Não houve bloqueador documental novo. Nenhum teste automatizado foi criado,
  alterado ou executado.

## Revisão final das evidências — 2026-07-31

- Veredito: documentação aprovada; `013-08` continua bloqueada para conclusão.
- Capturas gerais permanecem identificadas como previamente sanitizadas. As três
  capturas de notificação usam versões `*-sanitized.png`, sem relógio civil.
- Originais das notificações foram removidos do workspace e mantidos somente fora
  dele; não devem ser versionados.
- Ticker, checklist único, progresso `7/17`, status `in_progress`, bloqueio físico
  e riscos residuais foram confirmados. Nenhum teste automatizado foi criado,
  alterado ou executado.
