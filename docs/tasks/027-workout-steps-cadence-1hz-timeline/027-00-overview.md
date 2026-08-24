# 027 — Passos, cadência e timeline 1 Hz

- **Status geral:** `completed`
- **Spec:** [027-workout-steps-cadence-1hz-timeline.md](../../specs/027-workout-steps-cadence-1hz-timeline.md)
- **Progresso:** `13/16` subtarefas concluídas

## Objetivo

Executar a Spec 027 em dois gates sequenciais:

- **Gate A:** publicar passos e cadência de atividades no Health Connect sem
  inflar o acumulado diário;
- **Gate B:** aposentado por decisão de produto; não faz parte do fluxo vigente.

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
- [x] [027-08-implementar-bpm-1hz.md](027-08-implementar-bpm-1hz.md)
- [x] [027-09-implementar-cadencia-1hz.md](027-09-implementar-cadencia-1hz.md)
- [x] [027-10-integrar-timeline-e-avaliar-cadencia-hc.md](027-10-integrar-timeline-e-avaliar-cadencia-hc.md)
- [ ] [027-11-validar-e-fechar-spec.md](027-11-validar-e-fechar-spec.md)
- [x] [027-12-corrigir-bloqueadores-timeline-1hz.md](027-12-corrigir-bloqueadores-timeline-1hz.md)
- [ ] [027-13-calibrar-suavizacao-bpm-1hz.md](027-13-calibrar-suavizacao-bpm-1hz.md)
- [ ] [027-14-revalidar-e-fechar-spec.md](027-14-revalidar-e-fechar-spec.md)
- [x] [027-15-remover-gate-b-timeline-1hz.md](027-15-remover-gate-b-timeline-1hz.md)
- [x] [027-16-revalidar-gate-a-e-fechar-spec.md](027-16-revalidar-gate-a-e-fechar-spec.md)

## Dependências

```text
027-01 → 027-02 → 027-03 → 027-04 → 027-05 → 027-06
                                                    ↓
027-07 → 027-08 → 027-09 → 027-10 ──→ 027-12 ──→ 027-15 ──→ 027-16
                              027-11 ──→ (achados)
                              027-13/14 canceladas por decisão de produto
```

Gate A permanece vigente e independente. Gate B foi removido por `027-15`;
`027-16` revalidou Gate A e fechou a Spec.

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
- Validação física Health Connect/Health Sync/Strava permanece registrada em
  `027-06`; `027-08` a `027-10` implementaram e integraram a timeline 1 Hz
  local. Validação física final foi executada em `027-11`; naquele registro,
  fechamento documental permaneceu bloqueado por lacunas identificadas na
  implementação da timeline 1 Hz.
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
- `027-08` implementou BPM 1 Hz sob demanda em `core/model`, sem persistência,
  UI ou Health Connect. A série preserva SOURCE, expõe identidade/fingerprint,
  Hampel, EMA, continuidade defensiva, gaps e provenance da grade.
- `027-09` implementou cadência 1 Hz sob demanda sobre intervalos normalizados,
  com Hampel, EMA por midpoint, continuidade/gaps defensivos, ausência por
  canal e identidade/fingerprint versionados. Parser parcial, `CYCLING`,
  intervalos sem passos decodificados e janelas sem adjacência não geram
  observações sintéticas.
- Revisão independente anterior de `027-09` aprovou a implementação, mas a
  revisão final de `027-11` identificou lacunas ainda não cobertas: intervalo
  estrutural inválido falha a série de cadência inteira em vez de preservar gap,
  BPM aceita parser `PARTIAL` e `fallbackGapSeconds` de BPM não limita `90 s`.
  `027-12` corrigiu esses pontos; o Gate B foi posteriormente aposentado por
  decisão de produto.
- `027-10` integrou timeline comum derivada ao detalhe da atividade, preservando
  gráfico BPM raw, métricas históricas, gaps e provenance interpolada. Screenshot
  sanitizado está em `docs/integration/027-10-detail-timeline.png`.
- A decisão de `027-10` é manter `StepsCadenceRecord` por buckets D4: Gate A
  demonstrou aceite e cadência no Health Sync/Strava, mas não comprovou benefício
  concreto, consumidor real ou estabilidade de série 1 Hz interpolada. Nenhuma
  sincronização adicional com Health Connect foi feita durante a validação visual.
  `ADR-026` registra decisão autoritativa e requisitos para eventual promoção futura.
- A validação de Gate A já cobre Health Connect e Health Sync/Strava, com
  classificação `go_with_adjustments`. A tentativa de importar as duas
  atividades relatadas no relógio em `027-11` terminou com `Operação Bluetooth
  expirou; foi confirmado que o relógio estava descarregado. Após recarga, UI
  observou `Inseridos 2, duplicados 0, inválidos 0, ausentes 0`; a atividade
  longa (`32:17`, `4.605` passos) e a curta (`05:30`, `639` passos) foram
  sincronizadas individualmente e terminaram `SYNCED`. Health Connect não foi
  lido; evidência em `docs/integration/027-11-final-validation.md`. Mudança de
  dia/reset e repetição Health Sync/Strava dessas duas atividades permanecem
  limitações documentadas. Naquele registro, o fechamento aguardava remoção do
  Gate B e revalidação de Gate A em `027-15`/`027-16`; o fechamento atual está
  registrado abaixo.
- `027-12` corrigiu continuidade estrutural, parser BPM parcial e limite de
  fallback. `027-13` e `027-14` foram canceladas por decisão de produto. `027-15`
  removeu código/UI da timeline 1 Hz sem tocar BPM raw, cadência D4 ou Health
  Connect; `027-16` concluiu revalidação e fechamento. Histórico documental foi
  preservado.
- `027-16` revalidou Gate A como `go_with_adjustments` e classificou Gate B como
  `retired_by_product_decision`. A auditoria encontrou ausência de implementação,
  estado e UI ativos da timeline 1 Hz; BPM raw segue `HeartRateRecord` e
  cadência D4 segue `StepsCadenceRecord`. Identidades, payloads, retry, Daily e
  crédito de Gate A permanecem inalterados. Não houve nova sincronização física;
  Health Connect continua write-only. Limitações de mudança de dia/reset,
  readback e repetição Health Sync/Strava permanecem registradas.
- Verificação final de `027-16`: `git diff --check` passou e
  `./gradlew lint assembleDebug assembleRelease` terminou com `BUILD SUCCESSFUL`.
  Nenhum teste automatizado foi criado ou executado.
