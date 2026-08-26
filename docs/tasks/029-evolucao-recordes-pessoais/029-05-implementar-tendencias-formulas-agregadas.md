# 029-05 — Implementar tendências e fórmulas agregadas

- **Ticker:** `029`
- **Número:** `05`
- **Status:** `pending`
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
