# 027-05 — Publicar cadência por intervalos D4

- **Ticker:** `027`
- **Número:** `05`
- **Status:** `completed`
- **Depends on:** `027-01`, `027-02`, `027-03`, `027-04`

## Objetivo

Adicionar `StepsCadenceRecord` à projeção Health Connect da atividade usando somente intervalos D4 válidos, sem depender da timeline de 1 Hz.

## Derivação

Para cada intervalo elegível:

```text
cadenceSpm = steps / durationSeconds * 60
```

Usar duração normalizada já persistida/derivada pela atividade.

## Elegibilidade da amostra

Não produzir amostra quando:

- duração <= 0;
- passos ausentes/inválidos;
- intervalo fora da janela da sessão;
- tipo de atividade não admite cadência de passos;
- dados de origem estiverem em estado que a política atual considera não utilizável.

## Timestamp

Usar convenção determinística coerente com a timeline D4 atual, preferencialmente midpoint do intervalo quando confirmado na auditoria.

Não apresentar midpoint como timestamp medido pelo sensor.

## Health Connect

Criar projeção filha estável para `StepsCadenceRecord`:

- uma identidade por série/versão;
- múltiplas samples no record quando API atual permitir;
- retry com payload congelado;
- permissão explícita de cadência;
- metadata coerente com origem do relógio.

## Independência dos passos

Cadência:

- não gera crédito;
- não consome crédito;
- continua elegível quando `StepsRecord` da atividade estiver `ALREADY_COVERED_BY_DAILY`;
- não altera `DailySummary`.

## Métricas locais

Pode expor média/mínimo/máximo derivados para UI somente se isso já se encaixar no modelo atual sem inventar estatística incompatível. O foco desta task é a série Health Connect.

## Fora de escopo

- suavização;
- interpolação 1 Hz;
- mudança em BPM;
- validação Strava — `027-06`.

## Verificação

```bash
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.

## Resultado da execução

- **Status:** `completed`
- **Arquivos alterados:**
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthRecordMapper.kt`;
  - este arquivo;
  - `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-00-overview.md`.
- **Decisões:** `StepsCadenceSeries` entra na lista de filhos elegíveis quando a
  atividade não é ciclismo, o snapshot de derivação é elegível e há intervalos
  canônicos D4. Payload congelado schema 3, midpoint determinístico,
  `WRITE_STEPS`, retry e identidade existentes foram preservados. Cadência
  continua independente de `WorkoutSteps`, créditos e `DailySummary`.
- **Desvios:** nenhum. Não houve migration, nova permissão, timeline 1 Hz,
  suavização, interpolação ou alteração de BPM.
- **Comandos executados:** `git diff --check`; `./gradlew lint assembleDebug
  assembleRelease`. Não foram criados nem executados testes automatizados,
  conforme `AGENTS.md` e Spec 027.
- **Resultados e evidências:** `git diff --check`, lint, assemble Debug e
  assemble Release passaram. O fluxo normal agora prepara o filho
  `StepsCadenceSeries`; o mapper existente congela amostras D4 válidas usando
  midpoint dentro da janela da sessão e o gateway já escreve
  `StepsCadenceRecord` com metadata de origem do relógio.
- **Revisão independente:** aprovada com ressalvas residuais; nenhum bloqueador
  identificado. A revisão confirmou checklist único, progresso `5/11`,
  independência de créditos/Daily, identidade, retry, schema 3 e permissão
  preservados.
- **Riscos residuais:** Health Connect permanece write-only; compatibilidade
  Health Sync/Strava e preservação de cadência no destino ainda dependem da
  validação física de `027-06`. Não há testes automatizados para retry,
  cobertura de amostras ou variantes de firmware.
