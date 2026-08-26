# 029-01 — Auditar métricas, elegibilidade e pontos de persistência

- **Ticker:** `029`
- **Número:** `01`
- **Status:** `completed`

## Objetivo

Mapear os fatos disponíveis, as fórmulas agregáveis e todos os pontos que podem tornar uma atividade elegível ou alterar sua métrica final antes de implementar evolução e recordes.

## Resultado

Auditoria concluída e congelada em:

`docs/integration/029-01-evolution-records-audit.md`.

## Decisões principais

- a primeira versão da 029 usa somente atividades `WATCH_D3_D4`;
- `READY_TO_SYNC`, `SYNCING`, `SYNCED` e `SYNC_FAILED` são elegíveis quando a métrica específica for válida;
- `PENDING_REVIEW`, `PENDING_CALIBRATION`, `DISCARDED` e tipo `UNKNOWN` ficam fora;
- `PARTIAL` pode participar depois de revisão final, conforme política vigente da Spec 011;
- corrida/caminhada usam distância canônica ativa;
- esteira usa distância calibrada após calibração concluída;
- bicicleta usa distância manual opcional e nunca usa passos D4 como performance;
- `FREE_WORKOUT` não participa do recorde inicial de maior distância;
- duração usa `metrics.duration` e período usa `metrics.startedAt`, nunca data de importação;
- cadência agregada deriva de passos/tempo elegíveis, sem média simples de intervalos;
- pace agregado, quando usado, deriva de tempo total / distância total;
- recordes iniciais: maior distância, maior duração e mais passos conforme modalidade elegível;
- maior BPM e maior cadência não são recordes de performance;
- pontos de avaliação pós-save: `submitReview`, `calibrateImportedActivity`, `applyReferenceDistanceCalibration` e rederivação/reprocessamento explícito quando alterarem métrica final elegível;
- importação/reimportação e transições de Health Connect não disparam conquista;
- navegação planejada: `AppDestination.Evolution` como subárvore de Exercícios, sem nova aba inferior;
- nenhuma migration Room é necessária na arquitetura atual.

## Idempotência congelada

```text
baselineOther = melhor valor das demais atividades elegíveis
oldEligibleValue = valor anterior da própria atividade quando antes elegível no mesmo escopo
previousBest = max(baselineOther, oldEligibleValue)

newValue > previousBest -> conquista
```

Consequências:

- resave sem mudança não repete alerta;
- atividade que já era recordista e melhora pode gerar novo alerta;
- empate não gera conquista;
- atividade antiga compara contra todo o histórico atualmente conhecido.

## Verificação

Task documental/auditoria. Nenhum código funcional, schema, dependência ou teste automatizado foi alterado/executado.

## Saída

`029-02` está liberada para criar os contratos e o read model de evolução conforme a matriz registrada na auditoria.
