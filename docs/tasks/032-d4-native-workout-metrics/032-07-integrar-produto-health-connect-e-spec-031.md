# 032-07 — Integrar produto, Health Connect e Spec 031

- **Status:** pending
- **Depende de:** 032-05 e 032-06

## Objetivo

Promover `d4-v4` ao fluxo efetivo e integrar repository, UI e Health Connect sobre
a baseline visual já estabilizada pela Spec 031.

## Pré-condição

Atualizar a branch 032 para uma `main` que contenha a Spec 031, ou registrar uma
baseline comum equivalente antes de tocar arquivos compartilhados.

## Escopo

- integrar `RoomRepositories.kt` e persistência transacional;
- tornar `d4-v4` parser efetivo para novas importações;
- encaixar novos estados/labels de distância e calibração em Exercícios sem
  reverter o redesign visual;
- preservar review-before-sync;
- atualizar projeções congeladas de DISTANCE/SPEED e consumidores relacionados;
- preservar STEPS/STEPS_CADENCE e créditos da Spec 027;
- atualizar baseline/documentação autoritativa.

## Arquivos de conflito esperado com 031

- `data/RoomRepositories.kt`;
- `feature/exercises/ExercisesScreen.kt`;
- contratos/read models eventualmente alterados pela 031.

Resolver conflitos semanticamente; nunca escolher versão inteira de um lado sem
auditar comportamento.

## Validação

`./gradlew lint assembleDebug assembleRelease` e `git diff --check`.
