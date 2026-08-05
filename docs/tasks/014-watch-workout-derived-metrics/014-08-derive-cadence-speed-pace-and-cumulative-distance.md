# 014-08 — Derivar cadência, velocidade, ritmo e acumulado

- **Ticker:** `014`
- **Número:** `08`
- **Status:** pending

## Objetivo e resultado esperado

Completar a série V1 sem suavização usando exclusivamente a distância canônica e
a duração efetiva.

Resultado: métricas totais/temporais e acumulado consistentes, com zero passos,
ausências e plausibilidade tratados explicitamente.

## Dependências

- `014-07` concluída.

## Arquivos permitidos

- políticas/contratos derivados em `core/model`;
- mappers/repository de snapshots;
- read models estritamente necessários;
- overview.

## Arquivos proibidos

- parser D4, perfil, UI final, splits e Health Connect;
- Gradle, manifesto, migrations e schemas;
- sobrescrita de dados raw/measured.

## Escopo

```text
cadência = passos ÷ duração em minutos
velocidade = distância canônica ÷ duração
ritmo = duração ÷ distância canônica em quilômetros
```

- distância acumulada soma deltas canônicos em ordem estável;
- total da atividade usa a distância canônica, nunca o total agregado estimado;
- zero passos produz distância, cadência e velocidade zero e ritmo ausente;
- série inelegível mantém métricas dependentes ausentes;
- plausibilidade é propagada, não corrigida;
- nenhuma média de médias.

## Fora do escopo

- suavização/interpolação de velocidade;
- zonas, VO2, calorias ou passada variável;
- splits de quilômetro;
- timestamp de `SpeedRecord`, já decidido para o ponto médio mas implementado em
  `014-13`;
- testes automatizados.

## Passos de implementação

1. Calcular métricas com `Double` sem arredondamento prematuro.
2. Aplicar fórmulas V1 por intervalo elegível.
3. Tratar zero passos conforme contrato.
4. Acumular somente deltas canônicos.
5. Calcular totais pela duração e soma canônica.
6. Proibir acesso funcional à estimativa agregada diagnóstica.
7. Propagar elegibilidade e plausibilidade para consumidores.
8. Persistir/fornecer read model reproduzível.
9. Atualizar overview.

## Critérios de aceite

- Fórmulas e unidades correspondem à spec.
- Acumulado é monotônico e termina exatamente na distância canônica.
- Intervalo zero segue contrato completo.
- Série ausente não produz `NaN`, infinito, zero sintético ou placeholder.
- Métricas importadas pela distância original deixam de alimentar consumidores
  de `WATCH_D3_D4`.
- Estimativa por passos totais nunca aparece como total funcional.
- Plausibilidade permanece visível no read model.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- tabela sanitizada intervalo → duração/passos/distância/cadência/velocidade/ritmo;
- acumulado final igual à distância canônica;
- casos zero, ausente e implausível;
- builds e smoke local.

## Definição de pronto

Série V1 completa e consistente, sem fallback ou suavização, com item `014-08`
marcado.

## Rollback

Retornar métricas novas como ausentes e preservar snapshots. Não restaurar
original ou estimativa agregada como fallback canônico.
