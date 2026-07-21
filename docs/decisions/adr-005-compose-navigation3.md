# ADR-005 — Usar Navigation 3 com back stack manual para navegação Compose

## Status

Aceita

## Contexto

Aplicativo atual tem conjunto pequeno de destinos de topo e ainda não depende de
deep links complexos, argumentos ricos ou grafo aninhado extenso.

Implementação vigente usa `androidx.navigation3` e mantém estado de navegação em
`ChampionNavigation.kt` com `mutableStateListOf<AppDestination>`. Destinos são
`@Serializable` e sem parâmetros em `AppDestination`.

Essa escolha difere do padrão mais comum com `NavHost` e `NavController`, então
merece registro explícito.

## Decisão

Manter navegação Compose baseada em `androidx.navigation3` com estado explícito:

- `AppDestination` será sealed class `@Serializable` com rotas estáveis e sem parâmetros nesta fase;
- back stack será mantido manualmente na UI raiz;
- troca entre destinos de topo usará deduplicação no back stack, sem `NavController`;
- introdução de `NavHost`/`NavController` ou rotas parametrizadas só deve ocorrer quando houver necessidade concreta, como deep links, fluxos aninhados complexos ou restauração mais sofisticada.

## Consequências

### Positivas

- navegação atual fica simples e explícita;
- estado cabe integralmente em tipos Kotlin serializáveis;
- comportamento dos destinos de topo é fácil de inspecionar e testar.

### Negativas

- app assume manualmente regras de back stack;
- recursos tradicionais de `NavController` não ficam disponíveis por padrão;
- expansão para fluxos mais complexos pode exigir refatoração da raiz de navegação.

## Alternativas consideradas

- usar `NavHost` e `NavController` desde o início;
- criar roteador próprio sem `androidx.navigation3`;
- modelar destinos com argumentos arbitrários já nesta fase.
