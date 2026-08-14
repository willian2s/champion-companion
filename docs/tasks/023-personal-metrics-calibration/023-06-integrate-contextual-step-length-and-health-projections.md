# 023-06 — Integrar comprimento contextual à derivação e às projeções

- **Status:** `completed`

## Escopo desta execução

Implementar evolução retrocompatível do frozen payload Health Connect para
identificar `WATCH_DERIVED` e `EXTERNAL_REFERENCE_CALIBRATION`, sem alterar
identidade de operações ou criar efeitos remotos; a validação física permanece
fora desta subtarefa e pertence a `023-07`.
- **Depends on:** `023-05`

## Objetivo

Fazer novos treinos de corrida/caminhada usarem o perfil aprendido correto sem
alterar silenciosamente atividades históricas e alinhar consumers locais/Health
Connect com a nova provenance de calibração.

## Nova versão de derivação

Introduzir versão identificável para a política contextual:

```text
watch-d3-d4-v3
```

O parser permanece `d4-v3`.

Snapshots antigos `watch-d3-d4-v2` continuam legíveis e não são migrados.

## Resolver perfil efetivo

Implementar resolução central, fora da UI:

```text
RUNNING -> learned RUNNING ?: BASE
WALKING -> learned WALKING ?: BASE
TREADMILL -> BASE
FREE_WORKOUT -> comportamento vigente
CYCLING -> sem distância por passos
DailySummary -> BASE
```

A resolução precisa ocorrer de forma coerente com o momento em que o tipo foi
confirmado, conforme decisão congelada em `023-01`.

## Importação e revisão

Evitar usar perfil contextual errado antes de `confirmedType` existir.

Direção preferida:

- importação inicial mantém derivação/base atual quando necessário;
- revisão de RUNNING/WALKING pode rederivar explicitamente com perfil contextual
  antes do sync;
- nova atividade após perfil aprendido deve terminar com snapshot que identifique
  o valor/contexto realmente usado;
- mudança de tipo não reaproveita snapshot semântico incompatível.

## Consumers locais

Auditar/adaptar:

- lista de Exercícios;
- detalhe da atividade;
- Calendário/detalhe diário;
- históricos de distância;
- splits/pace/velocidade;
- qualquer read model que assuma um único `stepLengthMeters` atual.

Todos devem consumir o snapshot congelado da atividade, nunca o perfil pessoal
corrente para recalcular histórico em leitura.

## Perfil UI

Conectar os perfis aprendidos à tela Perfil:

```text
Corrida
0,846 m por passo
2 calibrações • 8,36 km de referência

Caminhada
0,721 m por passo
1 calibração • 3,12 km de referência
```

Sem evidência:

```text
Ainda sem calibrações de corrida.
Será usado o comprimento de passo base.
```

Mostrar acesso ao histórico/correção de evidências.

## Health Connect

Auditar o contrato pós-Spec 022 para atividades diretamente calibradas por
referência.

Requisitos:

- sessão/BPM/calorias mantêm provenance vigente;
- distância/velocidade diretamente baseadas em referência digitada não podem ser
  apresentadas como GPS coletado pelo app;
- preservar IDs determinísticos, versões monotônicas e frozen payloads;
- não duplicar `DistanceRecord`;
- não quebrar operações antigas;
- não criar efeitos remotos durante rederivação/calibração;
- sync continua explícito.

Se for necessário adicionar provenance ao frozen payload, preferir evolução
retrocompatível e documentar versão/decoder. Não aumentar schema por conveniência.

Treino futuro que apenas usa perfil aprendido continua seguindo a projeção
canônica automática vigente, pois a distância daquele treino não foi digitada.

## Daily Summary

Confirmar explicitamente que a estimativa diária continua usando `BASE`.

Não substituir por walking/running learned profile porque passos do dia não têm
contexto confiável.

## Compatibilidade 022

Confirmar:

- `CYCLING` continua com `manualDistanceMeters` opcional;
- nenhum learned step profile é consumido por bicicleta;
- nenhuma `DistanceInterval`/`SpeedSeries` por passos retorna para CYCLING.

## Checklist

 - [x] derivation version nova introduzida
 - [x] resolver contextual centralizado
 - [x] RUNNING usa learned/fallback corretamente
 - [x] WALKING usa learned/fallback corretamente
 - [x] TREADMILL continua BASE
 - [x] DailySummary continua BASE
 - [x] CYCLING permanece isolado
 - [x] consumers usam snapshot congelado
 - [x] Perfil mostra learned profiles/evidências
 - [x] HC provenance auditada e implementada
 - [x] payloads antigos continuam legíveis
 - [x] sync continua explícito
 - [x] spec/overview atualizados

## Aceite

Depois de duas calibrações de corrida, um novo treino RUNNING sem distância de
referência deve usar o perfil aprendido corrente e congelá-lo em seu próprio
snapshot, permanecendo imutável quando calibrações futuras alterarem o perfil.

## Resultado da execução

 - **Status:** `completed`
 - **Escopo executado:** integração contextual da resolução de comprimento do
   passo na derivação, reprocessamento explícito, leitura de Exercícios e guards
   fail-closed de projeções Health Connect. Provenance do frozen payload v2 já
   existente foi preservada.
 - **Arquivos alterados nesta execução:**
   - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`;
   - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`;
   - `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
   - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`;
   - esta subtarefa;
   - `docs/tasks/023-personal-metrics-calibration/023-00-overview.md`.
 - **Decisões e desvios:** `StepLengthResolutionPolicy` tornou-se política única:
   corrida/caminhada usam perfil aprendido com fallback base; esteira e resumo
   diário permanecem base; bicicleta retorna ausência de perfil por passos.
   `confirmedType` é lido no contexto da derivação, perfil contextual e
   calibração direta usam `watch-d3-d4-v3`, e revisão continua sem rederivação
   implícita. Nenhum desvio.
 - **Compatibilidade preservada:** chave da operação, IDs, versionamento
   monotônico, `originId = activity.id` e `originVersion` do snapshot continuam
   sob contratos existentes; enqueue, reconciliação e sync continuam explícitos
   e inalterados. Snapshots v2 permanecem legíveis e não são migrados.
  - **Guards:** rederivação própria aceita somente
  `PENDING_CALIBRATION`, `READY_TO_SYNC` e `SYNC_FAILED` sem efeito remoto
   aceito; `PENDING_REVIEW`, `SYNCING`, `SYNCED`, `DISCARDED`, filhos em retry
   (`PENDING`/`FAILED`) e metadados inconsistentes falham fechados. Derivação
   inicial não materializa snapshot em estados terminais ou em ciclo remoto.
 - **Comandos executados:** `./gradlew :app:compileDebugKotlin --no-daemon`,
   `./gradlew :app:lintDebug --no-daemon`,
   `./gradlew lint assembleDebug assembleRelease --no-daemon`,
   `git diff --check`.
 - **Resultados e evidências:** compilação Kotlin, lint debug, lint, assemble
   debug, assemble release e verificação de whitespace passaram. Build release
   emitiu apenas warnings preexistentes de override deprecated e cast unchecked.
   Nenhum teste automatizado novo foi criado ou executado.
 - **Revisão independente:** revisão identificou risco de derivação direta em
   estados terminais e risco de payload congelado pendente; guards fail-closed
   foram aplicados antes da materialização do snapshot e revalidados por
   compilação/lint/build.
 - **Riscos residuais:** validação física de payload v2, retry e Health Connect
   real permanece em `023-07`; não há efeitos remotos durante calibração ou
   rederivação. A subtarefa `023-07` continua pendente.
