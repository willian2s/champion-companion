# 032-05 — Derivar distância nativa e métricas

- **Status:** completed
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

## Execução

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`

### Decisões e desvios

- A elegibilidade nativa exige parser v4 válido, tipo `RUNNING`/`WALKING`, header estruturalmente válido, série `T/N` elegível e fechamento dos totais de passos, distância, calorias e `sportValue`; a série nunca é preenchida pelo header.
- `WATCH_NATIVE` e `STEP_LENGTH_FALLBACK` foram adicionados à provenance persistida existente em `derivationMethod`. `CYCLING`, `TREADMILL`, `FREE_WORKOUT` e `UNKNOWN` não promovem distância nativa.
- `DerivedWorkoutInterval` recebe distância canônica resolvida, mantendo cadência baseada em passos e velocidade, pace, acumulados e splits baseados na distância e duração efetivas.
- Room permanece em v16. Para snapshots nativos, colunas v16 não nulas de comprimento de passo recebem sentinel interno `0.0`/`MANUAL`; o domínio expõe ausência e a provenance `WATCH_NATIVE` impede uso como fallback.
- Runtime de importação permanece `d4-v3`; promoção do parser v4 e integração Health Connect permanecem em 032-07.

### Comandos executados

- `./gradlew :app:compileDebugKotlin`
- `./gradlew lint assembleDebug assembleRelease`
- `git diff --check`

### Resultados e evidências

- Compilação Kotlin debug: passou.
- `lint`, `assembleDebug` e `assembleRelease`: passaram.
- `git diff --check`: passou.
- Revisão independente: aprovada com ressalvas não bloqueadoras; integração de provenance nativa no Health Connect permanece em 032-07.
- Nenhum teste automatizado foi criado ou executado, conforme AGENTS.md.

### Riscos residuais

- Validação física da variação de intensidade e comparação com referência externa permanecem em 032-08.
- `HealthRecordMapper` ainda usa provenance de comprimento de passo para distância/velocidade; adaptação de `WATCH_NATIVE` pertence à integração 032-07.
- Não há testes automatizados novos para round-trip nativo, conforme regra do repositório.
