# 029-05 — Implementar tendências e fórmulas agregadas

- **Ticker:** `029`
- **Número:** `05`
- **Status:** `completed`
- **Depends on:** `029-04`

## Objetivo

Adicionar tendências de métricas já disponíveis usando fórmulas matematicamente coerentes e populações comparáveis.

## Escopo

Implementar somente métricas aprovadas em `029-01`, podendo incluir:

- distância por atividade/bucket;
- duração;
- frequência cardíaca média com agregação definida;
- cadência D4 agregada para atividades elegíveis;
- pace ou velocidade apenas se a auditoria tiver definido fórmula/população segura.

## Regras matemáticas

- não calcular média de paces para obter pace agregado;
- quando pace agregado for elegível, derivar de `tempo elegível total / distância elegível total`;
- FC média deve usar fatos/amostras ou ponderação definida, não média ingênua entre médias de treinos;
- cadência agregada deve respeitar a granularidade/ponderação D4 aprovada;
- gaps e dados insuficientes permanecem ausentes.

## Critérios de aceite

1. Cada tendência implementada referencia fórmula documentada em `029-01`.
2. Nenhuma média de médias enganosa é usada.
3. Filtros por esporte preservam comparabilidade.
4. Gráficos usam contratos Champion da Spec 028.
5. Ausência de dados não cria linha sintética.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

## Implementação

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/EvolutionContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalEvolutionReadModel.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalEvolutionTrendAggregator.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalEvolutionVolumeAggregator.kt`;
- `docs/tasks/029-evolucao-recordes-pessoais/029-04-implementar-volume-consistencia.md`.

### Decisões e desvios

- Tendências diárias foram agregadas em
  `data/LocalEvolutionTrendAggregator.kt` e integradas ao snapshot completo por
  `EvolutionReadSnapshot.toEvolutionSnapshot()`.
- Distância e duração usam soma de fatos válidos; BPM usa média ponderada por
  `averageBpmSampleCount` e soma exata das amostras; cadência usa passos D4
  canônicos/duração somente em pares válidos e exclui bicicleta; pace/velocidade
  aparecem somente em filtro `ByType` de corrida, caminhada ou esteira e
  derivam dos totais agregados.
- Dias sem atividade permanecem `GAP`; atividade sem fato suficiente permanece
  `ABSENT`. Pace/velocidade consolidados são omitidos para evitar população não
  comparável.
- Decisão conservadora: pace/velocidade agregados usam somente atividades com
  distância e duração simultaneamente válidas; misturar populações parciais
  produziria razão semanticamente enganosa.
- Overflow de soma e resultado não finito seguem falha de dado local, coerente
  com o agregador de volume, em vez de emitir valor sintético.
- Atividade D4 válida com zero passos preserva duração no denominador e emite
  cadência factual `0`, sem promover ausência a valor positivo.

Não houve desvio dos requisitos. Pace e velocidade consolidados foram omitidos
por falta de população comparável, conforme auditoria `029-01`.

### Comandos executados e resultados

- `git diff --check` — passou.
- `./gradlew :app:compileDebugKotlin :app:compileReleaseKotlin` — `BUILD SUCCESSFUL`.
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`.
- Testes automatizados não foram criados nem executados, conforme guard rail.

### Evidências

- `EvolutionReadSnapshot.toEvolutionSnapshot()` agora expõe tendências diárias
  com contratos neutros `EvolutionMetricSeries`/`EvolutionSeriesPoint`.
- Distância/duração usam somas protegidas; BPM usa fatos na janela efetiva,
  soma de amostras e ponderação por quantidade; cadência usa passos e duração
  canônicos D4; pace/velocidade usam duração/distância agregadas.
- `GAP`, `ABSENT` e `NOT_APPLICABLE` preservam semântica de ausência, dado
  insuficiente e bicicleta sem cadência.
- Lint e builds debug/release concluíram com `BUILD SUCCESSFUL`.

### Riscos residuais

- Não há testes automatizados para DST, overflow extremo ou fixtures D4; guard
  rail da Spec 029 impede criar/executar testes sem solicitação explícita.
- O histórico completo continua carregado antes do filtro temporal; projeção
  limitada permanece otimização futura registrada em `029-02`.
- Conversão das séries para gráficos e integração visual pertencem a `029-08`.
