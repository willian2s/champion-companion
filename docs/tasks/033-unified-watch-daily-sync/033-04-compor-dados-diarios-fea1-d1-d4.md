# 033-04 — Compor dados diários FEA1 + D1 + D4

- **Status:** pending
- **Depende de:** 033-02 e 033-03

## Objetivo

Criar read model diário medido sem dupla contagem entre D1 e D4.

## Escopo

- FEA1 como total diário atual de passos;
- D1 fora de workouts;
- D4 dentro de workouts;
- supressão inicial do bucket D1 que intersecta qualquer janela D4;
- passos, distância, BPM e calorias locais conforme elegibilidade;
- métricas de cobertura/completude para não esconder lacunas;
- distância diária sem `steps × stepLength` quando fatos nativos existirem;
- consultas por dia/período para Home, Saúde e Calendário.

## Guard rails

- não dividir bucket D1 proporcionalmente;
- não usar totais de header D4 como série;
- não inventar calorias HC;
- histórico legacy continua legível.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```
