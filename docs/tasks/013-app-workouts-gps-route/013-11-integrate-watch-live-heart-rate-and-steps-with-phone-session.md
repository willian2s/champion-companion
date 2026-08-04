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

## Screenshots

Obrigatórias: status sem relógio, conectado, desconectado e conflito; sem BPM identificável se screenshot sair do ambiente privado.

## Definição de pronto

Integração opcional segura concluída ou ausência protocolar explicitamente aceita; overview atualizado.

## Riscos e cuidados

Maior risco: atribuir D0 manual à sessão errada. Falta de protocolo comprovado pode limitar entrega a status/owner e correlação posterior; registrar sem inventar.

## Rollback

Desabilitar participação BLE durante phone workout, liberar gate e preservar sessão/GPS; não apagar amostras já válidas.

Não criar, alterar ou executar testes automatizados.
