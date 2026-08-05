# 013-11 — Integrar participação do relógio à sessão do celular

- **Ticker:** `013`
- **Número:** `11`
- **Status:** cancelled
- **Última sincronização:** 2026-08-05

## Objetivo e resultado esperado

Adicionar participação opcional e conservadora do Champion C001 à sessão iniciada
pelo celular. O relógio pode ser associado à sessão e compartilhar o transporte
BLE de forma segura, mas o treino nunca depende dele para continuar.

BPM e passos ao vivo somente podem ser ingeridos quando a origem esportiva for
comprovada pelo protocolo. Na ausência dessa evidência, as métricas permanecem
ausentes, sem inferência ou persistência especulativa.

## Estado atual

A integração de ownership, conexão, desconexão e cleanup está implementada e
aprovada estaticamente. O CI está verde com lint e compilação debug.

O item permanece `in_progress` porque ainda faltam gates físicos específicos de
reconexão controlada, conflito entre owners e algumas corridas de lifecycle. A
ausência de BPM/passos ao vivo não é tratada como falha de implementação enquanto
o protocolo esportivo não estiver comprovado.

O overview da fase permanece com o item `013-11` desmarcado e progresso `8/17`.

## Decisão de protocolo

Evidências disponíveis não permitem atribuir com segurança métricas live à
sessão do celular:

- D0/F002 representa o fluxo de medição manual conhecido;
- FEA1 não carrega contexto temporal ou esportivo suficiente;
- A8/D8 não estão confirmados;
- não existe evidência segura para iniciar modo esporte pelo app;
- D1, D3 e D4 não devem ser importados como métricas live desta sessão.

Portanto:

- nenhum D0, F002, FEA1, A8, D8, D1, D3 ou D4 é disparado pela integração;
- BPM e passos permanecem ausentes quando a origem não é comprovada;
- nenhuma amostra é criada ou persistida por inferência;
- investigação adicional do protocolo fica para trabalho futuro específico.

## Implementação concluída

### Ownership BLE

- owner `APP_WORKOUT_SESSION` adicionado ao gate global;
- participação depende da ação explícita `Associar relógio à sessão`;
- conexão existente não implica participação automática;
- desconexão normal preserva o owner para permitir reconexão controlada;
- finalização, descarte ou recuperação persistida liberam somente o permit da
  própria sessão;
- permits são liberados por identidade, impedindo cleanup stale ou cenário ABA.

### Conexão e bootstrap

- conexão e handshake possuem cleanup transacional;
- falha, exceção ou cancelamento executam cleanup em `NonCancellable`;
- somente o transporte esperado pode ser desconectado;
- transporte substituído ou callback antigo não pode encerrar a sessão nova;
- falha de handshake não deixa transporte inválido reutilizável;
- promoção para `CONNECTED` exige bootstrap A1 concluído e identidade completa
  de sessão, transporte, permit e owner;
- sessões terminais ou em recuperação não podem ser promovidas novamente.

### Concorrência e lifecycle

- callbacks são protegidos por geração, sessão e referência do transporte;
- scanner e cancelamento são serializados sem manter o mutex principal durante
  I/O BLE;
- tentativas encerradas usam tombstone e aguardam cancelamento antes da liberação
  terminal;
- cleanup tardio não toca owner, transporte ou sessão de outra tentativa;
- o POC técnico usa o mesmo gate global com owner `DEVICE_CONNECTION`;
- scan, conexão, desconexão, reset e comandos técnicos recusam conflito antes de
  acessar o manager;
- `DEVICE_CONNECTION` e `APP_WORKOUT_SESSION` nunca são liberados por identidade
  reconstruída ou pertencente a outro fluxo.

### Desconexão, navegação e retry

- desconexão não encerra o treino nem remove o foreground service;
- a navegação preserva o `sessionId` ao abrir a tela Dispositivo e restaura
  `ActiveWorkout` somente para sessão ainda `ACTIVE` ou `PAUSED`;
- estados `FINISHING`, `AWAITING_RECOVERY`, `FINISHED` e `DISCARDED` não são
  reabertos;
- retry percorre UI → ViewModel → serviço → coordinator;
- retry faz uma única tentativa, sem loop automático;
- retry com owner retido não adquire `DEVICE_CONNECTION`;
- retry sem owner delega para a habilitação explícita fora do lock;
- estado stale é reconciliado contra transporte e lease atuais antes de expor
  `CONNECTED` ou `Tentar novamente`.

### Foreground service

- `connectedDevice` só é promovido após transporte aceito e bootstrap concluído;
- durante conexão, o serviço mantém apenas os tipos já comprovados;
- ausência ou desconexão do relógio não interrompe cronômetro, GPS ou sessão;
- cleanup do serviço libera o owner BLE somente após estado terminal ou
  recuperação persistida.

## Validações já realizadas

- sessão funciona sem dispositivo associado;
- sessão funciona com relógio associado, mas sem participação;
- associação explícita apresenta estado conectado;
- ausência de BPM/passos é apresentada de forma segura;
- nenhum comando BLE proibido foi observado no coordinator;
- foreground service permaneceu ativo após associação;
- desconexão física pela tela Dispositivo manteve o treino e o FGS;
- retorno da tela Dispositivo restaurou a mesma superfície `ActiveWorkout`;
- builds debug/release e instalação no Galaxy S22 passaram em diversas rodadas;
- revisões estáticas confirmaram ownership identitário, bootstrap fail-closed e
  proteção contra transporte stale.

## Validações físicas pendentes

Antes de concluir esta subtarefa, ainda devem ser verificados no aparelho:

1. `DISCONNECTED → Tentar novamente → CONNECTING → CONNECTED` com retry explícito;
2. tentativa de scan, medição manual, D1 ou importação enquanto
   `APP_WORKOUT_SESSION` possui o gate;
3. cancelamento ou finalização durante conexão e handshake;
4. perda de lease ou rollover durante conexão, confirmando que transporte novo
   não é desconectado por cleanup antigo;
5. nova sessão iniciada imediatamente após finalizar ou descartar a anterior;
6. handoff integrado com os caminhos de recuperação da `013-08`;
7. screenshots duráveis dos estados sem relógio, conectado, desconectado e
   conflito.

Não há autorização para fabricar estados, relaxar readiness, manipular banco ou
introduzir protocolo BLE não comprovado apenas para cumprir esses gates.

## Estratégia de CI e testes

Este é um aplicativo pessoal. Testes automatizados não são critério central de
aceite desta fase e não bloqueiam a PR.

O workflow Android executa somente:

```bash
./gradlew lint assembleDebug
```

O CI não executa `testDebugUnitTest` e não possui timeout ou otimizações voltadas
à suíte de testes.

Arquivos de teste já existentes no branch podem permanecer como apoio opcional,
mas:

- não fazem parte do gate do CI;
- não precisam ser expandidos para concluir a subtarefa;
- falhas da suíte legada não bloqueiam a entrega;
- a validação funcional principal é realizada no dispositivo físico, com logs e
  inspeção sanitizada;
- correções de testes só devem ocorrer quando ajudarem diretamente a reproduzir
  um problema real do aplicativo.

## Comandos de validação

Gate obrigatório do CI:

```bash
./gradlew lint assembleDebug
```

Validação local opcional antes de instalar no aparelho:

```bash
./gradlew assembleRelease
./gradlew installDebug
```

Testes automatizados não fazem parte dos comandos obrigatórios desta subtarefa.

## Critérios de aceite atuais

- treino funciona sem relógio;
- participação do relógio é explícita e opcional;
- nenhum protocolo não comprovado é enviado automaticamente;
- desconexão não encerra o treino nem apaga estado válido;
- operações BLE concorrentes são recusadas pelo gate compartilhado;
- cleanup só afeta permit, sessão e transporte da própria tentativa;
- incerteza do protocolo produz ausência de métricas, não atribuição especulativa;
- retry é controlado e não entra em loop;
- lint e compilação debug passam no CI;
- gates físicos pendentes são executados ou registrados como risco residual
  aceito antes do fechamento.

## Riscos residuais

- o protocolo pode nunca oferecer contexto suficiente para BPM/passos live;
- corridas raras de cancelamento e rollover ainda não possuem evidência física;
- reconexão controlada ainda precisa de confirmação ponta a ponta;
- o handoff com recuperação da `013-08` ainda participa do aceite integrado;
- screenshots duráveis ainda não cobrem todos os estados obrigatórios.

## Definição de pronto

A subtarefa pode ser concluída quando:

- ownership, conexão, desconexão e cleanup estiverem confirmados no fluxo físico;
- reconexão e conflito forem validados ou receberem waiver explícito;
- ausência de BPM/passos sem protocolo seguro estiver formalmente aceita;
- evidências manuais relevantes estiverem registradas;
- overview for atualizado para `[x]` somente após o aceite real.

## Rollback

Desabilitar a participação BLE durante treino iniciado pelo celular, liberar o
owner da sessão por identidade e preservar a sessão phone, GPS, rota e métricas
já válidas. Nenhuma amostra deve ser apagada como parte do rollback.

## Registro da validação física — 2026-08-05

### Preparação e gate técnico

- Branch confirmada: `feat/013-phone-workouts`.
- Working tree inicial limpo; `git diff --check` passou.
- Galaxy S22 disponível via ADB; serial não foi registrado.
- `./gradlew lint assembleDebug` passou.
- `./gradlew installDebug` passou e instalou o package debug esperado.
- `testDebugUnitTest` não foi executado.

### Resultados observados

- Conexão inicial passou: treino livre iniciou, associação do relógio foi
  explícita, estado conectado foi exibido, o FGS permaneceu ativo e BPM/passos
  permaneceram ausentes.
- Desconexão teve resultado divergente: `Mais → Dispositivo` exibiu
  `Desconectado` e manteve o FGS, mas o retorno a `ActiveWorkout` exibiu
  `Status: Conectado` e não mostrou `Tentar novamente`. Gate marcado como
  `FAIL`, sem alegar reconexão.
- Repetição controlada não alcançou sessão ativa. Após novo countdown, a UI ficou
  em `Solicitação de início emitida`, sem FGS e sem ActiveWorkout. O sintoma é
  compatível com bloqueio conhecido do handoff compartilhado de `013-08`, mas a
  causa não foi isolada nesta execução; `013-08` não foi alterada.
- Finalização e descarte do primeiro cenário liberaram serviço e notificação nos
  dumps sanitizados. Nova sessão não pôde ser aceita porque o handoff repetido
  falhou.
- Não foram executados gates de conflito de owner, cancelamento durante
  `CONNECTING`, rollover/perda de lease ou recuperação pós-`am crash`, pois não
  havia sessão ativa operacional para fazê-los com segurança.
- A inspeção do Logcat capturado não encontrou registros de comandos BLE de saída
  da participação. Payloads, IDs de sessão e identificadores do dispositivo não
  foram preservados.

### Evidências

Evidências sanitizadas estão em
`docs/integration/013-11-evidence/validation-summary.md`, com dumps reduzidos,
notificação sanitizada, Logcat sanitizado e capturas conectada, desconectada e
do estado stale exibido ao retornar a `ActiveWorkout`.
O Logcat bruto permaneceu fora do repositório.

### Decisão

Subtarefa permanece `in_progress`; overview continua em `8/17` e item
`013-11` continua `[ ]`. Não houve correção de produção nesta execução.

### Arquivos alterados nesta execução

- `docs/tasks/013-app-workouts-gps-route/013-11-integrate-watch-live-heart-rate-and-steps-with-phone-session.md`;
- `docs/tasks/013-app-workouts-gps-route/013-00-overview.md`;
- `docs/integration/013-11-evidence/` com capturas e evidências sanitizadas.

Nenhum arquivo de produção, `013-08`, contrato, banco, protocolo BLE ou teste
foi alterado.

### Decisões e desvios

- Não houve correção mínima aplicada: a reconexão apresentou estado stale, mas a
  causa não foi isolada e a repetição ficou bloqueada pelo handoff da sessão.
- Não foram fabricados estados, fakes, hooks, payloads, dados de métricas ou
  operações BLE concorrentes.
- A evidência visual desconectada foi recortada para remover identificador do
  relógio e horários desnecessários.

### Riscos residuais

- Reconexão explícita ponta a ponta não comprovada; estado stale após
  desconexão foi observado uma vez.
- Handoff de `013-08` bloqueia repetição dos gates dependentes.
- Conflito de owner, cleanup durante handshake, rollover/perda de lease e
  recuperação continuam sem evidência física.
- BPM/passos live continuam ausentes até protocolo esportivo seguro comprovado.

### Comandos e resultados

```text
git branch --show-current                         PASS
git status --short                                PASS (inicialmente limpo)
git diff --check                                  PASS
adb devices                                       PASS
./gradlew lint assembleDebug                     PASS
./gradlew installDebug                           PASS
adb logcat -c                                     PASS
adb logcat --pid="$(adb shell pidof -s br.com.mwconsulting.championcompanion.dev)" -v threadtime > /tmp/013-11-logcat.txt  PASS
adb shell dumpsys activity services ...           PASS (dumps coletados)
adb shell dumpsys notification --noredact         PASS (dump coletado e não versionado)
adb exec-out screencap -p                         PASS (capturas sanitizadas)
adb shell uiautomator dump                      PASS (inspeção de estados)
testDebugUnitTest                                 NÃO EXECUTADO
```

## Registro do reteste físico focado — fc43550 — 2026-08-05

### Status

- `Status: in_progress`.
- Overview permanece `[ ]`; progresso permanece `8/17`.
- Nenhuma correção de produção foi aplicada.

### Preparação

- Branch: `feat/013-phone-workouts`.
- HEAD: `fc43550384f745f0b5bb42537e12ffbf786da210`.
- `git status --short`: somente diretório novo de evidências após captura.
- `git diff --check`: PASS.
- `./gradlew lint assembleDebug`: PASS.
- `./gradlew installDebug`: PASS.
- `testDebugUnitTest`: não executado.
- Logcat foi limpo e capturado por PID do package debug.

### Fatos comprovados

- Gate A passou: treino livre ativo, associação explícita, `Status: Conectado`,
  FGS ativo e BPM/passos ausentes.
- Gate B passou: `Mais → Dispositivo` reconheceu o transporte e mostrou
  `Conectado`.
- Bug reproduzido na primeira sequência: `Desconectar` da tela Dispositivo foi
  aceito durante participação do treino. Esperado era recusa por conflito com
  `APP_WORKOUT_SESSION`.
- Após a operação, Dispositivo mostrou `Desconectado`, mas ActiveWorkout voltou a
  mostrar `Status: Conectado`, sem `Tentar novamente`. FGS permaneceu ativo.
- Primeira divergência comprovada: `Device screen=DISCONNECTED` versus
  `ActiveWorkout=CONNECTED`.
- Sessão anterior foi finalizada e descartada. Nova tentativa terminou em
  `Solicitação de início emitida`, sem FGS e sem ActiveWorkout.
- Não houve base operacional para repetir desconexão física, retry ou demais
  gates. Execução física foi encerrada sem atribuir causa.

### Matriz de estado

```text
connectionManager.state = não observável
connectionManager.currentSession() = não observável
owner ForegroundBleOperationGate = não observável
WatchWorkoutParticipationCoordinator.state = não observável diretamente
ExercisesViewModel = não observável; status visual renderizado = CONNECTED
ActiveWorkout = CONNECTED
Device screen = DISCONNECTED
```

Não há evidência para decidir se divergência começa no manager, coordinator,
owner ou ViewModel.

### Evidências

Evidências sanitizadas ficam em
`docs/integration/013-11-evidence/retest-fc43550/`:

- `01-active-workout-connected.png`;
- `02-device-screen-connected.png`;
- `03-device-disconnect-result.png`;
- `04-active-after-disconnect-stale.png`;
- `service-connected.txt`;
- `service-after-device-disconnect.txt`;
- `service-after-real-disconnect.txt`;
- `service-after-reconnect.txt`;
- `notification-after-real-disconnect.txt`;
- `logcat-sanitized.txt`;
- `validation-summary.md`.

Logcat bruto e dumps não sanitizados permaneceram fora do repositório. Nenhum
MAC, serial ADB, UUID de sessão, localização, payload BLE bruto ou identificador
completo do relógio foi preservado nas evidências versionáveis.

### Decisões e desvios

- Não foi aplicada nova correção de produção.
- Não houve alteração em `013-08`, protocolo BLE, banco, GPS, rota, Health
  Connect, persistência ou testes.
- Conforme regra de parada após bug, não foram executados desconexão física,
  retry, reconexão, conflito adicional, rollover ou nova repetição do mesmo
  cenário.
- A segunda tentativa não reproduziu o bug de desconexão porque o handoff não
  criou sessão ativa; foi registrada como bloqueio, não como PASS.

### Riscos residuais

- Desconexão concorrente não bloqueada; estado stale entre telas observado.
- Causa interna ainda não localizada temporalmente.
- Desconexão física, retry explícito, ausência de reconexão automática, conflito
  completo, lifecycle, rollover, recuperação e handoff permanecem sem aceite.
- BPM/passos live continuam ausentes por falta de origem esportiva comprovada.

### Comandos desta execução

```text
git branch --show-current                         PASS
git rev-parse HEAD                                PASS
git status --short                                PASS (inicialmente limpo)
git diff --check                                  PASS
adb devices                                       PASS
./gradlew lint assembleDebug                     PASS
./gradlew installDebug                           PASS
adb logcat -c                                     PASS
adb logcat --pid="$(adb shell pidof -s br.com.mwconsulting.championcompanion.dev)" -v threadtime  PASS
adb shell dumpsys activity services ...           PASS (trechos sanitizados)
adb shell dumpsys notification --noredact         PASS (não versionado)
adb exec-out screencap -p                         PASS (capturas sanitizadas)
adb shell uiautomator dump                       PASS
testDebugUnitTest                                 NÃO EXECUTADO
```

## Encerramento da Fase 013 — cancelada por reorientação de produto

- **Status atual:** `cancelled`.
- **Motivo:** `product_reorientation`; a participação live do relógio em
  sessões iniciadas pelo celular não será continuada nesta direção de produto.
- **Resultado aproveitável:** ownership `APP_WORKOUT_SESSION`, participação
  explícita, cleanup identitário, preservação do treino em desconexão e
  ausência segura de BPM/passos permanecem registrados como infraestrutura e
  evidência potencialmente reutilizáveis. Não há aceitação integral da
  subtarefa.
- **Execução, bloqueios e evidências preservados:** os gates de conexão,
  desconexão, estado stale, handoff, conflito, rollover, recuperação e
  screenshots permanecem exatamente como registrados; o gate de desconexão
  continua `FAIL`/inconclusivo onde indicado. As evidências em
  `docs/integration/013-11-evidence/` não foram apagadas nem reclassificadas.
- **Limites:** BPM/passos live continuam sem origem esportiva comprovada; não se
  alegam reconexão, conflito ou cleanup físico concluídos. Nenhum novo gate foi
  executado para esta decisão.
- **Destino:** preservar contratos, dados e evidências sem expandir BLE live em
  sessão phone. A prioridade passa a ser importar, derivar, revisar e
  sincronizar treinos iniciados pelo relógio.
