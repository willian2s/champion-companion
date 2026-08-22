# 027-00 — Overview: passos, cadência e timeline 1 Hz

## Objetivo

Executar a Spec 027 em dois gates independentes:

- **Gate A:** publicar passos/cadência de atividades no Health Connect sem inflar o acumulado diário;
- **Gate B:** produzir timeline derivada comum de 1 Hz para BPM e cadência.

## Pré-condições

Antes de qualquer task:

1. ler `AGENTS.md`;
2. ler `docs/baseline/current-baseline.md`;
3. ler `docs/specs/027-workout-steps-cadence-1hz-timeline.md`;
4. para Gate A, ler também `docs/specs/016-health-connect-incremental-daily-steps.md` e ADRs Health Connect vigentes;
5. auditar o código real da branch antes de assumir schema, identidades ou APIs.

## Invariantes

- `DailySummary.steps` permanece cumulativo e autoritativo para o total diário observado;
- passos enviados por atividade não podem ser somados novamente pelo próximo incremento diário;
- crédito só é válido após aceite remoto do `StepsRecord` da atividade;
- retry não duplica record nem crédito;
- atividade já coberta por Daily sincronizado não publica novo `StepsRecord`;
- `StepsCadenceRecord` não participa do crédito de passos;
- dados D4 de origem nunca são substituídos pela timeline 1 Hz;
- gaps estruturais permanecem gaps;
- sync continua manual, foreground e write-only;
- nenhuma task reativa treino phone, GPS, mapa ou rota.

## Gate A — Integridade de passos e interoperabilidade

### 027-01 — Auditar pipeline e fechar decisão

Auditar:

- Spec 016 e checkpoint incremental;
- `DailyStepsSyncPayload` e persistência;
- `HealthSyncOperation`/filhos congelados;
- mapper/gateway/permissões;
- `WorkoutActivity`, intervalos D4 e total de passos;
- cobertura temporal já sincronizada;
- semântica oficial de `StepsRecord` e `StepsCadenceRecord`.

Saída: documento de auditoria + ADR-025 se continuar sendo o próximo número livre.

### 027-02 — Modelar passos/cadência e crédito

Criar contratos persistentes mínimos para:

- filho de passos da atividade;
- filho de cadência;
- estado idempotente de crédito/cobertura;
- payload congelado e identidade estável.

Migration apenas se necessária após auditoria.

### 027-03 — Publicar passos da atividade

Adicionar `StepsRecord` elegível de `WorkoutActivity`, sem ainda alterar silenciosamente o Daily Summary.

Aceite remoto deve ativar crédito idempotente. Falha não ativa crédito.

### 027-04 — Integrar crédito ao Daily Summary

Calcular residual diário seguro:

```text
grossDelta - acceptedWorkoutCredits = residualDailySteps
```

Preservar checkpoint cumulativo real do relógio, sem valor negativo, sem crédito cruzando dia e sem sobreposição/duplicidade remota.

### 027-05 — Publicar cadência por intervalos D4

Derivar `steps/min` por intervalo válido e publicar `StepsCadenceRecord` independente da contabilidade diária.

### 027-06 — Validar fisicamente HC -> Health Sync -> Strava

Validar pelo menos:

- atividade primeiro, Daily depois;
- Daily primeiro, atividade depois;
- retry;
- total diário final correto;
- cadência no Health Connect;
- resultado real no Strava via Health Sync.

Classificar `go`, `go_with_adjustments` ou `no_go`.

## Gate B — Timeline derivada 1 Hz

### 027-07 — Definir contrato/versionamento temporal

Formalizar SOURCE / DERIVED / RESAMPLED_1HZ, parâmetros, política de gaps e autoridade estatística.

### 027-08 — BPM 1 Hz

Implementar validação robusta, suavização temporal e interpolação somente em regiões elegíveis.

### 027-09 — Cadência 1 Hz

Implementar derivação/suavização temporal da cadência e reamostragem na mesma grade de 1 segundo.

### 027-10 — Integrar timeline e avaliar projeção HC

Usar a timeline comum em detalhes/gráficos e decidir, com base na evidência do Gate A, se `StepsCadenceRecord` deve permanecer por bucket ou migrar para série 1 Hz versionada.

### 027-11 — Validação final e fechamento

Validar integridade temporal, gaps, estatísticas, UI afetada, Health Connect e documentação final.

## Ordem

```text
027-01
  ↓
027-02
  ↓
027-03
  ↓
027-04
  ↓
027-05
  ↓
027-06   Gate A
  ↓
027-07
  ↓
027-08
  ↓
027-09
  ↓
027-10
  ↓
027-11   Gate B / fechamento
```

## Verificação

Para tasks com código ou Gradle:

```bash
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito do usuário.
