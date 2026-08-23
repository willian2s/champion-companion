# 027-01 — Auditar passos D4, Daily Summary e Health Connect

- **Ticker:** `027`
- **Número:** `01`
- **Status:** `completed`
- **Depends on:** nenhuma

## Objetivo

Mapear o estado real antes de qualquer mudança de schema ou comportamento remoto e fechar a decisão arquitetural da Spec 027.

## Ler antes

- `AGENTS.md`
- `docs/baseline/current-baseline.md`
- `docs/specs/027-workout-steps-cadence-1hz-timeline.md`
- `docs/specs/016-health-connect-incremental-daily-steps.md`
- ADRs vigentes de Health Connect/projeções filhas
- protocolo/parser D3/D4 relevante

## Auditar

### Daily Summary

- estrutura real de `DailyStepsSyncPayload`;
- checkpoint de último total enfileirado;
- `startTime`, `endTime`, fuso e ordinal;
- transação de criação do incremento;
- ponte legada;
- retry;
- reconciliação de backlog;
- quando o checkpoint avança em relação ao efeito remoto.

### WorkoutActivity/D4

- total de passos autoritativo da atividade;
- passos por intervalo e duração normalizada;
- comportamento de intervalos ausentes/parciais;
- atividades que cruzam meia-noite;
- reimportação/reprocessamento e preservação de identidade;
- tipos de treino nos quais passos são semanticamente aplicáveis.

### Health Connect

- `HealthProjectionChildKey` atual;
- `HealthSyncRecordType` e permissões;
- payload congelado;
- identities/versions;
- mapper/gateway;
- ordenação de operações;
- comportamento de falha parcial;
- ponto seguro para ativar crédito somente após aceite remoto.

### Cobertura temporal

Determinar como provar localmente se uma atividade já está coberta por um `StepsRecord` diário sincronizado.

Fechar política para residual diário sem criar sobreposição de passos do mesmo produtor. Como o produto prioriza acumulado diário e não granularidade intradiária, a solução pode usar uma janela representacional aproximada, mas não pode:

- duplicar passos;
- produzir total negativo;
- inventar passos adicionais;
- atravessar dia local indevidamente;
- violar janela temporal válida.

Se não houver representação segura, comportamento é fail-closed: não promover passos da atividade e preservar Daily como fonte.

## Decisão duradoura

Criar `ADR-025` se ainda for o próximo número livre. Registrar:

- precedência de passos da atividade sobre futuro incremento diário ainda não publicado;
- crédito somente pós-aceite remoto;
- ordem inversa sem reescrita automática do histórico remoto;
- independência de `StepsCadenceRecord`;
- prioridade do total diário sobre precisão intradiária residual;
- política de persistência/Room escolhida.

## Entregáveis

- `docs/integration/027-01-steps-sync-audit.md`;
- ADR correspondente;
- atualização de baseline/AGENTS se a decisão mudar regra operacional de execução;
- overview atualizado com achados factuais.

## Fora de escopo

- escrever `StepsRecord` novo;
- migration sem necessidade comprovada;
- timeline 1 Hz;
- mudanças visuais;
- GPS/phone workout.

## Aceite

- cada premissa da Spec 027 está confirmada ou corrigida contra o código real;
- política de cobertura/residual está fechada;
- nenhuma escrita remota nova é introduzida;
- documentação não promete comportamento ainda não implementado.

## Resultado da execução

- **Status:** `completed`
- **Arquivos alterados:**
  - `docs/integration/027-01-steps-sync-audit.md`;
  - `docs/decisions/adr-025-workout-steps-credit-and-daily-coverage.md`;
  - `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-01-auditar-passos-d4-daily-health-connect.md`;
  - `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-00-overview.md`.
- **Decisões:** política fail-closed para cobertura/residual; crédito somente
  pós-aceite; ledger Room separado; atividade cruzando data local bloqueada;
  cadência independente.
- **Desvios:** nenhum. Não houve alteração de código, schema, migration,
  permissões ou escrita remota.
- **Comandos executados:** `git status --short`; buscas/leitura dirigida de
  Spec 016, Spec 027, baseline, ADRs 012/015/016/018/019 e código Room/D4/
  Health Connect; `git diff --check`; `git diff --no-index --check /dev/null`
  para cada arquivo novo.
- **Resultados e evidências:** auditoria documental concluída; verificações
  `git diff --check` passaram para arquivos rastreados e novos; revisão
  independente corrigiu conflito ADR-012/ADR-025, citações e cobertura legada;
  nenhum teste automatizado foi criado ou executado conforme AGENTS.md e Spec
027. A evidência detalhada está em
  `docs/integration/027-01-steps-sync-audit.md`.
- **Revisão independente:** aprovada com ressalvas; nenhum bloqueador restante.
  Atomicidade real entre aceite, crédito e Daily fica para `027-02` a `027-04`.
- **Riscos residuais:** gateway sem readback; janela diária conservadora pode
  bloquear atividade realmente não coberta; residual inseguro pode manter
  checkpoint parado; compatibilidade Health Sync/Strava para cadência ainda não
  foi validada; divergência entre total do pacote e soma D4 permanece sem
  heurística.
