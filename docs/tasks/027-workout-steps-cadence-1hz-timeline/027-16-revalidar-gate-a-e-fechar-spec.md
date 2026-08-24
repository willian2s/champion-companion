# 027-16 — Revalidar Gate A e fechar a Spec 027

- **Ticker:** `027`
- **Número:** `16`
- **Status:** `completed`
- **Depends on:** `027-15`

## Objetivo

Revalidar que remoção do Gate B não alterou Gate A e fechar a Spec 027 com
classificação factual.

## Escopo

- confirmar ausência de timeline 1 Hz em código, estado e UI;
- confirmar BPM raw elegível e mapeado para `HeartRateRecord`;
- confirmar `StepsCadenceRecord` derivado de intervalos D4;
- confirmar Gate A sem mudança de identidade, payload, retry, Daily ou crédito;
- executar lint e builds;
- validar visualmente detalhe de atividade sem timeline, se necessário;
- atualizar Spec, overview, baseline, ADR-026 e evidência de remoção.

Não executar suavização, não revalidar Gate B aposentado e não fazer nova
sincronização Health Connect/Health Sync/Strava sem pedido específico.

## Critérios de aceite

- Gate A classificado separadamente como `go_with_adjustments`;
- Gate B classificado como `retired_by_product_decision`, não como aprovado;
- BPM raw permanece preservado e não é substituído por série derivada;
- cadência D4 permanece preservada e independente de crédito;
- Health Connect continua write-only, sem readback declarado;
- `./gradlew lint assembleDebug assembleRelease` passa;
- histórico de 027-01 a 027-15 permanece auditável.

## Verificação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.

## Definição de pronto

Revisão independente aprovada, checklist coerente e Spec encerrada com Gate B
aposentado por decisão de produto.

## Riscos

Gate A mantém limitações já documentadas: mudança de dia/reset não exercitada,
Health Connect sem readback e Health Sync/Strava não repetido para as duas
atividades finais.

## Resultado desta execução

- **Status:** `completed`; Gate A revalidado como `go_with_adjustments` e Spec
  027 encerrada. Gate B classificado separadamente como
  `retired_by_product_decision`.
- **Arquivos alterados nesta subtarefa:**
  - `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-16-revalidar-gate-a-e-fechar-spec.md`;
  - `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-00-overview.md`;
  - `docs/specs/027-workout-steps-cadence-1hz-timeline.md`;
  - `docs/baseline/current-baseline.md`;
  - `docs/decisions/adr-026-workout-1hz-derived-timeline.md`;
  - `docs/integration/027-15-gate-b-removal.md`.
- **Decisões:** a remoção de `027-15` não alterou Gate A. BPM raw continua
  elegível para `HeartRateRecord`; cadência continua derivada de intervalos D4
  e publicada em `StepsCadenceRecord`. Identidades, payloads congelados, retry,
  Daily, crédito e independência da cadência permanecem inalterados. Não existe
  implementação, estado ou UI ativa de timeline 1 Hz.
- **Desvios:** nenhum. `027-11`, `027-13` e `027-14` permanecem desmarcadas
  como histórico `blocked`/cancelado; isso não reabre nem bloqueia o fechamento
  vigente, pois Gate B foi aposentado por decisão de produto.
- **Comandos executados:** `git diff --check` — passou, sem erros de whitespace;
  `./gradlew lint assembleDebug assembleRelease` — passou com `BUILD SUCCESSFUL`
  e configuração de build cache reutilizada. Testes automatizados não foram
  criados nem executados.
- **Evidências da auditoria:** diff de `027-15`, busca na árvore ativa sem uso
  de `Workout1HzTimeline`, `deriveBpm1Hz` ou `deriveCadence1Hz`, preservação de
  BPM raw, `HeartRateRecord`, `StepsCadenceRecord`, Daily, crédito, retry e
  contratos de projeção. Evidências físicas permanecem em `027-06` e `027-11`.
- **Sincronização física:** não houve nova sincronização física. Health Connect
  continua write-only; `SYNCED` representa aceite local da escrita, não readback.

## Riscos residuais e limitações de fechamento

- mudança de dia/reset não foi exercitada;
- Health Sync/Strava não foi repetido para as duas atividades finais;
- Health Connect não foi lido, conforme contrato write-only;
- limitações de interoperabilidade e diferenças de representação permanecem
  documentadas em `027-06` e `027-11`;
- screenshots citados na evidência `027-06` não estão presentes nesta árvore;
  a validação permanece auditável pelo registro textual sanitizado, com essa
  lacuna documental explícita;
- `027-11` permanece registro histórico `blocked` e `027-13`/`027-14`
  permanecem canceladas/desmarcadas; nenhuma delas é reaberta por este
  fechamento.
