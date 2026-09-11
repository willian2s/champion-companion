# 032-07 — Integrar produto, Health Connect e baseline pós-031

- **Status:** pending
- **Depende de:** 032-05 e 032-06

## Objetivo

Promover `d4-v4` ao fluxo efetivo e integrar repository, UI e Health Connect sobre
a baseline visual já estabilizada e incorporada pela Spec 031.

## Pré-condição

**Satisfeita em 2026-09-10.** A branch 032 já foi atualizada sobre uma `main` que
contém a Spec 031. A reauditoria `032-01` confirmou `behind_by = 0` e merge-base
`6025fbaaf119727fbb95c2f51e6f16946ef646ea`.

Referência:

`docs/integration/032-01-post-031-reaudit.md`.

Não realizar novo merge específico da 031 nesta task; trabalhar sobre a baseline
pós-031 já presente e preservar seus read models, navegação e desenho visual.

## Escopo

- integrar `RoomRepositories.kt` e persistência transacional;
- tornar `d4-v4` parser efetivo para novas importações;
- encaixar novos estados/labels de distância e calibração em Exercícios sem
  reverter o redesign visual;
- preservar review-before-sync;
- preservar a participação de `WATCH_WORKOUT_ACTIVITY` nos históricos de Saúde;
- atualizar projeções congeladas de DISTANCE/SPEED e consumidores relacionados;
- preservar STEPS/STEPS_CADENCE e créditos da Spec 027;
- atualizar baseline/documentação autoritativa.

## Arquivos compartilhados relevantes

- `data/RoomRepositories.kt`;
- `feature/exercises/ExercisesScreen.kt`;
- contratos/read models de Saúde/Calendário que consomem métricas canônicas e FC de workout.

Esses arquivos não são mais pontos de conflito entre branches 031/032. Qualquer
mudança deve preservar semanticamente o baseline pós-031 e manter offsets/packing
D4 confinados às camadas de protocolo/adapters apropriadas.

## Validação

`./gradlew lint assembleDebug assembleRelease` e `git diff --check`.
