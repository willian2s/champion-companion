# 029-06 — Implementar comparação entre períodos

- **Ticker:** `029`
- **Número:** `06`
- **Status:** `pending`
- **Depends on:** `029-05`

## Objetivo

Comparar o período selecionado com a janela imediatamente anterior de mesmo tamanho, usando a mesma população e as mesmas fórmulas.

## Escopo

Para métricas elegíveis, produzir:

- valor atual;
- valor anterior;
- diferença absoluta;
- diferença percentual quando matematicamente válida;
- direção descritiva (`aumentou`, `reduziu`, `estável`) sem interpretar qualidade fisiológica.

## Regras

- denominador anterior zero não gera percentual;
- ausência em qualquer lado deve degradar para estado textual seguro;
- a comparação respeita o mesmo filtro de esporte;
- métricas em que “mais” não significa “melhor” não recebem linguagem de ganho/perda de performance.

## Critérios de aceite

1. Janelas comparadas têm o mesmo tamanho e não se sobrepõem.
2. Percentuais nunca produzem infinito/NaN.
3. Comparação não atribui melhora/piora fisiológica.
4. Valores reconciliam com os snapshots de cada período.
5. Estados vazios são apresentados explicitamente.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```
