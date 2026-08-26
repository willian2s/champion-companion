# 029-02 — Criar contratos e read model de evolução

- **Ticker:** `029`
- **Número:** `02`
- **Status:** `pending`
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
