# 032-01 — Auditar e congelar layout D4 v4

- **Status:** pending
- **Depende de:** nenhuma
- **Bloqueia:** 032-02 e 032-03

## Objetivo

Consolidar em documento autoritativo o layout D3/D4 observado no APK, os dados
físicos validados e as fronteiras entre fato, derivação e diagnóstico antes de
mudar código.

## Entregáveis

- atualizar `docs/protocol/workouts-d3-d4.md` com header de 42 bytes e sample v4;
- registrar sem payload integral a atividade física de referência e invariantes;
- congelar unidades confirmadas, candidatas e desconhecidas;
- congelar regra de índice físico D3;
- congelar que `T/N` permanece autoritativo para timeline efetiva;
- mapear todos os consumidores de `rawMotionValue`, `rawDistanceValue`,
  `rawCaloriesValue`, `decodedSteps`, distância canônica e calibração.

## Decisões que devem sair fechadas

- `distanceRaw` do sample pode ser promovido a metros no runtime v4 ou precisa
  permanecer raw até 032-05;
- escala de `caloriesRaw`;
- campos persistentes mínimos para `sportValue`, passos, distância, calorias e
  pausa;
- comportamento em divergência header × soma de samples.

## Validação

Somente documentação/auditoria. `git diff --check`.
