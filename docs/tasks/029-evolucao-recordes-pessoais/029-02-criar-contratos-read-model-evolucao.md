# 029-02 — Criar contratos e read model de evolução

- **Ticker:** `029`
- **Número:** `02`
- **Status:** `completed`
- **Depends on:** `029-01`

## Objetivo

Criar contratos neutros de apresentação/consulta para evolução esportiva sem acoplar a feature a entidades Room ou ao renderer de gráficos.

## Escopo

Definir contratos equivalentes para:

- `EvolutionPeriod`;
- filtro por `WorkoutType`/visão consolidada;
- `EvolutionVolumeSnapshot`;
- séries/tendências por métrica;
- comparação de períodos;
- `PersonalRecord` e `PersonalRecordsSnapshot`;
- estados loading/empty/failed da nova superfície.

Criar agregador/repository de leitura que consuma fatos locais já existentes e retorne snapshots imutáveis.

## Direção arquitetural

```text
repositories de fatos locais
        ↓
EvolutionReadModel
        ↓
contracts da feature
        ↓
ViewModel/UI
```

Tipos de Vico não entram nos contratos; adaptação para `ChampionChartSeries` fica na camada de apresentação.

## Guard rails

- evitar migration/schema novo nesta task;
- não persistir agregados derivados;
- não mover regras de domínio para Composables;
- não criar dependência de Health Connect read;
- não criar testes automatizados.

## Critérios de aceite

1. A feature possui contratos suficientes para volume, tendência, comparação e recordes.
2. O read model é recalculável a partir de fatos existentes.
3. Ausência e gaps permanecem representáveis.
4. Nenhum tipo Vico vaza para domínio/repository.
5. Nenhum schema Room é alterado sem necessidade comprovada.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

## Implementação

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/EvolutionContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalEvolutionReadModel.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`.

### Decisões e desvios

- contratos usam janelas civis semiabertas, filtros consolidados ou por tipo e
  ordenação determinística por `startedAt` e identificador;
- o read model lê somente atividades `WATCH_D3_D4` com tipo confirmado, parser
  `VALID`/`PARTIAL` e workflow elegível;
- ausência de distância, passos, duração ou métricas derivadas permanece
  representada como `null`, sem fabricar zero; distância factual de
  `FREE_WORKOUT` pode ser preservada para volume, mas recorde de distância
  continua sendo decisão do avaliador posterior;
- `averageBpmSampleCount` preserva base disponível para agregação posterior,
  sem implementar média de médias nesta subtarefa;
- cálculo de volume, tendências, comparação e recordistas all-history permanece
  nas subtarefas `029-03` a `029-07`; esta entrega fornece fatos normalizados e
  contratos imutáveis para esses agregadores;
- nenhuma entidade, DAO, migration, schema, Vico ou leitura do Health Connect
  foi adicionada.

### Comandos executados e resultados

- `git diff --check` — passou;
- `./gradlew app:compileDebugKotlin app:compileReleaseKotlin` — passou;
- `./gradlew lint assembleDebug assembleRelease` — passou;
- uma primeira execução da validação completa excedeu o limite do shell após
  concluir parte do build; a execução repetida concluiu com `BUILD SUCCESSFUL`;
- nenhum teste automatizado foi criado ou executado, conforme guard rail.

### Evidências

- compilação Kotlin debug/release concluída;
- lint debug/release concluído;
- assemble debug/release concluído;
- binding Hilt de `EvolutionReadModel` para `LocalEvolutionReadModel` validado
  durante compilação;
- nenhuma alteração de schema Room detectada.

### Riscos residuais

- o read model atual carrega atividades completas antes de filtrar o período;
  uma projeção limitada poderá ser necessária se o histórico crescer;
- recordes exigem leitura sem limite temporal, a ser implementada em `029-07`
  sem usar o snapshot limitado por período como baseline;
- agregação exata de frequência cardíaca entre atividades ainda depende de
  fórmula e população definidas em `029-05`;
- mapeamentos persistentes existentes podem descartar registros inválidos
  antes de o read model observá-los.
