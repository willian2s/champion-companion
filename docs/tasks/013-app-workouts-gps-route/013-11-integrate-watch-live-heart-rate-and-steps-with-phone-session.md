# 013-11 — Integrar BPM e passos do relógio à sessão do celular

- **Ticker:** `013`
- **Número:** `11`
- **Status:** in_progress

## Objetivo e resultado esperado

Adicionar participação opcional e conservadora do Champion C001, com owner BLE próprio, amostras vinculadas e desconexão tolerada. Resultado: dados live somente quando origem esportiva for observável com segurança; sessão nunca depende do relógio.

## Contexto

Gate não possui owner app workout. D0/F002 atual é manual; A8/D8 não confirmados; FEA1 não carrega contexto esportivo. Não iniciar modo esporte nem D0 automaticamente.

## Documentos obrigatórios

Spec 013; ADR-008/014; `013-01/03/08`; protocolos D0/F002, D3/D4, transporte, comandos e Champion C001.

## Dependências

Base operacional de `013-08` disponível. Pode iniciar antes do aceite integrado de
`013-08` e avançar em paralelo com `013-09`/`013-10`, com contratos/repositories
congelados. Seu handoff BLE é necessário para o aceite integrado de `013-08`.

## Arquivos permitidos

`core/bluetooth/**`; gate/contracts; coordinator/session service; persistência de `APP_WORKOUT_SESSION` por API existente; feature device/exercises/health para conflitos/status; docs protocol somente com evidência; task/overview.

## Arquivos e símbolos prováveis

`ForegroundBleOperationOwner`, `InMemoryForegroundBleOperationGate`, `ChampionConnectionManager`, `ChampionNotificationHub`, `ChampionManualHeartRateProtocol`, `HeartRateMeasurement`, `HeartRateSource.APP_WORKOUT_SESSION` e coordinator da sessão.

## Arquivos proibidos

A8/D8 ou comando não confirmado; start D0 automático; importar D1/D3/D4 durante sessão; GPS/rota; Health Connect; testes.

## Requisitos cobertos

Relógio opcional; owner; BPM/passos; dedup; desconexão/reconexão; bloqueio de operações concorrentes; orientação ao usuário.

## Escopo

- owner `APP_WORKOUT_SESSION` durante participação BLE;
- status associado/conectado/modo esportivo observável/desconectado;
- ingestão segura de BPM/passos já emitidos pelo relógio no contexto comprovado;
- persistir HR como `APP_WORKOUT_SESSION`, sessão+timestamp;
- preservar amostras e permitir reconexão limitada/controlada;
- mensagens de conflito em Home/Saúde/Exercícios/Dispositivo.

## Fora do escopo

Iniciar esporte do relógio, D0 fallback, BLE permanente, retry infinito, D3/D4 merge e sync HC.

## Passos de implementação

1. Adicionar owner e atualizar `when`/mensagens dos owners existentes.
2. Adquirir gate somente quando sessão usar BLE; liberar em final/interrupção/descarte.
3. Reusar sessão saudável/A1/subscriptions sem comando esportivo novo.
4. Definir condição observável segura para atribuir BPM/passos; se ausente, manter dados ausentes.
5. Persistir BPM incremental com dedup sessão/timestamp; passos por delta seguro quando protocolo permitir.
6. Não apagar dados em disconnect; atualizar status e seguir treino.
7. Implementar ação controlada de reconectar, sem loop.
8. Bloquear D1, D3/D4, manual e refresh conflitantes com copy amigável.
9. Mostrar orientação para iniciar treino também no relógio.
10. Registrar achados físicos no protocolo somente quando observados e atualizar overview.

## Critérios de aceite

- treino funciona sem device;
- nenhum D0/A8/D8 automático é enviado;
- amostras têm owner/session/timestamp e não duplicam;
- disconnect não encerra treino nem apaga amostras;
- operações BLE conflitantes são recusadas;
- incerteza do protocolo produz ausência, não atribuição especulativa.

## Validação manual

Sem relógio; conectado sem modo esporte; modo esporte com BPM quando observado; passos quando disponíveis; disconnect/reconnect; tentativa de manual/D1/D3D4/refresh durante sessão; inspeção de comandos sanitizada.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WatchWorkoutParticipationCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutRecordingService.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/ChampionD1HeartRateSyncCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/ChampionManualHeartRateCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementDebugPoc.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ChampionWatchWorkoutImportCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- este arquivo e `013-00-overview.md`

### Decisões e desvios

- `APP_WORKOUT_SESSION` foi adicionado ao gate BLE. Owner só é adquirido pela
  ação explícita `Associar relógio à sessão`.
- `WatchWorkoutParticipationCoordinator` observa associação, transporte e
  estado terminal; desconexão mantém treino e owner, enquanto finalização ou
  descarte libera o permit.
- Conexão existente não significa participação: UI mantém ação explícita até o
  owner ser adquirido.
- Protocolos D0/F002 não comprovam modo esporte live; FEA1 não traz contexto
  temporal/esportivo. Portanto BPM/passos permanecem ausentes e não há
  persistência especulativa.
- Nenhum D0, A8, D8, D1, D3 ou D4 é enviado por esta integração. Health Connect,
  GPS/rota, schema Room e correção do teardown de `013-08` ficaram fora.

### Matriz manual e evidências

| Cenário | Resultado |
|---|---|
| Sessão sem participação do relógio | Treino iniciou e permaneceu operacional; card informa ausência segura de BPM/passos. |
| Sem dispositivo associado | Galaxy S22 exibiu `Status: Sem relógio associado`; treino livre iniciou, exibiu duração, ausência de GPS e orientação de que treino continua sem relógio. |
| Relógio associado, owner não adquirido | Card exibiu `Status: Associado` e ação `Associar relógio à sessão`. |
| Ação explícita de participação | Card exibiu `Status: Conectado`, modo esportivo não comprovado e BPM/passos ausentes. |
| Foreground service após participação | `dumpsys activity services` confirmou `startForegroundCount=1`; notificação confirmou duas ações. |
| Desconexão/reconexão física | Não executada nesta rodada; estados e retry limitado estão implementados estaticamente. |
| Conflito com outro owner | Não executado fisicamente; gate e copy de conflito estão implementados estaticamente. |
| Comandos proibidos | Inspeção estática confirmou ausência de envio D0/F002/FEA1/A8/D8/D1/D3/D4 no coordinator. |

### Comandos executados

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`;
  103 tarefas, 28 executadas e 75 atualizadas. Permaneceram somente warnings
  preexistentes de cast não verificado em `ExercisesViewModel.kt`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado no Galaxy S22.
- `adb shell uiautomator dump ...`: confirmou estados associado/conectado,
  orientação sem BPM/passos, duração e controles da sessão.
- `adb shell dumpsys activity services ...`: confirmou serviço ativo e
  `startForegroundCount=1` após participação.
- `adb shell dumpsys notification --noredact`: confirmou notificação privada
  com duas ações; nenhum payload BLE foi registrado.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Capturas temporárias: `013-11-associated.png` e `013-11-connected.png`; não
  foram adicionadas ao repositório nesta execução. Captura adicional
  `013-11-no-device.png` registra o cenário sem dispositivo associado.

### Resultado e riscos residuais

- Integração segura de owner/status foi implementada; ausência protocolar de
  BPM/passos foi preservada explicitamente.
- Status `in_progress` permanece correto: validações físicas de desconexão,
  reconexão, conflito e cenário sem dispositivo ainda faltam, assim como
  screenshots duráveis desses cenários.
- `013-08` permanece `in_progress`; seu handoff/teardown não foi alterado.
- A aceitação de métricas live depende de futura evidência protocolar segura;
  não atribuir FEA1, D0 ou F002 à sessão sem essa evidência.

### Correções após revisão independente

- O tipo FGS `connectedDevice` agora é sinalizado somente após `TransportReady`
  do transporte aceito; durante `CONNECTING`, a sessão mantém somente o tipo
  base já permitido.
- Conexões iniciadas pela tela Dispositivo agora adquirem owner transitório
  `DEVICE_CONNECTION` no mesmo gate, com liberação em `finally`, impedindo que
  `ChampionConnectionManager.connect()` derrube participação de treino em
  andamento.
- Callbacks do transporte validam `ChampionSessionId`, dispositivo associado e
  transporte aceito antes de alterar estado; callbacks antigos são ignorados.
- Nova revisão física completa ainda não foi executada após essas correções.
- Gate sem dispositivo associado foi executado após esquecer o Champion C001;
  associação e histórico local foram removidos somente da preferência do
  dispositivo, preservando histórico de atividades. Sessão phone continuou
  iniciando normalmente.

### Revisão independente pós-correção

- Veredito: bloqueado para conclusão.
- Revisão confirmou `connectedDevice` somente após transporte pronto, owner
  `DEVICE_CONNECTION` atômico, callbacks protegidos por sessão e ausência segura
  de métricas/protocolos não comprovados.
- Na revisão independente anterior à nova validação sem dispositivo, permaneciam
  bloqueados os gates físicos de ausência de dispositivo, desconexão, reconexão e
  conflito, além de screenshots duráveis. O arquivo novo
  `WatchWorkoutParticipationCoordinator.kt` permanece não rastreado até a
  entrega do working tree; não houve commit nesta execução.

### Continuação da validação física — 2026-08-04

- **Status:** `in_progress`; item do overview permanece `[ ]`.
- O Champion C001 foi reconectado no Galaxy S22 e a tela de Exercícios confirmou
  `Relógio associado: Champion`.
- Treino livre foi iniciado com countdown sem espera. A preparação confirmou
  ausência de GPS, orientação para iniciar também o treino no relógio e ausência
  segura de BPM/passos.
- Após zero, a tela permaneceu em `Countdown concluído` / `Solicitação de início
  emitida`, sem abrir a sessão ativa. `dumpsys activity services` confirmou o
  `WorkoutRecordingService` como foreground, `startForegroundCount=1` e
  notificação com duas ações.
- O retorno por back não expôs controles da sessão. Force-stop foi usado somente
  para limpar o serviço de validação; a reabertura exibiu `Treino interrompido`,
  duração `01:00`, `Retomar treino`, `Finalizar treino` e `Descartar treino`.
- Desconexão, reconexão controlada e conflito com outro owner continuam sem
  execução física. A tentativa confirma bloqueio no handoff compartilhado de
  `013-08`, não falha de protocolo BLE live; nenhum D0/F002/FEA1/A8/D8/D1/D3/D4
  foi enviado.
- Após esquecer o dispositivo e remover a associação, uma nova sessão de treino
  livre iniciou normalmente. A tela ativa exibiu `Sessão sem GPS`, `Nenhuma
  métrica opcional disponível no momento`, `Status: Sem relógio associado` e
  confirmou que o treino continua funcionando sem relógio.

### Comandos executados na continuação

- `adb devices`: Galaxy S22 disponível.
- `adb shell uiautomator dump ...`: confirmou dispositivo conectado, preparação
  de treino livre, handoff pendente e recuperação após force-stop.
- `adb shell dumpsys activity services br.com.mwconsulting.championcompanion.dev`:
  confirmou `WorkoutRecordingService`, `startForegroundCount=1`, FGS ativo e
  notificação privada com duas ações.
- `adb shell am force-stop br.com.mwconsulting.championcompanion.dev`: limpeza
  controlada do serviço de validação; depois, reabertura confirmou recuperação.
- Fluxo UI de Dispositivo: desconectar, esquecer com confirmação e voltar a
  Exercícios; fluxo UI de treino: iniciar treino livre sem associação e confirmar
  a tela ativa sem GPS/BPM/passos.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Decisão e risco residual da continuação

- As correções estáticas desta rodada permaneceram no escopo de `013-11`; o
  handoff de `013-08` permanece fora do escopo e não foi editado.
- A integração continua limitada a owner/status e ausência segura de métricas.
  Não há evidência física para associar BPM/passos ao treino.
- O aceite permanece bloqueado por handoff compartilhado, desconexão/reconexão,
  conflito e screenshots duráveis.

### Correções mínimas após revisão independente — 2026-08-04

- Causa do primeiro risco confirmada: `DevicePocCoordinator` usava seu
  `ChampionConnectionManager` diretamente e não participava do gate global.
  Scan, conexão, desconexão, reset e comandos explícitos do POC agora exigem ou
  retêm `DEVICE_CONNECTION`; a permissão é liberada de forma idempotente em
  falha, desconexão, reset e descarte da superfície técnica.
- A superfície debug obtém o mesmo `ForegroundBleOperationGate` singleton usado
  pelo app. Operação concorrente é recusada sem chamar o manager; nenhum comando
  BLE novo foi adicionado.
- Causa do segundo risco confirmada: cleanup do serviço podia deixar sessão em
  `AWAITING_RECOVERY` enquanto o coordenador retinha `APP_WORKOUT_SESSION`.
  O coordenador agora libera owner somente após observar recuperação persistida
  ou estado terminal; o serviço chama explicitamente a liberação nos caminhos de
  recuperação antes do cleanup. Desconexão durante sessão continua retendo owner.
- Nenhum documento da subtarefa `013-08` foi editado. Mudanças no serviço
  limitam-se ao lifecycle do owner BLE de `013-11`; contratos, Room, GPS, rota e
  protocolos proibidos permanecem intocados.

### Validação das correções

- `git diff --check`: passou.
- `./gradlew app:lintDebug`: `BUILD SUCCESSFUL`; warning preexistente de cast não
  verificado em `ExercisesViewModel.kt` permaneceu.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Nenhum gate físico novo foi executado; não há evidência fabricada para
  desconexão, reconexão, conflito, handoff integrado ou métricas live.

### Estado após correções

- Status permanece `in_progress`; item do overview permanece `[ ]` e progresso
  `8/17`.
- Desconexão, reconexão controlada, conflito e screenshots duráveis continuam
  bloqueados. O handoff `013-08` continua bloqueado por suas próprias evidências;
  não foi alterado nesta execução.
- Revisão independente deve ser repetida para confirmar os dois riscos estáticos.
  Gate físico posterior continua necessário antes de conclusão.

### Correções finais do POC após revisão independente — 2026-08-04

- O construtor de compatibilidade que criava `InMemoryForegroundBleOperationGate`
  local foi removido. Toda construção de `DevicePocCoordinator` agora recebe o
  gate compartilhado explicitamente.
- Falhas de handshake, subscription, D0, FEA1 e `connectAndBootstrap` passam por
  cleanup terminal serializado e idempotente. Observadores são cancelados, o
  transporte só é desconectado quando a sessão e o permit `DEVICE_CONNECTION`
  pertencem ao POC, e o permit é liberado por identidade sem tocar
  `APP_WORKOUT_SESSION`.
- Nenhum protocolo novo foi adicionado. BPM/passos permanecem ausentes sem
  contexto esportivo comprovado.

### Validação final das correções do POC

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Gates físicos de handoff associado, desconexão, reconexão, conflito e
  screenshots duráveis continuam pendentes; nenhuma evidência foi fabricada.

### Blindagem final de lifecycle — 2026-08-04

- Lease do POC passou a carregar ciclo, sessão e permit. Cleanup terminal roda em
  `NonCancellable`, separa detach de observers da liberação do gate e ignora
  callbacks sem correspondência ao lease atual.
- `Disconnected`/`Failed` agora validam sessão, ciclo e transporte atual antes de
  limpar estado ou liberar `DEVICE_CONNECTION`. `startHeartRateSession` executa
  cleanup identitário em cancelamento e relança a `CancellationException`.
- `connectAndBootstrap` compensa conexão sem lease e limpa cancelamento somente
  da sessão esperada. `APP_WORKOUT_SESSION` não é tocado.
- O único ajuste adicional foi a passagem explícita de `dateTime` ao handshake;
  nenhum protocolo BLE novo foi introduzido.

### Validação da blindagem final

- Primeira tentativa do build após a blindagem falhou porque a chamada interna de
  `executeHandshake` omitia o argumento obrigatório `dateTime`; correção aplicada
  com `ZonedDateTime.now()` antes da validação final.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: não concluído após esta última alteração porque
  `adb devices` estava sem dispositivo conectado. Instalação anterior, antes da
  blindagem final, havia passado no Galaxy S22.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Handoff associado, desconexão, reconexão, conflito e screenshots duráveis
  continuam bloqueados; não há evidência física nova.

### Correção P1 de compensação após conexão — 2026-08-04

- Causa confirmada: `connect()` podia receber `Success` de
  `connectionManager.connect()` depois de perder lease ou ciclo; o `bind` nulo
  retornava falha, mas deixava o `ChampionTransportSession` recém-criado ativo.
  `connectAndBootstrap` e seu cleanup existente foram preservados.
- O caminho nulo agora executa compensação em `NonCancellable`. A compensação
  adquire somente owner transitório `DEVICE_CONNECTION`, revalida ciclo, estado,
  lease local e identidade/sessão do transporte sob `terminalCleanupMutex` e
  `permitMutex`, e desconecta somente o transporte retornado ainda ativo.
- Owner concorrente, inclusive `APP_WORKOUT_SESSION`, rejeita compensação. O
  lease temporário e o lease original só são liberados por suas próprias
  identidades; nenhum contrato, protocolo, GPS, rota ou persistência foi tocado.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado no Galaxy S22.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Status permanece `in_progress`; item do overview permanece `[ ]` e progresso
  `8/17`. Validação física específica da corrida entre perda de lease, ciclo e
  conexão continua pendente, assim como desconexão, reconexão, conflito e
  screenshots duráveis.

### Correção P1 final da compensação — 2026-08-04

- A revisão final confirmou que a compensação ainda podia desconectar o
  transporte esperado após rollover quando `activeSessionId` já era nulo: a
  validação anterior só comparava o ciclo quando havia sessão ativa.
- `disconnectExpectedTransport` agora exige simultaneamente ciclo esperado,
  sessão ativa esperada e estado PoC permitido de pré-bootstrap
  (`Connecting`/`TransportReady`) antes de desconectar. As verificações de
  referência do transporte, `ChampionSessionId` e lease local foram mantidas.
- O permit transitório continua sendo adquirido com owner
  `DEVICE_CONNECTION`; rejeição de `APP_WORKOUT_SESSION`, liberação identitária
  e cleanup existente permanecem preservados. Nenhum contrato, protocolo,
  persistência, GPS, rota, teste ou arquivo de `013-08` foi alterado.

### Validação da correção P1 final

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL` (103
  tarefas acionáveis; 3 executadas e 100 atualizadas).
- `adb devices`: nenhum dispositivo conectado; `./gradlew installDebug` não foi
  executado nesta rodada.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Status permanece `in_progress`; item do overview permanece `[ ]` e progresso
  `8/17`. A corrida física de rollover/perda de lease, desconexão, reconexão,
  conflito e screenshots duráveis continua bloqueada.

### Correções P1 adicionais após revisão — 2026-08-04

- Causa do primeiro P1 confirmada: `DeviceManagementViewModel.startScan()` ainda
  chamava `connectionManager.scan()` diretamente, apesar de conexão, POC e
  operações de sync já usarem a abstração do gate. O scan agora adquire owner
  transitório `DEVICE_CONNECTION` antes da chamada, recusa conflito sem chamar o
  manager, preserva estados/eventos existentes e libera o permit em `finally`
  `NonCancellable`.
- Causa do segundo P1 confirmada: cancelamento de
  `connectAssociatedDevice()` podia deixar `enabledSessionId`, `CONNECTING` e o
  permit `APP_WORKOUT_SESSION` presos. `enable()` agora identifica o permit da
  tentativa e o transporte esperado; cleanup roda em `NonCancellable`, valida
  sessão e permit ainda pertencentes à tentativa, restaura `DISCONNECTED`,
  desconecta somente transporte com mesma identidade/chave/estado e libera
  somente permit adquirido por aquela tentativa. Desconexão normal continua
  mantendo owner conforme requisito.
- Cleanup não altera owner de outra sessão, não adiciona D0/F002/FEA1/A8/D8/D1/
  D3/D4, GPS, rota, Room ou Health Connect. Nenhum arquivo de `013-08` foi
  alterado.

### Validação das correções P1 adicionais

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`;
  103 tarefas acionáveis.
- `adb devices`: Galaxy S22 disponível; `./gradlew installDebug`:
  `BUILD SUCCESSFUL`, instalado em um dispositivo.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Não houve gate físico específico para corrida de cancelamento, conflito de
  scan, perda de lease, desconexão/reconexão ou transporte substituído. Esses
  riscos permanecem bloqueadores físicos, junto com screenshots duráveis,
  handoff compartilhado de `013-08` e evidência protocolar de BPM/passos live.
- Status permanece `in_progress`; item do overview permanece `[ ]`, progresso
  `8/17`.

### Correções P1/P2 adicionais após revisão — 2026-08-04

- Causa do P1 confirmada: após `connectAssociatedDevice()` criar o transporte,
  uma falha de handshake caía apenas em `setDisconnectedLocked()`. O transporte
  inválido permanecia ativo e uma nova ação podia reutilizar `currentSession()`
  sem novo handshake.
- `enable()` agora encaminha falha não cancelável e exceção externa para cleanup
  identitário em `NonCancellable`. O cleanup exige simultaneamente sessão do
  treino, owner/operationId e referência do permit da tentativa; valida
  `transportKey`, `ChampionSessionId`, estado publicado e referência do
  `ChampionTransportSession` quando disponível. Desconecta somente transporte
  que satisfaz toda a identidade esperada, libera o owner da tentativa quando o
  transporte deixou de ser válido e bloqueia reuso do transporte marcado como
  inválido até novo bootstrap.
- `ChampionBleState.Disconnected` agora restaura `DISCONNECTED`, limpa
  `acceptedTransportSessionId`, `pendingTransportSessionId` e o tipo foreground,
  mas mantém `enabledSessionId` e `APP_WORKOUT_SESSION` para reconexão explícita.
  Cleanup não altera permit ou transporte de outra sessão.
- Nenhum protocolo D0/F002/FEA1/A8/D8/D1/D3/D4, contrato, Room, GPS, rota,
  teste ou arquivo da subtarefa `013-08` foi alterado.

### Validação desta correção

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas acionáveis, 29 executadas e 74 atualizadas.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Corridas físicas de falha não cancelável, reuso após transporte inválido,
  desconexão normal/reconexão, conflito e screenshots duráveis continuam sem
  evidência; o status permanece `in_progress`, item `[ ]`, progresso `8/17`.

### Revisão independente final — 2026-08-04

- Veredito: implementação aprovada estaticamente com ressalvas; fechamento
  bloqueado por ausência de gates físicos.
- A revisão confirmou cleanup `NonCancellable` para falha não cancelável,
  identidade de sessão/permit/`operationId`/`transportKey`/`ChampionSessionId`,
  bloqueio de reuso por `invalidTransportSessionId` e retenção correta do owner
  durante desconexão normal.
- Scan com `DEVICE_CONNECTION`, lifecycle do POC, conflito com
  `APP_WORKOUT_SESSION` e ausência de protocolos proibidos não têm bloqueador
  estático.
- Permanecem pendentes falha não cancelável/reuso, desconexão/reconexão,
  conflito, rollover/perda de lease, handoff integrado de `013-08`, screenshots
  duráveis e evidência física de BPM/passos live.
- Status permanece `in_progress`; item `[ ]`; progresso `8/17`. Nenhum teste foi
  criado ou executado; nenhuma evidência foi fabricada.

### Validação física adicional — 2026-08-04

- Galaxy S22 reconheceu Champion C001 associado e conectado. Sessão de treino
  livre iniciou após countdown e a tela ativa confirmou `Sessão sem GPS`,
  `Nenhuma métrica opcional disponível no momento` e `Status: Associado` antes da
  participação explícita.
- A ação explícita `Associar relógio à sessão` foi executada. A tela confirmou
  `Status: Conectado`, manteve BPM/passos ausentes e informou que o modo esportivo
  continua não comprovado. `dumpsys activity services` confirmou
  `WorkoutRecordingService` foreground, `startForegroundCount=1`, notificação
  privada com duas ações e tipos FGS `0x00000110`.
- Com sessão ativa, a tela Dispositivo executou `Desconectar`. A UI confirmou
  `Dispositivo desconectado` e `Desconectado`; o FGS permaneceu ativo com
  `startForegroundCount=1`. Gate físico de desconexão aprovado.
- Reconexão não foi concluída: ao retornar à tela Dispositivo, `Tentar novamente`
  não expôs candidato selecionável nem reabriu a superfície da sessão ativa para
  nova ação `Associar relógio à sessão`. Não há evidência suficiente para marcar
  reconexão ou conflito.
- Force-stop e descarte foram usados somente para limpar a sessão de validação;
  `dumpsys activity services` terminou sem serviço ativo.
- Comandos: `adb devices`, `adb shell uiautomator dump ...`,
  `adb shell dumpsys activity services ...`, `adb shell am force-stop ...`.
  Nenhum teste automatizado foi criado, alterado ou executado.
- Riscos restantes: reconexão, conflito, rollover/perda de lease, falha não
  cancelável/reuso, screenshots duráveis, handoff `013-08` e BPM/passos live.

### Correção de reconexão — 2026-08-04

- **Status:** `in_progress`; item do overview permanece `[ ]`; progresso da fase
  permanece `8/17`.
- Causa confirmada: `ChampionNavigation.removeExercisesDestinations()` removia
  `AppDestination.ActiveWorkout` ao abrir `Device`, e a tela de Exercícios era
  recriada como landing. A navegação agora guarda somente o `sessionId` da
  superfície `ActiveWorkout` quando a observação persistida correspondente está
  `ACTIVE` ou `PAUSED`, remove a superfície apenas da pilha visível e a restaura
  ao voltar a Exercícios. A validação ocorre na saída e no retorno; sessões
  `FINISHING`, `AWAITING_RECOVERY`, `FINISHED` e `DISCARDED` não são reabertas.
- O retry de relógio não foi duplicado na feature Dispositivo: a ação continua
  em `WorkoutShells` e percorre `ExercisesViewModel.retryWatchParticipation()` →
  `WorkoutRecordingService.ACTION_RETRY_WATCH` →
  `WatchWorkoutParticipationCoordinator.enable()`. Nenhum coordinator foi
  chamado diretamente pela `DeviceScreen`, nenhum gate foi contornado e
  `APP_WORKOUT_SESSION` continua retido durante desconexão.
- Arquivos alterados nesta correção: `core/navigation/ChampionNavigation.kt`,
  `feature/exercises/ExercisesViewModel.kt`, esta task e
  `013-00-overview.md`. Não houve alteração em Device VM/Screen, contratos,
  Room, GPS, Health Connect, handoff/startForeground de `013-08` ou protocolos
  BLE; BPM/passos continuam ausentes sem evidência segura.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`;
  warning preexistente de cast não verificado em `ExercisesViewModel.kt` foi
  mantido.
- `adb devices`: Galaxy S22 disponível.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22.
- Nenhum teste automatizado foi criado, alterado ou executado. Não foi executado
  gate físico novo de reconexão nesta rodada; portanto, a correção estática está
  validada, mas o risco físico de retorno, retry, reconexão real e preservação do
  FGS permanece aberto. Também continuam bloqueados conflito, rollover/perda de
  lease, handoff de `013-08`, screenshots duráveis e BPM/passos live.

### Validação física pós-correção de navegação — 2026-08-04

- APK corrigido foi instalado no Galaxy S22. Sessão livre iniciou, Champion C001
  foi associado explicitamente e mostrou `Status: Conectado`.
- Após abrir `Mais → Dispositivo`, desconectar e voltar por `Exercícios`, a
  navegação restaurou `Treino ativo`/`ActiveWorkout` na mesma sessão, em vez de
  recriar landing. FGS permaneceu ativo durante a sessão.
- Em uma segunda repetição, `DeviceScreen` mostrou estado desconectado com ação
  `Conectar` disponível; ao retornar a `ActiveWorkout`, a UI mostrou
  `Status: Conectado`. Isso comprova restauração da superfície, mas não comprova
  retry explícito nem reconexão controlada; o estado visual precisa ser tratado
  como evidência inconclusiva até novo gate.
- Após validação, force-stop e descarte limparam a sessão; `dumpsys activity
  services` terminou sem serviço ativo.
- Comandos executados: `git diff --check`,
  `./gradlew lint assembleDebug assembleRelease`, `./gradlew installDebug`,
  `adb devices`, `adb shell uiautomator dump ...`,
  `adb shell dumpsys activity services ...` e `adb shell am force-stop ...`.
- Nenhum teste automatizado foi criado, alterado ou executado. Item permanece
  `in_progress`, `[ ]`, progresso `8/17`; retry explícito, conflito, rollover,
  handoff `013-08`, screenshots e BPM/passos live continuam pendentes.

### Correção de estado stale e retry explícito — 2026-08-04

- Causa confirmada: `WatchWorkoutParticipationCoordinator` mantinha estado local
  `CONNECTED` após a tela Dispositivo publicar desconexão; ao restaurar
  `ActiveWorkout`, a UI não expunha `Tentar novamente`.
- Coordinator agora reconcilia estado contra `ChampionConnectionManager`, sessão
  de transporte atual, dispositivo associado, IDs de transporte aceito/pendente,
  sessão habilitada e permit `APP_WORKOUT_SESSION` durante bind e retry.
- `ACTION_RETRY_WATCH` agora chama API `retry(sessionId)` distinta de `enable`;
  retry preserva owner existente, não adquire `DEVICE_CONNECTION`, executa uma
  única tentativa e publica `CONNECTING`, `CONNECTED` ou `DISCONNECTED`.
- Nenhum acesso direto da tela Dispositivo ao coordinator foi introduzido; nenhum
  protocolo BLE, BPM/passos, GPS, Room ou Health Connect foi alterado.
- Arquivos de produção alterados nesta correção: `WatchWorkoutParticipationCoordinator.kt`
  e `WorkoutRecordingService.kt`. Arquivos de navegação da correção anterior
  permanecem no working tree.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: falhou nesta execução porque `adb devices` estava sem
  dispositivo conectado. Nenhum teste automatizado foi criado, alterado ou
  executado.
- Validação física `DISCONNECTED → Tentar novamente → CONNECTING → CONNECTED`
  permanece pendente; item continua `in_progress`, `[ ]`, progresso `8/17`.

### Correção de cleanup do retry — 2026-08-04

- Falha estática corrigida: cleanup de tentativa agora preserva token com sessão,
  permit, `operationId`, chave e sessão de transporte esperados, independente de
  o owner local já ter sido liberado.
- Falha de handshake não pode deixar `TransportReady` inválido ativo para próximo
  retry. Cleanup `NonCancellable` desconecta somente transporte identificado da
  própria tentativa, limpa token/IDs de estado de forma owner-aware e não toca
  owner concorrente.
- Retry/rebind/enable rejeitam sessão terminal. Naquele ponto, retry sem owner
  ainda não delegava para `enable()`; correção posterior passou a delegar fora
  de `operationMutex`, sem adotar transporte alheio.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `installDebug`: não executado nesta correção porque `adb devices` estava sem
  dispositivo conectado. Nenhum teste automatizado foi criado, alterado ou
  executado.
- Gate físico `DISCONNECTED → Tentar novamente → CONNECTING → CONNECTED`, conflito,
  rollover, handoff `013-08`, screenshots e BPM/passos live permanecem pendentes.

### Correção dos bloqueadores estáticos residuais — 2026-08-04

- Causa do primeiro bloqueador confirmada: `captureAttemptTransport` só aceitava
  transporte retornado ou sessão já conhecida; uma falha de
  `DeviceConnectionBootstrap` podia deixar `TransportReady` ativo sem token e
  `sessionId` completos, permitindo adoção stale no próximo `reconcile`.
- A captura agora lê `TransportReady` junto com a referência atual do manager,
  chave, `ChampionSessionId` e owner/permit `APP_WORKOUT_SESSION` da tentativa.
  Identidade incompleta recebe `transportIdentityInvalid` e
  `invalidTransportSessionId`; `reconcileBoundStateLocked` não adota essa
  sessão. Cleanup exige referência, chave, sessão e estado correspondentes,
  desconecta somente transporte esperado e invalida o estado stale restante.
- Causa do segundo bloqueador confirmada: a leitura anterior de
  `canBindSession()` ocorria antes de adquirir `operationMutex` e podia ficar
  stale antes de bind, aquisição ou bootstrap. A revalidação agora ocorre dentro
  do lock em `bind`, `enable` e `retry`, e novamente imediatamente antes de
  iniciar conexão. `FINISHING`, `AWAITING_RECOVERY`, `FINISHED` e `DISCARDED` não
  reabrem nem entram em retry.
- `APP_WORKOUT_SESSION`, `DEVICE_CONNECTION`, reconciliação, retry separado e
  ausência de D0/F002/FEA1/A8/D8/D1/D3/D4 foram preservados. Nenhum arquivo de
  `013-08`, teste ou contrato foi alterado.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `adb devices`: nenhum dispositivo conectado; `installDebug` não foi executado
  nesta rodada.
- Nenhum teste automatizado foi criado, alterado ou executado. Item permanece
  `in_progress`, `[ ]`, progresso `8/17`. Handoff `013-08`, desconexão/reconexão,
  conflito, rollover/perda de lease, screenshots e BPM/passos live continuam
  bloqueados.

### Correção P1 final da corrida durante bootstrap — 2026-08-04

- A causa foi confirmada em `reconcileBoundStateLocked`: `TransportReady`
  pendente podia promover `CONNECTED` e limpar `connectionAttemptToken` enquanto
  `connectAssociatedDevice()` ainda executava o handshake A1. Se A1 falhasse,
  `captureAttemptTransport` e o cleanup perdiam a identidade da tentativa, com
  risco de manter transporte ativo e `APP_WORKOUT_SESSION` retido.
- Durante tentativa com token, `TransportReady` pendente agora mantém
  `CONNECTING`, `pendingTransportSessionId` e o token. A promoção só ocorre após
  `captureAttemptTransport` validar referência atual, chave, `ChampionSessionId`,
  permit/owner e após `connectAssociatedDevice()` retornar `Success` para a mesma
  referência e identidade. O token só é limpo nessa promoção validada.
- Falha ou cancelamento captura estado, sessão e referência atuais enquanto o
  token ainda está rastreado. Cleanup invalida a identidade, desconecta somente
  o transporte da própria tentativa, limpa token/estado e libera somente o owner
  esperado. `APP_WORKOUT_SESSION`, `DEVICE_CONNECTION`, retry separado,
  `canBind` terminal e ausência de protocolos proibidos foram preservados.
- Nenhum arquivo de `013-08`, teste, contrato, Room, GPS, rota ou protocolo
  proibido foi alterado.

### Validação da correção P1 final

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `adb devices`: nenhum dispositivo conectado; `./gradlew installDebug` não foi
  executado nesta rodada.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Status permanece `in_progress`; item do overview permanece `[ ]`, progresso
  `8/17`. Permanecem bloqueados gates físicos de falha/cancelamento e rollover,
  desconexão, reconexão, conflito, handoff de `013-08`, screenshots duráveis e
  evidência protocolar de BPM/passos live.

### Correção do bloqueador final de promoção após bootstrap — 2026-08-04

- Causa confirmada: o bloco final de `attemptConnection()` validava `Success`,
  identidade do transporte, `ChampionSessionId`, permit e `TransportReady`, mas
  não relia o estado persistido da sessão imediatamente antes de promover
  `CONNECTED`. Uma sessão que avançasse para `FINISHING`,
  `AWAITING_RECOVERY`, `FINISHED` ou `DISCARDED` ainda podia ser promovida.
- A promoção agora revalida `canBindSessionLocked(sessionId)` dentro de
  `operationMutex`, depois da identidade do bootstrap estar validada e antes de
  alterar os IDs aceitos, limpar `connectionAttemptToken` ou publicar
  `CONNECTED`. Rejeição segue o cleanup identitário existente: token permanece
  rastreado até essa decisão, somente o transporte da tentativa pode ser
  desconectado e o owner é liberado conforme a política de `enable`/`retry`.
- `APP_WORKOUT_SESSION`, retry separado, `DEVICE_CONNECTION`, cleanup
  `NonCancellable` e protocolos D0/F002/FEA1/A8/D8/D1/D3/D4 foram preservados.
  Nenhum arquivo de `013-08`, contrato ou teste foi alterado.

### Validação do bloqueador final

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103
  tarefas acionáveis, 5 executadas e 98 atualizadas.
- `adb devices`: Galaxy S22 conectado (`SM-S901E`, Android 16).
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado em 1 dispositivo.
- Nenhum teste automatizado foi criado, alterado ou executado.
- Não foi executada corrida física durante bootstrap nem novo gate de
  desconexão, reconexão, conflito, rollover/perda de lease, handoff de `013-08`,
  screenshots ou BPM/passos live; não há evidência fabricada.
- Status permanece `in_progress`; item do overview permanece `[ ]`; progresso
  `8/17`. Esses gates e a evidência protocolar live continuam bloqueados.

### Correção do transporte stale após desconexão — 2026-08-04

- **Causa confirmada no gate físico:** `ChampionConnectionManager.disconnect()`
  publicava `ChampionBleState.Disconnected`, mas `cleanupSession()` não recebia
  a referência do transporte encerrado. A limpeza por `sessionId` não
  distinguia transporte antigo de tentativa concorrente, e
  `currentSession()` podia continuar expondo uma referência stale para o
  coordinator durante a reconciliação.
- `disconnect()` e `handleClientEvent()` agora passam o
  `ChampionTransportSession` esperado a `cleanupSession()`. `activeTransport` só
  é limpo quando referência (`===`) e `sessionId` ainda correspondem ao
  transporte encerrado; tentativa concorrente não é apagada. Caminhos de falha
  anteriores à criação do wrapper de transporte não limpam uma sessão que não
  lhes pertence.
- Owners `APP_WORKOUT_SESSION` e `DEVICE_CONNECTION`, lifecycle, retry separado,
  adoção fail-closed e protocolos BLE foram preservados. Nenhum arquivo de
  `013-08`, contrato, persistência, GPS, rota, protocolo proibido ou teste foi
  alterado. Arquivo de produção alterado: `core/bluetooth/ChampionConnectionManager.kt`.

### Validação da correção

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22.
- Nenhum teste automatizado foi criado, alterado ou executado.
- A correção estática cobre publicação de `Disconnected`/falha de transporte e
  impede reuso/adoção de transporte stale sem alterar coordinator ou owners.
  O gate físico `DISCONNECTED → Tentar novamente → CONNECTING → CONNECTED` ainda
  não foi repetido após esta correção; conflito, rollover/perda de lease,
  handoff de `013-08`, screenshots duráveis e BPM/passos live continuam
  bloqueados.
- Status permanece `in_progress`; item do overview permanece `[ ]`; progresso
  `8/17`.

### Correção dos dois P1 residuais de concorrência — 2026-08-04

- Causa do P1 de scan confirmada: `disconnectCurrent()` cancelava o scanner antes
  de revalidar sua geração. Scan e cancelamento agora usam `scannerMutex`
  dedicado durante toda a operação do scanner; o mutex principal não permanece
  adquirido durante I/O BLE. A seleção do transporte continua revalidando
  geração e referência esperada após o cancelamento serializado.
- Causa do P1 de cancelamento confirmada: `connect()` limpava recursos em
  `NonCancellable`, mas podia manter estado intermediário sem transporte ou
  pending. Somente no caminho de `CancellationException`, helper sob mutex
  publica `Disconnected` quando geração ainda coincide e `activeTransport` e
  `pendingConnection` continuam nulos.
- Cleanup de pending permanece identitário por referência (`===`). Geração nova,
  transporte novo ou pending novo impedem publicação terminal stale.
- Arquivo de produção alterado nesta correção:
  `core/bluetooth/ChampionConnectionManager.kt`. Coordinator, contratos, Room,
  GPS, rota, testes, Gradle, manifesto e protocolos não foram alterados.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; 103 tarefas
  acionáveis, 28 executadas e 75 atualizadas.
- Nenhum teste automatizado foi criado, alterado ou executado. Item permanece
  `in_progress`, `[ ]`, progresso `8/17`; gates físicos de reconexão, conflito,
  rollover/perda de lease, handoff de `013-08`, screenshots e evidência
  protocolar de BPM/passos live continuam bloqueados.

### Correções de concorrência após revisão independente — 2026-08-04

- A revisão encontrou quatro riscos estáticos: transporte stale podia publicar
  estado terminal, tentativa encerrada podia reter token, retry em conflito ou
  sem owner era recusado e `TransportReady` podia ser adotado sem bootstrap
  comprovado. A implementação corrigiu esses caminhos sem adicionar protocolo
  BLE, GPS, rota, Room ou Health Connect.
- `ChampionConnectionManager` agora protege publicações por geração e
  referência de transporte. `disconnectExpectedTransport()` só encerra a
  referência esperada; callback antigo não sobrescreve transporte novo.
- Tentativas carregam `Job` e tombstone identitário. Estado terminal cancela e
  aguarda a tentativa antes de liberar `APP_WORKOUT_SESSION`; cleanup tardio não
  toca owner, transporte ou sessão de outra tentativa.
- Retry com owner retido continua uma tentativa única e não adquire
  `DEVICE_CONNECTION`. Retry sem owner delega para `enable()` fora do lock,
  permitindo repetir conflito ou falha inicial pela ação explícita da UI.
- Adoção genérica de `TransportReady` foi removida. `CONNECTED` exige sucesso do
  bootstrap A1 e identidade de transporte/owner revalidada; ausência de prova
  mantém estado desconectado e não promove `connectedDevice`.
- `git diff --check`: passou; `./gradlew lint assembleDebug assembleRelease`:
  `BUILD SUCCESSFUL`; `./gradlew installDebug`: `BUILD SUCCESSFUL`, instalado
  no Galaxy S22. Nenhum teste automatizado foi criado, alterado ou executado.
- Status permanece `in_progress`; item do overview permanece `[ ]`; progresso
  `8/17`. Retry/reconexão, conflito, rollover/perda de lease, handoff de
  `013-08`, screenshots duráveis e BPM/passos live seguem sem aceite físico.

## Screenshots

Obrigatórias: status sem relógio, conectado, desconectado e conflito; sem BPM identificável se screenshot sair do ambiente privado.

## Definição de pronto

Integração opcional segura concluída ou ausência protocolar explicitamente aceita; overview atualizado.

## Riscos e cuidados

Maior risco: atribuir D0 manual à sessão errada. Falta de protocolo comprovado pode limitar entrega a status/owner e correlação posterior; registrar sem inventar.

## Rollback

Desabilitar participação BLE durante phone workout, liberar gate e preservar sessão/GPS; não apagar amostras já válidas.

Não criar, alterar ou executar testes automatizados.
