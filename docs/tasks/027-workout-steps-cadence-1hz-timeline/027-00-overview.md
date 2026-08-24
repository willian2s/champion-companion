# 027 — Passos, cadência e timeline 1 Hz

- **Status geral:** `in_progress`
- **Spec:** [027-workout-steps-cadence-1hz-timeline.md](../../specs/027-workout-steps-cadence-1hz-timeline.md)
- **Progresso:** `7/11` subtarefas concluídas

## Objetivo

Executar a Spec 027 em dois gates sequenciais:

- **Gate A:** publicar passos e cadência de atividades no Health Connect sem
  inflar o acumulado diário;
- **Gate B:** produzir timeline derivada comum de 1 Hz para BPM e cadência.

## Pré-condições

Antes de executar qualquer subtarefa:

1. ler `AGENTS.md`;
2. ler `docs/baseline/current-baseline.md`;
3. ler a Spec 027;
4. para o Gate A, ler a Spec 016 e ADRs vigentes de Health Connect;
5. auditar o código real da branch antes de assumir schema, identidades ou APIs.

## Invariantes e guard rails

- `DailySummary.steps` permanece cumulativo e autoritativo;
- passos publicados pela atividade não podem ser contados novamente pelo Daily;
- crédito só existe após aceite remoto do `StepsRecord` da atividade;
- retry não duplica record, crédito ou contagem;
- atividade já coberta por Daily sincronizado não publica outro `StepsRecord`;
- `StepsCadenceRecord` não participa da contabilidade de passos;
- dados D4 de origem permanecem intactos;
- gaps estruturais permanecem gaps;
- sync continua manual, foreground e write-only;
- nenhuma subtarefa reativa treino phone, GPS, mapa ou rota;
- não criar nem executar testes automatizados sem pedido explícito.

## Checklist

- [x] [027-01-auditar-passos-d4-daily-health-connect.md](027-01-auditar-passos-d4-daily-health-connect.md)
- [x] [027-02-modelar-passos-cadencia-e-creditos.md](027-02-modelar-passos-cadencia-e-creditos.md)
- [x] [027-03-publicar-passos-da-atividade.md](027-03-publicar-passos-da-atividade.md)
- [x] [027-04-descontar-creditos-no-daily-summary.md](027-04-descontar-creditos-no-daily-summary.md)
- [x] [027-05-publicar-cadencia-por-intervalos-d4.md](027-05-publicar-cadencia-por-intervalos-d4.md)
- [x] [027-06-validar-health-sync-strava-e-total-diario.md](027-06-validar-health-sync-strava-e-total-diario.md)
- [x] [027-07-definir-contrato-timeline-1hz.md](027-07-definir-contrato-timeline-1hz.md)
- [ ] [027-08-implementar-bpm-1hz.md](027-08-implementar-bpm-1hz.md)
- [ ] [027-09-implementar-cadencia-1hz.md](027-09-implementar-cadencia-1hz.md)
- [ ] [027-10-integrar-timeline-e-avaliar-cadencia-hc.md](027-10-integrar-timeline-e-avaliar-cadencia-hc.md)
- [ ] [027-11-validar-e-fechar-spec.md](027-11-validar-e-fechar-spec.md)

## Dependências

```text
027-01 → 027-02 → 027-03 → 027-04 → 027-05 → 027-06
                                                    ↓
027-07 → 027-08 → 027-09 → 027-10 → 027-11
```

O Gate B só inicia após o Gate A ser fechado sem risco de duplicidade ou
subcontagem de passos.

## Verificação

Para subtarefas com código, schema ou Gradle:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Para validações físicas, registrar somente evidência observada e sanitizada.

## Observações

- Spec, pasta, overview e subtarefas usam ticker `027`.
- Planejamento foi normalizado para uma única seção `## Checklist`, com um item
  por subtarefa.
- `027-01` confirmou que Daily já usa payload incremental e checkpoint de total
  enfileirado, mas não há passos/cadência de atividade nem crédito pós-aceite.
- `ADR-025` fechou cobertura conservadora por janelas locais próprias, residual
  fail-closed e ledger Room separado. Nenhuma escrita remota nova foi feita.
- `027-02` modelou contratos, payload congelado schema 3, ledger Room e
  migration `13 -> 14`, sem iniciar escrita remota, crédito no Daily ou
  timeline 1 Hz.
- `027-03` publicou `StepsRecord` de atividade com elegibilidade fail-closed,
  cobertura Daily comprovada, crédito pós-aceite idempotente e retry por
  identidade/payload congelados. Desconto no Daily e cadência permanecem fora
  desta subtarefa.
- A verificação combinada de lint/build teve falha transitória do lint por
  arquivo Hilt gerado ausente durante análise paralela; lint isolado e builds
  separados passaram. Revisão independente final aprovou a subtarefa sem
  bloqueadores.
- `027-04` integrou residual Daily com créditos aceitos, checkpoint técnico e
  reservas pós-aceite; payload residual foi versionado como
  `RESIDUAL_INCREMENTAL`, sem reinterpretar incrementos antigos. Migration
  aditiva `14 -> 15` e schema 15 foram atualizados.
- `027-05` ativou a criação normal da projeção filha `StepsCadenceSeries` para
  snapshots D4 elegíveis com intervalos canônicos; publicação permanece
  independente de passos/créditos e usa mapper, payload congelado, midpoint,
  retry e permissão já existentes.
- Durante `027-06`, a primeira APK física crashou no sync; a APK defensiva
  seguinte revelou regressão de `DailySummary + STEPS` no calendário e falha de
  construção dos novos payloads de projeção antes do `.copy()`. As correções
  atuais restauram Daily sem child, congelam `WorkoutSteps` e
  `StepsCadenceSeries` atomicamente, preservam falhas reais de elegibilidade e
  mantêm a proteção no boundary do mapper. O run CI `32680327755` passou
  `./gradlew lint assembleDebug assembleRelease` e publicou novo APK debug. Esse
  registro descreve estado anterior ao reteste físico final.
- Validação física Health Connect/Health Sync/Strava permanece em `027-06`;
  timeline 1 Hz não foi iniciada.
- Revisão independente de `027-05` aprovada sem bloqueadores; permanecem apenas
  riscos de interoperabilidade física e ausência de testes automatizados.
- `027-06` reproduziu atividade → Daily sem inflação: passos e cadência foram
  aceitos, crédito de 16 passos foi consumido uma vez e checkpoint terminou em
  16. Strava exibiu atividade, gráfico de cadência, média e máxima em `16 ppm`;
  o valor diferiu da representação observada no Health Connect (`2,308
  passos/min`, vírgula decimal compatível com 16 passos em 6:56). Daily →
  atividade também passou, com passos bloqueados e cadência independente; Gate A
  fechou com ajustes.
- No reteste Daily → atividade, Daily chegou a `201` antes da atividade de `132`
  passos; crédito ficou `BLOCKED/ALREADY_COVERED`, `STEPS_CADENCE` foi aceito
  independentemente e demais filhos terminaram sincronizados. Gate A fechou
  como `go_with_adjustments`; mudança de dia não foi exercitada.
- `027-07` concluiu o contrato documental do Gate B em `ADR-026` e integrou a
  decisão à Spec 027 antes de `027-08`: camadas semânticas, identidade
  `watch-d3-d4-1hz-v1`, grade `[startedAt, endedAt)`, políticas versionadas de
  BPM/cadência, continuidade por índice, ausência por canal e cache somente em
  memória.
- A timeline 1 Hz ainda não foi implementada nem promovida ao Health Connect;
  `027-08` é a próxima subtarefa elegível e decisões/validações posteriores
  continuam bloqueadas até suas dependências.
