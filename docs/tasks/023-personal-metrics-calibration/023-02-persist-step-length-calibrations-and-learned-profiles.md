# 023-02 — Persistir calibrações e expor perfis aprendidos

- **Status:** `completed`
- **Depends on:** `023-01`

## Objetivo

Adicionar a persistência mínima para evidências de distância de referência e uma
leitura determinística dos perfis aprendidos de corrida/caminhada, sem alterar
atividades ou Health Connect durante migration.

## Direção de persistência

Room:

```text
v12 -> v13
```

Tabela planejada:

```text
step_length_calibrations
- id
- profileId
- activityId UNIQUE
- workoutType
- referenceDistanceMeters
- decodedSteps
- acceptedAtEpochMillis
- updatedAtEpochMillis
```

A auditoria `023-01` pode ajustar FK/índices desde que preserve:

- uma evidência ativa por atividade;
- consulta eficiente por perfil + tipo;
- nenhum estado derivado duplicado obrigatório;
- migration aditiva e sem wipe.

## Domínio

Adicionar contratos equivalentes a:

```text
StepLengthCalibration
LearnedStepLengthProfile
StepLengthCalibrationSummary
```

Regras:

- apenas `RUNNING` e `WALKING` são contextos calibráveis;
- `referenceDistanceMeters > 0` e finito;
- `decodedSteps > 0`;
- `observedStepLength = referenceDistance / decodedSteps`;
- perfil aprendido = `Σ distância / Σ passos`;
- `sampleCount`, distância total, passos totais e `updatedAt` devem estar
  disponíveis para UI/auditoria.

## Repository

Expandir `PersonalMetricsRepository` (ou contrato auditado equivalente) para:

- observar calibrações por contexto;
- observar perfis aprendidos;
- salvar/substituir evidência de uma atividade;
- remover evidência;
- resolver perfil efetivo para um `WorkoutType` sem depender de UI.

O `StepLengthProfile` base atual continua separado e compatível.

## Migration

Requisitos:

- registrar `MIGRATION_12_13` ou mecanismo equivalente explícito;
- atualizar `ChampionDatabase.version` para 13;
- exportar schema 13;
- nenhum `INSERT` em calibrações durante upgrade;
- nenhuma rederivação;
- nenhuma alteração de fila HC;
- preservar `manualDistanceMeters` da Spec 022 e demais dados v12.

## Fora do escopo

- UI de calibração;
- aplicar referência à atividade;
- editar altura/meta/peso;
- mudar consumer de derivação;
- preparar records Health Connect.

## Checklist

- [x] entidade/DAO definidos
- [x] migration 12->13 implementada
- [x] schema 13 exportado
- [x] domínio de evidência implementado
- [x] perfil RUNNING agregado deterministicamente
- [x] perfil WALKING agregado deterministicamente
- [x] uma evidência por activityId garantida
- [x] substituição sem dupla contagem
- [x] remoção recalcula leitura futura
- [x] perfil BASE preservado
- [x] subtarefa/overview atualizados

## Evidências

- Entidade `StepLengthCalibrationEntity` usa FK somente para `user_profiles` com
  `ON DELETE CASCADE`, `UNIQUE(activityId)` e índice `(profileId, workoutType)`.
- `MIGRATION_12_13` é aditiva, não insere calibrações e foi registrada no módulo
  de banco; versão Room atualizada para 13.
- `LearnedStepLengthProfile` é calculado em DAO/repository por `Σ distância /
  Σ passos`, com `sampleCount`, totais e `updatedAt` máximo.
- `saveStepLengthCalibration` aceita somente o perfil atual; deriva a soma de
  `decodedSteps` dos fatos D4 persistidos e rejeita denominador divergente,
  série ausente/inelegível, workflow não autorizado ou projeção filha com
  efeito remoto. Estados permitidos: `PENDING_CALIBRATION`, `READY_TO_SYNC` e
  `SYNC_FAILED` sem filhos inseguros.
- A inspeção de operações filhas falha fechada para estados `SYNCING`/
  `SYNCED`, checkpoint `AFTER_REMOTE_EFFECT`, metadata inválida ou qualquer
  filho desconhecido. `CYCLING` retorna `NotFound` no resolver e não recebe
  semântica de distância por passos.
- A inspeção reutiliza `HealthSyncOperationEntity.toDomainStrict()` e valida
  identidade, versão, ação/tipo do filho, payload congelado, origem e coerência
  de reconciliação antes de permitir `SYNC_FAILED`.
- Operações legacy de atividades `WATCH_D3_D4` (`projectionChildKey =
  '__legacy__'`) agora são sempre inconsistentes para calibração e bloqueiam
  fail-closed; não foi criada reconciliação nova.
- A agregação lê somente linhas filtradas pelo DAO (`referenceDistanceMeters >
  0`, `decodedSteps > 0`) e ainda mapeadas/validadas individualmente pelo
  domínio; linhas corrompidas não entram em `sampleCount`, totais ou `updatedAt`.
- Contratos de repository permanecem compatíveis por métodos default; fakes
  existentes não precisam implementar APIs novas.
- Validação: `./gradlew clean app:compileDebugKotlin --no-daemon` passou;
  `./gradlew app:lintDebug --no-daemon` passou; `git diff --check` passou.
- `./gradlew exportRoomSchema --no-daemon` falhou porque task não existe no
  projeto; schema v13 já está presente em `app/schemas/.../13.json`.
- Risco de migration: operações legacy preservadas por migrations continuam
  bloqueando calibração até eventual decisão explícita de reconciliação futura.

## Aceite

Com duas evidências de corrida, o repositório deve expor exatamente:

```text
Σ referenceDistanceMeters / Σ decodedSteps
```

sem alterar qualquer atividade existente.
