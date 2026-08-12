# 023-02 — Persistir calibrações e expor perfis aprendidos

- **Status:** `pending`
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

- [ ] entidade/DAO definidos
- [ ] migration 12->13 implementada
- [ ] schema 13 exportado
- [ ] domínio de evidência implementado
- [ ] perfil RUNNING agregado deterministicamente
- [ ] perfil WALKING agregado deterministicamente
- [ ] uma evidência por activityId garantida
- [ ] substituição sem dupla contagem
- [ ] remoção recalcula leitura futura
- [ ] perfil BASE preservado
- [ ] spec/overview atualizados

## Aceite

Com duas evidências de corrida, o repositório deve expor exatamente:

```text
Σ referenceDistanceMeters / Σ decodedSteps
```

sem alterar qualquer atividade existente.