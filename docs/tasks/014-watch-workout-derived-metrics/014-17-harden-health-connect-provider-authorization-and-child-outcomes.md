# 014-17 — Endurecer autorização e resultado por filho do Health Connect

- **Ticker:** `014`
- **Número:** `17`
- **Status:** completed

## Origem e bloqueio

A validação física de `014-15` no Galaxy S22 `SM-S901E`, Android 16, marcou o
Gate F como `FAIL`: o Health Connect real não exibiu distância ou velocidade, e
`WRITE_SPEED` apareceu como não permitida. O aplicativo informou sucesso local,
mas a integração é write-only e não faz leitura posterior para confirmar records
no provider.

A análise causal do fan-out local isolou também o bloqueador funcional:
`NO_GLOBAL_THRESHOLD_BLOCKS_PROJECTION` era a política padrão do snapshot. Como
`isEligibleForHealthProjection` exigia `EXPLICITLY_ACCEPTED`, DISTANCE/SPEED não
eram criados. Isso é bloqueio fail-closed de plausibilidade, não autorização
implícita nem limiar global ausente a ser inventado.

O follow-up funcional foi aberto em
`docs/integration/014-15-health-connect-provider-follow-up.md`. A subtarefa
`014-15` permanece `blocked` e não pode ser marcada por esta normalização.

## Objetivo e resultado esperado

Corrigir, em follow-up funcional separado, as lacunas de autorização, resultado
por filho e feedback local do Health Connect para projeções `WATCH_D3_D4`, sem
adicionar leitura remota. O resultado esperado é uma cadeia explícita e
diagnosticável:

```text
permissão real do provider
        ↓
manifesto local de filhos
        ↓
resultado independente de DISTANCE/SPEED
        ↓
feedback local sem alegar confirmação remota
        ↓
revalidação física do Gate F em 014-15
```

## Dependências

- `014-01` a `014-14` concluídas.
- Execução inicial de `014-15` concluída somente como evidência bloqueada.
- `014-15` deve ser revalidada após esta subtarefa; `014-16` depende dessa
  revalidação sem Gate F bloqueador.
- ADR-012, ADR-018 e o follow-up de provider de `014-15` permanecem
  autoritativos.

## Escopo incluído

1. Confirmar estado real de `WRITE_DISTANCE` e `WRITE_SPEED` no provider e
   diferenciar permissão ausente de provider indisponível ou escrita rejeitada.
2. Garantir que cada filho esperado tenha resultado local explícito e que filho
   ausente ou falho não desapareça da decisão de conclusão da atividade.
3. Expor falhas por capacidade (`DISTANCE` ou `SPEED`) em sync explícito,
   preservando falha parcial e retry granular.
4. Atualizar Settings e Exercícios para diferenciar permissão pendente, falha
   parcial, escrita aceita sem leitura posterior e provider indisponível.
5. Atualizar o fluxo de retorno de Settings/provider para refletir permissões
   concedidas atuais, inclusive no package debug final.
6. Revalidar fisicamente distância, velocidade, midpoint, retry e falha parcial
   no provider real.

## Escopo excluído

- `readRecords`, `ChangeLogs` ou leitura ampla do Health Connect;
- usar provider como fonte visual primária ou como fallback local;
- alteração da distância canônica, do midpoint ou do mapper sem causa isolada;
- nova migration, schema Room, exportação ou alteração de backup;
- nova permissão de rota, GPS, `ExerciseRoute`, treino phone ou BLE live;
- reprocessamento automático de atividade já `SYNCED`;
- correção funcional dentro de `014-15`;
- testes automatizados criados ou executados sem autorização específica.

## Contratos mínimos

1. Declaração de permissão no Manifest não equivale a permissão concedida pelo
   provider.
2. `SYNCED` de operação significa que a escrita local foi aceita pelo caminho
   operacional; não significa confirmação por leitura remota.
3. `HealthSyncSummary` e fila local devem ser apresentados como operações, não
   como quantidade de atividades ou records visíveis no provider.
4. Atividade não pode ser considerada concluída quando filho esperado estiver
   ausente, não processado ou em `FAILED`.
5. `DISTANCE` continua usando somente intervalos canônicos positivos; `SPEED`
   continua usando amostras no midpoint dos intervalos.
6. Permissão ausente deve manter operação recuperável e permitir retry após
   autorização, com o mesmo ID, versão e payload congelados.
7. Série sem indicadores exige aceite explícito antes de preparar projeção;
   `EXPLICITLY_ACCEPTED` é persistido no snapshot existente, sem migration/schema.
8. Indicadores `IMPLAUSIBLE_*` impedem aceite genérico; `SYNCED` permanece
   bloqueado.

## Arquivos e símbolos prováveis

- `core/healthconnect/HealthConnectGateway.kt` — permissões concedidas e
  classificação do efeito de escrita;
- `core/healthconnect/HealthConnectSyncCoordinator.kt` — manifesto, resultado
  por filho, falha parcial e transição de workflow;
- `core/healthconnect/HealthRecordMapper.kt` — auditoria do payload de
  `DistanceRecord` e `SpeedRecord`; alteração somente com evidência causal;
- `feature/settings/SettingsViewModel.kt`, `SettingsScreen.kt` e
  `SettingsFeatureContract.kt` — refresh e estado de autorização;
- `feature/exercises/ExercisesViewModel.kt`, `ExercisesScreen.kt` e
  `ExercisesFeatureContract.kt` — feedback por capacidade e escrita local;
- `core/model/HeartRateAndSync.kt` e `RepositoryContracts.kt` — somente se
  relatório granular exigir contrato aditivo sem migration;
- `data/RoomRepositories.kt` e `core/database/ChampionDaos.kt` — somente se
   leitura local por filho não puder reutilizar contratos existentes.
- `core/model/WatchWorkoutDerivedMetrics.kt`, `WatchWorkoutActivity.kt` e
  `RepositoryContracts.kt` — política de aceite e transições explícitas.

Manifest, Gradle, migrations e schemas devem permanecer sem alteração salvo
evidência independente de declaração incorreta de permissão.

## Ordem de execução

1. Reproduzir o cenário no mesmo provider, package debug e versão de aplicativo;
   registrar permissões antes e depois da solicitação.
2. Inspecionar manifesto local, filhos esperados, estados e erros por operação;
   separar filho não criado de filho falho e de provider sem exibição.
3. Ajustar contratos e coordinator somente para preservar resultado explícito,
   autorização atual e conclusão por conjunto completo de filhos.
4. Ajustar Settings e Exercícios sem transformar sucesso de escrita em
   confirmação de leitura.
5. Validar, no provider real, cenário com ambas as permissões, revogação isolada
   de `WRITE_SPEED`, revogação isolada de `WRITE_DISTANCE` e retry após concessão.
6. Reexecutar o Gate F de `014-15`; somente depois avaliar desbloqueio de
   `014-16`.

### Estado da implementação

O aceite foi implementado no domínio, contrato, repository, snapshot existente,
coordinator e Exercícios. A revisão pode registrar o aceite junto com sua
conclusão; atividade `READY_TO_SYNC` ou `SYNC_FAILED` não sincronizada oferece
ação equivalente. O coordinator permanece fail-closed para indicadores reais,
preserva operações por filho e não altera payloads congelados.

## Validação e evidência

```bash
adb devices
./gradlew installDebug
adb shell dumpsys package br.com.mwconsulting.championcompanion.dev
adb shell dumpsys package com.google.android.apps.healthdata
adb shell monkey -p br.com.mwconsulting.championcompanion.dev 1
adb exec-out screencap -p > <evidência sanitizada>
./gradlew lint assembleDebug assembleRelease
git diff --check
git status --short
```

Registrar provider, package, permissões, estados por filho, falhas e retry sem
IDs, payload BLE, BPM ou identificadores pessoais. Toques no dispositivo não
constituem teste automatizado. Não executar `Phase003PrivacyGuardTest`.

## Critérios de aceite

- `WRITE_DISTANCE`, `WRITE_SPEED` e demais permissões elegíveis são verificadas
  no provider real para o mesmo package instalado.
- Provider aceita projeção de distância e `SpeedRecord` no dispositivo-alvo, ou
  a incompatibilidade permanece documentada como bloqueador sem falso `PASS`.
- Atividade com permissões completas exibe distância e velocidade no provider e
  mantém operações locais por filho.
- Revogação de `WRITE_SPEED` produz falha nomeada de velocidade, sem atividade
  `SYNCED`; revogação de `WRITE_DISTANCE` produz resultado equivalente para
  distância.
- Reautorização permite retry idempotente com IDs, versões e payloads
  congelados, sem duplicação observável.
- UI distingue permissão pendente, falha de distância, falha de velocidade,
  provider indisponível e escrita local aceita sem readback.
- UI distingue aceite ausente de permissão pendente e falha por filho; após
  aceite, Settings exibe `Velocidade` e solicita `WRITE_SPEED` quando SPEED é
  elegível.
- Nenhuma leitura ampla do Health Connect, fallback de distância, rota ou fluxo
  phone é introduzido.
- Gate F de `014-15` recebe nova evidência física revisada; `014-15` somente
  poderá ser marcada após todos os bloqueadores restantes serem tratados.

## Definição de pronto

Follow-up funcional implementado, matriz física de autorização, distância,
velocidade, retry e falha parcial registrada, Gate F de `014-15` revalidado
para os cenários novos e revisão independente concluída sem bloqueadores
funcionais. `014-17` não marca `014-15` nem `014-16` automaticamente.

## Registro de execução local

- Causa funcional corrigida: filhos DISTANCE/SPEED só são preparados após
  `EXPLICITLY_ACCEPTED` persistido no snapshot ativo.
- Nenhuma migration, schema, Gradle, manifesto, leitura remota ou teste
  automatizado foi criado/executado.
- Validação física complementar realizada no Galaxy S22 `SM-S901E`, Android 16,
  com package debug `br.com.mwconsulting.championcompanion.dev` e provider
  `com.google.android.apps.healthdata`.
- Após nova limpeza de dados, importação explícita inseriu 3 treinos. Perfil
  recebeu `stepLengthMeters = 0,8818 m` por entrada manual.
- Uma atividade `WATCH_D3_D4` foi revisada com aceite explícito, promovida a
  `READY_TO_SYNC` e sincronizada. O provider mostrou manualmente distância
  `3,463 km` e velocidade `7,245 km/h` da fonte `Champion Companion Dev`.
- `WRITE_SPEED` foi revogada isoladamente no provider. Settings exibiu
  `Velocidade — Pendente`; a permissão foi restaurada pelo fluxo `Conectar` do
  app e o package voltou a exibir acesso concedido.
- Registro histórico da primeira tentativa: não havia outra atividade
  `WATCH_D3_D4` elegível não sincronizada; a segunda corrida importada tinha
  duração zero e permaneceu sem derivação após reprocessamento explícito.
- Esse bloqueio foi superado pela segunda importação física com dois treinos
  novos; `014-15` permanece `blocked` somente por seus demais gates e pela
  revisão documental independente.
- Validação complementar concluída com dois treinos novos do relógio: o treino
  de 15 minutos foi revisado como `Corrida`; o de 20 minutos, como `Treino
  livre`. Com `WRITE_SPEED` revogada, a corrida produziu falha parcial por
  capacidade somente no filho de velocidade.
- Após restauração de `WRITE_SPEED`, o retry da mesma corrida terminou
  `Sincronizada`, sem reescrever o filho de distância já aceito. O provider
  mostrou o novo intervalo de velocidade `0,202 km/h` e distância acumulada
  `3,517 km`, compatível com o acréscimo do treino novo.
- `WRITE_DISTANCE` foi revogada isoladamente; o treino livre produziu falha
  parcial nomeada para três filhos de distância pendentes. Após restauração, o
  retry terminou `Sincronizada`; o provider mostrou distância acumulada
  `3,570 km`, com acréscimo novo de `0,053 km` e sem duplicação observada.
- O provider mostrou uma única entrada nova de velocidade para `12:33–12:49` e
  entradas de distância correspondentes ao novo treino; não foram observadas
  duplicações no retry.
- A tela de resultado do retry manteve confirmação local e informou que o
  provider não foi lido pelo app.

## Arquivos alterados

- Implementação já registrada nesta subtarefa: `core/healthconnect/HealthConnectGateway.kt`,
  `core/healthconnect/HealthConnectSyncCoordinator.kt`,
  `core/model/RepositoryContracts.kt`, `core/model/WatchWorkoutActivity.kt`,
  `core/model/WatchWorkoutDerivedMetrics.kt`, `data/RoomRepositories.kt`,
  `feature/exercises/ExercisesFeatureContract.kt`, `ExercisesScreen.kt`,
  `ExercisesViewModel.kt`, `feature/settings/SettingsScreen.kt` e
  `SettingsViewModel.kt`.
- Evidências sanitizadas: `docs/integration/014-15-physical-validation.md`,
  `docs/integration/014-15-health-connect-provider-follow-up.md` e
  `docs/integration/014-15-physical-validation-evidence/`.
- Planejamento atualizado: esta subtarefa, `014-15-run-physical-validation.md`
  e `014-00-overview.md`.

## Comandos executados e evidências

- `adb devices`: Galaxy S22 conectado.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`.
- `adb shell dumpsys package br.com.mwconsulting.championcompanion.dev`:
  `WRITE_DISTANCE` e `WRITE_SPEED` concedidas após restauração.
- `adb shell am start -a android.health.connect.action.MANAGE_HEALTH_DATA`:
  provider real abriu telas de distância, velocidade e acesso por app.
- `adb shell am start -a android.health.connect.action.MANAGE_HEALTH_PERMISSIONS
  --es android.intent.extra.PACKAGE_NAME br.com.mwconsulting.championcompanion.dev`:
  provider abriu permissões do package debug; `WRITE_SPEED` e `WRITE_DISTANCE`
  foram revogadas e restauradas manualmente.
- Interação manual: importação, revisão, aceite explícito, sincronização,
  revogação isolada de velocidade e distância, falha parcial, reautorização e
  retry.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `git diff --check`: sucesso, sem saída.
- Evidências principais: `014-17-health-connect-distance.png`,
  `014-17-health-connect-speed.png`, `014-17-health-connect-speed-access.png`,
  `014-17-health-connect-speed-revoked.png`,
  `014-17-health-connect-restored.png` e
  `014-17-settings-permissions-post-sync.png`,
  `014-17-health-connect-distance-after-retry.png`,
  `014-17-health-connect-distance-after-distance-retry.png` e
  `014-17-health-connect-speed-after-retry.png`.

## Resultados e riscos residuais

- PASS: provider aceitou e exibiu `DistanceRecord` granular e `SpeedRecord`
  para atividades elegíveis com ambas as permissões.
- PASS: package correto e estados concedidos de `WRITE_DISTANCE` e `WRITE_SPEED`
  foram observados antes e depois das reautorizações.
- PASS: corrida de 15 minutos produziu falha nomeada de velocidade, e treino
  livre de 20 minutos produziu falha nomeada de distância; nenhum ficou
  `SYNCED` durante a permissão revogada.
- PASS: retry da mesma atividade após reautorização terminou `Sincronizada` nos
  dois cenários; não foram observadas duplicações no provider.
- Os dois treinos antigos permanecem aguardando revisão; não são necessários
  para esta matriz e não foram sincronizados.
- Revalidação física passou para os dois treinos novos: falha parcial por
  velocidade e por distância, retry concluído, estado final `Sincronizada` e
  registros provider sem duplicação observada.
- Ainda há dois treinos antigos aguardando revisão local; não fazem parte desta
  matriz e não foram sincronizados.

## Riscos e cuidados

- Permissões Health Connect são específicas do package final; debug e release
  não devem compartilhar evidência.
- Provider pode declarar disponibilidade geral sem aceitar `SpeedRecord`.
- Interface do provider pode não exibir todos os tipos no mesmo resumo de treino;
  evidência deve consultar o tipo de dado correto.
- Operação já `SYNCED` não deve ser reaberta silenciosamente para reparar
  ausência observada somente na UI do provider.
- Falha de provider não pode apagar dados locais nem alterar snapshot canônico.

## Rollback

Reverter somente ajustes funcionais ou documentais da integração introduzidos
por esta subtarefa. Preservar operações, payloads congelados, evidências válidas,
migrations e schemas existentes.
