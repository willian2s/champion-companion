# 029-06 — Implementar comparação entre períodos

- **Ticker:** `029`
- **Número:** `06`
- **Status:** `completed`
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

## Implementação

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalEvolutionComparisonAggregator.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalEvolutionVolumeAggregator.kt`.

### Decisões e desvios

- A comparação foi limitada às métricas de volume previstas no contrato:
  atividades, distância, duração, passos e dias ativos. Tendências não foram
  reinterpretadas nem receberam novo contrato de comparação.
- `EvolutionVolumeSnapshot.toPeriodComparison` delega validação de período,
  tamanho, não sobreposição, timezone e filtro para `EvolutionPeriodComparison`,
  que exige `previousQuery()` exata.
- Distância, duração e passos ausentes permanecem nulos em cada lado; contagem
  de atividades e dias ativos preserva zero factual de período vazio. Assim,
  período vazio não fabrica zero para métricas sem fato.
- Diferença percentual só é emitida com denominador anterior não-zero e
  resultado finito. Ausência em qualquer lado produz `UNAVAILABLE`; direção
  presente é somente numérica (`INCREASED`, `DECREASED` ou `STABLE`).
- `toEvolutionSnapshot(previous)` integra comparação com snapshots já
  agregados, sem rederivação de atividades, Room, schema, Health Connect, GPS
  ou UI.

### Comandos executados e resultados

- `git diff --check` — passou;
- `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin` — `BUILD SUCCESSFUL`;
- `./gradlew :app:lintDebug` — `BUILD SUCCESSFUL`;
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`;
- nenhum teste automatizado foi criado ou executado, conforme guard rail.

### Evidências

- `EvolutionPeriodComparison` impede filtros divergentes e consultas que não
  sejam a janela imediatamente anterior de mesmo tamanho;
- métricas comparadas são reconciliadas diretamente com campos de cada
  `EvolutionVolumeSnapshot`;
- períodos atual/anterior vazios mantêm contagens factuais e ausência explícita
  das métricas sem valor;
- percentual infinito/NaN é omitido de forma segura.

### Riscos residuais

- O read model continua emitindo estados `Empty` separadamente quando uma
  consulta não possui atividades; a composição desses estados com a UI e a
  apresentação textual de comparação pertence à subtarefa `029-08`.
- Tendências não possuem comparação nesta subtarefa por ausência de contrato
  específico; a comparação permanece restrita ao volume elegível.
