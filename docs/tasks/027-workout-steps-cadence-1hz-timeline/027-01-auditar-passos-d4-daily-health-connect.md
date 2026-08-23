# 027-01 — Auditar passos D4, Daily Summary e Health Connect

- **Ticker:** `027`
- **Número:** `01`
- **Status:** `pending`
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
