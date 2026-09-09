# 032-06 — Implementar calibração de distância contextual

- **Status:** pending
- **Depende de:** 032-04
- **Pode rodar em paralelo com:** 032-05 após contrato comum congelado

## Objetivo

Substituir, para `d4-v4` elegível, a calibração principal por passada por uma
correção proporcional da distância nativa, mantendo histórico antigo intacto.

## Escopo

- persistir evidência manual `referenceDistance / watchDistance`;
- perfis independentes RUNNING/WALKING;
- agregado ponderado `Σreference / Σwatch`;
- provenance explícita no snapshot;
- rederivação explícita da atividade usada como referência;
- correção/remoção de evidência sem duplicação de peso;
- fallback para distância nativa sem fator (`1.0`) e depois step length se a série
  nativa for inelegível;
- preservar calibrações legacy de comprimento de passo.

## Decisões de UI para 032-07

Esta task deve entregar contratos de estado/ação sem redesenhar a tela. A UI final
será encaixada depois da integração com a Spec 031.

## Guard rails

- referência externa é manual e pode conter erro;
- nenhuma integração GPS/Adidas/Strava;
- `TREADMILL` não herda RUNNING automaticamente;
- `CYCLING` fora do aprendizado;
- nenhuma atividade `SYNCED` é alterada silenciosamente.

## Validação

`./gradlew lint assembleDebug assembleRelease` e `git diff --check`.
