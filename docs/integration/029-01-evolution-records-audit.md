# 029-01 — Auditoria de evolução, recordes e persistência

## Resultado

Auditoria concluída sobre o fluxo watch-first vigente antes da implementação da Spec 029.

## Fontes revisadas

- `AGENTS.md`;
- `docs/baseline/current-baseline.md`;
- `docs/roadmap/post-phase-15-direction.md`;
- Spec 011 de revisão/calibração;
- Spec 022 de bicicleta;
- Spec 023 de calibração por distância de referência;
- `core/model/WatchWorkoutActivity.kt`;
- `core/model/RepositoryContracts.kt`;
- `core/model/CalendarHistory.kt`.

## Escopo de atividades da 029

A primeira versão da feature usa somente atividades `WATCH_D3_D4`.

Código histórico de treinos phone permanece preservado, mas não será expandido nem usado para justificar nova semântica na 029.

## Elegibilidade por workflow

| Workflow | Evolução/recordes | Motivo |
| --- | --- | --- |
| `PENDING_REVIEW` | não | tipo e decisões ainda transitórios |
| `PENDING_CALIBRATION` | não | distância final de esteira ainda pendente |
| `READY_TO_SYNC` | sim | revisão local concluída |
| `SYNCING` | sim | métricas locais finais; sync é transição remota |
| `SYNCED` | sim | atividade final |
| `SYNC_FAILED` | sim | falha remota não invalida fatos locais |
| `DISCARDED` | não | removida logicamente da população |

`ParserValidity.PARTIAL` não é exclusão automática: a Spec 011 permite revisão e avanço para estado final. Cada métrica ainda precisa passar por sua validação própria.

## Tipos

- `UNKNOWN`: sempre inelegível;
- `RUNNING`: elegível;
- `WALKING`: elegível;
- `TREADMILL`: elegível somente depois da calibração obrigatória quando o workflow exigir;
- `CYCLING`: elegível, com política própria de distância e sem passos de performance;
- `FREE_WORKOUT`: elegível para contagem, duração, dias ativos e passos medidos; distância não participa de recorde pessoal inicial por semântica pouco específica.

## Fontes de verdade das métricas

### Distância final

`RUNNING` / `WALKING`:

```text
WorkoutActivity.derivationSnapshot.canonicalDistanceMeters
```

O snapshot pode refletir perfil contextual ou calibração por distância de referência quando explicitamente aplicada. O valor ativo persistido é a fonte; não recalcular distância dentro do evaluator.

`TREADMILL`:

```text
WorkoutActivity.metrics.distances.calibratedMeters
```

Somente após `calibrateImportedActivity`/`applyCalibration` levar a atividade a `READY_TO_SYNC`.

`CYCLING`:

```text
WorkoutActivity.metrics.distances.manualDistanceMeters
```

A distância é opcional. Ausência permanece ausência e não bloqueia duração/contagem.

`FREE_WORKOUT`:

- não usar para recorde de maior distância na primeira versão;
- eventual distância apresentada por consumer existente pode permanecer para volume somente se a implementação da `029-04` reutilizar exatamente a política local já vigente.

### Duração

Usar `WorkoutActivity.metrics.duration`, desde que positiva. A data de agrupamento vem de `metrics.startedAt`/início real, nunca `recordedAt` ou data de importação.

### Passos

Usar `WorkoutActivity.metrics.steps` somente quando `stepsAvailable == true` e o tipo for elegível.

- `RUNNING`, `WALKING`, `TREADMILL`, `FREE_WORKOUT`: elegíveis;
- `CYCLING`: inelegível para recorde/volume de passos de performance.

### Frequência cardíaca

Para tendência por atividade, a média persistida pode ser exibida como fato daquela atividade quando válida. Agregado entre atividades não deve ser média simples dessas médias. Se a 029 implementar uma média de período, usar amostras reais elegíveis ou outra ponderação explicitamente documentada na `029-05`.

### Cadência

Usar somente cadência D4 derivada por intervalo. Para resumo por atividade/período, direção aprovada:

```text
cadenceSpm = totalEligibleSteps / totalEligibleDurationSeconds * 60
```

sem média simples das cadências dos intervalos, sem suavização e sem timeline 1 Hz.

### Pace

Quando implementado para população comparável:

```text
pace = totalEligibleDurationSeconds / totalEligibleDistanceKilometers
```

Nunca média simples de paces. Separar modalidades; `CYCLING` não usa pace.

### Velocidade

Quando implementada, derivar de distância final / duração para população semanticamente compatível. Bicicleta permanece separada de corrida/caminhada/esteira.

## Recordes iniciais

### Maior distância

Por tipo:

- `RUNNING`;
- `WALKING`;
- `TREADMILL`;
- `CYCLING` quando possui distância manual.

`FREE_WORKOUT` fica fora deste recorde inicial.

### Maior duração

Por tipo confirmado elegível, incluindo `FREE_WORKOUT` e `CYCLING`.

### Mais passos

Por tipo confirmado elegível:

- `RUNNING`;
- `WALKING`;
- `TREADMILL`;
- `FREE_WORKOUT`.

`CYCLING` fica fora.

Não criar recorde de maior BPM ou maior cadência.

## Pontos de persistência que podem disparar evaluator

### 1. `WorkoutActivityRepository.submitReview`

`SubmitWorkoutReviewInput` já inclui `manualDistanceMeters` aplicável somente a `CYCLING`.

Após sucesso:

- `RUNNING`, `WALKING`, `FREE_WORKOUT` e `CYCLING` podem ficar em estado final local elegível;
- avaliar somente se o snapshot retornado/persistido estiver em workflow elegível;
- `TREADMILL` que resultar em `PENDING_CALIBRATION` não dispara recorde ainda.

### 2. `TreadmillCalibrationRepository.calibrateImportedActivity`

É o ponto que persiste a calibração e leva a atividade de esteira para estado final local. Após sucesso, recarregar a atividade e avaliar todos os record types aplicáveis.

### 3. `PersonalMetricsRepository.applyReferenceDistanceCalibration`

Pode rederivar explicitamente `RUNNING`/`WALKING` pré-sync e alterar distância canônica, pace e velocidade. Após sucesso, se a atividade permanecer em workflow elegível, avaliar recordes com snapshot anterior e novo snapshot persistido.

### 4. `WorkoutActivityRepository.rederiveWatchWorkout` / `reprocessWatchWorkout`

Ações explícitas podem substituir snapshot ativo e alterar distância calculada. Não disparar durante reimportação D3/D4; avaliar somente após a rederivação explícita concluir e a atividade estar elegível.

## Operações que não disparam conquista

- `persistImported` / reimportação D3/D4;
- `acceptWatchWorkoutPlausibility` quando não altera métrica recordável;
- `startSync`, `markSynced`, `markSyncFailed`;
- atualização de evidência de calibração que explicitamente não rederiva a atividade;
- remoção de evidência que afeta apenas perfis futuros;
- discard/delete.

Essas operações podem mudar quem aparece como recordista no read model, mas não representam um novo recorde pós-save da atividade.

## Idempotência pós-save

O evaluator deve receber o snapshot anterior persistido e o novo snapshot persistido.

```text
baselineOther = máximo das demais atividades elegíveis
oldEligibleValue = valor anterior da atividade no mesmo escopo, se elegível
previousBest = max(baselineOther, oldEligibleValue)

emitir somente quando newValue > previousBest
```

Isso cobre:

- resave sem mudança: não emite;
- atividade recordista corrigida para cima: emite e compara contra o próprio valor anterior;
- mudança de tipo: compara contra a baseline do novo tipo;
- atividade antiga importada hoje: compara contra todo o histórico atualmente conhecido;
- empate: não emite.

## Read model

`WorkoutActivityRepository.observeActivitiesBetween(...)` já expõe projeção temporal leve de atividades não descartadas e o calendário já possui semântica de distância funcional. Para recordes e detalhes de passos/cadência, a 029 precisará ler atividades completas ou criar uma projeção de leitura aditiva; não há justificativa atual para tabela materializada.

## Navegação

Direção escolhida:

- adicionar `AppDestination.Evolution`;
- tratar como subárvore de `Exercises` no back stack/seleção inferior;
- entrada inicial por ação/card na superfície Exercícios;
- não adicionar nova aba inferior nesta spec.

Isso mantém Evolução como análise esportiva sem aumentar a navegação principal.

## Impacto de persistência

Nenhuma migration Room é necessária para a arquitetura planejada. Se durante implementação surgir necessidade real de materialização, a task correspondente deve parar e registrar nova decisão antes de alterar schema.

## Saída

`029-02` pode criar os contratos/read model com estas decisões congeladas. O destaque de recorde deve ser integrado somente na `029-09`, depois que evaluator e superfície de recordes já estiverem estáveis.
