# 027-07 — Definir contrato e versionamento da timeline 1 Hz

- **Ticker:** `027`
- **Número:** `07`
- **Status:** `completed`
- **Depends on:** `027-06`

## Objetivo

Formalizar a camada derivada temporal antes de implementar algoritmos de BPM/cadência.

## Pré-condição

Gate A (`027-01` a `027-06`) fechado como `go_with_adjustments`: integridade
de passos passou sem inflação nos cenários exercitados localmente e no Health
Connect. Health Sync/Strava preservou as atividades e exibiu cadência, mas o
total de passos não ficou visível no Strava. Mudança de dia não foi exercitada
e permanece risco residual.

## Contrato

Distinguir explicitamente:

```text
SOURCE
DERIVED
RESAMPLED_1HZ
```

SOURCE continua sendo a informação persistida/interpretada do D4.

DERIVED aplica tratamento somente sobre observações suportadas pela fonte.

RESAMPLED_1HZ cria uma grade de um segundo para consumo analítico/visual.

## Definir

- versão do algoritmo;
- parâmetros versionados de BPM;
- parâmetros versionados de cadência;
- política de outlier;
- política de gaps;
- convenção de timestamp;
- arredondamento para BPM inteiro quando necessário;
- representação de ausência em segundos sem suporte;
- regras para início/fim da sessão;
- reprocessamento futuro;
- se a série deve ser persistida ou calculada sob demanda/cacheada.

## Autoridade

A timeline 1 Hz não substitui:

- amostras D4 persistidas;
- intervalos D4;
- totais da atividade;
- estatísticas autoritativas existentes.

Qualquer uso da série interpolada para média/mínimo/máximo exige decisão explícita posterior.

## Gaps

Não interpolar através de bucket estruturalmente ausente.

A política deve preferir índices de origem/continuidade semântica a um timeout arbitrário quando ambos estiverem disponíveis.

## Persistência

Se houver nova entidade/cache:

- migration aditiva;
- vínculo com `activityId` e versão da derivação;
- regeneração determinística;
- exclusão/reprocessamento sem afetar SOURCE.

## Entregável

Contrato final registrado antes de `027-08`.

## Arquivos alterados

- `docs/decisions/adr-026-workout-1hz-derived-timeline.md`;
- `docs/specs/027-workout-steps-cadence-1hz-timeline.md`;
- `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-07-definir-contrato-timeline-1hz.md`;
- `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-00-overview.md`.

## Decisões e desvios

- contrato fechado com camadas `SOURCE`, `DERIVED` e `RESAMPLED_1HZ`, sem
  alterar autoridade D4, totais ou estatísticas existentes;
- versionamento 1 Hz separado como `watch-d3-d4-1hz-v1`, com parâmetros
  `workout-1hz-params-v1`, preservando `d4-v3`, `watch-d3-d4-v2`,
  `watch-d3-d4-v3` e snapshots anteriores;
- grade absoluta `[startedAt, endedAt)` com offset inteiro, bucket final parcial,
  ausência por canal e provenance explícita;
- BPM e cadência receberam políticas versionadas de faixa, Hampel, EMA temporal,
  segmentos, gaps e interpolação, com conjunto `workout-1hz-params-v1`, regras
  determinísticas de MAD/EMA, fingerprint SHA-256 canônico e arredondamento BPM
  somente no boundary;
  `CYCLING` ficou fora da cadência por passos;
- não houve desvio funcional: persistência permanece fora desta subtarefa;
  cálculo futuro será sob demanda sobre snapshot imutável, com cache apenas em
  memória e reprocessamento explícito.

## Validação e evidências

- Comandos executados: `git diff --check`; validação estrutural do overview com
  `rg` para seção, itens e subtarefas.
- Resultado: passou, sem whitespace errors.
- Evidência: atualização documental limitada à Spec 027, a esta task e ao
  overview; nenhum arquivo de produção, teste ou Gradle foi alterado.
- A evidência física de Gate A já está registrada em
  `docs/integration/027-06-health-sync-strava-validation.md`: `StepsRecord`,
  `StepsCadenceRecord`, crédito, checkpoint e cenário inverso foram observados
  no Health Connect/estado local; Health Sync/Strava exibiu as atividades e a
  cadência, mas não exibiu o total de passos na tela observada. Nenhuma
  sincronização adicional é necessária para fechar o contrato desta subtarefa.
- As duas atividades atualmente disponíveis no relógio não foram usadas como
  evidência desta subtarefa documental. Eventual importação e validação final
  pertencem exclusivamente à `027-11`.
- Revisão independente final aprovou o diff documental; manteve como ressalvas
  a validação final pendente em `027-11` e a referência histórica a screenshots
  não presentes no workspace no relatório de `027-06`.

## Riscos residuais e bloqueios

- parâmetros iniciais e comportamento físico da timeline ainda dependem da
  validação final em `027-11`;
- sessão com `endedAt <= startedAt` falha fechada e não gera buckets;
- `027-10` decidiu manter `StepsCadenceRecord` por buckets D4; promoção de
  cadência 1 Hz ao Health Connect não foi autorizada;
- fallback temporal de `90 s` não substitui prova estrutural por índice;
- o relatório de `027-06` referencia screenshots em
  `docs/tasks/027-workout-steps-cadence-1hz-timeline/antes-daily/`, diretório
  não encontrado no workspace atual.

Próxima subtarefa elegível: `027-11`.

## Fora de escopo

- implementar EMA/interpolação;
- mudanças Health Connect;
- mudanças visuais.
