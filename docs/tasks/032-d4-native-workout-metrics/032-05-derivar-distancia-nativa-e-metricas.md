# 032-05 — Derivar distância nativa e métricas

- **Status:** pending
- **Depende de:** 032-04
- **Pode rodar em paralelo com:** 032-06 após contrato comum congelado

## Objetivo

Tornar a distância D4 nativa a base primária da derivação quando elegível,
preservando `T/N`, passos/cadência e fallback por comprimento de passo.

## Escopo

- introduzir provenance de distância;
- derivar distância por intervalo a partir da série nativa;
- recalcular velocidade, pace, acumulados e splits sobre duração efetiva;
- manter passos e cadência independentes da fonte de distância;
- usar `stepLength` somente como fallback explícito;
- preservar política própria de `CYCLING`;
- definir plausibilidade/fail-closed sem limiares inventados.

## Invariantes

```text
Σ intervalDistance = canonicalDistance
Σ effectiveDuration = workout duration
cumulativeDistance é monotônica
steps não mudam ao calibrar distância
```

## Validação

`./gradlew lint assembleDebug assembleRelease` e `git diff --check`.
