# 029-04 — Implementar volume e consistência

- **Ticker:** `029`
- **Número:** `04`
- **Status:** `pending`
- **Depends on:** `029-03`

## Objetivo

Entregar os agregados básicos de treinamento por período e filtro sem inferência fisiológica.

## Escopo

Calcular, conforme elegibilidade definida em `029-01`:

- quantidade de atividades;
- distância total;
- duração total;
- passos em atividades;
- dias ativos;
- série de volume por bucket temporal apropriado.

## Regras

- distância soma somente distâncias finais semanticamente válidas;
- passos de `CYCLING` não entram como volume esportivo de passos;
- duração usa janela oficial/final da atividade;
- múltiplas atividades no mesmo dia contam como um único dia ativo;
- valores ausentes permanecem ausentes quando zero mudaria a semântica.

## Apresentação

Preparar dados para cards de resumo e gráficos Champion da Spec 028, sem Vico direto na feature.

## Critérios de aceite

1. Totais reconciliam com as atividades elegíveis do período.
2. Dias ativos não duplicam por múltiplos treinos no mesmo dia.
3. `CYCLING` não ganha passos de performance derivados do D4.
4. Distâncias incompatíveis/ausentes não são inventadas.
5. Série temporal respeita filtros e períodos.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```
